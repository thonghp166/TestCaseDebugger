package auto_testcase_generation.testdatagen;

import auto_testcase_generation.config.PointerOrArrayBound;
import auto_testcase_generation.config.PrimitiveBound;
import auto_testcase_generation.testdatagen.fastcompilation.randomgeneration.BasicTypeRandom;
import auto_testcase_generation.utils.ASTUtils;
import com.dse.config.FunctionConfig;
import com.dse.config.IFunctionConfig;
import com.dse.config.IFunctionConfigBound;
import com.dse.config.UndefinedBound;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.ProjectParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.SizeOfArrayOrPointerDependency;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.Search2;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.gen.module.type.PointerTypeInitiation;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.*;
import com.dse.util.*;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.log4j.Level;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTypeId;

import java.io.File;
import java.util.*;

/**
 * Example:
 * <p>
 * Class: {r1=Rectangle(int,int), r1.a=4, r1.b=9, r2=Triangle()}
 * <p>
 * Pointer class:
 * <p>
 * Struct:
 */
public class RandomInputGeneration {
    final static AkaLogger logger = AkaLogger.get(RandomInputGeneration.class);
    private ICommonFunctionNode functionNode;
    private int depth = 0;
    private String additionalHeader = "";

    private ConstructorNode selectedConstructor;

    private TestCase selectedPrototype;

    // store the template type to real type in template function, e.g, "T"->"int"
    // key: template type
    // value: real type
    private Map<String, String> realTypeMapping;
    /**
     * The maximum depth, especially to avoid infinite expansion in linked list
     * <p>
     * Ex: x: depth = 1; x.a: depth = 2; x.x.a: depth = 3
     */
    private int MAX_DEPTH = 5;

    /**
     * The limit of size in array expansion.
     * If the size of an array is too high, the performance might become poor.
     */
    private final int LIMIT_ARRAY_AND_POINTER_SIZE = 20;

    public static final String DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT = "_";
    public static final String DIMENSIONAL_STARTING = "[";
    public static final String DIMENSIONAL_END = "]";
    public static final String DELIMITER_BETWEEN_STRUCT_INSTANCE_AND_ATTRIBUTE = ".";
    public static final String DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT = "_";
    private RootDataNode root;

    public static void main(String[] args) throws Exception {
        logger.setLevel(Level.DEBUG);
//        ProjectParser parser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/stl/vector"));
//        ProjectParser parser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/autogen"));
        ProjectParser parser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/Algorithmv2/src"));
//        ProjectParser parser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/lamnt/data_entering/templateFunction"));
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        parser.setCpptoHeaderDependencyGeneration_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setFuncCallDependencyGeneration_enabled(true);
        parser.setSizeOfDependencyGeneration_enabled(true);
        parser.setGlobalVarDependencyGeneration_enabled(true);
        parser.setTypeDependency_enable(true);
//        String functionName = "std::map<int, std::string>";
        String functionName = "Rectangle2::Rectangle2(int,int)";

        INode root = parser.getRootTree();
        Environment.getInstance().setProjectNode((ProjectNode) root);
        IFunctionNode n = (IFunctionNode) Search.searchNodes(root, new FunctionNodeCondition(), functionName).get(0);
        logger.debug(n.getAST().getRawSignature());

        // set up function config
        FunctionConfig functionConfig = new FunctionConfig();
        functionConfig.setTheMaximumNumberOfIterations(1);
        functionConfig.getBoundOfArray().setUpper(5 + "");
        n.setFunctionConfig(functionConfig);
        functionConfig.setFunctionNode(n);

        logger.debug("[" + Thread.currentThread().getName() + "] " + n.getAST().getRawSignature());
        RandomInputGeneration random = new RandomInputGeneration();
        random.setFunctionNode(n);
        List<RandomValue> values = random.constructRandomInput(n.getArguments(), n.getFunctionConfig(), "");
        logger.debug("[" + Thread.currentThread().getName() + "] " + "random values = " + values);
    }

    protected List<RandomValue> constructRandomInput(IVariableNode argument, IFunctionConfig functionConfig,
                                                     String prefixName) {
        if (prefixName == null)
            prefixName = "";
        List<RandomValue> input = new ArrayList<>();
        List<IVariableNode> vars = new ArrayList<>();
        vars.add(argument);
        input.addAll(constructRandomInput(vars, functionConfig, prefixName));
        return input;
    }

    IVariableNode originalCurrentVarNode = null;

    protected List<RandomValue> constructRandomInput(List<IVariableNode> arguments, IFunctionConfig functionConfig,
                                                     String prefixName) {
        logger.debug("\n");
        List<RandomValue> input = new ArrayList<>();
        for (IVariableNode argument : arguments) {
            if (depth == 0)
                originalCurrentVarNode = argument;
            else if (depth > MAX_DEPTH) {
                logger.debug("Interrupt " + argument.getName() + " due to reach the maximum of test chain (MAX_DEPTH = " + MAX_DEPTH + ")");
                continue;
            }
            depth++;
            try {
                logger.debug("Analyze " + prefixName + argument.getName());

                // normalize type
                String type = argument.getRealType();
//                String type = argument.getRawType().replaceAll("\\b" + argument.getCoreType() +"\\b", realType);
                type = VariableTypeUtils.deleteStorageClasses(type);
                type = VariableTypeUtils.deleteReferenceOperator(type);

                // Boolean
                if (VariableTypeUtils.isBoolBasic(type)) {
                    logger.debug(type + ": isBoolBasic");
                    handleBool(input, prefixName, argument, functionConfig);

                } // Character
                else if (VariableTypeUtils.isChBasic(type)) {
                    logger.debug(type + ": isChBasic");
                    handleChBasic(input, prefixName, argument, functionConfig);

                } // Number
                else if (VariableTypeUtils.isNumBasic(type)) {
                    logger.debug(type + ": isNumBasic");
                    handleNumBasic(input, prefixName, argument, type, functionConfig);

                }
                // Template
                else if (TemplateUtils.isTemplateTypeDefinedByUser(type, functionConfig.getFunctionNode())) {
                    logger.debug(type + ": isTemplateTypeDefinedByUser");
                    handleTemplateTypeDefinedByUser(input, prefixName, argument, type, functionConfig);

                }
                // Macro
                else if (argument.getRawType().equals(MacroFunctionNode.MACRO_UNDEFINE_TYPE)){
                    logger.debug(type + ": handleMacroTypeDefinedByUser");
                    handleTemplateTypeDefinedByUser(input, prefixName, argument, type, functionConfig);
                }
                // Structure
                else if (VariableTypeUtils.isStructureSimple(type)) { // ok
                    logger.debug(type + ": isStructureSimple");
                    handleStructureSimple(input, prefixName, argument, type, functionConfig);

                } // Std
                else if (VariableTypeUtilsForStd.isStdVectorBasic(type)) {
                    logger.debug(type + ": isStdVectorBasic");
                    handleStdVector(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdListBasic(type)) {
                    logger.debug(type + ": isStdListBasic");
                    handleStdList(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdSetBasic(type)) {
                    logger.debug(type + ": isStdSetBasic");
                    handleStdSet(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdStackBasic(type)) {
                    logger.debug(type + ": isStdStackBasic");
                    handleStdStack(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdQueueBasic(type)) {
                    logger.debug(type + ": isStdSQueueBasic");
                    handleStdQueue(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdPairBasic(type)) {
                    logger.debug(type + ": isStd Pair Basic");
                    handleStdPair(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isStdMapBasic(type)) {
                    logger.debug(type + ": isStd Map Basic");
                    handleStdMap(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtilsForStd.isSharedPtr(type)) {
                    logger.debug(type + ": isSharedPtr");
                    handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*shared_ptr\\s*<",
                            SharedPtrDataNode.class.getName());
                } else if (VariableTypeUtilsForStd.isUniquePtr(type)) {
                    logger.debug(type + ": isUniquePtr");
                    handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*unique_ptr\\s*<",
                            UniquePtrDataNode.class.getName());
                } else if (VariableTypeUtilsForStd.isWeakPtr(type)) {
                    logger.debug(type + ": isWeakPtr");
                    handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*weak_ptr\\s*<",
                            WeakPtrDataNode.class.getName());
                } else if (VariableTypeUtilsForStd.isAutoPtr(type)) {
                    logger.debug(type + ": isAutoPtr");
                    handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*auto_ptr\\s*<",
                            AutoPtrDataNode.class.getName());
                }

                // Multi-dimension
                else if (VariableTypeUtils.isNumMultiDimension(type)
                        || VariableTypeUtils.isChMultiDimension(type)

                        || VariableTypeUtilsForStd.isStdVectorMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdListMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdStackMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdSetMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdQueueMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdPairMultiDimension(type)
                        || VariableTypeUtilsForStd.isStdMapMultiDimension(type)

                        || VariableTypeUtils.isBoolMultiDimension(type)
                        || VariableTypeUtils.isStructureMultiDimension(type)
                        || VariableTypeUtils.isStrMultiDimension(type)) {
                    logger.debug(type + ": is multi dimension");
                    handleMultiDimensionalArray(input, prefixName, argument, type, functionConfig,
                            "(\\[[a-zA-Z0-9\\s]*\\]\\s*)(\\[[a-zA-Z0-9\\s]*\\]\\s*)*$", "$2");

                }
                // Multi-level
                else if (VariableTypeUtils.isNumMultiLevel(type)
                        || VariableTypeUtils.isChMultiLevel(type)

                        || VariableTypeUtilsForStd.isStdVectorMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdListMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdStackMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdSetMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdQueueMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdPairMultiLevel(type)
                        || VariableTypeUtilsForStd.isStdMapMultiLevel(type)

                        || VariableTypeUtils.isBoolMultiLevel(type)
                        || VariableTypeUtils.isStructureMultiLevel(type)
                        || VariableTypeUtils.isStrMultiLevel(type)) {
                    logger.debug(type + ": is multi level");
                    handleMultiLevelPointer(input, prefixName, argument, type, functionConfig,
                            "\\*\\s*$", "");

                } else if (VariableTypeUtils.isStrBasic(type)) {
                    logger.debug(type + ": isString");
                    handleString(input, prefixName, argument, type, functionConfig);

                } else if (VariableTypeUtils.isVoidPointer(type)) {
                    logger.debug(type + ": isVoidPointer");
                    handleVoidPointer(input, prefixName, argument, type, functionConfig);

                } else {
                    logger.debug("Do not support to generate a random value of " + type);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                depth--;
            }
        }
        return input;
    }

    private void handleVoidPointer(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + argument.getName() + "\"");

        final int PRIMITIVE_TYPE = 0;
        final int POINTER_TYPE = 1;
        final int STD_SET = 2;
        final int STD_LIST = 3;
        final int STD_MAP = 4;
        final int STD_QUEUE = 5;
        final int STD_STACK = 6;
        final int STD_VECTOR = 7;
        final int STD_PAIR = 8;


        // get all basic types
        List<Class> possibleClasses = new ArrayList<>();
        possibleClasses.add(VariableTypeUtils.BASIC.STRING.class);
        possibleClasses.add(VariableTypeUtils.BASIC.NUMBER.class);
        possibleClasses.add(VariableTypeUtils.BASIC.CHARACTER.class);
        possibleClasses.add(VariableTypeUtils.BASIC.BOOLEAN.class);
        Class selectedClass = possibleClasses.get(new Random().nextInt(possibleClasses.size()));
        logger.debug("selectedClass = " + selectedClass);
        List<String> possibleTypes = VariableTypeUtils.getAllBasicFieldNames(selectedClass);
        logger.debug("possibleTypes = " + possibleTypes);

        // generate complex type
        int ran = PRIMITIVE_TYPE; // default for both C project and Cpp project
        if (Environment.getInstance().getCompiler().isGPlusPlusCommand())
            ran = new Random().nextInt(9);
        switch (ran) {
            case PRIMITIVE_TYPE:{
                input.add(new RandomValue(prefixName + argument.getName(),
                        // a random value
                        "void* " + argument.getName() + " = " +
                                new RandomDataGenerator().nextInt(-999999, 999999) +";"));
                break;
            }

            case POINTER_TYPE: {
                String selectedType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                input.add(new RandomValue(prefixName + argument.getName(),
                        "void* " + argument.getName() + " = new " + selectedType + "[" + functionConfig.getBoundOfArray().getLower() + "];"));
                break;
            }
            case STD_LIST: {
                String selectedType = "list";
                additionalHeader += "#include <list>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_MAP: {
                String selectedType = "map";
                additionalHeader += "#include <map>;";
                String coreType1 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                String coreType2 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* p = new std::map<int, float>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s, %s>[%s];",
                                argument.getName(), selectedType, coreType1, coreType2, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_SET: {
                String selectedType = "set";
                additionalHeader += "#include <set>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: std::map<int, float>* p = new std::map<int, float>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_QUEUE: {
                String selectedType = "queue";
                additionalHeader += "#include <queue>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* p = new std::map<int, float>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_STACK: {
                String selectedType = "stack";
                additionalHeader += "#include <stack>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::stack<char>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_VECTOR: {
                String selectedType = "vector";
                additionalHeader += "#include <vector>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::vector<char>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_PAIR: {
                String selectedType = "pair";
                additionalHeader += "#include <utility>;";
                String coreType1 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                String coreType2 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::vector<char, char>[5];
                input.add(new RandomValue(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s, %s>[%s];",
                                argument.getName(), selectedType, coreType1, coreType2, functionConfig.getBoundOfArray().getLower())));
                break;
            }
        }
    }

//    private void handleMacroTypeDefinedByUser(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
//        // generate random value
//        if (this.selectedPrototype != null && this.selectedPrototype.getRootDataNode() != null) {
//            SubprogramNode subprogramNode = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode());
//
//            // map template type and real type
//            List<IDataNode> args = Search2.findArgumentNodes(root);
//            realTypeMapping = new HashMap<>();
//            for (IDataNode arg : args)
//                if (arg instanceof ValueDataNode) {
//                    realTypeMapping.put(arg.getName(), ((ValueDataNode) arg).getType());
//                }
//
//            //
//            List<IVariableNode> vars = new ArrayList<>();
//            for (IDataNode child : subprogramNode.getChildren())
//                if (!(child.getName().equals(INameRule.RETURN_VARIABLE_NAME_PREFIX)))
//                    if (child instanceof ValueDataNode)
//                        if (argument.getName().equals(child.getName())) {
//                            IVariableNode variableNode = new TmpVariableNode();
//                            variableNode.setName(argument.getName());
//                            variableNode.setRawType(realTypeMapping.get(argument.getName()));
//                            variableNode.setParent(argument.getParent());
//
//                            vars.add(variableNode);
//                        }
//
//            input.addAll(constructRandomInput(vars, functionConfig, prefixName));
//        }
//    }

    private void handleTemplateTypeDefinedByUser(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        // generate random value
        if (this.selectedPrototype != null && this.selectedPrototype.getRootDataNode() != null) {
            SubprogramNode subprogramNode = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode());

            // map template type and real type
            List<IDataNode> args = Search2.findArgumentNodes(root);
            realTypeMapping = new HashMap<>();
            for (IDataNode arg : args)
                if (arg instanceof ValueDataNode) {
                    realTypeMapping.put(arg.getName(), ((ValueDataNode) arg).getType());
                }

            //
            List<IVariableNode> vars = new ArrayList<>();
            for (IDataNode child : subprogramNode.getChildren())
                if (!(child.getName().equals(INameRule.RETURN_VARIABLE_NAME_PREFIX)))
                    if (child instanceof ValueDataNode)
                        if (argument.getName().equals(child.getName())) {
                            IVariableNode variableNode = new TmpVariableNode();
                            variableNode.setName(argument.getName());
                            variableNode.setRawType(realTypeMapping.get(argument.getName()));
                            variableNode.setParent(argument.getParent());

                            vars.add(variableNode);
                        }

            input.addAll(constructRandomInput(vars, functionConfig, prefixName));
        }
    }

    private String arrayElement(long index) {
        return DIMENSIONAL_STARTING + index + DIMENSIONAL_END;
    }

    public static String arraySize(String arrayName, long size) {
        return "sizeof(" + arrayName + ")=" + size;
    }

    private long getFirstSizeOfArray(String varName, String type, IFunctionConfig functionConfig) {
        // get size
        int level = PointerTypeInitiation.getLevel(type);
        if (level == 0)
            // is an array
            level = Utils.getIndexOfArray(type).size();

        long size = 0;
        IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(varName);
        if (bound != null) {
            if (bound instanceof PointerOrArrayBound) {
                List<String> indexes = ((PointerOrArrayBound) bound).getIndexes();
                String currentIndex = indexes.get(indexes.size() - level);
                long lower = 0;
                long upper = 0;
                if (currentIndex.contains(IFunctionConfigBound.RANGE_DELIMITER)) {
                    lower = Long.parseLong(currentIndex.split(IFunctionConfigBound.RANGE_DELIMITER)[0]);
                    upper = Long.parseLong(currentIndex.split(IFunctionConfigBound.RANGE_DELIMITER)[1]);
                } else {
                    lower = Long.parseLong(currentIndex);
                    upper = Long.parseLong(currentIndex);
                }
                size = BasicTypeRandom.random(lower, upper);
            }
        } else {
            List<String> indexes = Utils.getIndexOfArray(type);
            if (indexes.size() >= 1 && Utils.toInt(indexes.get(0)) != Utils.UNDEFINED_TO_INT)
                size = Utils.toInt(indexes.get(0)); // return the first size
            else
                size = BasicTypeRandom.random(functionConfig.getBoundOfArray().getLowerAsLong(),
                        functionConfig.getBoundOfArray().getUpperAsLong());
        }
        return size;
    }

    private long getFirstSizeOfPointer(IFunctionConfig functionConfig) {
        return BasicTypeRandom.random(
                functionConfig.getBoundOfPointer().getLowerAsLong(),
                functionConfig.getBoundOfPointer().getUpperAsLong());
    }

    private void handleString(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("type " + type + " : handleString");

        long size = getFirstSizeOfPointer(functionConfig);
        if (size == 0) // string size >= 1
            size = 1;

        //
        String stringValue = arraySize(prefixName + argument.getName(), size);
        input.add(new RandomValue(prefixName + argument.getName(), stringValue));

        //
        for (int i = 0; i < size; i++) {
            String value = "";
            String configName = getConfigName(argument);
            IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
            if (bound == null)
                bound = functionConfig.getBoundOfOtherCharacterVars();

            String lower = IFunctionConfigBound.MIN_VARIABLE_TYPE, upper = IFunctionConfigBound.MAX_VARIABLE_TYPE;
            if (bound instanceof PrimitiveBound) {
                lower = ((PrimitiveBound) bound).getLower();
                upper = ((PrimitiveBound) bound).getUpper();
            }

            String elementType = NormalStringDataNode.getStringToCharacterTypeMap().get(type.replace(" ", ""));
            if (elementType != null && elementType.length() > 0) {
                long ascii = BasicTypeRandom.generateInt(
                        lower,
                        upper,
                        elementType);
                value = ascii + "";

                String nameUsedInExpansion = prefixName + argument.getName() + "[" + i + "]";
                input.add(new RandomValue(nameUsedInExpansion, value));
            }
        }
    }

    private void handleStructureSimple(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("type " + type + " : handleStructureSimple");
        INode correspondingNode = argument.resolveCoreType();

        if (correspondingNode instanceof StructNode) {
            logger.debug("Defined in " + correspondingNode.getAbsolutePath());
//            input.add(new RandomValue(prefixName + argument.getName(), correspondingNode.getName()));
            input.addAll(constructRandomInput(((StructNode) correspondingNode).getAttributes(),
                    functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_STRUCT_INSTANCE_AND_ATTRIBUTE));

        } else if (correspondingNode instanceof ClassNode) {
            logger.debug("Defined in " + correspondingNode.getAbsolutePath());
            if (this.selectedConstructor == null) {
                // choose an inheritance class or this class
                List<INode> derivedNodes = ((ClassNode) correspondingNode).getDerivedNodes();
                int randomDerivedNodes = new Random().nextInt(derivedNodes.size());

                // choose a constructor among constructors
                ArrayList<ICommonFunctionNode> constructors = ((ClassNode) derivedNodes.get(randomDerivedNodes)).getConstructors();
                logger.debug("There are " + constructors.size() + " candidate constructors");
                int randomConstructor = new Random().nextInt(constructors.size());

                String nameUsedInExpansion = prefixName + argument.getName();
                String value = constructors.get(randomConstructor).getName();
                logger.debug("Choose constructor " + value);

                input.add(new RandomValue(nameUsedInExpansion, value));
                input.addAll(constructRandomInput(constructors.get(randomConstructor).getArguments(),
                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            } else {
                String nameUsedInExpansion = prefixName + argument.getName();
                String value = this.selectedConstructor.getName();
                input.add(new RandomValue(nameUsedInExpansion, value));
                List<IVariableNode>  arguments = this.selectedConstructor.getArguments();
                this.selectedConstructor = null; // reset
                input.addAll(constructRandomInput(arguments,
                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            }

        } else if (correspondingNode instanceof EnumNode) {
            List<String> possibleValues = ((EnumNode) correspondingNode).getAllNameEnumItems();
            String chosenValue = possibleValues.get(new Random().nextInt(possibleValues.size()));
            input.add(new RandomValue(prefixName + argument, chosenValue));

        } else if (correspondingNode instanceof UnionNode) {
            // choose a random attribue in union
            List<Node> possibleValues = correspondingNode.getChildren();
            IVariableNode chosenValue = (IVariableNode) possibleValues.get(new Random().nextInt(possibleValues.size()));
            input.add(new RandomValue(prefixName + argument, chosenValue.getName()));

            // generate value for attribute
            TmpVariableNode tmpvar = new TmpVariableNode();
            String insideType = chosenValue.getRawType();
            tmpvar.setRawType(insideType);
            tmpvar.setName(chosenValue.getName());
            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(argument.getParent());
            tmpvar.setAbsolutePath(argument.getAbsolutePath());

            input.addAll(constructRandomInput(tmpvar,
                    functionConfig, prefixName + argument.getName() + "."));
        } else {
            logger.debug("not found definition of " + type);
        }
    }

    private void handleChBasic(List<RandomValue> input, String prefixName, IVariableNode argument, IFunctionConfig functionConfig) {
        String configName = getConfigName(argument);
        IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
        if (bound == null)
            bound = functionConfig.getBoundOfOtherCharacterVars();

        if (bound instanceof PrimitiveBound) {
            String lower = ((PrimitiveBound) bound).getLower();
            String upper = ((PrimitiveBound) bound).getUpper();
            long ascii = BasicTypeRandom.generateInt(lower, upper, argument.getRawType());

            String nameUsedInExpansion = prefixName + argument.getName();

//            String value = "";
//            if (ascii >= 33 && ascii <= 126)
//                // visible range
//                // http://www.asciitable.com
//                value = (char) ascii + "";
//            else
//            String value = NormalCharacterDataNode.OCTAL_NUMBER_PREFIX + ascii;
            String value = ascii + "";

            input.add(new RandomValue(nameUsedInExpansion, value));
        }
    }

    private String getConfigName(IVariableNode variableNode) {
        return variableNode.getName();
    }

    private void handleNumBasic(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        if (VariableTypeUtils.isNumBasicFloat(type)) {
            String nameUsedInExpansion = prefixName + argument.getName();
            String configName = getConfigName(argument);
            IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
            if (bound == null)
                bound = functionConfig.getBoundOfOtherNumberVars();

            if (bound instanceof PrimitiveBound) {
                String lower = ((PrimitiveBound) bound).getLower();
                String upper = ((PrimitiveBound) bound).getUpper();
                String value = BasicTypeRandom.generateFloat(lower, upper) + "";
                input.add(new RandomValue(nameUsedInExpansion, value));
            }
        } else {
            // integer type
            boolean isSizeOfArrayOrPointer = false;
            // two cases happen: (1) this variable is size of array/pointer, (2) this variable is not size of array/pointer
            if (argument.getParent() instanceof FunctionNode)
                for (Dependency d : argument.getDependencies())
                    if (d instanceof SizeOfArrayOrPointerDependency) {
                        isSizeOfArrayOrPointer = true;
                        String nameUsedInExpansion = prefixName + argument.getName();
                        String value = functionConfig.getBoundOfArray().getLower() + "";
                        input.add(new RandomValue(nameUsedInExpansion, value));
                        break;
                    }
            if (!isSizeOfArrayOrPointer) {
                String nameUsedInExpansion = prefixName + argument.getName();

                String configName = getConfigName(argument);
                IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);

                // when we do not know the bound of variable
                if (bound == null || bound instanceof UndefinedBound)
                    bound = functionConfig.getBoundOfOtherNumberVars();

                if (bound instanceof PrimitiveBound) {
                    String lower = ((PrimitiveBound) bound).getLower();
                    String upper = ((PrimitiveBound) bound).getUpper();
                    String value = BasicTypeRandom.generateInt(lower, upper, argument.getRawType()) + "";
                    input.add(new RandomValue(nameUsedInExpansion, value));
                }
            }
        }
    }

    private void handleBool(List<RandomValue> input, String prefixName, IVariableNode argument, IFunctionConfig functionConfig) {
        // 0 - false; 1 - true
        String configName = getConfigName(argument);
        IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
        if (bound == null)
            bound = functionConfig.getBoundOfOtherCharacterVars();

        if (bound instanceof PrimitiveBound) {
            String lower = ((PrimitiveBound) bound).getLower();
            String upper = ((PrimitiveBound) bound).getUpper();

            String nameUsedInExpansion = prefixName + argument.getName();
            long value = BasicTypeRandom.generateInt(lower, upper, argument.getRawType());
            input.add(new RandomValue(nameUsedInExpansion, value + ""));
        }

    }

    private List<Long> generateAListOfRandomNumbers(long realSize, long limit) {
        List<Long> randomNumbers = new ArrayList<>();

        if (limit < realSize) {
            // just select some random indexes to initialize values
            for (int i = 0; i < limit; i++)
                randomNumbers.add(BasicTypeRandom.random(0, realSize));
        } else {
            for (long i = 0; i < realSize; i++)
                randomNumbers.add(i);
        }
        return randomNumbers;
    }

    private void handleMultiDimensionalArray(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig,
                                             String regrex, String replacement) {
        long size = getFirstSizeOfArray(originalCurrentVarNode.getName(), type, functionConfig);

        logger.debug("Size = " + size);
        String nameUsedInExpansion = prefixName + argument.getName();
        String value = arraySize(prefixName + argument.getName(), size);
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        List<Long> randomNumbers = generateAListOfRandomNumbers(size, LIMIT_ARRAY_AND_POINTER_SIZE);
        for (Long index : randomNumbers) {
//        for (int index = 0; index < size; index++)
            IVariableNode tmpvar = new TmpVariableNode();
            // Ex: "a[3][5]" -> "a[3]"
            String insideType = type.replaceAll(regrex, replacement);
            tmpvar.setRawType(insideType);
            tmpvar.setName(arrayElement(index));
            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(argument.getParent());
            tmpvar.setAbsolutePath(argument.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefixName + argument.getName()));
    }

    private void handleMultiLevelPointer(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig,
                                         String regrex, String replacement) {
        if (depth >= MAX_DEPTH) {
            logger.debug("Assign pointer to null");
            String nameUsedInExpansion = prefixName + argument.getName();
            String value = arraySize(prefixName + argument.getName(), PointerDataNode.NULL_VALUE);
            input.add(new RandomValue(nameUsedInExpansion, value));

        } else {
            long size = getFirstSizeOfArray(originalCurrentVarNode.getName(), type, functionConfig);
            if (size == 0) {
                logger.debug("Assign pointer to null");
                String nameUsedInExpansion = prefixName + argument.getName();
                String value = arraySize(prefixName + argument.getName(), PointerDataNode.NULL_VALUE);
                input.add(new RandomValue(nameUsedInExpansion, value));
            } else {
                logger.debug("Size = " + size);
                String nameUsedInExpansion = prefixName + argument.getName();
                String value = arraySize(prefixName + argument.getName(), size);
                input.add(new RandomValue(nameUsedInExpansion, value));
            }

            List<IVariableNode> list = new ArrayList<>();
            List<Long> randomNumbers = generateAListOfRandomNumbers(size, LIMIT_ARRAY_AND_POINTER_SIZE);
            for (Long index : randomNumbers) {
//        for (int index = 0; index < size; index++)
                IVariableNode tmpvar = new TmpVariableNode();
                // Ex: "a[3][5]" -> "a[3]"
                String insideType = type.replaceAll(regrex, replacement);
                tmpvar.setRawType(insideType);
                tmpvar.setName(arrayElement(index));
                tmpvar.setCoreType(insideType);
                tmpvar.setReducedRawType(insideType);
                tmpvar.setParent(argument.getParent());
                tmpvar.setAbsolutePath(argument.getAbsolutePath());
                list.add(tmpvar);
            }

            input.addAll(constructRandomInput(list,
                    functionNode.getFunctionConfig(),
                    prefixName + argument.getName()));
        }
    }

    private void handleStdStack(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::stack is "top"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "top");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdSet(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::set is "begin"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "begin");
            else if (i == size - 1)
                // the virtual name of the last element in std::set is "end"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "end");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdList(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::list is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "front");
            else if (i == size - 1)
                // the virtual name of the last element in std::list is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "back");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdVector(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);
            tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);
            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdQueue(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::queue is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "front");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdMap(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + "";
        input.add(new RandomValue(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            // Ex: "map<int, int>" -> "pair<int,int>"
            String insideType = type.replaceFirst("\\bmap\\b\\s*<", "pair<");
            tmpvar.setRawType(insideType);
            tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);
            tmpvar.setCoreType(insideType);
            tmpvar.setReducedRawType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleSmartPointer(List<RandomValue> input, String prefixName, IVariableNode argument, String type,
                                    IFunctionConfig functionConfig,
                                    String regex, String smartPointerObject) {
        if (this.selectedConstructor == null) {
            String templateType = type.replaceAll(regex, "").replace(">", "").trim();
            String nameUsedInExpansion = prefixName + argument.getName();
            try {
                SmartPointerDataNode smartPointerDataNode = (SmartPointerDataNode) Class.forName(smartPointerObject).newInstance();

                int randIndex = new Random().nextInt(smartPointerDataNode.getConstructors().length);
                String value = smartPointerDataNode.getConstructors()[randIndex];
                logger.debug("Choose constructor " + value);
                value = value.replaceAll("\\bT\\b", templateType);

                input.add(new RandomValue(nameUsedInExpansion, value));
                smartPointerDataNode.setType("auto");

                smartPointerDataNode.chooseConstructor(value);
                ICommonFunctionNode selectedConstructorAST = smartPointerDataNode.getSelectedConstructor();
                List<IVariableNode> arguments = new ArrayList<>();
                for (INode node : selectedConstructorAST.getChildren())
                    if (node instanceof IVariableNode) {
                        node.setAbsolutePath(functionConfig.getFunctionNode().getAbsolutePath() + File.separator + node.getName());
                        arguments.add((IVariableNode) node);
                    }
                input.addAll(constructRandomInput(arguments,
                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            String nameUsedInExpansion = prefixName + argument.getName();
            String value = this.selectedConstructor.getName();
            input.add(new RandomValue(nameUsedInExpansion, value));
            List<IVariableNode> arguments = this.selectedConstructor.getArguments();
            this.selectedConstructor = null; // reset
            input.addAll(constructRandomInput(arguments,
                    functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
        }
    }

    private void handleStdPair(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        List<IVariableNode> list = new ArrayList<>();

        // Ex: "std::pair<int, float>" -> "int, float"
        CPPASTDeclarationStatement ast = (CPPASTDeclarationStatement) ASTUtils.convertToIAST(type + " a;");
        IASTNode qualifiedName = ast;
        while (!(qualifiedName instanceof CPPASTQualifiedName) && !(qualifiedName instanceof CPPASTTemplateId)) {
            qualifiedName = qualifiedName.getChildren()[0];
        }

        CPPASTTemplateId pair = null;
        if (qualifiedName instanceof CPPASTQualifiedName) {
            if (qualifiedName.getChildren()[0].getRawSignature().equals("std")) {
                pair = (CPPASTTemplateId) qualifiedName.getChildren()[1];
            } else {
                pair = (CPPASTTemplateId) qualifiedName.getChildren()[0];
            }
        } else if (qualifiedName instanceof CPPASTTemplateId)
            pair = (CPPASTTemplateId) qualifiedName;

        CPPASTTypeId astTypeA = (CPPASTTypeId) pair.getChildren()[1];
        CPPASTTypeId astTypeB = (CPPASTTypeId) pair.getChildren()[2];

        // first type
        String typeA = astTypeA.getRawSignature();
        IVariableNode tmpvarA = new TmpVariableNode();
        tmpvarA.setRawType(typeA);
        tmpvarA.setName(".first");
        tmpvarA.setCoreType(typeA);
        tmpvarA.setReducedRawType(typeA);
        tmpvarA.setParent(var.getParent());
        tmpvarA.setAbsolutePath(var.getAbsolutePath());
        list.add(tmpvarA);
        logger.debug("Type A = " + typeA);
        //g++ -c -std=c++98 "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/g/test-drivers/testPair9.425857.cpp" -o"/Users/ducanhnguyen/Documents/akautauto/local/working-directory/g/test-drivers/testPair9.425857.out"
        // second type
        String typeB = astTypeB.getRawSignature();
        IVariableNode tmpvarB = new TmpVariableNode();
        tmpvarB.setRawType(typeB);
        tmpvarB.setName(".second");
        tmpvarB.setCoreType(typeB);
        tmpvarB.setReducedRawType(typeB);
        tmpvarB.setParent(var.getParent());
        tmpvarB.setAbsolutePath(var.getAbsolutePath());
        list.add(tmpvarB);
        logger.debug("Type B = " + typeB);

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    protected boolean assignPointerToNull() {
        return new Random().nextInt(6/* default */) == 1;
//        return false;
    }

    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    public static Map<String, String> getRandomValuesUsedInExpansion(List<RandomValue> randomValues) {
        Map<String, String> output = new HashMap<>();
        for (RandomValue randomValue : randomValues) {
            output.put(randomValue.getNameUsedInExpansion(), randomValue.getValue());
        }
        return output;
    }

    public static Map<String, String> getRandomValuesUsedInCode(List<RandomValue> randomValues) {
        Map<String, String> output = new HashMap<>();
        for (RandomValue randomValue : randomValues) {
            output.put(randomValue.getNameUsedToUpdateValue(), randomValue.getValue());
        }
        return output;
    }

    public String getAdditionalHeader() {
        return additionalHeader;
    }

    public void setAdditionalHeader(String additionalHeader) {
        this.additionalHeader = additionalHeader;
    }

    public void setRoot(RootDataNode root) {
        this.root = root;
    }

    public RootDataNode getRoot() {
        return root;
    }

    public void setSelectedPrototype(TestCase selectedPrototype) {
        this.selectedPrototype = selectedPrototype;
    }

    public TestCase getSelectedPrototype() {
        return selectedPrototype;
    }

    public Map<String, String> getRealTypeMapping() {
        return realTypeMapping;
    }

    public void setRealTypeMapping(Map<String, String> realTypeMapping) {
        this.realTypeMapping = realTypeMapping;
    }

    public ConstructorNode getSelectedConstructor() {
        return selectedConstructor;
    }

    public void setSelectedConstructor(ConstructorNode selectedConstructor) {
        this.selectedConstructor = selectedConstructor;
    }
}

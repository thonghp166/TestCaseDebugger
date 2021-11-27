package com.dse.testcase_manager;

import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.ResolveCoreTypeHelper;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.TypeDependency;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.ClassNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.search.condition.VariableNodeCondition;
import com.dse.testdata.Iterator;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.*;
import com.dse.util.*;
import com.google.gson.*;
import com.dse.util.AkaLogger;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class TestCaseDataImporter {
    private final static AkaLogger logger = AkaLogger.get(TestCaseDataImporter.class);

    public static void main(String[] args) {
    }

    public DataNode importRootDataNode(JsonObject json) {
        // change serialization for specific types
        JsonDeserializer<DataNode> deserializer = (json1, typeOfT, context) -> {
            JsonObject jsonObject = json1.getAsJsonObject();
            DataNode node = null;
            ValueDataNode dataNode = null;

            try {
                String type = jsonObject.get("type").getAsString();
                node = (DataNode) Class.forName(type).newInstance();

                if (node != null) {
                    String name = jsonObject.get("name").getAsString();
                    node.setName(name);

                    String dataType = "";
                    boolean external;

                    if (node instanceof ValueDataNode) {
                        dataNode = (ValueDataNode) node;

                        JsonElement tempDataTypeJsonElement = jsonObject.get("dataType");
                        if (tempDataTypeJsonElement != null) {
                            dataType = tempDataTypeJsonElement.getAsString();
                            dataNode.setType(dataType);
                        }

                        JsonElement tempExternalJsonElement = jsonObject.get("external");
                        if (tempExternalJsonElement != null) {
                            external = tempExternalJsonElement.getAsBoolean();
                            dataNode.setExternel(external);
                        }
                    }

                    if (node instanceof MacroSubprogramDataNode)
                        dataNode.setType(dataType);

                    if (node instanceof RootDataNode) {
                        RootDataNode root = (RootDataNode) node;

                        String level = jsonObject.get("level").getAsString();
                        NodeType nodeType = stringToNodeType(level);

                        if (nodeType != null) {
                            root.setLevel(nodeType);
                            if (nodeType.equals(NodeType.ROOT)) {
                                String path = jsonObject.get("functionNode").getAsString();
                                path = PathUtils.toAbsolute(path);
                                ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(path);
                                root.setFunctionNode(functionNode);
                            }
                        }

                    } else if (node instanceof UnitNode) {
                        UnitNode unit = (UnitNode) node;

                        String sourcePath = jsonObject.get("sourceNode").getAsString();
                        sourcePath = PathUtils.toAbsolute(sourcePath);

                        List<INode> sourceCodeFileNodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());
                        for (INode scfnode : sourceCodeFileNodes) {
                            SourcecodeFileNode cast = (SourcecodeFileNode) scfnode;
                            if (Utils.normalizePath(cast.getAbsolutePath()).equals(Utils.normalizePath(sourcePath)))
                                unit.setSourceNode(cast);
                        }
//                        List<INode> possibles = Search.searchNodes(
//                                Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition(), Utils.normalizePath(sourcePath));
//                        if (!possibles.isEmpty())
//                            unit.setSourceNode(possibles.get(0));

//                        boolean stubChildren = jsonObject.get("stubChildren").getAsBoolean();
//                        unit.setStubChildren(stubChildren);

                    } else if (dataNode instanceof NormalCharacterDataNode
                            || dataNode instanceof NormalStringDataNode
                            || dataNode instanceof NormalNumberDataNode) {
                        loadCorrespondingDependency(jsonObject, dataNode);

                        String value = jsonObject.get("value").getAsString();
                        if (!value.equals("null"))
                            ((NormalDataNode) dataNode).setValue(value);

                    } else if (dataNode instanceof PointerDataNode
                            && jsonObject.get("correspondingVar") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode)
                                && jsonObject.get("size") != null && jsonObject.get("level") != null) {
                            int level = jsonObject.get("level").getAsInt();
                            ((PointerDataNode) dataNode).setLevel(level);

                            int size = jsonObject.get("size").getAsInt();
                            if (size >= 0) {
                                ((PointerDataNode) dataNode).setAllocatedSize(size);
                                ((PointerDataNode) dataNode).setSizeIsSet(true);
                            }
                        }

                    } else if (dataNode instanceof OneDimensionDataNode
                            && jsonObject.get("correspondingVar") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode) && jsonObject.get("size") != null) {
                            int size = jsonObject.get("size").getAsInt();
                            if (size >= 0) {
                                ((OneDimensionDataNode) dataNode).setSize(size);
                                ((OneDimensionDataNode) dataNode).setSizeIsSet(true);
                            }
                        }

                    } else if (dataNode instanceof MultipleDimensionDataNode
                            && jsonObject.get("correspondingVar") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode)
                                && jsonObject.get("size") != null && jsonObject.get("dimensions") != null) {
                            int dimensions = jsonObject.get("dimensions").getAsInt();
                            String[] sizesInString = jsonObject.get("size").getAsString().split(", ");

                            if (dimensions == sizesInString.length) {
                                int[] sizes = new int[dimensions];
                                boolean error = false;

                                for (int i = 0; i < dimensions; i++) {
                                    sizes[i] = Integer.parseInt(sizesInString[i]);
                                    if (sizes[i] <= 0)
                                        error = true;
                                }

                                if (!error) {
                                    ((MultipleDimensionDataNode) dataNode).setDimensions(dimensions);
                                    ((MultipleDimensionDataNode) dataNode).setSizes(sizes);
                                    ((MultipleDimensionDataNode) dataNode).setSizeIsSet(true);
                                }
                            }
                        }

                    } else if (dataNode instanceof ConstructorDataNode
                            && jsonObject.get("functionNode") != null) { // need to set function node for it
                        String path = jsonObject.get("functionNode").getAsString();
                        path = PathUtils.toAbsolute(path);

                        try {
                            INode functionNode = UIController.searchFunctionNodeByPath(path);
                            ((ConstructorDataNode) dataNode).setFunctionNode(functionNode);
                        } catch (FunctionNodeNotFoundException e) {
                            String prototype = path;

                            IASTNode astNode = Utils.convertToIAST(prototype);

                            if (astNode instanceof IASTDeclarationStatement) {
                                astNode = ((IASTDeclarationStatement) astNode).getDeclaration();
                            }

                            if (astNode instanceof CPPASTSimpleDeclaration) {
                                DefinitionFunctionNode functionNode = new DefinitionFunctionNode();

                                String ptrPath = jsonObject.get("pointer_path").getAsString();
                                ptrPath = PathUtils.toAbsolute(ptrPath);

                                functionNode.setAbsolutePath(ptrPath);
                                functionNode.setAST((CPPASTSimpleDeclaration) astNode);
                                functionNode.setName(functionNode.getNewType());
                                ((ConstructorDataNode) dataNode).setFunctionNode(functionNode);
                            }
                        }


                    } else if (dataNode instanceof SubprogramNode) { // need to set function node for it
                        String path = jsonObject.get("functionNode").getAsString();
                        path = PathUtils.toAbsolute(path);

                        INode functionNode = UIController.searchFunctionNodeByPath(path);
                        ((SubprogramNode) dataNode).setFunctionNode(functionNode);

                        if (dataNode instanceof MacroSubprogramDataNode || dataNode instanceof TemplateSubprogramDataNode)
                            dataNode.setType(dataType);

                        if (dataNode instanceof TemplateSubprogramDataNode)
                            // load the template parameters of template functions
                            if (jsonObject.get(TemplateSubprogramDataNode.NAME_TEMPLATE_TYPE) != null){
                                JsonArray template_types = jsonObject.getAsJsonArray(TemplateSubprogramDataNode.NAME_TEMPLATE_TYPE);
                                Map<String, String> realTypeMapping = new HashMap<>();
                                for (JsonElement element: template_types){
//                                    String str = element.getAsString().substring(1, element.getAsString().length()-1);
                                    realTypeMapping.put(element.getAsString().split("->")[0],
                                            element.getAsString().split("->")[1]);
                                }
                                ((TemplateSubprogramDataNode) dataNode).setRealTypeMapping(realTypeMapping);
                            }

                        if (dataNode instanceof MacroSubprogramDataNode)
                            // load the template parameters of macro functions
                            if (jsonObject.get(MacroSubprogramDataNode.NAME_MACRO_TYPE) != null){
                                JsonArray macro_types = jsonObject.getAsJsonArray(MacroSubprogramDataNode.NAME_MACRO_TYPE);
                                Map<String, String> realTypeMapping = new HashMap<>();
                                for (JsonElement element: macro_types){
//                                    String str = element.getAsString().substring(1, element.getAsString().length()-1);
                                    realTypeMapping.put(element.getAsString().split("->")[0],
                                            element.getAsString().split("->")[1]);
                                }
                                ((MacroSubprogramDataNode) dataNode).setRealTypeMapping(realTypeMapping);
                            }

                    } else if (dataNode instanceof SubClassDataNode
                            && jsonObject.get("correspondingType") != null
                            && jsonObject.get("correspondingVar") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode)
                                && jsonObject.get("selectedConstructor") != null) {
                            VariableNode prevVar = dataNode.getCorrespondingVar();

                            if (!dataType.equals(VariableTypeUtils.getFullRawType(prevVar))) {
                                String classPath = jsonObject.get("correspondingType").getAsString();
                                classPath = PathUtils.toAbsolute(classPath);

//                                INode classNode = Search.searchNodes(
//                                        Environment.getInstance().getProjectNode(), new ClassNodeCondition(), classPath).get(0);
                                List<INode> nodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new ClassNodeCondition());
                                for (INode n : nodes) {
                                    ClassNode cast = (ClassNode) n;
                                    if (Utils.normalizePath(cast.getAbsolutePath()).equals(Utils.normalizePath(classPath))) {
                                        dataNode.setCorrespondingVar(refactorVariableType(dataType, prevVar, n));
                                        break;
                                    }
                                }
                                if (dataNode.getCorrespondingVar() == null) {
                                    logger.debug("Failed to search corresponding var of the data node: " + dataNode.getName());
                                }
                            }

                            String constructorName = jsonObject.get("selectedConstructor").getAsString();
                            if (constructorName != null) {
                                try {
                                    ((SubClassDataNode) dataNode).chooseConstructor(constructorName);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (dataNode instanceof ClassDataNode
                            && jsonObject.get("correspondingVar") != null
                            && jsonObject.get("correspondingType") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            VariableNode prevVar = dataNode.getCorrespondingVar();

                            if (!dataType.equals(VariableTypeUtils.getFullRawType(prevVar))) {
                                String classPath = jsonObject.get("correspondingType").getAsString();
                                classPath = PathUtils.toAbsolute(classPath);

//                                INode classNode = Search.searchNodes(
//                                        Environment.getInstance().getProjectNode(), new ClassNodeCondition(), classPath).get(0);
//                                dataNode.setCorrespondingVar(refactorVariableType(dataType, prevVar, classNode));
                                List<INode> nodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new ClassNodeCondition());
                                for (INode n : nodes) {
                                    ClassNode cast = (ClassNode) n;
                                    if (Utils.normalizePath(cast.getAbsolutePath()).equals(Utils.normalizePath(classPath))) {
                                        dataNode.setCorrespondingVar(refactorVariableType(dataType, prevVar, n));
                                        break;
                                    }
                                }
                                if (dataNode.getCorrespondingVar() == null) {
                                    logger.debug("Failed to search corresponding var of the data node: " + dataNode.getName());
                                }
                            }
                        }

                    } else if (dataNode instanceof EnumDataNode
                            && jsonObject.get("correspondingVar") != null
                            && jsonObject.get("correspondingType") != null) {

                        if (loadCorrespondingDependency(jsonObject, dataNode)
                                && jsonObject.get("value") != null && jsonObject.get("valueIsSet") != null) {
                            String value = jsonObject.get("value").getAsString();
                            String valueIsSet = jsonObject.get("valueIsSet").getAsString();
                            if (valueIsSet.equals("true")) {
                                ((EnumDataNode) dataNode).setValue(value);
                                ((EnumDataNode) dataNode).setValueIsSet(true);
                            }
                        }
                    } else if (dataNode instanceof UnionDataNode
                            && jsonObject.get("correspondingVar") != null
                            && jsonObject.get("correspondingType") != null) {

                        if (!loadCorrespondingDependency(jsonObject, dataNode))
                            logger.error("Can not load corresponding var for UnionDataNode: " + dataNode.getName());

                    } else if (dataNode instanceof StructDataNode
                            && jsonObject.get("correspondingVar") != null
                            && jsonObject.get("correspondingType") != null) {

                        if (!loadCorrespondingDependency(jsonObject, dataNode))
                            logger.error("Can not load corresponding var for UnionDataNode: " + dataNode.getName());

                    } else if (dataNode instanceof ListBaseDataNode) {
                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            String templateArg = jsonObject.get("templateArg").getAsString();

                            List<String> arguments = new ArrayList<>();
                            arguments.add(templateArg);
                            ((ListBaseDataNode) dataNode).setArguments(arguments);

                            if (jsonObject.get("size") != null) {
                                int size = jsonObject.get("size").getAsInt();
                                ((ListBaseDataNode) dataNode).setSize(size);
                                ((ListBaseDataNode) dataNode).setSizeIsSet(true);
                            }
                        }

                    } else if (dataNode instanceof PairDataNode) {
                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            JsonObject templateArg = jsonObject.get("templateArg").getAsJsonObject();
                            String first = templateArg.get("first").getAsString();
                            String second = templateArg.get("second").getAsString();

                            List<String> arguments = Arrays.asList(first, second);
                            ((STLDataNode) dataNode).setArguments(arguments);
                        }

                    } else if (dataNode instanceof SmartPointerDataNode
                            || dataNode instanceof AllocatorDataNode
                            || dataNode instanceof DefaultDeleteDataNode) {
                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            List<String> arguments = new ArrayList<>();

                            if (jsonObject.get("templateArg") != null) {
                                String templateArg = jsonObject.get("templateArg").getAsString();
                                arguments.add(templateArg);
                            }

                            ((STLDataNode) dataNode).setArguments(arguments);
                        }

                    } else if (dataNode instanceof VoidPointerDataNode) {
                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            String userCode = jsonObject.get("userCode").getAsString();

                            ((VoidPointerDataNode) dataNode).setUserCode(userCode);
                        }
                    } else if (dataNode instanceof OtherUnresolvedDataNode) {
                        if (loadCorrespondingDependency(jsonObject, dataNode)) {
                            String userCode = jsonObject.get("userCode").getAsString();

                            ((OtherUnresolvedDataNode) dataNode).setUserCode(userCode);
                        }
                    }

                }

                // load children
                if (jsonObject.get("children") != null) {
                    for (JsonElement child : jsonObject.get("children").getAsJsonArray()) {
                        DataNode childNode = context.deserialize(child, DataNode.class);
                        //TODO: build project tree
                        if (childNode != null && node != null)
                            generateTreeDependency(node, childNode);
                    }
                }

                // if dataNode is sut, load expected outputs of parameters
                if (dataNode instanceof SubprogramNode) {
                    if (jsonObject.get("paramExpectedOuputs") != null) {
                        for (JsonElement eo : jsonObject.get("paramExpectedOuputs").getAsJsonArray()) {
                            DataNode eoDataNode = context.deserialize(eo, DataNode.class);
                            if (eoDataNode != null) {
                                eoDataNode.setParent(node);
                                ((SubprogramNode) dataNode).putParamExpectedOutputs((ValueDataNode) eoDataNode);
                            }
                        }
                    }
                } else if (node instanceof RootDataNode && ((RootDataNode) node).getLevel().equals(NodeType.GLOBAL)) {
                    if (jsonObject.get("paramExpectedOuputs") != null) {
                        ((RootDataNode) node).setGlobalInputExpOutputMap(new HashMap<>());
                        for (JsonElement eo : jsonObject.get("paramExpectedOuputs").getAsJsonArray()) {
                            DataNode eoDataNode = context.deserialize(eo, DataNode.class);
                            if (eoDataNode != null) {
                                eoDataNode.setParent(node);
                                if (! ((RootDataNode) node).putGlobalExpectedOutput((ValueDataNode) eoDataNode)) {
                                    logger.debug("Failed when import and put expected output to GlobalInputExpectedOutputMap");
                                }
                            }
                        }
                    }
                }

                // load iterator
                if (jsonObject.get("iterators") != null && dataNode != null) {
                    JsonArray iteratorsJsonArray = jsonObject.get("iterators").getAsJsonArray();
                    List<Iterator> iterators = loadIterators(context, dataNode, iteratorsJsonArray);
                    dataNode.setIterators(iterators);
                }

            } catch (FunctionNodeNotFoundException fe) {
                logger.debug("function node not found: " + fe.getFunctionPath());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return node;
        };

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DataNode.class, deserializer);
        Gson customGson = gsonBuilder.create();
        return customGson.fromJson(json, DataNode.class);
    }

    private List<Iterator> loadIterators(JsonDeserializationContext context, ValueDataNode node, JsonArray iteratorsJsonArray) {
        List<Iterator> iterators = new ArrayList<>();

        for (JsonElement iteratorJsonElement : iteratorsJsonArray) {
            JsonObject iteratorJsonObj = iteratorJsonElement.getAsJsonObject();

            Iterator iterator = new Iterator();

            int start = iteratorJsonObj.get("start").getAsInt();
            iterator.setStartIdx(start);

            int repeat = iteratorJsonObj.get("repeat").getAsInt();
            iterator.setRepeat(repeat);

            JsonElement dataNodeJsonObj = iteratorJsonObj.get("dataNode");

            if (dataNodeJsonObj == null) {
                iterator.setDataNode(node);
            } else {
                ValueDataNode childNode = context.deserialize(dataNodeJsonObj, DataNode.class);
                iterator.setDataNode(childNode);
                childNode.setIterators(iterators);
            }

            iterators.add(iterator);
        }

        return iterators;
    }

    private VariableNode refactorVariableType(String dataType, VariableNode prevVar, INode classNode) {
        return VariableTypeUtils.cloneAndReplaceType(dataType, prevVar, classNode);
    }

    private boolean loadCorrespondingDependency(JsonObject jsonObject, ValueDataNode dataNode) {
        if (jsonObject.get("correspondingVar") != null) {
            String absolutePath = jsonObject.get("correspondingVar").getAsString();
            absolutePath = PathUtils.toAbsolute(absolutePath);

            boolean success = loadCorrespondingVar(dataNode, absolutePath);
            if (jsonObject.get("correspondingType") != null) {
                // can not find the var on physical tree
                if (!success) {
                    String correspondingType = jsonObject.get("correspondingType").getAsString();
                    correspondingType = PathUtils.toAbsolute(correspondingType);
                    success = loadVariableFromCorrespondingType(dataNode,
                            correspondingType, absolutePath);
                }
            } else {
                // can not find the var on physical tree
                if (!success) {
                    VariableNode v = createVariableNode(dataNode, absolutePath);
                    dataNode.setCorrespondingVar(v);

                    success = true;
                }
            }

            return success;
        } else {
            return false;
        }
    }

    private void generateTreeDependency(DataNode parent, DataNode child) {
        // set subClass
        if (child instanceof SubClassDataNode && parent instanceof ClassDataNode)
            ((ClassDataNode) parent).setSubClass((SubClassDataNode) child);
        else {
            parent.getChildren().add(child);
            child.setParent(parent);

            if (child instanceof ValueDataNode) {
                List<Iterator> iterators = ((ValueDataNode) child).getIterators();

                if (iterators.size() > 1) {
                    for (int i = 1; i < iterators.size(); i++) {
                        iterators.get(i).getDataNode().setParent(parent);
                    }
                }
            }
        }
    }

    private boolean loadCorrespondingVar(ValueDataNode dataNode, String absolutePath) {
        String relativePath = absolutePath.substring(absolutePath.indexOf(File.separator));
        ProjectNode root = Environment.getInstance().getProjectNode();
        List<INode> nodes = Search.searchNodes(root, new VariableNodeCondition(), relativePath);

        if (nodes.size() == 1) {
            VariableNode variableNode = (VariableNode) nodes.get(0);
            // set corresponding variable for subclass data node
            dataNode.setCorrespondingVar(variableNode);
            return true;
        }

        nodes = Search.searchNodes(Environment.getInstance().getSystemLibraryRoot(), new VariableNodeCondition(), relativePath);

        if (nodes.size() == 1) {
            VariableNode variableNode = (VariableNode) nodes.get(0);
            // set corresponding variable for subclass data node
            dataNode.setCorrespondingVar(variableNode);
            return true;
        }

        return false;
    }

    private boolean loadVariableFromCorrespondingType(ValueDataNode dataNode,
                                                      String typeAbsolutePath, String varAbsolutePath) {
        // Find corresponding type node in project tree.
        INode type = ResolveCoreTypeHelper.getType(typeAbsolutePath);

        if (type == null) {
            if (dataNode instanceof NormalDataNode
                || dataNode instanceof OneDimensionCharacterDataNode || dataNode instanceof OneDimensionNumberDataNode || dataNode instanceof OneDimensionStringDataNode
                || dataNode instanceof MultipleDimensionCharacterDataNode || dataNode instanceof MultipleDimensionNumberDataNode || dataNode instanceof MultipleDimensionStringDataNode
                || dataNode instanceof PointerNumberDataNode || dataNode instanceof PointerCharacterDataNode || dataNode instanceof PointerStringDataNode) {
                type = new AvailableTypeNode();
                ((AvailableTypeNode) type).setType(dataNode.getType());
            } else if (dataNode instanceof STLDataNode) {
                type = new STLTypeNode();
                ((STLTypeNode) type).setType(dataNode.getType());
            } else
                return false;
        }

        // STEP1: Create variable node & add property
        VariableNode v = createVariableNode(dataNode, varAbsolutePath);

        // STEP2: Generate type dependency
        v.setTypeDependencyState(true);
        v.setCorrespondingNode(type);
        Dependency dependency = new TypeDependency(v, type);
        type.getDependencies().add(dependency);
        v.getDependencies().add(dependency);

        dataNode.setCorrespondingVar(v);

        return true;
    }

    private VariableNode createVariableNode(ValueDataNode dataNode, String varAbsolutePath) {
        VariableNode v = new VariableNode();
        String name = dataNode.getName();
        String rType = dataNode.getType();

        if (name.equals("RETURN"))
            v = new ReturnVariableNode();
        else if (name.startsWith(IGTestConstant.INSTANCE_VARIABLE))
            v = new InstanceVariableNode();

        v.setName(name);
        v.setRawType(rType);
        v.setReducedRawType(rType);

        String cType;
        if (TemplateUtils.isTemplate(rType))
            cType = rType.substring(0, rType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1);
        else
            cType = rType.replaceAll(IRegex.POINTER, SpecialCharacter.EMPTY)
                    .replaceAll(IRegex.ARRAY_INDEX, SpecialCharacter.EMPTY);
        v.setCoreType(cType);

        v.setAbsolutePath(varAbsolutePath);

        return v;
    }

    private NodeType stringToNodeType(String type) {
        NodeType nodeType = null;

        switch (type) {
            case "ROOT":
                nodeType = NodeType.ROOT;
                break;
            case "UUT":
                nodeType = NodeType.UUT;
                break;
            case "GLOBAL":
                nodeType = NodeType.GLOBAL;
                break;
            case "STUB":
                nodeType = NodeType.STUB;
                break;
            case "DONT_STUB":
                nodeType = NodeType.DONT_STUB;
                break;
            case "SBF":
                nodeType = NodeType.SBF;
                break;
        }

        return nodeType;
    }
}

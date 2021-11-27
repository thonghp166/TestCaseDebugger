package com.dse.stub_manager;

import com.dse.compiler.message.ICompileMessage;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.EnviroLibraryStubNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.ProjectParser;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.FunctionCallDependency;
import com.dse.parser.object.*;
import com.dse.project_init.ProjectClone;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.util.*;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.dse.stub_manager.StubManager.STUB_EXTENSION;

public class SystemLibrary {
    private static final AkaLogger logger = AkaLogger.get(SystemLibrary.class);
    public static final String LIBRARY_EXTENSION = ".akalib.h";
    public static final String LIBRARY_ROOT = "LIBRARY";
    private static final String LIBRARY_VIRTUAL_DIRECTORY = "libraries/";

    public static void main(String[] args) {
        Map<String, String> config = new HashMap<>();
        config.put("copy_backward", "algorithm");
        config.put("fill", "algorithm");
        config.put("adjacent_find", "algorithm");
        config.put("scanf", "stdio.h");
        config.put("hello", "stdio.h");
        config.put("equal_range", "algorithm");

        List<INode> output = generate(config);

        System.out.println(output.size());
    }

    /**
     * Generate stub code for stub libraries.
     */
    public static void generateStubCode() {
        List<IEnvironmentNode> stubLibraries = EnvironmentSearch
                .searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroLibraryStubNode());

        for (IEnvironmentNode stubLibrary : stubLibraries) {
            if (stubLibrary instanceof EnviroLibraryStubNode) {
                Map<String, String> libraries = ((EnviroLibraryStubNode) stubLibrary).getLibraries();

                generate(libraries);
            }
        }
    }

    /**
     * After generate definition of library, AKA save source code
     * in working directory for reuses.
     *
     * Open environment AKA will parse source code to build
     * the physical tree.
     *
     * @return system library root
     */
    public static SystemLibraryRoot parseFromFile() {
        ProjectParser parser = new ProjectParser(new File(getLibrariesDirectory()));
        parser.setExpandTreeuptoMethodLevel_enabled(true);

        ProjectNode temp = parser.getRootTree();
        if (temp == null)
            return null;

        final String location = temp.getAbsolutePath();

        SystemLibraryRoot systemLibrary = new SystemLibraryRoot();
        systemLibrary.setLocation(location);
        systemLibrary.setAbsolutePath(LIBRARY_ROOT);
        systemLibrary.getChildren().addAll(temp.getChildren());

        for (INode child : temp.getChildren())
            child.setParent(systemLibrary);

        preprocess(systemLibrary, location);

        return systemLibrary;
    }

    /**
     * Generate list of definition function node after select which
     * library funtion to stub
     *
     * @param functionToLib map function -> library
     *              Ex: printf -> stdio.h
     * @return list of definition function node
     */
    public static List<INode> generate(Map<String, String> functionToLib) {
        List<INode> output = new ArrayList<>();

        Map<String, List<String>> list = mapLibraryToFunction(functionToLib);
        for (Map.Entry<String, List<String>> entry : list.entrySet()) {
            String library = entry.getKey();

            String librarySourceCode = generateSystemLibrarySource(library);

            try {
                INode root = parseSystemLibrary(library);

                List<INode> allFunctions = Search.searchNodes(root, new AbstractFunctionNodeCondition());

                List<INode> functions = new ArrayList<>();

                for (String function : entry.getValue()) {
                    List<INode> definitions = allFunctions.stream()
                            .filter(node -> {
                                ICommonFunctionNode functionNode = (ICommonFunctionNode) node;

                                return functionNode.getSingleSimpleName().equals(function)
                                        && functionNode.getVisibility() == ICPPASTVisibilityLabel.v_public
                                        && !(functionNode.getParent() instanceof StructureNode)
                                        && !(functionNode.getParent() instanceof ICommonFunctionNode);

                            }).collect(Collectors.toList());

                    if (definitions.isEmpty())
                        definitions = generateLibraryFunction(function, library, librarySourceCode);

                    for (INode definition : definitions) {
                        if (!functions.contains(definition)) {
                            String absolutePath = LIBRARY_ROOT + File.separator + library + File.separator + definition.getName();
                            definition.setAbsolutePath(absolutePath);
                            definition.getChildren().removeIf(c -> c instanceof ICommonFunctionNode);
                            StubManager.addStubFile(absolutePath, getLibraryStubFilePath(library, (ICommonFunctionNode) definition));
                            functions.add(definition);
                        }
                    }
                }

                saveDefinitionsToFile(functions, library);
                output.addAll(functions);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return output;
    }

    /**
     * Check if current function call library function.
     *
     * @param functionName of library function (called)
     * @param functionNode current function node (callee)
     */
    public static boolean isUseLibrary(String functionName, ICommonFunctionNode functionNode) {
        for (Dependency dependency : functionNode.getDependencies()) {
            if (dependency instanceof FunctionCallDependency) {
                if (dependency.getStartArrow().equals(functionNode)) {
                    INode called = dependency.getEndArrow();
                    if (called instanceof ICommonFunctionNode) {
                        String calledName = ((ICommonFunctionNode) called).getSingleSimpleName();
                        if (calledName.equals(functionName))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Save definition of library function into file located in working directory.
     *
     * @param functions list function node
     * @param library name
     */
    private static void saveDefinitionsToFile(List<INode> functions, String library) {
        String sourceCode = String.format("#include <%s>\nusing namespace std;\n\n", library);

        for (INode function : functions) {
            if (function instanceof ICommonFunctionNode) {
                String functionSourceCode = generateFunctionSourceCode((ICommonFunctionNode) function);

                if (testCompile(library, functionSourceCode)) {
                    sourceCode += functionSourceCode;
                    initializeStubCode((ICommonFunctionNode) function, library);
                }
            }
        }

        String path = getLibrariesDirectory() + library + LIBRARY_EXTENSION;
        Utils.writeContentToFile(sourceCode, path);

        logger.debug("Save library " + library + " successfully");
        logger.debug("Source Code:\n" + sourceCode);
    }

    private static boolean testCompile(String library, String source) {
        String fullSource = String.format("#include <%s>\nusing namespace std;\n\n%s", library,
                source.replaceAll("#.*\n", "").replaceAll("AKA_MARK.+\n", ""));

        String temporarySourceFile = getLibrariesDirectory() + library + "-temp.cpp";
        Utils.writeContentToFile(fullSource, temporarySourceFile);

        ICompileMessage response = Environment.getInstance().getCompiler().compile(temporarySourceFile);

        Utils.deleteFileOrFolder(new File(temporarySourceFile));

        return !response.getType().equals(ICompileMessage.MessageType.ERROR);
    }

    /**
     * initialize stub code for libraries with default template
     *
     * @param function library
     * @param library name
     */
    private static void initializeStubCode(ICommonFunctionNode function, String library) {
        String functionPath = function.getAbsolutePath();
        String functionName = function.getSimpleName();

        StubManager.initializeStubCode(functionName, library, functionPath,
                getLibraryStubFilePath(library, function));
    }

    /**
     * Get libraries archive libraries virtual code
     */
    public static String getLibrariesDirectory() {
        String path = new WorkspaceConfig().fromJson().getStubCodeDirectory();
        if (!path.endsWith(File.separator))
            path += File.separator;
        path += LIBRARY_VIRTUAL_DIRECTORY;

        if (!(new File(path).exists()))
            new File(path).mkdirs();

        return path;
    }

    /**
     * Generate stub code for library function.
     *
     * @param function in libraries
     */
    private static String generateFunctionSourceCode(ICommonFunctionNode function) {
        String functionName = function.getSimpleName();

        String sourceCode;
        IASTNode ast;

        if (function instanceof DefinitionFunctionNode)
            ast = ((DefinitionFunctionNode) function).getAST();
        else
            ast = ((AbstractFunctionNode) function).getAST();

        if (function.isTemplate())
            ast = ast.getParent();

        sourceCode = ast.getRawSignature().replaceAll("\\b" + functionName + "\\b",
                IGTestConstant.STUB_PREFIX + functionName);

        String functionPath = function.getAbsolutePath();
        String stubFilePath = StubManager.getStubCodeFilePath(functionPath);
        String functionCall = generateFunctionCall(function);

        String body;

        String markStm = ProjectClone.generateCallingMark(function.getAbsolutePath());

        if (function.getReturnType().equals(VariableTypeUtils.VOID_TYPE.VOID))
            body = String.format("{\n\t%s\n\t#include \"%s\"\n\n\t%s;\n}\n\n", markStm, stubFilePath, functionCall);
        else
            body = String.format("{\n\t%s\n\t#include \"%s\"\n\n\treturn %s;\n}\n\n", markStm, stubFilePath, functionCall);

        if (sourceCode.endsWith(SpecialCharacter.END_OF_STATEMENT)) {
            sourceCode = sourceCode.replace(SpecialCharacter.END_OF_STATEMENT, body);
        } else if (sourceCode.contains("{"))
            sourceCode = sourceCode.substring(0, sourceCode.indexOf("{")) + body;
        else
            sourceCode += body;

        return sourceCode;
    }

    /**
     * Generate function call expression
     */
    private static String generateFunctionCall(ICommonFunctionNode functionNode) {
        String functionCall = "";

        if (!functionNode.getReturnType().equals(VariableTypeUtils.VOID_TYPE.VOID)) {
            functionCall += functionNode.getSimpleName();

            functionCall += "(";

            for (IVariableNode v : functionNode.getArguments())
                functionCall += v.getName() + ", ";

            functionCall += ")";

            functionCall = functionCall.replace(", )", ")");
        }

        return functionCall;
    }

    /**
     * Convert function -> library to library -> function.
     *
     * @param functionToLib Ex printf -> stdio.h
     * @return Ex: stdio.h -> {printf, scanf,...}
     */
    private static Map<String, List<String>> mapLibraryToFunction(Map<String, String> functionToLib) {
        Map<String, List<String>> output = new HashMap<>();

        for (Map.Entry<String, String> entry : functionToLib.entrySet()) {
            String library = entry.getValue();
            List<String> functions = output.get(library);

            if (output.get(library) == null)
                functions = new ArrayList<>();

            functions.add(entry.getKey());
            output.put(library, functions);
        }

        return output;
    }



    private static INode parseSystemLibrary(String library) throws Exception {
//        String libraryName = library.replaceAll("\\..*", "");

//        String temporarySourceFile = getLibrariesDirectory() + libraryName + "-temp.cpp";
        String preprocessorFilePath = getLibrariesDirectory() + library + LIBRARY_EXTENSION;

//        Utils.writeContentToFile(String.format("#include <%s>", library), temporarySourceFile);
//
//        String sourceCode = Environment.getInstance().getCompiler()
//                .preprocess(temporarySourceFile, preprocessorFilePath);
//
//        sourceCode = sourceCode.replaceAll("#.*\n", "");
//
//        Utils.writeContentToFile(sourceCode, temporarySourceFile);

        //        Utils.deleteFileOrFolder(new File(temporarySourceFile));
//        Utils.deleteFileOrFolder(new File(preprocessorFilePath));

        return new SourcecodeFileParser()
                .parseSourcecodeFile(new File(preprocessorFilePath));
    }

    /**
     * Generate header file source code using pre-processing (option -c -E)
     *
     * @param library name
     * @return source code
     */
    public static String generateSystemLibrarySource(String library) {
        String libraryName = library.replaceAll("\\..*", "");

        String temporarySourceFile = getLibrariesDirectory() + libraryName + "-temp.cpp";
        String preprocessorFilePath = getLibrariesDirectory() + library + LIBRARY_EXTENSION;

        Utils.writeContentToFile(String.format("#include <%s>", library), temporarySourceFile);

        String sourceCode = Environment.getInstance()
                .getCompiler()
                .preprocess(temporarySourceFile, preprocessorFilePath);

//        Utils.deleteFileOrFolder(new File(preprocessorFilePath));
        Utils.deleteFileOrFolder(new File(temporarySourceFile));

        sourceCode = sourceCode.replaceAll("#.*\n", "");

        Utils.writeContentToFile(sourceCode, preprocessorFilePath);

        return sourceCode;

    }

    private static String normalizePreprocessor(String origin) {
        String sourceCode = origin.replaceAll(" \\}", "\n}");
        sourceCode = sourceCode.replaceAll("\\{ ", "{\n");
        sourceCode = sourceCode.replaceAll(";", ";\n");

        String normalizedSourceCode = "";
        String[] lines = sourceCode.split("\\R");

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (!trimmedLine.isEmpty()) {
                if (trimmedLine.endsWith(String.valueOf(SpecialCharacter.OPEN_BRACE))
                        || trimmedLine.endsWith(String.valueOf(SpecialCharacter.CLOSE_BRACE))) {
                    normalizedSourceCode += trimmedLine + SpecialCharacter.LINE_BREAK;
                } else if (trimmedLine.endsWith(SpecialCharacter.END_OF_STATEMENT)) {
                    normalizedSourceCode += trimmedLine + SpecialCharacter.LINE_BREAK;
                } else {
                    normalizedSourceCode += trimmedLine + SpecialCharacter.SPACE;
                }
            }
        }

        normalizedSourceCode = normalizedSourceCode.trim();

        return normalizedSourceCode;
    }

    /**
     * Generate Library Function
     *
     * @param functionName need to stub
     * @param sourceCode after generation
     *
     * @return list of definition funtion node
     */
    private static List<INode> generateLibraryFunction(String functionName, String library, String sourceCode) {
        String normalize = normalizePreprocessor(sourceCode);

        List<String> lines = new ArrayList<>(Arrays.asList(normalize.split("\\R")));

        List<String> definitions = parse(functionName, lines);

        if (definitions.isEmpty())
            logger.error("Function " + functionName + " not found");

        List<INode> functions = new ArrayList<>();

        for (String definition : definitions) {
            definition = normalizeDefinition(definition);

            IASTNode ast = Utils.convertToIAST(definition);

            if (ast instanceof IASTDeclarationStatement)
                ast = ((IASTDeclarationStatement) ast).getDeclaration();

            if (ast instanceof ICPPASTTemplateDeclaration)
                ast = ((ICPPASTTemplateDeclaration) ast).getDeclaration();

            if (!(ast instanceof IASTSimpleDeclaration))
                continue;

            DefinitionFunctionNode functionNode = new DefinitionFunctionNode();
            functionNode.setAST((CPPASTSimpleDeclaration) ast);
            functionNode.setName(functionNode.getNewType());
            String absolutePath = LIBRARY_ROOT + File.separator + library + File.separator + functionNode.getName();
            functionNode.setAbsolutePath(absolutePath);

            if (!functionNode.getName().startsWith(functionName + "("))
                continue;

            List<IVariableNode> arguments = functionNode.getArguments();

            boolean inComplete = false;
            boolean flag = false;

            for (INode f : functions) {
                List<IVariableNode> fArguments = ((DefinitionFunctionNode) f).getArguments();
                if (f.equals(functionNode) && fArguments.size() == arguments.size()) {
                    if (!arguments.isEmpty()) {
                        if (arguments.get(0).getName().isEmpty()) {
                            inComplete = true;
                            flag = true;

                        } else if (fArguments.get(0).getName().isEmpty()) {
                            functions.remove(f);
                            logger.debug("Overload library function " + f + " with " + functionNode + " successfully");
                            flag = true;
                        }
                    }
                }

                if (flag)
                    break;
            }

            if (inComplete)
                continue;

            for (IVariableNode parameter : arguments) {
                // reset absolute path
                parameter.getAbsolutePath();

                String coreType = parameter.getCoreType();
                if (!VariableTypeUtils.isBasic(coreType)
                        || !VariableTypeUtils.isStdInt(coreType)
                        || !VariableTypeUtilsForStd.isSTL(coreType)) {

                    String resolvedCoreType = recursiveResolveCoreType(coreType, lines);

                    parameter.setCoreType(resolvedCoreType);

                    String rawType = parameter.getRawType().replaceAll("\\b" + coreType + "\\b", resolvedCoreType);
                    parameter.setRawType(rawType);
                    parameter.setReducedRawType(rawType);
                }
            }

            functions.add(functionNode);
            logger.debug("Generate library function " + functionNode + " in " + library + " successfully");
        }

        if (functions.isEmpty())
            logger.error("Cant not analyze definition of " + functionName);

        return functions;
    }

    private static String getLibraryStubFilePath(String library, ICommonFunctionNode function) {
        String stubDirectory = new WorkspaceConfig().fromJson().getStubCodeDirectory();
        if (!stubDirectory.endsWith(File.separator))
            stubDirectory += File.separator;

        String subprogramName = function.getSingleSimpleName();


        int offset;
        if (function instanceof AbstractFunctionNode) {
            IASTFileLocation location = ((AbstractFunctionNode) function).getNodeLocation();
            offset = location.getNodeOffset();
        } else
            offset = function.getArguments().size();

        return stubDirectory + library + SpecialCharacter.DOT
                + subprogramName + SpecialCharacter.DOT
                + offset + STUB_EXTENSION;
    }

    private static String recursiveResolveCoreType(String coreType, List<String> source) {
        if (VariableTypeUtils.isBasic(coreType) || !VariableTypeUtils.isStdInt(coreType) || !VariableTypeUtilsForStd.isSTL(coreType))
            return coreType;

        if (coreType.contains("va_list"))
            return "va_list";

        if (coreType.equals("FILE *"))
            return coreType;

        List<String> typeDeclarations = findLines(source,
                String.format(" %s;", coreType), "typedef ");

        typeDeclarations.addAll(findLines(source, String.format(" %s {", coreType)));

        typeDeclarations.addAll(findLines(source, String.format(" %s{", coreType)));

        int size = typeDeclarations.size();

        if (size == 0) {
            logger.error("Can't find declaration of " + coreType);
            return coreType;
        } else {
            IASTNode astTypeDeclaration = Utils.convertToIAST(typeDeclarations.get(0));

            if (astTypeDeclaration instanceof IASTDeclarationStatement)
                astTypeDeclaration = ((IASTDeclarationStatement) astTypeDeclaration).getDeclaration();

            String resolvedCoreType = ((IASTSimpleDeclaration) astTypeDeclaration).getDeclSpecifier().toString();

            return recursiveResolveCoreType(resolvedCoreType, source);
        }
    }

    private static List<String> findLines(List<String> lines, String... keywords) {
        List<String> output = new ArrayList<>();

        int i = 0;
        while (i < lines.size()) {
            if (match(lines.get(i), keywords)) {
                String completeLine = lines.get(i) + "";

                while (!completeLine.contains(";")
                        || Utils.countCharIn(completeLine, '{') != Utils.countCharIn(completeLine, '}')) {
                    i++;
                    completeLine += " " + lines.get(i).trim();
                }

                output.add(completeLine.trim());
            }

            i++;
        }

        return output;
    }

    private static boolean match(String content, String... keywords) {
        for (String keyword : keywords) {
            if (!content.contains(keyword))
                return false;
        }

        return true;
    }


    private static List<String> parse(String functionName, List<String> lines) {
        lines.removeIf(line -> line.trim().isEmpty());

        return new ArrayList<>(findLines(lines, functionName));
    }

    private static String normalizeDefinition(String definition) {
        definition = definition.replaceAll("\\{.*}", SpecialCharacter.END_OF_STATEMENT)
                .replaceAll("\\s+", " ")
                .replace(" ;", SpecialCharacter.END_OF_STATEMENT).trim();

        definition = definition.replaceAll("\\b__restrict\\b", SpecialCharacter.EMPTY);

        definition = definition.replaceAll("\\bthrow.+;", SpecialCharacter.END_OF_STATEMENT);

        return definition;
    }

    private static void preprocess(INode root, final String location) {
        String path = root.getAbsolutePath().replace(location, LIBRARY_ROOT)
                .replace(LIBRARY_EXTENSION, SpecialCharacter.EMPTY)
                .replace(IGTestConstant.STUB_PREFIX, SpecialCharacter.EMPTY);

        root.setAbsolutePath(path);

        if (root instanceof ICommonFunctionNode)
            root.getChildren().removeIf(n -> n instanceof ICommonFunctionNode);

        for (INode child : root.getChildren())
            preprocess(child, location);
    }
}

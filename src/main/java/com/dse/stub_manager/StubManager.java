package com.dse.stub_manager;

import com.dse.config.WorkspaceConfig;
import com.dse.parser.object.*;
import com.dse.search.Search2;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.Iterator;
import com.dse.testdata.object.IDataNode;
import com.dse.testdata.object.SubprogramNode;
import com.dse.testdata.object.ValueDataNode;
import com.dse.util.*;
import com.dse.util.AkaLogger;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StubManager {
    private static final AkaLogger logger = AkaLogger.get(StubManager.class);

    public static final String AKA_TEST_CASE_NAME = "AKA_TEST_CASE_NAME";

    /**
     * Function path -> Stub file path
     */
    private static Map<String, String> fileList = new HashMap<>();

    private static String generateIteratorBody(ValueDataNode dataNode) throws Exception {
        String body = SpecialCharacter.EMPTY;

        if (dataNode.isStubArgument()) {
            List<Iterator> iterators = dataNode.getIterators();

            for (Iterator iterator : iterators) {
                int start = iterator.getStartIdx();
                int repeat = iterator.getRepeat();
                int end = start + repeat;

                String source = iterator.getDataNode().getInputForGoogleTest();

                if (!dataNode.getName().equals(RETURN_NAME)) {
                    source += SpecialCharacter.LINE_BREAK;
                    source += "#ifdef AKA_GTEST_EXECUTION_MODE\n";
                    source += iterator.getDataNode().getAssertionForGoogleTest(IGTestConstant.EXPECT_EQ,
                            iterator.getDataNode().getVituralName(), iterator.getDataNode().getName());
                    source += "\n#endif\n";
                } else {
                    source += String.format("return %s;", iterator.getDataNode().getVituralName());
                }

                body += String.format(ITERATOR_TEMPLATE, start, repeat, end, source);
            }
        }

        return body;
    }

    private static final String ITERATOR_TEMPLATE =
            "if (AKA_INTRA_FCOUNT >= %d && (%d <= 0 || AKA_INTRA_FCOUNT < %d)) {\n" +
                "%s\n" +
            "}\n";

    private static final String RETURN_NAME = "RETURN";

    private static String generateStubBodyFor(SubprogramNode subprogram) throws Exception {
        StringBuilder output = new StringBuilder();

//        output.append(subprogram.getInputForGoogleTest());

//        String returnVariableName = null;

        List<IDataNode> children = subprogram.getChildren();

//        output.append("#ifdef AKA_GTEST_EXECUTION_MODE\n");

        for (IDataNode node : children) {
            if (node instanceof ValueDataNode) {
                ValueDataNode parameter = (ValueDataNode) node;

                output.append(generateIteratorBody(parameter));

//                if (!parameter.getName().equals(RETURN_NAME)) {
//                    output.append(parameter.getAssertionForGoogleTest(IGTestConstant.EXPECT_EQ,
//                            parameter.getVituralName(), parameter.getName()));
//                }
            }
        }

//        output.append("\n#endif\n");

//        if (!children.isEmpty()) {
//            IDataNode returnDataNode = children.get(children.size() - 1);
//            if (returnDataNode.getName().equals(RETURN_NAME)) {
//                returnVariableName = returnDataNode.getVituralName();
//            }
//        }

        if (isVoid(subprogram))
//            output.append(String.format("return %s;", returnVariableName));
//        else
            output.append("return;");

        return output.toString();
    }

    public static void generateStubCode(TestCase testCase) {
        List<IDataNode> stubSubprograms = Search2.searchStubableSubprograms(testCase.getRootDataNode());
        String testCaseName = testCase.getName();

        try {
            for (IDataNode child : stubSubprograms) {
                if (child instanceof SubprogramNode) {
                    SubprogramNode subprogram = (SubprogramNode) child;

                    INode function = subprogram.getFunctionNode();
                    String stubPath = getStubCodeFilePath(function.getAbsolutePath());

                    String testCaseStubCode = SpecialCharacter.EMPTY;

                    if (subprogram.isStub())
                        testCaseStubCode = generateStubCodeFor(testCaseName, subprogram);

                    String stubCode = getStubCode(function);

                    if (isStubByTestCase(stubCode, testCaseName)) {
                        String prevTestCaseStubCode = getTestCaseStubCode(stubCode, testCaseName);
                        stubCode = stubCode.replace(prevTestCaseStubCode, testCaseStubCode);
                    } else {
                        stubCode += testCaseStubCode;
                    }

                    stubCode = stubCode.replaceAll("\n\n\n+", "\n\n");

                    Utils.writeContentToFile(stubCode, stubPath);
                }
            }
        } catch (Exception ex) {
            logger.error("Can't generate stub code: " + ex.getMessage());
        }
    }

    public static String generateStubCodeFor(String testCaseName, SubprogramNode subprogram) throws Exception {
        String code = "";

        String body = generateStubBodyFor(subprogram);
        LocalDateTime updatedTime = LocalDateTime.now();
        String beginTag = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName)
                + BEGIN_LAST_UPDATED_TAG.replace("{{INSERT_LAST_UPDATED_HERE}}",
                DateTimeUtils.getDate(updatedTime) + " " + DateTimeUtils.getTime(updatedTime));
        String endTag = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

        code += beginTag + "\n";
        code += String.format("if (%s == \"%s\") {\n%s\n}\n", AKA_TEST_CASE_NAME, testCaseName, body);
        code += endTag + "\n";

        return code;
    }

    private static boolean isVoid(SubprogramNode subprogram) {
        String returnType = subprogram.getType();
        returnType = VariableTypeUtils.deleteStorageClasses(returnType);
        returnType = VariableTypeUtils.deleteVirtualAndInlineKeyword(returnType);

        return returnType.equals(VariableTypeUtils.VOID_TYPE.VOID);
    }

//    private String generateStubFunctionPrototype(FunctionCallDependency dependency, boolean isInScope) {
//        ICommonFunctionNode callee = (ICommonFunctionNode) dependency.getStartArrow();
//        ICommonFunctionNode called = (ICommonFunctionNode) dependency.getEndArrow();
//
//        // STEP 1: get full function name
//        String stubFunctionName = StubManager.generateStubFunctionName(called);
//
//        if (!isInScope) {
//            INode parent = callee.getParent();
//
//            while (parent != null) {
//                if (parent instanceof StructureNode || parent instanceof NamespaceNode) {
//                    stubFunctionName = parent.getName() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + stubFunctionName;
//                    parent = parent.getParent();
//                } else if (parent instanceof SourcecodeFileNode)
//                    break;
//            }
//        }
//
//        // STEP 2: get parameter
//        String paramater = called.toString();
//        paramater = paramater.substring(paramater.indexOf('('));
//
//        // STEP 3: get return type
//        String returnType = called.getReturnType();
//
//        String fullReturnType = returnType;
//
//        String coreReturnType = VariableTypeUtils.deleteStorageClasses(returnType);
//        coreReturnType = VariableTypeUtils.deleteStructKeyword(coreReturnType);
//        coreReturnType = VariableTypeUtils.deleteUnionKeyword(coreReturnType);
//
//        if (TemplateUtils.isTemplate(coreReturnType))
//            coreReturnType = coreReturnType.substring(0, coreReturnType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1);
//        else {
//            coreReturnType = coreReturnType.replaceAll(IRegex.POINTER, SpecialCharacter.EMPTY);
//            coreReturnType = coreReturnType.replaceAll(IRegex.ARRAY_INDEX, SpecialCharacter.EMPTY);
//            coreReturnType = coreReturnType.replace(IDataNode.REFERENCE_OPERATOR, SpecialCharacter.EMPTY);
//        }
//
//        if (VariableTypeUtils.isStructureSimple(coreReturnType) || TemplateUtils.isTemplateClass(coreReturnType)) {
//            VariableNode tmpVarNode = new VariableNode();
//            tmpVarNode.setRawType(returnType);
//            tmpVarNode.setCoreType(coreReturnType);
//            tmpVarNode.setAbsolutePath(called.getAbsolutePath());
//            fullReturnType = tmpVarNode.getFullType();
//        }
//
//        if (fullReturnType.isEmpty())
//            fullReturnType = VariableTypeUtils.VOID_TYPE.VOID;
//
//
//        String prefix = returnType.substring(returnType.indexOf(coreReturnType) + coreReturnType.length());
//
//        if (TemplateUtils.isTemplateClass(coreReturnType)) {
//            int openPos = returnType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG);
//            int closePos = returnType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG);
//            fullReturnType = fullReturnType + returnType.substring(openPos, closePos + 1) + prefix;
//        } else
//            fullReturnType = fullReturnType + prefix;
//
//        return fullReturnType + " " + stubFunctionName + paramater;
//    }
//
//    public static String generateStubFunctionName(ICommonFunctionNode functionNode) {
//        String functionPath = functionNode.getAbsolutePath();
//        String projectPath = Environment.getProjectNode().getAbsolutePath();
//
//        String shortenFunctionPath = functionPath;
//        if (functionPath.startsWith(projectPath))
//            shortenFunctionPath = functionPath.substring(projectPath.length());
//
//        if (shortenFunctionPath.startsWith(File.separator))
//            shortenFunctionPath = shortenFunctionPath.substring(1);
//
//        shortenFunctionPath = shortenFunctionPath
//                .replace(SpecialCharacter.SPACE, SpecialCharacter.UNDERSCORE_CHAR)
//                .replace(File.separatorChar, SpecialCharacter.UNDERSCORE_CHAR)
//                .replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);
//
//        shortenFunctionPath = shortenFunctionPath.substring(0, shortenFunctionPath.indexOf('('));
//
//        return IGTestConstant.STUB_PREFIX +  shortenFunctionPath;
//    }

    public static void initializeStubCode(String functionName, String unitName, String functionPath, String filePath) {
        if (new File(filePath).exists()) {
//            logger.debug("Stub code for " + functionName + " in " + unitName + " already exist");
            return;
        }

        final int MAX_LENGTH = 68;

        if (functionName.length() > MAX_LENGTH) {
            int pos = MAX_LENGTH;
            while (functionName.charAt(pos) != ',' && functionName.charAt(pos) != '(' && pos > 0)
                pos--;
            functionName = functionName.substring(0, pos) + "\n *\t\t\t\t\t\t\t" + functionName.substring(pos);
        }

        if (functionPath.length() > MAX_LENGTH) {
            int pos = MAX_LENGTH;
            while (functionPath.charAt(pos) != File.separatorChar && pos > 0)
                pos--;
            functionPath = functionPath.substring(0, pos) + "\n *\t\t\t\t\t\t\t" + functionPath.substring(pos);
        }

        functionPath = PathUtils.toRelative(functionPath);

        String fileBanner = String.format(FILE_BANNER, unitName, functionName, functionPath);

        Utils.writeContentToFile(fileBanner, filePath);

        logger.debug("Initialize stub code for " + functionName + " in " + unitName + " to " + filePath + " successfully");
    }

    public static void initializeStubCode(FunctionNode function) {
        String unit = Utils.getSourcecodeFile(function).getName();
        String name = function.getName();
        String path = function.getAbsolutePath();
        String filePath = new WorkspaceConfig().fromJson().getStubCodeDirectory();
        if (!filePath.endsWith(File.separator))
            filePath += File.separator;
        filePath += getStubCodeFileName(function);

        initializeStubCode(name, unit, path, filePath);
    }



    public static boolean isStubByTestCase(String stubCode, String testCaseName) {
        String begin = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);
        String end = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

        return stubCode.contains(begin) && stubCode.contains(end);
    }

    public static String getTestCaseStubCode(String stubCode, String testCaseName) {
        String begin = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);
        String end = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

        int beginPos = stubCode.indexOf(begin);
        int endPos = stubCode.lastIndexOf(end) + end.length();

        return stubCode.substring(beginPos, endPos);
    }

    public static String getStubCode(INode functionNode) {
        String functionPath = functionNode.getAbsolutePath();
        String stubPath = getStubCodeFilePath(functionPath);

        return Utils.readFileContent(stubPath);
    }


    /**
     * Get corresponding stub code file path
     * <<Unit name>>.<<Subprogram>>.<<Offset>>.stub
     *
     * @param origin subprogram
     * @return corresponding stub code file path
     */
    public static String getStubCodeFileName(AbstractFunctionNode origin) {
        INode unit = Utils.getSourcecodeFile(origin);
        String unitName = unit.getName();
        String subprogramName = origin.getSimpleName()
                .replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, SpecialCharacter.DOT_IN_STRUCT);

        IASTFileLocation location = origin.getNodeLocation();
        int offset = location.getNodeOffset();

        return unitName + SpecialCharacter.DOT + subprogramName + SpecialCharacter.DOT
                + offset + STUB_EXTENSION;
    }

    /**
     * Get file list
     */
    public static Map<String, String> getFileList() {
        if (fileList == null || fileList.isEmpty()) {
            File stubsFile = new File(getStubFileListPath());
            if (stubsFile.exists())
                fileList = importStubFileList();
            else
                fileList = new HashMap<>();
        }

        return fileList;
    }

    /**
     * Add stub file into list
     */
    public static void addStubFile(String functionPath, String stubPath) {
        getFileList().put(functionPath, stubPath);
    }

    /**
     * Get stub file path
     * @param path of corresponding function node
     * @return stub file path
     */
    public static String getStubCodeFilePath(String path) {
        return getFileList().get(path);
    }

    /**
     * Export list stub file
     * function path -> stub file path
     */
    public static void exportListToFile() {
        String path = getStubFileListPath();
        StringBuilder fileContent = new StringBuilder();

        fileList.forEach((k, v) -> fileContent
                .append(PathUtils.toRelative(k))
                .append(": ")
                .append(PathUtils.toRelative(v))
                .append(SpecialCharacter.LINE_BREAK));

        Utils.writeContentToFile(fileContent.toString(), path);
    }

    private static String getStubFileListPath() {
        String path = new WorkspaceConfig().fromJson().getStubCodeDirectory();
        if (!path.endsWith(File.separator))
            path += File.separator;
        path += STUBS_FILE_NAME;

        return path;
    }

    public static Map<String, String> importStubFileList() {
        Map<String, String> fileList = new HashMap<>();
        String path = getStubFileListPath();

        String content = Utils.readFileContent(path);
        String[] lines = content.split("\\R");

        for (String line : lines) {
            String[] pathItems = line.split(":\\s");
            if (pathItems.length == 2) {
                String source = PathUtils.toAbsolute(pathItems[0]);
                String stub = PathUtils.toAbsolute(pathItems[1]);

                fileList.put(source, stub);
            }
        }

        return fileList;
    }

    private static final String FILE_BANNER =
            "/** Static counting variable **/\n" +
            "static int AKA_INTRA_FCOUNT = 0;\n" +
            "AKA_INTRA_FCOUNT++;\n\n" +
            "/*******************************************************************************\n" +
            " * Stub code for subprogram in tested project.\n" +
            " * Initialize automatically by AKA.\n" +
            " *\n" +
            " * @unit: %s\n" +
            " * @name: %s\n" +
            " * @path: %s\n" +
            " ******************************************************************************/\n";

    private static final String BEGIN_STUB_CASE_TAG =
            "/**\n" +
            " * BEGIN AKA STUB CASE\n" +
            " * @name: {{INSERT_TEST_CASE_NAME_HERE}}\n";

    private static final String BEGIN_LAST_UPDATED_TAG =
            " *\n" +
            " * @last-updated: {{INSERT_LAST_UPDATED_HERE}}\n" +
            " */";

    private static final String END_STUB_CASE_TAG = "/** END AKA STUB CASE - {{INSERT_TEST_CASE_NAME_HERE}} */";

    public static final String STUB_EXTENSION = ".stub";

    public static final String STUBS_FILE_NAME = "STUBS.aka";
}

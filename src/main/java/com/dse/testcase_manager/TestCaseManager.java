package com.dse.testcase_manager;

import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.testcasescript.TestcaseSearch;
import com.dse.testcasescript.object.*;
import com.dse.testdata.object.DataNode;
import com.dse.testdata.object.RootDataNode;
import com.dse.util.AkaLogger;
import com.dse.util.DateTimeUtils;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import com.google.gson.*;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

public class TestCaseManager {
    private final static AkaLogger logger = AkaLogger.get(TestCaseManager.class);
    public static Map<String, TestCase> nameToBasicTestCaseMap = new HashMap<>();
    private static Map<String, CompoundTestCase> nameToCompoundTestCaseMap = new HashMap<>();
    private static Map<ICommonFunctionNode, List<String>> functionToTestCasesMap = new HashMap<>();

    public static void main(String[] args) {
//        TestCase testCase = TestCaseManager.getTestCaseByName("mergeTwoArray.57777", "local/hoan_wd/HOHO/testcases");

    }

    public static void clearMaps() {
        nameToBasicTestCaseMap.clear();
        nameToCompoundTestCaseMap.clear();
        functionToTestCasesMap.clear();
    }

    public static void initializeMaps() {
        TestcaseRootNode rootNode = Environment.getInstance().getTestcaseScriptRootNode();
        if (rootNode != null) {
            // initialize nameToBasicTestCaseMap
            List<ITestcaseNode> nodes = TestcaseSearch.searchNode(Environment.getInstance().getTestcaseScriptRootNode(), new TestNormalSubprogramNode());
            try {
                for (ITestcaseNode node : nodes) {
                    String path = ((TestNormalSubprogramNode) node).getName();
                    ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(path);
                    List<ITestcaseNode> testNewNodes = TestcaseSearch.searchNode(node, new TestNewNode());
                    List<String> testCaseNames = new ArrayList<>();
                    for (ITestcaseNode testNewNode : testNewNodes) {
                        List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
                        if (names.size() == 1) {
                            String name = ((TestNameNode) names.get(0)).getName();
                            nameToBasicTestCaseMap.put(name, null);
                            testCaseNames.add(name);
                        }
                    }

                    functionToTestCasesMap.put(functionNode, testCaseNames);
                }
            } catch (FunctionNodeNotFoundException fe) {
                logger.error("Function node not found");
            }

            // initialize nameToCompoundTestCaseMap
            nodes = TestcaseSearch.searchNode(Environment.getInstance().getTestcaseScriptRootNode(), new TestCompoundSubprogramNode());
            List<ITestcaseNode> testNewNodes = TestcaseSearch.searchNode(nodes.get(0), new TestNewNode());

            for (ITestcaseNode testNewNode : testNewNodes) {
                List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
                if (names.size() == 1) {
                    String name = ((TestNameNode) names.get(0)).getName();
                    nameToCompoundTestCaseMap.put(name, null);
                }
            }
        } else {
            logger.error("The test case script root node is null when initialize maps for TestcaseManager.");
        }
    }

    public static TestCase createTestCase(String name, ICommonFunctionNode functionNode) {
        if (!TestCaseManager.checkTestCaseExisted(name)) {
            TestCase testCase = new TestCase(functionNode, name);
            testCase.setCreationDateTime(LocalDateTime.now());

            // need to validate name of testcase
            List<String> testcaseNames = functionToTestCasesMap.get(functionNode);
            if (testcaseNames != null) {
                testcaseNames.add(name);
                nameToBasicTestCaseMap.put(name, testCase);
            } else {
                logger.error("Can not find list testcase names correspond to functionNode: " + functionNode.getAbsolutePath() + "when create test case");
            }

            // init parameter expected outputs
            logger.debug("initParameterExpectedOuputs");
            testCase.initParameterExpectedOuputs(testCase.getRootDataNode());

            logger.debug("initGlobalInputExpOutputMap");
            testCase.initGlobalInputExpOutputMap(testCase.getRootDataNode(), testCase.getGlobalInputExpOutputMap());

            exportBasicTestCaseToFile(testCase);
            exportBreakpointsToFile(testCase);
            return testCase;
        } else {
            return null;
        }
    }

    public static TestCase createMinMidMaxTestCase(String type, IFunctionNode functionNode) {
        String testcaseName;
        switch (type) {
            case "MIN":
                testcaseName = AbstractTestCase.removeSpecialCharacter(functionNode.getSimpleName() + ".MIN");
                return createTestCase(testcaseName, functionNode);
            case "MID":
                testcaseName = AbstractTestCase.removeSpecialCharacter(functionNode.getSimpleName() + ".MID");
                return createTestCase(testcaseName, functionNode);
            case "MAX":
                testcaseName = AbstractTestCase.removeSpecialCharacter(functionNode.getSimpleName() + ".MAX");
                return createTestCase(testcaseName, functionNode);
            default:
                logger.error(type + "is not in {MIN, MID, MAX}");
                return null;
        }
    }

    public static TestCase createTestCase(ICommonFunctionNode functionNode, String nameTestcase) {
        TestCase testCase;
        do {
            String testCaseName = "";
            if (nameTestcase != null || nameTestcase.length() > 0)
                testCaseName = AbstractTestCase.removeSpecialCharacter(nameTestcase);
            else
                testCaseName = AbstractTestCase.removeSpecialCharacter(functionNode.getSimpleName() + ITestCase.AKA_SIGNAL + new Random().nextInt(100000));
            testCase = createTestCase(testCaseName, functionNode);
        } while (testCase == null);
        return testCase;
    }

    public static TestCase createTestCase(ICommonFunctionNode functionNode) {
        TestCase testCase;
        do {
            String testCaseName = AbstractTestCase.removeSpecialCharacter(functionNode.getSimpleName()) + ITestCase.AKA_SIGNAL + new Random().nextInt(100000);
            testCase = createTestCase(testCaseName, functionNode);
        } while (testCase == null);
        return testCase;
    }

    public static CompoundTestCase createCompoundTestCase() {
        String testCaseName = "COMPOUND" + ITestCase.AKA_SIGNAL + new Random().nextInt(100000);
        CompoundTestCase compoundTestCase = new CompoundTestCase(testCaseName);
        compoundTestCase.setCreationDateTime(LocalDateTime.now());

        nameToCompoundTestCaseMap.put(testCaseName, compoundTestCase);
        exportCompoundTestCaseToFile(compoundTestCase);

        return compoundTestCase;
    }

    public static ITestCase getTestCaseByName(String name) {
        ITestCase testCase = getBasicTestCaseByName(name);

        if (testCase == null)
            testCase = getCompoundTestCaseByName(name);

        if (testCase == null)
            logger.error(String.format("Test case %s not found.", name));

        return testCase;
    }

    public static TestCase getBasicTestCaseByNameWithoutData(String name) {
        // find in the map first
        if (nameToBasicTestCaseMap.containsKey(name)) {
            TestCase testCaseInMap = nameToBasicTestCaseMap.get(name);
            if (testCaseInMap != null) {
                return testCaseInMap;

            } else { // haven't loaded yet
                String testCaseDirectory = new WorkspaceConfig().fromJson().getTestcaseDirectory();
                // find and load from disk
                TestCase testCase = getBasicTestCaseByName(name, testCaseDirectory, false);

                if (testCase != null) {
                    nameToBasicTestCaseMap.replace(name, testCase);
                } else {
                    logger.error(String.format("Failed to load test case %s", name));
                }

                return testCase;
            }
        } else {
            logger.error(String.format("Test case %s does not exist.", name));
            return null;
        }
    }

    public static TestCase getBasicTestCaseByName(String name) {
        // find in the map first
        if (nameToBasicTestCaseMap.containsKey(name)) {
            TestCase testCaseInMap = nameToBasicTestCaseMap.get(name);
            if (testCaseInMap != null) {
                if (testCaseInMap.getRootDataNode() == null) {
                    String testCaseDirectory = new WorkspaceConfig().fromJson().getTestcaseDirectory();
                    TestCase testCase = getBasicTestCaseByName(name, testCaseDirectory, true);

                    if (testCase != null)
                        nameToBasicTestCaseMap.replace(name, testCase);

                    return testCase;
                } else
                    return testCaseInMap;
            } else { // haven't loaded yet
                String testCaseDirectory = new WorkspaceConfig().fromJson().getTestcaseDirectory();
                // find and load from disk
                TestCase testCase = getBasicTestCaseByName(name, testCaseDirectory, true);

                if (testCase != null) {
                    nameToBasicTestCaseMap.replace(name, testCase);
                } else {
                    logger.error(String.format("Failed to load test case %s", name));
                }

                return testCase;
            }
        } else {
            logger.error(String.format("Test case %s does not exist.", name));
            return null;
        }
    }

    public static CompoundTestCase getCompoundTestCaseByName(String name) {
        if (nameToCompoundTestCaseMap.containsKey(name)) {
            CompoundTestCase testCaseInMap = nameToCompoundTestCaseMap.get(name);
            if (testCaseInMap != null) {
                return testCaseInMap;
            } else { // haven't loaded yet
                String compoundTestCaseDirectory = new WorkspaceConfig().fromJson().getCompoundTestcaseDirectory();
                File directory = new File(compoundTestCaseDirectory);
                if (directory.exists() && directory.isDirectory()) {
                    for (File compoundTCFile : Objects.requireNonNull(directory.listFiles())) {
                        if (compoundTCFile.getName().equals(name + ".json")) { // testcase.001.json
                            CompoundTestCaseImporter importer = new CompoundTestCaseImporter();
                            CompoundTestCase compoundTestCase = importer.importCompoundTestCase(compoundTCFile);
                            compoundTestCase.validateSlots();
                            TestCaseManager.exportCompoundTestCaseToFile(compoundTestCase);

                            nameToCompoundTestCaseMap.replace(name, compoundTestCase);

                            return compoundTestCase;
                        }
                    }
                }
                logger.error(String.format("Compound test case %s not found.", name));
                return null;
            }
        } else {
            logger.error(String.format("Compound test case %s does not exist.", name));
            return null;
        }
    }

    private static void exportBreakpointsToFile(TestCase testCase) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        json.add(testCase.getRootDataNode().getFunctionNode().getParent().getAbsolutePath(), array);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(json);
        Utils.writeContentToFile(jsonString, testCase.getBreakpointPath());
    }

    private static TestCase parserJsonToTestCaseWithoutData(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();

        String status = "N/A";
        if (jsonObject.get("status") != null)
            status = jsonObject.get("status").getAsString();

        ICommonFunctionNode functionNode = null;
        if (jsonObject.get("rootDataNode") != null) {
            JsonObject rootDataNodeJsonObject = jsonObject.get("rootDataNode").getAsJsonObject();
            String functionPath = rootDataNodeJsonObject.get("functionNode").getAsString();
            functionPath = PathUtils.toAbsolute(functionPath);

            try {
                functionNode = UIController.searchFunctionNodeByPath(functionPath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (functionNode == null)
                return null;

        } else
            return null;

        TestCase testCase = new TestCase();
        testCase.setName(name);
        testCase.setStatus(status);

        if (jsonObject.get("path") != null)
            testCase.setPath(PathUtils.toAbsolute(jsonObject.get("path").getAsString()));

        if (jsonObject.get("sourceCode") != null)
            testCase.setSourceCodeFile(PathUtils.toAbsolute(jsonObject.get("sourceCode").getAsString()));

        if (jsonObject.get("testPath") != null)
            testCase.setTestPathFile(PathUtils.toAbsolute(jsonObject.get("testPath").getAsString()));

        if (jsonObject.get("executable") != null)
            testCase.setExecutableFile(PathUtils.toAbsolute(jsonObject.get("executable").getAsString()));

        if (jsonObject.get("commandConfig") != null)
            testCase.setCommandConfigFile(PathUtils.toAbsolute(jsonObject.get("commandConfig").getAsString()));

        if (jsonObject.get("commandDebug") != null)
            testCase.setCommandDebugFile(PathUtils.toAbsolute(jsonObject.get("commandDebug").getAsString()));

        if (jsonObject.get("breakPoint") != null)
            testCase.setBreakpointPath(PathUtils.toAbsolute(jsonObject.get("breakPoint").getAsString()));

        if (jsonObject.get("debugExecutable") != null)
            testCase.setDebugExecutableFile(PathUtils.toAbsolute(jsonObject.get("debugExecutable").getAsString()));

        if (jsonObject.get("executionResult") != null)
            testCase.setExecutionResultFile(PathUtils.toAbsolute(jsonObject.get("executionResult").getAsString()));

        if (jsonObject.get("creationDate") != null) {
            LocalDateTime dt = DateTimeUtils.parse(jsonObject.get("creationDate").getAsString());
            testCase.setCreationDateTime(dt);
        }

        // todo: nead to validate status
        testCase.setFunctionNode(functionNode);
        return testCase;
    }

    public static TestCase parseJsonToTestCase(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();

        String status = "N/A";

        DataNode rootDataNode;
        if (jsonObject.get("rootDataNode") != null) {
            JsonObject rootDataNodeJsonObject = jsonObject.get("rootDataNode").getAsJsonObject();
            TestCaseDataImporter importer = new TestCaseDataImporter();
            rootDataNode = importer.importRootDataNode(rootDataNodeJsonObject);
            if (rootDataNode == null) {
                logger.debug("Failed to import root data node of " + name);
                return null;
            }
        } else
            return null;

        TestCase testCase = new TestCase();
        testCase.setName(name);
        testCase.setStatus(status);

        // todo: nead to validate status
        testCase.setRootDataNode((RootDataNode) rootDataNode);
        testCase.setFunctionNode(((RootDataNode) rootDataNode).getFunctionNode());
        testCase.updateGlobalInputExpOutputAfterInportFromFile();
        return testCase;
    }

    private static TestCase parserJsonToTestCase(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();

        String status = "N/A";
        if (jsonObject.get("status") != null)
            status = jsonObject.get("status").getAsString();

        int[] result = null;
        if (jsonObject.get("result") != null) {
            String[] tempList = jsonObject.get("result").getAsString().split("/");
            result = new int[tempList.length];

            for (int i = 0; i < tempList.length; i++)
                result[i] = Integer.parseInt(tempList[i]);
        }

        DataNode rootDataNode;
        if (jsonObject.get("rootDataNode") != null) {
            JsonObject rootDataNodeJsonObject = jsonObject.get("rootDataNode").getAsJsonObject();
            TestCaseDataImporter importer = new TestCaseDataImporter();
            rootDataNode = importer.importRootDataNode(rootDataNodeJsonObject);

            if (rootDataNode == null) {
                logger.debug("Failed to import root data node of " + name);
                return null;
            }
        } else
            return null;

        TestCase testCase = new TestCase();
        testCase.setName(name);
        testCase.setStatus(status);
        testCase.setExecutionResult(result);

        if (jsonObject.get("path") != null)
            testCase.setPath(PathUtils.toAbsolute(jsonObject.get("path").getAsString()));

        if (jsonObject.get("sourceCode") != null)
            testCase.setSourceCodeFile(PathUtils.toAbsolute(jsonObject.get("sourceCode").getAsString()));

        if (jsonObject.get("testPath") != null)
            testCase.setTestPathFile(PathUtils.toAbsolute(jsonObject.get("testPath").getAsString()));

        if (jsonObject.get("executable") != null)
            testCase.setExecutableFile(PathUtils.toAbsolute(jsonObject.get("executable").getAsString()));

        if (jsonObject.get("commandConfig") != null)
            testCase.setCommandConfigFile(PathUtils.toAbsolute(jsonObject.get("commandConfig").getAsString()));

        if (jsonObject.get("commandDebug") != null)
            testCase.setCommandDebugFile(PathUtils.toAbsolute(jsonObject.get("commandDebug").getAsString()));

        if (jsonObject.get("breakPoint") != null)
            testCase.setBreakpointPath(PathUtils.toAbsolute(jsonObject.get("breakPoint").getAsString()));

        if (jsonObject.get("debugExecutable") != null)
            testCase.setDebugExecutableFile(PathUtils.toAbsolute(jsonObject.get("debugExecutable").getAsString()));

        if (jsonObject.get("executionResult") != null)
            testCase.setExecutionResultFile(PathUtils.toAbsolute(jsonObject.get("executionResult").getAsString()));

        if (jsonObject.get("creationDate") != null) {
            LocalDateTime dt = DateTimeUtils.parse(jsonObject.get("creationDate").getAsString());
            testCase.setCreationDateTime(dt);
        }

        // todo: nead to validate status
        testCase.setRootDataNode((RootDataNode) rootDataNode);
        testCase.setFunctionNode(((RootDataNode) rootDataNode).getFunctionNode());
        testCase.updateGlobalInputExpOutputAfterInportFromFile();
        return testCase;
    }

    public static String getStatusTestCaseByName(String name, String testCaseDirectory) {
        File directory = new File(testCaseDirectory);
        if (directory.exists() && directory.isDirectory()) {
            for (File testcaseFile : Objects.requireNonNull(directory.listFiles())) {
                if (testcaseFile.getName().equals(name + ".json")) { // testcase.001.json
                    String json = Utils.readFileContent(testcaseFile);
                    if (json.contains(ITestCase.STATUS_SUCCESS))
                        return ITestCase.STATUS_SUCCESS;
                    else if (json.contains(ITestCase.STATUS_EXECUTING))
                        return ITestCase.STATUS_EXECUTING;
                    else if (json.contains(ITestCase.STATUS_NA))
                        return ITestCase.STATUS_NA;
                    else if (json.contains(ITestCase.STATUS_FAILED))
                        return ITestCase.STATUS_FAILED;
                    else
                        return ITestCase.STATUS_EMPTY;
                }
            }
        }
        logger.error(String.format("Basic test case %s not found.", name));
        return null;
    }

    public static TestCase getBasicTestCaseByName(String name, String testCaseDirectory, boolean parseData) {// testcase.001
        String fullPathOfTestcase = new File(testCaseDirectory).getAbsolutePath() + File.separator + name + ".json";
        if (new File(fullPathOfTestcase).exists()) {
            String json = Utils.readFileContent(fullPathOfTestcase);
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = (JsonObject) parser.parse(json);

            if (parseData)
                return parserJsonToTestCase(jsonObject);
            else
                return parserJsonToTestCaseWithoutData(jsonObject);
        }
        logger.error(String.format("Basic test case %s not found.", name));
        return null;
    }

    public static TestCase importBasicTestCaseByFile(File testcaseFile) {
        if (testcaseFile != null) {
            String json = Utils.readFileContent(testcaseFile);
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = (JsonObject) parser.parse(json);
            TestCase testCase = parseJsonToTestCase(jsonObject);

            if (testCase != null) {
                testCase.setName(testCase.getName());

                ICommonFunctionNode functionNode = testCase.getFunctionNode();
                if (!functionToTestCasesMap.containsKey(functionNode)) {
                    logger.debug("Failed to import basic testcase from file " + testcaseFile.getAbsolutePath());
                    return null;
                } else {
                    if (!nameToBasicTestCaseMap.containsKey(testCase.getName())) {
                        nameToBasicTestCaseMap.put(testCase.getName(), testCase);
                        functionToTestCasesMap.get(functionNode).add(testCase.getName());
                        // save to tst file
                        Platform.runLater(testCase::updateToTestCasesNavigatorTree);
                    }

                    TestCaseManager.exportTestCaseToFile(testCase);
                    return testCase;
                }
            }
        }

        return null;
    }

    public static void importTestCasesFromDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            String[] extensions = new String[]{"json"};
            List<File> files = (List<File>) FileUtils.listFiles(directory, extensions, false);// false -> non recursive
            for (File testcaseFile : files) {
                if (importBasicTestCaseByFile(testcaseFile) == null) {
                    logger.error("Failed to import test case from file: " + testcaseFile.getAbsolutePath());
                } else {
                    logger.debug("Import testcase successfully: " + testcaseFile.getAbsolutePath());
                }
            }
        }
    }

    public static void exportCompoundTestCaseToFile(CompoundTestCase compoundTestCase) {
        logger.debug("Export test compound to " + compoundTestCase.getPath());
        CompoundTestCaseExporter exporter = new CompoundTestCaseExporter();
        File file = new File(compoundTestCase.getPath());
        exporter.export(file, compoundTestCase);
    }

    public static void exportTestCaseToFile(ITestCase testCase) {
        if (testCase instanceof TestCase)
            exportBasicTestCaseToFile((TestCase) testCase);
        else if (testCase instanceof CompoundTestCase)
            exportCompoundTestCaseToFile((CompoundTestCase) testCase);
    }

    public static void exportBasicTestCaseToFile(TestCase testCase) {
        JsonObject json = new JsonObject();
        json.addProperty("name", testCase.getName());

        String status = testCase.getStatus();
        if (status != null) {
            json.addProperty("status", testCase.getStatus());
        } else {
            json.addProperty("status", "null");
        }

        int[] result = testCase.getExecutionResult();
        if (result != null)
            json.addProperty("result", String.format("%d/%d", result[0], result[1]));

        json.addProperty("path", PathUtils.toRelative(testCase.getPath()));
        if (testCase.getSourceCodeFile() != null)
            json.addProperty("sourceCode", PathUtils.toRelative(testCase.getSourceCodeFile()));
        if (testCase.getTestPathFile() != null)
            json.addProperty("testPath", PathUtils.toRelative(testCase.getTestPathFile()));
        if (testCase.getExecutableFile() != null)
            json.addProperty("executable", PathUtils.toRelative(testCase.getExecutableFile()));
        if (testCase.getCommandConfigFile() != null)
            json.addProperty("commandConfig", PathUtils.toRelative(testCase.getCommandConfigFile()));
        if (testCase.getCommandDebugFile() != null)
            json.addProperty("commandDebug", PathUtils.toRelative(testCase.getCommandDebugFile()));
        if (testCase.getBreakpointPath() != null)
            json.addProperty("breakPoint", PathUtils.toRelative(testCase.getBreakpointPath()));
        if (testCase.getDebugExecutableFile() != null)
            json.addProperty("debugExecutable", PathUtils.toRelative(testCase.getDebugExecutableFile()));
        if (testCase.getExecutionResultFile() != null)
            json.addProperty("executionResult", PathUtils.toRelative(testCase.getExecutionResultFile()));
        if (testCase.getCreationDateTime() != null)
            json.addProperty("creationDate", PathUtils.toRelative(testCase.getCreationDateTime().toString()));

        TestCaseDataExporter exporter = new TestCaseDataExporter();
//        JsonElement rootDataNode = exporter.exportToJsonElement(testCase.getRootDataNode());
        JsonElement rootDataNode = exporter.exportToJsonElement(testCase);
        json.add("rootDataNode", rootDataNode);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(json);
        Utils.writeContentToFile(jsonString, testCase.getPath());

        // save the real type of template function
        if (testCase.getFunctionNode() instanceof IFunctionNode && testCase.getFunctionNode().isTemplate()
                && new File(testCase.getPath()).getName().startsWith(ITestCase.PROTOTYPE_SIGNAL)) {
            String realTypeFile = testCase.getFunctionNode().getTemplateFilePath();
            PrototypeOfFunction obj = new PrototypeOfFunction();

            if (new File(realTypeFile).exists()) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson customGson = gsonBuilder.create();
                String jsonStr = Utils.readFileContent(realTypeFile);
                obj = customGson.fromJson(jsonStr, PrototypeOfFunction.class);
            }

            if (obj != null) {
                // remove deleted prototypes
                for (int i = obj.getPrototypes().size() - 1; i >= 0; i--)
                    if (!(new File(obj.getPrototypes().get(i)).exists()))
                        obj.getPrototypes().remove(i);

                // save
                if (!obj.getPrototypes().contains(testCase.getPath()))
                    obj.getPrototypes().add(testCase.getPath());

                GsonBuilder builder = new GsonBuilder();
                gson = builder.setPrettyPrinting().create();
                String jsonStr = gson.toJson(obj, PrototypeOfFunction.class);
                Utils.writeContentToFile(jsonStr, realTypeFile);
            }
        }
    }

    public static void removeBasicTestCase(String name) {
        TestCase testCase = getBasicTestCaseByName(name);
        if (testCase != null) {
            testCase.deleteOldData();
            nameToBasicTestCaseMap.remove(name);

            ICommonFunctionNode functionNode = testCase.getFunctionNode();
            functionToTestCasesMap.get(functionNode).remove(name);

//            File testCaseFile = new File(testCase.getPath());
//            if (testCaseFile.delete()) {
//                logger.debug("Delete successful test case: " + name);
//            } else {
//                logger.debug("Failed to delete test case: " + name);
//            }
        } else {
            logger.error("Test case not found. Name: " + name);
        }
    }

    public static void removeCompoundTestCase(String name) {
        CompoundTestCase compoundTestCase = getCompoundTestCaseByName(name);
        if (compoundTestCase != null) {
            compoundTestCase.deleteOldData();
            nameToCompoundTestCaseMap.remove(name);

//            File file = new File(compoundTestCase.getPath());
//            if (file.delete()) {
//                logger.debug("Delete successful test case: " + name);
//            } else {
//                logger.debug("Failed to delete test case: " + name);
//            }
        }
    }

    public static TestCase duplicateBasicTestCase(String name) {
        // load from disk
        TestCase clone = getBasicTestCaseByName(name);
        if (clone != null) {
            ICommonFunctionNode functionNode = clone.getRootDataNode().getFunctionNode();
            if (functionNode != null) {
                String testCaseName = functionNode.getSimpleName() + "." + new Random().nextInt(100000);
                clone.setName(testCaseName);
                exportBasicTestCaseToFile(clone);

                nameToBasicTestCaseMap.put(testCaseName, clone);
                List<String> testcaseNames = functionToTestCasesMap.get(functionNode);
                if (testcaseNames != null) {
                    testcaseNames.add(testCaseName);
                } else {
                    logger.error("Can not find list testcase names correspond to functionNode: " + functionNode.getAbsolutePath() + "when duplicate test case " + name);
                }

                return clone;
            } else {
                logger.error("The functionNode of test case is null");
            }
        }
        return null;
    }

    public static CompoundTestCase duplicateCompoundTestCase(String name) {
        // load from disk
        CompoundTestCase clone = getCompoundTestCaseByName(name);
        if (clone != null) {
            String[] strings = clone.getName().split("\\.");
            String str = strings[strings.length - 1];
            String newExtension = "." + new Random().nextInt(100000);
            String compoundTestCaseName = clone.getName().replace("." + str, newExtension);
            clone.setName(compoundTestCaseName);
            exportCompoundTestCaseToFile(clone);
            nameToCompoundTestCaseMap.put(compoundTestCaseName, clone);
            return clone;
        }
        return null;
    }

    /**
     * This method to check if the testcase file (or compound testcase) that has the name
     * exists or not
     *
     * @param name: name of the testcase (or compound testcase)
     * @return: return true if find out a testcase or a compound testcase in directories
     */
    public static boolean checkTestCaseExisted(String name) {
//        String testCaseDirectory = new WorkspaceConfig().fromJson().getTestcaseDirectory();
//        String compoundTestCaseDirectory = new WorkspaceConfig().fromJson().getCompoundTestcaseDirectory();
//        // check if the name is name of a compound testcase
//        File directory = new File(testCaseDirectory);
//        if (directory.exists() && directory.isDirectory()) {
//            for (File testCaseFile : Objects.requireNonNull(directory.listFiles())) {
//                if (testCaseFile.getName().equals(name + ".json")) { // testcase.001.json
//                    return true;
//                }
//            }
//            // if not found, maybe the name is name of a compound testcase, so continue to find in testcase directory
//        }
//
//        // check if the name is name of a testcase
//        directory = new File(compoundTestCaseDirectory);
//        if (directory.exists() && directory.isDirectory()) {
//            for (File compoundTCFile : Objects.requireNonNull(directory.listFiles())) {
//                if (compoundTCFile.getName().equals(name + ".json")) { // testcase.001.json
//                    return true;
//                }
//            }
//        }
//
//        // still not found, so the testcase file is not existed
//        return false;
        return nameToBasicTestCaseMap.containsKey(name);
    }

    public static List<TestCase> getTestCasesByFunction(ICommonFunctionNode functionNode) {
        List<TestCase> testCases = new ArrayList<>();
        List<String> names = functionToTestCasesMap.get(functionNode);
        for (String name : names) {
            testCases.add(getBasicTestCaseByName(name));
        }
        return testCases;
    }

    public static Map<ICommonFunctionNode, List<String>> getFunctionToTestCasesMap() {
        return functionToTestCasesMap;
    }

    public static Map<String, CompoundTestCase> getNameToCompoundTestCaseMap() {
        return nameToCompoundTestCaseMap;
    }

    public static Map<String, TestCase> getNameToBasicTestCaseMap() {
        return nameToBasicTestCaseMap;
    }
}

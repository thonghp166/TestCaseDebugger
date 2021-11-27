package com.dse.guifx_v3.controllers.build_environment;

import com.dse.compiler.gui.SrcResolverController;
import com.dse.config.AkaConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.WorkspaceCreation;
import com.dse.environment.object.*;
import com.dse.guifx_v3.controllers.main_view.BaseSceneController;
import com.dse.guifx_v3.controllers.main_view.MenuBarController;
import com.dse.guifx_v3.controllers.object.build_environment.Step;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.IProjectNode;
import com.dse.parser.object.SourcecodeFileNode;
import com.dse.project_init.ProjectClone;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.MacroFunctionNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testcasescript.TestcaseSearch;
import com.dse.testcasescript.object.*;
import com.dse.util.AkaLogger;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BaseController implements Initializable {
    final static AkaLogger logger = AkaLogger.get(BaseController.class);
    /**
     * Singleton patern
     */
    private static BaseController baseController = null;
    private static Scene baseScene = null;
    private int currentStep = CHOOSE_COMPILER_WINDOW_INDEX;
    private static Map<Integer, Step> map = new HashMap<>();

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/envbuilding/Base.fxml"));
        try {
            baseScene = new Scene(loader.load());
            baseController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Scene getBaseScene() {
        if (baseScene == null) prepare();
        return baseScene;
    }

    public static BaseController getBaseController() {
        if (baseController == null) prepare();
        return baseController;
    }

    @FXML
    private Button bBack;
    @FXML
    private Button bNext;
    @FXML
    private Button bBuild;
    @FXML
    private Label labelChooseCompiler;
    @FXML
    private Label labelNameEnvironment;
    @FXML
    private Label labelTestingMethod;
    @FXML
    private Label labelBuildOptions;
    @FXML
    private Label labelLocateSourceFiles;
    @FXML
    private Label labelChooseUUT;
    @FXML
    private Label labelUserCode;
    @FXML
    private Label labelSummary;
    @FXML
    private Label title;
    @FXML
    private SplitPane inputWindow;

    public void initialize(URL location, ResourceBundle resources) {
        map.put(CHOOSE_COMPILER_WINDOW_INDEX, new Step(labelChooseCompiler, Step.CHOOSE_COMPILER));
        map.put(NAME_ENVIRONMENT_WINDOW_INDEX, new Step(labelNameEnvironment, Step.NAME_ENVIRONMENT));
        map.put(TESTING_METHOD_WINDOW_INDEX, new Step(labelTestingMethod, Step.TESTING_METHOD));
        map.put(BUILDING_OPTIONS_WINDOW_INDEX, new Step(labelBuildOptions, Step.BUILD_OPTIONS));
        map.put(LOCATE_SOURCE_CODE_FILE_WINDOW_INDEX, new Step(labelLocateSourceFiles, Step.LOCATE_SOURCE_FILES));
        map.put(CHOOSE_UUTS_AND_STUB_WINDOW_INDEX, new Step(labelChooseUUT, Step.CHOOSE_UUT));
        map.put(USER_CODE_WINDOW_INDEX, new Step(labelUserCode, Step.USER_CODE));
        map.put(SUMMARY_WINDOW_INDEX, new Step(labelSummary, Step.SUMARY));

        setCurrentStep(CHOOSE_COMPILER_WINDOW_INDEX);
    }

    public static void resetStatesInAllWindows() {
        logger.debug("Reset all states in all windows in environment builder");
        prepare();

        Environment.WindowState.isSearchListNodeUpdated(false);
    }

    private void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;

        if (this.currentStep == CHOOSE_COMPILER_WINDOW_INDEX) bBack.setDisable(true);
        else bBack.setDisable(false);

        if (this.currentStep == SUMMARY_WINDOW_INDEX) {
            bNext.setDisable(true);
            bBuild.setDisable(false);
        } else {
            bNext.setDisable(false);
            bBuild.setDisable(true);
        }

        selectStep();
    }

    public static void loadEnvironment() {
        for (Step step : map.values()) {
            step.getController().loadFromEnvironment();
        }
    }

    public void save() {
        Step step = map.get(currentStep);
        if (step != null) {
            step.getController().save();
        }
    }

    public void next() {
        if (currentStep == map.size()) return;
        save();
        deSelectStep();
        setCurrentStep(++currentStep);
        selectStep();
    }

    public void back() {
        if (currentStep == CHOOSE_COMPILER_WINDOW_INDEX) return;
        deSelectStep();
        setCurrentStep(--currentStep);
        selectStep();
    }

    private void selectStep() {
        Step step = map.get(currentStep);
        if (step != null) {
            title.setText(step.getLabel().getText());
            step.getLabel().setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: white");

            inputWindow.getItems().clear();
            AnchorPane anchorPane = step.getAnchorPane();
            inputWindow.getItems().add(anchorPane);
        }
    }

    private void deSelectStep() {
        Step step = map.get(currentStep);
        if (step != null) {
            step.getLabel().setStyle("-fx-border-color: Grey; -fx-border-width: 1; -fx-background-color: white");
        }
    }

    public void cancel() {
        resetStatesInAllWindows();
        Environment.restoreEnvironment();
        UIController.getEnvironmentBuilderStage().close();
    }

    public void updateChooseUUT() {
        Step stepChooseUUT = map.get(CHOOSE_UUTS_AND_STUB_WINDOW_INDEX);
//        ((ChooseUUTController) stepChooseUUT.getController()).update(new Environment());
        ((ChooseUUTController) stepChooseUUT.getController()).update();
    }

    public void updateSummary() {
        Step stepSummary = map.get(SUMMARY_WINDOW_INDEX);
        ((SummaryController) stepSummary.getController()).update();
    }
//    /**
//     * When we set a new name, we need to update configuration of the old workspace.
//     *
//     * For example:
//     * {old_name}.env -> {new.name}.env
//     * {old_name}.tst -> {new.name}.tst
//     * etc.
//     * @param newEnvName
//     */
//    private void changeTheNameOfWorkspace(String newEnvName) {
//        // rename the environment file .env
//        String oldEnvFile = new WorkspaceConfig().fromJson().getEnvironmentFile();
//        String newEnvFile = new File(oldEnvFile).getParent() + File.separator + newEnvName + ".env";
//        new File(oldEnvFile).renameTo(new File(newEnvFile));
//
//        // rename the environment file .tst
//        String oldTstFile = new WorkspaceConfig().fromJson().getTestscriptFile();
//        String newTstFile = new File(oldTstFile).getParent() + File.separator + newEnvName + ".tst";
//        new File(oldTstFile).renameTo(new File(newTstFile));
//
//        // create new path of the workspace
//        String oldWorkspacePath = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
//        String newWorkspacePath = new File(oldWorkspacePath).getParent() + File.separator + newEnvName;
//
//
//        // rename path in test case
//        List<String> allTestcasePaths = Utils.getAllFiles(new AkaConfig().fromJson().getWorkingDirectory());
//        for (String testcasePath: allTestcasePaths){
//            String contentTestcase = Utils.readFileContent(testcasePath);
//            contentTestcase = contentTestcase.replace(oldWorkspacePath, newWorkspacePath);
//            Utils.writeContentToFile(contentTestcase, testcasePath);
//        }
//
//        // update workspace.aka
//        String workspaceConfig = new AkaConfig().fromJson().getOpenWorkspaceConfig();
//        String content = Utils.readFileContent(workspaceConfig);
//        content = content.replace(oldWorkspacePath, newWorkspacePath);
//        Utils.writeContentToFile(content, workspaceConfig);
//        new WorkspaceConfig().fromJson().setCurrentEnvironmentName(newEnvName).exportToJson();
//
//        // rename the workspace
//        new File(oldWorkspacePath).renameTo(new File(newWorkspacePath));
//
//        // update history in aka config file
//        AkaConfig akaConfig = new AkaConfig().fromJson();
//        akaConfig.getRecentEnvironments().add(newEnvFile);
//        String newWorkspace = new File(akaConfig.getOpeningWorkspaceDirectory()).getParent() + File.separator + newEnvFile;
//        akaConfig.setOpenWorkspaceConfig(newWorkspace + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME);
//        akaConfig.setOpeningWorkspaceDirectory(newWorkspace);
//        akaConfig.exportToJson();
//    }

    @FXML
    public void buildEnvironment(ActionEvent actionEvent) {
        logger.debug("Build the environment");

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (AbstractCustomController.ENVIRONMENT_STATUS == AbstractCustomController.STAGE.UPDATING_ENV_FROM_OPENING_ENV) {
                    Alert alert = UIController.showYesNoDialog(Alert.AlertType.INFORMATION, "Rebuild confirmation", "Rebuild confirmation", "This action will delete all test cases in deleted source code file. Do you want to continue?");
                    Optional<ButtonType> result = alert.showAndWait();

                    if (result.get().getText().toLowerCase().equals("yes")) {
                        int success = rebuildExistingEnvironment(actionEvent);
                        if (success != BUILD_NEW_ENVIRONMENT.SUCCESS.CREATE_ENVIRONMENT) {
                            switch (success) {
                                case BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION: {
                                    UIController.showErrorDialog("Could not rebuild the environment due to failed compilation", "Rebuild environment", "Fail");
                                    break;
                                }
                                case BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_ENV_FILE:
                                case BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_TST_FILE: {
                                    UIController.showErrorDialog("Could not rebuild the environment because the name of the new environment exists", "Rebuild environment", "Fail");
                                    break;
                                }
                                case BUILD_NEW_ENVIRONMENT.FAILURE.OTHER: {
                                    UIController.showErrorDialog("Could not rebuild the environment due to unexpected error", "Rebuild environment", "Fail");
                                    break;
                                }
                            }
                        } else
                            UIController.showSuccessDialog("Rebuild the environment successfully", "Rebuild environment", "Success");
                    } else {
                        // nothing to do
                    }

                } else {
                    int success = buildNewEnvironment(actionEvent);
                    if (success != BUILD_NEW_ENVIRONMENT.SUCCESS.CREATE_ENVIRONMENT) {
                        switch (success) {
                            case BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION: {
                                UIController.showErrorDialog("Could not build the environment due to failed compilation", "Build environment", "Fail");
                                break;
                            }
                            case BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_ENV_FILE:
                            case BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_TST_FILE: {
                                UIController.showErrorDialog("Could not build the environment because the name of the new environment exists", "Build environment", "Fail");
                                break;
                            }
                            case BUILD_NEW_ENVIRONMENT.FAILURE.OTHER: {
                                UIController.showErrorDialog("Could not build the environment due to unexpected error", "Build environment", "Fail");
                                break;
                            }
                        }
                    } else {
//                        new AkaConfig().fromJson().setOpeningWorkspaceDirectory(
//                                new AkaConfig().fromJson().getWorkingDirectory() + File.separator + Environment.getInstance().getName()).exportToJson();
//                        new AkaConfig().fromJson().setOpenWorkspaceConfig(
//                                new AkaConfig().fromJson().getWorkingDirectory() + File.separator + Environment.getInstance().getName() + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME).exportToJson();
                        UIController.showSuccessDialog("Build the environment successfully", "Build environment", "Success");
                    }
                }
                MenuBarController.getMenuBarController().refresh();
            }
        });
    }

    private void updateNameOfEnv(){
        // STEP: update name of the environment
        String newName = Environment.getInstance().getName();
        String oldName = Environment.getBackupEnvironment().getName();
        if (!newName.equals(oldName)){
            EnviroNameNode envNameNode = (EnviroNameNode) EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroNameNode()).get(0);
            envNameNode.setName(newName);
        }
    }

    private int rebuildExistingEnvironment(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null)
            return BUILD_NEW_ENVIRONMENT.FAILURE.OTHER;

        // STEP: compile all
        int isCompiledSuccessfully = compileTheTestedProject(Environment.getInstance().getProjectNode());
        if (isCompiledSuccessfully != BUILD_NEW_ENVIRONMENT.SUCCESS.COMPILATION) {
            return isCompiledSuccessfully;
        }
        logger.debug("The project " + Environment.getInstance().getProjectNode().getAbsolutePath() + " is compiled successfully");

        // STEP: delete all related folders when environment is updated
        // Then export physical_tree.json, dependencies, elements, etc. to initialized working space
        WorkspaceConfig wkConfig = new WorkspaceConfig().fromJson();
        Utils.deleteFileOrFolder(new File(wkConfig.getDependencyDirectory()));
        Utils.deleteFileOrFolder(new File(wkConfig.getPhysicalJsonFile()));
        Utils.deleteFileOrFolder(new File(wkConfig.getElementDirectory()));
        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
        addAnalysisInformationToWorkspace(workspace,
                wkConfig.getDependencyDirectory(),
                wkConfig.getPhysicalJsonFile(),
                wkConfig.getElementDirectory());

        // STEP: update env file
        File envFile = new File(new WorkspaceConfig().fromJson().getEnvironmentFile());
        EnvironmentRootNode envRoot = Environment.getInstance().getEnvironmentRootNode();
        int exportedEnvironmentDone = updateEnvFileWhenRebuilding(envFile, envRoot);
        if (exportedEnvironmentDone != BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_ENV_FILE) {
            return exportedEnvironmentDone;
        }

        // STEP: update tst file
        TestcaseRootNode updatedTstRoot = Environment.getBackupEnvironment().getTestcaseScriptRootNode();
        String tstFile = new WorkspaceConfig().fromJson().getTestscriptFile();
        int exportTestscriptDone = updateTestcaseScriptWhenRebuilding(updatedTstRoot, envRoot, tstFile);
        if (exportTestscriptDone != BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_TST_FILE) {
            return exportTestscriptDone;
        }

//        // STEP: update name of the environment
//        String newName = Environment.getInstance().getNameOfEnvironment();
//        String oldName = Environment.getBackupEnvironment().getNameOfEnvironment();
//        if (!newName.equals(oldName)){
//            changeTheNameOfWorkspace(Environment.getInstance().getNameOfEnvironment());
//        }

        // OTHERS
        updateRemaining(envFile, actionEvent);

        return BUILD_NEW_ENVIRONMENT.SUCCESS.CREATE_ENVIRONMENT;
    }

    public static class BUILD_NEW_ENVIRONMENT {
        public static class SUCCESS {
            public final static int EXPORT_ENV_FILE = 0;
            public final static int EXPORT_TST_FILE = 1;
            public final static int CREATE_ENVIRONMENT = 2;
            public final static int COMPILATION = 3;
        }

        public  static class FAILURE {
            public final static int OTHER = 12;
            public final static int DUPLICATE_ENV_FILE = 10;
            public final static int DUPLICATE_TST_FILE = 11;
            public final static int COMPILATION = 13;
        }
    }

    private int buildNewEnvironment(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null)
            return BUILD_NEW_ENVIRONMENT.FAILURE.OTHER;

        /**
         * Point the current workspace to the creating workspace.
         *
         * new WorkspaceConfig().fromJson() will return the configuration of creating workspace
         */
        saveCreatingWorkspaceToAkaConfig();

        // initialize workspace (just create necessary folder) FIRSTLY (mandatory)
//        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
        File project = new File(Environment.getInstance().getProjectNode().getAbsolutePath());
        String workspace = ".." /*project.getParent()*/ + File.separator + "aka-working-space"
                + File.separator + Environment.getInstance().getName();
        try {
            initializeRelativeWorkspace(workspace);
        } catch (IOException e) {
            e.printStackTrace();
            return BUILD_NEW_ENVIRONMENT.FAILURE.OTHER;
        }

        // compile all
        int isCompiledSuccessfully = compileTheTestedProject(Environment.getInstance().getProjectNode());
        if (isCompiledSuccessfully != BUILD_NEW_ENVIRONMENT.SUCCESS.COMPILATION) {
            return isCompiledSuccessfully;
        }
        logger.debug("The project " + Environment.getInstance().getProjectNode().getAbsolutePath() + " is compiled successfully");

        // export physical_tree.json, dependencies, elements, etc. to initialized working space
        addAnalysisInformationToWorkspace(workspace,
                new WorkspaceConfig().fromJson().getDependencyDirectory(),
                new WorkspaceConfig().fromJson().getPhysicalJsonFile(),
                new WorkspaceConfig().fromJson().getElementDirectory());

        File envFile = new File(new WorkspaceConfig().fromJson().getEnvironmentFile());
        EnvironmentRootNode envRoot = Environment.getInstance().getEnvironmentRootNode();
        int exportedEnvironmentDone = exportEnvironmentToFile(envFile, envRoot);
        if (exportedEnvironmentDone != BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_ENV_FILE) {
            return exportedEnvironmentDone;
        }

        File tstFile = new File(new WorkspaceConfig().fromJson().getTestscriptFile());
        int exportTestscriptDone = exportTestscriptToFile(tstFile, envRoot);
        if (exportTestscriptDone == BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_TST_FILE) {
            updateRemaining(envFile, actionEvent);
            return BUILD_NEW_ENVIRONMENT.SUCCESS.CREATE_ENVIRONMENT;
        } else
            return exportTestscriptDone;
    }

    public static void addAnalysisInformationToWorkspace(String newWorkspace, String dependencyFolder, String physicalFile, String elementFolder) {
        // create workspace
        WorkspaceCreation wk = new WorkspaceCreation();
        wk.setWorkspace(newWorkspace);
        wk.setDependenciesFolder(dependencyFolder);
        wk.setElementFolder(elementFolder);
        wk.setPhysicalTreePath(physicalFile);
        wk.setRoot(Environment.getInstance().getProjectNode());
        wk.create(wk.getRoot(), wk.getElementFolder(), wk.getDependenciesFolder(), wk.getPhysicalTreePath());
    }

    private void updateRemaining(File envFile, ActionEvent actionEvent) {
//        setIgnoredFoldersInProjectTree();

        // Clone project
//        new ProjectClone.CloneThread().start();
//        Platform.runLater();

//        new SystemHeaderParser.InitThread().start();
//        Platform.runLater();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(new ProjectClone.CloneThread());
//        executorService.submit(new SystemHeaderParser.InitThread());

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);

            // create workspace
            WorkspaceCreation wk = new WorkspaceCreation();
            wk.setWorkspace(new AkaConfig().fromJson().getOpeningWorkspaceDirectory());
            wk.setDependenciesFolder(new WorkspaceConfig().fromJson().getDependencyDirectory());
            wk.setElementFolder(new WorkspaceConfig().fromJson().getElementDirectory());
            wk.setPhysicalTreePath(new WorkspaceConfig().fromJson().getPhysicalJsonFile());
            wk.setRoot(Environment.getInstance().getProjectNode());
            wk.create(wk.getRoot(), wk.getElementFolder(), wk.getDependenciesFolder(), wk.getPhysicalTreePath());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //
        closeTheEnvironmentBuilderWindow(actionEvent);

        // clear every thing (MDIWindows, messange windows)
        UIController.clear();
        resetStatesInAllWindows();

        File testcasesScriptFile = new File(new WorkspaceConfig().fromJson().getTestscriptFile());
        loadTestCasesNavigator(testcasesScriptFile);

        TestCaseManager.clearMaps();
        TestCaseManager.initializeMaps();

        // update information displaying on bottom of screen
        BaseSceneController.getBaseSceneController().updateInformation();

        loadProjectStructureTree();

        MenuBarController.addEnvironmentToHistory(envFile);
    }

//    private void setIgnoredFoldersInProjectTree() {
//        List<INode> sourcecodeNodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());
//        List<IEnvironmentNode> ignoredNodes = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroIgnoreNode());
//
//        for (int i = sourcecodeNodes.size() - 1; i >= 0; i--) {
//            INode sourcodeFileNode = sourcecodeNodes.get(i);
//
//            if (sourcodeFileNode instanceof SourcecodeFileNode) {
//                boolean isIgnoredWhenConfiguringEnv = false;
//
//                for (IEnvironmentNode ignoredNode : ignoredNodes)
//
//                    if (ignoredNode instanceof EnviroIgnoreNode &&
//                            ((EnviroIgnoreNode) ignoredNode).getName().equals(sourcodeFileNode.getAbsolutePath())) {
//                        isIgnoredWhenConfiguringEnv = true;
//                        break;
//                    }
//                if (isIgnoredWhenConfiguringEnv) {
//                    sourcodeFileNode.getParent().getChildren().remove(sourcodeFileNode);
//                }
//            }
//        }
//    }

    private AkaConfig saveCreatingWorkspaceToAkaConfig() {
        AkaConfig akaConfig = new AkaConfig().fromJson();
        akaConfig.setOpeningWorkspaceDirectory(akaConfig.getWorkingDirectory() + File.separator + Environment.getInstance().getName());
        akaConfig.setOpenWorkspaceConfig(akaConfig.getWorkingDirectory() + File.separator + Environment.getInstance().getName() + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME);
        akaConfig.exportToJson();
        return akaConfig;
    }

    private WorkspaceConfig initializeRelativeWorkspace(String workspace) throws IOException {
        String testingProject = "../../" + Environment.getInstance().getProjectNode().getName();

        WorkspaceConfig wkConfig = new WorkspaceConfig().fromJson();

        // save .tst and .env out of workspace
        wkConfig.setEnvironmentFile(new File(workspace).getParent() + File.separator + Environment.getInstance().getName() + ".env");
        wkConfig.setTestscriptFile(new File(workspace).getParent() + File.separator + Environment.getInstance().getName() + ".tst");

        // initialize folders in workspace
        wkConfig.setPhysicalJsonFile(workspace + File.separator + WorkspaceConfig.PHYSICAL_JSON_NAME);
        wkConfig.setProbePointDirectory(workspace + File.separator + WorkspaceConfig.PROBE_POINT_FOLDER_NAME);
        wkConfig.setRegressionScriptDirectory(workspace + File.separator + WorkspaceConfig.REGRESSION_SCRIPT_FOLDER_NAME);
        wkConfig.setCompoundTestcaseDirectory(workspace + File.separator + WorkspaceConfig.COMPOUND_FOLDER_NAME);
        wkConfig.setDependencyDirectory(workspace + File.separator + WorkspaceConfig.DEPENDENCY_FOLDER_NAME);
        wkConfig.setTestcaseDirectory(workspace + File.separator + WorkspaceConfig.TESTCASE_FOLDER_NAME);
        wkConfig.setTestingProject(testingProject);
        wkConfig.setCurrentEnvironmentName(Environment.getInstance().getName());
        wkConfig.setCommandFile(workspace + File.separator + WorkspaceConfig.COMPILATION_COMMAND_FILE_NAME);
        wkConfig.setTestcaseCommandsDirectory(workspace + File.separator + WorkspaceConfig.TEST_CASE_COMMAND_NAME);
        wkConfig.setDebugDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_NAME);
        wkConfig.setDebugCommandsDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_COMMAND_NAME);
        wkConfig.setDebugLogDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_LOG_NAME);
        wkConfig.setBreakpointDirectory(workspace + File.separator + WorkspaceConfig.BREAKPOINT_FOLDER_NAME);
        wkConfig.setTestpathDirectory(workspace + File.separator + WorkspaceConfig.TEST_PATH_FOLDER_NAME);
        wkConfig.setCoverageDirectory(workspace + File.separator + WorkspaceConfig.COVERAGE_FOLDER_NAME);
        wkConfig.setExecutableFolderDirectory(workspace + File.separator + WorkspaceConfig.EXECUTABLE_FOLDER_NAME);
        wkConfig.setFunctionConfigDirectory(workspace + File.separator + WorkspaceConfig.FUNCTION_CONFIG_FOLDER_NAME);
        wkConfig.setReportDirectory(workspace + File.separator + WorkspaceConfig.REPORT_FOLDER_NAME);
        wkConfig.setFullReportDirectory(workspace + File.separator + WorkspaceConfig.FULL_REPORT_FOLDER_NAME);
        wkConfig.setExecutionReportDirectory(workspace + File.separator + WorkspaceConfig.EXECUTION_REPORT_FOLDER_NAME);
        wkConfig.setTestDriverDirectory(workspace + File.separator + WorkspaceConfig.TEST_DRIVER_FOLDER_NAME);
        wkConfig.setExecutionResultDirectory(workspace + File.separator + WorkspaceConfig.EXECUTION_RESULT_FOLDER_NAME);
        wkConfig.setTestDataReportDirectory(workspace + File.separator + WorkspaceConfig.TEST_DATA_REPORT_FOLDER_NAME);
        wkConfig.setStubCodeDirectory(workspace + File.separator + WorkspaceConfig.STUB_CODE_FOLDER_NAME);
        wkConfig.setElementDirectory(workspace + File.separator + WorkspaceConfig.ELEMENT_FOLDER_NAME);
        wkConfig.setVersionComparisonDirectory(workspace + File.separator + WorkspaceConfig.VERSION_COMPARISON_FOLDER_NAME);
        wkConfig.setTemplateFunctionDirectory(workspace + File.separator + WorkspaceConfig.TEMPLATE_FUNCTION_FOLDER_NAME);
        wkConfig.setBoundOfDataTypeFile(workspace + File.separator + WorkspaceConfig.BOUND_OF_DATA_TYPES);
        wkConfig.setHeaderPreprocessorDirectory(workspace + File.separator + WorkspaceConfig.HEADER_PREPROCESSOR_FOLDER_NAME);
        wkConfig.setConstraintFolder(workspace + File.separator + WorkspaceConfig.CONSTRAINTS_FOLDER_NAME);

        String workspaceConfig = new File(workspace).getCanonicalPath() + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME;
        File project = new File(Environment.getInstance().getProjectNode().getAbsolutePath());
        workspace = project.getParent() + File.separator + "aka-working-space"
                + File.separator + Environment.getInstance().getName();
        workspaceConfig = workspace
                + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME;

        AkaConfig akaConfig = new AkaConfig().fromJson();
        akaConfig.setOpenWorkspaceConfig(workspaceConfig);
        akaConfig.setOpeningWorkspaceDirectory(workspace);
        akaConfig.exportToJson();

        wkConfig.exportToJson(workspaceConfig);
        return wkConfig;
    }


    private WorkspaceConfig initializeWorkspace(String workspace) {
        WorkspaceConfig wkConfig = new WorkspaceConfig().fromJson();

        // save .tst and .env out of workspace
        wkConfig.setEnvironmentFile(new File(workspace).getParent() + File.separator + Environment.getInstance().getName() + ".env");
        wkConfig.setTestscriptFile(new File(workspace).getParent() + File.separator + Environment.getInstance().getName() + ".tst");

        // initialize folders in workspace
        wkConfig.setPhysicalJsonFile(workspace + File.separator + WorkspaceConfig.PHYSICAL_JSON_NAME);
        wkConfig.setProbePointDirectory(workspace + File.separator + WorkspaceConfig.PROBE_POINT_FOLDER_NAME);
        wkConfig.setRegressionScriptDirectory(workspace + File.separator + WorkspaceConfig.REGRESSION_SCRIPT_FOLDER_NAME);
        wkConfig.setCompoundTestcaseDirectory(workspace + File.separator + WorkspaceConfig.COMPOUND_FOLDER_NAME);
        wkConfig.setDependencyDirectory(workspace + File.separator + WorkspaceConfig.DEPENDENCY_FOLDER_NAME);
        wkConfig.setTestcaseDirectory(workspace + File.separator + WorkspaceConfig.TESTCASE_FOLDER_NAME);
        wkConfig.setTestingProject(Environment.getInstance().getProjectNode().getAbsolutePath());
        wkConfig.setCurrentEnvironmentName(Environment.getInstance().getName());
        wkConfig.setCommandFile(workspace + File.separator + WorkspaceConfig.COMPILATION_COMMAND_FILE_NAME);
        wkConfig.setTestcaseCommandsDirectory(workspace + File.separator + WorkspaceConfig.TEST_CASE_COMMAND_NAME);
        wkConfig.setDebugDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_NAME);
        wkConfig.setDebugCommandsDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_COMMAND_NAME);
        wkConfig.setDebugLogDirectory(workspace + File.separator + WorkspaceConfig.DEBUG_LOG_NAME);
        wkConfig.setBreakpointDirectory(workspace + File.separator + WorkspaceConfig.BREAKPOINT_FOLDER_NAME);
        wkConfig.setTestpathDirectory(workspace + File.separator + WorkspaceConfig.TEST_PATH_FOLDER_NAME);
        wkConfig.setCoverageDirectory(workspace + File.separator + WorkspaceConfig.COVERAGE_FOLDER_NAME);
        wkConfig.setExecutableFolderDirectory(workspace + File.separator + WorkspaceConfig.EXECUTABLE_FOLDER_NAME);
        wkConfig.setFunctionConfigDirectory(workspace + File.separator + WorkspaceConfig.FUNCTION_CONFIG_FOLDER_NAME);
        wkConfig.setReportDirectory(workspace + File.separator + WorkspaceConfig.REPORT_FOLDER_NAME);
        wkConfig.setFullReportDirectory(workspace + File.separator + WorkspaceConfig.FULL_REPORT_FOLDER_NAME);
        wkConfig.setExecutionReportDirectory(workspace + File.separator + WorkspaceConfig.EXECUTION_REPORT_FOLDER_NAME);
        wkConfig.setTestDriverDirectory(workspace + File.separator + WorkspaceConfig.TEST_DRIVER_FOLDER_NAME);
        wkConfig.setExecutionResultDirectory(workspace + File.separator + WorkspaceConfig.EXECUTION_RESULT_FOLDER_NAME);
        wkConfig.setTestDataReportDirectory(workspace + File.separator + WorkspaceConfig.TEST_DATA_REPORT_FOLDER_NAME);
        wkConfig.setStubCodeDirectory(workspace + File.separator + WorkspaceConfig.STUB_CODE_FOLDER_NAME);
        wkConfig.setElementDirectory(workspace + File.separator + WorkspaceConfig.ELEMENT_FOLDER_NAME);
        wkConfig.setVersionComparisonDirectory(workspace + File.separator + WorkspaceConfig.VERSION_COMPARISON_FOLDER_NAME);
        wkConfig.setTemplateFunctionDirectory(workspace + File.separator + WorkspaceConfig.TEMPLATE_FUNCTION_FOLDER_NAME);
        wkConfig.setBoundOfDataTypeFile(workspace + File.separator + WorkspaceConfig.BOUND_OF_DATA_TYPES);
        wkConfig.setHeaderPreprocessorDirectory(workspace + File.separator + WorkspaceConfig.HEADER_PREPROCESSOR_FOLDER_NAME);
        wkConfig.setConstraintFolder(workspace + File.separator + WorkspaceConfig.CONSTRAINTS_FOLDER_NAME);

        String workspaceConfig = workspace + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME;
        wkConfig.exportToJson(workspaceConfig);
        return wkConfig;
    }
    private void closeTheEnvironmentBuilderWindow(ActionEvent actionEvent) {
//         if there is no error, move to the main window
        final Node source = (Node) actionEvent.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private int exportTestscriptToFile(File testscriptFile, EnvironmentRootNode envRoot) {
        logger.debug("Exporting test script to file");

        // check the existence of the test script file
        if (AbstractCustomController.ENVIRONMENT_STATUS == AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_OPENING_GUI
                || AbstractCustomController.ENVIRONMENT_STATUS == AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_BLANK_GUI)
            if (testscriptFile.exists()) {
                logger.error("The test script file " + testscriptFile.getAbsolutePath() + " exists! Check it again.");
                return BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_TST_FILE;
            }

        /**
         * Create a test case script
         */
        String relativePath = PathUtils.toRelative(testscriptFile.getAbsolutePath());
        new WorkspaceConfig().fromJson().setTestscriptFile(relativePath).exportToJson();
        TestcaseRootNode testCaseRoot = new TestcaseRootNode();

        // <<COMPOUND>>
        TestCompoundSubprogramNode compoundNode = new TestCompoundSubprogramNode();
        testCaseRoot.addChild(compoundNode);

        // <<INIT>>
        TestInitSubprogramNode initNode = new TestInitSubprogramNode();
        testCaseRoot.addChild(initNode);

        // <<SBF>> and <<UUT>>
        List<IEnvironmentNode> allSourceCodeNodes = new ArrayList<>();
        List<IEnvironmentNode> uutNodes = EnvironmentSearch.searchNode(envRoot, new EnviroUUTNode());
        allSourceCodeNodes.addAll(uutNodes);
        List<IEnvironmentNode> sbfNodes = EnvironmentSearch.searchNode(envRoot, new EnviroSBFNode());
        allSourceCodeNodes.addAll(sbfNodes);

        for (IEnvironmentNode scNode : allSourceCodeNodes) {
            TestUnitNode unitNode = new TestUnitNode();

            if (scNode instanceof EnviroUUTNode) {
                unitNode.setName(((EnviroUUTNode) scNode).getName());
            } else if (scNode instanceof EnviroSBFNode) {
                unitNode.setName(((EnviroSBFNode) scNode).getName());
            }

            testCaseRoot.addChild(unitNode);

            // find the source code file node corresponding to the absolute path
            INode matchedSourcecodeNode = null;
            List<INode> sourcecodeNodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());
            for (INode sourcecodeNode : sourcecodeNodes)
                if (sourcecodeNode instanceof SourcecodeFileNode)
                    if (sourcecodeNode.getAbsolutePath().equals(unitNode.getName())) {
                        matchedSourcecodeNode = sourcecodeNode;
                        break;
                    }

            // add subprograms
            if (matchedSourcecodeNode != null) {
                List<INode> children = Search.searchNodes(matchedSourcecodeNode, new AbstractFunctionNodeCondition());
                children.addAll(Search.searchNodes(matchedSourcecodeNode, new MacroFunctionNodeCondition()));

                for (INode function : children) {
                    if (function instanceof ICommonFunctionNode) {
                        TestNormalSubprogramNode newSubprogram = new TestNormalSubprogramNode();
                        newSubprogram.setName(function.getAbsolutePath());
                        unitNode.addChild(newSubprogram);
                    }
                }
            }

        }
        String content = testCaseRoot.exportToFile();

        // export test script to file
        try {
            FileWriter writer = new FileWriter(testscriptFile);
            writer.write(content);
            writer.close();
            Environment.getInstance().loadTestCasesScript(testscriptFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("The test script has been exported. Path = " + testscriptFile.getAbsolutePath());
        return BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_TST_FILE; // success
    }

    private int updateTestcaseScriptWhenRebuilding(TestcaseRootNode tstRoot, EnvironmentRootNode updatedEnvRoot,
                                                   String tstFile) {
        // get all uuts and stubs (after being updated)
        List<IEnvironmentNode> sbfNodes = EnvironmentSearch.searchNode(updatedEnvRoot, new EnviroSBFNode());
        List<IEnvironmentNode> uutNodes = EnvironmentSearch.searchNode(updatedEnvRoot, new EnviroUUTNode());
        sbfNodes.addAll(uutNodes);

        // get all unit nodes in tst
        List<ITestcaseNode> uutNodesInUpdatedTst = TestcaseSearch.searchNode(tstRoot, new TestUnitNode());

        Map<String, String> deletedTestcases = new HashMap<>();

        // Case 1: exist in environment tree, but not exist in tst
        for (IEnvironmentNode envNode : sbfNodes) {
            // get name of environment node
            String envNodeName = "";
            if (envNode instanceof EnviroSBFNode)
                envNodeName = ((EnviroSBFNode) envNode).getName();
            else if (envNode instanceof EnviroUUTNode)
                envNodeName = ((EnviroUUTNode) envNode).getName();

            if (envNodeName.equals(""))
                continue;
            handledAddedSourcecodeFileWhenUpdatingEnv(envNodeName, uutNodesInUpdatedTst, tstRoot);

        }

        // Case 2: deleted source code file when updating environment
        for (ITestcaseNode tstNode : uutNodesInUpdatedTst)
            if (tstNode instanceof TestUnitNode) {
                String tstName = ((TestUnitNode) tstNode).getName();
                handledDeletedSourcecodeFileWhenUpdatingEnv(tstName, sbfNodes, tstNode);
            }

        // save in file
        Utils.writeContentToFile(tstRoot.exportToFile(), new WorkspaceConfig().fromJson().getTestscriptFile());

        // save deleted node
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(deletedTestcases);
        Utils.writeContentToFile(json, new WorkspaceConfig().fromJson().getDeletedTestcaseWhenUpdatingEnv());
        return BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_TST_FILE;
    }

    private boolean handledDeletedSourcecodeFileWhenUpdatingEnv(String tstNodeName,
                                                                List<IEnvironmentNode> envNodes,
                                                                ITestcaseNode tstNode) {
        boolean existInEnvTree = false;
        for (IEnvironmentNode envNode : envNodes) {
            String envName = "";
            if (envNode instanceof EnviroUUTNode)
                envName = ((EnviroUUTNode) envNode).getName();
            else if (envNode instanceof EnviroSBFNode)
                envName = ((EnviroSBFNode) envNode).getName();
            if (envName.equals(tstNodeName)) {
                existInEnvTree = true;
                break;
            }
        }

        if (!existInEnvTree) {
            // we need to remove this node in tst tree
            ITestcaseNode root = tstNode.getParent();
            root.getChildren().remove(tstNode);

            for (ITestcaseNode child : TestcaseSearch.searchNode(tstNode, new TestNameNode()))
                if (child instanceof TestNameNode) {
                    logger.debug("Deleted: " + ((TestNameNode) child).getName() + " in " + tstNodeName);
                }
        }

        return false;
    }

    /**
     *
     * @param envNodeName
     * @param uutNodesInUpdatedTst
     * @param tstRoot
     * @return true if envNode is not in tst tree, and we can add it to tst tree
     */
    private boolean handledAddedSourcecodeFileWhenUpdatingEnv(String envNodeName,
                                                              List<ITestcaseNode> uutNodesInUpdatedTst,
                                                              TestcaseRootNode tstRoot) {
        // find a tst node corresponding to the env node
        ITestcaseNode matchingUut = null;
        for (ITestcaseNode uutNodeInTst : uutNodesInUpdatedTst)
            if (uutNodeInTst instanceof TestUnitNode)
                if (((TestUnitNode) uutNodeInTst).getName().equals(envNodeName)) {
                    matchingUut = uutNodeInTst;
                    break;
                }

        if (matchingUut == null) {
            // if we can not found a corresponding node in tst tree,
            // it means that we add new uut/sbf when updating env.
            // We need to add this node to tst tree.
            SourcecodeFileParser srcParser = new SourcecodeFileParser();
            try {
                INode rootSrc = srcParser.parseSourcecodeFile(new File(envNodeName));

                // add source code file to tst tree
                TestUnitNode unitNode = new TestUnitNode();
                unitNode.setName(envNodeName);
                unitNode.setParent(tstRoot);
                tstRoot.getChildren().add(unitNode);

                // add all functions to tst tree
                List<INode> children = Search.searchNodes(rootSrc, new AbstractFunctionNodeCondition());
                children.addAll(Search.searchNodes(rootSrc, new MacroFunctionNodeCondition()));

                for (INode child : children)
                    if (child instanceof ICommonFunctionNode) {
                        TestNormalSubprogramNode subprogramNode = new TestNormalSubprogramNode();
                        subprogramNode.setName(child.getAbsolutePath());
                        subprogramNode.setParent(unitNode);
                        unitNode.getChildren().add(subprogramNode);
                    }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }


    private int updateEnvFileWhenRebuilding(File environmentFile, EnvironmentRootNode root) {
        return exportEnvironmentToFile(environmentFile, root);
    }

    /**
     * Export a environment tree to file
     *
     * @return true if success, false if failed
     */
    private int exportEnvironmentToFile(File environmentFile, EnvironmentRootNode root) {
        logger.debug("Exporting the environment script to file.");

        // if we are creating new environment, but the environment exists before
        if (AbstractCustomController.ENVIRONMENT_STATUS == AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_BLANK_GUI
                || AbstractCustomController.ENVIRONMENT_STATUS == AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_OPENING_GUI)
            if (environmentFile.exists()) {
                logger.error("The environment file " + environmentFile.getAbsolutePath() + " exists! Check it again.");
                return BUILD_NEW_ENVIRONMENT.FAILURE.DUPLICATE_ENV_FILE;
            }

        // export the environment script to file in the working directory
        String envFileRelative = PathUtils.toRelative(environmentFile.getAbsolutePath());
        new WorkspaceConfig().fromJson().setEnvironmentFile(envFileRelative).exportToJson();
        try {
            FileWriter writer = new FileWriter(environmentFile);
            writer.write(root.exportToFile());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("The environment has been exported. Path = " + environmentFile.getAbsolutePath());
        return BUILD_NEW_ENVIRONMENT.SUCCESS.EXPORT_ENV_FILE;
    }

    private void loadTestCasesNavigator(File testCasesScript) {
        UIController.loadTestCasesNavigator(testCasesScript);
    }

    private void loadProjectStructureTree() {
        IProjectNode root = Environment.getInstance().getProjectNode();
        root.setName(Environment.getInstance().getName());
        UIController.loadProjectStructureTree(root);
    }

    public static int compileTheTestedProject(com.dse.parser.object.Node projectRootNode) {
        try {
            FXMLLoader loader = new FXMLLoader(BaseController.class.getResource("/FXML/envbuilding/SourceCodeResolver.fxml"));
            Parent root = loader.load();

            SrcResolverController controller = loader.getController();
            // set the directory where the linkage command is executed
            controller.setDirectory(Environment.getInstance().getProjectNode().getAbsolutePath());

            Stage sourceCodeResolverStage = new Stage();
            sourceCodeResolverStage.setTitle("Source code resolver");
            sourceCodeResolverStage.setResizable(false);
            sourceCodeResolverStage.setScene(new Scene(root));
            controller.setStage(sourceCodeResolverStage);

            // block the environment building window
            sourceCodeResolverStage.initModality(Modality.WINDOW_MODAL);
            sourceCodeResolverStage.initOwner(UIController.getEnvironmentBuilderStage().getScene().getWindow());

            // compile every source code file to find problem
            int foundProblem = controller.findProblemInTestedProject(projectRootNode);
            if (foundProblem == BaseController.BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION) {
                sourceCodeResolverStage.show();
                return BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION;
            } else {
//                UIController.showSuccessDialog("The project is compiled successfully.", "Compilation message", "Success");
                return BUILD_NEW_ENVIRONMENT.SUCCESS.COMPILATION;
            }

        } catch (Exception e) {
            logger.error("Error when compile the tested project " + projectRootNode.getAbsolutePath());
            UIController.showErrorDialog("The project is unable to compile successfully.", "Compilation message", "Unexpected error");
            e.printStackTrace();
            return BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION;
        }
    }

    private static final int CHOOSE_COMPILER_WINDOW_INDEX = 1;
    private static final int NAME_ENVIRONMENT_WINDOW_INDEX = 2;
    private static final int TESTING_METHOD_WINDOW_INDEX = 3;
    private static final int BUILDING_OPTIONS_WINDOW_INDEX = 4;
    private static final int LOCATE_SOURCE_CODE_FILE_WINDOW_INDEX = 5;
    private static final int CHOOSE_UUTS_AND_STUB_WINDOW_INDEX = 6;
    private static final int USER_CODE_WINDOW_INDEX = 7;
    private static final int SUMMARY_WINDOW_INDEX = 8;
}

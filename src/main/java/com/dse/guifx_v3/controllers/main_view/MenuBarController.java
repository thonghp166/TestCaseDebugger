package com.dse.guifx_v3.controllers.main_view;

import auto_testcase_generation.testdatagen.ITestcaseExecution;
import com.dse.compiler.Compiler;
import com.dse.compiler.message.ICompileMessage;
import com.dse.config.AkaConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.WorkspaceCreation;
import com.dse.environment.WorkspaceLoader;
import com.dse.environment.object.*;
import com.dse.guifx_v3.controllers.TestCasesNavigatorController;
import com.dse.guifx_v3.controllers.build_environment.AbstractCustomController;
import com.dse.guifx_v3.controllers.build_environment.BaseController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.Factory;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.objects.background_task.BackgroundTaskObjectController;
import com.dse.guifx_v3.objects.background_task.BackgroundTasksMonitorController;
import com.dse.guifx_v3.objects.background_task.RebuildEnvironmentTask;
import com.dse.optimize.TestCaseCleaner;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.IProjectNode;
import com.dse.parser.object.ProjectNode;
import com.dse.parser.systemlibrary.SystemHeaderParser;
import com.dse.probe_point_manager.ProbePointManager;
import com.dse.project_init.ProjectClone;
import com.dse.regression.ChangesBetweenSourcecodeFiles;
import com.dse.regression.IncrementalBuildingConfirmWindowController;
import com.dse.regression.controllers.AvailableRegressionScriptsController;
import com.dse.report.*;
import com.dse.search.Search;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_execution.TestCaseExecutionThread;
import com.dse.testcase_manager.CompoundTestCase;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testcasescript.SelectionUpdater;
import com.dse.testcasescript.object.*;
import com.dse.thread.AkaThreadManager;
import com.dse.util.AkaLogger;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuBarController implements Initializable {
    final static AkaLogger logger = AkaLogger.get(MenuBarController.class);

    @FXML
    public MenuItem miViewCoverage;

    @FXML
    public MenuItem miViewTestCaseManage;

    @FXML
    public MenuItem miViewTestCaseData;

    @FXML
    public MenuItem miViewFull;

    @FXML
    public Menu mView;

    @FXML
    public MenuItem miViewEnvironment;

    @FXML
    public MenuItem miAutomatedTestdataGenerationConfig;

    @FXML
    public MenuBar mMenuBar;

    @FXML
    public MenuItem miCleanAtSourcecodeFileLevel;

    @FXML
    public MenuItem miOpenTextEditorConfig;

    @FXML
    public Menu mSetting;

    @FXML
    public Menu mRun;

    @FXML
    public Menu mEdit;

    @FXML
    public Menu mEnvironment;

    @FXML
    public MenuItem miStopAutomatedTestdataGeneration;

    @FXML
    public MenuItem miDebug;

    @FXML
    public MenuItem miClean;

    @FXML
    public MenuItem miRunWithReport;

    @FXML
    public MenuItem miRunWithoutReport;

    @FXML
    MenuItem minewCCPPEnvironment;

    @FXML
    MenuItem miOpenProject;

    @FXML
    Menu miRecentEnvironments;

//    @FXML
//    MenuItem miSetWorkingDirectory;

    @FXML
    MenuItem miExit;

    @FXML
    MenuItem miSetupBoundOfVariableType;

    private BackgroundTaskObjectController rebuildEnvironmentBGController;

    @FXML
    MenuItem miImportTestcases;

    @FXML
    MenuItem miOpenEnvironment;

    @FXML
    Menu mNew;

    @FXML
    Menu mfile;

    /**
     * Singleton patern like
     */
    private static MenuBar menuBar = null;
    private static MenuBarController menuBarController = null;

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/MenuBar.fxml"));
        try {
            Parent parent = loader.load();
            menuBar = (MenuBar) parent;
            menuBarController = loader.getController();
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    public static MenuBar getMenuBar() {
        if (menuBar == null) {
            prepare();
        }
        return menuBar;
    }

    public static MenuBarController getMenuBarController() {
        if (menuBarController == null) {
            prepare();
        }
        return menuBarController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        miViewTestCaseData.setDisable(true);
        miViewCoverage.setDisable(true);
        miViewFull.setDisable(true);
        miViewTestCaseManage.setDisable(true);

        // set icon
        mNew.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/newEnvironment.png"))));
        miOpenEnvironment.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/openEnvironment.png"))));
//        miSetWorkingDirectory.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/setWorkingDirectory.png"))));
        miImportTestcases.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/importTestcase.png"))));
        miExit.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/exit.png"))));
        miRecentEnvironments.setGraphic(new ImageView(new Image(Factory.class.getResourceAsStream("/icons/file/recentEnvironments.png"))));

        // load existing environments
        mfile.setOnShowing(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                refreshRecentEnvironments();
            }
        });

//        refresh();
    }

    private void refreshRecentEnvironments() {
        miRecentEnvironments.getItems().removeAll(miRecentEnvironments.getItems());

        List<String> recentEnvironment = new AkaConfig().fromJson().getRecentEnvironments();
        for (String recentEnv : recentEnvironment)
            if (new File(recentEnv).exists()) {
                MenuItem miRecentEnv = new MenuItem();
                miRecentEnv.setText(recentEnv);

                // check exist
                boolean exist = false;
                for (MenuItem menuItem : miRecentEnvironments.getItems()) {
                    if (menuItem.getText().equals(recentEnv))
                        exist = true;
                }
                if (!exist) {
                    miRecentEnvironments.getItems().add(miRecentEnv);
                    // event when opening a recent environment
                    miRecentEnv.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            openEnvironmentFromEnvFile(new File(recentEnv));
                        }
                    });
                }
            }
    }
//    @FXML
//    void openProject(ActionEvent event) {
//        Stage primaryStage = UIController.getPrimaryStage();
//        DirectoryChooser projectChooser = new DirectoryChooser();
//        projectChooser.setInitialDirectory(new File(new AkaConfig().fromJson().getWorkingDirectory()));
//        File folder = projectChooser.showDialog(primaryStage);
//        if (folder != null) {
//            // Step 1. Clear the screen
//            UIController.clear();
//            uiLogger.log("Cleared screen...");
//            logger.debug("Cleared screen");
//
//            //uiLogger.log("Loading test cases navigator");
//            //UIController.loadTestCasesNavigator(new File("environment/test_cases_tree.json"));
//
//            // Step 2. Parse the selected project
//            uiLogger.log("Creating new thread to load project");
//            logger.debug("Creating new thread to load project");
//            ProjectLoadThread projectLoadThread = new ProjectLoadThread(folder);
//            Thread loadThread = new Thread(projectLoadThread);
//            loadThread.setDaemon(true);
//            loadThread.start();
//            uiLogger.log("The thread that's loading the project is running");
//            logger.debug("The thread that's loading the project is running");
//        }
//    }

    @FXML
    void exit(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to close AKA?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            System.exit(0);
        }
    }

    @FXML
    public void createNewEnvironment() {
        if (Environment.getInstance().getProjectNode() == null)
            AbstractCustomController.ENVIRONMENT_STATUS = AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_BLANK_GUI;
        else
            AbstractCustomController.ENVIRONMENT_STATUS = AbstractCustomController.STAGE.CREATING_NEW_ENV_FROM_OPENING_GUI;
        UIController.newCCPPEnvironment();
    }

    @FXML
    public void importTestCases() {
        if (Environment.getInstance().getName().equals("")) {
            String msg = "Must to have an environment to import testcases.";
            UIController.showErrorDialog(msg, "Environment not found", "Fail");
        } else {
            Stage primaryStage = UIController.getPrimaryStage();
            DirectoryChooser workingDirectoryChooser = new DirectoryChooser();
            workingDirectoryChooser.setTitle("Choose test cases directory");
            File file = workingDirectoryChooser.showDialog(primaryStage);

            if (file != null) {
                logger.debug("Import testcases from directory: " + file.getAbsolutePath());
                TestCaseManager.importTestCasesFromDirectory(file);
            }
        }
    }

    @FXML
    public void openEnvironment() {
        // backup current Environment
        Environment.createNewEnvironment();

        FileChooser fileChooser = new FileChooser();
//        String workingDirectory = new AkaConfig().fromJson().getWorkingDirectory();
//        if (workingDirectory == null || !(new File(workingDirectory).exists())) {
//            UIController.showErrorDialog("There is problem in the working directory. It may not be set up!",
//                    "Working directory does not exist", "Error");
//            Environment.setInstance(Environment.getBackupEnvironment());
//            return;
//        } else {
//            logger.debug("Working directory: " + workingDirectory);
//
//            // STEP: show a dialog to choose environment file .env
//            fileChooser.setInitialDirectory(new File(workingDirectory));
            addExtensionFilterWhenOpeningEnvironment(fileChooser);
            File environmentFile = fileChooser.showOpenDialog(UIController.getPrimaryStage());
            if (environmentFile != null && environmentFile.exists())
                openEnvironmentFromEnvFile(environmentFile);
//            else
//                return;
//        }
    }

    private void openEnvironmentFromEnvFile(File environmentFile) {
        setUpWorkingDirectoryAgain(environmentFile);

        // STEP: parse the environment file to construct environment tree
        boolean isAnalyzedSuccessfully = analyzeEnvironmentScript(environmentFile);
        if (!isAnalyzedSuccessfully) {
            UIController.showDetailDialogInMainThread(Alert.AlertType.ERROR, "Error",
                    "Environment file analysis error",
                    "Could not parse environment file " + environmentFile.getAbsolutePath());
            return;
        }

        RebuildEnvironmentTask task = new RebuildEnvironmentTask(environmentFile);
        // add task to background tasks monitor
        BackgroundTaskObjectController controller = BackgroundTaskObjectController.getNewInstance();
        if (controller != null) {
            rebuildEnvironmentBGController = controller;
            controller.setlTitle("Rebuild Environment");
            controller.setCancelTitle("Stopping Rebuild");
            BackgroundTasksMonitorController.getController().addBackgroundTask(controller);
            // set task to controller to cancel as need when processing
            controller.setTask(task);
            controller.getProgressIndicator().progressProperty().bind(task.progressProperty());
            controller.getProgressBar().progressProperty().bind(task.progressProperty());

            new Thread(task).start();
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    // remove task if done
                    Platform.runLater(() -> BackgroundTasksMonitorController.getController().removeBackgroundTask(controller));
                    rebuildEnvironmentBGController = null;
                    MenuBarController.getMenuBarController().refresh();

                    Platform.runLater(new SystemHeaderParser.ReloadThread());
//                    new SystemHeaderParser.ReloadThread().start();
                }
            });
            task.setOnCancelled(event -> {
//                Platform.runLater(() -> BackgroundTasksMonitorController.getController().removeBackgroundTask(controller));
                rebuildEnvironmentBGController = null;
                MenuBarController.getMenuBarController().refresh();
            });
        }
    }

    public BackgroundTaskObjectController getRebuildEnvironmentBGController() {
        return rebuildEnvironmentBGController;
    }

//    /**
//     * @return true if we can load GUI successfully
//     */
//    private boolean loadGUIWhenOpeningEnv() {
//        UIController.clear();
//
//        loadProjectStructureTree();
//        loadTestCaseNavigator();
//
//        // after load testcase, load probe points
//        loadProbePoints();
//        loadUnitTestableState();
//
//        ProjectClone.cloneEnvironment();
//
//        // update coverage type displaying on bottom,...
//        BaseSceneController.getBaseSceneController().updateInformation();
//
//        UIController.showSuccessDialog("Loading environment is successfully", "Success",
//                "The environment has been loaded");
//        return true;
//    }

    private void loadProbePoints() {
        ProbePointManager.getInstance().clear();
        ProbePointManager.getInstance().loadProbePoints();
        MDIWindowController.getMDIWindowController().updateLVProbePoints();
    }

    /**
     * When we build an environment successfully, or when we open an existing environment,
     * we will add the path of these environments to history.
     *
     * @param environmentFile the environment added to history
     * @return true if the environment file does not exist in the history,
     * false if otherwise
     */
    public static boolean addEnvironmentToHistory(File environmentFile) {
        AkaConfig akaConfig = new AkaConfig().fromJson();
        List<String> recentEnvironments = akaConfig.getRecentEnvironments();
        if (!(recentEnvironments.contains(environmentFile.getAbsolutePath()))) {
            recentEnvironments.add(environmentFile.getAbsolutePath());
            akaConfig.exportToJson();
            return true;
        } else
            return false;
    }

    public void loadUnitTestableState() {
        List<INode> sources = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());

        List<IEnvironmentNode> uuts = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroUUTNode());
        for (IEnvironmentNode uut : uuts) {
            for (INode source : sources) {
                if (((EnviroUUTNode) uut).getName().equals(source.getAbsolutePath())) {
                    ((EnviroUUTNode) uut).setUnit(source);
                    break;
                }
            }
        }

        List<IEnvironmentNode> sbfs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroSBFNode());
        for (IEnvironmentNode sbf : sbfs) {
            for (INode source : sources) {
                if (((EnviroSBFNode) sbf).getName().equals(source.getAbsolutePath())) {
                    ((EnviroSBFNode) sbf).setUnit(source);
                    break;
                }
            }
        }

        List<IEnvironmentNode> dontStubs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroDontStubNode());
        for (IEnvironmentNode dontStub : dontStubs) {
            for (INode source : sources) {
                if (((EnviroDontStubNode) dontStub).getName().equals(source.getAbsolutePath())) {
                    ((EnviroDontStubNode) dontStub).setUnit(source);
                    break;
                }
            }
        }
    }

    private void setUpWorkingDirectoryAgain(File environmentFile) {
        // change the working directory to the directory contains the environment file
        AkaConfig akaConfig = new AkaConfig().fromJson();

        File newWorkingDirectory = environmentFile.getParentFile();
        akaConfig = akaConfig.setWorkingDirectory(newWorkingDirectory.getAbsolutePath());

        String workspace = environmentFile.getAbsolutePath().replace(".env", SpecialCharacter.EMPTY);
        akaConfig = akaConfig.setOpeningWorkspaceDirectory(workspace);

        akaConfig.setOpenWorkspaceConfig(environmentFile.getAbsolutePath());
        akaConfig.exportToJson();

        BaseSceneController.getBaseSceneController().getLblCurrentWorkspace().setText("Current workspace: " + workspace);
        logger.debug("The new work space: " + workspace);
    }

//    /**
//     * Load the project rather than parsing it
//     * @return null if we find compilation errors
//     */
//    private INode loadProject() {
//        ChangesBetweenSourcecodeFiles.reset();
//
//        WorkspaceLoader loader = new WorkspaceLoader();
//
//        String workspacePath = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
//        loader.setWorkspace(new File(workspacePath));
//
//        String physicalTreePath = new WorkspaceConfig().fromJson().getPhysicalJsonFile();
//        loader.setPhysicalTreePath(new File(physicalTreePath));
//        loader.load(loader.getPhysicalTreePath());
//        INode root = loader.getRoot();
//        return root;
//    }

    public static Alert showDialogWhenComparingSourcecode(Alert.AlertType type, String title, String headText, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headText);
        alert.setContentText(content);
        ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        ButtonType viewChangesButton = new ButtonType("Changes", ButtonBar.ButtonData.HELP);
        ButtonType viewDependenciesButton = new ButtonType("Problems", ButtonBar.ButtonData.HELP);

        alert.getButtonTypes().setAll(okButton, noButton, viewChangesButton, viewDependenciesButton);

        // show changes
        Button tmp = (Button) alert.getDialogPane().lookupButton(viewChangesButton);
        tmp.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    UIController.showDetailDialog(Alert.AlertType.INFORMATION, "Changes detection",
                            "Changes",
                            Utils.readFileContent(new WorkspaceConfig().fromJson().getFileContainingChangesWhenComparingSourcecode()));
                    event.consume();
                }
        );

        // show unresolved dependencies
        Button tmp2 = (Button) alert.getDialogPane().lookupButton(viewDependenciesButton);
        tmp2.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    String unresolvedDependenciesContent = Utils.readFileContent(new WorkspaceConfig().fromJson().getFileContainingUnresolvedDependenciesWhenComparingSourcecode());
                    UIController.showDetailDialog(Alert.AlertType.INFORMATION, "Unresolved dependencies detection",
                            "Unresolved dependencies",
                            unresolvedDependenciesContent);
                    event.consume();
                }
        );


        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setMinHeight(350);
        textArea.setText(content);

        alert.getDialogPane().setContent(textArea);

        return alert;
    }

//    private void loadTestCaseNavigator() {
//        if (Environment.getInstance().getEnvironmentRootNode() != null) {
//            EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
//            if (root != null) {
//                List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroNameNode());
//                if (nodes.size() == 1) {
//
//                    String testcaseScriptPath = new WorkspaceConfig().fromJson().getTestscriptFile();
//                    File testcaseScriptFile = new File(testcaseScriptPath);
//                    if (testcaseScriptFile.exists()) {
//                        UIController.loadTestCasesNavigator(testcaseScriptFile);
//                        logger.debug("The test script file path: " + testcaseScriptPath);
//
//                        TestCaseManager.clearMaps();
//                        TestCaseManager.initializeMaps(new Environment());
//
//                        addMenuItemDisableProperty(miViewTestCaseData,
//                                new TestNewNode(),
//                                new TestNormalSubprogramNode(),
//                                new TestCompoundSubprogramNode(),
//                                new TestInitSubprogramNode(),
//                                new TestUnitNode(),
//                                new TestcaseRootNode()
//                        );
//                        addMenuItemDisableProperty(miViewFull,
//                                new TestNewNode(),
//                                new TestNormalSubprogramNode(),
//                                new TestCompoundSubprogramNode(),
//                                new TestInitSubprogramNode(),
//                                new TestUnitNode(),
//                                new TestcaseRootNode());
//                        addMenuItemDisableProperty(miViewTestCaseManage,
////                                new TestNewNode(),
////                                new TestNormalSubprogramNode(),
//                                new TestCompoundSubprogramNode(),
////                                new TestInitSubprogramNode(),
//                                new TestUnitNode(),
//                                new TestcaseRootNode()
//                        );
//                        addMenuItemDisableProperty(miViewCoverage,
//                                new TestNewNode(),
//                                new TestNormalSubprogramNode()
////                                new TestUnitNode(),
////                                new TestcaseRootNode()
//                        );
//
//                    } else {
//                        logger.error("Can not load the test case navigator script file");
//                        logger.debug("The test script file path: " + testcaseScriptPath);
//                    }
//                } else {
//                    logger.error("There is more than one name in the environment file");
//                }
//            }
//        }
//    }

    public MenuItem getMiViewTestCaseData() {
        return miViewTestCaseData;
    }

    public MenuItem getMiViewFull() {
        return miViewFull;
    }

    public MenuItem getMiViewCoverage() {
        return miViewCoverage;
    }

    public MenuItem getMiViewTestCaseManage() {
        return miViewTestCaseManage;
    }

    private void addMenuItemDisableProperty(MenuItem menuItem, Class<?>... activeNodes) {
        if (menuItem == null)
            return;
        try {
            menuItem.setDisable(true);
        } catch (Exception e) {

        }
        TreeTableView<ITestcaseNode> testCasesNavigator = TestCasesNavigatorController
                .getInstance().getTestCasesNavigator();

        testCasesNavigator.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            menuItem.disableProperty().bind(new BooleanBinding() {
                @Override
                protected boolean computeValue() {
                    try {
                        ObservableList<TreeItem<ITestcaseNode>> list = testCasesNavigator
                                .getSelectionModel().getSelectedItems();

                        for (TreeItem<ITestcaseNode> selectedItem : list) {
                            boolean disable = true;

                            if (activeNodes != null)
                                for (Class<?> node : activeNodes)
                                    if (selectedItem != null && selectedItem.getValue() != null) {
                                        if (node.isInstance(selectedItem.getValue())) {
                                            disable = false;
                                            break;
                                        }
                                    }

                            if (disable)
                                return true;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
        });
    }

    // trung lap voi file NameEnvironmentController
    public void loadProjectStructureTree() {
        if (Environment.getInstance().getProjectNode() != null) {
            IProjectNode root = Environment.getInstance().getProjectNode();

            EnvironmentRootNode environmentRootNode = Environment.getInstance().getEnvironmentRootNode();
            List<IEnvironmentNode> nameNodes = EnvironmentSearch.searchNode(environmentRootNode, new EnviroNameNode());
            if (nameNodes.size() == 1) {
                root.setName(((EnviroNameNode) nameNodes.get(0)).getName());
                UIController.loadProjectStructureTree(root);
            } else {
                logger.error("There are more than one name in the enviroment script. Can not export!");
            }
        }
    }

    /**
     * Only accept .env files when opening a dialog for choosing environment file
     *
     * @param fileChooser
     */
    private void addExtensionFilterWhenOpeningEnvironment(FileChooser fileChooser) {
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ENV files (*.env)", "*.env");
        fileChooser.getExtensionFilters().add(extFilter);
    }

    /**
     * @param environmentFile
     * @return false if parsing environment file .env causses errors
     */
    private boolean analyzeEnvironmentScript(File environmentFile) {
        logger.debug("You select the environment script: " + environmentFile.getAbsolutePath());
        logger.debug("Analyzing the environment script...");
        EnvironmentAnalyzer analyzer = new EnvironmentAnalyzer();
        analyzer.analyze(environmentFile);
        IEnvironmentNode root = analyzer.getRoot();

        if (root != null && root instanceof EnvironmentRootNode) {
            Environment.getInstance().setEnvironmentRootNode((EnvironmentRootNode) root);

            // update config at application level
            AkaConfig akaConfig = new AkaConfig().fromJson();

            String openingWorkspaceDir = environmentFile.getParentFile().getAbsolutePath() + File.separator +  Environment.getInstance().getName();
            akaConfig.setOpeningWorkspaceDirectory(
                    openingWorkspaceDir
                    //        akaConfig.getWo.gerkingDirectory() + File.separator + Environment.getInstance().getName()
            );

            akaConfig.setOpenWorkspaceConfig(
                    environmentFile.getParentFile().getAbsolutePath()
                    //        akaConfig.getWorkingDirectory() + File.separator + Environment.getInstance().getName()
            );
            akaConfig.setOpenWorkspaceConfig(
                    openingWorkspaceDir + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME
                    //akaConfig.getWorkingDirectory() + File.separator + Environment.getInstance().getName() + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME
            );
            akaConfig.exportToJson();
            return true;
        } else
            return false;
    }

//    @FXML
//    public void setWorkingDirectory(ActionEvent actionEvent) {
//        Stage primaryStage = UIController.getPrimaryStage();
//        DirectoryChooser workingDirectoryChooser = new DirectoryChooser();
//        workingDirectoryChooser.setTitle("Choose working directory");
//        File file = workingDirectoryChooser.showDialog(primaryStage);
//
//        if (file != null) {
//            if (file.isDirectory()) {
//                new AkaConfig().fromJson().setWorkingDirectory(file.getAbsolutePath()).exportToJson();
//                BaseSceneController.getBaseSceneController().setupWorkingDirectory();
//                logger.debug("The current working directory: " + file.getAbsolutePath());
//
//                showValidWorkingDirectoryStatus();
//                getMinewCCPPEnvironment().setDisable(false);
//            } else {
//                // never happen
//            }
//        }
//    }

//    private void showValidWorkingDirectoryStatus() {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Set the working directory");
//        alert.setHeaderText("Success");
//        alert.setContentText("The working directory has been set up");
//        alert.showAndWait();
//    }

    private void showValidZ3SolverStatus() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Set the z3 solver");
        alert.setHeaderText("Success");
        alert.setContentText("The z3 solver has been set up");
        alert.showAndWait();
    }

    private List<ITestCase> getAllSelectedTestCases() {
        List<ITestCase> testCases = new ArrayList<>();

        if (Environment.getInstance().isCoverageModeActive()) {
            List<ITestcaseNode> selectedTestcases =
                    SelectionUpdater.getAllSelectedTestcases(Environment.getInstance().getTestcaseScriptRootNode());

            for (ITestcaseNode node : selectedTestcases)
                if (node instanceof TestNameNode) {
                    try {
                        ITestCase testCase = TestCaseManager.getTestCaseByName(((TestNameNode) node).getName());
                        if (testCase != null)
                            testCases.add(testCase);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
        }
        return testCases;
    }

    @FXML
    public void viewTestCaseData(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        List<ITestCase> testCases = getAllSelectedTestCases();

        // Generate test case data report
        IReport report = new TestCaseDataReport(testCases, LocalDateTime.now());
        ReportManager.export(report);

        // display on MDIWindow
        MDIWindowController.getMDIWindowController().viewReport(report.getName(), report.toHtml());
    }

    @FXML
    public void viewFullReport(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        List<ITestCase> testCases = getAllSelectedTestCases();

        for (ITestCase testCase : testCases) {
            Platform.runLater(() -> {
                // Generate test case data report
                IReport report = new FullReport(testCase, LocalDateTime.now());
                ReportManager.export(report);

                // display on MDIWindow
                MDIWindowController.getMDIWindowController().viewReport(report.getName(), report.toHtml());
            });
        }
    }

    @FXML
    public void viewTestCaseManage(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        List<ITestcaseNode> selectedNodes = SelectionUpdater.getAllSelectedSourcecodeNodes(Environment.getInstance().getTestcaseScriptRootNode());
        IReport report = new TestCaseManagementReport(selectedNodes, LocalDateTime.now());
        ReportManager.export(report);

        // display on MDIWindow
        MDIWindowController.getMDIWindowController().viewReport(report.getName(), report.toHtml());
    }

    @FXML
    @Deprecated
    public void viewCoverage(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }

        List<ITestCase> testCases = getAllSelectedTestCases();
        testCases.removeIf(tc -> tc instanceof CompoundTestCase || !tc.getStatus().equals(TestCase.STATUS_SUCCESS));

        if (!testCases.isEmpty()) {
            if (testCases.size() == 1)
                UIController.viewCoverageOfATestcase((TestCase) testCases.get(0));
            else {
                Map<INode, List<TestCase>> map = new HashMap<>();

                for (ITestCase testCase : testCases) {
                    ICommonFunctionNode function = ((TestCase) testCase).getFunctionNode();
                    INode unit = Utils.getSourcecodeFile(function);
                    List<TestCase> testCasesOfUnit = map.get(unit);

                    if (testCasesOfUnit == null)
                        testCasesOfUnit = new ArrayList<>();

                    if (!testCasesOfUnit.contains(testCase))
                        testCasesOfUnit.add((TestCase) testCase);

                    map.put(function, testCasesOfUnit);
                }

                for (INode unit : map.keySet())
                    Platform.runLater(() -> UIController.viewCoverageOfMultipleTestcases(unit.getName(), map.get(unit)));
            }
        }
    }

    public MenuItem getMinewCCPPEnvironment() {
        return minewCCPPEnvironment;
    }

//    public void rebuildEnvironment() {
//        // STEP: load project
//        boolean findCompilationError = loadProject() == null;
//        if (findCompilationError) {
//            String compilationError = Utils.readFileContent(new WorkspaceConfig().fromJson().getCompilationMessageWhenComplingProject());
//            UIController.showDetailDialogInMainThread(Alert.AlertType.ERROR, "Compilation error",
//                    "Found compilation error. The environment does not change!",
//                    compilationError);
//            return;
//        }
//
//        // STEP: check whether we need to
//        if (ChangesBetweenSourcecodeFiles.modifiedSourcecodeFiles.size() == 0) {
//            Platform.runLater(new Runnable() {
//                @Override
//                public void run() {
//                    loadGUI();
//                }
//            });
////            loadGUI();
//            return;
//        }
//
//        // STEP: show a dialog to inform changes
//        Alert alert = showDialogWhenComparingSourcecode(Alert.AlertType.WARNING, "Warning",
//                "Found changes in the testing project!\n" +
//                        "Do you still want to load the environment?",
//                ChangesBetweenSourcecodeFiles.getModifiedSourcecodeFilesInString());
//        alert.showAndWait().ifPresent(type -> {
//            if (type.getText().toLowerCase().equals("no")) {
//                UIController.showInformationDialog("You stopped loading the environment file due to changes in source code files"
//                        , "Information", "Stop loading environment");
//
//            } else if (type.getText().toLowerCase().equals("yes")) {
//                new WorkspaceUpdater().update();
//                Platform.runLater(this::loadGUI);
////            loadGUI();
//            }
//        });
//    }

    @FXML
    public void updateEnvironment() {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        AbstractCustomController.ENVIRONMENT_STATUS = AbstractCustomController.STAGE.UPDATING_ENV_FROM_OPENING_ENV;
        UIController.updateCCPPEnvironment();
        BaseController.loadEnvironment();
    }

    public void setMinewCCPPEnvironment(MenuItem minewCCPPEnvironment) {
        this.minewCCPPEnvironment = minewCCPPEnvironment;
    }

    public void setMiExit(MenuItem miExit) {
        this.miExit = miExit;
    }

    public MenuItem getMiExit() {
        return miExit;
    }

    public void setMiOpenProject(MenuItem miOpenProject) {
        this.miOpenProject = miOpenProject;
    }

    public MenuItem getMiOpenProject() {
        return miOpenProject;
    }

//    public void setMiSetWorkingDirectory(MenuItem miSetWorkingDirectory) {
//        this.miSetWorkingDirectory = miSetWorkingDirectory;
//    }

//    public MenuItem getMiSetWorkingDirectory() {
//        return miSetWorkingDirectory;
//    }

    public Menu getMiRecentProject() {
        return miRecentEnvironments;
    }

    public void setMiRecentProject(Menu miRecentProject) {
        this.miRecentEnvironments = miRecentProject;
    }

    @FXML
    public void cleanTestCases(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }

        List<ITestCase> testCases = getAllSelectedTestCases();
        int numSelectedFunction = SelectionUpdater.getAllSelectedFunctions(Environment.getInstance().getTestcaseScriptRootNode()).size();
        Alert confirmAlert = UIController.showYesNoDialog(Alert.AlertType.CONFIRMATION, "Clean Test Cases",
                "Remove all unnecessary test cases",
                "All test cases which don't increase accumulated coverage will be removed. Are you sure want to continue?\n\n" +
                        "Number of selected test cases: " + testCases.size() + "\nNumber of selected functions: " + numSelectedFunction);

        Optional<ButtonType> option = confirmAlert.showAndWait();
        if (option.get() == ButtonType.YES) {

            Platform.runLater(() -> TestCaseCleaner.clean(testCases));
        } else {
            confirmAlert.close();
        }
    }

    @FXML
    public void runAllSelectedTestCasesWithReport(ActionEvent actionEvent) {
        runAllSelectedTestCases(true);
    }

    @FXML
    public void runAllSelectedTestCasesWithoutReport(ActionEvent actionEvent) {
        runAllSelectedTestCases(false);
    }

    public void runAllSelectedTestCases(boolean showReport) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        // get all test cases
        List<ITestCase> testCases = getAllSelectedTestCases();
        logger.debug("You are requesting to execute " + testCases.size() + " test cases");

        // put test cases in different threads
        List<TestCaseExecutionThread> tasks = new ArrayList<>();
        for (ITestCase testCase : testCases) {
            testCase.deleteOldDataExceptValue();
            TestCaseExecutionThread executionThread = new TestCaseExecutionThread(testCase);
            executionThread.setExecutionMode(ITestcaseExecution.IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE);
            executionThread.setShouldShowReport(showReport);
            tasks.add(executionThread);
        }
        logger.debug("Create " + tasks.size() + " threads to execute " + tasks.size() + " test cases");

        // add these threads to executors
        // at the same time, we do not execute all of the requested test cases.
        int MAX_EXECUTING_TESTCASE = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_EXECUTING_TESTCASE);
        for (TestCaseExecutionThread task : tasks)
            executorService.execute(task);
        executorService.shutdown();
    }

    @FXML
    public void debugTestCase(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        // get all test cases
        List<ITestCase> testCases = getAllSelectedTestCases();
        testCases.removeIf(tc -> tc instanceof CompoundTestCase);
        logger.debug("You are requesting to execute " + testCases.size() + " test cases");

        // put test cases in different threads
        List<Task<ITestCase>> tasks = new ArrayList<>();
        for (ITestCase testCase : testCases) {
            testCase.deleteOldDataExceptValue();
            Task<ITestCase> task = new Task<ITestCase>() {
                @Override
                protected ITestCase call() {
                    Platform.runLater(() -> UIController.executeTestCaseWithDebugMode((TestCase) testCase));
                    return testCase;
                }
            };
            tasks.add(task);
        }
        logger.debug("Create " + tasks.size() + " threads to execute " + tasks.size() + " test cases");

        // add these threads to executors
        // at the same time, we do not execute all of the requested test cases.
        int MAX_EXECUTING_TESTCASE = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_EXECUTING_TESTCASE);
        for (Task<ITestCase> task : tasks)
            executorService.execute(task);
        executorService.shutdown();
    }

    public void viewEnvironment(ActionEvent actionEvent) {
        IReport report = new EnvironmentReport(Environment.getInstance(), LocalDateTime.now());
        ReportManager.export(report);

        // display on MDIWindow
        MDIWindowController.getMDIWindowController().viewReport(report.getName(), report.toHtml());
    }

    @FXML
    public void openAutomatedTestdataGenerationConfig(ActionEvent actionEvent) {
        // will show a dialog to config the default function configuration in automated test data generation module
        // save config in workspace.aka
        Stage stage = Factory.generateDefaultFunctionConfigStage();
        if (stage != null) {
            stage.initOwner(UIController.getPrimaryStage());
            stage.show();
        }
    }

    @FXML
    public void openTexteditorConfig(ActionEvent actionEvent) {
        // will show a dialog to config the default function configuration in automated test data generation module
        // save config in workspace.aka
        // config: length of tab, v.v.
    }

    /**
     * Analyze dependency again.
     * <p>
     * This function will overwrite existing dependency files in the folder {working-space}/{env-name}/dependencies
     *
     * @param actionEvent
     */
    public void analyzeDependencies(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        try {
            ProjectNode projectRootNode = Environment.getInstance().getProjectNode();
            new WorkspaceCreation().exportSourcecodeFileNodeToWorkingDirectory(projectRootNode,
                    new WorkspaceConfig().fromJson().getElementDirectory(),
                    new WorkspaceConfig().fromJson().getDependencyDirectory());
            UIController.showSuccessDialog("Dependency analyzer successes", "Dependency analyzer", "Success");
        } catch (Exception e) {
            e.printStackTrace();
            UIController.showErrorDialog("Dependency analyzer caught an unexpected error", "Dependency analyzer", "Fail");
        }
    }

    public void cleanTestCasesAtSourcecodeFileLevel(ActionEvent actionEvent) {
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }
        // code later
    }

    @FXML
    public void incrementalBuild(ActionEvent actionEvent) {
        // todo: put to running processes
        if (Environment.getInstance().getProjectNode() == null) {
            UIController.showErrorDialog("You must open an environment firstly", "Update Environment", "There is no opening environment");
            return;
        }

        // backup Environment before build
        backupEnvironment();

        // STEP: compile all source code files
        // todo: move to Task to run in another thread
        String error = compileAllSourcecodeFiles();
        if (error != null && error.length() > 0) {
            UIController.showDetailDialogInMainThread(Alert.AlertType.ERROR, "Incremental build",
                    "Found compilation error. The environment does not change!",
                    error);
            // restore Environment if find out any compile error
            Environment.restoreEnvironment();
            return;
        }

        // STEP: load project
        ChangesBetweenSourcecodeFiles.reset();
        WorkspaceLoader loader = new WorkspaceLoader();
        String physicalTreePath = new WorkspaceConfig().fromJson().getPhysicalJsonFile();
        loader.setPhysicalTreePath(new File(physicalTreePath));
        loader.setShouldCompileAgain(false);
        loader.setElementFolderOfOldVersion(new WorkspaceConfig().fromJson().getElementDirectory());

        // the load method below might call one thread, so use while loop to wait for the thread done
        loader.load(loader.getPhysicalTreePath());
        while (!loader.isLoaded()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (ChangesBetweenSourcecodeFiles.modifiedSourcecodeFiles.size() == 0) {
            UIController.showSuccessDialog("There is no change in source code files when rebuilding incrementally",
                    "Incremental build", "No change");
            // restore Environment if find out any compile error
            Environment.restoreEnvironment();
            return;
        } else {
//            UIController.showDetailDialog(Alert.AlertType.INFORMATION, "Incremental build", "Changes",
//                    ChangesBetweenSourcecodeFiles.getReportOfDifferences());
            Stage window = IncrementalBuildingConfirmWindowController.getWindow();
            if (window != null) {
                window.setResizable(false);
                window.initModality(Modality.WINDOW_MODAL);
                window.initOwner(UIController.getPrimaryStage().getScene().getWindow());
                window.setOnCloseRequest(event -> {
                    Environment.restoreEnvironment();
                });
                window.show();
            }

            logger.debug("Deleted Nodes: ");
            for (String str : ChangesBetweenSourcecodeFiles.deletedPaths) {
                logger.debug(str);
            }
            logger.debug("End Deleted Node.");
            logger.debug("Modified Nodes:");
            for (INode node : ChangesBetweenSourcecodeFiles.modifiedNodes) {
                logger.debug(node.getAbsolutePath());
            }
            logger.debug("End Modified Nodes.");
            logger.debug("Added Nodes:");
            for (INode node : ChangesBetweenSourcecodeFiles.addedNodes) {
                logger.debug(node.getAbsolutePath());
            }
            logger.debug("End Added Nodes.");
        }

    }

    private void backupEnvironment() {
        // use update Environment to backup and create a clone version of currently Environment
        Environment.backupEnvironment();
        Environment.getInstance().setActiveSourcecodeTabs(Environment.getBackupEnvironment().getActiveSourcecodeTabs());
    }

    public void loadTestCaseNavigator() {
        if (Environment.getInstance().getEnvironmentRootNode() != null) {
            EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
            if (root != null) {
                List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroNameNode());
                if (nodes.size() == 1) {

                    String testcaseScriptPath = new WorkspaceConfig().fromJson().getTestscriptFile();
                    File testcaseScriptFile = new File(testcaseScriptPath);
                    if (testcaseScriptFile.exists()) {
                        UIController.loadTestCasesNavigator(testcaseScriptFile);
                        logger.debug("The test script file path: " + testcaseScriptPath);

                        TestCaseManager.clearMaps();
                        TestCaseManager.initializeMaps();

                        addMenuItemDisableProperty(miViewTestCaseData,
                                TestNewNode.class,
                                TestNormalSubprogramNode.class,
                                TestCompoundSubprogramNode.class,
                                TestInitSubprogramNode.class,
                                TestUnitNode.class,
                                TestcaseRootNode.class
                        );
                        addMenuItemDisableProperty(miViewFull,
                                TestNewNode.class,
                                TestNormalSubprogramNode.class,
                                TestCompoundSubprogramNode.class,
                                TestInitSubprogramNode.class,
                                TestUnitNode.class,
                                TestcaseRootNode.class
                        );
                        addMenuItemDisableProperty(miViewTestCaseManage,
                                TestCompoundSubprogramNode.class,
                                TestInitSubprogramNode.class,
                                TestUnitNode.class,
                                TestcaseRootNode.class
                        );
//                        addMenuItemDisableProperty(miViewCoverage,
//                                TestNewNode.class,
//                                TestNormalSubprogramNode.class,
//                                TestUnitNode.class
//                        );

                    } else {
                        logger.error("Can not load the test case navigator script file");
                        logger.debug("The test script file path: " + testcaseScriptPath);
                    }
                } else {
                    logger.error("There is more than one name in the environment file");
                }
            }
        }
    }

    /**
     * @return a message if we found compilation error
     */
    private String compileAllSourcecodeFiles() {
        // some changes make a source code file unable to compile
        String error = "";
        List<INode> sourcecodeFileNodes = WorkspaceLoader.getCompilableSourceNodes();
        for (INode modifiedSrcFile : sourcecodeFileNodes) {
            Compiler c = Environment.getInstance().getCompiler();
            ICompileMessage message = c.compile(modifiedSrcFile);
            if (message.getType() == ICompileMessage.MessageType.ERROR) {
                error += modifiedSrcFile.getAbsolutePath() + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
                return error;
            }
        }
        return error;
    }

    public void stopAllAutomatedTestdataGenerationThread(ActionEvent actionEvent) {
        logger.debug("Shut down all automated test data generation threads");
        AkaThreadManager.stopAutomatedTestdataGenerationForAll(Environment.getInstance().getProjectNode());
        UIController.showSuccessDialog("Stop automated test data generation successfully", "Automated test data generation", "Terminate successfully");
        MenuBarController.getMenuBarController().refresh();
    }

    public synchronized void refresh() {
        boolean openedProject = Environment.getInstance().getProjectNode() == null;
        ITestcaseNode testcaseRoot = Environment.getInstance().getTestcaseScriptRootNode();

        // FILE
        miImportTestcases.setDisable(openedProject);
        miRecentEnvironments.setDisable(new AkaConfig().fromJson().getRecentEnvironments().size() == 0);

        // ENVIRONMENT
        mEnvironment.setDisable(openedProject);

        // EDIT
        mEdit.setDisable(openedProject);
        boolean selectAtLeastOneFunction = SelectionUpdater.selectAtLeastOneSubprogram(testcaseRoot);
        miClean.setDisable(!selectAtLeastOneFunction);
        miCleanAtSourcecodeFileLevel.setDisable(!selectAtLeastOneFunction);

        // RUN
        mRun.setDisable(openedProject);
        miRunWithoutReport.setDisable(!SelectionUpdater.selectAtLeastOneTestcase(testcaseRoot));
        miRunWithReport.setDisable(!SelectionUpdater.selectAtLeastOneTestcase(testcaseRoot));
        miDebug.setDisable(!SelectionUpdater.selectJustOneTestcase(testcaseRoot));
        miStopAutomatedTestdataGeneration.setDisable(!AkaThreadManager.runningAtLeastOneAutomatedTestdataGenerationThread());

        // SETTING
        mSetting.setDisable(openedProject);
//        miSetupBoundOfVariableType.setDisable(false);
//        mSetting.setDisable(false);

        // VIEW
        mView.setDisable(openedProject);
    }

    @FXML
    public void showAvailableRegressionScripts () {
        AvailableRegressionScriptsController controller = AvailableRegressionScriptsController.getInstance();
        if (controller != null) {
            Stage stage = controller.getStage();
            if (stage != null) {
                stage.setResizable(false);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initOwner(UIController.getPrimaryStage().getScene().getWindow());
                stage.show();
            }
        }
    }

    public void instrumentProject(ActionEvent actionEvent) {
        try {
            ProjectClone.cloneEnvironment();
            UIController.showSuccessDialog("Project is instrumented successfully", "Project instrumentation", "Success");
        } catch (Exception e) {
            e.printStackTrace();
            UIController.showErrorDialog("Could not instrument the project", "Project instrumentation", "Fail");
        }
    }

    public void setupBoundOfVariableType(ActionEvent actionEvent) {
        Stage stage = Factory.generateBoundOfVariableTypesStage();
        if (stage != null) {
            stage.initOwner(UIController.getPrimaryStage());
            stage.show();
        }
    }

    public void setZ3Solver(ActionEvent actionEvent) {
        while (true) {
            Stage primaryStage = UIController.getPrimaryStage();
            DirectoryChooser workingDirectoryChooser = new DirectoryChooser();
            workingDirectoryChooser.setTitle("Select Z3 Solver");
            File selectedDir = workingDirectoryChooser.showDialog(primaryStage);

            if (selectedDir != null) {
                String z3Path = null;
                if (selectedDir.isDirectory()) {
                    List<String> allFiles = Utils.getAllFiles(selectedDir.getAbsolutePath());
                    for (String f : allFiles) {
                        String path = new File(f).getAbsolutePath();
                        if (path.endsWith("bin" + File.separator + "z3")
                                || path.equals("bin" + File.separator + "z3.exe")){
                            z3Path = new File(f).getAbsolutePath();
                            break;
                        }
                    }
                }

                if (z3Path != null) {
                    new AkaConfig().fromJson().setZ3Path(z3Path).exportToJson();
//                    BaseSceneController.getBaseSceneController().setupWorkingDirectory();
                    logger.debug("The current z3 solver file: " + z3Path);
                    showValidZ3SolverStatus();
                    return;
                } else {
                    if (selectedDir.isDirectory())
                        UIController.showErrorDialog("Do not find z3 in folder " + selectedDir.getAbsolutePath()
                                , "Set up z3 solver", "Wrong configuration");
                }
            } else
                return;
        }
    }
}

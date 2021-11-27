package com.dse.guifx_v3.objects.background_task;

import com.dse.config.WorkspaceConfig;
import com.dse.environment.WorkspaceLoader;
import com.dse.guifx_v3.controllers.main_view.BaseSceneController;
import com.dse.guifx_v3.controllers.main_view.MDIWindowController;
import com.dse.guifx_v3.controllers.main_view.MenuBarController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.INode;
import com.dse.probe_point_manager.ProbePointManager;
import com.dse.regression.ChangesBetweenSourcecodeFiles;
import com.dse.regression.RegressionScriptManager;
import com.dse.regression.WorkspaceUpdater;
import com.dse.thread.AbstractAkaTask;
import com.dse.util.Utils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.log4j.Logger;

import java.io.File;

public class RebuildEnvironmentTask extends AbstractAkaTask<Object> {
    final static Logger logger = Logger.getLogger(RebuildEnvironmentTask.class);

    private File environmentFile;

    public RebuildEnvironmentTask(File environmentFile) {
        this.environmentFile = environmentFile;
    }

    @Override
    protected Object call() throws Exception {
        int max = 10;

        // STEP: load project
        // if there are any compilation errors, we show it to users
        ChangesBetweenSourcecodeFiles.reset();
        WorkspaceLoader loader = new WorkspaceLoader();
        String physicalTreePath = new WorkspaceConfig().fromJson().getPhysicalJsonFile();
        loader.setPhysicalTreePath(new File(physicalTreePath));
        loader.setShouldCompileAgain(true);
        loader.setElementFolderOfOldVersion(new WorkspaceConfig().fromJson().getElementDirectory());

        // GUI
        updateProgress(1, max);

        loader.load(loader.getPhysicalTreePath());

        // todo: can hoi y kien anh Duc Anh
        while (!loader.isLoaded()) {
            Thread.sleep(100);
        }

        if (loader.isCancel()) {
            // STOP LOADING ENVIRONMENT
            logger.debug("Rebuild Environment Task was canceled");
            Environment.restoreEnvironment();
            if (Environment.getInstance() == null) Environment.createNewEnvironment();
            return null;
        }

        INode root = loader.getRoot();
        updateProgress(6, max);

        boolean findCompilationError = root == null;
        // so findCompilationError == true if there are error when compile OR the compile process was stopped
        if (findCompilationError) {
            String compilationError = Utils.readFileContent(new WorkspaceConfig().fromJson().getCompilationMessageWhenComplingProject());
            UIController.showDetailDialogInMainThread(Alert.AlertType.ERROR, "Compilation error",
                    "Found compilation error. The environment does not change!",
                    compilationError);
            updateProgress(10, max);
            // STOP LOADING ENVIRONMENT
            return null;
        }

        // STEP: If all source code files are compiled successfully,
        // we need to check whether any source code files are modified.
        // If we found at least one, show it to users
        if (ChangesBetweenSourcecodeFiles.modifiedSourcecodeFiles.size() > 0) {
            // STEP: show a dialog to inform changes
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Alert alert = MenuBarController.showDialogWhenComparingSourcecode(Alert.AlertType.WARNING, "Warning",
                            "Found changes in the testing project!\n" +
                                    "Do you still want to load the environment?",
                            ChangesBetweenSourcecodeFiles.getModifiedSourcecodeFilesInString());
                    alert.showAndWait().ifPresent(type -> {
                        if (type.getText().toLowerCase().equals("no")) {
                            UIController.showInformationDialog("You stopped loading the environment file due to changes in source code files"
                                    , "Information", "Stop loading environment");

                        } else if (type.getText().toLowerCase().equals("yes")) {
                            // update workspace with modification
                            new WorkspaceUpdater().update();

                            loadGUIWhenOpeningEnv();
//                    ProjectClone.cloneEnvironment();
                            MenuBarController.addEnvironmentToHistory(environmentFile);
                            MenuBarController.getMenuBarController().refresh();
                        }
                    });
                }
            });

        } else {
            Platform.runLater(this::loadGUIWhenOpeningEnv);
//            ProjectClone.cloneEnvironment();
            MenuBarController.addEnvironmentToHistory(environmentFile);
            MenuBarController.getMenuBarController().refresh();
            updateProgress(10, 10);
        }

        updateProgress(10, 10);
        return null;
    }

    /**
     * @return true if we can load GUI successfully
     */
    private boolean loadGUIWhenOpeningEnv() {
        UIController.clear();

//        loadProjectStructureTree();
        MenuBarController.getMenuBarController().loadProjectStructureTree();
//        loadTestCaseNavigator();
        MenuBarController.getMenuBarController().loadTestCaseNavigator();

        // after load testcase, load probe points and regression scripts
        loadProbePoints();
        loadRegressionScripts();
//        loadUnitTestableState();
        MenuBarController.getMenuBarController().loadUnitTestableState();

//        ProjectClone.cloneEnvironment();

        // update coverage type displaying on bottom,...
        BaseSceneController.getBaseSceneController().updateInformation();

        UIController.showSuccessDialog("Loading environment is successfully", "Success",
                "The environment has been loaded");
        return true;
    }


    private void loadProbePoints() {
        ProbePointManager.getInstance().clear();
        ProbePointManager.getInstance().loadProbePoints();
        MDIWindowController.getMDIWindowController().updateLVProbePoints();
    }

    private void loadRegressionScripts() {
        RegressionScriptManager.getInstance().clear();
        RegressionScriptManager.getInstance().loadRegressionScripts();
    }

//    private void loadGUI() {
//        UIController.clear();
//
////        loadProjectStructureTree();
//        MenuBarController.getMenuBarController().loadProjectStructureTree();
////        loadTestCaseNavigator();
//        MenuBarController.getMenuBarController().loadTestCaseNavigator();
//
//        // after load testcase, load probe points
////        loadProbePoints();
////        loadUnitTestableState();
//        MenuBarController.getMenuBarController().loadUnitTestableState();
//
//        ProjectClone.cloneEnvironment();
//
//        // update coverage type displaying on bottom,...
//        BaseSceneController.getBaseSceneController().updateInformation();
//
////        new Thread(new SourcecodeDateCheckerThread()).start();
//
//        UIController.showSuccessDialog("Loading environment is successfully", "Success",
//                "The environment has been loaded");
//    }

//    private void loadUnitTestableState() {
//        List<INode> sources = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());
//
//        List<IEnvironmentNode> uuts = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroUUTNode());
//        for (IEnvironmentNode uut : uuts) {
//            for (INode source : sources) {
//                if (((EnviroUUTNode) uut).getName().equals(source.getAbsolutePath())) {
//                    ((EnviroUUTNode) uut).setUnit(source);
//                    break;
//                }
//            }
//        }
//
//        List<IEnvironmentNode> sbfs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroSBFNode());
//        for (IEnvironmentNode sbf : sbfs) {
//            for (INode source : sources) {
//                if (((EnviroSBFNode) sbf).getName().equals(source.getAbsolutePath())) {
//                    ((EnviroSBFNode) sbf).setUnit(source);
//                    break;
//                }
//            }
//        }
//
//        List<IEnvironmentNode> dontStubs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroDontStubNode());
//        for (IEnvironmentNode dontStub : dontStubs) {
//            for (INode source : sources) {
//                if (((EnviroDontStubNode) dontStub).getName().equals(source.getAbsolutePath())) {
//                    ((EnviroDontStubNode) dontStub).setUnit(source);
//                    break;
//                }
//            }
//        }
//    }

//    private void loadProjectStructureTree() {
//        if (Environment.getInstance().getProjectNode() != null) {
//            IProjectNode root = Environment.getInstance().getProjectNode();
//
//            EnvironmentRootNode environmentRootNode = Environment.getInstance().getEnvironmentRootNode();
//            List<IEnvironmentNode> nameNodes = EnvironmentSearch.searchNode(environmentRootNode, new EnviroNameNode());
//            if (nameNodes.size() == 1) {
//                root.setName(((EnviroNameNode) nameNodes.get(0)).getName());
//                UIController.loadProjectStructureTree(root);
//            } else {
//                logger.error("There are more than one name in the enviroment script. Can not export!");
//            }
//        }
//    }

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
//                        TestCaseManager.initializeMaps();
//
//                        addMenuItemDisableProperty(miViewTestCaseData,
//                                TestNewNode.class,
//                                TestNormalSubprogramNode.class,
//                                TestCompoundSubprogramNode.class,
//                                TestInitSubprogramNode.class,
//                                TestUnitNode.class,
//                                TestcaseRootNode.class
//                        );
//                        addMenuItemDisableProperty(miViewFull,
//                                TestNewNode.class,
//                                TestNormalSubprogramNode.class,
//                                TestCompoundSubprogramNode.class,
//                                TestInitSubprogramNode.class,
//                                TestUnitNode.class,
//                                TestcaseRootNode.class
//                        );
//                        addMenuItemDisableProperty(miViewTestCaseManage,
//                                TestCompoundSubprogramNode.class,
//                                TestInitSubprogramNode.class,
//                                TestUnitNode.class,
//                                TestcaseRootNode.class
//                        );
//                        addMenuItemDisableProperty(miViewCoverage,
//                                TestNewNode.class,
//                                TestNormalSubprogramNode.class,
//                                TestUnitNode.class
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

//    private void addMenuItemDisableProperty(MenuItem menuItem, ITestcaseNode... activeNodes) {
//        if (menuItem == null)
//            return;
//        try {
//            menuItem.setDisable(true);
//        } catch (Exception e) {
//
//        }
//
//        TreeTableView<ITestcaseNode> testCasesNavigator = TestCasesNavigatorController
//                .getInstance().getTestCasesNavigator();
//
//        testCasesNavigator.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
//            menuItem.disableProperty().bind(new BooleanBinding() {
//                @Override
//                protected boolean computeValue() {
//                    ObservableList<TreeItem<ITestcaseNode>> list = testCasesNavigator
//                            .getSelectionModel().getSelectedItems();
//
//                    for (TreeItem<ITestcaseNode> selectedItem : list) {
//                        boolean disable = true;
//
//                        for (ITestcaseNode node : activeNodes) {
//                            if (selectedItem.getValue().getClass().isInstance(node)) {
//                                disable = false;
//                                break;
//                            }
//                        }
//
//                        if (disable)
//                            return true;
//                    }
//
//                    return false;
//                }
//            });
//        });
//    }

//    public static Alert showDialogWhenComparingSourcecode(Alert.AlertType type, String title, String headText, String content) {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle(title);
//        alert.setHeaderText(headText);
//        alert.setContentText(content);
//        ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
//        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
//        ButtonType viewChangesButton = new ButtonType("Changes", ButtonBar.ButtonData.HELP);
//        ButtonType viewDependenciesButton = new ButtonType("Problems", ButtonBar.ButtonData.HELP);
//
//        alert.getButtonTypes().setAll(okButton, noButton, viewChangesButton, viewDependenciesButton);
//
//        // show changes
//        Button tmp = (Button) alert.getDialogPane().lookupButton(viewChangesButton);
//        tmp.addEventFilter(
//                ActionEvent.ACTION,
//                event -> {
//                    UIController.showDetailDialog(Alert.AlertType.INFORMATION, "Changes detection",
//                            "Changes",
//                            Utils.readFileContent(new WorkspaceConfig().fromJson().getFileContainingChangesWhenComparingSourcecode()));
//                    event.consume();
//                }
//        );
//
//        // show unresolved dependencies
//        Button tmp2 = (Button) alert.getDialogPane().lookupButton(viewDependenciesButton);
//        tmp2.addEventFilter(
//                ActionEvent.ACTION,
//                event -> {
//                    String unresolvedDependenciesContent = Utils.readFileContent(new WorkspaceConfig().fromJson().getFileContainingUnresolvedDependenciesWhenComparingSourcecode());
//                    UIController.showDetailDialog(Alert.AlertType.INFORMATION, "Unresolved dependencies detection",
//                            "Unresolved dependencies",
//                            unresolvedDependenciesContent);
//                    event.consume();
//                }
//        );
//
//
//        TextArea textArea = new TextArea();
//        textArea.setWrapText(true);
//        textArea.setEditable(false);
//        textArea.setMinHeight(350);
//        textArea.setText(content);
//
//        alert.getDialogPane().setContent(textArea);
//
//        return alert;
//    }
}

package com.dse.guifx_v3.controllers.object;

import auto_testcase_generation.testdatagen.RandomAutomatedTestdataGeneration;
import auto_testcase_generation.testdatagen.TestcaseExecution;
import com.dse.config.FunctionConfig;
import com.dse.config.FunctionConfigDeserializer;
import com.dse.config.FunctionConfigSerializer;
import com.dse.config.WorkspaceConfig;
import com.dse.debugger.controller.DebugController;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.exception.OpenFileException;
import com.dse.guifx_v3.controllers.CompoundTestCaseTreeTableViewController;
import com.dse.guifx_v3.controllers.TestCasesNavigatorController;
import com.dse.guifx_v3.controllers.main_view.BaseSceneController;
import com.dse.guifx_v3.controllers.main_view.MDIWindowController;
import com.dse.guifx_v3.controllers.main_view.MenuBarController;
import com.dse.guifx_v3.helps.*;
import com.dse.guifx_v3.objects.CheckBoxTreeTableRow;
import com.dse.guifx_v3.objects.TestCasesTreeItem;
import com.dse.guifx_v3.objects.popups.SomeTestCasesAreNotSuccessPopupController;
import com.dse.parser.object.*;
import com.dse.project_init.ProjectClone;
import com.dse.report.FullReport;
import com.dse.report.IReport;
import com.dse.report.ReportManager;
import com.dse.report.TestCaseManagementReport;
import com.dse.search.Search2;
import com.dse.testcase_execution.TestCaseExecutionThread;
import com.dse.testcase_manager.*;
import com.dse.testcasescript.SelectionUpdater;
import com.dse.testcasescript.TestcaseSearch;
import com.dse.testcasescript.object.*;
import com.dse.testdata.object.IDataNode;
import com.dse.thread.AkaThread;
import com.dse.thread.AkaThreadManager;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// For context menu of test case navigator (when right click)
public class TestCasesNavigatorRow extends CheckBoxTreeTableRow<ITestcaseNode> {
    private final static AkaLogger logger = AkaLogger.get(TestCasesNavigatorRow.class);

    public TestCasesNavigatorRow() {
        super();
        setEventWhenClickCheckbox();
    }

    private void setEventWhenClickCheckbox() {
        // When we click the checkbox of an item, we need to update the selected state of its children
        // Two case: (1) the children are expanded before, (2) the children are not expanded
        getCheckBox().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                TestCasesTreeItem treeItem = (TestCasesTreeItem) getTreeItem();
                treeItem.loadChildren(true);

                if (treeItem.isSelected()) {
                    SelectionUpdater.check(treeItem.getValue());
                } else {
                    SelectionUpdater.uncheck(treeItem.getValue());
                }

                // when we select in tree, it may trigger menubar. Some menu items may be activated.
                MenuBarController.getMenuBarController().refresh();
            }
        });
    }


    @Override
    public void updateItem(ITestcaseNode item, boolean empty) {
        super.updateItem(item, empty);
        if (getTreeItem() != null && getTreeItem().getValue() != null) {
            // initialize popup
            setContextMenu(new ContextMenu());
            TestCasesTreeItem treeItem = (TestCasesTreeItem) getTreeItem();

            // update the selected status
            treeItem.setSelected(treeItem.getValue().isSelectedInTestcaseNavigator());

            // add options to popup
            ObservableList<TreeItem<ITestcaseNode>> list = getTreeTableView().getSelectionModel().getSelectedItems();
            if (list.contains(getTreeItem()) && list.size() > 1) {
                addMenuItemForMultiSelection(item);
            } else {
                addMenuItemsForSingleSelection(item, treeItem);
            }
        } else {
            setContextMenu(null);
        }
    }

    private void addMenuItemsForSingleSelection(ITestcaseNode node, TestCasesTreeItem item) {
        if (node instanceof TestInitSubprogramNode) {
            addSelectAllChildren(node);
            addDeselectAllChildren(node);

        } else if (node instanceof TestNormalSubprogramNode) {
            addInsertTestCase(node, item);
            addExecute(node);
            addCreatePrototypeForTemplateAndMacroFunction(node, item);
            addViewDependency(node);
//                addInsertMinMidMaxOption(node, item);
//                addInsertBasisPathTestCasesOption(node);
//            addExecuteOption(node);
            addOpenSourceOption(node);
            addDeleteOption(node);
//                addExpandAllChildrenOption(node);
//                addCollapseAllChildrenOption(node);
//                addDeselectCoverageOption(node);
            addConfigureFunction(node);
            addAutomatedTestdataGenerationOptionForAFunction(node, item);
            addStopAutomatedTestdataGenerationOption(node, item);
            addViewTestCasesExecution(node);
            addSelectAllChildren(node);
            addDeselectAllChildren(node);
            addResetAllFunctionConfigToDefault(node);
            addDeleteMultiTestCases(node);
            addDeleteMultiPrototypes(node);

        } else if (node instanceof TestCompoundSubprogramNode) {
            addInsertTestCase(node, item);
            addExecute(node);
//            addExecuteOption(node);
            addDeleteOption(node);
            addExpandAllChildrenOption(node);
            addCollapseAllChildrenOption(node);
            addDeselectCoverageOption(node);
            addViewTestCaseManagementReport(node);
            addSelectAllChildren(node);
            addDeselectAllChildren(node);

        } else if (node instanceof TestUnitNode) {
            addOpenSourceOption(node);
            addExecute(node);
            addOpenSourceInDebugMode(node);
            addViewCoverageOptionToAFile(node, item);
            addViewDependency(node);
            addViewTestCaseManagementReport(node);
            addViewInstrumentedSourcecodeOption(node, item);
            addGenerateTestdataAutomaticallyOptionForAnUnit(node, item);
            addStopAutomatedTestdataGenerationOption(node, item);
            addSelectAllChildren(node);
            addDeselectAllChildren(node);
            addResetAllFunctionConfigToDefault(node);
            addDeleteMultiTestCases(node);
            addDeleteMultiPrototypes(node);

        } else if (node instanceof TestNewNode) {
            TestNewNode cast = (TestNewNode) node;
            if (cast.getParent() instanceof TestCompoundSubprogramNode) {
                addOpenTestCase(cast);
                addAddToCompound(cast);
                addDuplicateTestCase(cast, item);
                addDeleteTestCase(cast, item);
                addExecuteTestCase(cast);
                addExecuteTestCaseWithDebugMode(cast);
                addGenerateTestCaseReport(cast);
                addViewTestCaseJson(cast);

            } else if (cast.getParent() instanceof TestNormalSubprogramNode) {
                if (cast.isPrototypeTestcase()) {
                    addOpenPrototype(cast);
                    addDeletePrototype(cast, item);
                    addGenerateTestdataAutomaticallyForPrototype(cast, item);
                    addViewTestCaseJson(cast);

                } else {
                    addOpenTestCase(cast);
                    addAddToCompound(cast);
                    addDuplicateTestCase(cast, item);
                    addDeleteTestCase(cast, item);
                    addExecuteTestCase(cast);
                    addExecuteTestCaseWithDebugMode(cast);
                    addDebugAndGetLog(cast);
                    addGenerateTestCaseReport(cast);
                    addViewReport((TestNewNode) node);
                    viewTestdriver(cast);
                    viewTestpath(cast);
                    viewCommands(cast);
                    addViewCoverageOptionToATestcase(node, item);
                    addViewTestCaseJson(cast);
                }
            }
        } else if (node instanceof TestcaseRootNode) {
            addTurnOnViewCoverageMode(node);
            addExecute(node);
            addSelectCoverage(node);
            addViewTestCaseManagementReport(node);
            addGenerateTestdataAutomaticallyOptionForAnUnit(node, item);
            addStopAutomatedTestdataGenerationOption(node, item);
            addSelectAllChildren(node);
            addDeselectAllChildren(node);
            addDeleteMultiTestCases(node);
            addDeleteMultiPrototypes(node);
            addResetAllFunctionConfigToDefault(node);
        }
    }

    private void addMenuItemForMultiSelection(ITestcaseNode node) {
        addExecuteMultiple(node);
        addDeleteMultiTestCases(node);
        addDeleteMultiPrototypes(node);
    }

    // By default, users can select the type of code coverage when creating the environment.
    // However, users can change the type of code coverage later by using this option.
    // IMPORTANT:
    // All the information of the current environment will be deleted before switching the new type of code coverage.
    private void addSelectCoverage(ITestcaseNode node) {
        if (node instanceof TestcaseRootNode) {
            Menu miSelectCoverage = new Menu("Select coverage");
            getContextMenu().getItems().add(miSelectCoverage);

            // statement coverage
            {
                MenuItem miSelectStatementCov = new MenuItem("Statement coverage");
                miSelectCoverage.getItems().add(miSelectStatementCov);
                miSelectStatementCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.STATEMENT));
            }

            // branch coverage
            {
                MenuItem miSelectBranchCov = new MenuItem("Branch coverage");
                miSelectCoverage.getItems().add(miSelectBranchCov);
                miSelectBranchCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.BRANCH));
            }

            // mcdc coverage
            {
                MenuItem miSelectMcdcCov = new MenuItem("MCDC coverage");
                miSelectCoverage.getItems().add(miSelectMcdcCov);
                miSelectMcdcCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.MCDC));
            }

            // basis path coverage
            {
                MenuItem miSelectBasisPathCov = new MenuItem("Basis path coverage");
                miSelectCoverage.getItems().add(miSelectBasisPathCov);
//                    miSelectBasisPathCov.setDisable(true);
                miSelectBasisPathCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.BASIS_PATH));
            }

            // Statement + branch coverage coverage
            {
                MenuItem miSelectStatementAndBranchCov = new MenuItem("Statement + branch coverage");
                miSelectCoverage.getItems().add(miSelectStatementAndBranchCov);
//                    miSelectStatementAndBranchCov.setDisable(true);
                miSelectStatementAndBranchCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.STATEMENT_AND_BRANCH));
            }
            // Statement + mcdc coverage coverage
            {
                MenuItem miSelectStatementAndMcdcCov = new MenuItem("Statement + mcdc coverage");
//                    miSelectStatementAndMcdcCov.setDisable(true);
                miSelectCoverage.getItems().add(miSelectStatementAndMcdcCov);
                miSelectStatementAndMcdcCov.setOnAction(event ->
                        updateCoverage(EnviroCoverageTypeNode.STATEMENT_AND_MCDC));
            }
        }
    }

    private void updateCoverage(String coverage) {
        if (coverage.equals(Environment.getInstance().getTypeofCoverage())) {
            UIController.showSuccessDialog("The current coverage is " + coverage + ". There is no change!", "Cover coverage configuration", "No change to the environment");
        } else {
            // show a dialog to confirm
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning");
            alert.setHeaderText("Change coverage");
            alert.setContentText("All the information related to the current environment will be deleted. Only test cases are kept. Do you want to continue?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                // if users decide to change code coverage

                // update the environment script
                List<IEnvironmentNode> enviroNodes = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroCoverageTypeNode());
                if (enviroNodes.size() == 1) {
                    EnviroCoverageTypeNode covNode = (EnviroCoverageTypeNode) enviroNodes.get(0);
                    covNode.setCoverageType(coverage);
                    Environment.getInstance().saveEnvironmentScriptToFile();

                    // update windows
                    BaseSceneController.getBaseSceneController().updateInformation();
                    MDIWindowController.getMDIWindowController().removeViewsAfterChangeCoverageType();

                    logger.debug("[" + Thread.currentThread().getName() + "] " + "Update the environment script " + Environment.getInstance().getEnvironmentRootNode().getEnvironmentScriptPath());
                }

                // delete all information of test cases
                WorkspaceConfig workspaceConfig = new WorkspaceConfig().fromJson();
                Utils.deleteFileOrFolder(new File(workspaceConfig.getCoverageDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getDebugDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getExecutableFolderDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getTestcaseCommandsDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getTestDriverDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getReportDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getTestpathDirectory()));
                Utils.deleteFileOrFolder(new File(workspaceConfig.getExecutionResultDirectory()));

                // delete all test driver of test cases inside the testing project
                //deleteAkaFile(ITestCase.AKA_SIGNAL, new File(workspaceConfig.getTestingProject()));

//                    new ProjectClone.CloneThread().start();
            } else {
                // nothing to do
            }
        }
    }

    /**
     * Delete all files which contains a signal in its name
     *
     * @param akaSignal
     * @param root
     */
    private void deleteAkaFile(String akaSignal, File root) {
        for (File f : root.listFiles())
            if (f.getName().contains(akaSignal))
                f.delete();
            else if (f.isDirectory()) {
                deleteAkaFile(akaSignal, f);
            }
    }

    private void addViewTestCaseManagementReport(ITestcaseNode node) {
        MenuItem menuItem = new MenuItem("View Test Case Management Report");
        getContextMenu().getItems().add(menuItem);

        menuItem.setOnAction(event -> {
            IReport report = new TestCaseManagementReport(Collections.singletonList(node), LocalDateTime.now());
            ReportManager.export(report);

            // display on MDIWindow
            MDIWindowController.getMDIWindowController().viewReport(Environment.getInstance().getName(), report.toHtml());

        });
    }

    private void addTurnOnViewCoverageMode(ITestcaseNode node) {
        MenuItem miActive = new MenuItem();
        getContextMenu().getItems().add(miActive);
        boolean isActive = Environment.getInstance().isCoverageModeActive();
        if (isActive) {
            miActive.setText("Turn Off Coverage Mode / MultiSelection");
        } else {
            miActive.setText("Turn On Coverage Mode / MultiSelection");
        }
        miActive.setOnAction(event -> {
            if (isActive) {
                SelectionUpdater.reset(Environment.getInstance().getTestcaseScriptRootNode());

                Environment.getInstance().setCoverageModeActive(false);
                miActive.setText("Turn On Coverage Mode / MultiSelection");
            } else {
                SelectionUpdater.reset(Environment.getInstance().getTestcaseScriptRootNode());

                Environment.getInstance().setCoverageModeActive(true);
                miActive.setText("Turn Off Coverage Mode / MultiSelection");
            }
            TestCasesNavigatorController.getInstance().refreshNavigatorTree();
        });
    }

    private void addAddToCompound(TestNewNode testNewNode) {
        MenuItem miAddToCompound = new MenuItem("Add this test case to the opening test compound");
        miAddToCompound.setOnAction(event -> {
            CompoundTestCaseTreeTableViewController controller = Environment.getInstance().getCurrentTestcompoundController();
            if (controller == null) {
                UIController.showErrorDialog("You need to open a test compound first", "Add test case to test compound failed", "Test compound generation");
            } else {

//                    // find the clicking test case
//                    ITestcaseNode parent = testNewNode.getParent();
//                    List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
//                    if (names.size() == 1) {
//                        String name = ((TestNameNode) names.get(0)).getName();
//                        if (parent instanceof TestNormalSubprogramNode) {
//                            TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
//                            if (testCase != null) {
//                                logger.debug("The clicking test case = " + testCase.getName());
//                                TestCaseSlot testcaseSlot = new TestCaseSlot(testCase);
//                                openingCompoundTestcase.getSlots().add(testcaseSlot);
//                                TestCaseManager.exportCompoundTestCaseToFile(openingCompoundTestcase);
//                                logger.debug("Saving " + openingCompoundTestcase.getName() +
//                                        " in " + openingCompoundTestcase.getPath());
//                            }
//                        }
//                    }
                List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
                if (names.size() == 1) {
                    String name = ((TestNameNode) names.get(0)).getName();
                    controller.addSlot(name);
                }
            }
        });
        getContextMenu().getItems().add(miAddToCompound);
    }

    private void addExecuteMultiple(ITestcaseNode node) {
//        Menu miExecMulti = new Menu("Execute all selected test cases");
//        getContextMenu().getItems().add(miExecMulti);

//        MenuItem miExecMultiWithoutGoogleTest = new MenuItem("Without google test");
//        miExecMulti.getItems().add(miExecMultiWithoutGoogleTest);
//        miExecMultiWithoutGoogleTest.setOnAction(event -> {
//            executeMultiTestcase(TestcaseExecution.IN_EXECUTION_WITHOUT_GTEST_MODE);
//        });

        MenuItem miExecMultiWithGoogleTest = new MenuItem("Execute all selected test cases");
        getContextMenu().getItems().add(miExecMultiWithGoogleTest);
        miExecMultiWithGoogleTest.setOnAction(event -> {
            executeMultiTestcase(TestcaseExecution.IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE);
        });
    }

    private void functionNodeNotFoundExeptionHandle(FunctionNodeNotFoundException fe) {
        UIController.showErrorDialog(
                "Does not find the function " + fe.getFunctionPath(),
                "Error", "Not found");
        logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
    }

    private void addExecute(ITestcaseNode node) {
        MenuItem miExecMulti = new MenuItem("Execute all selected test cases");
        getContextMenu().getItems().add(miExecMulti);

        miExecMulti.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    List<ITestcaseNode> selectedTestcases = SelectionUpdater.getAllSelectedTestcases(node);
                    if (selectedTestcases.size() == 0) {
                        UIController.showErrorDialog("You must select at least one test case", "Test case execution", "Error");
                    } else {
                        Alert confirmAlert = UIController.showYesNoDialog(Alert.AlertType.CONFIRMATION, "Test case execution", "Confirmation",
                                "You select " + selectedTestcases.size() + " test cases. Do you want to execute them all?");
                        Optional<ButtonType> option = confirmAlert.showAndWait();
                        if (option.get() == ButtonType.YES) {
                            for (ITestcaseNode selectedTc : selectedTestcases)
                                if (selectedTc instanceof TestNameNode) {
                                    if (!(((TestNewNode) selectedTc.getParent()).isPrototypeTestcase()))
                                        executeTestcaseInASpecifiedMode((TestNewNode) selectedTc.getParent()
                                                , TestcaseExecution.IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE);
                                }
                        } else {
                            confirmAlert.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addResetAllFunctionConfigToDefault(ITestcaseNode node) {
        MenuItem miDeleteMulti = new MenuItem("Delete all selected function configs");
        getContextMenu().getItems().add(miDeleteMulti);

        miDeleteMulti.setOnAction(event -> {
            try {
                List<ITestcaseNode> selectedFunctions = SelectionUpdater.getAllSelectedFunctions(node);
                for (ITestcaseNode selectedFunction : selectedFunctions)
                    if (selectedFunction instanceof TestSubprogramNode) {
                        ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestSubprogramNode) selectedFunction).getName());

                        String functionConfigPath = new WorkspaceConfig().fromJson().getFunctionConfigDirectory() + File.separator + functionNode.getNameOfFunctionConfigJson() + ".json";
                        if (new File(functionConfigPath).exists()) {
                            Utils.deleteFileOrFolder(new File(functionConfigPath));
                        }
                        functionNode.setFunctionConfig(null);
                    }

                UIController.showSuccessDialog("Delete all function configs of selected functions successfully",
                        "Success", "Delete function config");
            } catch (FunctionNodeNotFoundException fe) {
                fe.printStackTrace();
                UIController.showErrorDialog("Raise error when deleting function configs of selected functions" +
                                "\nFunctionNodeNotFound " + fe.getFunctionPath(),
                        "Fail", "Delete function config");
            } catch (Exception e) {
                e.printStackTrace();
                UIController.showErrorDialog("Raise error when deleting function configs of selected functions",
                        "Fail", "Delete function config");
            }
        });
    }

    private void deleteTestCase(TestNewNode testNewNode, TestCasesTreeItem item) {
        TestCasesNavigatorController.getInstance().deleteTestCase(testNewNode, item);
    }

    private void addDeleteMultiPrototypes(ITestcaseNode node) {
        MenuItem miDeleteMulti = new MenuItem("Delete all selected prototypes");
        getContextMenu().getItems().add(miDeleteMulti);

        miDeleteMulti.setOnAction(event -> {
            try {
                // get all prototypes
                List<TreeItem<ITestcaseNode>> treeItems = new ArrayList<>();
                for (TreeItem<ITestcaseNode> treeItem : getTreeTableView().getSelectionModel().getSelectedItems()) // return the index where the mouse is clicked
                    if (treeItem.getValue().isSelectedInTestcaseNavigator()) { // just check to confirm
                        addTreeItemContainsTestCaseToList(treeItems, treeItem);
                    }

                for (TreeItem<ITestcaseNode> treeItem : treeItems)
                    if (treeItem.getValue() instanceof TestNewNode) {
                        TestNewNode newNode = (TestNewNode) treeItem.getValue();
                        if (newNode.isPrototypeTestcase())
                            deleteTestCase(newNode, (TestCasesTreeItem) treeItem);
                    }
                UIController.showSuccessDialog("All selected prototypes are deleted", "Delete prototypes", "Success");
            } catch (Exception e) {
                e.printStackTrace();
                UIController.showErrorDialog("There occurs problem while deleting selected prototypes", "Delete prototypes", "Success");
            }
        });
    }

    private void addDeleteMultiTestCases(ITestcaseNode node) {
        MenuItem miDeleteMulti = new MenuItem("Delete all test cases in selection");
        getContextMenu().getItems().add(miDeleteMulti);

        miDeleteMulti.setOnAction(event -> {
            try {
                // get all test cases
                List<TreeItem<ITestcaseNode>> treeItems = new ArrayList<>();
                for (TreeItem<ITestcaseNode> treeItem : getTreeTableView().getSelectionModel().getSelectedItems()) {
                    addTreeItemContainsTestCaseToList(treeItems, treeItem);
                }

                for (TreeItem<ITestcaseNode> treeItem : treeItems)
                    if (treeItem.getValue() instanceof TestNewNode) {
                        TestNewNode newNode = (TestNewNode) treeItem.getValue();
                        if (!(newNode.isPrototypeTestcase()))
                            deleteTestCase(newNode, (TestCasesTreeItem) treeItem);
                    }
                UIController.showSuccessDialog("All selected test cases are deleted", "Delete test cases", "Success");
            } catch (Exception e) {
                e.printStackTrace();
                UIController.showErrorDialog("There occurs problem while deleting selected test cases", "Delete test cases", "Success");
            }

        });
    }

    private void executeMultiTestcase(int mode) {
        // get all test cases
        List<TestNewNode> testNewNodes = new ArrayList<>();
        for (TreeItem<ITestcaseNode> treeItem : getTreeTableView().getSelectionModel().getSelectedItems()) {
            addTestCaseToList(testNewNodes, treeItem.getValue());
        }
        logger.debug("[" + Thread.currentThread().getName() + "] " + "You are requesting to execute " + testNewNodes.size() + " test cases");

        // put test cases in different threads
        List<TestCaseExecutionThread> tasks = new ArrayList<>();
        List<TestCase> testCases = new ArrayList<>();
        for (TestNewNode testNewNode : testNewNodes) {
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
            if (names.size() == 1) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(((TestNameNode) names.get(0)).getName());
                testCases.add(testCase);

                if (testCase != null) {
                    testCase.deleteOldDataExceptValue();
                    TestCaseExecutionThread executionThread = new TestCaseExecutionThread(testCase);
                    executionThread.setExecutionMode(mode);
                    tasks.add(executionThread);
                }
            }
        }
        logger.debug("[" + Thread.currentThread().getName() + "] " + "Create " + tasks.size() + " threads to execute " + tasks.size() + " test cases");

        // add these threads to executors
        // at the same time, we do not execute all of the requested test cases.
        int MAX_EXECUTING_TESTCASE = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_EXECUTING_TESTCASE);
        for (TestCaseExecutionThread task : tasks)
            executorService.execute(task);
        executorService.shutdown();

        UIController.viewCoverageOfMultipleTestcases("Summary Coverage Report", testCases);
    }

    private void addTestCaseToList(List<TestNewNode> testNewNodes, ITestcaseNode selectedNode) {
        if (selectedNode instanceof TestNewNode) {
            if (!testNewNodes.contains(selectedNode)) {
                testNewNodes.add((TestNewNode) selectedNode);
            }
        } else {
            for (ITestcaseNode child : selectedNode.getChildren()) {
                addTestCaseToList(testNewNodes, child);
            }
        }
    }

    private void addTreeItemContainsTestCaseToList(List<TreeItem<ITestcaseNode>> treeItems, TreeItem<ITestcaseNode> item) {
        if (item.getValue() instanceof TestNewNode) {
            if (!treeItems.contains(item)) {
                treeItems.add(item);
            }
        } else {
            for (TreeItem<ITestcaseNode> child : item.getChildren()) {
                addTreeItemContainsTestCaseToList(treeItems, child);
            }
        }
    }

    private void viewCommands(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("View command of this test case (after execution)");
        getContextMenu().getItems().add(mi);

        mi.setOnAction(event -> {
            ITestcaseNode parent = testNewNode.getParent();
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

            if (names.size() == 1) {
                String name = ((TestNameNode) names.get(0)).getName();
                if (name != null && name.length() > 0 && parent instanceof TestNormalSubprogramNode) {
                    // find the corresponding source code file
                    File commandFile = new File(new WorkspaceConfig().fromJson().getTestcaseCommandsDirectory() + File.separator + name + ".json");
                    if (commandFile != null) {
                        try {
                            Utils.openFolderorFileOnExplorer(commandFile.getAbsolutePath());
                        } catch (OpenFileException e) {
                            UIController.showErrorDialog("The command file of this test case is not found", "Open command file", "Not found");
                        }
                    } else {
                        UIController.showErrorDialog("The command file of this test case is not found", "Open command file", "Not found");
                    }
                }
            }
        });
    }

    private void viewTestpath(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("View test path (after execution)");
        getContextMenu().getItems().add(mi);

        mi.setOnAction(event -> {
            ITestcaseNode parent = testNewNode.getParent();
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

            if (names.size() == 1) {
                String name = ((TestNameNode) names.get(0)).getName();
                if (name != null && name.length() > 0 && parent instanceof TestNormalSubprogramNode) {
                    // find the corresponding source code file
                    File testpathFile = new File(new WorkspaceConfig().fromJson().getTestpathDirectory() + File.separator + name + ".tp");
                    if (testpathFile != null) {
                        try {
                            Utils.openFolderorFileOnExplorer(testpathFile.getAbsolutePath());
                        } catch (OpenFileException e) {
                            UIController.showErrorDialog("The test path of this test case is not found", "Open test path", "Not found");
                        }
                    } else {
                        UIController.showErrorDialog("The test path of this test case is not found", "Open test path", "Not found");
                    }
                }
            }
        });
    }

    private void viewTestdriver(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("View test driver (after execution)");
        getContextMenu().getItems().add(mi);

        mi.setOnAction(event -> {

            ITestcaseNode parent = testNewNode.getParent();
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

            if (names.size() == 1) {
                String name = ((TestNameNode) names.get(0)).getName();
                if (name != null && name.length() > 0 && parent instanceof TestNormalSubprogramNode) {
                    TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);

                    // find the function
//                        IFunctionNode function = searchFunctionNodeByPath(((TestNormalSubprogramNode) testNewNode.getParent()).getName(), Environment.getProjectNode());
//                        INode sourcecodeFile = Utils.getSourcecodeFile(function);
//                        String nameOfSourcecodeFile = sourcecodeFile.getName().substring(0, sourcecodeFile.getName().indexOf("."));
//                        String comparedName = nameOfSourcecodeFile + ITestCase.AKA_SIGNAL + name + ".";
//
//                        // find the corresponding source code file
//                        File f = new File(new WorkspaceConfig().fromJson().getTestDriverDirectory());
//                        Collection<File> files = listFileTree(f);
//                        File testdriver = null;
//                        for (File file : files)
//                            if (file.getName().contains(comparedName)) {
//                                testdriver = file;
//                                break;
//                            }
//                        if (testdriver != null) {
                    try {
                        Utils.openFolderorFileOnExplorer(testCase.getSourceCodeFile());
                    } catch (OpenFileException e) {
                        UIController.showErrorDialog("The test driver of this test case is not found", "Open test driver", "Not found");
                    }
//

                }
            }
        });
    }

    private void addExecuteTestCase(TestNewNode testNewNode) {
        // execute in google test mode
        MenuItem miExecute = new MenuItem("Execute");
        getContextMenu().getItems().add(miExecute);
        miExecute.setOnAction(event -> {
            executeTestcaseInASpecifiedMode(testNewNode, TestcaseExecution.IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE);
        });

        // execute without google test mode
//        MenuItem miExecuteWithoutGoogleTestMode = new MenuItem("In normal mode");
//        miExecuteWithoutGoogleTestMode.setDisable(true);
//        mi.getItems().add(miExecuteWithoutGoogleTestMode);
//        executeTestcaseInASpecifiedMode(miExecuteWithoutGoogleTestMode, testNewNode, TestcaseExecution.IN_EXECUTION_WITHOUT_GTEST_MODE);
    }

    private void executeTestcaseInASpecifiedMode(TestNewNode testNewNode, int mode) {
        String name = testNewNode.getName();
        if (name == null || name.length() == 0)
            return;

        ITestCase testCase = null;
        ITestcaseNode parent = testNewNode.getParent();
        if (parent instanceof TestNormalSubprogramNode) {
            try {
                ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) parent).getName());
                logger.debug("[" + Thread.currentThread().getName() + "] " + "The function corresponding to the clicked node: " + function.getAbsolutePath());
                testCase = TestCaseManager.getBasicTestCaseByName(name);

                if (testCase != null)
                    ((TestCase) testCase).setFunctionNode(function);
            } catch (FunctionNodeNotFoundException fe) {
                fe.printStackTrace();
                logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
            }

        } else if (parent instanceof TestCompoundSubprogramNode) {
            testCase = TestCaseManager.getCompoundTestCaseByName(name);
        }

        if (testCase != null) {
            testCase.deleteOldDataExceptValue();
            TestCaseExecutionThread task = new TestCaseExecutionThread(testCase);
            task.setExecutionMode(mode);
            AkaThreadManager.executedTestcaseThreadPool.execute(task);
//            new AkaThread(task).start();
        }
    }


    private void addConfigureFunction(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode) {
            MenuItem mi = new MenuItem("Configure Function");
            getContextMenu().getItems().add(mi);

            mi.setOnAction(event -> {
                try {
                    ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) node).getName());
                    logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
                    MDIWindowController.getMDIWindowController().viewFunctionConfiguration(function);

                } catch (FunctionNodeNotFoundException fe) {
                    UIController.showErrorDialog(
                            "Does not find the function " + ((TestNormalSubprogramNode) node).getName(),
                            "Error", "Not found");
                    logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
                }
            });
        }
    }

    private void addViewTestCasesExecution(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode) {
            MenuItem mi = new MenuItem("View Test Cases Execution");
            getContextMenu().getItems().add(mi);

            mi.setOnAction(event -> {
                try {
                    ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) node).getName());
                    logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
                    MDIWindowController.getMDIWindowController().viewTestCasesExecution(function);
                } catch (FunctionNodeNotFoundException fe) {
                    UIController.showErrorDialog(
                            "Does not find the function " + ((TestNormalSubprogramNode) node).getName(),
                            "Error", "Not found");
                    logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
                    logger.debug("The function not found || macro function");
                }

            });
        }
    }

    /**
     * The function is a template function or
     * a normal function (not a template function)
     *
     * @param selectedFunction the function need to generate test cases
     * @param item             Testcase tree item for updating test cases navigator tree
     */
    private void generateTestdataAutomaticallyForFunction(TestNormalSubprogramNode selectedFunction, TestCasesTreeItem item) {
        try {
            ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(selectedFunction.getName());
            if (functionNode.isTemplate() || functionNode instanceof MacroFunctionNode) {
                generateTestcaseAutomaticallyForTemplateFunction(functionNode, false, item);
            } else {
                generateTestcaseAutomaticallyForNormalFunction(functionNode, false, item, null);
            }
        } catch (FunctionNodeNotFoundException fe) {
            UIController.showErrorDialog(
                    "Does not find the function " + fe.getFunctionPath(),
                    "Error", "Not found");
            logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * For a function
     *
     * @param subprogramNode TestNormalSubprogramNode
     * @param item           for updating test cases navigator tree
     */
    private void addAutomatedTestdataGenerationOptionForAFunction(ITestcaseNode subprogramNode, TestCasesTreeItem item) {
        if (subprogramNode instanceof TestNormalSubprogramNode) {
            MenuItem mi = new MenuItem("Generate test data automatically");
            getContextMenu().getItems().add(mi);

            mi.setOnAction(event -> {
                try {
                    ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) subprogramNode).getName());
                    logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
                    if (
                        //function instanceof ConstructorNode || function instanceof DestructorNode ||
                            function instanceof MacroFunctionNode) {
                        UIController.showErrorDialog("Do not support to generate test case automatically for this kind of function",
                                "Automated test case generation failure", "Do not support");

                    } else if (function instanceof IFunctionNode)
                        generateTestdataAutomaticallyForFunction((TestNormalSubprogramNode) subprogramNode, item);
                } catch (FunctionNodeNotFoundException fe) {
                    UIController.showErrorDialog(
                            "Does not find the function " + fe.getFunctionPath(),
                            "Error", "Not found");
                    logger.error("FunctionNodeNotFound: " + fe.getFunctionPath());
                }
            });
        }
    }

    private void generateTestcaseAutomaticallyForTemplateFunction(ICommonFunctionNode functionNode, boolean shouldViewTestCaseExecution, TestCasesTreeItem item) {
        // if a function is a template function, we will generate test cases for all prototypes
        String templatePath = functionNode.getTemplateFilePath();
        if (!(new File(templatePath).exists())) {
            UIController.showErrorDialog("You must create a prototype for function " + functionNode.getAbsolutePath() + " before generating test data automatically.\n" +
                            "Aka generates test data for these prototypes",
                    "Autogen for a function", "Fail");
            return;
        }

        List<TestCase> prototypes = RandomAutomatedTestdataGeneration.getAllPrototypesOfTemplateFunction((IFunctionNode) functionNode);
        if (prototypes.size() == 0)
            UIController.showErrorDialog("Not find out any prototypes of this template function. Please right click > Insert a prototype",
                    "Autogen for a function", "Fail");
        else {
            for (TestCase selectedPrototype : prototypes) {
                UILogger.getUiLogger().logToBothUIAndTerminal("Prototype: " + selectedPrototype.getName());
                generateTestcaseAutomaticallyForNormalFunction(functionNode, false, item, selectedPrototype);
            }
        }
    }

    /**
     * @param functionNode                ICommonFunctionNode
     * @param shouldViewTestCaseExecution true if we want to display test case execution tab, false if otherwise
     * @param item                        TestCasesTreeItem
     * @param selectedPrototype           a prototype of a template function (used to generate test data for a specific prototype of a template function)
     */
    private void generateTestcaseAutomaticallyForNormalFunction(ICommonFunctionNode functionNode, boolean shouldViewTestCaseExecution, TestCasesTreeItem item
            , TestCase selectedPrototype) {
        UILogger uiLogger = UILogger.getUiLogger();
        Platform.runLater(() -> BaseSceneController.getBaseSceneController().showMessagesPane());
        uiLogger.info("Start generate test cases for function: " + functionNode.getSingleSimpleName());

        if (functionNode.isTemplate() && selectedPrototype == null)
            return;
        if (functionNode instanceof MacroFunctionNode && selectedPrototype == null)
            return;
        // refresh executions
        TCExecutionDetailLogger.clearExecutions(functionNode);

        MDIWindowController.getMDIWindowController().removeTestCasesExecutionTabByFunction(functionNode);
        TCExecutionDetailLogger.initFunctionExecutions(functionNode);

        uiLogger.info("Getting function configuration...");
        FunctionConfig functionConfig = null;

        // search for the function config file of the current function node
        String functionConfigDir = new WorkspaceConfig().fromJson().getFunctionConfigDirectory() +
                File.separator + functionNode.getNameOfFunctionConfigJson() + ".json";
        if (new File(functionConfigDir).exists()) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(FunctionConfig.class, new FunctionConfigDeserializer());
            Gson customGson = gsonBuilder.create();
            functionConfig = customGson.fromJson(Utils.readFileContent(functionConfigDir), FunctionConfig.class);
            functionNode.setFunctionConfig(functionConfig);
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Loading the function config of " + functionNode.getAbsolutePath() + ": " + functionNode.getAbsolutePath());

        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Create new function config of " + functionNode.getAbsolutePath());
            functionConfig = new WorkspaceConfig().fromJson().getDefaultFunctionConfig();
            functionConfig.setFunctionNode(functionNode);
            functionNode.setFunctionConfig(functionConfig);
            functionConfig.createBoundOfArgument(functionConfig, functionNode);

            //
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(FunctionConfig.class, new FunctionConfigSerializer());
            Gson gson = builder.setPrettyPrinting().create();
            String json = gson.toJson(functionConfig, FunctionConfig.class);

            String functionConfigFile = new WorkspaceConfig().fromJson().getFunctionConfigDirectory() + File.separator +
                    functionConfig.getFunctionNode().getNameOfFunctionConfigJson() + ".json";
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Export the config of function " + functionConfig.getFunctionNode().getAbsolutePath() + " to " + functionConfigFile);
            Utils.writeContentToFile(json, functionConfigFile);
        }

        // GUI logic
        item.setExpanded(true);

        logger.debug("[" + Thread.currentThread().getName() + "] " + "Create new thread to run the test data");
        GenerateTestdataTask task = new GenerateTestdataTask();
        task.setSelectedPrototype(selectedPrototype);
        task.setFunction(functionNode);
        task.setTreeNodeInTestcaseNavigator(item);
        AkaThread thread = new AkaThread(task);
        thread.setName(functionNode.getSimpleName());
        AkaThreadManager.akaThreadList.add(thread);
        AkaThreadManager.autoTestdataGenForSrcFileThreadPool.execute(thread);

//        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
//            @Override
//            public void handle(WorkerStateEvent event) {
//                try {
//                    List<AutoGeneratedTestCaseExecTask> list = task.getTestCaseExecTask();
//
//                    EventHandler<WorkerStateEvent> handler = new EventHandler<WorkerStateEvent>() {
//                        private int remainingTasks = list.size();
//
//                        @Override
//                        public void handle(WorkerStateEvent event) {
//                            if ((--remainingTasks) == 0) {
//                                try {
//                                    UIController.viewCoverageOfMultipleTestcasesFromAnotherThread(functionNode.getName(), task.get());
//                                } catch (Exception ex) {
//                                    ex.printStackTrace();
//                                }
//                            }
//                        }
//                    };
//
//                    List<TestCase> testCases = list.stream()
//                            .map(AutoGeneratedTestCaseExecTask::getTestCase)
//                            .collect(Collectors.toList());
//
//                    UIController.viewCoverageOfMultipleTestcasesFromAnotherThread(functionNode.getName(), testCases);
//
//                    if (list.size() > 0) {
//                        ExecutorService executorService = Executors.newFixedThreadPool(list.size());
//                        list.forEach(t -> {
//                            t.setOnSucceeded(handler);
//                            executorService.submit(t);
//                        });
//                        executorService.shutdown();
//                    }
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
    }

    private TestCase getTestCaseByTestNewNode(TestNewNode testNewNode) {
        List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
        if (names.size() == 1) {
            String name = ((TestNameNode) names.get(0)).getName();
            TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
            return testCase;
        }
        return null;
    }

    private void addDebugAndGetLog(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("Debug and get Log");
        getContextMenu().getItems().add(mi);
        mi.setOnAction(event -> {
                /*
                  Find the test case object
                 */
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

            if (names.size() == 1) {
                String name = testNewNode.getName();
                ITestcaseNode parent = testNewNode.getParent();
                if (name != null && name.length() > 0) {
                    ITestCase testCase = null;

                    if (parent instanceof TestNormalSubprogramNode) {
                        try {
                            ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) parent).getName());
                            logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
                            /*
                              Find the test case object by its name
                             */
                            testCase = TestCaseManager.getBasicTestCaseByName(name);

                            if (testCase != null)
                                ((TestCase) testCase).setFunctionNode(function);
                        } catch (FunctionNodeNotFoundException fe) {
                            functionNodeNotFoundExeptionHandle(fe);
                        }

                    } else if (parent instanceof TestCompoundSubprogramNode) {
                        testCase = TestCaseManager.getCompoundTestCaseByName(name);
                    }

                    if (testCase != null) {
                        UIController.debugAndGetLog(testCase);
                    }
                }
            }
        });
    }

    private void addExecuteTestCaseWithDebugMode(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("Execute With Debug Mode");
        getContextMenu().getItems().add(mi);
        mi.setOnAction(event -> {
                /*
                  Find the test case object
                 */
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

            if (names.size() == 1) {
                String name = testNewNode.getName();
                ITestcaseNode parent = testNewNode.getParent();
                if (name != null && name.length() > 0) {
                    ITestCase testCase = null;

                    if (parent instanceof TestNormalSubprogramNode) {
                        try {
                            ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) parent).getName());
                            logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
                            /*
                              Find the test case object by its name
                             */
                            testCase = TestCaseManager.getBasicTestCaseByName(name);

                            if (testCase != null)
                                ((TestCase) testCase).setFunctionNode(function);
                        } catch (FunctionNodeNotFoundException fe) {
                            functionNodeNotFoundExeptionHandle(fe);
                        }

                    } else if (parent instanceof TestCompoundSubprogramNode) {
                        testCase = TestCaseManager.getCompoundTestCaseByName(name);
                    }

                    if (testCase != null) {
                        UIController.executeTestCaseWithDebugMode(testCase);
                    }
                }
            }
        });
    }

    private void addGenerateTestCaseReport(TestNewNode testNewNode) {
        MenuItem mi = new MenuItem("Generate Test Case Data Report");
        getContextMenu().getItems().add(mi);
        mi.setOnAction(event -> {
            try {
                ITestCase testCase = TestCaseManager.getTestCaseByName(testNewNode.getName());

                if (testCase instanceof TestCase) {
                    ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) testNewNode.getParent()).getName());
                    ((TestCase) testCase).setFunctionNode(function);
                }

                // Generate test case data report
                IReport report = new FullReport(testCase, LocalDateTime.now());
                ReportManager.export(report);

                // display on MDIWindow
                MDIWindowController.getMDIWindowController().viewReport(report.getName(), report.toHtml());

                logger.debug("[" + Thread.currentThread().getName() + "] " + "generate test report for test case " + testCase.getName() + " success");
            } catch (FunctionNodeNotFoundException fe) {
                functionNodeNotFoundExeptionHandle(fe);
            }
        });
    }

    private void openPrototype(TestNewNode testNewNode) {
        TestCasesNavigatorController.getInstance().openPrototype(testNewNode);
    }

    private void addOpenPrototype(TestNewNode testNewNode) {
        MenuItem miOpen = new MenuItem("Open prototype");
        getContextMenu().getItems().add(miOpen);

        miOpen.setOnAction(event -> openPrototype(testNewNode));
    }

    /**
     * For a prototype of a template function
     *
     * @param testNewNode
     * @param item
     */
    private void addGenerateTestdataAutomaticallyForPrototype(TestNewNode testNewNode, TestCasesTreeItem item) {
        MenuItem miOpen = new MenuItem("Generate test data automatically for this prototype");
        getContextMenu().getItems().add(miOpen);

        miOpen.setOnAction(event -> {
            try {
                ITestcaseNode subprogramNode = testNewNode.getParent();
                if (!(subprogramNode instanceof TestNormalSubprogramNode))
                    return;
                ICommonFunctionNode function = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) subprogramNode).getName());
                logger.debug("The function corresponding to the clicked node: " + function.getAbsolutePath());
//                if (function instanceof AbstractFunctionNode)
                if (function.isTemplate() || function instanceof MacroFunctionNode) {
                    TestCase selectedPrototype = TestCaseManager.getBasicTestCaseByName(testNewNode.getName(),
                            new WorkspaceConfig().fromJson().getTestcaseDirectory(), true);

                    List<IDataNode> arguments = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode()).getChildren();
                    if (arguments == null || arguments.size() == 0) {
                        UIController.showErrorDialog("The template arguments in prototype " + selectedPrototype.getName() + " are not set up completely",
                                "Autogen for a prototype", "Fail");
                    } else
                        generateTestcaseAutomaticallyForNormalFunction(function,
                                false, item, selectedPrototype);
                }
            } catch (FunctionNodeNotFoundException fe) {
                functionNodeNotFoundExeptionHandle(fe);
            }
        });
    }

    private void openTestCase(TestNewNode testNewNode) {
        TestCasesNavigatorController.getInstance().openTestCase(testNewNode);
    }

    private void addOpenTestCase(TestNewNode testNewNode) {
        MenuItem miOpen = new MenuItem("Open Test Case");
        getContextMenu().getItems().add(miOpen);

        miOpen.setOnAction(event -> openTestCase(testNewNode));
    }

    private void viewTestcaseJson(TestNewNode testNewNode) {
        try {
            ITestcaseNode parent = testNewNode.getParent();
            String name = testNewNode.getName();

            ITestCase testCase = null;
            if (parent instanceof TestNormalSubprogramNode) {
                testCase = TestCaseManager.getBasicTestCaseByName(name);
            } else if (parent instanceof TestCompoundSubprogramNode) {
                testCase = TestCaseManager.getCompoundTestCaseByName(name);
            }

            try {
                Utils.openFolderorFileOnExplorer(testCase.getPath());
            } catch (OpenFileException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIController.showErrorDialog("Can not open json file", "Open Test case", "Fail");
        }
    }

    private void addViewTestCaseJson(TestNewNode testNewNode) {
        MenuItem miOpen = new MenuItem("Open Test Case in Json");
        getContextMenu().getItems().add(miOpen);

        miOpen.setOnAction(event -> viewTestcaseJson(testNewNode));
    }

    private void viewReport(TestNewNode testNewNode) {
        ITestcaseNode parent = testNewNode.getParent();
        List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

        if (names.size() == 1) {
            String name = ((TestNameNode) names.get(0)).getName();
            if (parent instanceof TestNormalSubprogramNode) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
                if (testCase != null) {
                    MDIWindowController.getMDIWindowController().viewReport(testCase);
                }
            } else if (parent instanceof TestCompoundSubprogramNode) {
//                CompoundTestCase compoundTestCase = TestCaseManager.getCompoundTestCaseByName(name);
//                Environment.setCurrentTestcompound(compoundTestCase);
//                if (compoundTestCase != null) {
//                    UIController.viewTestCase(compoundTestCase);
//                }
            }
        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "There are more than two similar names in a test case ");
        }
    }

    private void addViewReport(TestNewNode testNewNode) {
        MenuItem miViewReport = new MenuItem("View Report of this Test Case");
        getContextMenu().getItems().add(miViewReport);

        miViewReport.setOnAction(event -> viewReport(testNewNode));
    }

    private void addDuplicateTestCase(TestNewNode testNewNode, TestCasesTreeItem treeItem) {
        ITestcaseNode parent = testNewNode.getParent();
        MenuItem miDuplicate = new MenuItem("Duplicate Test Case");
        getContextMenu().getItems().add(miDuplicate);
        miDuplicate.setOnAction(event -> {
            getTreeTableView().getSelectionModel().clearSelection();
            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
            if (names.size() == 1) {
                String name = ((TestNameNode) names.get(0)).getName();
                if (parent instanceof TestNormalSubprogramNode) {
                    // duplicate on disk
                    TestCase testCase = TestCaseManager.duplicateBasicTestCase(name);
                    if (testCase != null) {
                        // add to children of parent
                        TestNewNode newTestCase = testCase.getTestNewNode();
                        parent.getChildren().add(newTestCase);
                        newTestCase.setParent(parent);
                        // display on the navigator
                        TestCasesTreeItem item = new TestCasesTreeItem(newTestCase);
                        treeItem.getParent().getChildren().add(item);
                        getTreeTableView().getSelectionModel().select(item);
                        // save the testcases scripts to file .tst
                        Environment.getInstance().saveTestcasesScriptToFile();
                        // display on MDIWindow
                        UIController.viewTestCase(testCase);
                    }
                } else if (parent instanceof TestCompoundSubprogramNode) {
                    // duplicate on disk
                    CompoundTestCase compoundTestCase = TestCaseManager.duplicateCompoundTestCase(name);
                    if (compoundTestCase != null) {
                        // add to children of parent
                        TestNewNode newTestCase = compoundTestCase.getTestNewNode();
                        parent.getChildren().add(newTestCase);
                        newTestCase.setParent(parent);
                        // display on the navigator
                        TestCasesTreeItem item = new TestCasesTreeItem(newTestCase);
                        treeItem.getParent().getChildren().add(item);
                        getTreeTableView().getSelectionModel().select(item);
                        // save the testcases scripts to file .tst
                        Environment.getInstance().saveTestcasesScriptToFile();
                        // display on MDIWindow
                        UIController.viewTestCase(compoundTestCase);
                    }
                }
            } else {
                logger.debug("[" + Thread.currentThread().getName() + "] " + "There are more than two similar names in a test case ");
            }
        });
    }

    private void addDeletePrototype(TestNewNode testNewNode, TestCasesTreeItem item) {
        MenuItem miDelete = new MenuItem("Delete Prototype");
        getContextMenu().getItems().add(miDelete);

        miDelete.setOnAction(event -> {
            deleteTestCase(testNewNode, item);
        });
    }

    private void addDeleteTestCase(TestNewNode testNewNode, TestCasesTreeItem item) {
        MenuItem miDelete = new MenuItem("Delete Test Case");
        getContextMenu().getItems().add(miDelete);

        miDelete.setOnAction(event -> {
            deleteTestCase(testNewNode, item);
        });
    }

//        private void deleteTestCase(TestNewNode testNewNode, TestCasesTreeItem item) {
//            ITestcaseNode parent = testNewNode.getParent();
//            // remove the testcase
//            parent.getChildren().remove(testNewNode);
//            // save the testcases scripts to file .tst
//            Environment.saveTestcasesScriptToFile();
//            // remove from the test case navigator
////                TreeItem<ITestcaseNode> item = getTreeItem();
//            if (item.getParent().getChildren()!=null)
//                item.getParent().getChildren().remove(item);
//            getTreeTableView().getSelectionModel().clearSelection();
//
//            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
//            if (names.size() == 1) {
//                String name = ((TestNameNode) names.get(0)).getName();
//                if (parent instanceof TestNormalSubprogramNode) {
//                    // remove from disk
//                    TestCaseManager.removeBasicTestCase(name);
//                    // remove from MDI window
//                    MDIWindowController.getMDIWindowController().removeTestCaseTab(name);
//                } else if (parent instanceof TestCompoundSubprogramNode) {
//                    // remove from disk
//                    TestCaseManager.removeCompoundTestCase(name);
//                    // remove from MDI window
//                    MDIWindowController.getMDIWindowController().removeCompoundTestCaseTab(name);
//                }
//                // refresh compound testcase tree table views that were opened
//                UIController.refreshCompoundTestcaseViews();
//
//            } else {
//                logger.debug("There are more than two similar names in a test case ");
//            }
//        }

    private void addDeselectCoverageOption(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode || node instanceof TestCompoundSubprogramNode) {
            MenuItem miDeselectCoverage = new MenuItem(("Deselect Coverage"));
            getContextMenu().getItems().add(miDeselectCoverage);
            miDeselectCoverage.setDisable(true);

            miDeselectCoverage.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: handle here
                }

            });
        }
    }

    private void addCollapseAllChildrenOption(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode || node instanceof TestCompoundSubprogramNode) {
            MenuItem miCollapseAllChildren = new MenuItem(("Collapse All Children"));
            getContextMenu().getItems().add(miCollapseAllChildren);
            miCollapseAllChildren.setDisable(true);

            miCollapseAllChildren.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: handle here
                }

            });
        }
    }

    private void addExpandAllChildrenOption(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode || node instanceof TestCompoundSubprogramNode) {
            MenuItem miExpandAllChildren = new MenuItem(("Expand All Children"));
            getContextMenu().getItems().add(miExpandAllChildren);
            miExpandAllChildren.setDisable(true);

            miExpandAllChildren.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: handle here
                }

            });
        }
    }

    private void addDeleteOption(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode || node instanceof TestCompoundSubprogramNode) {
            MenuItem miDelete = new MenuItem(("Delete"));
            getContextMenu().getItems().add(miDelete);
            miDelete.setDisable(true);

            miDelete.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: handle here
                }

            });
        }
    }

    private void addOpenSourceInDebugMode(ITestcaseNode node) {
        if (node instanceof TestUnitNode) {
            MenuItem miOpenSourceInDebug = new MenuItem("Open source code in debug");
            miOpenSourceInDebug.setDisable(!MDIWindowController.getMDIWindowController().checkDebugOpen());
            getContextMenu().getItems().add(miOpenSourceInDebug);
            miOpenSourceInDebug.setOnAction(e -> {
                TestUnitNode testUnitNode = (TestUnitNode) node;
                String path = testUnitNode.getName();
                DebugController.getDebugController().openSource(path);
            });
        } else {
            // todo: handle if not unit node
        }
    }

    private void addViewCoverageOptionToAFile(ITestcaseNode node, TestCasesTreeItem treeItem) {
        MenuItem miViewCoverage = new MenuItem("View Coverage of this file");
        // enable/disable CoverageMode
        miViewCoverage.setDisable(!Environment.getInstance().isCoverageModeActive());

        getContextMenu().getItems().add(miViewCoverage);
        viewCoverage(miViewCoverage, node, treeItem);
    }

    private void addViewCoverageOptionToATestcase(ITestcaseNode node, TestCasesTreeItem treeItem) {
        MenuItem miViewCoverage = new MenuItem("View Coverage of this test case");
        // enable/disable CoverageMode
        miViewCoverage.setDisable(!Environment.getInstance().isCoverageModeActive());

        getContextMenu().getItems().add(miViewCoverage);
        viewCoverage(miViewCoverage, node, treeItem);
    }


    private void addInsertBasisPathTestCasesOption(ITestcaseNode node) {
        if (node instanceof TestNormalSubprogramNode) {
            MenuItem miInsertBasisPathTestCases = new MenuItem("Insert Basis Path Test Cases");
            getContextMenu().getItems().add(miInsertBasisPathTestCases);
            miInsertBasisPathTestCases.setDisable(true);

            miInsertBasisPathTestCases.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: handle here
                }

            });
        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Can not insert test case for " + node.getClass());
        }
    }

    private void addSelectAllChildren(ITestcaseNode node) {
        MenuItem mi = new MenuItem("Select All");
        getContextMenu().getItems().add(mi);

        mi.setDisable(!Environment.getInstance().isCoverageModeActive());

        mi.setOnAction(event -> {
            SelectionUpdater.check(node);
            TestCasesNavigatorController.getInstance().refreshNavigatorTree();
        });
    }

    private void addDeselectAllChildren(ITestcaseNode node) {
        MenuItem mi = new MenuItem("Deselect All");
        getContextMenu().getItems().add(mi);

        mi.setDisable(!Environment.getInstance().isCoverageModeActive());

        mi.setOnAction(event -> {
            SelectionUpdater.uncheck(node);
            TestCasesNavigatorController.getInstance().refreshNavigatorTree();
        });
    }

    private void addStopAutomatedTestdataGenerationOption(ITestcaseNode node, TestCasesTreeItem item) {
        MenuItem mi = new MenuItem("Stop automated test data generation");
        getContextMenu().getItems().add(mi);

        mi.setOnAction(event -> {
            List<ITestcaseNode> nodes = TestcaseSearch.searchNode(node, new TestNormalSubprogramNode());
            if (node instanceof TestNormalSubprogramNode)
                nodes.add(node);
            for (ITestcaseNode child : nodes) {
                try {
                    ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) child).getName());
                    if (functionNode instanceof IFunctionNode)
                        AkaThreadManager.stopAutomatedTestdataGenerationThreadForAFunction((IFunctionNode) functionNode);
                } catch (FunctionNodeNotFoundException fe) {
                    functionNodeNotFoundExeptionHandle(fe);
                }
            }
            TestCasesNavigatorController.getInstance().refreshNavigatorTree();
        });
    }

    /**
     * For an unit
     *
     * @param node
     * @param item
     */
    private void addGenerateTestdataAutomaticallyOptionForAnUnit(ITestcaseNode node, TestCasesTreeItem item) {
        MenuItem mi = new MenuItem("Generate test data automatically");
        getContextMenu().getItems().add(mi);

        mi.setOnAction(event -> {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    item.loadChildren(true);

                    List<ITestcaseNode> selectedFunctions = null;
                    if (Environment.getInstance().isCoverageModeActive())
                        selectedFunctions = SelectionUpdater.getAllSelectedFunctions(item.getValue());
                    else
                        selectedFunctions = TestcaseSearch.searchNode(item.getValue(), new TestNormalSubprogramNode());

                    if (selectedFunctions == null || selectedFunctions.size() == 0) {
                        UIController.showErrorDialog("Please select at least one function", "Automated test data generation for a file", "No selected functions");
                    } else {
                        for (ITestcaseNode selectedFunction : selectedFunctions)
                            if (selectedFunction instanceof TestNormalSubprogramNode) {
                                generateTestdataAutomaticallyForFunction((TestNormalSubprogramNode) selectedFunction, item);
                            }
                        MenuBarController.getMenuBarController().refresh();
                    }
                }
            });
        });
    }

    private void addViewInstrumentedSourcecodeOption(ITestcaseNode node, TestCasesTreeItem item) {
        if (node instanceof TestUnitNode) {
            MenuItem miViewInstrumentedSourcecode = new MenuItem("View Instrumented Source Code");
            getContextMenu().getItems().add(miViewInstrumentedSourcecode);

            miViewInstrumentedSourcecode.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    ISourcecodeFileNode sourcecodeFileNode =
                            UIController.searchSourceCodeFileNodeByPath((TestUnitNode) node, Environment.getInstance().getProjectNode());
                    if (sourcecodeFileNode != null) {
                        logger.debug("[" + Thread.currentThread().getName() + "] " + "The src file corresponding to the clicked node: " + sourcecodeFileNode.getAbsolutePath());
                        try {
                            Utils.openFolderorFileOnExplorer(ProjectClone.getClonedFilePath(sourcecodeFileNode.getAbsolutePath()));
                        } catch (OpenFileException e) {
                            e.printStackTrace();
                        }
                    } else {
                        logger.debug("[" + Thread.currentThread().getName() + "] " + "The source code file not found");
                    }
                }

            });
        }
    }

    private void addInsertMinMidMaxOption(ITestcaseNode node, TestCasesTreeItem item) {
        if (node instanceof TestNormalSubprogramNode) {
            Menu mMinMidMax = new Menu("Insert Min Mid Max");

            // menu Item 1
            MenuItem miMinMidMax = new MenuItem("Min Mid Max");
            mMinMidMax.getItems().add(miMinMidMax);
            miMinMidMax.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    insertNormalTestCase("MIN", node, item);
                    insertNormalTestCase("MID", node, item);
                    insertNormalTestCase("MAX", node, item);
                }

            });

            // menu item 2
            MenuItem miMin = new MenuItem("Min");
            mMinMidMax.getItems().add(miMin);
            miMin.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    insertNormalTestCase("MIN", node, item);
                }
            });

            // menu item 3
            MenuItem miMid = new MenuItem("Mid");
            mMinMidMax.getItems().add(miMid);
            miMid.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    insertNormalTestCase("MID", node, item);
                }

            });

            // menu item 4
            MenuItem miMax = new MenuItem("Max");
            mMinMidMax.getItems().add(miMax);
            miMax.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    insertNormalTestCase("MAX", node, item);
                }

            });

            // merge all
            getContextMenu().getItems().add(mMinMidMax);

        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Can not insert test case for " + node.getClass());
        }
    }

//    private void addExecuteOption(ITestcaseNode node) {
//        assert (node != null);
//        if (node instanceof TestNormalSubprogramNode || node instanceof TestNewNode
//                || node instanceof TestCompoundSubprogramNode) {
//            MenuItem miExecute = new MenuItem("Execute");
//            getContextMenu().getItems().add(miExecute);
//            miExecute.setDisable(true);
//
//            miExecute.setOnAction(new EventHandler<ActionEvent>() {
//                @Override
//                public void handle(ActionEvent event) {
//                    // TODO: handle here
//                }
//
//            });
//        }
//    }


    /**
     * Allow to insert prototype
     *
     * @param navigatorNode
     * @param item
     */
    private void addCreatePrototypeForTemplateAndMacroFunction(ITestcaseNode navigatorNode, TestCasesTreeItem item) {
        assert (navigatorNode != null);

        if (navigatorNode instanceof TestNormalSubprogramNode) {
            try {
                ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());
//                if (functionNode instanceof IFunctionNode)
                if (functionNode.isTemplate() || functionNode instanceof MacroFunctionNode) {
                    MenuItem miInsertRealType = new MenuItem("Create new prototype");

                    String prototypeName = ITestCase.PROTOTYPE_SIGNAL + AbstractTestCase.removeSpecialCharacter(functionNode.getName()) + "_" +
                            functionNode.getAbsolutePath().length() + new Random().nextInt(99999);
                    miInsertRealType.setOnAction(event -> {
                        insertPrototypeOfFunction(navigatorNode, item, prototypeName);

                    });
                    getContextMenu().getItems().add(miInsertRealType);
                }
            } catch (FunctionNodeNotFoundException fe) {
                functionNodeNotFoundExeptionHandle(fe);
            }
        }
    }

    private void addInsertTestCase(ITestcaseNode navigatorNode, TestCasesTreeItem item) {
        assert (navigatorNode != null);

        if (navigatorNode instanceof TestNormalSubprogramNode) {
            MenuItem miInsertTestCase = new MenuItem("Insert Test Case");

            miInsertTestCase.setOnAction(event -> insertNormalTestCase(navigatorNode, item));
            getContextMenu().getItems().add(miInsertTestCase);

        } else if (navigatorNode instanceof TestCompoundSubprogramNode) {
            MenuItem miInsertTestCase = new MenuItem("Insert Test Case");
            TestCompoundSubprogramNode cast = (TestCompoundSubprogramNode) navigatorNode;
            miInsertTestCase.setOnAction(event -> {
                getTreeTableView().getSelectionModel().clearSelection();
                // create new compound testcase
                CompoundTestCase compoundTestCase = TestCaseManager.createCompoundTestCase();

                TestNewNode testNewNode = compoundTestCase.getTestNewNode();
                // display on testcase navigator tree
                item.setExpanded(true);
                cast.addChild(testNewNode);
                testNewNode.setParent(cast);
                TestCasesTreeItem newItem = new TestCasesTreeItem(testNewNode);
                item.getChildren().add(newItem);
                getTreeTableView().getSelectionModel().select(newItem);
                // view compound Testcase on MDIWindow
                UIController.viewTestCase(compoundTestCase);
                // update the testcase .tst file
                Environment.getInstance().saveTestcasesScriptToFile();
            });
            getContextMenu().getItems().add(miInsertTestCase);
        }
    }

    private void addViewDependency(ITestcaseNode navigatorNode) {
        assert (navigatorNode != null);

        if (navigatorNode instanceof TestUnitNode || navigatorNode instanceof TestNormalSubprogramNode) {

            MenuItem miViewDependency = new MenuItem("View dependency");
            getContextMenu().getItems().add(miViewDependency);

            miViewDependency.setOnAction(event -> {
                // find the node corresponding the clicked item
                INode node = null;
                if (navigatorNode instanceof TestUnitNode)
                    node = UIController.searchSourceCodeFileNodeByPath((TestUnitNode) navigatorNode, Environment.getInstance().getProjectNode());
                else { // navigatorNode instance of TestNormalSubprogramNode
                    try {
                        node = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());
                    } catch (FunctionNodeNotFoundException fe) {
                        functionNodeNotFoundExeptionHandle(fe);
                    }
                }

                if (node != null) {
                    Utils.viewDependency(node);
                }
            });
        }
    }

    private void addOpenSourceOption(ITestcaseNode navigatorNode) {
        assert (navigatorNode != null);

        if (navigatorNode instanceof TestUnitNode || navigatorNode instanceof TestNormalSubprogramNode) {

            MenuItem miViewSourceCode = new MenuItem("Open Source");

            if (navigatorNode instanceof TestUnitNode) {
                miViewSourceCode.setOnAction(event -> {
                    ISourcecodeFileNode node = UIController.searchSourceCodeFileNodeByPath((TestUnitNode) navigatorNode, Environment.getInstance().getProjectNode());
                    try {
                        UIController.viewSourceCode(node);
                        UILogger.getUiLogger().logToBothUIAndTerminal("Opened source code of " + node.getName() + " [" + node.getClass().getSimpleName() + "] on this tool");
                    } catch (Exception e) {
                        UIController.showErrorDialog("Error code: " + e.getMessage(), "Open source code file",
                                "Can not open source code file");
                    }
                });
            } else { // navigatorNode instanceof TestNormalSubprogramNode
                try {
                    ICommonFunctionNode iFunctionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());
//                    if (iFunctionNode instanceof AbstractFunctionNode) {
//                        AbstractFunctionNode node = (AbstractFunctionNode) iFunctionNode;
                    miViewSourceCode.setOnAction(event -> {
                        try {
                            UIController.viewSourceCode(iFunctionNode);
                            UILogger.getUiLogger().logToBothUIAndTerminal("Opened source code of " + iFunctionNode.getName() + " [" + iFunctionNode.getClass().getSimpleName() + "] on this tool");
                        } catch (Exception e) {
                            e.printStackTrace();
                            UIController.showErrorDialog("Error code: " + e.getMessage(), "Open source code file",
                                    "Can not open source code file");
                        }
                    });
                } catch (FunctionNodeNotFoundException fe) {
                    functionNodeNotFoundExeptionHandle(fe);
                }
            }

            getContextMenu().getItems().add(miViewSourceCode);
        }
    }

    private void viewCoverage(MenuItem miViewCoverage, ITestcaseNode node, TestCasesTreeItem treeItem) {
        if (node instanceof TestUnitNode) {
            miViewCoverage.setOnAction(event -> {
                viewCoverageOfSourcecodeFile(node, treeItem);
            });
        } else if (node instanceof TestNewNode && node.getParent() instanceof TestNormalSubprogramNode) {
            miViewCoverage.setOnAction(event -> {
                viewCoverageOfATestcase(node);
            });
        }
    }

    private void viewCoverageOfATestcase(ITestcaseNode node) {
        List<ITestcaseNode> names = TestcaseSearch.searchNode(node, new TestNameNode());

        if (names.size() == 1) {
            String name = ((TestNameNode) names.get(0)).getName();
            TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
            if (testCase != null) {
                UIController.viewCoverageOfATestcase(testCase);
            }
        }
    }

    private void viewCoverageOfSourcecodeFile(ITestcaseNode node, TestCasesTreeItem treeItem) {
        if (!(node instanceof TestUnitNode))
            return;

        TestCasesNavigatorController.getInstance().refreshNavigatorTree();

        treeItem.loadChildren(true);
        List<TestCase> testCases = new ArrayList<>();
        ISourcecodeFileNode sourcecodeFileNode =
                UIController.searchSourceCodeFileNodeByPath((TestUnitNode) node, Environment.getInstance().getProjectNode());

        /**
         * STEP: Get unsuccessful test cases
         */
        List<String> notSuccessTCNames = new ArrayList<>(); // contain name of unsuccessful test cases

        for (TreeItem subprogramTreeItem : treeItem.getChildren())
            for (Object testcaseItem : subprogramTreeItem.getChildren())
                // only consider the test cases checked on test case navigator
                if (testcaseItem instanceof CheckBoxTreeItem &&
                        ((CheckBoxTreeItem) testcaseItem).getValue() != null &&
                        ((ITestcaseNode) ((CheckBoxTreeItem) testcaseItem).getValue()).isSelectedInTestcaseNavigator()) {
                    TestNewNode testNewNode = (TestNewNode) ((CheckBoxTreeItem) testcaseItem).getValue();
                    if (testNewNode.isPrototypeTestcase()) {
                        // ignore
                    } else {
                        String name = testNewNode.getName();
                        TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
                        if (testCase != null) {
                            if (!testCase.getStatus().equals(TestCase.STATUS_SUCCESS)) {
                                // add to display on popup
                                notSuccessTCNames.add(testCase.getName());
                            } else {
                                testCases.add(testCase);
                            }
                        }
                    }
                }

        /**
         * STEP: If there exist at least one successful test case, we need to notify users
         *
         */
        if (notSuccessTCNames.size() > 0) { // need to pupup to notify user
            SomeTestCasesAreNotSuccessPopupController popupController = SomeTestCasesAreNotSuccessPopupController.getInstance(notSuccessTCNames);
            Stage popUpWindow = popupController.getPopUpWindow();
            // block the environment window
            assert popUpWindow != null;
            popUpWindow.initModality(Modality.WINDOW_MODAL);
            popUpWindow.initOwner(UIController.getPrimaryStage().getScene().getWindow());

            popupController.getbContinue().setOnAction(event1 -> {
                // users accept to compute coverage of test cases
                UIController.viewCoverageOfMultipleTestcases(sourcecodeFileNode.getName(), testCases);
                popUpWindow.close();
            });

            popUpWindow.show();
        } else {
            // all test cases are executed successfully before
            UIController.viewCoverageOfMultipleTestcases(sourcecodeFileNode.getName(), testCases);
        }
    }

    private void showUnsupportedFunctionDialog(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setTitle("Test case generation");
        alert.setHeaderText("This function is not supported");
        alert.showAndWait();
    }

    // for MIN, MID, MAX testcase inserting
    private void insertNormalTestCase(String type, ITestcaseNode navigatorNode, TestCasesTreeItem item) {
        if (!(navigatorNode instanceof TestSubprogramNode))
            return;
        //clear selection
        getTreeTableView().getSelectionModel().clearSelection();

        try {
            ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());
            if (functionNode instanceof IFunctionNode) {
                ITestcaseNode subprogramNode = getItem();
                // create new test case
                TestCase testCase = TestCaseManager.createMinMidMaxTestCase(type, (IFunctionNode) functionNode);
                if (testCase != null) {
                    // display testcase on testcase navigator tree
                    TestNewNode testNewNode = testCase.getTestNewNode();
                    item.setExpanded(true);
                    // add to the top of children
                    subprogramNode.getChildren().add(0, testNewNode);
                    testNewNode.setParent(subprogramNode);
                    TestCasesTreeItem newTestCaseTreeItem = new TestCasesTreeItem(testNewNode);
                    item.getChildren().add(0, newTestCaseTreeItem);
                    getTreeTableView().getSelectionModel().select(newTestCaseTreeItem);

                    // update the testcase .tst file
                    Environment.getInstance().saveTestcasesScriptToFile();
                    // render testcase view in MDI window
                    UIController.viewTestCase(testCase);
                } else {
                    showUnsupportedFunctionDialog("Do not support to generate test case for this kind of function.");
                    logger.debug("[" + Thread.currentThread().getName() + "] " + "Failed to create new test case.");
                }
            } else {
                showUnsupportedFunctionDialog("The function is " + functionNode.getClass() + ". Do not support to generate test case.");
            }

        } catch (FunctionNodeNotFoundException fe) {
            UIController.showErrorDialog("The function does not exist: " + fe.getFunctionPath(), "Generate test case",
                    "Can not generate test case");
        }
    }

    /**
     * Show prototype tab
     *
     * @param navigatorNode
     * @param item
     * @param nameTestcase
     */
    private void insertPrototypeOfFunction(ITestcaseNode navigatorNode, TestCasesTreeItem item, String nameTestcase) {
        if (!(navigatorNode instanceof TestSubprogramNode))
            return;
        //clear selection
        getTreeTableView().getSelectionModel().clearSelection();

        try {
            ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());
            // create new test case
            TestCase testCase = TestCaseManager.createTestCase(functionNode, nameTestcase);
            // display testcase on testcase navigator tree
            TestNewNode testNewNode = testCase.getTestNewNode();

            item.setExpanded(true);
            navigatorNode.getChildren().add(testNewNode);
            testNewNode.setParent(navigatorNode);
            TestCasesTreeItem newTestCaseTreeItem = new TestCasesTreeItem(testNewNode);
            item.getChildren().add(newTestCaseTreeItem);
            getTreeTableView().getSelectionModel().select(newTestCaseTreeItem);

            // update the testcase .tst file
            Environment.getInstance().saveTestcasesScriptToFile();
            // render testcase view in MDI window
            UIController.viewPrototype(testCase);
        } catch (FunctionNodeNotFoundException fe) {
            UIController.showErrorDialog("The function does not exist", "Prototype generation",
                    "Can not generate a prototype");
        }
    }

    private void insertNormalTestCase(ITestcaseNode navigatorNode, TestCasesTreeItem item) {
        if (!(navigatorNode instanceof TestSubprogramNode))
            return;

        //clear selection
        getTreeTableView().getSelectionModel().clearSelection();

        try {
            ICommonFunctionNode functionNode = UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) navigatorNode).getName());

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    // create new test case
                    TestCase testCase = TestCaseManager.createTestCase(functionNode);
                    // display testcase on testcase navigator tree
                    TestNewNode testNewNode = testCase.getTestNewNode();

                    item.setExpanded(true);
                    navigatorNode.getChildren().add(testNewNode);
                    testNewNode.setParent(navigatorNode);
                    TestCasesTreeItem newTestCaseTreeItem = new TestCasesTreeItem(testNewNode);
                    item.getChildren().add(newTestCaseTreeItem);
                    getTreeTableView().getSelectionModel().select(newTestCaseTreeItem);

                    // update the testcase .tst file
                    Environment.getInstance().saveTestcasesScriptToFile();
                    // render testcase view in MDI window
                    UIController.viewTestCase(testCase);
                }
            });
        } catch (FunctionNodeNotFoundException fe) {
            UIController.showErrorDialog("The function does not exist", "Generate test case",
                    "Can not generate test case");
        }
    }

    public Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<>();
        if (dir == null || dir.listFiles() == null) {
            return fileTree;
        }
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) fileTree.add(entry);
            else fileTree.addAll(listFileTree(entry));
        }
        return fileTree;
    }
}
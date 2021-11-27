package com.dse.guifx_v3.controllers;

import com.dse.config.WorkspaceConfig;
import com.dse.coverage.AbstractCoverageManager;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.guifx_v3.controllers.main_view.MDIWindowController;
import com.dse.guifx_v3.controllers.object.TestCasesNavigatorRow;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.GenerateTestdataTask;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.objects.DefaultTreeTableCell;
import com.dse.guifx_v3.objects.TestCasesTreeItem;
import com.dse.parser.object.ISourcecodeFileNode;
import com.dse.testcase_manager.CompoundTestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testcasescript.TestcaseSearch;
import com.dse.testcasescript.object.*;
import com.dse.thread.AkaThread;
import com.dse.thread.AkaThreadManager;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TestCasesNavigatorController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(TestCasesNavigatorController.class);

    /**
     * Singleton pattern like
     */
    private static AnchorPane navigatorTreeTable = null;
    private static TestCasesNavigatorController instance = null;

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/TestCasesNavigator.fxml"));
        try {
            Parent parent = loader.load();
            navigatorTreeTable = (AnchorPane) parent;
            instance = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AnchorPane getNavigatorTreeTable() {
        if (navigatorTreeTable == null) {
            prepare();
        }
        return navigatorTreeTable;
    }

    public static TestCasesNavigatorController getInstance() {
        if (instance == null) {
            prepare();
        }
        return instance;
    }

    public TreeTableView<ITestcaseNode> getTestCasesNavigator() {
        return testCasesNavigator;
    }

    @FXML
    private TreeTableView<ITestcaseNode> testCasesNavigator;
    @FXML
    private TreeTableColumn<ITestcaseNode, String> colName;
    @FXML
    private TreeTableColumn<ITestcaseNode, String> colStatus;
    @FXML
    private TreeTableColumn<ITestcaseNode, String> colCoverage;
//    @FXML
//    private TreeTableColumn<ITestcaseNode, String> colDate;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        testCasesNavigator.setRowFactory(param -> {
            TestCasesNavigatorRow row = new TestCasesNavigatorRow();
            row.setOnMouseClicked(event -> {
                try {
                    if (row.getItem() instanceof TestNewNode) {
                        if (event.getClickCount() == 2 && (!row.isEmpty())) {
                            TestNewNode testNewNode = (TestNewNode) row.getItem();
                            if (testNewNode.isPrototypeTestcase())
                                openPrototype(testNewNode);
                            else
                                openTestCase(testNewNode);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            row.setOnDragDetected(event -> {
                try {
                    if (!row.isEmpty()) {
                        if (row.getTreeItem().getValue() instanceof TestNewNode) {
                            TestNewNode testNewNode = (TestNewNode) row.getTreeItem().getValue();
                            Dragboard db = row.startDragAndDrop(TransferMode.ANY);
                            ClipboardContent content = new ClipboardContent();
                            List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
                            if (names.size() == 1) {
                                content.putString(((TestNameNode) names.get(0)).getName());
                                db.setContent(content);
                            } else {
                                UIController.showErrorDialog("Can not add this test case to test compound", "Test compound generation", "Drag fail");
                            }
                            event.consume();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return row;
        });

        // set the treetable to multi selection mode
        testCasesNavigator.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // A listener for list selections, multiple selections in the TableView
        ListChangeListener<TreeItem<ITestcaseNode>> multiSelection = changed -> {
            try {
                testCasesNavigator.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        // register the listener on the ObservableList<TreeItem<ITestCaseNode>>
        testCasesNavigator.getSelectionModel().getSelectedItems().addListener(multiSelection);

        colName.setCellFactory(param -> new DefaultTreeTableCell<>());
        colName.setCellValueFactory((TreeTableColumn.CellDataFeatures<ITestcaseNode, String> param) -> {
            try {
                ITestcaseNode node = param.getValue().getValue();
                String name = "";
                if (node instanceof TestcaseRootNode) {
                    name = new File(((TestcaseRootNode) node).getAbsolutePath()).getName();

                    int nThreads = AkaThreadManager.getTotalRunningThreads().size();
                    if (nThreads > 0)
                        name += " [" + AkaThreadManager.getTotalRunningThreads().size() + " threads]";

                } else if (node instanceof TestSubprogramNode) {
                    name = ((TestSubprogramNode) node).getSimpleNameToDisplayInTestcaseView();

                } else if (node instanceof TestUnitNode) {
                    // name of unit node is sometimes too long, need to shorten it.
                    name = ((TestUnitNode) node).getShortNameToDisplayInTestcaseTree();

                    ISourcecodeFileNode srcNode = UIController.searchSourceCodeFileNodeByPath((TestUnitNode) node,
                            Environment.getInstance().getProjectNode());

                    int nThreads = AkaThreadManager.getTotalRunningThreads(srcNode).size();
                    if (nThreads > 0)
                        name += " [" + AkaThreadManager.getTotalRunningThreads(srcNode).size() + " threads]";

                } else if (node instanceof TestNewNode) {
                    List<ITestcaseNode> nameNodes = TestcaseSearch.searchNode(node, new TestNameNode());
                    if (nameNodes.size() == 1) {
                        name = ((TestNameNode) nameNodes.get(0)).getName();

                    } else {
                        logger.debug("[" + Thread.currentThread().getName() + "] " + "[Error] there are 2 TestNameNode in a test case");
                    }
                }

                return new ReadOnlyStringWrapper(name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        });

        colStatus.setCellValueFactory(param -> {
            try {
                ITestcaseNode valueNode = param.getValue().getValue();

                if (valueNode instanceof TestNewNode) {
//                    if (((TestNewNode) valueNode).isPrototypeTestcase()) {
//                        // nothing to do
//                    } else {
                    //only test case need to represent the status and date
                    TestNewNode testNewNode = (TestNewNode) param.getValue().getValue();
                    List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());
                    if (names.size() == 1) {
                        String testCaseDirectory = new WorkspaceConfig().fromJson().getTestcaseDirectory();
                        String name = ((TestNameNode) names.get(0)).getName();
                        String status = TestCaseManager.getStatusTestCaseByName(name, testCaseDirectory);
                        if (status != null) {
                            return new SimpleStringProperty(status);
                        }
                    } else
                        logger.debug("[" + Thread.currentThread().getName() + "] " + "Unexpected result when display status of testcase: " + testNewNode.getName());
//                    }
                } else if (valueNode instanceof TestNormalSubprogramNode) {
                    for (AkaThread thread : AkaThreadManager.akaThreadList)
                        if (thread.getTask() instanceof GenerateTestdataTask) {
                            if (((GenerateTestdataTask) thread.getTask()).getFunction().getAbsolutePath()
                                    .equals(((TestNormalSubprogramNode) valueNode).getName())) {
                                if (((GenerateTestdataTask) thread.getTask()).isStillRunning())
                                    return new SimpleStringProperty(((GenerateTestdataTask) thread.getTask()).getStatus());
                            }
                        }

                    return new SimpleStringProperty("");

                } else if (valueNode instanceof TestUnitNode) {
                    for (AkaThread thread : AkaThreadManager.akaThreadList)
                        if (thread.getTask() instanceof GenerateTestdataTask) {
                            String srcPath = Utils.getSourcecodeFile(((GenerateTestdataTask) thread.getTask()).getFunction()).getAbsolutePath();
                            if (((TestUnitNode) valueNode).getName().equals(srcPath)) {
                                if (((GenerateTestdataTask) thread.getTask()).isStillRunning())
                                    return new SimpleStringProperty("generating");
                            }
                        }
                    return new SimpleStringProperty("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        colCoverage.setCellValueFactory(param -> {
            try {
                if (param == null || param.getValue() == null || !(param.getValue().getValue() instanceof TestNewNode))
                    return null;
                TestNewNode testNewNode = (TestNewNode) param.getValue().getValue();
                if (testNewNode.isPrototypeTestcase())
                    return null;

                //only test case need to represent the coverage, status and date
                String name = testNewNode.getName();
                TestCase testCase = TestCaseManager.getBasicTestCaseByNameWithoutData(name);
                if (testCase == null)
                    return null;

                String typeOfCoverage = Environment.getInstance().getTypeofCoverage();
                switch (typeOfCoverage) {
                    case EnviroCoverageTypeNode.STATEMENT:
                    case EnviroCoverageTypeNode.BRANCH:
                    case EnviroCoverageTypeNode.BASIS_PATH:
                    case EnviroCoverageTypeNode.MCDC: {
                        float coverage = AbstractCoverageManager.getProgress(testCase, typeOfCoverage);
                        return new SimpleStringProperty(Float.toString(coverage));
                    }

                    case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH: {
                        float stmCoverage = AbstractCoverageManager.getProgress(testCase, EnviroCoverageTypeNode.STATEMENT);
                        float branchCoverage = AbstractCoverageManager.getProgress(testCase, EnviroCoverageTypeNode.BRANCH);
                        return new SimpleStringProperty(Utils.round(stmCoverage * 100, 4) + "% / " + Utils.round(branchCoverage * 100, 4) + "%");
                    }

                    case EnviroCoverageTypeNode.STATEMENT_AND_MCDC: {
                        float stmCoverage = AbstractCoverageManager.getProgress(testCase, EnviroCoverageTypeNode.STATEMENT);
                        float mcdcCoverage = AbstractCoverageManager.getProgress(testCase, EnviroCoverageTypeNode.MCDC);
                        return new SimpleStringProperty(Utils.round(stmCoverage * 100, 4) + "% / " + Utils.round(mcdcCoverage * 100, 4) + "%");
                    }
                    default: {
                        return null;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        });
//        date.setCellValueFactory(param -> {
//            if (!(param.getValue().getValue() instanceof TestCaseNode)) {
//                return null;
//            }
//            //only test case need to represent the status and date
//            TestCaseNode tcn = (TestCaseNode) param.getValue().getValue();
//            SimpleStringProperty parameter = new SimpleStringProperty(tcn.getDate());
//            return parameter;
//        });
    }

    // load content from root node
    public void loadContent(ITestcaseNode testcaseNode) {
        TestCasesTreeItem item = new TestCasesTreeItem(testcaseNode);
        this.testCasesNavigator.setRoot(item);
    }

    public synchronized void refreshNavigatorTree() {
        testCasesNavigator.refresh();
    }

    public void refreshNavigatorTreeFromAnotherThread() {
        Platform.runLater(() -> testCasesNavigator.refresh());
    }

    public void openPrototype(TestNewNode testNewNode) {
        ITestcaseNode parent = testNewNode.getParent();
        List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

        if (names.size() == 1) {
            String name = ((TestNameNode) names.get(0)).getName();
            if (parent instanceof TestNormalSubprogramNode) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
                if (testCase != null) {
                    UIController.viewPrototype(testCase);
                }
            }
        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "There are more than two similar names in a test case ");
        }
    }

    public void openTestCase(TestNewNode testNewNode) {
        ITestcaseNode parent = testNewNode.getParent();
        List<ITestcaseNode> names = TestcaseSearch.searchNode(testNewNode, new TestNameNode());

        if (names.size() == 1) {
            String name = ((TestNameNode) names.get(0)).getName();
            if (parent instanceof TestNormalSubprogramNode) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
                if (testCase != null) {
                    UIController.viewTestCase(testCase);
                }
            } else if (parent instanceof TestCompoundSubprogramNode) {
                CompoundTestCase compoundTestCase = TestCaseManager.getCompoundTestCaseByName(name);
                if (compoundTestCase != null) {
                    UIController.viewTestCase(compoundTestCase);
                }
            }
        } else {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "There are more than two similar names in a test case ");
        }
    }

    /**
     * Delete single/compound test case or prototype test case
     *
     * @param testNewNode to get name of test case
     * @param item to update test case navigator tree
     */
    public void deleteTestCase(TestNewNode testNewNode, TestCasesTreeItem item) {
        ITestcaseNode parent = testNewNode.getParent();
        // remove the testcase
        parent.getChildren().remove(testNewNode);
        // save the testcases scripts to file .tst
        Environment.getInstance().saveTestcasesScriptToFile();
        // remove from the test case navigator
        if (item != null && item.getParent() != null && item.getParent().getChildren() != null)
            item.getParent().getChildren().remove(item);
        TestCasesNavigatorController.getInstance().getTestCasesNavigator().getSelectionModel().clearSelection();

        String name = testNewNode.getName();
        if (parent instanceof TestNormalSubprogramNode) {
            if (testNewNode.isPrototypeTestcase()) {
                // remove from disk
                TestCaseManager.removeBasicTestCase(name);
                // remove from MDI window
                MDIWindowController.getMDIWindowController().removePrototypeTab(name);
            } else {
                // remove from disk
                TestCaseManager.removeBasicTestCase(name);
                // remove from MDI window
                MDIWindowController.getMDIWindowController().removeTestCaseTab(name);
            }
        } else if (parent instanceof TestCompoundSubprogramNode) {
            // remove from disk
            TestCaseManager.removeCompoundTestCase(name);
            // remove from MDI window
            MDIWindowController.getMDIWindowController().removeCompoundTestCaseTab(name);
        }
        // refresh compound testcase tree table views that were opened
        UIController.refreshCompoundTestcaseViews();
    }

    public void clear() {
        testCasesNavigator.setRoot(null);
    }

//    private ISourcecodeFileNode searchSourceCodeFileNodeByPath(TestUnitNode unitNode, ProjectNode projectNode) {
//        if (unitNode.getSrcNode() == null) {
//            List<INode> sourcecodeNodes = Search.searchNodes(projectNode, new SourcecodeFileNodeCondition());
//            for (INode sourcecodeNode : sourcecodeNodes)
//                if (sourcecodeNode instanceof SourcecodeFileNode)
//                    if (sourcecodeNode.getAbsolutePath().equals(unitNode.getName())) {
//                        unitNode.setSrcNode((ISourcecodeFileNode) sourcecodeNode);
//                        break;
//                    }
//
//        }
//        return unitNode.getSrcNode();
//    }

//    private synchronized ICommonFunctionNode searchFunctionNodeByPath(String path, ProjectNode projectNode) {
//        ICommonFunctionNode matchedFunctionNode;
//        List<INode> functionNodes = Search.searchNodes(projectNode, new FunctionNodeCondition());
//        functionNodes.addAll(Search.searchNodes(projectNode, new MacroFunctionNodeCondition()));
//
//        for (INode functionNode : functionNodes)
//            if (functionNode instanceof ICommonFunctionNode)
//                if (functionNode.getAbsolutePath().equals(path)) {
//                    matchedFunctionNode = (ICommonFunctionNode) functionNode;
//                    return matchedFunctionNode;
//                }
//        return null;
//    }

//    public TreeTableColumn<ITestcaseNode, String> getColStatus() {
//        return colStatus;
//    }


}
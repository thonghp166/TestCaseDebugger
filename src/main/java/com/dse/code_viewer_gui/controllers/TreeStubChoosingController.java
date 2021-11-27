package com.dse.code_viewer_gui.controllers;

import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.objects.TestCasesTreeItem;
import com.dse.parser.funcdetail.FunctionDetailTree;
import com.dse.parser.object.*;
import com.dse.testcase_manager.FunctionNodeNotFoundException;
import com.dse.testcasescript.object.ITestcaseNode;
import com.dse.testcasescript.object.TestNameNode;
import com.dse.testcasescript.object.TestNewNode;
import com.dse.testcasescript.object.TestNormalSubprogramNode;
import com.dse.util.NodeType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import com.dse.util.AkaLogger;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * This class represent a controller for choosing stub window
 *
 * @author zizoz
 */
public class TreeStubChoosingController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(TreeStubChoosingController.class);

    private FunctionDetailTree finalTree;

    private FunctionNode functionNode;
    private TestCasesTreeItem treeItem;

    @FXML
    private TreeView<INode> treeView;
    @FXML
    private Button createBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private ToggleGroup stubDependency;
    @FXML
    private RadioButton allBtn;
    @FXML
    private RadioButton noneBtn;
    @FXML
    private RadioButton customBtn;

    /**
     * Constructor controller for choosing stub view
     * @param functionNode input function
     */
    public TreeStubChoosingController(FunctionNode functionNode) {
        this.functionNode = functionNode;
        this.finalTree = new FunctionDetailTree(functionNode);
    }

    public TreeStubChoosingController(TestCasesTreeItem item) {
        this.treeItem = item;
        try {
            IFunctionNode functionNode = (IFunctionNode) UIController.searchFunctionNodeByPath(((TestNormalSubprogramNode) item.getValue()).getName());
            this.functionNode = (FunctionNode) functionNode;
        } catch (FunctionNodeNotFoundException fe) {
            logger.debug("function node not found: " + fe.getFunctionPath());
        }
        this.finalTree = new FunctionDetailTree(functionNode);
    }

    /**
     * Three icons for UUT,STUB and DONT STUB
     * Two icons for function node and definitionFunctionNode
     */
    private final Node stubIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/stub_16.png")));
    private final Node dontstubIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/dontstub_16.png")));
    private final Node uutIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/uut_16.png")));
    private final Image functionIcon = new Image(getClass().getResourceAsStream("/img/f_16.png"));
    private final Image defIcon = new Image(getClass().getResourceAsStream("/img/n_16.png"));

    /**
     * Initialize radio buttons, tree view
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
        //Set list view value
        RootNode stubTree = functionDetailTree.getSubTreeRoot(NodeType.STUB);
        RootNode dontStubTree = functionDetailTree.getSubTreeRoot(NodeType.DONT_STUB);
        RootNode uutTree = functionDetailTree.getSubTreeRoot(NodeType.UUT);

        TreeItem<INode> rootNode = new TreeItem<>(functionNode);
        rootNode.setExpanded(true);
        TreeItem<INode> uutNode = new TreeItem<>(uutTree, uutIcon);
        uutNode.setExpanded(true);
        List<INode> uutList = uutTree.getElements();
        uutList.forEach(node -> {
            TreeItem<INode> child = new TreeItem<>(node,new ImageView(functionIcon));
            uutNode.getChildren().add(child);
        });
        TreeItem<INode> stubNode = new TreeItem<>(stubTree, stubIcon);
        stubNode.setExpanded(true);
        List<INode> stubList = stubTree.getElements();
        stubList.forEach(node -> {
            TreeItem<INode> child;
            if (node instanceof DefinitionFunctionNode){
                child = new TreeItem<>(node,new ImageView(defIcon));
            } else {
                child = new TreeItem<>(node,new ImageView(functionIcon));
            }
            stubNode.getChildren().add(child);
        });
        TreeItem<INode> notStubNode = new TreeItem<>(dontStubTree, dontstubIcon);
        notStubNode.setExpanded(true);
        List<INode> notStubList = dontStubTree.getElements();
        notStubList.forEach(node -> {
            TreeItem<INode> child = new TreeItem<>(node,new ImageView(functionIcon));
            notStubNode.getChildren().add(child);
        });

        //noinspection unchecked
        rootNode.getChildren().addAll(uutNode, stubNode, notStubNode);
        treeView.setRoot(rootNode);
        treeView.setShowRoot(false);
        treeView.setCellFactory(tree -> new CellContextMenu());
        treeView.setDisable(true);

        // Group of radio button
        stubDependency.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            String text = ((RadioButton) newValue).getText();
            try {
                switch (text) {
                    case "All": {
                        treeView.setDisable(true);
                        this.finalTree.stubAll();
                        break;
                    }
                    case "None":{
                        treeView.setDisable(true);
                        this.finalTree.dontStubAll();
                        break;
                    }
                    case "Custom": {
                        treeView.setDisable(false);
                        break;
                    }
                }
            } catch (Exception e){
                 e.printStackTrace();
            }
        });

        // Setting for bottom buttons
        this.cancelBtn.setOnAction(event -> {
            this.finalTree = functionDetailTree;
            ((Stage) cancelBtn.getScene().getWindow()).close();
        });
        this.createBtn.setOnAction(event -> {
            ((Stage) createBtn.getScene().getWindow()).close();

            ITestcaseNode subprogramNode = treeItem.getValue();
            TestNewNode testNewNode = new TestNewNode();

            // TODO: need a class for generating name for testcase
            String testCaseName = functionNode.getSimpleName() + "." + new Random().nextInt(100000);
            TestNameNode testNameNode = new TestNameNode();
            testNewNode.getChildren().add(testNameNode);
            testNameNode.setName(testCaseName);

            treeItem.setExpanded(true);
            subprogramNode.getChildren().add(testNewNode);
            testNewNode.setParent(subprogramNode);
            TestCasesTreeItem newTestCaseTreeItem = new TestCasesTreeItem(testNewNode);
            treeItem.getChildren().add(newTestCaseTreeItem);

            // select the test case item on navigator tree and render testcase view in MDI window
//            LeftPaneController.getLeftPaneController().getNavigatorController().viewTestCase(newTestCaseTreeItem);

            // save the testcases scripts to file .tst
            Environment.getInstance().saveTestcasesScriptToFile();

            // render test case editor
            //UIController.viewTestCase(testNewNode);
        });
    }

    // tmp
    static int id = 0;

    /**
     * This inner class represent for a cell's context menu in tree view
     */
    private final class CellContextMenu extends TreeCell<INode> {

        private final ContextMenu changeMenu = new ContextMenu();

        CellContextMenu() {
            MenuItem stub = new MenuItem("Stub this function");
            changeMenu.getItems().add(stub);
            stub.setOnAction((ActionEvent t) -> {
                FunctionNode fn = (FunctionNode) getItem();
                change(fn,NodeType.STUB);
            });
            MenuItem notStub = new MenuItem("Do not stub this function");
            changeMenu.getItems().add(notStub);
            notStub.setOnAction((ActionEvent t) -> {
                FunctionNode fn = (FunctionNode) getItem();
                change(fn,NodeType.DONT_STUB);
            });
        }

        /**
         * Stub or do not stub function
         * @param fn function node
         * @param nodeType type of change: STUB or DONT STUB
         */
        private void change(FunctionNode fn,NodeType nodeType){
            try {
                if (nodeType == NodeType.STUB){
                    finalTree.stub(fn);
                } else if (nodeType == NodeType.DONT_STUB){
                    finalTree.dontStub(fn);
                }
                TreeItem<INode> item = getTreeItem();
                TreeItem<INode> itemParent = item.getParent();
                TreeItem<INode> itemGrand = treeView.getRoot();

                // add item to new parent
                itemGrand.getChildren().forEach(child -> {
                    if (((RootNode) child.getValue()).getType() == nodeType){
                        child.getChildren().add(new TreeItem<>(getItem(),new ImageView(functionIcon)));
                    }
                });

                itemParent.getChildren().remove(item);
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }

        /**
         * update tree view
         * @param item item selected
         * @param empty never mind
         */
        @Override
        public void updateItem(INode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(getString());
                setGraphic(getTreeItem().getGraphic());
                INode node = getItem();
                if (node instanceof FunctionNode) {
                    FunctionNode fn = (FunctionNode) node;
                    setTooltip(new Tooltip("This function was defined"));
                    if (finalTree.getTypeOf(fn) == NodeType.STUB){
                        changeMenu.getItems().get(0).setDisable(true);
                        changeMenu.getItems().get(1).setDisable(false);
                        setContextMenu(changeMenu);
                    } else if (finalTree.getTypeOf(fn) == NodeType.DONT_STUB){
                        changeMenu.getItems().get(0).setDisable(false);
                        changeMenu.getItems().get(1).setDisable(true);
                        setContextMenu(changeMenu);
                    }
                } else if (node instanceof DefinitionFunctionNode){
                    setTooltip(new Tooltip("This function was not defined.\nIt should be stubbed"));
                }
            }
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }
}

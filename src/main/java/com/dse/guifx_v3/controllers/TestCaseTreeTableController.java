package com.dse.guifx_v3.controllers;

import com.dse.guifx_v3.objects.*;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.object.*;
import com.dse.util.AkaLogger;
import com.dse.util.NodeType;
import com.dse.util.TemplateUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TestCaseTreeTableController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(TestCaseTreeTableController.class);

    private TreeItem<DataNode> root;
    private TestCase testCase;

    @FXML
    private TreeTableColumn<DataNode, String> typeCol;
    @FXML
    private TreeTableColumn<DataNode, Boolean> parameterCol;
    @FXML
    private TreeTableColumn<DataNode, String> inputCol;
    @FXML
    private TreeTableColumn<DataNode, String> outputCol;
    @FXML
    private TreeTableView<DataNode> treeTableView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root = new TreeItem<>(new RootDataNode());

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        treeTableView.setEditable(true);
        treeTableView.getSelectionModel().setCellSelectionEnabled(true);

        final PseudoClass pseudoClassInput = PseudoClass.getPseudoClass("input-column-is-selected");
        final PseudoClass pseudoClassExpected = PseudoClass.getPseudoClass("expected-output-column-is-selected");

        treeTableView.setRowFactory(param -> {
            final TreeTableRow<DataNode> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((o, oldValue, newValue) -> {
                if (newValue != null) {
                    if (row.getTreeItem() instanceof TestDataTreeItem) {
                        if (((TestDataTreeItem) row.getTreeItem()).getColumnType() == TestDataTreeItem.ColumnType.INPUT) {
                            row.pseudoClassStateChanged(pseudoClassInput, true);
                        } else if (((TestDataTreeItem) row.getTreeItem()).getColumnType() == TestDataTreeItem.ColumnType.EXPECTED) {
                            row.pseudoClassStateChanged(pseudoClassExpected, true);
                        }
                    }

                    if (row.getTreeItem() != null) {
                        row.getTreeItem().expandedProperty().addListener(event -> {
                            row.getTreeTableView().refresh();
                        });
                    }

                }
            });

            return row;
        });

        typeCol.setCellValueFactory(param -> {
            String type = null;

            DataNode node = param.getValue().getValue();

            if (node instanceof ValueDataNode)
                type = ((ValueDataNode) node).getType();

            if (type != null)
                type = TemplateUtils.deleteTemplateParameters(type);

            return new SimpleStringProperty(type);
        });
    }

    private void loadContent(TestCase testCase) {
        if (testCase != null) {
            RootDataNode rootDataNode = testCase.getRootDataNode();
//        root = new TreeItem<DataNode>(rootDataNode);
//        root.getChildren().clear();
//        loadChildren(root);
//        treeTableView.setRoot(root);
            root = new TestDataTreeItem(rootDataNode);
            root.getChildren().clear();
            loadChildren(testCase, root);
            treeTableView.setRoot(root);
        }
    }

    public void loadTestCase(TestCase testCase) {
        if (testCase != null) {
            this.testCase = testCase;
            inputCol.setCellFactory(new InputColumnCellFactory(testCase));
            outputCol.setCellFactory(new ExpectedOutputColumnCellFactory(testCase));
            parameterCol.setCellFactory(new ParameterColumnCellFactory(testCase));
            loadContent(testCase);
        }
    }

    public static void loadChildren(TestCase testCase, TreeItem<DataNode> treeItem, List<String> children) {
        if (treeItem == null)
            return;

        treeItem.getChildren().clear();

        TestDataTreeItem.ColumnType columnType = TestDataTreeItem.ColumnType.NONE;
        if (treeItem instanceof TestDataTreeItem) {
            columnType = ((TestDataTreeItem) treeItem).getColumnType();

            if (treeItem instanceof TestDataParameterTreeItem) {
                columnType = ((TestDataParameterTreeItem) treeItem).getSelectedColumn();
            } else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
                columnType = ((TestDataGlobalVariableTreeItem) treeItem).getSelectedColumn();
            }
        }

        DataNode node = treeItem.getValue();
        for (IDataNode child : node.getChildren()) {
            if (children.contains(child.getName())) {

                DataNode n = (DataNode) child;
                TreeItem<DataNode> item = new TestDataTreeItem(n);
                ((TestDataTreeItem) item).setColumnType(columnType);

                if (n instanceof SubprogramNode && !(child instanceof ConstructorDataNode))
                    item = new CheckBoxTreeItem<>(n);
                else if (isParameter(testCase, n))
                    item = new TestDataParameterTreeItem(n);
                else if (isGlobalVariable(n))
                    item = new TestDataGlobalVariableTreeItem(n);

                treeItem.getChildren().add(item);

                if (n.getChildren() != null) {
                    loadChildren(testCase, item);
                }
            }
        }


//        if (treeItem == null)
//            return;
//
//        treeItem.getChildren().clear();
//
//        DataNode node = treeItem.getValue();
//        for (IDataNode child: node.getChildren()) {
//            if (children.contains(child.getName())) {
//                DataNode n = (DataNode) child;
//                TreeItem<DataNode> item = new TreeItem<>(n);
//
//                if (child instanceof SubprogramNode && !(child instanceof ConstructorDataNode))
//                    item = new CheckBoxTreeItem<>(n);
//
//                treeItem.getChildren().add(item);
//
//                if (n.getChildren() != null)
//                    loadChildren(item);
//            }
//        }
    }

    public static void loadChildren(TestCase testCase, TreeItem<DataNode> treeItem) {

        if (treeItem == null)
            return;

        treeItem.getChildren().clear();

        TestDataTreeItem.ColumnType columnType = TestDataTreeItem.ColumnType.NONE;
        if (treeItem instanceof TestDataTreeItem) {
            columnType = ((TestDataTreeItem) treeItem).getColumnType();

            if (treeItem instanceof TestDataParameterTreeItem) {
                columnType = ((TestDataParameterTreeItem) treeItem).getSelectedColumn();
            } else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
                columnType = ((TestDataGlobalVariableTreeItem) treeItem).getSelectedColumn();
            }

        } else if (isStubFuntion(treeItem.getValue())) { // if the dataNode is StubFunction then column type is ColumnType.EXPECTED
            columnType = TestDataTreeItem.ColumnType.EXPECTED;
        }

        DataNode node = treeItem.getValue();
        for (IDataNode child : node.getChildren()) {
            DataNode n = (DataNode) child;
            TreeItem<DataNode> item = new TestDataTreeItem(n);
            ((TestDataTreeItem) item).setColumnType(columnType);

            if (n instanceof SubprogramNode && !(child instanceof ConstructorDataNode))
                item = new CheckBoxTreeItem<>(n);
            else if (isParameter(testCase, n))
                item = new TestDataParameterTreeItem(n);
            else if (isGlobalVariable(n))
                item = new TestDataGlobalVariableTreeItem(n);

            treeItem.getChildren().add(item);

            if (n.getChildren() != null) {
                loadChildren(testCase, item);
            }
        }
    }

    private static boolean isStubFuntion(DataNode dataNode) {
        return dataNode instanceof SubprogramNode
                && ((SubprogramNode) dataNode).isStubable()
                && dataNode.getChildren().size() > 0;
    }

    private static boolean isGlobalVariable(DataNode dataNode) {
        return dataNode.getParent() instanceof RootDataNode && ((RootDataNode) dataNode.getParent()).getLevel().equals(NodeType.GLOBAL);
    }

    //sua tam
    public static void loadChildren(TreeItem<DataNode> treeItem) {
        if (treeItem == null)
            return;

        treeItem.getChildren().clear();

        DataNode node = treeItem.getValue();
        for (IDataNode child : node.getChildren()) {
            DataNode n = (DataNode) child;
            TreeItem<DataNode> item = new TreeItem<>(n);

            if (child instanceof SubprogramNode && !(child instanceof ConstructorDataNode))
                item = new CheckBoxTreeItem<>(n);

            treeItem.getChildren().add(item);

            if (n.getChildren() != null) {
                loadChildren(item);
            }
        }
    }

    public static boolean isParameter(TestCase testCase, DataNode dataNode) {
        if (dataNode.getParent() instanceof SubprogramNode) {
            // get grandparent to distinguish with parameter of a constructor of a global variable
            if (dataNode.getParent().getParent() instanceof UnitUnderTestNode) {
                ICommonFunctionNode sut = testCase.getFunctionNode();
                return sut == ((SubprogramNode) dataNode.getParent()).getFunctionNode();
            }
        }

        return false;
    }

    // temporary
    public TreeTableView<DataNode> getTreeTableView() {
        return treeTableView;
    }
}

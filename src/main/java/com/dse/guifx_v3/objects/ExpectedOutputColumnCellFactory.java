package com.dse.guifx_v3.objects;

import com.dse.guifx_v3.controllers.TestCaseTreeTableController;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testdata.InputCellHandler;
import com.dse.testdata.object.*;
import com.dse.util.AkaLogger;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

public class ExpectedOutputColumnCellFactory implements Callback<TreeTableColumn<DataNode, String>, TreeTableCell<DataNode, String>> {
    private final static AkaLogger logger = AkaLogger.get(ExpectedOutputColumnCellFactory.class);
    private TestCase testCase;

    public ExpectedOutputColumnCellFactory(TestCase testCase) {
        super();
        this.testCase = testCase;
    }

    @Override
    public TreeTableCell<DataNode, String> call(TreeTableColumn<DataNode, String> param) {
        ExpectedCell cell = new ExpectedCell(testCase);
        cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            TreeItem<DataNode> treeItem = cell.getTreeTableRow().getTreeItem();
            if (treeItem instanceof TestDataParameterTreeItem) {
                if (!treeItem.getValue().getName().equals("RETURN")) {
                    if (((TestDataParameterTreeItem) treeItem).getSelectedColumn() != TestDataTreeItem.ColumnType.EXPECTED) {
                        ((TestDataParameterTreeItem) treeItem).setSelectedColumn(TestDataTreeItem.ColumnType.EXPECTED);
                        cell.getTreeTableView().refresh();
                        // lazy load
                        if (treeItem.getChildren().size() == 0) {
                            TestCaseTreeTableController.loadChildren(testCase, treeItem);
                        }
                    }
                }
            } else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
                if (((TestDataGlobalVariableTreeItem) treeItem).getSelectedColumn() != TestDataTreeItem.ColumnType.EXPECTED) {
                    ((TestDataGlobalVariableTreeItem) treeItem).setSelectedColumn(TestDataTreeItem.ColumnType.EXPECTED);
                    cell.getTreeTableView().refresh();
                    // lazy load
                    if (treeItem.getChildren().size() == 0) {
                        TestCaseTreeTableController.loadChildren(testCase, treeItem);
                    }
                }
            }
        });

        return cell;
    }

    /**
     * Represents a single row/column in the test case tab
     */
    private class ExpectedCell extends AbstractTableCell {
        public ExpectedCell(TestCase testCase) {
            super(testCase);
        }

        @Override
        public void startEdit() {
            logger.debug("Start editing on the cell at line " + this.getIndex());
            super.startEdit();
            saveValueWhenUsersPressEnter();

            if (getTreeTableRow().getTreeItem() instanceof TestDataTreeItem) {
                TestDataTreeItem testDataTreeItem = (TestDataTreeItem) getTreeTableRow().getTreeItem();
                TestDataTreeItem.ColumnType columnType = testDataTreeItem.getColumnType();

                // if the value is parameter of Stub Function, and not is a return node
                if (! (columnType.equals(TestDataTreeItem.ColumnType.EXPECTED) && testDataTreeItem.getValue().getName().equals("RETURN"))) {
                    if (columnType != TestDataTreeItem.ColumnType.INPUT) {
                        showText(CellType.EXPECTED);
                    }
                }
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            getTreeTableView().refresh();
            logger.debug("Canceled the edit on the cell");
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);

            TreeTableRow<DataNode> row = getTreeTableRow();
            DataNode dataNode = row.getItem();

            if (dataNode == null) {
                logger.debug("There is matching between a cell and its data");
            } else {

                try {
                    if (row.getTreeItem() instanceof TestDataParameterTreeItem) {
                        if (dataNode.getName().equals("RETURN")) {
                            new InputCellHandler().commitEdit((ValueDataNode) dataNode, newValue);
                            // reload các con của tree item
                            if (!(dataNode instanceof NormalDataNode || dataNode instanceof EnumDataNode)) {
                                TreeItem<DataNode> treeItem = row.getTreeItem();
                                TestCaseTreeTableController.loadChildren(testCase, treeItem);
                            }
                        } else {
                            ValueDataNode expectedOutput = (ValueDataNode) ((TestDataParameterTreeItem) row.getTreeItem()).getExpectedOutputDataNode();
                            new InputCellHandler().commitEdit(expectedOutput, newValue);
                            // reload các con của tree item
                            if (!(expectedOutput instanceof NormalDataNode || expectedOutput instanceof EnumDataNode)) {
                                TreeItem<DataNode> treeItem = row.getTreeItem();
                                TestCaseTreeTableController.loadChildren(testCase, treeItem);
                            }
                        }

                    } else  if (row.getTreeItem() instanceof TestDataGlobalVariableTreeItem) {
                        ValueDataNode expectedOutput = (ValueDataNode) ((TestDataGlobalVariableTreeItem) row.getTreeItem()).getExpectedOutputDataNode();
                        new InputCellHandler().commitEdit(expectedOutput, newValue);
                        // reload các con của tree item
                        if (!(expectedOutput instanceof NormalDataNode || expectedOutput instanceof EnumDataNode)) {
                            TreeItem<DataNode> treeItem = row.getTreeItem();
                            TestCaseTreeTableController.loadChildren(testCase, treeItem);
                        }
                    } else {
                        new InputCellHandler().commitEdit((ValueDataNode) dataNode, newValue);

                        // reload các con của tree item
                        if (!(dataNode instanceof NormalDataNode || dataNode instanceof EnumDataNode)) {
                            TreeItem<DataNode> treeItem = row.getTreeItem();
                            TestCaseTreeTableController.loadChildren(testCase, treeItem);
                        }
                    }

                } catch (Exception ex) {
                    logger.error("Error " + ex.getMessage() + " when entering data for " + dataNode.getClass());
                }

                getTreeTableView().refresh();
                logger.debug("Refreshed the current test case tab");

                // save data tree to the test script
                TestCaseManager.exportBasicTestCaseToFile(testCase);
            }
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            TreeItem<DataNode> treeItem = getTreeTableRow().getTreeItem();
            if (treeItem != null && treeItem.getValue() != null) {
                if (treeItem instanceof TestDataTreeItem
                        && ((TestDataTreeItem) treeItem).getColumnType().equals(TestDataTreeItem.ColumnType.EXPECTED)
                        && treeItem.getValue().getName().equals("RETURN")) {
                    setEditable(false);
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (treeItem instanceof TestDataTreeItem
                        && ((TestDataTreeItem) treeItem).getColumnType() == TestDataTreeItem.ColumnType.INPUT) {
                    setEditable(false);
                    setText(null);
                    setGraphic(null);
                    return;
                }

                final PseudoClass pseudoClass = PseudoClass.getPseudoClass("expected-output-column-parameter");
                if (treeItem instanceof TestDataParameterTreeItem || treeItem instanceof TestDataGlobalVariableTreeItem) {
                    pseudoClassStateChanged(pseudoClass, true);
                }

                DataNode dataNode = treeItem.getValue();

                // enable input when the variable is return variable
//                if (dataNode instanceof ValueDataNode)
//                    if (!((ValueDataNode) dataNode).isExpected()) {
////                        disable();
//                        return;
//                    }

                // if the dataNode is a parameter, show value of Expected Output
                if (treeItem instanceof TestDataParameterTreeItem) {
                    if (dataNode.getName().equals("RETURN")) {
                        new InputCellHandler().update(this, dataNode);
                    } else {
                        ValueDataNode expectedOutput = (ValueDataNode) ((TestDataParameterTreeItem) treeItem).getExpectedOutputDataNode();
                        new InputCellHandler().update(this, expectedOutput);
                    }
                } else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
                    ValueDataNode expectedOutput = (ValueDataNode) ((TestDataGlobalVariableTreeItem) treeItem).getExpectedOutputDataNode();
                    new InputCellHandler().update(this, expectedOutput);
                } else if (treeItem.getValue() instanceof TemplateSubprogramDataNode) {
                    // show nothing
                } else {
                    new InputCellHandler().update(this, dataNode);
                }
            } else {
                setText(null);
                setGraphic(null);
            }
        }
    }

//    private boolean isParameter(DataNode dataNode) {
//        if (dataNode.getParent() instanceof SubprogramNode) {
//            IFunctionNode sut = testCase.getFunctionNode();
//            return sut == ((SubprogramNode) dataNode.getParent()).getFunctionNode();
//        }
//
//        return false;
//    }
}

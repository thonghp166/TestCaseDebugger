package com.dse.guifx_v3.objects;

import com.dse.guifx_v3.controllers.TestCaseTreeTableController;
import com.dse.guifx_v3.controllers.TestCasesNavigatorController;
import com.dse.guifx_v3.helps.DataNodeIterationDialog;
import com.dse.guifx_v3.helps.UIController;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testdata.Iterator;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.testdata.gen.module.subtree.InitialStubTreeGen;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import com.dse.util.AkaLogger;

import java.util.List;

public class ParameterColumnCellFactory implements Callback<TreeTableColumn<DataNode, Boolean>, TreeTableCell<DataNode, Boolean>> {
    private final static AkaLogger logger = AkaLogger.get(ParameterColumnCellFactory.class);
    private TestCase testCase;

    public ParameterColumnCellFactory(TestCase testCase) {
        super();
        this.testCase = testCase;
    }

    @Override
    public TreeTableCell<DataNode, Boolean> call(TreeTableColumn<DataNode, Boolean> param) {
        return new ParameterColumnCell(testCase);
    }

    /**
     * Represents a single row/column in the test case tab
     */
    public static class ParameterColumnCell extends CheckBoxTreeTableCell<DataNode, Boolean> {
        private TestCase testCase;

        private ObservableValue<Boolean> booleanProperty;

        private BooleanProperty indeterminateProperty;

        public ParameterColumnCell(TestCase testCase) {
            this.testCase = testCase;
        }

        private ContextMenu setupContextMenu(DataNode dataNode) {
            ContextMenu contextMenu = new ContextMenu();

            if (dataNode instanceof ArrayDataNode || dataNode instanceof PointerDataNode
                    || dataNode instanceof ListBaseDataNode) {
                MenuItem miExpandItem = new MenuItem("Expand children");
                miExpandItem.setOnAction(event -> UIController.showArrayExpanderDialog(this));
                contextMenu.getItems().add(miExpandItem);
            }

            if (dataNode instanceof MacroSubprogramDataNode) {
                MenuItem miDeclareType = new MenuItem("Define macro type");
                miDeclareType.setOnAction(event ->
                        UIController.showMacroTypeDefinitionDialog((MacroSubprogramDataNode) dataNode,this));
                contextMenu.getItems().add(miDeclareType);
            }

            if (dataNode instanceof TemplateSubprogramDataNode) {
                MenuItem miDeclareType = new MenuItem("Define template type");
                miDeclareType.setOnAction(event ->
                        UIController.showTemplateTypeDefinitionDialog((TemplateSubprogramDataNode) dataNode,this));
                contextMenu.getItems().add(miDeclareType);
            }

            if (dataNode instanceof ValueDataNode && ((ValueDataNode) dataNode).isStubArgument()) {
                MenuItem miIterator = new MenuItem("Add iterator");
                miIterator.setOnAction(event -> {
                    DataNodeIterationDialog dialog = new DataNodeIterationDialog((ValueDataNode) dataNode) {

                        @Override
                        public void onExpand(Iterator iterator) {
                            if (iterator.getRepeat() == Iterator.FILL_ALL && iterator.getStartIdx() == 0)
                                return;

                            DataNode iteratorNode = iterator.getDataNode();

                            TestDataTreeItem child = new TestDataTreeItem(iteratorNode);
                            child.setColumnType(TestDataTreeItem.ColumnType.EXPECTED);
                            TreeItem<DataNode> current = getTreeTableRow().getTreeItem();
                            TreeItem<DataNode> parent = current.getParent();

                            int index = parent.getChildren().indexOf(current);
                            parent.getChildren().remove(index);
                            parent.getChildren().add(index, child);

//                            current.getChildren().clear();
//                            current.getChildren().add(child);
                            getTreeTableView().refresh();

                            TestCaseManager.exportBasicTestCaseToFile(testCase);
                        }
                    };

                    Platform.runLater(dialog::showAndWait);
                });

                contextMenu.getItems().add(miIterator);
            }

            return contextMenu;
        }

        private String getDisplayName(DataNode dataNode) {
            String displayName = dataNode.getDisplayNameInParameterTree();

            TreeItem<DataNode> treeItem = getTreeTableRow().getTreeItem();
            TreeItem<DataNode> parentTreeItem = treeItem.getParent();
            DataNode parentNode = parentTreeItem.getValue();

            if (parentNode.getDisplayNameInParameterTree().equals(displayName)) {
                if (parentNode instanceof ValueDataNode) {
                    Iterator firstIterator = ((ValueDataNode) parentNode).getIterators().get(0);
                    if (dataNode == firstIterator.getDataNode()) {
                        displayName = firstIterator.getDisplayName();
                    }
                }
            }

            return displayName;
        }

        private void updateDisplay(DataNode dataNode) {
            if (dataNode != null) {
                String displayName = getDisplayName(dataNode);
                Label label = new Label(displayName);
                ContextMenu contextMenu = setupContextMenu(dataNode);

                if (!contextMenu.getItems().isEmpty())
                    setContextMenu(contextMenu);

                // Các node show checkbox
                if (dataNode instanceof SubprogramNode && ((SubprogramNode) dataNode).isStubable()) {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setAlignment(Pos.TOP_LEFT);
                    checkBox.addEventFilter(MouseEvent.MOUSE_PRESSED,
                            event -> commitEdit(dataNode.getChildren().isEmpty()));
                    bindingCheckbox(dataNode, checkBox);

                    label.setGraphic(checkBox);
                    setGraphic(label);
                    label.requestFocus();

                    setText(null);
                }

                // Các node show text
                else {
                    setGraphic(label);
                    label.requestFocus();
                }

            } else {
                logger.debug("There is no matching between a cell and a data node");
            }
        }

        private void bindingCheckbox(DataNode node, CheckBox checkBox) {
            // uninstall bindings
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            if (indeterminateProperty != null) {
                checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
            }

            // install new bindings.
            // this can only handle TreeItems of type CheckBoxTreeItem
            TreeItem<DataNode> treeItem = getTreeTableRow().getTreeItem();

            if (treeItem instanceof CheckBoxTreeItem) {
                CheckBoxTreeItem<DataNode> cbti = (CheckBoxTreeItem<DataNode>) treeItem;
                booleanProperty = new SimpleBooleanProperty(!node.getChildren().isEmpty());
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);

                indeterminateProperty = cbti.indeterminateProperty();
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);
            }
        }

        @Override
        public void startEdit() {
            logger.debug("Start editing on the cell at line " + this.getIndex());
            super.startEdit();

            setText(null);

//            TreeItem<DataNode>
            DataNode dataNode = getTreeTableRow().getTreeItem().getValue();
            updateDisplay(dataNode);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            getTreeTableView().refresh();
            logger.debug("Canceled the edit on the cell");
        }

        @Override
        public void commitEdit(Boolean newValue) {
            super.commitEdit(newValue);

            TestCasesNavigatorController.getInstance().refreshNavigatorTree();

            TreeTableRow<DataNode> row = getTreeTableRow();
            DataNode dataNode = row.getItem();

            if (dataNode == null) {
                logger.debug("There is matching between a cell and its data");
            }

            TreeItem<DataNode> treeItem = row.getTreeItem();

            if (dataNode instanceof SubprogramNode && ((SubprogramNode) dataNode).isStubable()) {
                SubprogramNode subprogram = (SubprogramNode) dataNode;

                subprogram.getChildren().clear();

                if (newValue) {
                    try {
                        new InitialStubTreeGen().addSubprogram((SubprogramNode) dataNode);
                    } catch (Exception e) {
                        logger.debug("Error " + e.getMessage() + " when stub " + dataNode.getDisplayNameInParameterTree());
                    }
                }

                TestCaseTreeTableController.loadChildren(testCase, treeItem);

                getTreeTableView().refresh();
                logger.debug("Refreshed the current test case tab");

                // save data tree to the test script
                TestCaseManager.exportBasicTestCaseToFile(testCase);
            }
        }

        @Override
        public void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);

            if (getTreeTableRow().getTreeItem() != null) {
                DataNode dataNode = getTreeTableRow().getTreeItem().getValue();

                setEditable(true);

                updateDisplay(dataNode);

            } else {
                setText(null);
                setGraphic(null);
            }
        }

        public void refresh() {
            TreeTableRow<DataNode> row = getTreeTableRow();
            TreeItem<DataNode> treeItem = row.getTreeItem();

            TestCaseTreeTableController.loadChildren(testCase, treeItem);

            getTreeTableView().refresh();
            logger.debug("Refreshed the current test case tab");

            // save data tree to the test script
            TestCaseManager.exportBasicTestCaseToFile(testCase);
        }

        public void expandArrayItems(String input) throws Exception {
            TreeItem<DataNode> treeItem = getTreeTableRow().getTreeItem();
            DataNode node = treeItem.getValue();

            List<String> expanded = new TreeExpander()
                    .expandArrayItemByIndex((ValueDataNode) node, input);

            TestCaseTreeTableController.loadChildren(testCase, treeItem, expanded);

            getTreeTableView().refresh();
            logger.debug("Refreshed the current test case tab");

            // save data tree to the test script
            TestCaseManager.exportBasicTestCaseToFile(testCase);
        }
    }
}
package com.dse.guifx_v3.objects;

import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.*;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.testdata.object.stl.STLArrayDataNode;
import com.dse.testdata.object.stl.SmartPointerDataNode;
import com.dse.util.SpecialCharacter;
import com.dse.util.TemplateUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.dse.util.AkaLogger;
import org.fxmisc.richtext.CodeArea;

import java.util.List;

public abstract class AbstractTableCell extends TreeTableCell<DataNode, String> {
    private final static AkaLogger logger = AkaLogger.get(AbstractTableCell.class);

    private TestCase testCase;
    private TextField textField = null;

    public enum CellType {
        INPUT,
        EXPECTED,
    }

    AbstractTableCell(TestCase testCase) {
        this.testCase = testCase;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    protected void showText(CellType type) {
        DataNode dataNode = getTreeTableRow().getTreeItem().getValue();

        if (dataNode != null) {
            logger.debug("The type of data node corresponding to the cell: " + dataNode.getClass());
            // if it is a return variable node then ignore
            boolean isReturnNode = dataNode instanceof ValueDataNode && ((ValueDataNode) dataNode).isExpected();

//            if ((isReturnNode && type == CellType.INPUT) || (!isReturnNode && type == CellType.EXPECTED)) {
            if ((isReturnNode && type == CellType.INPUT)) {
//                disable();
                return;
            }

            // Các node cần nhập vào text field
            if (dataNode instanceof NormalDataNode // number and character
                    || (dataNode instanceof OneDimensionDataNode && !((OneDimensionDataNode) dataNode).isFixedSize()) // array cua normal data
                    || (dataNode instanceof MultipleDimensionDataNode && !((MultipleDimensionDataNode) dataNode).isFixedSize())
                    || (dataNode instanceof ListBaseDataNode && !(dataNode instanceof STLArrayDataNode))
                    || dataNode instanceof PointerDataNode) {
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();

            } else if (dataNode instanceof EnumDataNode // enum
                    || dataNode instanceof ClassDataNode // include SubClassDataNode
                    || dataNode instanceof UnionDataNode  // union
                    || dataNode instanceof TemplateSubprogramDataNode
                    || dataNode instanceof SmartPointerDataNode
                    || (dataNode instanceof SubprogramNode && ((SubprogramNode) dataNode).isStubable())) {
                // Các node cần có combo-box
                setGraphic(createComboBox(dataNode));
                setText(null);

            } else if (dataNode instanceof UnresolvedDataNode) {
                CodeArea codeArea = UIController.showCodeEditorDialog((UnresolvedDataNode) dataNode, this);
                codeArea.selectAll();
                codeArea.requestFocus();
                setText("User code");

            } else {
                logger.debug("Do not support to enter data for " + dataNode.getClass());
            }
        } else {
            logger.debug("There is no matching between a cell and a data node");
        }
    }

    protected void saveValueWhenUsersPressEnter() {
//            logger.debug("Set event when users click enter on the cell");
        if (textField == null) {
            textField = new TextField();
            textField.setOnKeyReleased((KeyEvent t) -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                }
            });
        }
    }

    protected ComboBox<String> createComboBox(DataNode dataNode) {
        ComboBox<String> comboBox = new ComboBox<>();
        ObservableList<String> options = FXCollections.observableArrayList();

        // enum oke
        if (dataNode instanceof EnumDataNode) {
            List<String> list = ((EnumDataNode) dataNode).getAllNameEnumItems();
            options = FXCollections.observableArrayList(list);
            if (!((EnumDataNode) dataNode).isSetValue()) {
                comboBox.setValue("Select value");
            } else {
                comboBox.setValue(((EnumDataNode) dataNode).getValue());
            }
        }
        // union oke
        else if (dataNode instanceof UnionDataNode) {
            INode node = ((UnionDataNode) dataNode).getCorrespondingType();
            if (node instanceof UnionNode) {
                UnionNode unionNode = (UnionNode) node;
                List<Node> list = unionNode.getChildren();
                for (Node child : list) {
                    options.add(child.getName());
                }
            }

            comboBox.setValue("Select attribute");
            if (!dataNode.getChildren().isEmpty()) {
                comboBox.setValue(dataNode.getChildren().get(0).getName());
            }
        }
        // subclass (cũng là class) oke
        else if (dataNode instanceof SubClassDataNode) {
            try {
                List<ICommonFunctionNode> list = ((SubClassDataNode) dataNode).getConstructorsOnlyInCurrentClass();

                for (ICommonFunctionNode node : list) {
                    if (!options.contains(node.getName())) {
                        options.add(node.getName());
                    }
                }

                comboBox.setValue("Select constructor");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (dataNode instanceof ClassDataNode) {
            // class oke
            options.add(SpecialCharacter.EMPTY);

            List<INode> list = ((ClassDataNode) dataNode).getDerivedClass();
            for (INode node : list)
                options.add(node.getName());

            comboBox.setValue("Select real class");
        } else if (dataNode instanceof TemplateSubprogramDataNode) {
            ICommonFunctionNode functionNode = (ICommonFunctionNode) ((TemplateSubprogramDataNode) dataNode).getFunctionNode();

            List<DefinitionFunctionNode> suggestions = TemplateUtils.getPossibleTemplateArguments(functionNode);

            for (INode suggestion : suggestions)
                options.add(suggestion.toString());

            comboBox.setValue("Select template parameters");
        } else if (dataNode instanceof SmartPointerDataNode) {
            SmartPointerDataNode smartPtrDataNode = (SmartPointerDataNode) dataNode;
            String[] constructors = smartPtrDataNode.getConstructorsWithTemplateArgument();

            options.addAll(constructors);
            comboBox.setValue("Choose constructor");
        }

        comboBox.setItems(options);
        // Chỉnh sửa cho combobox vừa với ô của tree table.
        comboBox.setMaxWidth(getTableColumn().getMaxWidth());
        // Khi chọn giá trị trong combobox thì commit giá trị đó.
        comboBox.valueProperty().addListener((ov, oldValue, newValue) -> commitEdit(newValue));
        return comboBox;
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
    }

    protected void disable() {
        setDisable(true);
        setStyle("-fx-text-fill: grey");
    }
}
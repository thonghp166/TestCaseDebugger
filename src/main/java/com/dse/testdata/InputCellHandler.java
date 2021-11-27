package com.dse.testdata;

import auto_testcase_generation.config.PrimitiveBound;
import com.dse.config.IFunctionConfigBound;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.objects.AbstractTableCell;
import com.dse.parser.object.StructNode;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.testdata.object.stl.STLArrayDataNode;
import com.dse.testdata.object.stl.SmartPointerDataNode;
import com.dse.util.AkaLogger;
import com.dse.util.VariableTypeUtils;
import com.dse.util.bound.DataSizeModel;
import javafx.scene.control.TreeTableCell;

import java.util.Map;

public class InputCellHandler implements IInputCellHandler {
    public final static AkaLogger logger = AkaLogger.get(AbstractTableCell.class);

    // store the template type to real type in template function, e.g, "T"->"int"
    // key: template type
    // value: real type
    private Map<String, String> realTypeMapping;

    public InputCellHandler(Map<String, String> realTypeMapping){
        this.realTypeMapping = realTypeMapping;
    }

    public InputCellHandler(){

    }

    @Override
    public void update(TreeTableCell<DataNode, String> cell, DataNode dataNode) {
        if (dataNode instanceof NormalNumberDataNode) {
            // int
            cell.setEditable(true);
            cell.setText(((NormalNumberDataNode) dataNode).getValue());

        } else if (dataNode instanceof NormalCharacterDataNode) {
            // char
            cell.setEditable(true);
            cell.setText(((NormalCharacterDataNode) dataNode).getValue());

        } else if (dataNode instanceof NormalStringDataNode) {
            // char
            cell.setEditable(true);
            cell.setText("<<Size: " + dataNode.getChildren().size() + ">>");


        } else if (dataNode instanceof EnumDataNode) {
            // enum
            cell.setText("Select value");
            if (((EnumDataNode) dataNode).isSetValue()) {
                cell.setText(((EnumDataNode) dataNode).getValue());
            }

        } else if (dataNode instanceof UnionDataNode) {
            // union
            cell.setText("Select attribute");
            if (!dataNode.getChildren().isEmpty()) {
                cell.setText(dataNode.getChildren().get(0).getName());
            }

        } else if (dataNode instanceof SubClassDataNode) {
            // subclass
            cell.setEditable(true);
            SubClassDataNode subClassDataNode = (SubClassDataNode) dataNode;
            cell.setText("Select constructor");
            if (subClassDataNode.getSelectedConstructor() != null) {
                // Hiển thị tên constuctor class
                cell.setText(subClassDataNode.getSelectedConstructor().getName());
            }

        } else if (dataNode instanceof ClassDataNode) {
            // class
            cell.setEditable(true);
            ClassDataNode classDataNode = (ClassDataNode) dataNode;
            cell.setText("Select real class");
            if (classDataNode.getSubClass() != null) {
                // Hiển thị tên class
                cell.setText(classDataNode.getSubClass().getType());
            }

        } else if (dataNode instanceof OneDimensionDataNode) {
            // array
            OneDimensionDataNode arrayNode = (OneDimensionDataNode) dataNode;
            if (arrayNode.isFixedSize()) {
                cell.setText(toSize(((OneDimensionDataNode) dataNode).getSize() + ""));
            } else {
                cell.setEditable(true);
                cell.setText("<<Define size>>");
                if (arrayNode.isSetSize()) {
                    cell.setText(toSize(arrayNode.getSize() + ""));
                }
            }

        } else if (dataNode instanceof PointerDataNode) {
            // con trỏ coi như array
            cell.setEditable(true);
            PointerDataNode arrayNode = (PointerDataNode) dataNode;
            cell.setText("<<Define size>>");
            if (arrayNode.isSetSize()) {
                cell.setText(toSize(arrayNode.getAllocatedSize() + ""));
            }

        } else if (dataNode instanceof MultipleDimensionDataNode) {
            // mảng 2 chiều của int, char
            cell.setEditable(true);
            MultipleDimensionDataNode arrayNode = (MultipleDimensionDataNode) dataNode;
            cell.setText("<<Define size>>");
            if (arrayNode.isSetSize()) {
                StringBuilder sizesInString = new StringBuilder();
                int lastIdx = arrayNode.getSizes().length - 1;
                for (int i = 0; i < lastIdx; i++)
                    sizesInString.append(arrayNode.getSizes()[i]).append(" x ");
                sizesInString.append(arrayNode.getSizes()[lastIdx]);

                cell.setText(toSize(sizesInString + ""));
            }

        } else if (dataNode instanceof ListBaseDataNode) {
            ListBaseDataNode vectorNode = (ListBaseDataNode) dataNode;
            if (dataNode instanceof STLArrayDataNode)
                cell.setEditable(false);
            else
                cell.setEditable(true);
            cell.setText("<<Define size>>");
            if (vectorNode.isSetSize()) {
                cell.setText(toSize(vectorNode.getSize() + ""));
            }

        } else if (dataNode instanceof TemplateSubprogramDataNode) {
            cell.setEditable(true);
            cell.setText("Change template");

        } else if (dataNode instanceof SmartPointerDataNode) {
            cell.setEditable(true);
            cell.setText("Choose constructor");

        } else if (dataNode instanceof VoidPointerDataNode) {
            cell.setEditable(false);
            String shorten = ((VoidPointerDataNode) dataNode).getUserCode();
            shorten = shorten.replace("\n", "↵");
            cell.setText(shorten);

        } else if (dataNode instanceof OtherUnresolvedDataNode) {
            cell.setEditable(false);
            String shorten = ((OtherUnresolvedDataNode) dataNode).getUserCode();
            shorten = shorten.replace("\n", "↵");
            cell.setText(shorten);

        } else if (dataNode instanceof NullPointerDataNode) {
            cell.setText(NullPointerDataNode.NULL_PTR);
            cell.setGraphic(null);

        } else {
            cell.setText(null);
            cell.setGraphic(null);
        }
    }

    @Override
    public void commitEdit(ValueDataNode dataNode, String newValue) throws Exception {

        if (dataNode instanceof NormalNumberDataNode) {
            String type = dataNode.getType();
            if (VariableTypeUtils.isNumFloat(type)) {
                try {
                    double value = Double.parseDouble(newValue);
                    IFunctionConfigBound bound = Environment.getBoundOfDataTypes().getBounds().get(type);
                    if (bound instanceof PrimitiveBound) {
                        if (value >= Double.parseDouble(((PrimitiveBound) bound).getLower())
                                && value <= Double.parseDouble(((PrimitiveBound) bound).getUpper())) {
                            if (type.equals("bool")) {
                                if (newValue.toLowerCase().equals("true") || newValue.toLowerCase().equals("false"))
                                    ((NormalNumberDataNode) dataNode).setValue(newValue);
                                else if (newValue.equals("1"))
                                    ((NormalNumberDataNode) dataNode).setValue("true");
                                else if (newValue.equals("0"))
                                    ((NormalNumberDataNode) dataNode).setValue("false");
                                else
                                    UIController.showErrorDialog("Invalid value of bool", "Test data entering", "Invalid value");
                            } else
                                ((NormalNumberDataNode) dataNode).setValue(value + "");
                        } else {
                            // nothing to do
                            UIController.showErrorDialog("Do not handle when committing " + dataNode.getClass() + " because value " + value + " out of scope " + bound,
                                    "Test data entering", "Invalid value");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Do not handle when committing " + dataNode.getClass());
                }
            } else {
                try {
                    long value = Long.parseLong(newValue);
                    if (VariableTypeUtils.isStdInt(type)) {
                        ((NormalNumberDataNode) dataNode).setValue(value + "");
                    } else {
                        IFunctionConfigBound bound = Environment.getBoundOfDataTypes().getBounds().get(type);
                        if (bound instanceof PrimitiveBound) {
                            if (value >= Long.parseLong(((PrimitiveBound) bound).getLower())
                                    && value <= Long.parseLong(((PrimitiveBound) bound).getUpper())) {
                                if (type.equals("bool")) {
                                    if (newValue.toLowerCase().equals("true") || newValue.toLowerCase().equals("false"))
                                        ((NormalNumberDataNode) dataNode).setValue(newValue);
                                    else if (newValue.equals("1"))
                                        ((NormalNumberDataNode) dataNode).setValue("true");
                                    else if (newValue.equals("0"))
                                        ((NormalNumberDataNode) dataNode).setValue("false");
                                    else
                                        UIController.showErrorDialog("Invalid value of bool", "Test data entering", "Invalid value");
                                } else
                                    ((NormalNumberDataNode) dataNode).setValue(value + "");
                            } else {
                                // nothing to do
                                UIController.showErrorDialog("Do not handle when committing " + dataNode.getClass() + " because value " + value + " out of scope " + bound,
                                        "Test data entering", "Invalid value");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    UIController.showErrorDialog("Wrong input for " + dataNode.getName() +"; unit = " + dataNode.getUnit().getName(), "Test data entering", "Invalid value");
                }
            }
        } else if (dataNode instanceof NormalCharacterDataNode) {
            // CASE: Type character
            if (newValue.startsWith(NormalCharacterDataNode.VISIBLE_CHARACTER_PREFIX)
                    && newValue.endsWith(NormalCharacterDataNode.VISIBLE_CHARACTER_PREFIX)) {
                String character = newValue.substring(1, newValue.length() - 1);

                String ascii = NormalCharacterDataNode.getCharacterToACSIIMapping().get(character);
                if (ascii != null)
                    ((NormalCharacterDataNode) dataNode).setValue(ascii + "");
                else
                    UIController.showErrorDialog("You type wrong character for the type " + dataNode.getType()
                                    + " in src " + dataNode.getUnit().getName() +
                                    NormalCharacterDataNode.RULE
                            , "Wrong input of character", "Fail");
                logger.error("Do not handle when the length of text > 1 for character parameter");

            } else {
                try {
                    // CASE: Type ascii
                    Long v = Long.parseLong(newValue);
                    DataSizeModel dataSizeModel = Environment.getBoundOfDataTypes().getBounds();
                    PrimitiveBound bound = dataSizeModel.get(dataNode.getType());
                    if (bound == null)
                        bound = dataSizeModel.get(dataNode.getType().replace("std::", "").trim());

                    if (bound == null) {
                        UIController.showErrorDialog("You type wrong character for the type " + dataNode.getType()
                                        + " in src " + dataNode.getUnit().getName() +
                                        NormalCharacterDataNode.RULE
                                , "Wrong input of character", "Fail");

                    } else if (v <= bound.getUpperAsLong() && v >= bound.getLowerAsLong()) {
                        ((NormalCharacterDataNode) dataNode).setValue(newValue);

                    } else {
                        UIController.showErrorDialog("Value " + newValue + " is out of bound " + dataNode.getType()
                                        + "[" + bound.getLowerAsLong() + "," + bound.getUpperAsLong() + "]"
                                        + " in src " + dataNode.getUnit().getName() +
                                        NormalCharacterDataNode.RULE
                                , "Wrong input of character", "Fail");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    UIController.showErrorDialog("You type wrong character for the type " + dataNode.getType()
                            + " in src " + dataNode.getUnit().getName() +
                            NormalCharacterDataNode.RULE, "Wrong input of character", "Fail");
                    logger.error("Do not handle when the length of text > 1 for character parameter");
                }
            }
        } else if (dataNode instanceof NormalStringDataNode) {
            try {
                long lengthOfString = Long.parseLong(newValue);

                if (lengthOfString < 0)
                    throw new Exception();

                ((NormalStringDataNode) dataNode).setAllocatedSize(lengthOfString);
                TreeExpander expander = new TreeExpander();
                expander.setRealTypeMapping(this.realTypeMapping);
                expander.expandTree(dataNode);
            } catch (Exception e) {
                e.printStackTrace();
                UIController.showErrorDialog("Length of a string must be >=0 and is an integer", "Wrong length of string", "Invalid length");
            }

        } else if (dataNode instanceof EnumDataNode) {
            // enum oke
            ((EnumDataNode) dataNode).setValue(newValue);
            ((EnumDataNode) dataNode).setValueIsSet(true);

        } else if (dataNode instanceof UnionDataNode) {
            // union oke
            // expand tree với thuộc tính được chọn ở combobox
            (new TreeExpander()).expandStructureNodeOnDataTree(dataNode, newValue);

        } else if (dataNode instanceof StructDataNode) {
            (new TreeExpander()).expandStructureNodeOnDataTree(dataNode, newValue);

        } else if (dataNode instanceof SubClassDataNode) {
            // subclass (cũng là class) oke
            ((SubClassDataNode) dataNode).chooseConstructor(newValue);
            (new TreeExpander()).expandTree(dataNode);

        } else if (dataNode instanceof ClassDataNode) {
            // class oke
            ((ClassDataNode) dataNode).setSubClass(newValue);

        } else if (dataNode instanceof OneDimensionDataNode) {
            //array cua normal data. oke
            int size = Integer.parseInt(newValue);
            OneDimensionDataNode currentNode = (OneDimensionDataNode) dataNode;
            currentNode.setSize(size);
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.setRealTypeMapping(this.realTypeMapping);
            expander.expandTree(dataNode);

        } else if (dataNode instanceof PointerDataNode) {
            // con trỏ coi như array
            int size = Integer.parseInt(newValue);
            PointerDataNode currentNode = (PointerDataNode) dataNode;
            currentNode.setAllocatedSize(size);
            // tmp
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.setRealTypeMapping(this.realTypeMapping);
            expander.expandTree(dataNode);

        } else if (dataNode instanceof MultipleDimensionDataNode) {
            int sizeA = Integer.parseInt(newValue);
            MultipleDimensionDataNode currentNode = (MultipleDimensionDataNode) dataNode;
            currentNode.setSizeOfDimension(0, sizeA);
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.expandTree(dataNode);

        } else if (dataNode instanceof TemplateSubprogramDataNode) {
            dataNode.getChildren().clear();
            ((TemplateSubprogramDataNode) dataNode).setRealFunctionNode(newValue);
//            ((TemplateDataNode) dataNode).generateArgumentsAndReturnVariable();

        } else if (dataNode instanceof SmartPointerDataNode) {
            dataNode.getChildren().clear();
            ((SmartPointerDataNode) dataNode).chooseConstructor(newValue);
            (new TreeExpander()).expandTree(dataNode);

        } else if (dataNode instanceof ListBaseDataNode && !(dataNode instanceof STLArrayDataNode)) {
            //array cua normal data. oke
            int size = Integer.parseInt(newValue);
            ListBaseDataNode currentNode = (ListBaseDataNode) dataNode;
            currentNode.setSize(size);
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.expandTree(dataNode);

        } else if (dataNode instanceof OtherUnresolvedDataNode) {
            ((OtherUnresolvedDataNode) dataNode).setUserCode(newValue);

        } else  if (dataNode instanceof VoidPointerDataNode) {
            ((VoidPointerDataNode) dataNode).setUserCode(newValue);
        } else
            logger.error("Do not support to enter data for " + dataNode.getClass());
    }

    private String toSize(String size) {
        if (size.equals("0") || size.equals("-1"))
            return "<<Size: NULL>>";
        else
            return "<<Size: " + size + ">>";
    }
}

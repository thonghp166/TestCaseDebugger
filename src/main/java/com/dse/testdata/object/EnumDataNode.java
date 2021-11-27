package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.EnumNode;
import com.dse.parser.object.INode;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

import java.util.List;

/**
 * Created by DucToan on 27/07/2017
 */
public class EnumDataNode extends StructureDataNode {
    /**
     * Represent value of variable
     */
    private String value;
    private boolean valueIsSet = false;

    @Override
    public String getInputForDisplay() {
        return this.getType() + " " + this.getName() + " = " + this.getValue() + SpecialCharacter.END_OF_STATEMENT;
    }

    @Override
    public String getInputForGoogleTest() throws Exception {
        String input = super.getInputForGoogleTest();

        // get type of variable
        String typeVar = this.getType().replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);

        if (Environment.getInstance().isC()) {
            int index = typeVar.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
            if (index >= 0)
                typeVar = typeVar.substring(index + 2);
        }

        if (this.getValue() != null) {
            if (isExternel())
                typeVar = "";

            String valueVar = getValue();
            if (!Environment.getInstance().isC()) {
                valueVar = typeVar + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + valueVar;
            }

            // generate the statement
            if (this.isPassingVariable()) {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isAttribute()) {
                input += this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isArrayElement()) {
                input += this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (isSTLListBaseElement()) {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isInConstructor()){
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;
            }

        } else if (isPassingVariable()) {
            input += typeVar + " " + getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        }

        return input + SpecialCharacter.LINE_BREAK;
    }

    @Override
    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
        String assertion = "";//super.getAssertionForGoogleTest();

        if (getValue() != null || getVituralName().equals(IGTestConstant.EXPECTED_OUTPUT)) {
            String actualOutputName = getVituralName().replace(source, target);

            assertion += method + "(" + getVituralName() + "," + actualOutputName + ")" + IGTestConstant.LOG_FUNCTION_CALLS;
        }


        return assertion;
    }

    /**
     * Get all name defined in the declaration of enum. For example, enum "Color {
     * RED=10, GREEN=40, BLUE}" -----> "RED", "GREEN", "BLUE"
     *
     * @return all enum items name
     */
    public List<String> getAllNameEnumItems() {
        INode coreType = getCorrespondingVar().resolveCoreType();
        if (coreType instanceof EnumNode) {
            return ((EnumNode) coreType).getAllNameEnumItems();
        }
        return null;
    }

    @Override
    public String generateInputToSavedInFile() {
        return getName() + "=" + getValue();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (getAllNameEnumItems().contains(value))
            this.value = value;
    }

    public void setValueIsSet(boolean valueIsSet) {
        this.valueIsSet = valueIsSet;
    }

    public boolean isSetValue() {
        return valueIsSet;
    }
}

package com.dse.testdata.object;

import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import com.dse.util.VariableTypeUtils;

/**
 * Represent basic types belong to number of character
 *
 * @author DucAnh
 */
public abstract class NormalDataNode extends ValueDataNode {
    public static final String CHARACTER_QUOTE = "'";

    /**
     * Represent value of variable
     */
    private String value;

    private String generateAssignmentForDisplay() {
        String varInit = "";
        String valueVar = "";
        if (VariableTypeUtils.isCh(this.getType())) {
            int numberValue = Utils.toInt(this.getValue());

            if (VariableTypeUtils.isChBasic(this.getType()) && Utils.isVisibleCh(numberValue)
                    && !Utils.isSpecialChInVisibleRange(numberValue))
                valueVar = NormalDataNode.CHARACTER_QUOTE + (char) numberValue + NormalDataNode.CHARACTER_QUOTE;
            else
                valueVar = numberValue + "";
        } else
            valueVar = this.getValue() + "";

        varInit = this.getDotSetterInStr(valueVar) + SpecialCharacter.LINE_BREAK;
        return varInit;
    }

    @Override
    public String getInputForDisplay() throws Exception {
        String input = "";
        input = this.generateAssignmentForDisplay();
        return input;
    }

    @Override
    public String generareSourcecodetoReadInputFromFile() throws Exception {
        String typeVar = VariableTypeUtils.deleteStorageClasses(this.getType())
                .replace(IDataNode.REFERENCE_OPERATOR, "");

        String loadValueStm = "data.findValueByName<" + typeVar + ">(\"" + getVituralName() + "\")";

        String fullStm = typeVar + " " + this.getVituralName() + "=" + loadValueStm + SpecialCharacter.END_OF_STATEMENT;
        return fullStm;
    }

    @Override
    public String generateInputToSavedInFile() throws Exception {
        if (this.getValue() != null)
            return this.getName() + "=" + this.getValue() + SpecialCharacter.LINE_BREAK;
        else
            return "";
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value + "";
    }
}

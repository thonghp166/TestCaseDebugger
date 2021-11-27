package com.dse.testdata.object;

import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;
import com.dse.util.AkaLogger;

public class NormalNumberDataNode extends NormalDataNode {
    private final static AkaLogger logger = AkaLogger.get(NormalNumberDataNode.class);

    @Override
    public String getInputForGoogleTest() throws Exception {
        String input =  super.getInputForGoogleTest() + SpecialCharacter.LINE_BREAK;

        // get type of variable
        String typeVar = VariableTypeUtils.deleteStorageClasses(this.getType())
                .replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);
        typeVar = VariableTypeUtils.deleteStructKeyword(typeVar);
        typeVar = VariableTypeUtils.deleteUnionKeyword(typeVar);

        if (this.getValue() != null) {
            if (isExternel())
                typeVar = "";

            // generate the statement
            if (isPassingVariable()) {
                input += typeVar + SpecialCharacter.SPACE + getVituralName() + SpecialCharacter.EQUAL + getValue() + SpecialCharacter.END_OF_STATEMENT;

            } else if (isAttribute()) {
                input += getVituralName() + SpecialCharacter.EQUAL + getValue() + SpecialCharacter.END_OF_STATEMENT;

            } else if (isArrayElement()) {
                input += getVituralName() + SpecialCharacter.EQUAL + getValue() + SpecialCharacter.END_OF_STATEMENT;

            } else if (isSTLListBaseElement()) {
                input += typeVar + SpecialCharacter.SPACE + getVituralName() + SpecialCharacter.EQUAL + getValue() + SpecialCharacter.END_OF_STATEMENT;
                
            } else if (isInConstructor()) {
                input += typeVar + SpecialCharacter.SPACE + getVituralName() + SpecialCharacter.EQUAL + getValue() + SpecialCharacter.END_OF_STATEMENT;

            } else {
                input += typeVar + " " + this.getVituralName() + "=" + getValue() + SpecialCharacter.END_OF_STATEMENT;
            }
        } else if (isPassingVariable()) {
            input += typeVar + " " + getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        } else if (this.getValue() == null) {
            input += "/* "+getName()+" : null value -> code code */";
        }

        return input + SpecialCharacter.LINE_BREAK;
    }

    @Override
    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
        String assertion = "";//super.getAssertionForGoogleTest();

        if (getValue() != null || getVituralName().equals(IGTestConstant.EXPECTED_OUTPUT)) {
            String actualOutputName = getVituralName().replace(source, target);

            String newMethod = method.substring(0, method.indexOf("_") + 1);
            if ((VariableTypeUtils.isNumBasicFloat(getType()))) {
                if (getType().contains(VariableTypeUtils.BASIC.NUMBER.FLOAT.FLOAT))
                    newMethod += "FLOAT_";
                else
                    newMethod += "DOUBLE_";
            }

            newMethod += "EQ";

            assertion += newMethod + "(" + getVituralName() + "," + actualOutputName + ")"
                    + IGTestConstant.LOG_FUNCTION_CALLS;
        }

        return assertion;
    }
}

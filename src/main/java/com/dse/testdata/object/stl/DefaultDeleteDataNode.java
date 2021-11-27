package com.dse.testdata.object.stl;

import com.dse.testdata.object.IDataNode;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

public class DefaultDeleteDataNode extends STLDataNode {
    @Override
    public String getInputForGoogleTest() throws Exception {
        String input = "";

        // get type of variable
        String typeVar = getType().replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);
        typeVar = VariableTypeUtils.deleteStructKeyword(typeVar);
        typeVar = VariableTypeUtils.deleteUnionKeyword(typeVar);

        if (isExternel())
            typeVar = "";

        // generate the statement
        if (this.isPassingVariable()) {
            input += typeVar + " " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        } else if (isSTLListBaseElement()) {
            input += typeVar + " " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        } else if (this.isInConstructor()) {
            input += typeVar + " " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
            input += typeVar + " " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        }

        return  input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
    }
}

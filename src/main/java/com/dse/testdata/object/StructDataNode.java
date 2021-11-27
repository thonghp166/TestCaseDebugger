package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

/**
 * Represent struct variable
 *
 * @author DucAnh
 */
public class StructDataNode extends StructureDataNode {

    @Override
    public String getInputForGoogleTest() throws Exception {
        if (Environment.getInstance().isC()) {
            return getCInputGTest();
        } else
            return getCppInputGTest();
    }

    private String getCInputGTest() throws Exception {
        String input = "";

        String typeVar = this.getType().replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);

        if (isExternel())
            typeVar = "";

        int index = typeVar.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
        if (index >= 0)
            typeVar = typeVar.substring(index + 2);

        if (this.isPassingVariable()){
            input += typeVar +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        } else if (getParent() instanceof OneDimensionDataNode || getParent() instanceof PointerDataNode){
            input += "";

        } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
            input += typeVar +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        }

        return  input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
    }

    private String getCppInputGTest() throws Exception {
        String input = "";

        if (this.isPassingVariable()){
            input += this.getType() +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        } else if (getParent() instanceof PointerDataNode) {
            input += getVituralName() + " = " + getType() + "()" + SpecialCharacter.END_OF_STATEMENT;

        } else if (getParent() instanceof OneDimensionDataNode){
            input += "";

        } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
            input += this.getType() +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        }

        return  input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
    }

    @Override
    public StructDataNode clone() {
        StructDataNode clone = (StructDataNode) super.clone();

        for (IDataNode child : getChildren()) {
            if (child instanceof ValueDataNode) {
                ValueDataNode cloneChild = ((ValueDataNode) child).clone();
                clone.getChildren().add(cloneChild);
                cloneChild.setParent(clone);
            }
        }

        return clone;
    }
}

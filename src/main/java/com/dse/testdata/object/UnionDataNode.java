package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

/**
 * Represent union variable
 *
 * @author ducanhnguyen
 */
public class UnionDataNode extends StructureDataNode {

    @Override
    public String getInputForGoogleTest() throws Exception {
        String input = "";

        String typeVar = this.getType().replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);

        if (isExternel())
            typeVar = "";

        if (Environment.getInstance().isC()) {
            int index = typeVar.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
            if (index >= 0)
                typeVar = typeVar.substring(index + 2);
        }

        if (this.isPassingVariable()){
            input += typeVar +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        } else if (getParent() instanceof PointerDataNode) {
            input += getVituralName() + " = " + getType() + "()" + SpecialCharacter.END_OF_STATEMENT;

        } else if (getParent() instanceof OneDimensionDataNode){
            input += "";
        } else if (isSutExpectedArgument() || isGlobalExpectedValue())
            input += typeVar +" " + this.getVituralName() + SpecialCharacter.END_OF_STATEMENT;

        return  input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
    }

    @Override
    public UnionDataNode clone() {
        UnionDataNode clone = (UnionDataNode) super.clone();

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

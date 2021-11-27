package com.dse.testdata.object;

import com.dse.util.SpecialCharacter;
import com.dse.util.TemplateUtils;
import com.dse.util.VariableTypeUtils;

public class OneDimensionStructureDataNode extends OneDimensionDataNode {

    @Override
    public String getInputForDisplay() throws Exception {
        String input = "";

        for (IDataNode child : this.getChildren())
            input += child.getInputForDisplay();
        if (this.isAttribute())
            input += this.getDotSetterInStr(this.getVituralName()) + SpecialCharacter.LINE_BREAK;
        return input;
    }

    @Override
    public String getInputForGoogleTest() throws Exception {
        String declarationType = VariableTypeUtils
                .deleteStorageClasses(this.getType().replace(IDataNode.REFERENCE_OPERATOR, ""));

        String coreType = declarationType.replaceAll("\\[.*\\]", "");

        if (TemplateUtils.isTemplate(declarationType))
            if (!getChildren().isEmpty())
                coreType = ((ValueDataNode) getChildren().get(0)).getType();

        if (isExternel())
            coreType = "";

        String input = "";

        if (this.isPassingVariable()){
            input += coreType + " " + getVituralName() + "[" + getSize() + "]" + SpecialCharacter.END_OF_STATEMENT;

        } else if (this.isAttribute()) {
            input += "";
        } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
            input += coreType + " " + getVituralName() + "[" + getSize() + "]" + SpecialCharacter.END_OF_STATEMENT;
        }

        return input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
    }

}

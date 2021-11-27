package com.dse.testdata.object.stl;

import com.dse.testdata.object.IDataNode;
import com.dse.testdata.object.ValueDataNode;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;


public class VectorDataNode extends ListBaseDataNode {
    @Override
    public String getElementName(int index) {
        return "element #" + index;
    }

    @Override
    public String getPushMethod() {
        return "push_back";
    }

    @Override
    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
        String output = "";

        if (isSetSize()) {
            String actualOutputName = getVituralName().replace(source, target);

            output += String.format("%s(%s.size(), %s.size())%s\n", method, getVituralName(),
                    actualOutputName, IGTestConstant.LOG_FUNCTION_CALLS);

            for (IDataNode child : getChildren()) {
                if (child instanceof ValueDataNode) {
                    ValueDataNode dataNode = (ValueDataNode) child;

                    String actualOutputChildName = dataNode.getVituralName().replace(source, target);

                    String coreType = VariableTypeUtils
                            .deleteStorageClasses(dataNode.getType().replace(IDataNode.REFERENCE_OPERATOR, ""));

                    output += String.format("%s %s = %s[%d];\n",
                            coreType, actualOutputChildName, actualOutputName, getChildren().indexOf(child));

                    output += dataNode.getAssertionForGoogleTest(method, source, target) + SpecialCharacter.LINE_BREAK;
                }
            }
        }

        return output;
    }
}

package com.dse.testdata.object.stl;

import com.dse.testdata.object.IDataNode;
import com.dse.testdata.object.ValueDataNode;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

public class StackDataNode extends ListBaseDataNode {
    @Override
    public String getElementName(int index) {
        if (index == 0)
            return "top";
        else
            return "element #" + index;
    }

    @Override
    public String getPushMethod() {
        return "push";
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

                    output += String.format("%s %s = %s.top();\n",
                            coreType, actualOutputChildName, actualOutputName);

                    output += String.format("%s.pop();\n", actualOutputName);

                    output += dataNode.getAssertionForGoogleTest(method, source, target) + SpecialCharacter.LINE_BREAK;
                }
            }

            // add again
            for (int i = getChildren().size() - 1; i >= 0; i--) {
                IDataNode child = getChildren().get(i);

                String actualOutputChildName = child.getVituralName().replace(source, target);

                output += String.format("%s.%s(%s);\n", actualOutputName, getPushMethod(), actualOutputChildName);
            }
        }

        return output;
    }
}

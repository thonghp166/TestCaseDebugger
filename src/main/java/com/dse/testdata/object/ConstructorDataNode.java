package com.dse.testdata.object;

import com.dse.parser.object.INode;
import com.dse.parser.object.InstanceVariableNode;
import com.dse.project_init.ProjectClone;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

public class ConstructorDataNode extends SubprogramNode {
    public ConstructorDataNode(INode fn) {
        super(fn);
    }

    @Override
    public String getType() {
        if (super.getType() == null) {
            if (getParent() instanceof ValueDataNode) {
                String type = ((ValueDataNode) getParent()).getCorrespondingVar().getFullType();
                type = VariableTypeUtils.deleteStorageClasses(type);
                type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);
                setType(type);
            }
        }

        return super.getType();
    }

    @Override
    public void setFunctionNode(INode functionNode) {
        this.functionNode = functionNode;
    }

    public ConstructorDataNode() {
        super();
    }

    @Override
    public String getDisplayNameInParameterTree() {
        return getName();
    }

    @Override
    public String getInputForGoogleTest() throws Exception {
        String input = super.getInputForGoogleTest();

        ValueDataNode subclassVar = (ValueDataNode) getParent();
        ValueDataNode classVar = (ValueDataNode) subclassVar.getParent();

        if (getTestCaseRoot().getFunctionNode().equals(getFunctionNode())) {
            if (classVar.getCorrespondingVar() instanceof InstanceVariableNode) {
                input += "AKA_MARK(\"<<PRE-CALLING>>\");";
            }
        }

        input += ProjectClone.generateCallingMark(String.format("%s|%s", functionNode.getAbsolutePath(), getPathFromRoot()));

        if (!(classVar instanceof ClassDataNode))
            classVar = subclassVar;

        String realType = VariableTypeUtils.getFullRawType(subclassVar.getCorrespondingVar());
        String originType = classVar.getType();

        if (classVar.isExternel())
            originType = "";

        String argumentInput = getConstructorArgumentsInputForGoogleTest();

        if (classVar.isInstance()) {
            input += getVituralName() + " = new " + realType
                    + argumentInput + SpecialCharacter.END_OF_STATEMENT;

        } else if (subclassVar.isArrayElement() || subclassVar.isAttribute()) {
            // can not use new
            input += getVituralName() + " = " + realType
                    + argumentInput + SpecialCharacter.END_OF_STATEMENT;

        } else if (!(classVar instanceof PointerDataNode)) {
            input += originType + " " + getVituralName() + " = " + realType
                    + argumentInput + SpecialCharacter.END_OF_STATEMENT;

        } else {
            // which case?
            input += realType + " " + getVituralName() + " = new " + getType()
                    + argumentInput + SpecialCharacter.END_OF_STATEMENT;
        }

        return input;
    }

    private String getConstructorArgumentsInputForGoogleTest() {
        StringBuilder input = new StringBuilder();
        input.append("(");

        if (getChildren().size() > 0) {
            for (IDataNode parameter : getChildren()) {
                input.append(parameter.getVituralName()).append(",");
            }
        }

        input.append(")");

        input = new StringBuilder(input.toString().replace(",)", ")"));

        return input.toString();
    }
}

package com.dse.compiler.message.error_tree.node;

import com.dse.parser.object.INode;

public abstract class ScopeErrorNode extends ErrorNode {
    protected INode scope;

    public INode getScope() {
        return scope;
    }

    public void setScope(INode scope) {
        this.scope = scope;
    }

//    @Override
//    public String getMessage() {
//        if (message == null) {
//            message = SpecialCharacter.EMPTY;
//
//            for (IErrorNode errorNode : getChildren())
//                message += errorNode.getMessage() + SpecialCharacter.LINE_BREAK;
//        }
//
//        return message;
//    }
}

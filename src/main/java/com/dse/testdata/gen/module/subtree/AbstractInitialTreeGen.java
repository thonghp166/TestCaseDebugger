package com.dse.testdata.gen.module.subtree;

import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.VariableNode;
import com.dse.testdata.gen.module.InitialTreeGen;
import com.dse.testdata.object.DataNode;
import com.dse.testdata.object.IDataNode;
import com.dse.testdata.object.ValueDataNode;
import com.dse.util.AkaLogger;

public abstract class AbstractInitialTreeGen implements IInitialSubTreeGen {
    protected final static AkaLogger logger = AkaLogger.get(AbstractInitialTreeGen.class);

    protected ICommonFunctionNode functionNode;
    protected IDataNode root;

    @Override
    public ValueDataNode genInitialTree(VariableNode vCurrentChild, DataNode nCurrentParent) throws Exception {
        return new InitialTreeGen().genInitialTree(vCurrentChild, nCurrentParent);
    }

    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    public IDataNode getRoot() {
        return root;
    }

    public void setRoot(IDataNode root) {
        this.root = root;
    }
}

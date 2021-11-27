package com.dse.testdata.gen.module.subtree;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.funcdetail.IFunctionDetailTree;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testdata.InputCellHandler;
import com.dse.testdata.object.*;
import com.dse.util.NodeType;
import com.dse.util.Utils;

import java.util.List;

import static com.dse.util.NodeType.GLOBAL;
import static com.dse.util.NodeType.SBF;

public class InitialUUTBranchGen extends AbstractInitialTreeGen {
    @Override
    public void generateCompleteTree(RootDataNode root, IFunctionDetailTree functionTree) throws Exception {
        this.root = root;
        functionNode = root.getFunctionNode();
        INode sourceCode = Utils.getSourcecodeFile(functionNode);

        if (sourceCode instanceof SourcecodeFileNode) {
            UnitNode unitNode = new UnitUnderTestNode(sourceCode);
//            unitNode.setStubChildren(false);

            root.addChild(unitNode);
            unitNode.setParent(root);

            if (Environment.getInstance().getSBFs().contains(sourceCode))
                generateSBFBranch(unitNode, sourceCode, functionNode);

            RootDataNode globalRoot = generateGlobalVarBranch(unitNode, functionTree);

            IDataNode sut = new InitialArgTreeGen().generate(unitNode, functionNode);

            if (functionNode instanceof ConstructorNode) {
                sut.getChildren().clear();
                expandInstance((ConstructorNode) functionNode, globalRoot);
            }
        }
    }

    // case test constructor
    private void expandInstance(IFunctionNode sut, RootDataNode globalRoot) throws Exception {
        INode parent = sut.getRealParent() == null ? sut.getParent() : sut.getRealParent();

        for (IDataNode child : globalRoot.getChildren()) {
            ValueDataNode globalVar = (ValueDataNode) child;

            if (globalVar.getCorrespondingVar() instanceof InstanceVariableNode
                    && globalVar.getCorrespondingType().equals(parent)) {

                new InputCellHandler().commitEdit(globalVar, globalVar.getCorrespondingType().getName());

                if (!globalVar.getChildren().isEmpty()) {
                    ValueDataNode subclass = (ValueDataNode) globalVar.getChildren().get(0);
                    new InputCellHandler().commitEdit(subclass, sut.getName());
                }
            }
        }
    }

    private RootDataNode generateSBFBranch(IDataNode current, INode sourceNode, ICommonFunctionNode sut) {
        RootDataNode sbfRoot = new RootDataNode(SBF);

        List<INode> functions = Search.searchNodes(sourceNode, new FunctionNodeCondition());

        for (INode child : functions) {
            if (child instanceof FunctionNode && !child.equals(sut)) {
                FunctionNode functionNode = (FunctionNode) child;
                SubprogramNode subprogramNode = new SubprogramNode(functionNode);

                if (functionNode.isTemplate())
                    subprogramNode = new TemplateSubprogramDataNode(functionNode);

                sbfRoot.addChild(subprogramNode);
                subprogramNode.setParent(sbfRoot);
            }
        }

        current.addChild(sbfRoot);
        sbfRoot.setParent(current);

        return sbfRoot;
    }

    private RootDataNode generateGlobalVarBranch(IDataNode current, IFunctionDetailTree functionTree) throws Exception {
        logger.debug("generateGlobalVarBranch");
        RootDataNode globalVarRoot = new RootDataNode(GLOBAL);

        List<INode> globalVariables = functionTree.getSubTreeRoot(NodeType.GLOBAL).getElements();

        for (INode globalVariable : globalVariables) {
            if (globalVariable instanceof VariableNode) {
                ValueDataNode dataNode = genInitialTree((VariableNode) globalVariable, globalVarRoot);
//                if (globalVariable instanceof ExternalVariableNode)
                    dataNode.setExternel(true);
            }
        }

        current.addChild(globalVarRoot);
        globalVarRoot.setParent(current);

        return globalVarRoot;
    }
}

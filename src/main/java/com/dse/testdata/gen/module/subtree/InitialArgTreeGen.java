package com.dse.testdata.gen.module.subtree;

import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.funcdetail.IFunctionDetailTree;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.Search2;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testdata.gen.module.SimpleTreeDisplayer;
import com.dse.testdata.object.*;
import com.dse.util.Utils;
import com.dse.util.VariableTypeUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;

import java.io.File;
import java.util.List;

/**
 * Given a function, this class will generate an initial tree of parameters
 */
public class InitialArgTreeGen extends AbstractInitialTreeGen {

    public static void main(String[] args) throws Exception {
        // Parse project
        ProjectParser parser = new ProjectParser(new File(Utils.normalizePath(Paths.DATA_GEN_TEST)));
        parser.setCpptoHeaderDependencyGeneration_enabled(true);
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setFuncCallDependencyGeneration_enabled(true);
        parser.setGlobalVarDependencyGeneration_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setExtendedDependencyGeneration_enabled(true);

        // Get a function
        String name = "test(int,int*,int[],int[2],char,char*,char[],char[10],SinhVien*,SinhVien,SinhVien[])";
        FunctionNode function = (FunctionNode) Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), name).get(0);
        logger.debug("function " + function.getAST().getRawSignature());

        // create initial tree
        RootDataNode root = new RootDataNode();
        root.setFunctionNode(function);
        InitialArgTreeGen dataTreeGen = new InitialArgTreeGen();
        dataTreeGen.generateCompleteTree(root, null);
        logger.debug("Initial tree:\n" + new SimpleTreeDisplayer().toString(root));
    }

    @Override
    public void generateCompleteTree(RootDataNode root, IFunctionDetailTree functionTree) throws Exception {
        this.root = root;
        this.functionNode = root.getFunctionNode();

        SubprogramNode subprogramNode = new SubprogramNode(functionNode);
        subprogramNode.setName(functionNode.getSimpleName());

        if (functionNode instanceof MacroFunctionNode) {
            subprogramNode = new MacroSubprogramDataNode((MacroFunctionNode) functionNode);

        } else if (functionNode.isTemplate()) {
            if (functionNode instanceof IFunctionNode)
                subprogramNode = new TemplateSubprogramDataNode((IFunctionNode) functionNode);

        } else {
            // add nodes corresponding to the parameters of the function
            List<IVariableNode> passingVariables = functionNode.getArguments();
            for (INode passingVariable : passingVariables)
                genInitialTree((VariableNode) passingVariable, subprogramNode);

            // add a node which expressing the return value of the function
            VariableNode returnVar = Search2.getReturnVarNode(functionNode);
            if (returnVar != null) {
                genInitialTree(returnVar, subprogramNode);
                subprogramNode.setType(VariableTypeUtils.getFullRawType(returnVar));
            }
        }

        root.addChild(subprogramNode);
        subprogramNode.setParent(root);

//        setVituralName(subprogramNode);
    }

    private boolean isTemplate(FunctionNode functionNode) {
        IASTNode astNode = functionNode.getAST().getParent();

        while (astNode != null) {
            if (astNode instanceof ICPPASTTemplateDeclaration)
                return true;
            else
                astNode = astNode.getParent();
        }
        return false;
    }

    public IDataNode generate(IDataNode parent, ICommonFunctionNode functionNode) throws Exception {
        SubprogramNode subprogramNode = new SubprogramNode(functionNode);
//        subprogramNode.setName(functionNode.getSimpleName());

        if (functionNode instanceof MacroFunctionNode) {
            subprogramNode = new MacroSubprogramDataNode((MacroFunctionNode) functionNode);

        } else if (functionNode.isTemplate()) {
            if (functionNode instanceof IFunctionNode)
                subprogramNode = new TemplateSubprogramDataNode((IFunctionNode) functionNode);

        } else {
            // add nodes corresponding to the parameters of the function
            List<IVariableNode> passingVariables = functionNode.getArguments();
            for (INode passingVariable : passingVariables)
                genInitialTree((VariableNode) passingVariable, subprogramNode);

            // add a node which expressing the return value of the function
            VariableNode returnVar = Search2.getReturnVarNode(functionNode);
            if (returnVar != null)
                genInitialTree(returnVar, subprogramNode);

//            subprogramNode.setType(VariableTypeUtils.getFullRawType(returnVar));
        }

        parent.addChild(subprogramNode);
        subprogramNode.setParent(parent);

        return subprogramNode;
    }
}

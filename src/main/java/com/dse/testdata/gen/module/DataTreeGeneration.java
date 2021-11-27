package com.dse.testdata.gen.module;

import com.dse.config.Paths;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.ProjectParser;
import com.dse.parser.funcdetail.IFunctionDetailTree;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testdata.IDataTree;
import com.dse.testdata.gen.module.subtree.*;
import com.dse.testdata.object.RootDataNode;
import com.dse.util.Utils;
import com.dse.util.AkaLogger;

import java.io.File;

import static com.dse.util.NodeType.*;

/**
 * dựng cây Function Detail Tree của một test case (có UUT, STUB, GLOBAL)
 *
 * @author DucAnh
 */
public class DataTreeGeneration extends AbstractDataTreeGeneration {
    final static AkaLogger logger = AkaLogger.get(DataTreeGeneration.class);

    private IFunctionDetailTree functionTree;
    private IDataTree dataTree;

    public static void main(String[] args) throws Exception {
        // Parse project
        ProjectParser parser = new ProjectParser(new File(Utils.normalizePath(Paths.JOURNAL_TEST)));
        parser.setCpptoHeaderDependencyGeneration_enabled(true);
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setFuncCallDependencyGeneration_enabled(true);
        parser.setGlobalVarDependencyGeneration_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setExtendedDependencyGeneration_enabled(true);

        // Get a function
        INode root = parser.getRootTree();
        Environment.getInstance().setProjectNode((ProjectNode) root);

        String name = "compare(Polygon,Polygon)";
//        TestCase tc = TestCaseManager.getTestCaseByName("compare.2289", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases");
        TestCase tc = TestCaseManager.getBasicTestCaseByName("compare.98803", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases", true);

//        String name = "Tritype(int,int,int)";
//        TestCase tc = TestCaseManager.getTestCaseByName("Tritype.74783", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases");

//        String name = "uninit_var(int[3],int[3])";
//        TestCase tc = TestCaseManager.getTestCaseByName("uninit_var.48732", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases");

//        String name = "getTail(struct Node*)";
//        TestCase tc = TestCaseManager.getTestCaseByName("getTail.62523", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases");

//        String name = "reverse_array(int*,int)";

//        String name = "bubbleSort1(int[],int)";
//        TestCase tc = TestCaseManager.getTestCaseByName("bubbleSort1.79284", "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/test/testcases");

        FunctionNode function = (FunctionNode) Search
                .searchNodes(root, new FunctionNodeCondition(), name).get(0);
        logger.debug("function " + function.getAST().getRawSignature());

        DataTreeGeneration dataGen = new DataTreeGeneration();
        dataGen.setFunctionNode(function);
        dataGen.setRoot(tc.getRootDataNode());
        dataGen.setVituralName(dataGen.getRoot());

        System.out.println(new SimpleTreeDisplayer().toString(dataGen.getRoot()));
        System.out.println("Function call = " + dataGen.getFunctionCall(function));
        System.out.println("Initialization = " + dataGen.getRoot().getInputForGoogleTest());
    }

    public DataTreeGeneration() {
    }

    public DataTreeGeneration(IDataTree dataTree, IFunctionDetailTree functionTree)  {
        this.dataTree = dataTree;
        setRoot(dataTree.getRoot());
        setFunctionNode(functionTree.getUUT());
        this.functionTree = functionTree;
    }

    @Override
    public void generateTree() throws Exception {
        root.setFunctionNode(functionNode);
        INode sourceCode = Utils.getSourcecodeFile(functionNode);

        // generate uut branch
        new InitialUUTBranchGen().generateCompleteTree(root, functionTree);

        // generate other sbf
        for (INode sbf : Environment.getInstance().getSBFs()) {
            if (!sourceCode.equals(sbf))
                new InitialStubUnitBranchGen().generate(root, sbf);
        }

        // generate stub branch
        RootDataNode stubRoot = new RootDataNode(STUB);
        root.addChild(stubRoot);
        stubRoot.setParent(root);
        new InitialStubTreeGen().generateCompleteTree(stubRoot, functionTree);

//        //
//        RootDataNode globalVarRoot = new RootDataNode(GLOBAL);
//        globalVarRoot.setFunctionNode(functionNode);
//        root.addChild(globalVarRoot);
//        globalVarRoot.setParent(root);
//        new InitialGlobalVarTreeGen().generateCompleteTree(globalVarRoot, functionTree);
//        setVituralName(globalVarRoot);
//
//        //
//        RootDataNode uutRoot = new RootDataNode(UUT);
//        uutRoot.setFunctionNode(functionNode);
//        root.addChild(uutRoot);
//        uutRoot.setParent(root);
//        new InitialArgTreeGen().generateCompleteTree(uutRoot, functionTree);
//        setVituralName(uutRoot);
//
//        //
//        RootDataNode stubRoot = new RootDataNode(STUB);
//        stubRoot.setFunctionNode(functionNode);
//        root.addChild(stubRoot);
//        stubRoot.setParent(root);
//        new InitialStubTreeGen().generateCompleteTree(stubRoot, functionTree);
//        setVituralName(stubRoot);
    }

    @Override
    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;

        if (dataTree != null)
            dataTree.setFunctionNode(functionNode);
    }
}

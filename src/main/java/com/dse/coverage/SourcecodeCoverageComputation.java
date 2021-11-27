package com.dse.coverage;

import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.instrument.IFunctionInstrumentationGeneration;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.coverage.CFGUpdaterv2;
import auto_testcase_generation.testdatagen.coverage.ICoverageComputation;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.highlight.AbstractHighlighterForSourcecodeLevel;
import com.dse.highlight.SourcecodeHighlighterForCoverage;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.MacroFunctionNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to compute coverage of source code file.
 *
 * The type of coverage is only STATEMENT, BRANCH, and MCDC.
 *
 * For STATEMENT+BRANCH, STATEMENT+MCDC, these kinds of coverage include two coverage types.
 */
public class SourcecodeCoverageComputation extends AbstractCoverageComputation{
    final static Logger logger = Logger.getLogger(SourcecodeCoverageComputation.class);

    public static void main(String[] args) {
        // parse project
        ProjectParser projectParser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/macro"));

        projectParser.setCpptoHeaderDependencyGeneration_enabled(true);
        projectParser.setExpandTreeuptoMethodLevel_enabled(true);
        projectParser.setExtendedDependencyGeneration_enabled(true);

        ProjectNode projectRoot = projectParser.getRootTree();
        Environment.getInstance().setProjectNode(projectRoot);

        ISourcecodeFileNode consideredSourcecodeNode = (ISourcecodeFileNode) Search
                .searchNodes(projectParser.getRootTree(), new SourcecodeFileNodeCondition(), "ex3.cpp").get(0);
        System.out.println(consideredSourcecodeNode.getAbsolutePath());

        //
        String tpFile = "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/nnnn/test-paths/test1.97817.tp";
        SourcecodeCoverageComputation computator = new SourcecodeCoverageComputation();
        computator.setTestpathContent(Utils.readFileContent(tpFile));
        computator.setConsideredSourcecodeNode(consideredSourcecodeNode);
        computator.setCoverage(EnviroCoverageTypeNode.STATEMENT);
        computator.compute();
        System.out.println("[" + Thread.currentThread().getName() + "] " + "number of instructions = " + computator.getNumberOfInstructions());
        System.out.println("[" + Thread.currentThread().getName() + "] " + "number of visited instructions = " + computator.getNumberOfVisitedInstructions());

        // highlighter
        SourcecodeHighlighterForCoverage highlighter = new SourcecodeHighlighterForCoverage();
        highlighter.setSourcecode(consideredSourcecodeNode.getAST().getRawSignature());
        highlighter.setTestpathContent(Utils.readFileContent(tpFile));
        highlighter.setSourcecodePath("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/macro/ex3.cpp");
        highlighter.setAllCFG(computator.getAllCFG());
        highlighter.setTypeOfCoverage(computator.getCoverage());
        highlighter.highlight();
        Utils.writeContentToFile(highlighter.getFullHighlightedSourcecode(), "/Users/ducanhnguyen/Desktop/x.html");
    }

    protected Map<String, TestpathsOfAFunction>  removeRedundantTestpath(Map<String, TestpathsOfAFunction> affectedFunctions){
        return affectedFunctions;
    }

    protected int getNumberofBranches(INode consideredSourcecodeNode, String coverageType) {
        int nBranches = 0;
        List<INode> functionNodes = Search.searchNodes(consideredSourcecodeNode, new AbstractFunctionNodeCondition());
        for (INode functionNode : functionNodes)
            if (functionNode instanceof AbstractFunctionNode) {
                try {
                    ICFG cfg = Utils.createCFG((IFunctionNode) functionNode, coverageType);
                    if (cfg != null) {
                        nBranches += cfg.getUnvisitedBranches().size() + cfg.getVisitedBranches().size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        return nBranches;
    }

    protected int getNumberofStatements(INode consideredSourcecodeNode, String coverageType) {
        int nStatements = 0;
        // constructor, deconstructor, normal function
        List<INode> functionNodes = Search.searchNodes(consideredSourcecodeNode, new AbstractFunctionNodeCondition());
        for (INode functionNode : functionNodes)
            if (functionNode instanceof AbstractFunctionNode) {
                try {
                    ICFG cfg = Utils.createCFG((IFunctionNode) functionNode, coverageType);
                    if (cfg != null) {
                        nStatements += cfg.getVisitedStatements().size() + cfg.getUnvisitedStatements().size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        // macro function
        List<INode> macrofunctionNodes = Search.searchNodes(consideredSourcecodeNode, new MacroFunctionNodeCondition());
        for (INode macroFunctionNode : macrofunctionNodes)
            if (macroFunctionNode instanceof MacroFunctionNode) {
                try {
                    ICFG cfg = Utils.createCFG(((MacroFunctionNode) macroFunctionNode).getCorrespondingFunctionNode(), coverageType);
                    if (cfg != null) {
                        nStatements += cfg.getVisitedStatements().size() + cfg.getUnvisitedStatements().size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        return nStatements;
    }

}

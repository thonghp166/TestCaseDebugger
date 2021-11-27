package com.dse.coverage;

import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.instrument.IFunctionInstrumentationGeneration;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.coverage.CFGUpdaterv2;
import auto_testcase_generation.testdatagen.coverage.ICoverageComputation;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.highlight.AbstractHighlighterForSourcecodeLevel;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.MacroFunctionNodeCondition;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCoverageComputation implements ICoverageComputation {
    final static Logger logger = Logger.getLogger(SourcecodeCoverageComputation.class);

    private String testpathContent; // may visit may source code files
    private INode consideredSourcecodeNode; // the source code file which we need to compute coverage at file level
    private String coverage;
    private int numberOfVisitedInstructions; // depend on coverage, instruction is statement, condition, or sub-condition
    private int numberOfInstructions;
    private List<ICFG> allCFG = new ArrayList<>();

    public void compute() {
        if (consideredSourcecodeNode == null || !(new File(consideredSourcecodeNode.getAbsolutePath())).exists()
                || !(consideredSourcecodeNode instanceof SourcecodeFileNode)
                || testpathContent == null || testpathContent.length() == 0) {
            this.numberOfInstructions = getNumberOfInstructions(consideredSourcecodeNode, coverage);
            return;
        }
        if (coverage.equals(EnviroCoverageTypeNode.STATEMENT_AND_BRANCH) ||
                coverage.equals(EnviroCoverageTypeNode.STATEMENT_AND_MCDC))
            return;

        Map<String, TestpathsOfAFunction> affectedFunctions = categoryTestpathByFunctionPath(testpathContent.split("\n"), coverage);
        affectedFunctions = removeRedundantTestpath(affectedFunctions);
        int nVisitedInstructions = getNumberOfVisitedInstructions(affectedFunctions, coverage, consideredSourcecodeNode, allCFG);
        int nInstructions = getNumberOfInstructions(consideredSourcecodeNode, coverage);

        this.numberOfInstructions = nInstructions;
        this.numberOfVisitedInstructions = nVisitedInstructions;
    }

    protected abstract Map<String, TestpathsOfAFunction> removeRedundantTestpath(Map<String, TestpathsOfAFunction> affectedFunctions);

    private int getNumberOfInstructions(INode consideredSourcecodeNode, String coverage) {
        int nInstructions = 0;
        switch (coverage) {
            case EnviroCoverageTypeNode.STATEMENT: {
                nInstructions = getNumberofStatements(consideredSourcecodeNode, EnviroCoverageTypeNode.STATEMENT);
                break;
            }
            case EnviroCoverageTypeNode.MCDC: {
                nInstructions = getNumberofBranches(consideredSourcecodeNode, EnviroCoverageTypeNode.MCDC);
                break;
            }

            case EnviroCoverageTypeNode.BRANCH: {
                nInstructions = getNumberofBranches(consideredSourcecodeNode, EnviroCoverageTypeNode.BRANCH);
                break;
            }

            case EnviroCoverageTypeNode.BASIS_PATH: {
                nInstructions = getNumberOfBasisPath(consideredSourcecodeNode, EnviroCoverageTypeNode.BASIS_PATH);
                break;
            }
        }
        return nInstructions;
    }

    /**
     * @param affectedFunctions
     * @param coverage
     * @param consideredSourcecodeNode
     * @param allCFG
     * @return the number of visited instructions (>=0), return -1 if there is error happening
     */
    private int getNumberOfVisitedInstructions(Map<String, TestpathsOfAFunction> affectedFunctions,
                                               String coverage, INode consideredSourcecodeNode,
                                               List<ICFG> allCFG) {
        final int ERROR = -1;
        int nVisitedInstructions = 0;
        for (String functionPath : affectedFunctions.keySet())
            // only consider functions in a specified source code file
            if (functionPath.contains(consideredSourcecodeNode.getAbsolutePath())) {
                logger.debug("[" + Thread.currentThread().getName() + "] " + "Analyzing " + functionPath);
                // Find the function node
                TestpathsOfAFunction testpathsOfAFunction = affectedFunctions.get(functionPath);

                List<INode> functionNodes = Search.searchNodes(consideredSourcecodeNode, new AbstractFunctionNodeCondition(), PathUtils.toAbsolute(functionPath));
                if (functionNodes.size() == 0)
                    functionNodes = Search.searchNodes(consideredSourcecodeNode, new MacroFunctionNodeCondition(), PathUtils.toAbsolute(functionPath));

                if (functionNodes.size() != 1)
                    return ERROR;

                // generate cfg of the function
                INode functionNode = functionNodes.get(0);
                try {
                    ICFG cfg = null;

                    if (functionNode instanceof MacroFunctionNode) {
                        IFunctionNode tmpFunctionNode = ((MacroFunctionNode) functionNode).getCorrespondingFunctionNode();
                        cfg = Utils.createCFG(tmpFunctionNode, coverage);
                        allCFG.add(cfg);
                        cfg.setFunctionNode(tmpFunctionNode);
                    } else if (functionNode instanceof AbstractFunctionNode) {
                        cfg = Utils.createCFG((IFunctionNode) functionNode, coverage);
                        allCFG.add(cfg);
                        cfg.setFunctionNode((IFunctionNode) functionNode);
                    }

                    if (cfg == null)
                        return ERROR;

                    // compute coverage of a cfg
                    TestpathString_Marker testpath = new TestpathString_Marker();

                    if (testpathsOfAFunction != null && testpathsOfAFunction.getTestpathsInArray() != null)
                        testpath.setEncodedTestpath(testpathsOfAFunction.getTestpathsInArray());
                    else
                        testpath.setEncodedTestpath(new String[]{});

                    CFGUpdaterv2 cfgUpdaterv2 = new CFGUpdaterv2(testpath, cfg);
                    cfgUpdaterv2.updateVisitedNodes();
                    switch (coverage) {
                        case EnviroCoverageTypeNode.STATEMENT: {
                            nVisitedInstructions += cfg.getVisitedStatements().size();
                            break;
                        }
                        case EnviroCoverageTypeNode.MCDC:
                        case EnviroCoverageTypeNode.BRANCH: {
                            nVisitedInstructions += cfg.getVisitedBranches().size();
                            break;
                        }

                        case EnviroCoverageTypeNode.BASIS_PATH: {
                            nVisitedInstructions += cfg.getVisitedBasisPaths().size();
                            break;
                        }
                    }
                    logger.debug("[" + Thread.currentThread().getName() + "] " + "num of visited instructions = " + nVisitedInstructions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        return nVisitedInstructions;
    }

    /**
     * @param testpaths
     * @return a hash map, where key is the path of function, value is a list. Each item in list is corresponding to a test path.
     */
    private Map<String, TestpathsOfAFunction> categoryTestpathByFunctionPath(String[] testpaths, String coverage) {
        Map<String, TestpathsOfAFunction> tps = new HashMap<>();

        for (String testpath : testpaths) {
            String functionAddress = getValue(testpath, IFunctionInstrumentationGeneration.FUNCTION_ADDRESS);

            if (functionAddress != null && functionAddress.length() > 0) {
                functionAddress = PathUtils.toAbsolute(functionAddress);
                functionAddress = Utils.normalizePath(functionAddress);

                switch (coverage) {
                    case EnviroCoverageTypeNode.BRANCH:
                    case EnviroCoverageTypeNode.BASIS_PATH:
                    case EnviroCoverageTypeNode.STATEMENT: {
                        if (AbstractHighlighterForSourcecodeLevel.isSubCondition(testpath))
                            // ignore the test path which goes through subcondition
                            continue;
                        else
                            break;
                    }

                    case EnviroCoverageTypeNode.MCDC: {
                        if (AbstractHighlighterForSourcecodeLevel.isFullCondition(testpath))
                            // ignore the test path which goes through full condition
                            continue;
                        else
                            break;
                    }
                }

                if (!tps.containsKey(functionAddress))
                    tps.put(functionAddress, new TestpathsOfAFunction());

                Offset offset = new Offset();
                offset.startingOffsetInFunction = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.START_OFFSET_IN_FUNCTION));
                offset.endOffsetInFunction = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.END_OFFSET_IN_FUNCTION));
                offset.startingOffsetInSourcecodeFile = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.START_OFFSET_IN_SOURCE_CODE_FILE));
                offset.endOffsetInSourcecodeFile = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.END_OFFSET_IN_SOURCE_CODE_FILE));
                offset.testpath = testpath;
                tps.get(functionAddress).testpaths.add(offset);
            }
        }
        return tps;
    }

    private int getNumberOfBasisPath(INode consideredSourcecodeNode, String coverageType) {
        return 0; // code later
    }

    private String getValue(String line, String property) {
        if (line.contains(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTIES)) {
            String[] tokens = line.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTIES);
            for (String token : tokens)
                if (token.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTY_AND_VALUE)[0].equals(property))
                    return token.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTY_AND_VALUE)[1];
        }
        return null;
    }

    protected abstract int getNumberofBranches(INode consideredSourcecodeNode, String coverageType);

    protected abstract int getNumberofStatements(INode consideredSourcecodeNode, String coverageType);

    public void setTestpathContent(String testpathContent) {
        this.testpathContent = testpathContent;
    }

    public String getTestpathContent() {
        return testpathContent;
    }

    public void setConsideredSourcecodeNode(INode consideredSourcecodeNode) {
        this.consideredSourcecodeNode = consideredSourcecodeNode;
    }

    public INode getConsideredSourcecodeNode() {
        return consideredSourcecodeNode;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public int getNumberOfInstructions() {
        return numberOfInstructions;
    }

    public void setNumberOfInstructions(int numberOfInstructions) {
        this.numberOfInstructions = numberOfInstructions;
    }

    public int getNumberOfVisitedInstructions() {
        return numberOfVisitedInstructions;
    }

    public void setNumberOfVisitedInstructions(int numberOfVisitedInstructions) {
        this.numberOfVisitedInstructions = numberOfVisitedInstructions;
    }

    public List<ICFG> getAllCFG() {
        return allCFG;
    }

    public void setAllCFG(List<ICFG> allCFG) {
        this.allCFG = allCFG;
    }

    class TestpathsOfAFunction {
        ArrayList<Offset> testpaths = new ArrayList<>();

        @Override
        public String toString() {
            return testpaths.toString();
        }

        String[] getTestpathsInArray() {
            String[] tpInArray = new String[testpaths.size()];
            int count = 0;
            for (Offset offset : testpaths) {
                tpInArray[count] = offset.testpath;
                count++;
            }
            return tpInArray;
        }
    }

    class Offset {
        int startingOffsetInSourcecodeFile;
        int endOffsetInSourcecodeFile;
        int startingOffsetInFunction;
        int endOffsetInFunction;
        String testpath;

        @Override
        public String toString() {
            return "offset in source code file = " + startingOffsetInSourcecodeFile + ":" + endOffsetInSourcecodeFile + ", offset in function = " + startingOffsetInFunction + ":" + endOffsetInFunction + "\n";
        }
    }
}

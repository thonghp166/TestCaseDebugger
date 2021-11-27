package com.dse.coverage;

import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.coverage.CFGUpdaterv2;
import com.dse.coverage.function_call.ConstructorCall;
import com.dse.coverage.function_call.FunctionCall;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.report.element.Event;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.util.PathUtils;
import com.dse.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.dse.coverage.AbstractCoverageManager.removeRedundantLineBreak;

public class TestPathUtils {
    public static final String CALLING_TAG = "Calling: ";
    public static final String SKIP_TAG = "SKIP ";
    public static final String BEGIN_TAG = "BEGIN OF ";
    public static final String END_TAG = "END OF ";

    public static List<FunctionCall> traceFunctionCall(String filePath) {
        List<FunctionCall> calledFunctions = new ArrayList<>();

        String[] lines = Utils.readFileContent(filePath).split("\\R");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(CALLING_TAG)) {
                String functionPath = lines[i].substring(CALLING_TAG.length());
                functionPath = Utils.normalizePath(functionPath);
                functionPath = PathUtils.toAbsolute(functionPath);

                FunctionCall call;

                if (functionPath.contains("|")) {
                    String[] paths = functionPath.split("\\Q|\\E");
                    call = new ConstructorCall();
                    call.setAbsolutePath(paths[0]);
                    ((ConstructorCall) call).setParameterPath(paths[1]);
                } else {
                    call = new FunctionCall();
                    call.setAbsolutePath(functionPath);
                }

                if (i > 0 && lines[i - 1].startsWith("<<PRE-CALLING>>"))
                    call.setCategory(Event.Position.FIRST);
                else
                    call.setCategory(Event.Position.MIDDLE);

                call.setIndex(calledFunctions.size());

                calledFunctions.add(call);

                int iterator = (int) calledFunctions.stream()
                        .filter(c -> c.getAbsolutePath().equals(call.getAbsolutePath()))
                        .count();

                call.setIterator(iterator);

            } else if (lines[i].startsWith("Return from: ")) {
                String functionPath = lines[i].substring("Return from: ".length());

                FunctionCall call;

                if (functionPath.contains("|")) {
                    String[] paths = functionPath.split("\\Q|\\E");
                    call = new ConstructorCall();
                    call.setAbsolutePath(PathUtils.toAbsolute(paths[0]));
                    ((ConstructorCall) call).setParameterPath(paths[1]);
                } else {
                    call = new FunctionCall();
                    functionPath = PathUtils.toAbsolute(functionPath);
                    call.setAbsolutePath(functionPath);
                }

                call.setCategory(Event.Position.LAST);
                call.setIndex(calledFunctions.size());
                calledFunctions.add(call);
            }
        }

        return calledFunctions;
    }

    public static ICFG getCFG(TestCase testCase) {
        ICommonFunctionNode sut = testCase.getRootDataNode().getFunctionNode();

        if (!(sut instanceof IFunctionNode))
            return null;

        IFunctionNode function = (IFunctionNode) sut;

        File testPath = new File(testCase.getTestPathFile());

        if (testPath.exists()) {
            String content = Utils.readFileContent(testPath);
            String[] lines = removeRedundantLineBreak(content).split("\n");

            try {
                ICFG cfg = Utils.createCFG(function, Environment.getInstance().getTypeofCoverage());

                // Update the cfg
                TestpathString_Marker marker = new TestpathString_Marker();
                marker.setEncodedTestpath(lines);

                CFGUpdaterv2 updater = new CFGUpdaterv2(marker, cfg);
                updater.updateVisitedNodes();

                return cfg;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    public static List<Object> getVisited(TestCase testCase, String type) {
        List<Object> visited;

        ICFG cfg = getCFG(testCase);

        if (cfg == null)
            return null;

        switch (type) {
            case EnviroCoverageTypeNode.STATEMENT:
                visited = cfg.getVisitedStatements().stream().map(n -> (Object) n).collect(Collectors.toList());
                break;
            case EnviroCoverageTypeNode.BRANCH:
            case EnviroCoverageTypeNode.MCDC:
                visited = cfg.getVisitedBranches().stream().map(n -> (Object) n).collect(Collectors.toList());
                break;

            case EnviroCoverageTypeNode.BASIS_PATH: {
                visited = cfg.getVisitedBasisPaths().stream().map(n -> (Object) n).collect(Collectors.toList());
                break;
            }
            case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH:
            case EnviroCoverageTypeNode.STATEMENT_AND_MCDC: {
                visited = cfg.getVisitedBranches().stream().map(n -> (Object) n).collect(Collectors.toList());
                break;
            }
            default:{
                visited = Collections.singletonList(0); // unspecified
            }
        }
        return visited;
    }

    public static List<Object> getVisitedOfCurrentType(TestCase testCase) {
        return getVisited(testCase, Environment.getInstance().getTypeofCoverage());
    }

    /**
     * Compare visited statement/branch between 2 test cases
     *
     * @return integer corresponding comparision
     *      [0] EQUAL           - equal
     *      [1] SEPARATED_GT    - tc1 and tc2 doesn't have common part (tc1 >= tc2)
     *      [2] COMMON_GT       - tc1 and tc2 have common part (tc1 >= tc2)
     *      [3] CONTAIN_GT      - tc1 contain tc2
     *      ~ negative value mean tc2 > tc1
     */
    public static int compare(TestCase tc1, TestCase tc2) {
        if (!tc1.getStatus().equals(ITestCase.STATUS_SUCCESS) || !tc2.getStatus().equals(ITestCase.STATUS_SUCCESS)) {
            if (!tc1.getStatus().equals(ITestCase.STATUS_SUCCESS))
                return HAVENT_EXEC;
            else
                return HAVENT_EXEC * -1;
        }

        if (!tc1.getFunctionNode().equals(tc2.getFunctionNode()))
            return ERR_COMPARE;

        List<Object> visited1 = getVisitedOfCurrentType(tc1);
        List<Object> visited2 = getVisitedOfCurrentType(tc2);

        return compare(visited1, visited2);
    }

    public static int compare(TestCase testCase, List<TestCase> other) {
        ICommonFunctionNode sut = testCase.getFunctionNode();

        if (!testCase.getStatus().equals(ITestCase.STATUS_SUCCESS))
            return HAVENT_EXEC;

        List<Object> visited1 = getVisitedOfCurrentType(testCase);
        List<Object> visited2 = new ArrayList<>();

        for (TestCase tc : other) {
            if (!tc.getFunctionNode().equals(sut))
                return ERR_COMPARE;

            if (tc.getStatus().equals(ITestCase.STATUS_SUCCESS)) {
                List<Object> visited = getVisitedOfCurrentType(tc);
                if (visited != null) {
                    for (Object node : visited) {
                        if (!visited2.contains(node))
                            visited2.add(node);
                    }
                }
            }
        }

        if (visited2.isEmpty())
            return HAVENT_EXEC * -1;

        return compare(visited1, visited2);
    }

    public static int compare(List<Object> visited1, List<Object> visited2) {
        int result;

        if (visited1 == null || visited2 == null)
            return ERR_COMPARE;
        else if (visited1.size() >= visited2.size())
            result = 1;
        else
            result = -1;

        List<Object> commonPart = new ArrayList<>();

        for (Object node1 : visited1) {
            for (Object node2 : visited2) {
                if (node1.equals(node2)) {
                    commonPart.add(node1);
                }
            }
        }

        if (commonPart.isEmpty())
            result *= SEPARATED_GT;
        else if (commonPart.size() == visited1.size() && commonPart.size() == visited2.size())
            result *= EQUAL;
        else if (commonPart.size() == visited1.size() || commonPart.size() == visited2.size())
            result *= CONTAIN_GT;
        else
            result *= COMMON_GT;

        return result;

    }

    public static final int EQUAL = 0;
    public static final int SEPARATED_GT = 1;
    public static final int COMMON_GT = 2;
    public static final int CONTAIN_GT = 3;
    public static final int ERR_COMPARE = 4;
    public static final int HAVENT_EXEC = 5;
}

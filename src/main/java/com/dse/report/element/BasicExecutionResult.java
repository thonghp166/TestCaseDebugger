package com.dse.report.element;

import com.dse.coverage.function_call.ConstructorCall;
import com.dse.coverage.function_call.FunctionCall;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.gtest.Execution;
import com.dse.gtest.Failure;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.search.Search2;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.object.IDataNode;
import com.dse.testdata.object.SubprogramNode;
import com.dse.coverage.TestPathUtils;
import com.dse.util.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import org.apache.commons.math3.stat.inference.GTest;

import java.io.File;
import java.util.List;

public class BasicExecutionResult extends AbstractExecutionResult {
    private static final int MAX_DUPLICATE = 3;
    private static final boolean SKIP_DUPLICATE = false;

    public BasicExecutionResult(ITestCase testCase, Execution execution) {
        super(testCase, execution);
    }

    @Override
    protected void generate(ITestCase tc, Execution execution) {
        TestCase testCase = (TestCase) tc;

        Line sectionTitle = new Line(testCase.getName() + " Execution Result", COLOR.DARK);
        title.add(sectionTitle);

        String testPath = testCase.getTestPathFile();

        if (testPath == null || !new File(testPath).exists()) {
            body.add(new Line("Test case haven't executed yet", COLOR.LIGHT));

        } else if (Environment.getInstance().isC()){
            generateC(testCase);
        } else
            generateCPP(testCase, execution);

        body.add(new BlankLine());
    }

    private void generateCoverageHighlight(TestCase testCase) {
        body.add(new Section.CenteredLine("Coverage Highlight", COLOR.MEDIUM));

        String typeOfCoverage = Environment.getInstance().getTypeofCoverage();
        switch (typeOfCoverage){
            case EnviroCoverageTypeNode.STATEMENT:
            case EnviroCoverageTypeNode.BRANCH:
            case EnviroCoverageTypeNode.BASIS_PATH:
            case EnviroCoverageTypeNode.MCDC:{
                String coverageHighlight = Utils.readFileContent(testCase.getHighlightedFunctionPath(typeOfCoverage));
                body.add(new CodeView(coverageHighlight));
                break;
            }

            case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH:{
                String coverageHighlight = Utils.readFileContent(testCase.getHighlightedFunctionPath(EnviroCoverageTypeNode.STATEMENT));
                body.add(new CodeView(coverageHighlight));

                coverageHighlight = Utils.readFileContent(testCase.getHighlightedFunctionPath(EnviroCoverageTypeNode.BRANCH));
                body.add(new CodeView(coverageHighlight));
                break;
            }

            case EnviroCoverageTypeNode.STATEMENT_AND_MCDC:{
                String coverageHighlight = Utils.readFileContent(testCase.getHighlightedFunctionPath(EnviroCoverageTypeNode.STATEMENT));
                body.add(new CodeView(coverageHighlight));

                coverageHighlight = Utils.readFileContent(testCase.getHighlightedFunctionPath(EnviroCoverageTypeNode.MCDC));
                body.add(new CodeView(coverageHighlight));
                break;
            }
        }
    }

    private void generateC(TestCase testCase) {
        File exeResult = new File(testCase.getExecutionResultFile());
        com.dse.gtest.TestCase gtTestCase;
        String content = Utils.readFileContent(exeResult);

        if (exeResult.exists() && content.contains("<SUCCEEDED>")) {
            if (content.contains("<SUCCEEDED> 1 </SUCCEEDED>")) {
                gtTestCase = new com.dse.gtest.TestCase() {
                    @Override
                    public String getResult() {
                        return Execution.PASSED;
                    }
                };

                gtTestCase.setStatus(Execution.PASSED);
            } else {
                gtTestCase = new com.dse.gtest.TestCase() {
                    @Override
                    public String getResult() {
                        return Execution.FAILED;
                    }
                };

                gtTestCase.setStatus(Execution.FAILED);
            }
        } else {
            gtTestCase = new com.dse.gtest.TestCase() {
                @Override
                public String getResult() {
                    return "Runtime Error";
                }
            };

            gtTestCase.setStatus("Runtime Error");
        }

        gtTestCase.setName(testCase.getName());
        gtTestCase.setClassName(testCase.getFunctionNode().getSimpleName());

        Table overall = generateOverallResultsTable(gtTestCase, testCase);
        body.add(overall);

        generateCoverageHighlight(testCase);
    }

    private void generateCPP(TestCase testCase, Execution execution) {
        com.dse.gtest.TestCase gtTestCase;
        if (execution == null) {
            gtTestCase = new com.dse.gtest.TestCase() {
                @Override
                public String getResult() {
                    return "Runtime Error";
                }
            };

            gtTestCase.setName(testCase.getName());
            gtTestCase.setClassName(testCase.getFunctionNode().getSimpleName());
            gtTestCase.setStatus("Runtime Error");

        } else
            gtTestCase = execution.getTestCaseByName(testCase.getName());

        Table overall = generateOverallResultsTable(gtTestCase, testCase);
        body.add(overall);

        generateCoverageHighlight(testCase);

        generateEventsSection(gtTestCase, testCase);
    }

    private void generateEventsSection(com.dse.gtest.TestCase gtTestCase, TestCase testCase) {
        // runtime error
        if (gtTestCase == null) {
            body.add(new Section.CenteredLine("Got runtime error when running executable file", COLOR.MEDIUM));
            return;
        }

        List<Failure> failures = gtTestCase.getFailure();

        List<FunctionCall> calledFunctions = TestPathUtils
                .traceFunctionCall(testCase.getTestPathFile());

        List<IDataNode> subprograms = Search2
                .searchNodes(testCase.getRootDataNode(), new SubprogramNode());

        int skip = 0;
        String firstLine = Utils.readFileContent(testCase.getTestPathFile()).split("\\R")[0];
        if (firstLine.startsWith(TestPathUtils.SKIP_TAG))
            skip = Integer.parseInt(firstLine.substring(TestPathUtils.SKIP_TAG.length()));

        /*
         * Result PASS/ALL
         */
        int[] results = new int[] {0, 0};

        int duplicate = 0;
        FunctionCall prev = null;

        Event.Position position = Event.Position.UNKNOWN;

        for (int i = 0; i < calledFunctions.size() && position != Event.Position.LAST; i++) {
            FunctionCall current = calledFunctions.get(i);
            SubprogramNode subprogram = findSubprogram(subprograms, current);
            position = current.getCategory();

            if (prev != null && prev.equals(current))
                duplicate++;
            else
                duplicate = 0;

            // skip current event if access the same subprogram as previous
            if (SKIP_DUPLICATE && duplicate >= MAX_DUPLICATE)
                continue;

            int index = i + skip + 1;

            if (subprogram != null && Utils.getSourcecodeFile(subprogram.getFunctionNode()) != null) {
                Event event;

                if (position == Event.Position.MIDDLE && current.getIterator() > 1)
                    event = new Event(subprogram, failures, index, position, current.getIterator());
                else
                    event = new Event(subprogram, failures, index, position);

                for (int j = 0; j < event.getResults().length; j++)
                    results[j] += event.getResults()[j];

                body.add(event);
            }

            prev = calledFunctions.get(i);
        }

        int pass = results[0];
        int all = results[1];

        testCase.setExecutionResult(results);

        String textColor = pass == all ? "green" : "red";
        double passPercent = (double) pass * 100 / (double) all;

        Table resultsSummary = new Table();
        resultsSummary.getRows().add(
            new Table.Row(
                new Text(String.format("Expected Results matched %.2f%%", passPercent)),
                new Text(String.format("(%d/%d) PASS", pass, all), TEXT_STYLE.BOLD, textColor)
            )
        );

        body.add(resultsSummary);
    }

    private SubprogramNode findSubprogram(List<IDataNode> subprograms, FunctionCall call) {
        for (IDataNode subprogram : subprograms) {
            if (subprogram instanceof SubprogramNode) {
                INode functionNode = ((SubprogramNode) subprogram).getFunctionNode();

                if (call instanceof ConstructorCall) {
                    if (functionNode.getAbsolutePath().equals(call.getAbsolutePath())
                            && subprogram.getPathFromRoot().equals(((ConstructorCall) call).getParameterPath()))
                        return (SubprogramNode) subprogram;
                } else {
                    if (functionNode.getAbsolutePath().equals(call.getAbsolutePath()))
                        return (SubprogramNode) subprogram;
                }
            }
        }

        try {
            INode called = UIController.searchFunctionNodeByPath(call.getAbsolutePath());
            return new SubprogramNode(called);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private Table generateOverallResultsTable(com.dse.gtest.TestCase gtTestCase, TestCase testCase) {
        String result = gtTestCase.getResult();
        Text resultText = new Text(result, TEXT_STYLE.BOLD, result.equals(Execution.PASSED) ? "green" : "red");

        String coveragePercentage = "";
        String typeOfCoverage = Environment.getInstance().getTypeofCoverage();
        switch (typeOfCoverage) {
            case EnviroCoverageTypeNode.BRANCH:
            case EnviroCoverageTypeNode.STATEMENT:
            case EnviroCoverageTypeNode.MCDC:
            case EnviroCoverageTypeNode.BASIS_PATH: {
                JsonElement json = new JsonParser().parse(Utils.readFileContent(testCase.getProgressCoveragePath(typeOfCoverage)));
                if (json != null && !(json instanceof JsonNull))
                    coveragePercentage = Utils.round(json.getAsJsonObject().get(typeOfCoverage).getAsDouble() * 100, 2) + "%";
                break;
            }

            case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH: {
                JsonElement json = new JsonParser().parse(Utils.readFileContent(testCase.getProgressCoveragePath(EnviroCoverageTypeNode.STATEMENT)));
                if (json != null && !(json instanceof JsonNull))
                    coveragePercentage = Utils.round(json.getAsJsonObject().get(EnviroCoverageTypeNode.STATEMENT).getAsDouble() * 100, 2) + "% (" + EnviroCoverageTypeNode.STATEMENT + "); ";

                json = new JsonParser().parse(Utils.readFileContent(testCase.getProgressCoveragePath(EnviroCoverageTypeNode.BRANCH)));
                if (json != null && !(json instanceof JsonNull))
                    coveragePercentage += Utils.round(json.getAsJsonObject().get(EnviroCoverageTypeNode.BRANCH).getAsDouble() * 100, 2) + "% (" + EnviroCoverageTypeNode.BRANCH + ")";
                break;
            }
            case EnviroCoverageTypeNode.STATEMENT_AND_MCDC: {
                JsonElement json = new JsonParser().parse(Utils.readFileContent(testCase.getProgressCoveragePath(EnviroCoverageTypeNode.STATEMENT)));
                if (json != null && !(json instanceof JsonNull))
                    coveragePercentage = Utils.round(json.getAsJsonObject().get(EnviroCoverageTypeNode.STATEMENT).getAsDouble() * 100, 2) + "% (" + EnviroCoverageTypeNode.STATEMENT + "); ";

                json = new JsonParser().parse(Utils.readFileContent(testCase.getProgressCoveragePath(EnviroCoverageTypeNode.MCDC)));
                if (json != null && !(json instanceof JsonNull))
                    coveragePercentage += Utils.round(json.getAsJsonObject().get(EnviroCoverageTypeNode.MCDC).getAsDouble() * 100, 2) + "% (" + EnviroCoverageTypeNode.MCDC + ")";
                break;
            }

            default:{

            }
        }

        double executedTime = gtTestCase.getTime();

        Table overall = new Table();
        overall.getRows().add(new Table.Row(new Table.Cell<Text>("Result:"), new Table.Cell<>(resultText)));
        overall.getRows().add(new Table.Row("Coverage:", String.format("%s", coveragePercentage)));
        overall.getRows().add(new Table.Row("Executed Time:", String.format("%.3fs", executedTime)));
        return overall;
    }
}

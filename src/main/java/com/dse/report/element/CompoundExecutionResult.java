package com.dse.report.element;

import com.dse.gtest.Execution;
import com.dse.testcase_manager.*;

import java.io.File;

public class CompoundExecutionResult extends AbstractExecutionResult {

    public CompoundExecutionResult(ITestCase testCase, Execution execution) {
        super(testCase, execution);
    }

    @Override
    protected void generate(ITestCase tc, Execution execution) {
        CompoundTestCase testCase = (CompoundTestCase) tc;

        Line sectionTitle = new Line(testCase.getName() + " Execution Result", COLOR.DARK);
        title.add(sectionTitle);

        String testPath = testCase.getTestPathFile();

        if (testPath == null || !new File(testPath).exists()) {
            body.add(new Line("Test case haven't executed yet", COLOR.LIGHT));

        } else if (execution == null) {
            body.add(new Line("Got runtime error when running executable file", COLOR.LIGHT));

        } else {
            Table overall = generateCompoundOverallResultTable(execution);

            body.add(overall);
            body.add(new Section.BlankLine());

            for (TestCaseSlot slot : testCase.getSlots()) {
                String elementName = slot.getTestcaseName();
                TestCase element = TestCaseManager.getBasicTestCaseByName(elementName);

                if (element != null) {
                    BasicExecutionResult elementResults = new BasicExecutionResult(element, execution);
                    testCase.appendExecutionResult(element.getExecutionResult());

                    body.add(elementResults);
                }
            }
        }

        body.add(new BlankLine());
    }

    private Table generateCompoundOverallResultTable(Execution execution) {
        int tests = execution.getTests();
        int failures = execution.getFailures();
        String result = Execution.PASSED + String.format(" (%d/%d)", tests - failures, tests);

        Text resultText = new Text(result, TEXT_STYLE.BOLD, failures == 0 ? "green" : "red");

        double executedTime = execution.getTime();

        Table overall = new Table();
        overall.getRows().add(new Table.Row(new Table.Cell<Text>("Result:"), new Table.Cell(resultText)));
        overall.getRows().add(new Table.Row("Executed Time:", String.format("%.3fs", executedTime)));

        return overall;
    }
}

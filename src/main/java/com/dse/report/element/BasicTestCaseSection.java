package com.dse.report.element;

import com.dse.gtest.Execution;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;

public class BasicTestCaseSection extends AbstractTestCaseSection {

    public BasicTestCaseSection(ITestCase testCase, Execution execution) {
        super(testCase, execution);
    }

    @Override
    protected void generate(ITestCase testCase, Execution execution) {
        Line sectionTitle = new Line("Test Case Section: " + testCase.getName(), COLOR.DARK);
        title.add(sectionTitle);

        body.add(new TestCaseConfiguration(testCase, execution));
        body.add(new TestCaseData((TestCase) testCase));
    }
}

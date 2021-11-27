package com.dse.report.element;

import com.dse.gtest.Execution;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;

public abstract class AbstractTestCaseSection extends Section {

    public AbstractTestCaseSection(ITestCase testCase, Execution execution) {
        super(testCase.getName());

        generate(testCase, execution);
    }

    protected abstract void generate(ITestCase testCase, Execution execution);
}

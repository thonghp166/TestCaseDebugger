package com.dse.report.element;

import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.gtest.Execution;
import com.dse.guifx_v3.helps.Environment;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.util.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

public abstract class AbstractExecutionResult extends Section {

    public AbstractExecutionResult(ITestCase testCase, Execution execution) {
        super(testCase.getName() + "-result");
        generate(testCase, execution);
    }

    protected abstract void generate(ITestCase testCase, Execution execution);
}

package com.dse.report;

import com.dse.config.WorkspaceConfig;
import com.dse.gtest.Execution;
import com.dse.report.element.*;
import com.dse.testcase_manager.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExecutionResultReport extends ReportView {
    private ITestCase testCase;

    private Execution execution;

    public ExecutionResultReport(ITestCase testCase, LocalDateTime creationDateTime) {
        // set report name
        super(String.format("%s - Test Case Report", testCase.getName()));

        // set report attributes
        this.testCase = testCase;
        this.creationDateTime = creationDateTime;

        // load GTest execution (xml file)
        this.execution = Execution.load(testCase);

        // set report location path to default
        setPathDefault();

        // generate test case report
        generate();
    }

    @Override
    protected void generate() {
        // STEP 1: generate table of contents section
        sections.add(generateTableOfContents());

        // STEP 2: generate configuration data section
        sections.add(generateConfigurationData());

        // STEP 3 + 4: generate test case section & generate execution result section
        if (testCase instanceof TestCase) {
            sections.add(new BasicExecutionResult(testCase, execution));
        } else if (testCase instanceof CompoundTestCase) {
            sections.add(new CompoundExecutionResult(testCase, execution));
        }

        // STEP 5: save execution result
        TestCaseManager.exportTestCaseToFile(testCase);
    }

    @Override
    protected TableOfContents generateTableOfContents() {
        TableOfContents tableOfContents = new TableOfContents();

        tableOfContents.getBody().add(new TableOfContents.Item("Configuration Data", "config-data"));

        String testCaseName = testCase.getName();

        tableOfContents.getBody().add(
                new TableOfContents.Item("Test Case Configuration", testCaseName + "-config"));

        tableOfContents.getBody().add(
                new TableOfContents.Item("Test Execution Result", testCaseName + "-result"));

        return tableOfContents;
    }

    protected ConfigurationData generateConfigurationData() {
        ConfigurationData configData = new ConfigurationData();

        List<ITestCase> elements = new ArrayList<>();

        if (testCase instanceof TestCase)
            elements.add((TestCase) testCase);
        else if (testCase instanceof CompoundTestCase) {
            for (TestCaseSlot slot : ((CompoundTestCase) testCase).getSlots()) {
                String elementName = slot.getTestcaseName();
                TestCase element = TestCaseManager.getBasicTestCaseByName(elementName);
                elements.add(element);
            }
        }

        TestCaseTable table = new TestCaseTable(elements);

        configData.setCreationDate(getCreationDate());
        configData.setCreationTime(getCreationTime());
        configData.setTable(table);

        configData.generate();

        return configData;
    }

    @Override
    protected void setPathDefault() {
        this.path = new WorkspaceConfig().fromJson().getExecutionReportDirectory()
                + File.separator + testCase.getName() + ".html";
    }

    public Execution getExecution() {
        return execution;
    }
}

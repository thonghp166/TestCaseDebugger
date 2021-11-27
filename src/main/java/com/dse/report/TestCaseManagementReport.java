package com.dse.report;

import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.INode;
import com.dse.report.element.*;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.testcasescript.object.*;
import com.dse.util.SpecialCharacter;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestCaseManagementReport extends ReportView {
    private boolean isEnvironment = false;
    private boolean containCompound = false;
    private List<INode> units = new ArrayList<>();

    public TestCaseManagementReport(List<ITestcaseNode> nodes, LocalDateTime creationDt) {
        super("Test Case Management Report");

        // set report creation date time
        setCreationDateTime(creationDt);

        // set report location path to default
        setPathDefault();

        // find all selected unit
        findAllSelectedUnitsByTestNode(nodes);

        generate();
    }

    public TestCaseManagementReport(String unitName, LocalDateTime creationDt) {
        super("Test Case Management Report");

        // set report creation date time
        setCreationDateTime(creationDt);

        // set report location path to default
        setPathDefault();

        // find all selected unit
        findSelectedUnitByName(unitName);

        generate();
    }

    private void findSelectedUnitByName(String name) {
        List<INode> uuts = new ArrayList<>(Environment.getInstance().getUUTs());
        uuts.addAll(Environment.getInstance().getSBFs());

        units = uuts.stream().filter(u ->
                u.getName().equals(name)).collect(Collectors.toList());
    }

    private void findAllSelectedUnitsByTestNode(List<ITestcaseNode> nodes) {
        List<INode> allUnits = Environment.getInstance().getUUTs();
        allUnits.addAll(Environment.getInstance().getSBFs());

        for (ITestcaseNode node : nodes) {
            if (node instanceof TestCompoundSubprogramNode)
                containCompound = true;

            else if (node instanceof TestUnitNode) {
                String unitPath = ((TestUnitNode) node).getName();
                for (INode unit : allUnits) {
                    if (unit.getAbsolutePath().equals(unitPath)) {
                        units.add(unit);
                        break;
                    }
                }

            } else if (node instanceof TestcaseRootNode) {
                units = allUnits;
                containCompound = true;
                isEnvironment = true;
                break;
            }
        }
    }

    @Override
    protected void generate() {
        sections.add(generateTableOfContents());

        sections.add(generateConfigurationData());
        sections.add(new Section.BlankLine());

        sections.add(generateOverallResults());
        sections.add(new Section.BlankLine());

        sections.add(new TestCaseManagement(containCompound, units));
        sections.add(new Section.BlankLine());

        sections.add(new Metrics(units));
    }

    @Override
    protected TableOfContents generateTableOfContents() {
        TableOfContents tableOfContents = new TableOfContents();

        tableOfContents.getBody().add(new TableOfContents.Item("Configuration Data", "config-data"));

        tableOfContents.getBody().add(
                new TableOfContents.Item("Overall Results", "overall-results"));

        tableOfContents.getBody().add(
                new TableOfContents.Item("Test Case Management", "tcs-manage"));

        tableOfContents.getBody().add(
                new TableOfContents.Item("Metrics", "metrics"));

        return tableOfContents;
    }

    @Override
    protected Section generateConfigurationData() {
        Section section = new Section("config-data");

        section.getTitle().add(new Section.Line("Configuration Data", COLOR.DARK));

        Table table = new Table();

        String scope = SpecialCharacter.EMPTY;
        if (isEnvironment)
            scope = "All units under test";
        else {
            if (containCompound)
                scope = TestSubprogramNode.COMPOUND_SIGNAL;

            for (INode unit : units) {
                scope += ", " + unit.getName();
            }

            if (scope.startsWith(", "))
                scope = scope.substring(2);
        }

        table.getRows().add(new Table.Row("This report include data for: ", scope));
        table.getRows().add(new Table.Row("Date of Report Creation:", getCreationDate()));
        table.getRows().add(new Table.Row("Time of Report Creation:", getCreationTime()));
        section.getBody().add(table);

        return section;
    }

    protected Section generateOverallResults() {
        Section section = new Section("overall-results");

        section.getTitle().add(new Section.Line("Overall Results", COLOR.DARK));

        Table table = new Table();
        table.getRows().add(new Table.HeaderRow("Category", "Results"));

        int[] testCaseResults = new int[] {0, 0};
        int[] expectedResults = new int[] {0, 0};

        List<String> testCaseNames = getAllTestCaseUnderUnit();

        for (String testCaseName : testCaseNames) {
            ITestCase testCase = TestCaseManager.getTestCaseByName(testCaseName);

            if (testCase != null) {
                int[] result = testCase.getExecutionResult();

                if (result != null) {
                    if (result[0] == result[1])
                        testCaseResults[0]++;

                    testCaseResults[1]++;

                    expectedResults[0] += result[0];
                    expectedResults[1] += result[1];
                }
            }
        }

        table.getRows().add(new Table.Row(
                new Table.Cell<Text>("Test Cases:"),
                new Table.Cell<Text>(String.format("PASS %d/%d", testCaseResults[0], testCaseResults[1]),
                        getBackgroundColor(testCaseResults))
        ));

        table.getRows().add(new Table.Row(
                new Table.Cell<Text>("Expecteds:"),
                new Table.Cell<Text>(String.format("PASS %d/%d", expectedResults[0], expectedResults[1]),
                        getBackgroundColor(expectedResults))
        ));

        section.getBody().add(table);

        return section;
    }

    private List<String> getAllTestCaseUnderUnit() {
        List<String> testCaseNames = new ArrayList<>();

        if (isEnvironment) {
            testCaseNames.addAll(new ArrayList<>(TestCaseManager.getNameToCompoundTestCaseMap().keySet()));
            testCaseNames.addAll(new ArrayList<>(TestCaseManager.getNameToBasicTestCaseMap().keySet()));

        } else {
            if (containCompound)
                testCaseNames.addAll(TestCaseManager.getNameToCompoundTestCaseMap().keySet());

            for (INode unit : units) {
                List<INode> subprograms = Search.searchNodes(unit, new AbstractFunctionNodeCondition());

                for (INode subprogram : subprograms) {
                    List<String> testcaseTmpNames = TestCaseManager.getFunctionToTestCasesMap().get(subprogram);
                    if (testcaseTmpNames != null) {
                        testCaseNames.addAll(testcaseTmpNames);
                    }
                }
            }
        }

        return testCaseNames;
    }

    protected String getBackgroundColor(int[] result) {
        String bgColor;

        if (result[0] == result[1])
            bgColor = COLOR.GREEN;
        else if (result[0] == 0)
            bgColor = COLOR.RED;
        else
            bgColor = COLOR.YELLOW;

        return bgColor;
    }

    @Override
    protected void setPathDefault() {
        this.path = new WorkspaceConfig().fromJson().getReportDirectory()
                + File.separator + "test-cases-management.html";
    }
}

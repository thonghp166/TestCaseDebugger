package com.dse.report.element;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.testcase_manager.CompoundTestCase;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.util.SpecialCharacter;

import java.util.*;

public class TestCaseManagement extends Section {
    private boolean haveCompound = false;
    private List<INode> units;

    public TestCaseManagement(boolean compound, List<INode> units) {
        super("tcs-manage");

        this.haveCompound = compound;
        this.units = units;

        generate();
    }

    private void generate() {
        title.add(new Line("Test Case Management", COLOR.DARK));

        Table table = new Table(false);
        table.getRows().add(new Table.HeaderRow("Unit", "Subprogram", "Test Cases", "Execution Date and Time", "Pass/Fail"));

        if (haveCompound)
            table.getRows().addAll(generateCompoundTable());

        for (INode unit : units) {
            if (units.indexOf(unit) == 0) {
                if (haveCompound)
                    table.getRows().add(new Table.BlankRow(5));
            } else
                table.getRows().add(new Table.BlankRow(5));

            List<Table.Row> row = generateBasicTable(unit);
            if (row != null)
                table.getRows().addAll(row);
        }

        body.add(table);
    }

    private List<Table.Row> generateBasicTable(INode unit) {
        List<Table.Row> rows = new ArrayList<>();

        int[] summaryResult = new int[] {0, 0};
        int subprogramLength = 0;
        int testCaseLength = 0;

        List<INode> subprograms = Search.searchNodes(unit, new AbstractFunctionNodeCondition());

        subprogramLength += subprograms.size();

        for (INode subprogram : subprograms) {
            List<String> testCaseNames = TestCaseManager.getFunctionToTestCasesMap().get(subprogram);

            if (testCaseNames == null)
                continue;
            boolean firstSubprogram = subprograms.indexOf(subprogram) == 0;

            testCaseLength += testCaseNames.size();

            if (testCaseNames.isEmpty()) {
                String unitCol = SpecialCharacter.EMPTY;
                if (firstSubprogram)
                    unitCol = unit.getName();

                String subprogramCol = subprogram.getName();

                rows.add(new Table.Row(unitCol, subprogramCol,
                        SpecialCharacter.EMPTY, SpecialCharacter.EMPTY, SpecialCharacter.EMPTY
                ));
            }

            for (String name : testCaseNames) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);

                if (testCase != null) {
                    boolean firstTestCase = testCaseNames.indexOf(name) == 0;
                    firstSubprogram = firstSubprogram && testCaseNames.indexOf(name) == 0;

                    Table.Row row = generateRow(testCase, unit, subprogram, firstSubprogram, firstTestCase);
                    rows.add(row);

                    int[] result = testCase.getExecutionResult();
                    if (result != null) {
                        if (result[0] == result[1])
                            summaryResult[0]++;

                        summaryResult[1]++;
                    }
                }
            }
        }

        Table.Row endRow = generateEndRow(
                String.valueOf(subprogramLength), String.valueOf(testCaseLength), summaryResult);
        rows.add(endRow);

        return rows;
    }

    private List<Table.Row> generateCompoundTable() {
        List<Table.Row> rows = new ArrayList<>();

        int[] summaryResult = new int[] {0, 0};

        Set<String> testCaseNames = TestCaseManager.getNameToCompoundTestCaseMap().keySet();

        for (String testCaseName : testCaseNames) {
            CompoundTestCase testCase = TestCaseManager.getCompoundTestCaseByName(testCaseName);

            if (testCase != null) {
                Table.Row row = generateRow(testCase, null, null, false, false);
                rows.add(row);

                int[] result = testCase.getExecutionResult();
                if (result != null) {
                    if (result[0] == result[1])
                        summaryResult[0]++;

                    summaryResult[1]++;
                }
            }
        }

        if (!rows.isEmpty()) {
            Table.Cell<Text> firstCell = rows.get(0).getCells().get(0);
            firstCell.setContent(new Text("<<COMPOUND>>"));
        } else {
            rows.add(new Table.Row("<<COMPOUND>>",
                    SpecialCharacter.EMPTY,
                    SpecialCharacter.EMPTY,
                    SpecialCharacter.EMPTY,
                    SpecialCharacter.EMPTY
            ));
        }

        Table.Row endRow = generateEndRow(SpecialCharacter.EMPTY, String.valueOf(testCaseNames.size()), summaryResult);
        rows.add(endRow);

        return rows;
    }

    private Table.Row generateEndRow(String subprograms, String testCases, int[] summaryResult) {
        Table.Row endRow = new Table.Row(new Text("TOTAL", TEXT_STYLE.BOLD),
                new Text(subprograms, TEXT_STYLE.BOLD),
                new Text(testCases, TEXT_STYLE.BOLD),
                new Text(SpecialCharacter.EMPTY)
        );

        Table.Cell<Text> statusCell = generateStatusCell(summaryResult);
        statusCell.getContent().setStyle(TEXT_STYLE.BOLD);
        endRow.getCells().add(statusCell);

        return endRow;
    }

    private Table.Row generateRow(ITestCase testCase, INode unit, INode subprogram,
                                  boolean firstSubprogram, boolean firstTestCase) {
        String unitCol = SpecialCharacter.EMPTY;
        if (firstSubprogram)
            unitCol = unit.getName();

        String subprogramCol = SpecialCharacter.EMPTY;
        if (firstTestCase)
            subprogramCol = subprogram.getName();

        String testCaseCol = testCase.getName();

        String execDateTimeCol = testCase.getExecutionDate() + " " + testCase.getExecutionTime();

        Table.Row row = new Table.Row(unitCol, subprogramCol, testCaseCol, execDateTimeCol);

        int[] result = testCase.getExecutionResult();
        row.getCells().add(generateStatusCell(result));

        return row;
    }

    private Table.Cell<Text> generateStatusCell(int[] result) {
        String statusCol = SpecialCharacter.EMPTY;

        String bgColor;
        if (result == null || result[1] == 0)
            bgColor = COLOR.LIGHT;
        else {
            statusCol = String.format("PASS %d/%d", result[0], result[1]);

            if (result[0] == result[1])
                bgColor = COLOR.GREEN;
            else if (result[0] == 0)
                bgColor = COLOR.RED;
            else
                bgColor = COLOR.YELLOW;
        }

        return new Table.Cell<>(statusCol, bgColor);
    }
}

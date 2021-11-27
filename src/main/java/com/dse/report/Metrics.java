package com.dse.report;

import com.dse.coverage.AbstractCoverageManager;
import com.dse.coverage.CoverageDataObject;
import com.dse.coverage.TestPathUtils;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.StructureNode;
import com.dse.report.element.Section;
import com.dse.report.element.Table;
import com.dse.report.element.Text;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.testcase_manager.AbstractTestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Metrics extends Section {
    private List<INode> units;

    public Metrics(List<INode> units) {
        super("metrics");
        this.units = units;
        generate();
    }

    private void generate() {
        title.add(new Line("Metrics", COLOR.DARK));
        title.add(new Line(Environment.getInstance().getTypeofCoverage(), COLOR.MEDIUM));

        Table table = new Table(false);
        Table.HeaderRow headerRow = new Table.HeaderRow("Unit", "Subprogram");
        String[] types = Environment.getInstance().getTypeofCoverage().split("\\+");
        Arrays.stream(types)
                .forEach(text -> {
                    char[] title = text.toLowerCase().toCharArray();
                    title[0] = Character.toUpperCase(title[0]);
                    Table.Cell<Text> cell = new Table.Cell<Text>(new String(title) +  " (File coverage)", COLOR.MEDIUM);

                    Table.Cell<Text> funcCell = new Table.Cell<Text>(new String(title) + " (Function coverage)", COLOR.MEDIUM);
                    headerRow.getCells().add(cell);
                    headerRow.getCells().add(funcCell);
                });

        table.getRows().add(headerRow);
        for (INode unit : units) {
            table.getRows().addAll(generateUnitRow(unit));

            if (units.indexOf(unit) != units.size() - 1)
                table.getRows().add(new Table.Row(new Table.SpanCell<Text>(SpecialCharacter.EMPTY, headerRow.getCells().size())));
        }

        body.add(table);
    }

    private String getSubprogramDisplayName(ICommonFunctionNode subprogram) {
        String tmpName = AbstractTestCase.removeSysPathInName(subprogram.getAbsolutePath());
        String simpleName = (new File(tmpName)).getName();

        String[] pathElements= null;
        if (Utils.isWindows())
            pathElements = tmpName.split("\\\\");
        else if (Utils.isUnix()|| Utils.isMac())
            pathElements = tmpName.split(File.separator);


        for (int i = pathElements.length - 2; i >= 0; i--)
            if (!pathElements[i].contains("."))
                simpleName = pathElements[i] + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + simpleName;
            else
                break;

        simpleName = AbstractTestCase.redoTheReplacementOfSysPathInName(simpleName);
        return simpleName;
    }

    private List<Table.Row> generateUnitRow(INode unit) {
        List<Table.Row> rows = new ArrayList<>();

        List<IFunctionNode> subprograms = Search
                .searchNodes(unit, new AbstractFunctionNodeCondition())
                .stream().map(f -> (IFunctionNode) f)
                .collect(Collectors.toList());

        if (!Environment.getInstance().isOnWhiteBoxMode() )
            subprograms.removeIf(sub -> sub.getVisibility() != ICPPASTVisibilityLabel.v_public);

        subprograms.removeIf(sub -> sub.isTemplate() && sub.getParent() instanceof ICommonFunctionNode);
        subprograms.removeIf(sub -> sub instanceof StructureNode.DefaultConstructor);

        String typeOfCoverage = Environment.getInstance().getTypeofCoverage();

        String[] typeItems = typeOfCoverage.split("\\+");

        if (subprograms.isEmpty()) {
            Table.Row row = new Table.Row();

            Table.Cell<Text> cell = new Table.Cell<Text>(unit.getName());
            row.getCells().add(cell);

            Table.SpanCell<Text> spanCell = new Table.SpanCell<>(SpecialCharacter.EMPTY, 1 + typeItems.length * 2);
            row.getCells().add(spanCell);

            rows.add(row);
        }

        List<TestCase> allTestCases = new ArrayList<>();

        for (IFunctionNode subprogram : subprograms) {
            // get unit name column
            String unitCol = SpecialCharacter.EMPTY;
            if (subprograms.indexOf(subprogram) == 0)
                unitCol = unit.getName();

            // get subprogram name column
            String subprogramCol = getSubprogramDisplayName(subprogram);

            Table.Row row = new Table.Row(unitCol, subprogramCol);

            List<TestCase> testCases = getAllTestCaseOf(subprogram);
            allTestCases.addAll(testCases);

            switch (typeOfCoverage) {
                case EnviroCoverageTypeNode.BRANCH:
                case EnviroCoverageTypeNode.STATEMENT:
                case EnviroCoverageTypeNode.MCDC:
                case EnviroCoverageTypeNode.BASIS_PATH: {
                    Table.Cell<Text> coverageCell, functionCoverageCell;

                    if (!testCases.isEmpty()) {
                        CoverageDataObject srcCoverageData = AbstractCoverageManager
                                .getCoverageOfMultiTestCaseAtSourcecodeFileLevel(testCases, typeOfCoverage);

                        if (srcCoverageData != null) {
                            int visited = srcCoverageData.getVisited();
                            int total = srcCoverageData.getTotal();
                            coverageCell = generateCoverageCell(visited, total);
                        } else {
                            coverageCell = generateCoverageCell(0, 0);
                        }

                        CoverageDataObject funcCoverageData = AbstractCoverageManager
                                .getCoverageOfMultiTestCaseAtFunctionLevel(testCases, typeOfCoverage);
                        if (funcCoverageData != null) {
                            functionCoverageCell = generateCoverageCell(funcCoverageData.getVisited(), funcCoverageData.getTotal());
                        } else {
                            functionCoverageCell = generateCoverageCell(0, 0);
                        }

                    } else {
                        coverageCell = new Table.Cell<>(SpecialCharacter.EMPTY);
                        functionCoverageCell = new Table.Cell<>(SpecialCharacter.EMPTY);
                    }

                    row.getCells().add(coverageCell);
                    row.getCells().add(functionCoverageCell);

                    break;
                }

                case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH:
                case EnviroCoverageTypeNode.STATEMENT_AND_MCDC: {
                    Table.Cell<Text>[] coverageCells = new Table.Cell[4];

                    if (!testCases.isEmpty()) {
                        for (int i = 0; i < typeItems.length; i++) {
                            String coverageType = typeItems[i];

                            int srcTotal = 0, srcVisited = 0, funcVisited = 0, funcTotal = 0;

                            CoverageDataObject srcCoverageData = AbstractCoverageManager
                                    .getCoverageOfMultiTestCaseAtSourcecodeFileLevel(testCases, coverageType);
                            if (srcCoverageData != null) {
                                srcVisited = srcCoverageData.getVisited();
                                srcTotal = srcCoverageData.getTotal();
                            }

                            coverageCells[i*2] = generateCoverageCell(srcVisited, srcTotal);

                            CoverageDataObject funcCoverageData = AbstractCoverageManager
                                    .getCoverageOfMultiTestCaseAtFunctionLevel(testCases, coverageType);
                            if (funcCoverageData != null) {
                                funcVisited = funcCoverageData.getVisited();
                                funcTotal = funcCoverageData.getTotal();
                            }
                            coverageCells[i*2+1] = generateCoverageCell(funcVisited, funcTotal);
                        }

                    } else {
                        for (int i = 0; i < coverageCells.length; i++)
                            coverageCells[i] = new Table.Cell<>(SpecialCharacter.EMPTY);
                    }

                    row.getCells().addAll(Arrays.asList(coverageCells));

                    break;
                }
            }

            rows.add(row);
        }

        Table.Row totalRow = generateEndRow(subprograms.size(), allTestCases, typeItems);

        rows.add(totalRow);

        return rows;
    }

    private String getBackgroundColor(int visited, int total) {
        String bgColor;
        if (visited == total && visited != 0)
            bgColor = COLOR.GREEN;
        else if (visited != 0)
            bgColor = COLOR.YELLOW;
        else
            bgColor = COLOR.RED;

        return bgColor;
    }

    private Table.Row generateEndRow(int subprograms, List<TestCase> testCases, String[] coverageType) {
        Table.Row endRow = new Table.Row(
                new Text("TOTAL", TEXT_STYLE.BOLD),
                new Text(String.valueOf(subprograms), TEXT_STYLE.BOLD)
        );

        for (String type : coverageType) {
            CoverageDataObject object = AbstractCoverageManager
                    .getCoverageOfMultiTestCaseAtSourcecodeFileLevel(testCases, type);

            if (object != null) {
                Table.Cell<Text> statusCell = generateCoverageCell(object.getVisited(), object.getTotal());
                endRow.getCells().add(statusCell);
            } else {
                endRow.getCells().add(new Table.Cell<Text>(""));
            }
            endRow.getCells().add(new Table.Cell<Text>(""));

        }

        return endRow;
    }

    private Table.Cell<Text> generateCoverageCell(int visited, int total) {
        String coverage = String.format("%.2f%% (%d/%d)", (double) visited * 100 / (double) total, visited, total);
        String bgColor = getBackgroundColor(visited, total);

        return new Table.Cell<>(coverage, bgColor);
    }

    private List<TestCase> getAllTestCaseOf(IFunctionNode function) {
        List<String> testCaseNames = TestCaseManager.getFunctionToTestCasesMap().get(function);
        List<TestCase> testCases = new ArrayList<>();

        if (testCaseNames != null)
            for (String name : testCaseNames) {
                TestCase testCase = TestCaseManager.getBasicTestCaseByName(name);
                testCases.add(testCase);
            }

        return testCases;
    }
}

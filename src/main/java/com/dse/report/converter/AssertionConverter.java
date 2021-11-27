package com.dse.report.converter;

import com.dse.gtest.Failure;
import com.dse.parser.object.INode;
import com.dse.report.element.IElement;
import com.dse.report.element.Section;
import com.dse.report.element.Table;
import com.dse.report.element.Text;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;

import java.util.Arrays;
import java.util.List;

public abstract class AssertionConverter extends Converter {
    /**
     * List of failures from gtest report
     */
    protected List<Failure> failures;

    /**
     * Function call tag: index of function call expresstion
     */
    protected int fcalls;

    /**
     * Result PASS/ALL
     */
    protected int[] results = new int[] {0, 0};

    public AssertionConverter(List<Failure> failures, int fcalls) {
        this.failures = failures;
        this.fcalls = fcalls;
    }

    public Table execute(SubprogramNode root) {
        Table table = generateHeaderRows(root);

        for (IDataNode child : root.getChildren())
            table = recursiveConvert(table, child, 2);

        return table;
    }

    protected Table generateHeaderRows(SubprogramNode root) {
        Table table = new Table(false);

        table.getRows().add(new Table.Row("Parameter", "Type", "Expected Value", "Actual Value"));

        INode sourceNode = Utils.getSourcecodeFile(root.getFunctionNode());
        String unitName = sourceNode.getName();
        Table.Row uutRow = new Table.Row("Calling UUT: " + unitName, "", "", "");
        table.getRows().add(uutRow);
        Table.Row sutRow = new Table.Row(generateTab(1) + "Subprogram: " + root.getName(), "", "", "");
        table.getRows().add(sutRow);

        return table;
    }

    @Override
    public boolean isShowInReport(IDataNode node) {
        return node instanceof ValueDataNode;
    }

    @Override
    public Table.Row convertSingleNode(IDataNode node, int level) {
        String key = generateTab(level) + node.getDisplayNameInParameterTree();

        String type = "";
        if (node instanceof ValueDataNode && !(node instanceof SubprogramNode))
            type = ((ValueDataNode) node).getType();

        Table.Row row = new Table.Row(key, type);

        Table.Cell[] valueCells = new Table.Cell[] {new Table.Cell<>(""), new Table.Cell<>("")};
        if (node instanceof ValueDataNode && isValuable(node)) {
            ValueDataNode dataNode = (ValueDataNode) node;

            if (dataNode.isExpected())
                valueCells = findValuesExpectCase(dataNode, node.getVituralName());
            else
                valueCells = findValuesActualCase(dataNode);
        }

        row.getCells().addAll(Arrays.asList(valueCells));

        return row;
    }

    protected Table.Cell[] findValuesActualCase(ValueDataNode node) {
        String actualValue = getNodeValue(node);
        String expectedValue = SpecialCharacter.EMPTY;

        return new Table.Cell[] {
                new Table.Cell<>(expectedValue),
                new Table.Cell<>(actualValue)
        };
    }

    protected Table.Cell[] findValuesExpectCase(ValueDataNode node, String expectedName) {
        String expectedValue = getNodeValue(node);
        String actualValue = MATCH;

        if (expectedValue == null || expectedValue.equals("<<null>>")) {
            return new Table.Cell[]{
                    new Table.Cell<>(SpecialCharacter.EMPTY),
                    new Table.Cell<>(SpecialCharacter.EMPTY)
            };
        }

        if (failures != null) {
//            String expectedName = node.getVituralName();

            if (node instanceof ListBaseDataNode)
                expectedName += ".size()";

            for (Failure failure : failures) {
                String fcallTag = "Aka function calls: " + fcalls;

                if (failure.getMessage().contains(expectedName)
                        && failure.getMessage().contains(fcallTag)) {

                    String[] values = findValuesFromFailure(failure);
                    actualValue = values[1];
                    expectedValue = values[0];
                    break;
                }
            }
        }

        results[1]++;

        String bgColor = IElement.COLOR.RED;
        if (actualValue.equals(MATCH)) {
            bgColor = IElement.COLOR.GREEN;
            results[0]++;
        }

        return new Table.Cell[] {
                new Table.Cell<>(expectedValue, bgColor),
                new Table.Cell<>(actualValue, bgColor)
        };
    }

    protected String[] findValuesFromFailure(Failure failure) {
        String[] values = new String[] {PROBLEM_VALUE, PROBLEM_VALUE};

        String[] lines = failure.getMessageLines();
        for (int i = 0; i < lines.length; i++) {
            String fcallTag = "Aka function calls: " + fcalls;

            if (lines[i].trim().equals(fcallTag)) {
                /*
                 * Example:
                 *
                 *       Expected: AKA_STUB_r
                 *       Which is: 4            [i - 3] expected
                 * To be equal to: r
                 *       Which is: 3            [i - 1] actual
                 * Aka function calls: 2        [i]     fcalls tag
                 *
                 */
                values[0] = lines[i - 3].substring(OFFSET);
                values[1] = lines[i - 1].substring(OFFSET);
                return values;
            }
        }

        return values;
    }

    protected boolean isValuable(IDataNode node) {
        if (node instanceof RootDataNode || node instanceof SubprogramNode)
            return false;
        else if (node instanceof StructDataNode || node instanceof ClassDataNode || node instanceof UnionDataNode)
            return false;
        else if (node instanceof ArrayDataNode || node instanceof PointerDataNode) {
            String value = getNodeValue(node);
            return value != null && !value.isEmpty();
        } else if (node instanceof NormalDataNode || node instanceof EnumDataNode)
            return true;
        else
            return node instanceof ListBaseDataNode || node instanceof UnresolvedDataNode;
    }

    public int[] getResults() {
        return results;
    }

    protected static final int OFFSET = 10;

    protected static final String PROBLEM_VALUE = "Can't find variable value";

    protected static final String MATCH = "MATCH";
}

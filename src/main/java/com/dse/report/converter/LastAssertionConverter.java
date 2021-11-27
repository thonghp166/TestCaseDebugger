package com.dse.report.converter;

import com.dse.gtest.Failure;
import com.dse.parser.object.INode;
import com.dse.parser.object.ReturnVariableNode;
import com.dse.parser.object.VariableNode;
import com.dse.report.element.Table;
import com.dse.report.element.Text;
import com.dse.search.Search2;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LastAssertionConverter extends AssertionConverter {
    private SubprogramNode sut;

    private Map<ValueDataNode, ValueDataNode> expectedGlobalMap;

    public LastAssertionConverter(List<Failure> failures, int fcalls) {
        super(failures, fcalls);
    }

    @Override
    public Table execute(SubprogramNode root) {
        this.sut = root;

        Table table = new Table(false);

        table.getRows().add(new Table.Row("Parameter", "Type", "Expected Value", "Actual Value"));

        INode sourceNode = Utils.getSourcecodeFile(root.getFunctionNode());
        String unitName = sourceNode.getName();
        Table.Row uutRow = new Table.Row("Return from UUT: " + unitName, "", "", "");
        table.getRows().add(uutRow);

        RootDataNode globalRoot = Search2.findGlobalRoot(root.getTestCaseRoot());
        if (globalRoot != null) {
            expectedGlobalMap = globalRoot.getGlobalInputExpOutputMap();

            for (IDataNode child : expectedGlobalMap.keySet()) {
                String nodeValue = getNodeValue(child);
                if (nodeValue != null && !nodeValue.equals("<<null>>"))
                    table = recursiveConvert(table, child, 1);
            }
        }
        Table.Row sutRow = new Table.Row(generateTab(1) + "Subprogram: " + root.getName(), "", "", "");
        table.getRows().add(sutRow);

        for (IDataNode child : root.getChildren())
            table = recursiveConvert(table, child, 2);

        return table;
    }



//    @Override
//    public boolean isShowInReport(IDataNode node) {
//        return super.isShowInReport(node) && (node != sut && !(node instanceof ConstructorDataNode));
//    }

    @Override
    public Table recursiveConvert(Table table, IDataNode node, int level) {
//        if (node instanceof SubClassDataNode) {
//            SubClassDataNode subClass = (SubClassDataNode) node;
//            ValueDataNode expectedSubClass = Search2.getExpectedValue(subClass);
//
//            if (expectedSubClass != null)
//                table = super.recursiveConvert(table, expectedSubClass, level);
//        }

        table = super.recursiveConvert(table, node, level);

        if (node instanceof PointerDataNode || node instanceof ArrayDataNode || node instanceof ListBaseDataNode) {
            ValueDataNode expectedNode = Search2.getExpectedValue((ValueDataNode) node);

            if (expectedNode != null) {
                for (IDataNode expectedChild : expectedNode.getChildren()) {
                    String expectedName = expectedChild.getName();
                    String expectedValue = getNodeValue(expectedChild);

                    if (expectedValue == null || expectedValue.equals("<<null>>"))
                        continue;

                    boolean found = false;

                    for (IDataNode actualChild : node.getChildren()) {
                        if (actualChild.getName().equals(expectedName)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        table = super.recursiveConvert(table, expectedChild, level + 1);
                    }
                }
            }
        }

        return table;
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
                valueCells = findValuesExpectCase(dataNode, dataNode.getVituralName());
            else if (dataNode.isSutExpectedValue() || expectedGlobalMap.containsKey(dataNode)) {
//                String actualValue = SpecialCharacter.EMPTY;
//                String expectedValue = getNodeValue(node);
//
//                valueCells = new Table.Cell[] {
//                        new Table.Cell<>(expectedValue),
//                        new Table.Cell<>(actualValue)
//                };
                String expectedName = dataNode.getVituralName();
                valueCells = findValuesExpectCase(dataNode, expectedName);
            }
            else
                valueCells = findParamValue(dataNode);
        }

        row.getCells().addAll(Arrays.asList(valueCells));

        return row;
    }

    private Table.Cell[] findParamValue(ValueDataNode dataNode) {

        Map<ValueDataNode, ValueDataNode> map = sut.getInputToExpectedOutputMap();

        Table.Cell[] valueCells = findValuesActualCase(dataNode);


//        if (map.containsKey(dataNode)) {
        ValueDataNode expectedNode = Search2.getExpectedValue(dataNode);
//            ValueDataNode expectedNode = map.get(dataNode);
            if (expectedNode != null) {
                String expectedValue = getNodeValue(expectedNode);
                if (expectedValue != null && !expectedValue.equals("<<null>>")) {
//                String expectedName = IGTestConstant.EXPECTED_PREFIX + dataNode.getVituralName();
                    String expectedName = expectedNode.getVituralName();
                    valueCells = findValuesExpectCase(dataNode, expectedName);
                }
            }
//        }

        return valueCells;
    }
}

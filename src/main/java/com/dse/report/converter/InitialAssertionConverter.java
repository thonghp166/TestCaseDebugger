package com.dse.report.converter;

import com.dse.gtest.Failure;
import com.dse.parser.object.ReturnVariableNode;
import com.dse.parser.object.VariableNode;
import com.dse.report.element.Table;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.util.NodeType;

import java.util.List;

public class InitialAssertionConverter extends AssertionConverter {

    public InitialAssertionConverter(List<Failure> failures, int fcalls) {
        super(failures, fcalls);
    }

    @Override
    public Table recursiveConvert(Table table, IDataNode node, int level) {
        if (!(node instanceof RootDataNode && ((RootDataNode) node).getLevel() == NodeType.ROOT))
            table.getRows().add(convertSingleNode(node, level++));

        if (!(node instanceof ValueDataNode && ((ValueDataNode) node).isExpected())) {
            for (IDataNode child : node.getChildren())
                if (isShowInReport(child))
                    table = recursiveConvert(table, child, level);

        }

        return table;
    }

    @Override
    public Table.Row convertSingleNode(IDataNode node, int level) {
        String key = generateTab(level) + node.getDisplayNameInParameterTree();
        String type = null;
        String expectedValue = null, actualValue = null;

        if (node instanceof ValueDataNode && !(node instanceof SubprogramNode)) {
            type = ((ValueDataNode) node).getType();
            VariableNode correspondingVar = ((ValueDataNode) node).getCorrespondingVar();

            if (((ValueDataNode) node).isExpected()) {
//                expectedValue = getNodeValue(node);
            } else {
                actualValue = getNodeValue(node);
            }
        }

        return new Table.Row(key, type, expectedValue, actualValue);
    }
}

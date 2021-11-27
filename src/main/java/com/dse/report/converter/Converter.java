package com.dse.report.converter;

import com.dse.report.element.Table;
import com.dse.testdata.object.*;
import com.dse.util.NodeType;

public abstract class Converter implements IHtmlConverter {
    @Override
    public Table recursiveConvert(Table table, IDataNode node, int level) {
        if (!(node instanceof RootDataNode && ((RootDataNode) node).getLevel() == NodeType.ROOT))
            table.getRows().add(convertSingleNode(node, level++));

        for (IDataNode child : node.getChildren())
            if (isShowInReport(child))
                table = recursiveConvert(table, child, level);

        return table;
    }



    protected String getNodeValue(IDataNode node) {
        String value = null;

        if (node instanceof NormalDataNode) {
            value = ((NormalDataNode) node).getValue();
            if (value == null || value.isEmpty())
                value = "<<null>>";
        } else if (node instanceof EnumDataNode)
            value = ((EnumDataNode) node).getValue();
        else if (node instanceof OneDimensionDataNode) {
            if (((OneDimensionDataNode) node).getSize() <= 0)
                value = null;
        } else if (node instanceof MultipleDimensionDataNode) {
            if (((MultipleDimensionDataNode) node).getSizeOfDimension(0) <= 0)
                value = null;
        } else if (node instanceof PointerDataNode) {
            if (((PointerDataNode) node).getAllocatedSize() <= 0)
                value = null;
        } else if (node instanceof UnresolvedDataNode)
            value = "<<User Code>>";

        return value;
    }

    public static String generateTab(int level) {
        String tab = "";

        for (int i = 0; i < level; i++)
            tab += "&emsp;";

        return tab;
    }

    protected abstract boolean isShowInReport(IDataNode node);
}

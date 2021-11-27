package com.dse.guifx_v3.objects;

import com.dse.testdata.object.DataNode;
import com.dse.testdata.object.SubprogramNode;
import com.dse.testdata.object.ValueDataNode;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;

public class TestDataParameterTreeItem extends TestDataTreeItem {
    private DataNode inputDataNode;
    private DataNode expectedOutputDataNode;
    private List<TreeItem<DataNode>> inputChildren = new ArrayList<>();
    private List<TreeItem<DataNode>> expectedOutputChildren = new ArrayList<>();

    private ColumnType selectedColumn = ColumnType.INPUT;

    public TestDataParameterTreeItem(DataNode dataNode) {
        super(dataNode);
        setColumnType(ColumnType.ALL);

        if (dataNode.getParent() instanceof SubprogramNode) {
            SubprogramNode parent = (SubprogramNode) dataNode.getParent();
            this.inputDataNode = dataNode;
            this.expectedOutputDataNode = parent.getExpectedOuput((ValueDataNode) dataNode);
        }
    }

    public ColumnType getSelectedColumn() {
        return selectedColumn;
    }

    public void setSelectedColumn(ColumnType selectedColumn) {
        if (selectedColumn != this.selectedColumn) {
            if (this.selectedColumn == ColumnType.INPUT) {
                // save children
                inputChildren.clear();
                inputChildren.addAll(getChildren());

                // switch value and children
                setValue(expectedOutputDataNode);
                getChildren().clear();
                getChildren().addAll(expectedOutputChildren);
            } else if (this.selectedColumn == ColumnType.EXPECTED) {
                // save children
                expectedOutputChildren.clear();
                expectedOutputChildren.addAll(getChildren());

                // switch value and children
                setValue(inputDataNode);
                getChildren().clear();
                getChildren().addAll(inputChildren);
            }

            this.selectedColumn = selectedColumn;
        }
    }

    public DataNode getInputDataNode() {
        return inputDataNode;
    }

    public DataNode getExpectedOutputDataNode() {
        return expectedOutputDataNode;
    }
}

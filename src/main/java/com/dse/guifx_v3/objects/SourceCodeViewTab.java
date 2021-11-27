package com.dse.guifx_v3.objects;

import com.dse.parser.object.INode;
import javafx.scene.control.Tab;

public class SourceCodeViewTab extends Tab {
    private INode sourceCodeFileNode;

    public SourceCodeViewTab(INode sourceCodeFileNode) {
        this.sourceCodeFileNode = sourceCodeFileNode;
    }

    public INode getSourceCodeFileNode() {
        return sourceCodeFileNode;
    }
}

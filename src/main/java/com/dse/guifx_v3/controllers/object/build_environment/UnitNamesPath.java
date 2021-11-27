package com.dse.guifx_v3.controllers.object.build_environment;

import com.dse.parser.object.SourcecodeFileNode;

public class UnitNamesPath {
    private String absolutePath;
    private String name;
    private boolean choosed = false;
    private SourcecodeFileNode sourcecodeFileNode;

    public UnitNamesPath(SourcecodeFileNode sourcecodeFileNode) {
        this.sourcecodeFileNode = sourcecodeFileNode;
        this.absolutePath = sourcecodeFileNode.getAbsolutePath();
        this.name = sourcecodeFileNode.getName();
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public boolean isChoosed() {
        return choosed;
    }

    public void setChoosed(boolean choosed) {
        this.choosed = choosed;
    }

    public SourcecodeFileNode getSourcecodeFileNode() {
        return sourcecodeFileNode;
    }

    public void setSourcecodeFileNode(SourcecodeFileNode sourcecodeFileNode) {
        this.sourcecodeFileNode = sourcecodeFileNode;
    }
}

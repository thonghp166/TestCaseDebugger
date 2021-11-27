package com.dse.guifx_v3.controllers.object.unit_node;

import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.IncludeHeaderDependency;
import com.dse.parser.object.INode;
import com.dse.parser.object.SourcecodeFileNode;
import com.dse.util.AkaLogger;

public class DependencyNode extends AbstractUnitNode {
    final static AkaLogger logger = AkaLogger.get(DependencyNode.class);

    private SourcecodeFileNode sourcecodeFileNode;
    private String type;
    private AbstractUnitNode parent;
    private boolean canSetStubType = true;

    public DependencyNode() {}

    public DependencyNode(SourcecodeFileNode sourcecodeFileNode) {
        this.sourcecodeFileNode = sourcecodeFileNode;
    }


    public void loadDependencies(SourcecodeFileNode sourcecodeFileNode){
        // add dependencies
        for (Dependency child : sourcecodeFileNode.getDependencies())
            if (child instanceof IncludeHeaderDependency) {
                // Get dependent header files
                IncludeHeaderDependency cast = (IncludeHeaderDependency) child;

                if (cast.getStartArrow().equals(sourcecodeFileNode)) {
                    INode dependentHeaderFile = cast.getEndArrow();

                    if (dependentHeaderFile instanceof SourcecodeFileNode) {
                        DependencyNode newNode = new DependencyNode((SourcecodeFileNode) dependentHeaderFile);
                        newNode.loadDependencies(newNode.getSourcecodeFileNode());
                        logger.debug("Found a depdency:" + child.getStartArrow().getAbsolutePath() + "->" + child.getEndArrow().getAbsolutePath());
                        getChildren().add(newNode);
                    }
                    // TODO: get source code file corresponding to header files
                }
            }

        // set the children node can be set stub type
        //setChildrenCanSetStubType();
    }
    public String getName() {
        return sourcecodeFileNode.getName();
    }

    public SourcecodeFileNode getSourcecodeFileNode() {
        return sourcecodeFileNode;
    }

    public boolean isCanSetStubType() {
        return canSetStubType;
    }

    public void setCanSetStubType(boolean canSetStubType) {
        this.canSetStubType = canSetStubType;
        if (!canSetStubType) {
            setType(null);
        }
    }

    public void setChildrenCanSetStubType() {
        if (type.equals(DependencyNode.DONT_STUB)) {
            for (AbstractUnitNode node : getChildren()) {
                ((DependencyNode) node).setCanSetStubType(true);
                ((DependencyNode) node).setChildrenCanSetStubType();
            }
        } else {
            for (AbstractUnitNode node : getChildren()) {
                ((DependencyNode) node).setCanSetStubType(false);
                ((DependencyNode) node).setChildrenCanSetStubType();
            }
        }
    }

    public String getType() {
        if (!canSetStubType) return null;
        return type;
    }

    public void setType(String type) {
        this.type = type;
        //setChildrenCanSetStubType();
    }

    public void makeAllChildrenDontStub() {
        for (AbstractUnitNode node : getChildren()) {
            ((DependencyNode) node).setType(DependencyNode.DONT_STUB);
        }
    }

    public final static String DONT_STUB = "DONT_STUB";
    public final static String STUB_BY_IMPLEMENTATION = "STUB_BY_IMPLEMENTATION";
    public final static String STUB_BY_PROTOTYPE = "STUB_BY_PROTOTYPE";

}

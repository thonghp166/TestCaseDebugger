package com.dse.guifx_v3.helps;

import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.TypeDependency;
import com.dse.parser.object.INode;
import com.dse.parser.object.VariableNode;
import com.dse.search.Search;
import com.dse.search.condition.StructurevsTypedefCondition;
import com.dse.util.AkaLogger;

import java.util.List;

public class ResolveCoreTypeHelper {

    private final static AkaLogger logger = AkaLogger.get(Environment.class);

    public static INode resolve(String vPath) {
        List<Dependency> dependencies = Environment.getInstance().getDependencies();
        for (Dependency dependency : dependencies) {
            if (dependency instanceof TypeDependency) {
                if (dependency.getStartArrow().getAbsolutePath().equals(vPath)) {
                    return dependency.getEndArrow();
                }
            }
        }

        logger.error("Can not resolve the path");
        return null;
    }

    public static INode resolve(VariableNode variableNode) {
        INode coreType = resolve(variableNode.getAbsolutePath());
        if (coreType == null) {
            logger.error("Can not resolve core type for the variableNode: " + variableNode.getAbsolutePath());
        }
        return coreType;
    }

    public static INode getType(String absolutePath) {
        // Search Level 1
        List<Dependency> dependencies = Environment.getInstance().getDependencies();
        for (Dependency dependency : dependencies) {
            if (dependency instanceof TypeDependency) {
                if (dependency.getEndArrow().getAbsolutePath().equals(absolutePath)) {
                    return dependency.getEndArrow();
                }
            }
        }

        // Search Level 2
        List<INode> structureNodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new StructurevsTypedefCondition());
        for (INode structureNode : structureNodes) {
            if (structureNode.getAbsolutePath().equals(absolutePath)) {
                return structureNode;
            }
        }

        return null;
    }
}

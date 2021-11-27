package com.dse.project_init;

import org.eclipse.cdt.core.dom.ast.IASTNode;

public interface IProjectCloneMap {
    IASTNode getClonedASTNode(IASTNode origin);

    int getStartingLine(IASTNode origin);
}

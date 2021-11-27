package com.dse.resolver;

import com.dse.parser.object.INode;

import java.util.List;

public interface IUndeclaredResolver {
    void resolve();

    List<ResolvedSolution> getSolutions();
}

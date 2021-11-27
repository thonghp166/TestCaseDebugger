package com.dse.search.condition;

import com.dse.parser.object.INode;
//import com.dse.parser.object.PrimitiveTypedefDeclaration;
import com.dse.parser.object.StructTypedefNode;
import com.dse.parser.object.TypedefDeclaration;
import com.dse.search.SearchCondition;

public class TypedefNodeCondifion extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof TypedefDeclaration;
    }
}

package com.dse.testdata.gen.module.type;

import com.dse.guifx_v3.helps.ResolveCoreTypeHelper;
import com.dse.parser.object.*;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.AllocatorDataNode;
import com.dse.testdata.object.stl.DefaultDeleteDataNode;
import com.dse.util.AkaLogger;
import com.dse.util.VariableTypeUtils;
import com.dse.util.VariableTypeUtilsForStd;

/**
 * Khoi tao bien truyen vao la kieu structure
 */
public class StructureTypeInitiation extends AbstractTypeInitiation {
    final static AkaLogger logger = AkaLogger.get(StructureTypeInitiation.class);

    public StructureTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        INode correspondingNode = vParent.resolveCoreType();
        if (correspondingNode == null)
            correspondingNode = ResolveCoreTypeHelper.resolve(vParent);

        if (correspondingNode == null && VariableTypeUtils.isStdInt(vParent.getRealType()))
            return new BasicTypeInitiation(vParent, nParent).execute();

        ValueDataNode child = null;

        String type = vParent.getRawType();

        if (correspondingNode instanceof StructNode)
            child = new StructDataNode();
        else if (correspondingNode instanceof ClassNode)
            child = new ClassDataNode();
        else if (correspondingNode instanceof UnionNode)
            child = new UnionDataNode();
        else if (correspondingNode instanceof EnumNode)
            return new EnumTypeInitiation(vParent, nParent).execute();
        else if (VariableTypeUtils.isNullPtr(type)) {
            child = new NullPointerDataNode();
        } else if (correspondingNode instanceof AvailableTypeNode) {
            return new BasicTypeInitiation(vParent, nParent).execute();
        } else if (correspondingNode instanceof STLTypeNode) {
            return new STLTypeInitiation(vParent, nParent).execute();
        } else  if (VariableTypeUtils.isVoidPointer(type)){
            child = new VoidPointerDataNode();
        } else if (VariableTypeUtils.isVoid(type)) {
            child = new VoidDataNode();
        } else{
            child = new OtherUnresolvedDataNode();
        }

        if (child  != null) {
            child.setParent(nParent);
            child.setName(vParent.getNewType());
            child.setType(VariableTypeUtils.getFullRawType(vParent));
//        child.setType(vParent.getFullType());
            child.setCorrespondingVar(vParent);

            if (correspondingNode instanceof StructNode) {
                (new TreeExpander()).expandTree(child);
            }

            if (vParent instanceof ExternalVariableNode)
                child.setExternel(true);

            nParent.addChild(child);
        }
        return child;
    }

}

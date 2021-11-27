package com.dse.testdata.gen.module.type;

import com.dse.parser.object.ExternalVariableNode;
import com.dse.parser.object.VariableNode;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.testdata.object.*;
import com.dse.util.IRegex;
import com.dse.util.VariableTypeUtils;

/**
 * Khoi tao bien dau vao la mang mot chieu
 */
public class OneDimensionTypeInitiation extends AbstractTypeInitiation {
    public OneDimensionTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        String coreType = VariableTypeUtils.getSimpleRawType(vParent)
                .replaceAll(IRegex.ARRAY_INDEX, "");

        ValueDataNode child;
        if (VariableTypeUtils.isPointer(coreType))
            child = new OneDimensionPointerDataNode();
        else if (VariableTypeUtils.isCh(coreType))
            child = new OneDimensionCharacterDataNode();
        else if (VariableTypeUtils.isNum(coreType))
            child = new OneDimensionNumberDataNode();
        else if (VariableTypeUtils.isStr(coreType))
            child = new OneDimensionStringDataNode();
        else
            child = new OneDimensionStructureDataNode();

        child.setParent(nParent);
        child.setType(VariableTypeUtils.getFullRawType(vParent));
        child.setName(vParent.getNewType());
        child.setCorrespondingVar(vParent);
        setSizeOf(child);

        if (vParent instanceof ExternalVariableNode)
            child.setExternel(true);

        nParent.addChild(child);
        return  child;
    }

    /**
     * Set size of the One Dimension Data Node
     *
     * @param node
     * @throws Exception
     */
    public void setSizeOf(ValueDataNode node) throws Exception {
        String type = node.getType();
        String sizeInString = type.substring(type.lastIndexOf('[') + 1, type.lastIndexOf(']'));
        int size = -1;

        if (!sizeInString.equals("")) {
            size = Integer.parseInt(sizeInString);
            ((OneDimensionDataNode) node).setFixedSize(true);
        }

        ((OneDimensionDataNode) node).setSize(size);

        (new TreeExpander()).expandTree(node);
    }

}

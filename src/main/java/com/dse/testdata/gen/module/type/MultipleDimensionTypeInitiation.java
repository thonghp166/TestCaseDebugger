package com.dse.testdata.gen.module.type;

import com.dse.parser.object.ExternalVariableNode;
import com.dse.parser.object.VariableNode;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.testdata.object.*;
import com.dse.util.IRegex;
import com.dse.util.TemplateUtils;
import com.dse.util.Utils;
import com.dse.util.VariableTypeUtils;

import java.util.List;
import java.util.regex.Pattern;


/**
 * Khoi tao bien dau vao la kieu mang 2 chieu
 */
public class MultipleDimensionTypeInitiation extends AbstractTypeInitiation {
    public MultipleDimensionTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        String coreType = VariableTypeUtils.getSimpleRawType(vParent)
//                .replaceAll(IRegex.POINTER, "")
                .replaceAll(IRegex.ARRAY_INDEX, "");

        ValueDataNode child;
        if (VariableTypeUtils.isPointer(coreType))
            child = new MultipleDimensionPointerDataNode();
        else if (VariableTypeUtils.isCh(coreType))
            child = new MultipleDimensionCharacterDataNode();
        else if (VariableTypeUtils.isNum(coreType))
            child = new MultipleDimensionNumberDataNode();
        else if (VariableTypeUtils.isStr(coreType))
            child = new MultipleDimensionStringDataNode();
        else
            child = new MultipleDimensionStructureDataNode();

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
     * Set size of the Two Dimension Data Node
     *
     * @param node
     * @throws Exception
     */
    private void setSizeOf(ValueDataNode node) throws Exception {
        String type = node.getType();

        // Remove template arguments
        type = TemplateUtils.deleteTemplateParameters(type);

        List<String> sizesInString = Utils.getIndexOfArray(VariableTypeUtils.deleteStorageClasses(type));

        int dimensions = sizesInString.size();

        int[] sizes = new int[dimensions];

        for (int i = 0; i < dimensions; i++) {
            if (isNumeric(sizesInString.get(i)))
                sizes[i] = Integer.parseInt(sizesInString.get(i));
            else
                sizes[i] = -1;

        }

        if (sizes[0] > 0) {
            ((MultipleDimensionDataNode) node).setFixedSize(true);
            ((MultipleDimensionDataNode) node).setSizeIsSet(true);
        }

        ((MultipleDimensionDataNode) node).setDimensions(dimensions);
        ((MultipleDimensionDataNode) node).setSizes(sizes);

        (new TreeExpander()).expandTree(node);
    }


    private boolean isNumeric(String strNum) {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

        if (strNum == null || strNum.length() == 0)
            return false;

        return pattern.matcher(strNum).matches();
    }
}

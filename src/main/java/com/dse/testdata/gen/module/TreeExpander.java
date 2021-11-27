package com.dse.testdata.gen.module;

import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.Search2;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testdata.gen.module.subtree.InitialArgTreeGen;
import com.dse.testdata.object.*;
import com.dse.util.Utils;
import com.dse.util.AkaLogger;

import java.io.File;

/**
 * @author ducanh
 */
public class TreeExpander extends AbstractDataTreeExpander {
    final static AkaLogger logger = AkaLogger.get(TreeExpander.class);

    private ICommonFunctionNode functionNode;

    public TreeExpander() {
    }

    public static void main(String[] args) throws Exception {
        /*
          Parse project
         */
        ProjectParser parser = new ProjectParser(new File(Utils.normalizePath(Paths.DATA_GEN_TEST)));
        parser.setCpptoHeaderDependencyGeneration_enabled(true);
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        parser.setParentReconstructor_enabled(true);
        parser.setGenerateSetterandGetter_enabled(true);
        parser.setFuncCallDependencyGeneration_enabled(true);
        parser.setGlobalVarDependencyGeneration_enabled(true);

        /*
          Get a function
         */
        String name = "test(int,int*,int[],int[2],char,char*,char[],char[10],SinhVien*,SinhVien,SinhVien[])";
        FunctionNode function = (FunctionNode) Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), name).get(0);
        logger.debug("function " + function.getAST().getRawSignature());

        /*
          create initial tree
         */
        RootDataNode root = new RootDataNode();
        root.setFunctionNode(function);
        InitialArgTreeGen dataTreeGen = new InitialArgTreeGen();
        dataTreeGen.generateCompleteTree(root, null);
        logger.debug("Initial tree:\n" + new SimpleTreeDisplayer().toString(root));

        /*
          Update value on tree
         */
        TreeExpander expander = new TreeExpander();
        expander.setFunctionNode(function);
        IDataNode subprogramNode = root.getChildren().get(0);
        // Find one dimensional variable
        IDataNode oneDimensionArrayNode = Search2.findNodeByName("i3", subprogramNode);
        if (oneDimensionArrayNode instanceof OneDimensionDataNode) {
            // set expected size for this variable
            ((OneDimensionDataNode) oneDimensionArrayNode).setSize(10);

            // Expand tree
            expander.expandTree((ValueDataNode) oneDimensionArrayNode);
            logger.debug("Expanded tree:\n" + new SimpleTreeDisplayer().toString(root));
        }
    }

//    public void expandTree(DataNode node) throws Exception {
////		if (node instanceof TemplateDataNode) {
////			expandTree(((TemplateDataNode) node).getReal());
////			return;
////		}
//
//        VariableNode vParent = node.getCorrespondingVar();
//        node.getChildren().clear();
//
//        if (node instanceof StructDataNode) {
//            generateStructureItem(vParent, node);
//            for (IDataNode child : node.getChildren())
//                if (child instanceof StructDataNode)
//                    expandTree(((DataNode) child));
//
//        } else if (node instanceof UnionDataNode) {
//            generateStructureItem(vParent, node);
//
//        } else if (node instanceof ClassDataNode) {
//            if (node instanceof SubClassDataNode) {
//                generateConstructor(vParent, node);
//                generateStructureItem(vParent, node);
//            } else { // Chỉ là Class Data Node chưa xác định real class
//                generateSubclass(vParent, (ClassDataNode) node);
//            }
//
//        } else if (node instanceof OneDimensionNumberDataNode
//                || node instanceof OneDimensionCharacterDataNode || node instanceof OneDimensionStringDataNode) {
//            int size = ((OneDimensionDataNode) node).getSize();
//            for (int i = 0; i < size; i++)
//                generateArrayItemInBasicVariable(node.getName() + "[" + i + "]", node);
//
//        } else if (node instanceof OneLevelNumberDataNode
//                || node instanceof OneLevelCharacterDataNode || node instanceof OneLevelStringDataNode) {
//            int size = ((OneLevelDataNode) node).getAllocatedSize();
//            for (int i = 0; i < size; i++)
//                generateArrayItemInBasicVariable(node.getName() + "[" + i + "]", node);
//
//        } else if (node instanceof OneDimensionStructureDataNode) {
//            int size = ((OneDimensionStructureDataNode) node).getSize();
//            for (int i = 0; i < size; i++)
//                generateArrayItemInStructureVariable(node.getName() + "[" + i + "]", node);
//
//        } else if (node instanceof OneLevelStructureDataNode) {
//            int size = ((OneLevelStructureDataNode) node).getAllocatedSize();
//            for (int i = 0; i < size; i++)
//                generateArrayItemInStructureVariable(node.getName() + "[" + i + "]", node);
//
//        } else if (node instanceof TwoDimensionCharacterDataNode
//                || node instanceof TwoDimensionNumberDataNode || node instanceof TwoDimensionStringDataNode) {
//            int sizeA = ((TwoDimensionDataNode) node).getSizeA();
//            int sizeB = ((TwoDimensionDataNode) node).getSizeB();
//            for (int i = 0; i < sizeA; i++)
//                for (int j = 0; j < sizeB; j++)
//                    generateArrayItemInBasicVariable(node.getName() + "[" + i + "][" + j + "]", node);
//
//        } else if (node instanceof TwoLevelNumberDataNode
//                || node instanceof TwoLevelCharacterDataNode || node instanceof TwoLevelStringDataNode) {
//            int sizeA = ((TwoLevelDataNode) node).getAllocatedSizeA();
//            int sizeB = ((TwoLevelDataNode) node).getAllocatedSizeB();
//            for (int i = 0; i < sizeA; i++)
//                for (int j = 0; j < sizeB; j++)
//                    generateArrayItemInBasicVariable(node.getName() + "[" + i + "][" + j + "]", node);
//
//        } else if (node instanceof TwoDimensionStructureDataNode) {
//            int sizeA = ((TwoDimensionStructureDataNode) node).getSizeA();
//            int sizeB = ((TwoDimensionStructureDataNode) node).getSizeB();
//            for (int i = 0; i < sizeA; i++)
//                for (int j = 0; j < sizeB; j++)
//                    generateArrayItemInStructureVariable(node.getName() + "[" + i + "][" + j + "]", node);
//
//        } else if (node instanceof TwoLevelStructureDataNode) {
//            int sizeA = ((TwoLevelStructureDataNode) node).getAllocatedSizeA();
//            int sizeB = ((TwoLevelStructureDataNode) node).getAllocatedSizeB();
//            for (int i = 0; i < sizeA; i++)
//                for (int j = 0; j < sizeB; j++)
//                    generateArrayItemInStructureVariable(node.getName() + "[" + i + "][" + j + "]", node);
//        } else {
//            logger.debug("Does not support to expand " + node.getClass());
//        }
////    }
//
//    public void expandTree(IDataNode root, String[] names) throws Exception {
//        IDataNode currentParent = root;
//        for (String name : names) {
//            IDataNode n = Search2.findNodeByName(name, currentParent);
//            if (n == null) {
//                // Case 1: Array index, e.g, [0]
//                if (name.matches("\\[.*\\]")) {
//                    int index = Utils.toInt(Utils.getIndexOfArray(name).get(0));
//
//                    if (currentParent instanceof OneLevelDataNode) {
//                        int oldIndex = getMax(((OneLevelDataNode) currentParent).getAllocatedSize(),
//                                functionNode.getFunctionConfig().getSizeOfArray());
//
//                        ((OneLevelDataNode) currentParent).setAllocatedSize(Math.max(oldIndex, index));
//
//                    } else if (currentParent instanceof TwoLevelDataNode) {
//                        // TODO: Dont support in this version
//                    }
//                }
//
//                if (currentParent instanceof StructureDataNode) {
//                    INode nParent = currentParent.getCorrespondingVar().resolveCoreType();
//                    if (nParent != null) {
//                        VariableNode searchedNode = (VariableNode) Search.searchFirstNodeByName(nParent, name);
//
//                        if (searchedNode != null)
//                            currentParent = generateStructureItem(searchedNode, name, (DataNode) currentParent);
//                    }
//                } else if (currentParent instanceof OneLevelStructureDataNode
//                        || currentParent instanceof OneDimensionStructureDataNode)
//                    currentParent = generateArrayItemInStructureVariable(name, (DataNode) currentParent);
//                else if (currentParent instanceof OneDimensionCharacterDataNode
//                        || currentParent instanceof OneDimensionNumberDataNode
//                        || currentParent instanceof OneLevelCharacterDataNode
//                        || currentParent instanceof OneLevelNumberDataNode)
//                    currentParent = generateArrayItemInBasicVariable(name, (DataNode) currentParent);
//                else if (currentParent instanceof TwoDimensionCharacterDataNode
//                        || currentParent instanceof TwoDimensionNumberDataNode
//                        || currentParent instanceof TwoLevelCharacterDataNode
//                        || currentParent instanceof TwoLevelNumberDataNode)
//                    currentParent = generateArrayItemInBasicVariable(name, (DataNode) currentParent);
//            } else
//                currentParent = n;
//        }
//    }
//
////    private NormalDataNode generateArrayItemInBasicVariable(String element, IDataNode currentParent) {
//        int index = getMax(Utils.toInt(Utils.getIndexOfArray(element).get(0)),
//                functionNode.getFunctionConfig().getSizeOfArray());
//        // STEP 1.
//        if (currentParent instanceof OneDimensionDataNode) {
//            if (((OneDimensionDataNode) currentParent).getSize() < index)
//                ((OneDimensionDataNode) currentParent).setSize(index);
//        } else if (currentParent instanceof OneLevelDataNode) {
//            if (((OneLevelDataNode) currentParent).getAllocatedSize() < index)
//                ((OneLevelDataNode) currentParent).setAllocatedSize(index);
//        } else if (currentParent instanceof TwoDimensionDataNode) {
//            if (((TwoDimensionDataNode) currentParent).getSizeA() < index)
//                ((TwoDimensionDataNode) currentParent).setSizeA(index);
//            if (((TwoDimensionDataNode) currentParent).getSizeB() < index)
//                ((TwoDimensionDataNode) currentParent).setSizeB(index);
//        } else if (currentParent instanceof TwoLevelDataNode) {
//            if (((TwoLevelDataNode) currentParent).getAllocatedSizeA() < index)
//                ((TwoLevelDataNode) currentParent).setAllocatedSizeA(index);
//            if (((TwoLevelDataNode) currentParent).getAllocatedSizeB() < index)
//                ((TwoLevelDataNode) currentParent).setAllocatedSizeB(index);
//        }
//        // STEP 2.
//        VariableNode v = new VariableNode();
//        v.setName(element);
//        v.setParent(currentParent.getCorrespondingVar());
//
//        String rType = VariableTypeUtils.deleteStorageClasses(currentParent.getCorrespondingVar().getReducedRawType());
//        rType = rType.replace("*", "").replaceAll("\\[.*\\]", "");
//
//        v.setRawType(rType);
//        v.setCoreType(rType);
//        v.setReducedRawType(rType);
//
//        // STEP 3.
//        NormalDataNode child;
//        if (VariableTypeUtils.isCh(v.getRawType()))
//            child = new NormalCharacterDataNode();
//        else
//            child = new NormalNumberDataNode();
//
//        child.setParent(currentParent);
//        child.setCorrespondingVar(v);
//        child.setType(v.getRawType());
//        child.setName(element);
//        currentParent.addChild(child);
//
//        return child;
//    }

//    private IDataNode generateArrayItemInStructureVariable(String element, IDataNode currentParent) {
//        // STEP 1.
//        VariableNode v = new VariableNode();
//        v.setName(element);
//        v.setParent(currentParent.getCorrespondingVar());
//
//        String rType = VariableTypeUtils.deleteStorageClasses(currentParent.getCorrespondingVar().getReducedRawType());
//        rType = rType.replace("*", "").replaceAll(IRegex.ARRAY_INDEX, "");
//
//        v.setRawType(rType);
//        v.setCoreType(rType);
//        v.setReducedRawType(rType);
//
//        // STEP 2.
//        StructureDataNode child = new StructureDataNode();
//        child.setParent(currentParent);
//        child.setCorrespondingVar(v);
//        child.setType(v.getRawType());
//        child.setName(element);
//
//        currentParent.addChild(child);
//
//        return child;
//    }

//    private IDataNode generateStructureItem(VariableNode vChild, String element, IDataNode currentParent)
//            throws Exception {
//        IDataNode child;
//
//        if (VariableTypeUtils.isBasic(vChild.getRawType())) {
//
//            if (VariableTypeUtils.isCh(vChild.getRawType()))
//                child = new NormalCharacterDataNode();
//            else
//                child = new NormalNumberDataNode();
//
//            child.setCorrespondingVar(vChild);
//            child.setName(element);
//            child.setType(vChild.getRawType());
//            child.setParent(currentParent);
//            currentParent.addChild(child);
//
////            currentParent = child;
//
//        } else if (VariableTypeUtils.isChOneDimension(vChild.getRawType())) {
//            child = new OneDimensionCharacterDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isChOneLevel(vChild.getRawType())) {
//
//            child = new OneLevelCharacterDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getReducedRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isNumOneLevel(vChild.getRawType())) {
//            child = new OneLevelNumberDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setName(element);
//            child.setType(vChild.getRawType());
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isNumOneDimension(vChild.getRawType())) {
//            child = new OneDimensionNumberDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isStructureSimple(vChild.getRawType())) {
//            child = new StructureDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isStructureOneLevel(vChild.getRawType())) {
//            child = new OneLevelStructureDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else if (VariableTypeUtils.isStructureOneDimension(vChild.getRawType())) {
//            child = new OneDimensionStructureDataNode();
//            child.setCorrespondingVar(vChild);
//            child.setType(vChild.getRawType());
//            child.setName(element);
//            currentParent.addChild(child);
//            child.setParent(currentParent);
//
//        } else
//            throw new Exception("Chua xu ly " + element + " trong generateStructureItem");
//
//        return child;
//    }

    private int getMax(int a, int b) {
        return Math.max(a, b);
    }

    public void expandStructureNodeOnDataTree(ValueDataNode node, String name) throws Exception {
        node.getChildren().clear();

        VariableNode vParent = node.getCorrespondingVar();
        INode correspondingNode = vParent.resolveCoreType();

        if (correspondingNode instanceof StructureNode) {
            StructureNode childClass = (StructureNode) correspondingNode;
            for (IVariableNode n : childClass.getPublicAttributes()) {
                if (n.getName().contains(name))
                    generateStructureItem((VariableNode) n, vParent + "." + name, node);
            }
        }else{
            logger.error("Do not handle the case " + correspondingNode.getClass());
        }
    }

	public ICommonFunctionNode getFunctionNode() {
		return functionNode;
	}

	public void setFunctionNode(ICommonFunctionNode functionNode) {
		this.functionNode = functionNode;
	}
}

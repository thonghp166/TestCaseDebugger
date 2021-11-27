package com.dse.testdata.gen.module;


import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testdata.gen.module.type.*;
import com.dse.testdata.object.*;
import com.dse.util.TemplateUtils;
import com.dse.util.VariableTypeUtils;
import com.dse.util.AkaLogger;

import java.io.File;
import java.util.List;

/**
 * Táº¡o cÃ¢y khá»Ÿi Ä‘áº§u dá»±a trÃªn arguments vÃ  external variable Táº¥t cáº£ má»�i biáº¿n
 * truyá»�n vÃ o hÃ m thuá»™c ba loáº¡i:
 * <p>
 * + Biáº¿n cÆ¡ báº£n: Ä�Æ°á»£c sinh giÃ¡ trá»‹ ngáº«u nhiÃªn
 * <p>
 * + Biáº¿n máº£ng: máº·c Ä‘á»‹nh sá»‘ pháº§n tá»­ lÃ  0
 * <p>
 * + Biáº¿n con trá»�: máº·c Ä‘á»‹nh gÃ­a trá»‹ lÃ  null
 *
 * @author ducanh
 */
public class InitialTreeGen {
    private IFunctionNode functionNode;
    protected final static AkaLogger logger = AkaLogger.get(InitialTreeGen.class);
    private IDataNode root;


    public static void main(String[] args) throws Exception {
        ProjectParser parser = new ProjectParser(new File(Paths.TSDV_R1));

        String name = "SimpleMethodTest()";
//         String name = "StackLinkedList::push(Node*)";
        FunctionNode function = (FunctionNode) Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), name).get(0);

        /*
         *
         */
        RootDataNode root = new RootDataNode();
        InitialTreeGen dataTreeGen = new InitialTreeGen();
        dataTreeGen.generateTree(root, function);
        System.out.println(new SimpleTreeDisplayer().toString(root));
    }

    public void generateTree(IDataNode root, IFunctionNode functionNode) throws Exception {
        this.root = root;
        this.functionNode = functionNode;

        root.setName(functionNode.getSimpleName());

        List<IVariableNode> passingVariables = functionNode.getPassingVariables();
        for (INode passingVariable : passingVariables)
            genInitialTree((VariableNode) passingVariable, (DataNode) root);
    }

    public ValueDataNode genInitialTree(VariableNode vCurrentChild, DataNode nCurrentParent) throws Exception {
        String rawType = VariableTypeUtils.getSimpleRawType(vCurrentChild);
        rawType = VariableTypeUtils.deleteReferenceOperator(rawType);

        // Step: Check the type
        if (VariableTypeUtils.isVoid(rawType)) {
            logger.error("Do not support type parameters for void function");
            return null;

        } else if (TemplateUtils.isTemplateClass(rawType)) {
            return new TemplateClassTypeInitiation(vCurrentChild, nCurrentParent).execute();

        } else if (VariableTypeUtils.isBasic(rawType)) {
            return new BasicTypeInitiation(vCurrentChild, nCurrentParent).execute();

        } else if (VariableTypeUtils.isMultipleDimension(rawType)) {
            return new MultipleDimensionTypeInitiation(vCurrentChild, nCurrentParent).execute();
        } else if (VariableTypeUtils.isOneDimension(rawType)) {
            return new OneDimensionTypeInitiation(vCurrentChild, nCurrentParent).execute();
        } else if (VariableTypeUtils.isPointer(rawType)) {
            return new PointerTypeInitiation(vCurrentChild, nCurrentParent).execute();
        } else if (VariableTypeUtils.isStructureSimple(rawType)) {
//            if (VariableTypeUtils.isEnumNode(rawType, Utils.getRoot(functionNode)))
//                new EnumTypeInitiation(vCurrentChild, nCurrentParent).execute();
//            else
            return new StructureTypeInitiation(vCurrentChild, nCurrentParent).execute();

        } else  {
            logger.error("Can not handle " + vCurrentChild.toString());
            return new ProblemTypeInitiation(vCurrentChild, nCurrentParent).execute();
        }
    }

    public IFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(FunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    public IDataNode getRoot() {
        return root;
    }

    public void setRoot(IDataNode root) {
        this.root = root;
    }

//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  kiá»ƒu cÆ¡ báº£n
//     *
//     * @param vParent
//     * @param nParent
//     */
//    private void initialBasicType(VariableNode vParent, IDataNode nParent) {
//        IDataNode child;
//        if (VariableTypeUtils.isCh(vParent.getRealType()))
//            child = new NormalCharacterDataNode();
//        else
//            child = new NormalNumberDataNode();
//
//        child.setChildren(null);
//        child.setParent(nParent);
//        child.setType(vParent.getRealType());
//        child.setName(vParent.getNewType());
//        child.setCorrespondingVar(vParent);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//        nParent.addChild(child);
//    }
//
//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  máº£ng má»™t chiá»�u
//     *
//     * @param vParent
//     * @param nParent
//     * @throws Exception
//     */
//    private void initialOneDimensionType(VariableNode vParent, IDataNode nParent) throws Exception {
//        String rawType = vParent.getRealType();
//        OneDimensionDataNode child;
//        if (VariableTypeUtils.isCh(rawType))
//            child = new OneDimensionCharacterDataNode();
//        else if (VariableTypeUtils.isNum(rawType))
//            child = new OneDimensionNumberDataNode();
//        else
//            child = new OneDimensionStructureDataNode();
//        /*
//
//         */
//        child.setParent(nParent);
//        child.setType(vParent.getRealType());
//        child.setName(vParent.getNewType());
//        child.setCorrespondingVar(vParent);
//
//        List<String> indexes = Utils.getIndexOfArray(vParent.getRawType());
//        if (indexes.size() == 1) {
//            /*
//              Ex: a[3]
//             */
//            int indexInt = Utils.toInt(indexes.get(0));
//            if (indexInt != Utils.UNDEFINED_TO_INT) {
//                child.setSize(indexInt);
//                child.setFixedSize(true);
//                child.setSizeIsSet(true);
//            } else {
//                child.setSize(OneDimensionDataNode.UNDEFINED_SIZE);
//                child.setFixedSize(false);
//                child.setSizeIsSet(false);
//            }
//        } else if (indexes.size() == 0) {
//            /*
//              Ex: a[]
//             */
//            child.setSize(OneDimensionDataNode.UNDEFINED_SIZE);
//            child.setFixedSize(false);
//            child.setSizeIsSet(false);
//        }
//
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//
//        nParent.addChild(child);
//
//    }
//
//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  con trá»� má»™t má»©c
//     *
//     * @param vParent
//     * @param nParent
//     * @throws Exception
//     */
//    private void initialOneLevelType(VariableNode vParent, IDataNode nParent) throws Exception {
//        String rawType = Utils.getRealType(vParent.getReducedRawType(), vParent.getParent());
//        OneLevelDataNode child;
//        if (VariableTypeUtils.isCh(rawType))
//            child = new OneLevelCharacterDataNode();
//        else if (VariableTypeUtils.isNum(rawType))
//            child = new OneLevelNumberDataNode();
//        else
//            child = new OneLevelStructureDataNode();
//
//        child.setParent(nParent);
//        child.setType(vParent.getRealType());
//        child.setName(vParent.getNewType());
//        child.setCorrespondingVar(vParent);
//        child.setAllocatedSize(PointerDataNode.NULL_VALUE);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//
//        nParent.addChild(child);
//    }
//
//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  kiá»ƒu cáº¥u trÃºc
//     *
//     * @param vParent
//     * @param nParent
//     * @throws Exception
//     */
//    private void initialStructureType(VariableNode vParent, IDataNode nParent) throws Exception {
//        INode correspondingNode = vParent.resolveCoreType();
//        StructureDataNode child = null;
//
//        if (correspondingNode instanceof StructNode)
//            child = new StructDataNode();
//        else if (correspondingNode instanceof ClassNode)
//            child = new ClassDataNode();
//        else if (correspondingNode instanceof UnionNode)
//            child = new UnionDataNode();
//
//        child.setParent(nParent);
//        child.setName(vParent.getNewType());
//        child.setType(vParent.getFullType());
//        child.setCorrespondingVar(vParent);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//        nParent.addChild(child);
//    }
//
//    private void initialEnumType(VariableNode vParent, IDataNode nParent) {
//        EnumDataNode child = new EnumDataNode();
//
//        child.setParent(nParent);
//        child.setName(vParent.getNewType());
//        child.setType(vParent.getRealType());
//        child.setCorrespondingVar(vParent);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//        nParent.addChild(child);
//    }
//
////    /**
////     * CÃ i tÃªn áº£o cho cÃ¢y
////     *
////     * @param n
////     */
////    private void setVituralName(IDataNode n) {
////        if (n == null)
////            return;
////        else
////            n.setVituralName(n.getName());
////        if (n.getChildren() != null)
////            for (IDataNode child : n.getChildren())
////                setVituralName(child);
////    }
//
//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  máº£ng hai chiá»�u
//     *
//     * @param vParent
//     * @param nParent
//     * @throws Exception
//     */
//    private void initialTwoDimensionType(VariableNode vParent, IDataNode nParent) throws Exception {
//        String rawType = vParent.getRealType();
//        TwoDimensionDataNode child;
//        if (VariableTypeUtils.isChTwoDimension(rawType))
//            child = new TwoDimensionCharacterDataNode();
//        else if (VariableTypeUtils.isNumTwoDimension(rawType))
//            child = new TwoDimensionNumberDataNode();
//        else
//            child = new TwoDimensionStructureDataNode();
//        /*
//
//         */
//        child.setParent(nParent);
//        child.setType(vParent.getRealType());
//        child.setName(vParent.getNewType());
//        child.setCorrespondingVar(vParent);
//        child.setSizeA(-1);
//        child.setSizeB(-1);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//
//        nParent.addChild(child);
//    }
//
//    /**
//     * Khá»Ÿi táº¡o biáº¿n truyá»�n vÃ o lÃ  con trá»� hai má»©c
//     *
//     * @param vParent
//     * @param nParent
//     * @throws Exception
//     */
//    private void initialTwoLevelType(VariableNode vParent, IDataNode nParent) throws Exception {
//        String rawType = vParent.getRealType();
//        TwoLevelDataNode child;
//        if (VariableTypeUtils.isChTwoLevel(rawType))
//            child = new TwoLevelCharacterDataNode();
//        else if (VariableTypeUtils.isNumTwoLevel(rawType))
//            child = new TwoLevelNumberDataNode();
//        else
//            child = new TwoLevelStructureDataNode();
//
//        child.setParent(nParent);
//        child.setType(vParent.getRealType());
//        child.setName(vParent.getNewType());
//        child.setCorrespondingVar(vParent);
//        child.setAllocatedSizeA(-1);
//        child.setAllocatedSizeB(-1);
//        if (vParent instanceof ExternalVariableNode)
//            child.setExternelVariable(true);
//
//        nParent.addChild(child);
//    }

}

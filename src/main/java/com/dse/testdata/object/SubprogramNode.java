package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.*;
import com.dse.search.Search2;
import com.dse.testdata.gen.module.subtree.InitialArgTreeGen;
import com.dse.util.IGTestConstant;
import com.dse.util.NodeType;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SubprogramNode extends ValueDataNode {
    protected INode functionNode;
    private String userCode;

//    private List<ValueDataNode> parameterExpectedOutputs = new ArrayList<>();
    Map<ValueDataNode, ValueDataNode> inputToExpectedOutputMap = new HashMap<>();

    public SubprogramNode() {

    }

    public Map<ValueDataNode, ValueDataNode> getInputToExpectedOutputMap() {
        return inputToExpectedOutputMap;
    }

    public SubprogramNode(INode fn) {
        setFunctionNode(fn);
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public void initInputToExpectedOutputMap() {
        inputToExpectedOutputMap.clear();
//        try {
//            for (IDataNode input : getChildren()) {
//                // except the RETURN VARIABLE
//                if (! input.getName().equals("RETURN")) {
//                    ValueDataNode expectedOutput = (ValueDataNode) ((ValueDataNode) input).clone();
//                    inputToExpectedOutputMap.put((ValueDataNode) input, expectedOutput);
//                }
//            }
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        /*
          create initial tree
         */
        try {
            ICommonFunctionNode castFunctionNode = (ICommonFunctionNode) functionNode;
            RootDataNode root = new RootDataNode();
            root.setFunctionNode(castFunctionNode);
            InitialArgTreeGen dataTreeGen = new InitialArgTreeGen();
            dataTreeGen.generateCompleteTree(root, null);
            for (IDataNode node : root.getChildren()) {
                if (node instanceof SubprogramNode && ((SubprogramNode) node).getFunctionNode() == functionNode) {
                    for (IDataNode eo : node.getChildren()) {
//                        String prevVirtualName = eo.getVituralName();
//                        String newVirtualName = IGTestConstant.EXPECTED_PREFIX + prevVirtualName;
//                        eo.setVituralName(newVirtualName);
                        for (IDataNode input : getChildren()) {
                            if (((ValueDataNode) input).getCorrespondingVar() == ((ValueDataNode) eo).getCorrespondingVar()) {
                                inputToExpectedOutputMap.put((ValueDataNode) input, (ValueDataNode) eo);
                                eo.setParent(input.getParent());

                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Collection<ValueDataNode> getParamExpectedOuputs() {
        return inputToExpectedOutputMap.values();
    }

    public boolean putParamExpectedOutputs(ValueDataNode expectedOuput) {
        if (expectedOuput.getName().equals("RETURN")) return false;
        ValueDataNode input = null;
        for (IDataNode child : getChildren()) {
            if (((ValueDataNode) child).getCorrespondingVar() == expectedOuput.getCorrespondingVar()) {
                input = (ValueDataNode) child;
                break;
            }
        }

        if (input != null) {
            if (inputToExpectedOutputMap.containsKey(input)) {
                inputToExpectedOutputMap.remove(input);
            }
            inputToExpectedOutputMap.put(input, expectedOuput);
            return true;
        }

        return false;
    }

    public boolean checkIsValidParamExpectedOuputs() {
        for (IDataNode input : getChildren()) {
            if (! input.getName().equals("RETURN")) {
                ValueDataNode eo = getExpectedOuput((ValueDataNode) input);
                if (eo == null) return false;
            }
        }

        return true;
    }

    public INode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(INode functionNode) {
        this.functionNode = functionNode;

        if (functionNode == null)
            return;

        setName(functionNode.getName());

        String type = ((ICommonFunctionNode) functionNode).getReturnType();
        VariableNode returnVarNode = Search2.getReturnVarNode((ICommonFunctionNode) functionNode);

        if (returnVarNode != null) {
            setCorrespondingVar(returnVarNode);
            type = VariableTypeUtils.getFullRawType(returnVarNode);
        }

        type = VariableTypeUtils.deleteStorageClasses(type);
        type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);

        setType(type);
    }

    public ValueDataNode getExpectedOuput(ValueDataNode inputValue) {
        ValueDataNode eo = inputToExpectedOutputMap.get(inputValue);
//        if (eo == null) {
//            try {
//                ValueDataNode newEo = (ValueDataNode) inputValue.clone();
////                new InputCellHandler().commitEdit(inputValue, "");
//                inputToExpectedOutputMap.put(inputValue, newEo);
//                return newEo;
//            } catch (CloneNotSupportedException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        return eo;
    }

    @Override
    public String getSetterInStr(String nameVar) {
        return "(no setter)";
    }

    @Override
    public String getGetterInStr() {
        return "(no getter)";
    }

//    @Override
//    public String getName() {
//        if (super.getName().length() == 0)
//            if (functionNode != null)
//                setName(functionNode.getName());
//
//        return super.getName();
//    }
//
//    @Override
//    public String getType() {
//        if (super.getType().length() == 0 && !(this instanceof ConstructorDataNode))
//            if (functionNode != null) {
//                VariableNode returnVarNode = Search2.getReturnVarNode((ICommonFunctionNode) functionNode);
//                if (returnVarNode != null) {
//                    String type = VariableTypeUtils.getFullRawType(returnVarNode);
//                    type = VariableTypeUtils.deleteStorageClasses(type);
////                type = VariableTypeUtils.deleteStructKeyword(type);
////                type = VariableTypeUtils.deleteUnionKeyword(type);
//                    type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);
//                    setType(type);
//                }
//            }
//
//        return super.getType();
//    }

    public String getDisplayNameInParameterTree() {
        String prefixPath = "";

        INode originalNode = getFunctionNode();

        if (originalNode instanceof ICommonFunctionNode) {
            prefixPath = ((ICommonFunctionNode) originalNode).getSingleSimpleName();

            if (isLibrary())
                return prefixPath.replace(IGTestConstant.STUB_PREFIX, SpecialCharacter.EMPTY);

            INode currentNode = originalNode.getParent();

            if (originalNode instanceof AbstractFunctionNode) {
                INode realParent = ((AbstractFunctionNode) originalNode).getRealParent();
                if (realParent != null)
                    currentNode = realParent;
            }

            while ((currentNode instanceof StructureNode || currentNode instanceof NamespaceNode)) {
                prefixPath = currentNode.getNewType() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + prefixPath;
                currentNode = currentNode.getParent();
            }
        }

        return prefixPath;
    }

    private boolean isLibrary() {
        if (functionNode == null)
            return false;

        if (Environment.getInstance().getSystemLibraryRoot() == null)
            return false;

        INode root = functionNode.getParent();

        if (root == null)
            return false;

        root = root.getParent();

        return Environment.getInstance().getSystemLibraryRoot().equals(root);
    }

    @Override
    public String getDotGetterInStr() {
        return "(no getter)";
    }

    @Override
    public String generateInputToSavedInFile() throws Exception {
        return super.generateInputToSavedInFile();
    }

    public boolean isStubable() {
        if (this instanceof ConstructorDataNode)
            return false;

        if (getRoot() instanceof RootDataNode) {
            NodeType type = ((RootDataNode) getRoot()).getLevel();

            if (type == NodeType.STUB || type == NodeType.SBF)
                return true;
        }

        if (getParent() instanceof UnitNode) {
            UnitNode unit = getUnit();

            if (unit instanceof StubUnitNode)
                return true;
        }

        return false;
    }

    public boolean isStub() {
        return !getChildren().isEmpty();
    }
}

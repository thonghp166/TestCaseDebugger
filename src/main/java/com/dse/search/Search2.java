package com.dse.search;

import com.dse.parser.object.*;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.testdata.object.*;
import com.dse.util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Search2 {
    final static AkaLogger logger = AkaLogger.get(Search2.class);

    public static List<IDataNode> searchNodes(IDataNode searchRoot, DataNode... conditions) {
        List<IDataNode> output = new ArrayList<>();

        for (IDataNode child : searchRoot.getChildren()) {
            boolean isSatisfied = true;
            for (DataNode condition : conditions)
                // check whether child is an sub-class of condition
                if (!condition.getClass().isInstance(child)) {
                    isSatisfied = false;
                    break;
                }
            if (isSatisfied)
                output.add(child);
            output.addAll(searchNodes(child, conditions));
        }
        return output;
    }

    public static List<IDataNode> searchNodes(IDataNode searchRoot, DataNode condition) {
        List<IDataNode> output = new ArrayList<>();

        for (IDataNode child : searchRoot.getChildren()) {
            if (condition.getClass().isInstance(child)) // check whether child is an sub-class of condition
                output.add(child);
            output.addAll(searchNodes(child, condition));
        }
        return output;
    }

    public static IValueDataNode getExpectedOutputNode(RootDataNode root) {
        SubprogramNode sut = findSubprogramUnderTest(root);

        if (sut != null && !sut.getChildren().isEmpty()) {
            for (IDataNode child : sut.getChildren())
                if (((IValueDataNode) child).getCorrespondingVar() instanceof ReturnVariableNode) {
                    return (IValueDataNode) child;
                }
        }

        return null;
    }

    public static ValueDataNode getExpectedValue(ValueDataNode node) {
        ValueDataNode expectedNode = null;
        List<String> traceNames = new ArrayList<>();

        IDataNode parent = node;

        boolean isInputArgument = false;
        boolean isGlobal = false;

        while (parent != null) {
            if (parent instanceof UnitNode)
                break;

            if (parent instanceof RootDataNode && ((RootDataNode) parent).getLevel() == NodeType.GLOBAL) {
                isGlobal = true;
                break;
            }

            if (parent instanceof SubprogramNode && parent.getParent() instanceof UnitUnderTestNode) {
                isInputArgument = true;
                break;
            }

            traceNames.add(0, parent.getName());

            parent = parent.getParent();
        }

        if (isInputArgument || isGlobal) {
            boolean found = false;

            String name = traceNames.remove(0);

            Collection<ValueDataNode> expectedParams;

            if (isInputArgument)
                expectedParams = ((SubprogramNode) parent).getParamExpectedOuputs();
            else
                expectedParams = ((RootDataNode) parent).getGlobalInputExpOutputMap().values();

            for (ValueDataNode child : expectedParams) {
                if (child.getName().equals(name)) {
                    expectedNode = child;
                    found = true;
                    break;
                }
            }

            if (found) {
                while (!traceNames.isEmpty()) {
                    found = false;
                    name = traceNames.remove(0);

                    for (IDataNode child : expectedNode.getChildren()) {
                        if (child instanceof ValueDataNode && child.getName().equals(name)) {
                            expectedNode = (ValueDataNode) child;
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        expectedNode = null;
                        break;
                    }
                }
            }
        }

        return expectedNode;
    }

//    public static ValueDataNode getExpectedValueNode(ValueDataNode node) {
//        SubprogramNode sut = findSubprogramUnderTest(node.getTestCaseRoot());
//
//
//    }

    public static List<IDataNode> searchStubSubprograms(RootDataNode root) {
        ICommonFunctionNode sut = root.getFunctionNode();
        List<IDataNode> subprograms = searchNodes(root, new SubprogramNode());

        subprograms.removeIf(f -> f instanceof ConstructorDataNode
                || sut.equals(((SubprogramNode) f).getFunctionNode())
                || f.getChildren().isEmpty());

        return subprograms;
    }

    public static List<IDataNode> searchStubableSubprograms(RootDataNode root) {
        ICommonFunctionNode sut = root.getFunctionNode();
        List<IDataNode> subprograms = searchNodes(root, new SubprogramNode());

        subprograms.removeIf(f -> f instanceof ConstructorDataNode
                || sut.equals(((SubprogramNode) f).getFunctionNode()));

        return subprograms;
    }

    public static RootDataNode findGlobalRoot(RootDataNode root) {
        if (root == null)
            return null;
        ICommonFunctionNode functionNode = root.getFunctionNode();

        if (functionNode != null) {
            INode source = Utils.getSourcecodeFile(functionNode);

            for (IDataNode child : root.getChildren()) {
                if (child instanceof UnitNode && ((UnitNode) child).getSourceNode().equals(source)) {
                    for (IDataNode globalRoot : child.getChildren()) {
                        if (globalRoot instanceof RootDataNode
                                && ((RootDataNode) globalRoot).getLevel() == NodeType.GLOBAL) {
                            return (RootDataNode) globalRoot;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Except RETURN
     * @param root
     * @return
     */
    public static List<IDataNode> findArgumentNodes(RootDataNode root) {
        List<IDataNode> vars = new ArrayList<>();
        SubprogramNode subprogramNode = findSubprogramUnderTest(root);
        for (IDataNode child : subprogramNode.getChildren())
            if (child instanceof ValueDataNode)
                if (!child.getName().equals(INameRule.RETURN_VARIABLE_NAME_PREFIX))
                    vars.add(child);
        return vars;
    }


    public static SubprogramNode findSubprogramUnderTest(RootDataNode root) {
        ICommonFunctionNode functionNode = root.getFunctionNode();

        if (functionNode != null) {
            INode source = Utils.getSourcecodeFile(functionNode);

            for (IDataNode child : root.getChildren()) {
                if (child instanceof UnitNode && ((UnitNode) child).getSourceNode().equals(source)) {
                    for (IDataNode sub : child.getChildren()) {
                        if (sub instanceof SubprogramNode) {
                            return (SubprogramNode) sub;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param names Danh sách tên có thứ tự
     * @param n     Tên node cha bắt đầu tìm kiếm (Bắt đầu tìm kiếm từ con node
     *              cha)
     * @return
     */
    public static IDataNode findNodeByChainName(String[] names, IDataNode n) {
        IDataNode output = n;
        for (String name : names) {
            IDataNode nName = Search2.findNodeByName(name, output);
            if (nName == null)
                return null;
            else
                output = nName;
        }
        return output;
    }

    /**
     * Tìm con một node có tên xác định
     *
     * @param name tên node cần tìm
     * @param n    node cha
     * @return
     */
    public static IDataNode findNodeByName(String name, IDataNode n) {
        for (IDataNode child : n.getChildren())
            if (child.getName().equals(name))
                return child;
        return null;
    }

    public static RootDataNode getRoot(IDataNode n) {
        do
            if (n instanceof RootDataNode)
                return (RootDataNode) n;
            else if (n.getParent() != null)
                n = n.getParent();
            else
                return null;
        while (n != null);
        return null;
    }

    public static VariableNode getReturnOf(IFunctionNode functionNode) {
        for (IVariableNode node : functionNode.getExpectedNodeTypes())
            if (node instanceof ReturnVariableNode) {
                node.setParent(functionNode);
                return (VariableNode) node;
            }
        return null;
    }

    public static VariableNode getReturnOf(DefinitionFunctionNode functionNode) {
        String returnType = functionNode.getReturnType();
        returnType = VariableTypeUtils.deleteVirtualAndInlineKeyword(returnType);

        if (returnType == null) {
            logger.error("Can not get the return value of " + functionNode.getAbsolutePath());
            return null;

        } else if (returnType.equals("void")) {
            logger.error("Do not support to add the return value of " + returnType);
            return null;

        } else {
            // add a variable representing return value
            VariableNode returnVar = new ReturnVariableNode();
            returnVar.setName(INameRule.RETURN_VARIABLE_NAME_PREFIX);
            returnVar.setRawType(returnType);
            String coreType = returnType.replace(SpecialCharacter.POINTER, "");
            coreType = VariableTypeUtils.deleteStorageClasses(coreType);
            coreType = VariableTypeUtils.deleteStructKeyword(coreType);
            coreType = VariableTypeUtils.deleteUnionKeyword(coreType);
            coreType = VariableTypeUtils.deleteVirtualAndInlineKeyword(coreType);
            returnVar.setCoreType(coreType);
            returnVar.setReducedRawType(returnType);

            if (functionNode.getChildren().isEmpty())
                returnVar.setParent(functionNode);
            else {
                if (!(functionNode.getChildren().get(functionNode.getChildren().size() - 1)
                        instanceof ReturnVariableNode))
                    returnVar.setParent(functionNode);
            }

//            returnVar.setParent(functionNode);

            return returnVar;
        }
    }

    public static VariableNode getReturnVarNode(ICommonFunctionNode functionNode) {
        if (functionNode instanceof ConstructorNode || functionNode instanceof DestructorNode)
            return null;
        else if (functionNode instanceof DefinitionFunctionNode)
            return getReturnOf((DefinitionFunctionNode) functionNode);
        else if (functionNode instanceof IFunctionNode)
            return getReturnOf((IFunctionNode) functionNode);
        else {
            logger.error("Do not handle " + functionNode.getClass());
            return null;
        }
    }

    public static INode findNodeById(INode root, int id, SearchCondition nodeType) {
        List<INode> completeVariables = Search.searchNodes(root, nodeType);
        for (INode variable : completeVariables) {
            if (variable.getId() == id) {
                return variable;
            }
        }
        return null;
    }

    public static INode findFunctionNodeById(INode root, int id) {
        List<INode> completeVariables = Search.searchNodes(root, new AbstractFunctionNodeCondition());
        for (INode variable : completeVariables) {
            if (variable.getId() == id) {
                return variable;
            }
        }
        return null;
    }
}

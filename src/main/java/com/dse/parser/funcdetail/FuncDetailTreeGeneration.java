package com.dse.parser.funcdetail;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.FunctionCallDependency;
import com.dse.parser.dependency.IncludeHeaderDependency;
import com.dse.parser.dependency.finder.VariableSearchingSpace;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.GlobalVariableNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_manager.TestCase;
import com.dse.util.*;

import java.util.ArrayList;
import java.util.List;

public class FuncDetailTreeGeneration implements IFuncDetailTreeGeneration {
    private final static AkaLogger logger = AkaLogger.get(FuncDetailTreeGeneration.class);

    public FuncDetailTreeGeneration(RootNode root, ICommonFunctionNode fn) {
        generateTree(root, fn);
    }

    @Override
    public void generateTree(RootNode root, ICommonFunctionNode fn) {
        logger.debug("generateGlobalSubTree");
        generateGlobalSubTree(root, fn);

        logger.debug("generateUUTSubTree");
        generateUUTSubTree(root, fn);

        logger.debug("generateStubSubTree");
        generateStubSubTree(root, fn);
    }

    List<INode> includeNodes = new ArrayList<>();

    private boolean isSystemUnit(INode unit) {
        List<INode> sources = Search
                .searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());

        return !sources.contains(unit);
    }

    private List<Node> getAllIncludedNodes(INode n) {
        List<Node> output = new ArrayList<>();

        if (n != null) {
            try {
                for (Dependency child : n.getDependencies()) {
                    if (child instanceof IncludeHeaderDependency) {
                        if (child.getStartArrow().equals(n)) {
                            includeNodes.add(n);

                            INode end = child.getEndArrow();
                            if (!includeNodes.contains(end) && !isSystemUnit(end)) {
                                output.add((Node) end);
                                /*
                                 * In case recursive include
                                 */
                                output.addAll(getAllIncludedNodes(end));
                            }
                        }
                    }
                }
            } catch (StackOverflowError e) {
                e.printStackTrace();
            }
        }

        return output;
    }


    @Override
    public void generateGlobalSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode globalRoot = new RootNode(NodeType.GLOBAL);

        /*
         * Them cac bien global co trong unit
         */
        INode unit = Utils.getSourcecodeFile(fn);
        List<INode> globalVariables = Search.searchNodes(unit, new GlobalVariableNodeCondition());

        List<Node> includedNodes = getAllIncludedNodes(unit);
        for (Node node : includedNodes) {
            List<INode> includedGlobalVariables = Search.searchNodes(node, new GlobalVariableNodeCondition());
            includedGlobalVariables.forEach(global -> {
                if (!globalVariables.contains(global))
                    globalVariables.add(global);
            });
        }

//        List<IVariableNode> globalVariables = fn.getReducedExternalVariables();
        for (INode node : globalVariables)
            if ((node instanceof ExternalVariableNode))
                globalRoot.addElement(node);

        /*
         * Them instance trong truong hop test method cua class
         */
        List<INode> instances = searchAllInstances(unit);

        for (INode instance : instances) {
            InstanceVariableNode instanceVarNode = generateInstance(instance);

            globalRoot.addElement(instanceVarNode);
        }
//        INode realParent = fn.getParent();
//        if (fn.getRealParent() != null)
//            realParent = fn.getRealParent();
//
//        if (realParent instanceof ClassNode) {
//            InstanceVariableNode instance = new InstanceVariableNode();
//            String type = Search.getScopeQualifier(realParent);
//            if (((ClassNode) realParent).isTemplate()) {
//                String[] templateParams = TemplateUtils.getTemplateParameters(realParent);
//                if (templateParams != null) {
//                    type += TemplateUtils.OPEN_TEMPLATE_ARG;
//
//                    for (String param : templateParams)
//                        type += param + ", ";
//
//                    type += TemplateUtils.CLOSE_TEMPLATE_ARG;
//                    type = type.replace(", >", ">");
//                }
//            }
//
//            instance.setCoreType(type);
//            instance.setRawType(type);
//            instance.setReducedRawType(type);
//
//            String instanceVarName = type.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
//            instanceVarName = IGTestConstant.INSTANCE_VARIABLE + SpecialCharacter.UNDERSCORE + instanceVarName;
//            instance.setName(instanceVarName);
//
//            instance.setParent(realParent);
//
//            globalRoot.addElement(instance);
//        }

        root.addElement(globalRoot);
    }

    private InstanceVariableNode generateInstance(INode correspondingType) {
        InstanceVariableNode instance = new InstanceVariableNode();
        String type = Search.getScopeQualifier(correspondingType);
        if (((ClassNode) correspondingType).isTemplate()) {
            String[] templateParams = TemplateUtils.getTemplateParameters(correspondingType);
            if (templateParams != null) {
                type += TemplateUtils.OPEN_TEMPLATE_ARG;

                for (String param : templateParams)
                    type += param + ", ";

                type += TemplateUtils.CLOSE_TEMPLATE_ARG;
                type = type.replace(", >", ">");
            }
        }

        instance.setCoreType(type);
        instance.setRawType(type);
        instance.setReducedRawType(type);

        String instanceVarName = type.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
        instanceVarName = IGTestConstant.INSTANCE_VARIABLE + SpecialCharacter.UNDERSCORE + instanceVarName;
        instance.setName(instanceVarName);

        instance.setParent(correspondingType);

        return instance;
    }

    private List<INode> searchAllInstances(INode unit) {
        List<INode> instances = new ArrayList<>();

        List<INode> functions = Search.searchNodes(unit, new AbstractFunctionNodeCondition());

        for (INode function : functions) {
            INode realParent = ((IFunctionNode) function).getRealParent() == null ?
                    function.getParent() : ((IFunctionNode) function).getRealParent();

            if (realParent instanceof ClassNode)
                if (!instances.contains(realParent))
                    instances.add(realParent);
        }

        return instances;
    }

    @Override
    public void generateUUTSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode uutRoot = new RootNode(NodeType.UUT);
        uutRoot.addElement(fn);
        root.addElement(uutRoot);
    }

    @Override
    public void generateStubSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode dontStubRoot = new RootNode(NodeType.DONT_STUB);
        RootNode stubRoot = new RootNode(NodeType.STUB);

        for (Dependency d : fn.getDependencies()) {
            if (d instanceof FunctionCallDependency && ((FunctionCallDependency) d).fromNode(fn)) {
                INode referNode = d.getEndArrow();
//                if (referNode instanceof FunctionNode)
//                    dontStubRoot.addElement(referNode);
//                else if (referNode instanceof DefinitionFunctionNode)
                    stubRoot.addElement(referNode);
            }
        }

        root.addElement(dontStubRoot);
        root.addElement(stubRoot);
    }
}

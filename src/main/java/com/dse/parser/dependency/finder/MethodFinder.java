package com.dse.parser.dependency.finder;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.*;
import com.dse.resolver.NewTypeResolver;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.DefinitionFunctionNodeCondition;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.search.condition.StructurevsTypedefCondition;
import com.dse.testdata.object.IDataNode;
import com.dse.util.*;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Find method
 *
 * @author DucAnh
 */
public class MethodFinder {
    public boolean ignoreArgsLength = false;
    public boolean ignoreArgsType = false;

    private static final List<String> BASIC_TYPES =
            VariableTypeUtils.getAllBasicFieldNames(
                    VariableTypeUtils.BASIC.NUMBER.class,
                    VariableTypeUtils.BASIC.STDINT.class,
                    VariableTypeUtils.BASIC.CHARACTER.class);

    /**
     * Node in the structure that contains the searched function
     */
    private INode context;

    public MethodFinder(INode context) {
        this.context = context;
    }

    public MethodFinder(INode context, int iterator) {
        this.context = context;
        this.iterator = iterator;
    }

    public List<INode> find(String simpleFunctionName) {
        List<INode> output = new ArrayList<>();
        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();

        for (Level l : spaces)
            for (INode n : l) {

                List<INode> completeFunctions = Search.searchNodes(n, new FunctionNodeCondition());
                for (INode function : completeFunctions) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        output.add(function);
                }

                List<INode> onlyDefinedFunction = Search.searchNodes(n, new DefinitionFunctionNodeCondition());
                for (INode function : onlyDefinedFunction) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        output.add(function);
                }
            }
        return output;
    }

    public List<INode> find(String simpleFunctionName, int paramater) {
        if (ignoreArgsLength)
            return find(simpleFunctionName);

        List<INode> output = new ArrayList<>();
        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();

        for (Level l : spaces)
            for (INode n : l) {

                List<INode> completeFunctions = Search.searchNodes(n, new FunctionNodeCondition());
                for (INode function : completeFunctions) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        if (((AbstractFunctionNode) function).getArguments().size() == paramater)
                            output.add(function);
                }

                List<INode> onlyDefinedFunction = Search.searchNodes(n, new DefinitionFunctionNodeCondition());
                for (INode function : onlyDefinedFunction) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        if (((DefinitionFunctionNode) function).getArguments().size() == paramater)
                            output.add(function);
                }
            }
        return output;
    }

    public INode find(String simpleFunctionName, IASTInitializerClause[] params) {
        if (ignoreArgsType) {
            List<INode> result = find(simpleFunctionName, params.length);
            return (result.isEmpty() ? null : result.get(0));
        }
        if (ignoreArgsLength) {
            List<INode> result = find(simpleFunctionName);
            return result.isEmpty() ? null : result.get(0);
        }

        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();
        for (Level l : spaces)
            for (INode n : l) {
                List<INode> completeFunctions = Search.searchNodes(n, new AbstractFunctionNodeCondition());
                for (INode function : completeFunctions) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        if (isSameDeclare(params, ((AbstractFunctionNode) function).getArguments()))
                            return function;
                }

                List<INode> onlyDefinedFunction = Search.searchNodes(n, new DefinitionFunctionNodeCondition());
                for (INode function : onlyDefinedFunction) {
                    String name = getFunctionSimpleName(function, n);

                    if (name.equals(simpleFunctionName))
                        if (isSameDeclare(params, ((DefinitionFunctionNode) function).getArguments()))
                            return function;
                }

                SystemLibraryRoot libraryRoot = Environment.getInstance().getSystemLibraryRoot();
                if (libraryRoot != null) {
                    List<INode> libraries = Search.searchNodes(libraryRoot, new AbstractFunctionNodeCondition());
                    libraries.removeIf(f -> f.getParent() instanceof ICommonFunctionNode);

                    for (INode function : libraries) {
                        String name = function.getName().replaceAll("\\(.*\\)", "");

                        if (name.equals(simpleFunctionName) || simpleFunctionName.equals(VariableTypeUtils.STD_SCOPE + name))
                            if (isSameDeclare(params, ((AbstractFunctionNode) function).getArguments()))
                                return function;
                    }
                }
            }
        return null;
    }

    public INode find(IASTFunctionCallExpression expression) {
        String funcName = getFunctionSimpleName(expression);
        IASTInitializerClause[] args = expression.getArguments();
        return find(funcName, args);
    }

    public String getFunctionSimpleName(IASTFunctionCallExpression expr) {
        IASTExpression ex = expr.getFunctionNameExpression();
        String funcName = null;
        if (ex instanceof IASTFieldReference) {
            String type = new NewTypeResolver(context).exec(((IASTFieldReference) ex).getFieldOwner());

            if (type == null)
                return null;

            // Delete 2 times
            type = VariableTypeUtils.deleteStorageClasses(type);
            type = VariableTypeUtils.deleteStorageClasses(type);

            funcName = type.replaceAll("[ *]", "")
                    + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS
                    + ((IASTFieldReference) ex).getFieldName().toString();

            VariableSearchingSpace space = new VariableSearchingSpace(context);
            List<INode> possibleTypeNode = space.search(type, new StructurevsTypedefCondition());
            INode typeNode = null;
            if (!possibleTypeNode.isEmpty()) {
                typeNode = possibleTypeNode.get(0);
                if (possibleTypeNode.size() > 1)
                    System.out.println("Multiple structure node found " + type);
            }

            if (typeNode instanceof StructureNode) {
                INode parent = typeNode.getParent();
                // TODO: structure define in another file
                while (parent != null) {
                    // structure define in a namespace
                    if (parent instanceof NamespaceNode || parent instanceof StructureNode) {
                        String namespace = parent.getName();
                        if (!(parent instanceof ClassNode && ((ClassNode) parent).isTemplate()
                                && funcName.startsWith(namespace)))
                            funcName = namespace + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + funcName;
                    }

                    parent = parent.getParent();
                }
            }
        } else if (ex instanceof IASTIdExpression) {
            funcName = ((IASTIdExpression) ex).getName().toString();
        }

        funcName = TemplateUtils.deleteTemplateParameters(funcName);

        return funcName;
    }

    private String getFunctionSimpleName(INode function, INode root) {
        StringBuilder parent = new StringBuilder();

        String name = null;
        if (function instanceof ICommonFunctionNode)
            name = ((ICommonFunctionNode) function).getSimpleName();

        INode realParent = function.getParent();

        if (function instanceof AbstractFunctionNode) {
            INode tmpRealParent = ((AbstractFunctionNode) function).getRealParent();
            if (tmpRealParent != null)
                realParent = tmpRealParent;
        }

        if (realParent instanceof StructureNode || realParent instanceof NamespaceNode) {
            parent = new StringBuilder(realParent.getName());
            INode parentNode = realParent.getParent();
            while (parentNode != null && parentNode != root) {
                if (parentNode instanceof StructureNode || parentNode instanceof NamespaceNode) {
                    parent.insert(0, parentNode.getName() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
                }
                parentNode = parentNode.getParent();
            }
        }

        /*
         * Method call to a method in a same class
         */
        if (!name.contains(parent.toString()) && !parent.toString().endsWith(root.getName()))
            name = parent + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + name;

        return name;
    }

    private boolean isSameDeclare(IASTInitializerClause[] params, List<IVariableNode> args) {
        /*
         * function(void) case
         */
        if (params.length == 0 && args.size() == 1)
            if (getArgType(args.get(0)).equals(VariableTypeUtils.VOID_TYPE.VOID))
                return true;

        if (params.length != args.size())
            return false;

        for (int i = 0; i < params.length; i++)
            if (!isSameType(params[i], args.get(i)))
                return false;

        return true;
    }


    private boolean isSameType(IASTInitializerClause param, IVariableNode arg) {
        String paramType = getParamType(param);

//        if (TemplateUtils.isTemplate(paramType)) {
        VariableNode tempVarNode = new VariableNode();
        IASTNode ast = Utils.convertToIAST(String.format("%s %s;", paramType, arg.getName()));
        if (ast instanceof IASTDeclarationStatement)
            ast = ((IASTDeclarationStatement) ast).getDeclaration();
        tempVarNode.setAST(ast);
        tempVarNode.setAbsolutePath(arg.getAbsolutePath());
        paramType = VariableTypeUtils.getFullRawType(tempVarNode);
        paramType = deleteKeyWord(paramType);
//            paramType = TemplateUtils.deleteTemplateParameters(paramType);
//        }

//        String argType = getArgType(arg);
        String argType = VariableTypeUtils.getFullRawType((VariableNode) arg);

        argType = deleteKeyWord(argType);

        // Check Template function with template arguments
        if (arg.getParent() instanceof ICommonFunctionNode) {
            ICommonFunctionNode called = (ICommonFunctionNode) arg.getParent();

            if (called.isTemplate()) {
                String[] parameters = TemplateUtils.getTemplateParameters(called);
                if (arg.resolveCoreType() == null) {
                    paramType = deleteKeyWord(paramType);
                    paramType = paramType.replace(IDataNode.REFERENCE_OPERATOR, "");
                    paramType = paramType.replaceAll(IRegex.POINTER, "[]");

                    String rawType = deleteKeyWord(arg.getRawType());
                    rawType = rawType.replaceAll(IRegex.POINTER, "[]");
                    rawType = rawType.replace(IDataNode.REFERENCE_OPERATOR, "");
                    rawType = rawType.replace("[]", "\\[\\]");

                    for (String paramater : parameters)
                        rawType = rawType.replace(paramater, ".+");

                    return paramType.matches(rawType);
                }
            }
        }

        if (paramType.equals(argType) /*|| paramType.equals(deleteKeyWord(arg.getFullType()))*/)
            return true;

        // Casting case
        return BASIC_TYPES.contains(paramType) && BASIC_TYPES.contains(argType);
    }

    private String getArgType(IVariableNode arg) {
        /*
         * TODO: giar dinhj laf ko typedef
         */
        String fullCoreType = arg.getFullType();
        if (fullCoreType.startsWith(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS))
            fullCoreType = fullCoreType.substring(2);

        String coreType = arg.getCoreType();

        String argType = arg.getRealType();

        argType = argType.replace(coreType, fullCoreType);

        argType = deleteKeyWord(argType);

        return argType;
    }

    private int iterator = 0;

    private String getParamType(IASTInitializerClause param) {
        if (!(param instanceof IASTExpression))
            return null;

//        String paramType = ((IASTExpression) param).getExpressionType().toString();
//
//        if (shoudCheck5652Flag) {
//            // Case: struct ... (C)
//            if (paramType.contains(CDT_5652_FLAG))
//                paramType = paramType.replace(CDT_5652_FLAG, "");
//        }
//
//        if (shouldCheckUnresolveType) {
//            // Case: Typedef, Data type in another file, ...
//            // In several cases, CDT has trouble in resolving the real type of parameters.
//            // For example, ...
//            if (paramType.contains(UNRESOLVED_TYPE_FLAG) || paramType.contains(PROBLEM_TYPE_FLAG))
//                paramType = resolveProblemType(param, paramType);
//        }

        String paramType = new NewTypeResolver(context, iterator).exec((IASTExpression) param);
        iterator++;

        paramType = deleteKeyWord(paramType);

        return paramType;
    }

    public String deleteKeyWord(String type) {
        type = VariableTypeUtils.deleteUnionKeyword(type);
        type = VariableTypeUtils.deleteStructKeyword(type);
        type = VariableTypeUtils.deleteStorageClasses(type);
        type = VariableTypeUtils.deleteSizeFromArray(type);
        type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);
        type = type.replaceAll(" \\*", "*")
                .replaceAll(" \\[]", "*").replaceAll("\\[]", "*");

        return type;
    }
}

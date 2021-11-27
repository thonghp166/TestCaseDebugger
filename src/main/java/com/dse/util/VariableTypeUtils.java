package com.dse.util;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.TypeDependency;
import com.dse.parser.object.INode;
import com.dse.parser.object.IVariableNode;
import com.dse.parser.object.VariableNode;
import com.dse.search.Search;
import com.dse.search.condition.ClassNodeCondition;
import com.dse.search.condition.EnumNodeCondition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by DucToan on 27/07/2017
 */
public class VariableTypeUtils {
    public static final String MULTI_LEVEL_POINTER = "(\\s*\\*)+\\s*$";
    public static final String MULTI_DIMENSIONAL_ARRAY_INDEX = "\\s*(\\[[a-zA-Z0-9\\s]*\\])+\\s*$";

    public static final String REFERENCE = "&";
    public static final String ONE_LEVEL = "*";
    public static final String TWO_LEVEL = "**";
    public static final char POINTER_CHAR = '*';
//    public static final String TemplateUtils.OPEN_TEMPLATE_ARG = "<";
//    public static final String CLOSE_TEMPLATE_ARG = ">";
    public static final String ONE_DIMENSION = "[]";
    public static final String TWO_DIMENSION = "[][]";
    public static final String THROW = "throw";
    public static final String UNSUPPORTED = "undefined";
    public static final String STD_SCOPE = "std::";

    /**
     * Remove redundant keywords but still keeps the type of variable
     * "int&" ---> "int"
     * "const int[3]" ---> "int[3]"
     * "union XXX" ---> "XXX"
     * "struct XXX" ---> "XXX"
     * "const int" ----> "int"
     *
     * @param rawType
     * @return
     */
    public static String removeRedundantKeyword(String rawType){
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
//        rawType = deleteSizeFromArray(rawType);
        rawType = VariableTypeUtils.deleteReferenceOperator(rawType);
        rawType = VariableTypeUtils.deleteVirtualAndInlineKeyword(rawType);
        rawType = rawType.trim();
        return rawType;
    }

    /**
     * Delete const, static, register, extern, mutable
     *
     * @param type raw type
     * @return new type
     */
    public static String deleteStorageClasses(String type) {
        type = type.replaceAll("\\bconst\\s+", "").replaceAll("\\s+const$", "")
                .replaceAll("\\s+const&", "&").replaceAll("\\s+const\\*", "*");
        for (int i = 0; i < 2; i++) {
            type = type.replaceAll("^static\\s*", "");
            type = type.replaceAll("^register\\s*", "");
            type = type.replaceAll("^extern\\s*", "");
            type = type.replaceAll("^mutable\\s*", "");
            type = type.replaceAll("^constexpr\\s*", "");
        }
        return type;
    }

    /**
     * Ex:"union Color x"-----delete----> "Color x"
     *
     * @return new type
     */
    public static String deleteUnionKeyword(String type) {
        return type.replaceAll("^union\\s*", "");
    }

    public static String deleteClassKeyword(String type) {
        return type.replaceAll("^class\\s*", "");
    }

    public static String deleteReferenceOperator(String type) {
        return type.replaceAll(IRegex.REFERENCE_OPERATOR, SpecialCharacter.EMPTY);
    }

    public static VariableNode cloneAndReplaceType(String type, VariableNode source, INode classNode) {
        // Clone current corresponding variable
        VariableNode correspondingVar = (VariableNode) source.clone();

        correspondingVar.setCorrespondingNode(source.getCorrespondingNode());

        // Set type of clone variable node
        correspondingVar.setRawType(type);
        correspondingVar.setReducedRawType(type);

        String coreType = type;
        if (TemplateUtils.isTemplateClass(type))
            coreType = TemplateUtils.getCoreType(type);
        correspondingVar.setCoreType(coreType);

//        if (TemplateUtils.isTemplate(classNode.getName()))
//            classNode = Search.searchNodes(Environment.getInstance().getProjectNode(), new ClassNodeCondition(), classNode.getAbsolutePath()).get(0);
        // search on Windows environment
        if (TemplateUtils.isTemplate(classNode.getName())) {
            List<INode> nodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new ClassNodeCondition());
            for (INode node : nodes) {
                if (node.getAbsolutePath().equals(classNode.getAbsolutePath())) {
                    classNode = node;
                    break;
                }
            }
        }

        List<Dependency> newDependencies = new ArrayList<>();
        List<Dependency> dependencies = correspondingVar.getDependencies();

        for (Dependency dependency : dependencies) {
            // Update new type dependency
            if (dependency instanceof TypeDependency &&
                    dependency.getStartArrow().getAbsolutePath().equals(correspondingVar.getAbsolutePath())) {
                TypeDependency newTypeDependency = new TypeDependency(correspondingVar, classNode);
                newDependencies.add(newTypeDependency);
                classNode.getDependencies().add(newTypeDependency);
            } else
                newDependencies.add(dependency);
        }

//        newDependencies.addAll(dependencies);

        correspondingVar.setDependencies(newDependencies);
        correspondingVar.setTypeDependencyState(true);

        return correspondingVar;
    }

    public static String getFullRawType(VariableNode variableNode) {
        String rawType = variableNode.getRawType();

        if (TemplateUtils.isTemplate(rawType)) {
            return TemplateUtils.getTemplateFullRawType(variableNode);
        }

        String fullType = variableNode.getFullType();

        String coreType = variableNode.getCoreType();

        if (fullType.contains(coreType) && rawType.contains(coreType)) {
            rawType = rawType.replace(coreType, fullType);
        }

        rawType = deleteStorageClasses(rawType);
        rawType = deleteVirtualAndInlineKeyword(rawType);
        rawType = deleteReferenceOperator(rawType);

        return rawType;
    }

//    public static void main(String[] args) {
//        ProjectParser parser = new ProjectParser(new File("/mnt/e/LAB/akautauto/datatest/lamnt/TemplateClassFullType"));
//        parser.setExpandTreeuptoMethodLevel_enabled(true);
//        parser.setCpptoHeaderDependencyGeneration_enabled(true);
//
//        ProjectNode root = parser.getRootTree();
//
//        Environment.setProjectNode(root);
//
//        List<INode> functions = Search.searchNodes(root, new FunctionNodeCondition());
//
//        for (INode function : functions) {
//            List<Level> space = new VariableSearchingSpace(function).getSpaces();
//
//            VariableNode variableNode = ((VariableNode)function.getChildren().get(0));
//            String fullType = VariableTypeUtils.getFullRawType(variableNode);
//
//            System.out.println();
//        }
//
//    }

//    public static String getTemplateFullRawType(VariableNode variableNode) {
//        String rawType = variableNode.getRawType();
//
//        String variablePath = variableNode.getAbsolutePath();
//
//        int closeBracketPos = variablePath.lastIndexOf(')') + 1;
//        String functionPath = variablePath.substring(0, closeBracketPos);
//
//        List<SearchCondition> conditions = Arrays.asList(
//                new FunctionNodeCondition(),
//                new DefinitionFunctionNodeCondition()
//        );
//
//        List<INode> functions = Search.searchNodes(Environment.getProjectNode(), conditions);
//
//        for (INode function : functions) {
//            if (function.getAbsolutePath().equals(functionPath)) {
//                List<Level> space = new VariableSearchingSpace(function).generateExtendSpaces();
//                return recursiveGetFullRawType(rawType, space);
//            }
//
//        }
//
//        return rawType;
//    }

//    public static String getCoreTypeWithTemplate(String rawType) {
//        String coreType = rawType.replaceAll(IRegex.POINTER, "")
//                .replaceAll(IRegex.ARRAY_INDEX, "");
//
//        if (rawType.contains(CLOSE_TEMPLATE_ARG)) {
//            int index = rawType.lastIndexOf(CLOSE_TEMPLATE_ARG) + 1;
//
//            coreType = rawType.substring(0, index) + rawType.substring(index)
//                    .replaceAll(IRegex.POINTER, "")
//                    .replaceAll(IRegex.ARRAY_INDEX, "");
//        }
//
//        return coreType;
//    }

//    public static String recursiveGetFullRawType(String rawType, List<Level> space) {
//        // Get Core Type
//        String coreType = getCoreTypeWithTemplate(rawType);
//
//        // Get type operator
//        String typeOperator = rawType.substring(coreType.length());
//        if (isTemplateClass(rawType)) typeOperator = "";
//
//        String fullType = coreType;
//
//        IASTNode astNode = Utils.convertToIAST(coreType);
//
//        if (astNode instanceof IASTIdExpression) {
//            IASTName fName = ((IASTIdExpression) astNode).getName();
//
//            String className = "";
//
//            if (fName instanceof ICPPASTQualifiedName) {
//                ICPPASTNameSpecifier[] qualifier = ((ICPPASTQualifiedName) fName).getQualifier();
//                for (ICPPASTNameSpecifier specifier : qualifier)
//                    className += specifier.getRawSignature() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS;
//
//                fName = fName.getLastName();
//            }
//
//            if (fName instanceof ICPPASTTemplateId) {
//                String templateName = ((ICPPASTTemplateId) fName).getTemplateName().getRawSignature();
//                className += templateName;
//            } else if (fName != null) {
//                className += fName.getRawSignature();
//            }
//
//            String classRelativePath = className.replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, File.separator);
//
//            List<INode> possibleNodes = Search.searchInSpace(space, new ClassNodeCondition(), classRelativePath);
//
//            if (!possibleNodes.isEmpty()) {
//                if (possibleNodes.size() == 1)
//                    fullType = Search.getScopeQualifier(possibleNodes.get(0));
//            } else {
//                fullType = className;
//            }
//
//            if (fName instanceof ICPPASTTemplateId) {
//                fullType += TemplateUtils.OPEN_TEMPLATE_ARG;
//
//                IASTNode[] templateArguments = ((ICPPASTTemplateId) fName).getTemplateArguments();
//                for (IASTNode templateArgument : templateArguments) {
//                    String templateArgRawType = templateArgument.getRawSignature();
//
//                    fullType += recursiveGetFullRawType(templateArgRawType, space);
//                    fullType += ", ";
//                }
//
//                fullType += CLOSE_TEMPLATE_ARG;
//
//                fullType = fullType.replace(", >", ">");
//            }
//        }
//
//        return fullType + typeOperator;
//    }

//    public static String getTemplateClassCoreType(String rawType) {
//        String coreType = rawType + "";
//
//        if (rawType.contains(TemplateUtils.OPEN_TEMPLATE_ARG) && rawType.contains(CLOSE_TEMPLATE_ARG)) {
//            int begin = rawType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG) + 1;
//            int end = rawType.lastIndexOf(CLOSE_TEMPLATE_ARG);
//
//            coreType = coreType.substring(begin, end);
//        }
//
//        return coreType;
//    }

    public static String getSimpleRawType(String rawType) {
        // Step: get the tail of the type
        // For example, 'A::B::C" ---> "C"
        while (rawType.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)) {
            int index = rawType.indexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS) + 2;
            String temp = rawType.substring(0, index);
            if (temp.contains("<")) {
                break;
            } else {
                rawType = rawType.substring(index);
            }
        }

        // Step: Clean up raw type
        rawType = deleteStorageClasses(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteSizeFromArray(rawType);
        rawType = deleteVirtualAndInlineKeyword(rawType);

        return rawType;
    }

    public static String getSimpleRawType(VariableNode variableNode) {
        String rawType = variableNode.getRawType();
        return getSimpleRawType(rawType);
    }

//    public static String getTemplateName(VariableNode variableNode) {
//        String templateClassName = "";
//
//        IASTParameterDeclaration parameterDeclaration = null;
//
//        if (variableNode.getAST() instanceof IASTParameterDeclaration)
//            parameterDeclaration = (IASTParameterDeclaration) variableNode.getAST();
//
//        if (parameterDeclaration == null || variableNode instanceof CloneVariableNode) {
//            String type = variableNode.getRawType();
//            String[] elements = type.split(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
//            String simpleType = elements[elements.length - 1];
//            templateClassName = simpleType.substring(0, simpleType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG));
//        } else {
//            IASTDeclSpecifier declSpecifier = parameterDeclaration.getDeclSpecifier();
//
//            if (declSpecifier instanceof IASTNamedTypeSpecifier) {
//                IASTName name = ((IASTNamedTypeSpecifier) declSpecifier).getName();
//                if (name instanceof ICPPASTQualifiedName)
//                    name = name.getLastName();
//                if (name instanceof ICPPASTTemplateId)
//                    templateClassName = ((ICPPASTTemplateId) name).getTemplateName().toString();
//            }
//        }
//
//        return templateClassName;
//    }

    public static String getElementIndex(String rawType) {
        rawType =rawType.trim();

        StringBuilder elementIndex = new StringBuilder();

        while (rawType.endsWith(SpecialCharacter.CLOSE_SQUARE_BRACE + SpecialCharacter.EMPTY)) {
            int index = rawType.lastIndexOf(SpecialCharacter.OPEN_SQUARE_BRACE);
            elementIndex.insert(0, rawType.substring(index));
            rawType = rawType.substring(0, index);
        }

        return elementIndex.toString();
    }

    /**
     * Get type of variable
     *
     * @param rawType raw type of variable (may contain const, static, register,
     *                extern, mutable, &, *, **, [], [][],union, enum, struct, etc.)
     *                Ex1: const int& <br/>
     *                Ex2: struct SV
     */
    public static String getType(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        if (rawType.equals(VOID_TYPE.VOID))
            return VOID_TYPE.VOID;

        for (String type : getAllBasicFieldNames(BASIC.class)) {
            String[] types = new String[]{type,
                    type + REFERENCE,
                    type + ONE_LEVEL,
                    type + ONE_DIMENSION,
                    type + TWO_LEVEL,
                    type + TWO_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return typeItem;

        }

        if (rawType.matches(THROW))
            return THROW;

        if (rawType.matches(STRUCTURE.ONE_LEVEL_STRUCTURE_REGEX))
            return STRUCTURE.ONE_LEVEL_STRUCTURE_REGEX;

        else if (rawType
                .matches(STRUCTURE.TWO_LEVEL_STRUCTURE_REGEX))
            return STRUCTURE.TWO_LEVEL_STRUCTURE_REGEX;

        else if (rawType
                .matches(STRUCTURE.SIMPLE_STRUCTURE_REGEX))
            return STRUCTURE.SIMPLE_STRUCTURE_REGEX;

        else if (rawType
                .matches(STRUCTURE.ONE_DIMENSION_STRUCTURE_REGEX))
            return STRUCTURE.ONE_DIMENSION_STRUCTURE_REGEX;

        else if (rawType
                .matches(STRUCTURE.TWO_DIMENSION_STRUCTURE_REGEX))
            return STRUCTURE.TWO_DIMENSION_STRUCTURE_REGEX;

        return UNSUPPORTED;
    }

    public static String getType(IVariableNode var) {
        String rawType = var.getRawType();
        return getType(rawType);
    }

    public static boolean isNullPtr(String type) {
        return type.equals("nullptr_t");
    }

    public static boolean isSTLArray(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.ARRAY.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }

    public static boolean isPair(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.PAIR.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }

    public static boolean isMap(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.MAP.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }



    public static boolean isPointer(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        rawType = TemplateUtils.deleteTemplateParameters(rawType);

        if (rawType.contains("[") || rawType.contains("]"))
            return false;

        return rawType.contains(ONE_LEVEL);
    }

    public static boolean isSet(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.SET.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }

    public static boolean isStack(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.STACK.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }

    public static boolean isQueue(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(STL.QUEUE.class)) {
            String[] types = new String[]{type, STD_SCOPE + type};
            for (String typeItem : types)
                if (rawType.startsWith(typeItem + TemplateUtils.OPEN_TEMPLATE_ARG))
                    return true;

        }
        return false;
    }

    /**
     * Check whether is basic type. Ex: int, int&
     *
     * @param rawType of variable
     */
    public static boolean isBasic(String rawType) {
        return isChBasic(rawType) || isNumBasic(rawType) || isStrBasic(rawType);
    }

    public static boolean isBoolMultiLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.BOOLEAN.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;
        }
        return false;
    }

    public static boolean isChMultiLevel(String rawType) {
        if (isVoidPointer(rawType))
            return false;
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;
        }
        return false;
    }

    public static boolean isChOneLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type + ONE_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isStrOneLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            String[] types = new String[]{type + ONE_LEVEL, STD_SCOPE + type + ONE_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

//    public static boolean isTemplate(VariableNode node) {
//        INode parent = node.getParent();
//
//        IASTNode astNode = null;
//        if (parent instanceof AbstractFunctionNode) {
//            astNode = ((AbstractFunctionNode) parent).getAST();
//        } else if (parent instanceof DefinitionFunctionNode) {
//            astNode = ((DefinitionFunctionNode) parent).getAST();
//        }
//
//        while (astNode != null) {
//            if (astNode instanceof ICPPASTTemplateDeclaration) {
//                ICPPASTTemplateParameter[] params = ((ICPPASTTemplateDeclaration) astNode)
//                        .getTemplateParameters();
//
//                for (ICPPASTTemplateParameter param : params){
//                    String templateType = param.toString();
//                    String variableType = node.getRawType();
//                    if (templateType.equals(variableType))
//                        return true;
//                }
//
//            }
//            astNode = astNode.getParent();
//        }
//
//        return false;
//    }

    public static boolean isChTwoLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type + TWO_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;

    }

    public static boolean isStrTwoLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            String[] types = new String[]{type + TWO_LEVEL, STD_SCOPE + type + TWO_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;

    }

    public static boolean isChOneDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type + ONE_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;

    }

    public static boolean isStrOneDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            String[] types = new String[]{type + ONE_DIMENSION, STD_SCOPE + type + ONE_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isCh(String rawType) {
        return isChBasic(rawType) || isChOneDimension(rawType)
                || isChOneLevel(rawType) || isChTwoLevel(rawType);
    }

    public static boolean isChBasic(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type, type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

//    public static boolean isNumMultipleDim(String rawType) {
//        String coretype = rawType.replaceAll(IRegex.ARRAY_INDEX, "").replace(IRegex.POINTER, "");
//
//        return isNum(coretype);
//    }
//
//    public static boolean isNotReturn(String rawType) {
//        return getType(rawType).equals(VOID_TYPE.VOID);
//    }

    /**
     * Return true if the type of variable is number or character
     *
     * @param rawType Ex: const int&
     */
    public static boolean isNumBasic(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            String[] types = new String[]{type, type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }

        return isStdInt(rawType);
    }

    public static boolean isStdInt(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.STDINT.class)) {
            String[] types = new String[]{type, type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    //-----------------------------------------
    // STRING - BEGIN
    //-----------------------------------------
    public static boolean isStrBasic(String rawType) {
        rawType = removeRedundantKeyword(rawType);
        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            String[] types = new String[]{type, STD_SCOPE + type, type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isStr(String rawType) {
        return isStrBasic(rawType) || isStrMultiLevel(rawType)
                || isStrMultiDimension(rawType);
    }

    public static boolean isStrMultiLevel(String rawType) {
        rawType = removeRedundantKeyword(rawType);
        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;
        }
        return false;
    }

    public static boolean isStrMultiDimension(String rawType) {
        rawType = removeRedundantKeyword(rawType);
        for (String type : getAllBasicFieldNames(BASIC.STRING.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;
    }
    //-----------------------------------------
    // STRING - END
    //-----------------------------------------

    public static boolean isNumOneLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            String[] types = new String[]{type + ONE_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    /**
     * Ex: const int **
     *
     * @param rawType of variable
     */
    public static boolean isNumTwoLevel(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            String[] types = new String[]{type + TWO_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumMultiLevel(String rawType) {
        if (isVoidPointer(rawType))
            return false;
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;

        }
        return false;
    }

    public static boolean isNumIntegerMultiLevel(String rawType) {
        if (isVoidPointer(rawType))
            return false;
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.INTEGER.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;

        }
        return false;
    }

    public static boolean isNumFloatMultiLevel(String rawType) {
        if (isVoidPointer(rawType))
            return false;
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            if (rawType.matches(type + MULTI_LEVEL_POINTER))
                return true;

        }
        return false;
    }

    public static boolean isChMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;

    }

    public static boolean isBoolMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.BOOLEAN.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;
    }

    public static boolean isNumMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;
    }

    public static boolean isNumIntergerMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.INTEGER.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;
    }

    public static boolean isNumFloatMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            if (rawType.matches(type + MULTI_DIMENSIONAL_ARRAY_INDEX))
                return true;
        }
        return false;
    }

    public static boolean isNumOneDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            String[] types = new String[]{type + ONE_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumTwoDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.class)) {
            String[] types = new String[]{type + TWO_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNum(String rawType) {
        return isNumBasic(rawType)
                || isNumOneDimension(rawType)
                || isNumOneLevel(rawType)
                || isNumTwoLevel(rawType);
    }

    public static boolean isNumFloat(String rawType) {
        return isNumBasicFloat(rawType)
                || isNumOneLevelFloat(rawType)
                || isNumTwoLevelFloat(rawType)
                || isNumOneDimensionFloat(rawType)
                || isNumTwoDimensionFloat(rawType);

    }
    public static boolean isNumBasicInteger(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.INTEGER.class)) {
            String[] types = new String[]{type,
                    type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }
    public static boolean isNumBasicFloat(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String[] types = new String[]{type,
                    type + REFERENCE};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumOneDimensionFloat(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String[] types = new String[]{type + ONE_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumTwoDimensionFloat(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String[] types = new String[]{type + TWO_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumOneLevelFloat(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String[] types = new String[]{type + ONE_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isNumTwoLevelFloat(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String[] types = new String[]{type + TWO_LEVEL};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

//    public static String removeTemplateParameter(String rawType) {
//        String rawTypeWithoutTemplate = rawType + "";
//        int openPos = rawType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG);
//        int closePos = rawType.lastIndexOf(CLOSE_TEMPLATE_ARG);
//        if (openPos > 0 && closePos > 0)
//            rawTypeWithoutTemplate = rawType.substring(0, openPos) + rawType.substring(closePos + 1);
//
//        return rawTypeWithoutTemplate;
//    }

    public static boolean isOneDimension(String rawType) {
        String rawTypeWithoutTemplate = TemplateUtils.deleteTemplateParameters(rawType);

        return isOneDimensionBasic(rawType) || isStructureOneDimension(rawType) || isTemplateOneDimension(rawType)
                || Utils.getIndexOfArray(deleteStorageClasses(rawTypeWithoutTemplate)).size() == 1
                || Utils.getIndexOfArray(deleteStorageClasses(rawTypeWithoutTemplate)).size() > 2;
    }

    public static boolean isOneDimensionBasic(String rawType) {
        return isChOneDimension(rawType) || isStrOneDimension(rawType)
                || isNumOneDimension(rawType);
    }

    public static boolean isTwoDimension(String rawType) {
        String rawTypeWithoutTemplate = TemplateUtils.deleteTemplateParameters(rawType);

        return isTwoDimensionBasic(rawType) || isStructureTwoDimension(rawType) || isTemplateTwoDimension(rawType)
                || Utils.getIndexOfArray(deleteStorageClasses(rawTypeWithoutTemplate)).size() == 2;
    }

    public static boolean isMultipleDimension(String rawType) {
        String rawTypeWithoutTemplate = TemplateUtils.deleteTemplateParameters(rawType);
        return Utils.getIndexOfArray(deleteStorageClasses(rawTypeWithoutTemplate)).size() >= 2;
    }

    public static boolean isTwoDimensionBasic(String rawType) {
        return isChTwoDimension(rawType) || isStrTwoDimension(rawType)
                || isNumTwoDimension(rawType);
    }

    public static boolean isOneLevel(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        return isOneLevelBasic(rawType)
                || isStructureOneLevel(rawType)
                || isTemplateOneLevel(rawType);
    }

    public static boolean isTemplateOneLevel(String rawType) {
        rawType = rawType.replace(" ", "");
        return rawType.contains(">*") && !rawType.contains(TWO_LEVEL);
    }

    public static boolean isTemplateTwoLevel(String rawType) {
        rawType = rawType.replace(" ", "");
        return rawType.contains(">**");
    }

    public static boolean isTemplateOneDimension(String rawType) {
        if (TemplateUtils.isTemplate(rawType)) {
            int begin = rawType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG);
            int end = rawType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1;

            rawType = rawType.substring(0, begin) + rawType.substring(end);

            return isStructureOneDimension(rawType);

        }

        return false;
    }

    public static boolean isTemplateTwoDimension(String rawType) {
        if (TemplateUtils.isTemplate(rawType)) {
            int begin = rawType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG);
            int end = rawType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1;

            rawType = rawType.substring(0, begin) + rawType.substring(end);

            return isStructureTwoDimension(rawType);

        }

        return false;
    }

    public static boolean isOneLevelBasic(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        return isNumOneLevel(rawType) || isStrOneLevel(rawType)
                || isChOneLevel(rawType);
    }

    public static boolean isTwoLevel(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        return isTwoLevelBasic(rawType)
                || isStructureTwoLevel(rawType)
                || isTemplateTwoLevel(rawType);
    }

    public static boolean isTwoLevelBasic(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        return isNumTwoLevel(rawType) || isStrTwoLevel(rawType)
                || isChTwoLevel(rawType);
    }

//    public static List<String> getTemplateArguments(String rawType) {
//        List<String> templateArguments = new ArrayList<>();
//
//        // Step 1: remove blank space
//        String rType = rawType.replace(" ", "");
//
//        // Step 2: remove template type name and < > container
//        rType = rType.substring(rType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG) + 1,
//                rType.lastIndexOf(CLOSE_TEMPLATE_ARG));
//
//        String[] tempTemplateArgs = rType.split(",");
//
//        String tempCompleteArg = "";
//
//        for (String argument : tempTemplateArgs) {
//            // case float, pair<float>, vector<vector<int>>
//            if ((!argument.contains(TemplateUtils.OPEN_TEMPLATE_ARG) && !argument.contains(CLOSE_TEMPLATE_ARG))
//                || (argument.contains(TemplateUtils.OPEN_TEMPLATE_ARG) && argument.contains(CLOSE_TEMPLATE_ARG)
//                    && Utils.countCharIn(argument, '<') == Utils.countCharIn(argument, '>'))) {
//                templateArguments.add(argument);
//                tempCompleteArg = "";
//            } else {
//                tempCompleteArg += ", " + argument;
//                if (Utils.countCharIn(tempCompleteArg, '<') == Utils.countCharIn(tempCompleteArg, '>')) {
//                    templateArguments.add(tempCompleteArg + "");
//                    tempCompleteArg = "";
//                }
//            }
//        }
//
//        return templateArguments;
//    }

    /**
     * Get all fields in a class in lower case, including all fields in child
     * class
     */
    public static List<String> getAllBasicFieldNames(Class<?> c) {
        List<String> fields = new ArrayList<>();
        Field[] f = c.getFields();

        final String SEPARATE = "_";

        for (Field element : f) {
            String type = element.getName().toLowerCase();
            /*
              for "wchar_t" and "__wchar_t";
             */
            type = type.replace(SEPARATE + SEPARATE, "@");
            type = type.replace(SEPARATE, " ");
            type = type.replace("@", "_");
            fields.add(type);
        }

        for (Class<?> child : c.getClasses())
            fields.addAll(getAllBasicFieldNames(child));

        return fields;

    }

    public static List<String> getAllBasicFieldNames(Class<?>... cs) {
        List<String> fields = new ArrayList<>();

        for (Class<?> c : cs)
            fields.addAll(getAllBasicFieldNames(c));

        return fields;
    }

    /**
     * Get all fields in a class in lower case, including all fields in child
     * class
     *
     */
    public static List<Field> getAllBasicFields(Class<?> c) {
        List<Field> fields = new ArrayList<>();
        Field[] f = c.getFields();

        Collections.addAll(fields, f);

        for (Class<?> child : c.getClasses())
            fields.addAll(getAllBasicFields(child));

        return fields;

    }

    /**
     * Delete size from array variable type
     * <p>
     * Ex: int[3] ==> int[]
     *
     */
    public static String deleteSizeFromArray(String type) {
        return type.replaceAll("\\[[0-9]+\\]", "[]");
    }

    /**
     * Remove pointer *
     *
     * "int" ---> "int"
     *
     * "int*" ----> "int"
     *
     * "int**" ----> "int"
     *
     * "vector<int*>* *" ----> "vector<int*>"
     *
     * @param type
     * @return
     */
    public static String deletePointerOperator(String type){
        return type.trim().replaceAll("(\\s*\\*)+$","");
    }

    public static boolean isStructureMultiLevel(String rawType) {
        if (isVoidPointer(rawType))
            return false;
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        if (isChOneDimension(rawType) || isNumOneDimension(rawType))
            return false;
        else
            return rawType
                    .matches(STRUCTURE.MULTI_LEVEL_STRUCTURE_REGEX);
    }

    public static boolean isStructureOneLevel(String type) {
        if (isChOneLevel(type) || isNumOneLevel(type))
            return false;
        else
            return type
                    .matches(STRUCTURE.ONE_LEVEL_STRUCTURE_REGEX)
                    || type
                    .matches(STRUCTURE.ONE_LEVEL_STRUCTURE_REGEX_TYPE2);
    }

    public static boolean isStructureTwoLevel(String type) {
        if (isChTwoLevel(type) || isNumTwoLevel(type))
            return false;
        else
            return type
                    .matches(STRUCTURE.TWO_LEVEL_STRUCTURE_REGEX);
    }

    public static boolean isStructureMultiDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);

        if (isChOneDimension(rawType) || isNumOneDimension(rawType))
            return false;
        else
            return rawType
                    .matches(STRUCTURE.MULTI_DIMENSION_STRUCTURE_REGEX);
    }

    public static boolean isStructureOneDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        if (isChOneDimension(rawType) || isNumOneDimension(rawType))
            return false;
        else
            return rawType
                    .matches(STRUCTURE.ONE_DIMENSION_STRUCTURE_REGEX);
    }

    public static boolean isStructureTwoDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        if (isChTwoDimension(rawType) || isNumTwoDimension(rawType))
            return false;
        else
            return rawType.matches(STRUCTURE.TWO_DIMENSION_STRUCTURE_REGEX);
    }

    public static boolean isStructureSimple(String type) {
        type = deleteUnionKeyword(type);
        type = deleteStructKeyword(type);
        type = deleteStorageClasses(type);
        type = type.trim();
        if (isBasic(type) || isVoid(type) || type.equals("auto"))
            return false;
        else
            return type.matches(STRUCTURE.SIMPLE_STRUCTURE_REGEX);
    }

//    public static boolean isTemplateClass(String type) {
//        type = deleteUnionKeyword(type);
//        type = deleteStructKeyword(type);
//        type = deleteStorageClasses(type);
//
//        return type.contains(TemplateUtils.OPEN_TEMPLATE_ARG) && type.endsWith(CLOSE_TEMPLATE_ARG);
//    }


    public static boolean isVoidPointer(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = rawType.trim();
        if (rawType.matches("\\s*void\\s*\\*\\s*$"))
            return true;
        else
            return false;
    }

    public static boolean isVoid(String rawType) {
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(VOID_TYPE.class)) {
            String[] types = new String[]{type};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isBoolBasic(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);


        String[] types = new String[]{BASIC.BOOLEAN.BOOL, BASIC.BOOLEAN.BOOL + REFERENCE};
        for (String typeItem : types)
            if (rawType.equals(typeItem))
                return true;

        return false;
    }

    public static String deleteStructKeyword(String type) {
        return type.replaceAll("^struct\\s*", "");
    }

    public static String deleteVirtualAndInlineKeyword(String type) {
        String out = type.replaceAll("\\bvirtual\\s+", "");
        out = out.replaceAll("\\binline\\s+", "");
        return out;
    }

    public static boolean isChTwoDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type + TWO_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    public static boolean isStrTwoDimension(String rawType) {
        rawType = deleteUnionKeyword(rawType);
        rawType = deleteStructKeyword(rawType);
        rawType = deleteStorageClasses(rawType);
        rawType = deleteSizeFromArray(rawType);

        for (String type : getAllBasicFieldNames(BASIC.CHARACTER.class)) {
            String[] types = new String[]{type + TWO_DIMENSION};
            for (String typeItem : types)
                if (rawType.equals(typeItem))
                    return true;

        }
        return false;
    }

    /**
     * Ex1: "auto x_alias = x*(-1.1);" -------> "float", or "double" <br/>
     * Ex2: "auto x_alias = 1;" -------> "int", "short", etc. <br/>
     * Ex2: "auto x_alias = new int[2];" -------> "int[]" <br/>
     *
     * @param intializer of varibale
     * @return real type
     */
    public static String getTypeOfAutoVariable(String intializer) {
        final String DEFAULT_TYPE = "int";
        final String FLOAT_TYPE = "float";
        final String ONE_LEVEL_INTEGER_TYPE = "int*";
        final String TWO_LEVEL_INTEGER_TYPE = "int**";
        final String ONE_LEVEL_FLOAT_TYPE = "float*";
        final String TWO_LEVEL_FLOAT_TYPE = "float**";
        /*
         * Ex1: initializer = "1.2"
         *
         * Ex2: initializer = "1/2"
         */
        final String[] FLOAT_SIGNALS = new String[]{".", "/"};
        for (String floatSignal : FLOAT_SIGNALS)
            if (intializer.contains(floatSignal))
                return FLOAT_TYPE;

        /*
         * Ex: initializer = "new int  [2]"
         */
        for (String integerType : VariableTypeUtils
                .getAllBasicFieldNames(BASIC.NUMBER.INTEGER.class)) {
            String signalRegex = "new " + integerType + "[";
            if (Utils.containRegex(intializer, Utils.toRegex(signalRegex)))
                return ONE_LEVEL_INTEGER_TYPE;
        }
        /*
         * Ex: initializer = "new int * [2]"
         */
        for (String integerType : VariableTypeUtils
                .getAllBasicFieldNames(BASIC.NUMBER.INTEGER.class)) {
            String signalRegex = "new " + integerType + "* [";
            if (Utils.containRegex(intializer, Utils.toRegex(signalRegex)))
                return TWO_LEVEL_INTEGER_TYPE;
        }

        /*
         * Ex: initializer = "new float[2]"
         */
        for (String integerType : VariableTypeUtils
                .getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String signalRegex = "new " + integerType + "[";
            if (Utils.containRegex(intializer, Utils.toRegex(signalRegex)))
                return ONE_LEVEL_FLOAT_TYPE;
        }

        /*
         * Ex: initializer = "new float*[2]"
         */
        for (String integerType : VariableTypeUtils
                .getAllBasicFieldNames(BASIC.NUMBER.FLOAT.class)) {
            String signalRegex = "new " + integerType + "* [";
            if (Utils.containRegex(intializer, Utils.toRegex(signalRegex)))
                return TWO_LEVEL_FLOAT_TYPE;
        }

        /*
         * If not map all, it may be integer type
         */

        return DEFAULT_TYPE;
    }

    public static boolean isThrow(String rawType) {
        return rawType.equals(THROW);
    }

    public static class BASIC {

        public static class BOOLEAN {
            public static final String BOOL = "bool";
        }

        public static class NUMBER {

            public static class FLOAT {
                public static final String FLOAT = "float";
                public static final String DOUBLE = "double";
                public static final String LONG_DOUBLE = "long double";
                public static final String LONG_FLOAT = "long float";
                public static final String LONG_LONG_DOUBLE = "long long double";
                public static final String LONG_LONG_FLOAT = "long long float";
            }

            public static class INTEGER {
                public static final String BOOL = "bool";

                public static final String INT = "int";
                public static final String SIGNED = "signed";
                public static final String SIGNED_INT = "signed int";
                public static final String UNSIGNED_INT = "unsigned int";
                public static final String SHORT_INT = "short int";
                public static final String SIGNED_SHORT = "signed short";
                public static final String UNSIGNED_SHORT_INT = "unsigned short int";
                public static final String SIGNED_SHORT_INT = "signed short int";
                public static final String LONG_INT = "long int";
                public static final String SIGNED_LONG_INT = "signed long int";
                public static final String UNSIGNED_LONG_INT = "unsigned long int";

                public static final String SHORT = "short";

                public static final String LONG = "long";
                public static final String LONG_LONG = "long long";
                public static final String SIGNED_LONG_LONG = "signed long long";
                public static final String LONG_LONG_INT = "long long int";
                public static final String SIGNED_LONG_LONG_INT = "signed long long int";

                public static final String UNSIGNED = "unsigned";
                public static final String UNSIGNED_SHORT = "unsigned short";
                public static final String UNSIGNED_LONG = "unsigned long";
                public static final String SIGNED_LONG = "signed long";

                public static final String UNSIGNED_LONG_LONG_INT = "unsigned long long int";
                public static final String UNSIGNED_LONG_LONG = "unsigned long long";

                public static final String SIZE__T = "size_t";

            }
        }

        public static class STDINT {
            public static final String INTMAX__T = "intmax_t";
            public static final String UINTMAX__T = "uintmax_t";
            public static final String INT8__T = "int8_t";
            public static final String UINT8__T = "uint8_t";
            public static final String INT16__T = "int16_t";
            public static final String UINT16__T = "uint16_t";
            public static final String INT32__T = "int32_t";
            public static final String UINT32__T = "uint32_t";
            public static final String INT64__T = "int64_t";
            public static final String UINT64__T = "uint64_t";
            public static final String INT__LEAST8__T = "int_least8_t";
            public static final String UINT__LEAST8__T = "uint_least8_t";
            public static final String INT__LEAST16__T = "int_least16_t";
            public static final String UINT__LEAST16__T = "uint_least16_t";
            public static final String INT__LEAST32__T = "int_least32_t";
            public static final String UINT__LEAST32__T = "uint_least32_t";
            public static final String INT__LEAST64__T = "int_least64_t";
            public static final String UINT__LEAST64__T = "uint_least64_t";
            public static final String INT__FAST8__T = "int_fast8_t";
            public static final String UINT__FAST8__T = "uint_fast8_t";
            public static final String INT__FAST16__T = "int_fast16_t";
            public static final String UINT__FAST16__T = "uint_fast16_t";
            public static final String INT__FAST32__T = "int_fast32_t";
            public static final String UINT__FAST32__T = "uint_fast32_t";
            public static final String INT__FAST64__T = "int_fast64_t";
            public static final String UINT__FAST64__T = "uint_fast64_t";
            public static final String INTPTR__T = "intptr_t";
            public static final String UINTPTR__T = "uintptr_t";
        }

        public static class CHARACTER {
            public static final String CHAR = "char";
            public static final String SIGNED_CHAR = "signed char";
            public static final String UNSIGNED_CHAR = "unsigned char";
            public static final String WCHAR__T = "wchar__t";
//            public static final String ____WCHAR__T = "__wchar_t";
            public static final String CHAR16__T = "char16_t";
            public static final String CHAR32__T = "char32_t";
        }

        public static class STRING {
            public static final String STRING = "string";
            public static final String WSTRING = "wstring";
            public static final String U16STRING = "u16string";
            public static final String U32STRING = "u32string";
            public static final String STD_STRING = "std::string";
        }
    }

    public static class STL {
        public static class VECTOR {
            public static final String VECTOR = "vector";
        }
        public static class LIST {
            public static final String LIST = "list";
        }
        public static class QUEUE {
            public static final String QUEUE = "queue";
        }
        public static class STACK {
            public static final String STACK = "stack";
        }
        public static class SET {
            public static final String SET = "set";
        }
        public static class PAIR {
            public static final String PAIR = "pair";
        }
        public static class MAP {
            public static final String MAP = "map";
        }
        public static class ARRAY {
            public static final String ARRAY = "array";
        }
    }

    /**
     * Represent name of variables that its type is struct, class, union.
     */
    public static class STRUCTURE {
        /**
         * Ex1: abc[]
         * <p>
         * Ex2: A::B::C::abc[]
         */
        public static final String ONE_DIMENSION_STRUCTURE_REGEX = "[a-zA-Z0-9:]+" + "(\\[\\]){1}";
        /**
         * Ex: abc[][]
         * <p>
         * Ex2: A::B::C::abc[][]
         */
        public static final String TWO_DIMENSION_STRUCTURE_REGEX = "[a-zA-Z0-9:]+" + "(\\[\\]){2}";
        // struct Node[][], struct Node[3], struct Node[]
        public static final String MULTI_DIMENSION_STRUCTURE_REGEX = "([a-zA-Z0-9:_\\s]+)(\\[[a-zA-Z0-9:_\\s]*\\])+$";
        /**
         * Ex: abc*
         * <p>
         * Ex2: A::B::C::abc*
         */
        public static final String ONE_LEVEL_STRUCTURE_REGEX = "[a-zA-Z0-9:]+" + "\\s*\\*{1}\\s*$";
        public static final String ONE_LEVEL_STRUCTURE_REGEX_TYPE2 = "struct\\s+[a-zA-Z0-9:]+" + "\\*{1}";
        /**
         * Ex: abc**
         * <p>
         * Ex2: A::B::C::abc**
         */
        public static final String TWO_LEVEL_STRUCTURE_REGEX = ".*[a-zA-Z0-9:]+" + "\\s*\\*{2}\\s*$";

        public static final String MULTI_LEVEL_STRUCTURE_REGEX = "([a-zA-Z0-9:_\\s]+)(\\*\\s*)+$";
        /**
         * Ex: abc
         * <p>
         * Ex2: A::B::C::abc
         */
        public static final String SIMPLE_STRUCTURE_REGEX = "([a-zA-Z_0-9]*\\s*(::)*\\s*)*" + IRegex.NAME_REGEX + "\\s*$";
    }

    public static class VOID_TYPE {
        public static final String VOID = "void";
        public static final String VOID_PTR = "void*";
    }

    public static boolean isEnumNode(String type, INode root) {
        List<INode> myDefines = Search.searchNodes(root, new EnumNodeCondition());

        for (INode myDefine : myDefines) {
            if (myDefine.getNewType().equals(type)) {
                return true;
            }
        }
        return false;
    }

//    public static EnumNode findEnumNode(String type, INode root) {
//        List<INode> mydefines = Search.searchNodes(root, new EnumNodeCondition());
//        for (INode mydefine : mydefines) {
//            if (mydefine.getNewType().equals(type)) {
//                return (EnumNode) mydefine;
//            }
//        }
//        return null;
//    }
//
//    public static boolean isDefineNode(String type, INode root) {
//        List<INode> mydefines = Search.searchNodes(root, new TypedefNodeCondition());
//
//        for (INode mydefine : mydefines) {
//            if (mydefine.getNewType().equals(type)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    public static boolean isDefineNodeOfBasicType(String type, INode root) {
//        List<INode> mydefines = Search.searchNodes(root, new TypedefNodeCondition());
//
//        for (INode mydefine : mydefines) {
//            if (mydefine.getNewType().equals(type)) {
//                String oldType = ((TypedefDeclaration) mydefine).getOldType();
//                if (isBasic(oldType))
//                    return true;
//            }
//        }
//
//        return false;
//    }
//
//    public static boolean isIncludeHeader(IDataNode dataNode, List<String> includes) {
//        List<String> normalize = includes.stream()
//                .map(i -> i.replace(" ", ""))
//                .collect(Collectors.toList());
//
//        IDataNode parent = dataNode;
//
//        while (parent != null) {
//            if (parent instanceof SubprogramNode && !(parent instanceof ConstructorDataNode))
//                break;
//            else
//                parent = parent.getParent();
//        }
//
//        if (parent != null) {
//            INode functionNode = ((SubprogramNode) parent).getFunctionNode();
//            List<Level> space = new VariableSearchingSpace(functionNode).generateExtendSpaces();
//            for (Level level : space) {
//                for (INode node : level) {
//                    List<INode> nodes = Search.searchNodes(node, new IncludeHeaderNodeCondition());
//                    for (INode includeHeaderNode : nodes) {
//                        String target = includeHeaderNode.getNewType().replace(" ", "");
//                        if (normalize.contains(target))
//                            return true;
//                    }
//                }
//            }
//        }
//
//        return false;
//    }

    /**
     * "int*" ---> "int"
     *
     * "int**" ---> "int*"
     * @param type
     * @return
     */
    public static String getElementTypeOfPointer(String type){
        return type.replaceAll("\\*$", "");
    }
}
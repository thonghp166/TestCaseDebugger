package com.dse.parser;

import auto_testcase_generation.normalizer.AbstractNormalizer;
import auto_testcase_generation.normalizer.Cpp11ClassNormalizer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.EnviroDefinedVariableNode;
import com.dse.environment.object.EnvironmentRootNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.*;
import com.dse.util.AkaLogger;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import com.dse.util.tostring.DependencyTreeDisplayer;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

import java.io.File;
import java.util.*;

//import com.dse.search.condition.EnumTypedefNodeCondifion;
//import com.dse.search.condition.UnionTypedefNodeCondifion;

/**
 * Construct structure tree of the given source code file
 *
 * @author DucAnh
 */
public class SourcecodeFileParser implements ISourcecodeFileParser {
    final static AkaLogger logger = AkaLogger.get(SourcecodeFileParser.class);

    private SourcecodeFileNode sourcecodeNode;

    private IASTTranslationUnit translationUnit;

    public static void main(String[] args) throws Exception {
        SourcecodeFileParser cppParser = new SourcecodeFileParser();

//        String path = "/Users/ducanhnguyen/Documents/akautauto/datatest/fsoft/c-algorithms/src/rb-tree.h";
//        String path = "/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/Algorithmv2/src/class/FriendClass.cpp";
//        String path = "/Users/ducanhnguyen/Documents/akautauto/datatest/fsoft/c-algorithms/src/list.h";
        String path = "/home/lamnt/IdeaProjects/akautauto/datatest/official_tested_projects/c-algorithms/src/binary-heap.h";
        INode root = cppParser.parseSourcecodeFile(new File(path));

        System.out.println(new DependencyTreeDisplayer(root).getTreeInString());

        /*
          display tree of project
         */
//        INode headers = Search.searchNodes(root, new StructurevsTypedefCondition())
//                .stream().filter(s -> s.getName().equals("Headers")).findFirst().orElse(null);
//
//        INode request = Search.searchNodes(root, new StructurevsTypedefCondition())
//                .stream().filter(s -> s.getName().equals("Request")).findFirst().orElse(null);
//
//        assert request != null;
//
//        INode attribute = request.getChildren().stream()
//                .filter(n -> n.getName().equals("headers"))
//                .findFirst()
//                .orElse(null);
//
//        assert attribute != null;
//        INode type = ((IVariableNode) attribute).resolveCoreType();
//
//        System.out.println();
    }

    @Override
    public INode generateTree() throws Exception {
        if (!sourcecodeNode.isExpandedToMethodLevelState()) {
            sourcecodeNode.setExpandedToMethodLevelState(true);

            File f = new File(sourcecodeNode.getAbsolutePath());
            if (f.exists()) {
                INode root = parseSourcecodeFile(f);
                if (root instanceof SourcecodeFileNode)
                    ((SourcecodeFileNode) root).setExpandedToMethodLevelState(true);
                return root;
            } else {
                logger.debug(sourcecodeNode.getAbsolutePath() + " does not exist.");
                return null;
            }
        } else {
            logger.debug(sourcecodeNode.getAbsolutePath() + " is expanded down to method level before");
            return sourcecodeNode;
        }
    }

    public INode parseSourcecodeFile(File filePath) throws Exception {
        logger.debug("Parse " + filePath);
        normalizeFile(filePath);

        translationUnit = getIASTTranslationUnit(Utils.readFileContent(
                filePath.getAbsolutePath()).toCharArray());

        CustomCppStack stackNodes = new CustomCppStack();
        Node vituralRoot = new TemporaryNode("tmp root Node");
        vituralRoot.setAbsolutePath(filePath.getCanonicalPath());
        stackNodes.push(vituralRoot);

        ASTVisitor visitor = new ASTVisitor() {
            boolean isPrivate = false;
            int visibility = ICPPASTVisibilityLabel.v_public;

            @Override
            public int leave(IASTDeclaration declaration) {
                stackNodes.pop();

                /*
                  Nếu thoát khỏi class thì scope biến luôn là public
                 */
                if (SourcecodeFileParser.this
                        .getTypeOfAstDeclaration(declaration) == ISourcecodeFileParser.IS_CLASS_DECLARATION
                        || SourcecodeFileParser.this
                        .getTypeOfAstDeclaration(declaration) == ISourcecodeFileParser.IS_STRUCT_DECLARATION) {
                    isPrivate = false;
                    visibility = ICPPASTVisibilityLabel.v_public;
                }
                return ASTVisitor.PROCESS_CONTINUE;
            }

            @Override
            public int leave(ICPPASTNamespaceDefinition namespaceDefinition) {
                stackNodes.pop();
                return ASTVisitor.PROCESS_CONTINUE;
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                /*
                  IASTDeclaration đại diện một khai bảo trong mã nguồn
                 */
                Node declarationNode = new TemporaryNode("tmpNode");

                int typeOfDeclaration = SourcecodeFileParser.this
                        .getTypeOfAstDeclaration(declaration);

                switch (typeOfDeclaration) {
                    case IS_FUNCTION_POINTER_TYPEDEF_DECLARATION: {
                        declarationNode = new FunctionPointerTypedefDeclaration();
                        ((FunctionPointerTypedefDeclaration) declarationNode).setAST((IASTSimpleDeclaration) declaration);
                        break;
                    }

                    case IS_LINKAGE_SPECIFICATION: {
                        declarationNode = new LinkageSpecificationDeclaration();
                        ((LinkageSpecificationDeclaration) declarationNode).setAST((ICPPASTLinkageSpecification) declaration);
                        break;
                    }

                    case IS_ALIAS_DECLARATION:
                        declarationNode = new AliasDeclaration();

                        ((AliasDeclaration) declarationNode)
                                .setAST((ICPPASTAliasDeclaration) declaration);

                        break;

                    case IS_FUNCTION_DECLARATION:
                        if (declaration instanceof ICPPASTTemplateDeclaration)
                            break;
                        declarationNode = new FunctionNode();
                        ((FunctionNode) declarationNode)
                                .setAST((IASTFunctionDefinition) declaration);
                        break;

                    case IS_FUNCTION_AS_VARIABLE_DECLARATION:
                        declarationNode = new DefinitionFunctionNode();
                        ((DefinitionFunctionNode) declarationNode)
                                .setAST((CPPASTSimpleDeclaration) declaration);
                        break;

                    case IS_CONSTRUCTOR_DECLARATION:
                        declarationNode = new ConstructorNode();
                        ((ConstructorNode) declarationNode)
                                .setAST((IASTFunctionDefinition) declaration);
                        break;

                    case IS_DESTRUCTOR_DECLARATION:
                        declarationNode = new DestructorNode();
                        ((DestructorNode) declarationNode)
                                .setAST((IASTFunctionDefinition) declaration);
                        break;

                    case IS_TEMPLATE_DECLARATION:
                        IASTDeclaration template = ((ICPPASTTemplateDeclaration) declaration)
                                .getDeclaration();
                        if (template instanceof IASTFunctionDefinition) {
                            FunctionNode fn = new FunctionNode();
                            fn.setAST((IASTFunctionDefinition) template);
                            declarationNode = fn;
                        } else if (template instanceof IASTSimpleDeclaration) {
                            IASTDeclSpecifier declSpec = ((IASTSimpleDeclaration) template).getDeclSpecifier();
                            if (declSpec instanceof IASTCompositeTypeSpecifier) {
                                ClassNode cn = new ClassNode();
                                cn.setAST((IASTSimpleDeclaration) template);
                                declarationNode = cn;
                                isPrivate = true;
                            } else {
                                IASTSimpleDeclaration decList = (IASTSimpleDeclaration) template;
                                IASTDeclSpecifier type = decList.getDeclSpecifier();
                                INodeFactory fac = translationUnit.getASTNodeFactory();

                                for (IASTDeclarator dec : decList.getDeclarators()) {
                                    IASTSimpleDeclaration decItem = fac
                                            .newSimpleDeclaration(type
                                                    .copy(CopyStyle.withLocations));
                                    decItem.addDeclarator(dec.copy(CopyStyle.withLocations));

                                    stackNodes.push(declarationNode);
                                    stackNodes.pop();

                                    // Note: We have two types of variables known as internal variable, and external variable.
                                    // Internal variable are passed into the function, e.g., void test(int a, int b) -----> a, b: internal variable
                                    // All variables declared outside functions are considered as external variables.
                                    // Because we only parse down to method level, so all variables discovered in this process belong to kind of external variables.
                                    VariableNode var = null;
                                    if (stackNodes.peek() instanceof StructureNode){
                                        var = new AttributeOfStructureVariableNode();
                                    } else if (stackNodes.peek().getParent() == null
                                            || stackNodes.peek().getParent() instanceof NamespaceNode) {
                                        var = new ExternalVariableNode();
                                    }

                                    if (var != null) {
                                        var.setAST(decItem);
                                        declarationNode = var;
                                        var.setPrivate(isPrivate);
                                    }
                                }
                            }

                        }
                        break;

                    case IS_STRUCT_DECLARATION:
                        declarationNode = new StructNode();
                        ((StructNode) declarationNode)
                                .setAST((IASTSimpleDeclaration) declaration);

                        isPrivate = false;
                        break;

                    case IS_CLASS_DECLARATION:
                        declarationNode = new ClassNode();
                        ((ClassNode) declarationNode)
                                .setAST((IASTSimpleDeclaration) declaration);

                        isPrivate = true;
                        break;

                    case IS_VARIABLE_DECLARATION: {
                        IASTSimpleDeclaration decList = (IASTSimpleDeclaration) declaration;
//                        IASTDeclSpecifier type = decList.getDeclSpecifier();
//                        INodeFactory fac = translationUnit.getASTNodeFactory();

                        for (IASTDeclarator dec : decList.getDeclarators()) {
                            IASTNode decItem = Utils.convertToIAST(decList.getDeclSpecifier().getRawSignature()+" " + dec.getRawSignature());
                            decItem = decItem.getChildren()[0];
//                            IASTSimpleDeclaration decItem = fac
//                                    .newSimpleDeclaration(type
//                                            .copy(CopyStyle.withLocations));
//                            decItem.addDeclarator(dec.copy(CopyStyle.withLocations));

                            stackNodes.push(declarationNode);
                            stackNodes.pop();

                            // Note: We have two types of variables known as internal variable, and external variable.
                            // Internal variable are passed into the function, e.g., void test(int a, int b) -----> a, b: internal variable
                            // All variables declared outside functions are considered as external variables.
                            // Because we only parse down to method level, so all variables discovered in this process belong to kind of external variables.
                            VariableNode var = null;
                            if (stackNodes.peek() instanceof StructureNode){
                                var = new AttributeOfStructureVariableNode();
                            } else if (stackNodes.peek().getParent() == null
                                || stackNodes.peek().getParent() instanceof NamespaceNode) {
                                var = new ExternalVariableNode();
                            }

                            if (var != null) {
                                var.setAST(decItem);
                                declarationNode = var;
                                var.setPrivate(isPrivate);
                            }
                        }
                        break;
                    }

                    case IS_PRIMITIVE_TYPEDEF_DECLARATION: {
                        IASTSimpleDeclaration decList = (IASTSimpleDeclaration) declaration;
                        IASTDeclSpecifier type = decList.getDeclSpecifier();
                        INodeFactory fac = translationUnit.getASTNodeFactory();

                        for (IASTDeclarator dec : decList.getDeclarators()) {
                            IASTSimpleDeclaration decItem = fac
                                    .newSimpleDeclaration(type
                                            .copy(CopyStyle.withLocations));
                            decItem.addDeclarator(dec.copy(CopyStyle.withLocations));

                            stackNodes.push(declarationNode);
                            stackNodes.pop();

                            TypedefDeclaration td = new PrimitiveTypedefDeclaration();
                            td.setAST(decItem);
                            declarationNode = td;
                        }
                        break;
                    }

                    case IS_STRUCT_TYPEDEF_DECLARATION: {
                        /*
                         * Ex1: typedef struct MyStruct4{ int x; } MyStruct5;
                         *
                         * Ex2: typedef struct { int x; } MyStruct5;
                         */
                        declarationNode = new StructTypedefNode();
                        ((StructTypedefNode) declarationNode)
                                .setAST((IASTSimpleDeclaration) declaration);

                        isPrivate = false;
                        break;
                    }

                    case IS_PROTECTED_LABEL:
                    case IS_PRIVATE_LABEL:
                        isPrivate = true;
                        break;

                    case IS_PUBLIC_LABEL:
                        isPrivate = false;
                        break;

                    case IS_ENUM:
                        declarationNode = new EnumNode();
                        ((EnumNode) declarationNode)
                                .setAST((IASTSimpleDeclaration) declaration);
                        break;

                    case IS_ENUM_TYPEDEF_DECLARATION:
                        declarationNode = new EnumTypedefNode();
                        ((EnumTypedefNode) declarationNode).setAST((IASTSimpleDeclaration) declaration);
                        isPrivate = false;
                        break;

                    case IS_UNION:
                        declarationNode = new UnionNode();
                        ((UnionNode) declarationNode)
                                .setAST((IASTSimpleDeclaration) declaration);
                        break;

                    case IS_UNION_TYPEDEF_DECLARATION:
                        declarationNode = new UnionTypedefNode();
                        ((UnionTypedefNode) declarationNode).setAST((IASTSimpleDeclaration) declaration);
                        isPrivate = false;
                        break;

                    case IS_UNSPECIFIED_DECLARATION:
                    default: {
                        declarationNode = new UnspecifiedDeclaration();
                        ((UnspecifiedDeclaration)declarationNode).setAST(declaration);
                        break;
                    }
                }

                stackNodes.push(declarationNode);

                if (typeOfDeclaration == ISourcecodeFileParser.IS_FUNCTION_DECLARATION) {
                    stackNodes.pop();
                    return ASTVisitor.PROCESS_SKIP;
                }
                return ASTVisitor.PROCESS_CONTINUE;
            }

            @Override
            public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
                NamespaceNode namespaceNode = new NamespaceNode();
                namespaceNode.setAST(namespaceDefinition);
                stackNodes.push(namespaceNode);
                return ASTVisitor.PROCESS_CONTINUE;
            }

        };
        visitor.shouldVisitDeclarations = true;
        visitor.shouldVisitNamespaces = true;

        translationUnit.accept(visitor);

        INode root = stackNodes.rootOfStack;

//        createSpecialNode(root);
        addIncludeHeaderNodes(getHeader(translationUnit), root);
        parseMacroFunction(root);
        return root;
    }

    private void parseMacroFunction(INode root) {
        IASTPreprocessorMacroDefinition[] macroDefinitions = translationUnit.getMacroDefinitions();

        //Utils.convertToIAST(translationUnit.getMacroDefinitions()[3].getExpansion())
        for (IASTPreprocessorMacroDefinition definition : macroDefinitions) {
            if (definition instanceof IASTPreprocessorFunctionStyleMacroDefinition) {
                String expansion = definition.getExpansion();
                IASTNode tempAST = Utils.convertToIAST(expansion);

                if (!(tempAST instanceof IASTProblemStatement)) {
                    MacroFunctionNode functionNode = new MacroFunctionNode();

                    String name = definition.getName().getBinding().toString();
                    functionNode.setAbsolutePath(root.getAbsolutePath() + File.separator + name);

                    functionNode.setAST((IASTPreprocessorFunctionStyleMacroDefinition) definition);

                    functionNode.setParent(root);
                    root.getChildren().add(functionNode);
                }
            }
        }
    }

//    private void createSpecialNode(INode root) {
//        List<SearchCondition> conditions = new ArrayList<>();
//        conditions.add(new StructTypedefNodeCondifion());
//        conditions.add(new UnionTypedefNodeCondifion());
//        conditions.add(new EnumTypedefNodeCondifion());
//        List<INode> typedefNodes = Search.searchNodes(root, conditions);
//
//        for (INode typedefNode : typedefNodes) {
//            StructureNode newTypedefNode = null;
//
//            if (typedefNode instanceof StructNode) {
//                newTypedefNode = new SpecialStructTypedefNode();
//            } else if (typedefNode instanceof EnumNode) {
//                newTypedefNode = new SpecialEnumTypedefNode();
//            } else if (typedefNode instanceof UnionNode) {
//                newTypedefNode = new SpecialUnionTypedefNode();
//            }
//            /*
//             * Ex: typedef xxx myXXX { int x; } MyXXX5;
//             */
//            try {
//                newTypedefNode.setAST(((StructureNode) typedefNode).getAST());
//                newTypedefNode.setChildren(typedefNode.getChildren());
//                newTypedefNode.setParent(typedefNode.getParent());
//                typedefNode.getParent().getChildren().add(newTypedefNode);
//            } catch (Exception e) {
//                 e.printStackTrace();
//            }
//
//        }
//    }

    /**
     * Thêm các node mô tả include vào cây
     *
     * @param includeHeaderNodes
     * @param root
     */

    private void addIncludeHeaderNodes(
            List<IASTPreprocessorIncludeStatement> includeHeaderNodes,
            INode root) {
        for (IASTPreprocessorIncludeStatement include : includeHeaderNodes) {
            IncludeHeaderNode includeHeaderNode = new IncludeHeaderNode();
            includeHeaderNode.setAST(include);
            includeHeaderNode.setAbsolutePath(root.getAbsolutePath()
                    + File.separator + "\"" + includeHeaderNode.getNewType()
                    + "\"");
            root.getChildren().add(includeHeaderNode);
        }
    }

    /**
     * Lấy danh sách include của một tệp .cpp
     *
     * @param u
     * @return
     */
    private List<IASTPreprocessorIncludeStatement> getHeader(
            IASTTranslationUnit u) {
        List<IASTPreprocessorIncludeStatement> includes = new ArrayList<>();
        for (IASTPreprocessorIncludeStatement includeDirective : u.getIncludeDirectives())
            // if the include header is active by macro
             if (includeDirective.isActive())
            {
                includes.add(includeDirective);
            }
        return includes;
    }

    /**
     * Get all macro defined by users
     * @return
     */
    private Map<String, String> getAllMacros() {
        Map<String, String> macros = new HashMap<>();
        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> macroNodes = EnvironmentSearch.searchNode(root, new EnviroDefinedVariableNode());
        for (IEnvironmentNode macroNode: macroNodes) {
            if (macroNode instanceof EnviroDefinedVariableNode) {
                if (((EnviroDefinedVariableNode) macroNode).getValue() == null)
                    macros.put(((EnviroDefinedVariableNode) macroNode).getName(), "");
                else
                    macros.put(((EnviroDefinedVariableNode) macroNode).getName(), ((EnviroDefinedVariableNode) macroNode).getValue());
            }
        }
        return macros;
    }

    public IASTTranslationUnit getIASTTranslationUnit(char[] code)
            throws Exception {
        FileContent fc = FileContent.create("", code);
        Map<String, String> macroDefinitions = getAllMacros();

        String[] includeSearchPaths = new String[0];
        IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
        IncludeFileContentProvider ifcp = IncludeFileContentProvider
                .getEmptyFilesProvider();
        IIndex idx = null;
        int options = ILanguage.OPTION_IS_SOURCE_UNIT;
        IParserLogService log = new DefaultLogService();
        return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp,
                idx, options, log);
    }

    /**
     * Lấy kiểu AST Node
     *
     * @param astNode
     * @return
     */
    private int getTypeOfAstDeclaration(IASTDeclaration astNode) {
        if (astNode instanceof ICPPASTLinkageSpecification) {
            return IS_LINKAGE_SPECIFICATION;

        } else if (astNode instanceof ICPPASTAliasDeclaration) {
            return ISourcecodeFileParser.IS_ALIAS_DECLARATION;
        }
        else
            // Câu lệnh rỗng
            if (astNode instanceof IASTNullStatement)
                return ISourcecodeFileParser.IS_UNSPECIFIED_DECLARATION;
            else
                /*
                 * Ex: enum Color { RED, GREEN, BLUE };
                 */
                if (astNode.getChildren().length >= 1
                        && astNode.getChildren()[0] instanceof ICPPASTEnumerationSpecifier
                        //TODO: LAMNT FIX
                        && astNode.getChildren()[0].getRawSignature().contains(
                        ISourcecodeFileParser.ENUM_SYMBOL)
                )
                    if (astNode.getChildren()[0].getRawSignature().contains(ISourcecodeFileParser.TYPEDEF_SYMBOL))
                        return ISourcecodeFileParser.IS_ENUM_TYPEDEF_DECLARATION;
                    else
                        return ISourcecodeFileParser.IS_ENUM;
                else
                    /*
                     * Ex: union RGBA{ int color; int aliasColor;}
                     */
                    if (astNode.getChildren().length >= 1
                            && astNode.getChildren()[0] instanceof CPPASTCompositeTypeSpecifier
                            //TODO: LAMNT FIX
                            && astNode.getChildren()[0].getRawSignature().contains(
                            ISourcecodeFileParser.UNION_SYMBOL)
                    )
                        if (astNode.getChildren()[0].getRawSignature().contains(ISourcecodeFileParser.TYPEDEF_SYMBOL))
                            return ISourcecodeFileParser.IS_UNION_TYPEDEF_DECLARATION;
                        else
                            return ISourcecodeFileParser.IS_UNION;
                    else
                        /*
                         *
                         * Nếu node là public/private/protected
                         */
                        if (astNode instanceof ICPPASTVisibilityLabel)
                            switch (((ICPPASTVisibilityLabel) astNode).getVisibility()) {
                                case ICPPASTVisibilityLabel.v_private:
                                    return ISourcecodeFileParser.IS_PRIVATE_LABEL;
                                case ICPPASTVisibilityLabel.v_protected:
                                    return ISourcecodeFileParser.IS_PROTECTED_LABEL;
                                case ICPPASTVisibilityLabel.v_public:
                                    return ISourcecodeFileParser.IS_PUBLIC_LABEL;
                            }
                        else if (astNode instanceof IASTFunctionDefinition) {
                            if (astNode.getRawSignature().contains(
                                    ISourcecodeFileParser.FUNCTION_BODY_SIGNAL)) {
                                if (isConstructor((IASTFunctionDefinition) astNode))
                                    return ISourcecodeFileParser.IS_CONSTRUCTOR_DECLARATION;
                                else if (isDestructor((IASTFunctionDefinition) astNode))
                                    return ISourcecodeFileParser.IS_DESTRUCTOR_DECLARATION;
                                else
                                    return ISourcecodeFileParser.IS_FUNCTION_DECLARATION;
                            } else
                                return ISourcecodeFileParser.IS_UNSPECIFIED_DECLARATION;
                        } else if (astNode instanceof ICPPASTTemplateDeclaration)
                            return ISourcecodeFileParser.IS_TEMPLATE_DECLARATION;
                        else if (astNode instanceof IASTSimpleDeclaration) {
                            // special case: "struct Node *quickSortRecur(struct Node *head, struct Node *end);" put in a namespace
                            // it is the definition of a function, not a variable declaration
                            for (IASTDeclarator declarator : ((IASTSimpleDeclaration) astNode).getDeclarators()) {
                                if (declarator instanceof CPPASTFunctionDeclarator)
                                    if (astNode.getRawSignature().startsWith(ISourcecodeFileParser.TYPEDEF_SYMBOL))
                                        // typedef int (*ListCompareFunc)(ListValue value1, ListValue value2);
                                        return ISourcecodeFileParser.IS_FUNCTION_POINTER_TYPEDEF_DECLARATION;
                                    else
                                        // *quickSortRecur(struct Node *head, struct Node *end)
                                    return ISourcecodeFileParser.IS_FUNCTION_AS_VARIABLE_DECLARATION;
                            }

                            /*
                             * IASTSimpleDeclaration đại diện câu lệnh khai báo biến, struct,
                             * class, enum, union
                             */
                            IASTDeclSpecifier declSpecifier = ((IASTSimpleDeclaration) astNode)
                                    .getDeclSpecifier();
                            /*
                             * IASTCompositeTypeSpecifier đại diện cho cấu trúc chứa khai báo
                             * nhiều thành phần con bên trong VD: struct, class, union.
                             */
                            if (declSpecifier instanceof IASTCompositeTypeSpecifier) {
                                switch (((IASTCompositeTypeSpecifier) declSpecifier).getKey()) {

                                    case IASTCompositeTypeSpecifier.k_struct:
                                        /*
                                         * Ex: typedef struct { int x; } MyStruct1;
                                         */
                                        if (astNode.getRawSignature().startsWith("typedef "))
                                            return ISourcecodeFileParser.IS_STRUCT_TYPEDEF_DECLARATION;
                                        else
                                            return ISourcecodeFileParser.IS_STRUCT_DECLARATION;
                                    case ICPPASTCompositeTypeSpecifier.k_class:
                                        return ISourcecodeFileParser.IS_CLASS_DECLARATION;
                                }
                            }
                            /* Lam fix bug with attribute contains struct keyword
                             * TODO: union
                             */
                            else if (declSpecifier instanceof IASTElaboratedTypeSpecifier) {
                                if (declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
                                    return ISourcecodeFileParser.IS_PRIMITIVE_TYPEDEF_DECLARATION;
                                else
                                    return ISourcecodeFileParser.IS_VARIABLE_DECLARATION;
                            } else
                                /*
                                 * IASTSimpleDeclSpecifier tương ứng với biến kiểu cơ bản như int,
                                 * float, double, v.v.
                                 */
                                if (declSpecifier instanceof IASTSimpleDeclSpecifier
                                        || declSpecifier instanceof CPPASTCompositeTypeSpecifier
                                        || declSpecifier instanceof IASTNamedTypeSpecifier)
                                    /*
                                     * CPPASTNamedTypeSpecifier tương ứng với biến tự định nghĩa. Ví dụ
                                     * như kiểu DEPT trong khai báo DEPT department;
                                     */ {
                                    if (declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
                                        return ISourcecodeFileParser.IS_PRIMITIVE_TYPEDEF_DECLARATION;
                                    else if (!astNode.getRawSignature().contains(
                                            ISourcecodeFileParser.METHOD_SIGNAL))
                                        return ISourcecodeFileParser.IS_VARIABLE_DECLARATION;
                                    else
                                        return ISourcecodeFileParser.IS_FUNCTION_AS_VARIABLE_DECLARATION;
                                } else if (declSpecifier instanceof ICPPASTElaboratedTypeSpecifier) {
                                    ICPPASTElaboratedTypeSpecifier decl = (ICPPASTElaboratedTypeSpecifier) declSpecifier;
                                    if (decl.getStorageClass() == IASTDeclSpecifier.sc_typedef)
                                        return ISourcecodeFileParser.IS_PRIMITIVE_TYPEDEF_DECLARATION;


                                } else if (declSpecifier instanceof ICPPASTEnumerationSpecifier) {
                                    /**
                                     * Ex: typedef enum {
                                     * 	RB_TREE_NODE_RED,
                                     * 	RB_TREE_NODE_BLACK,
                                     * } RBTreeNodeColor;
                                     */
                                    ICPPASTEnumerationSpecifier decl = (ICPPASTEnumerationSpecifier) declSpecifier;
                                    if (decl.getStorageClass() == IASTDeclSpecifier.sc_typedef)
                                        return ISourcecodeFileParser.IS_PRIMITIVE_TYPEDEF_DECLARATION;
                                }
                        }

        return ISourcecodeFileParser.IS_UNSPECIFIED_DECLARATION;
    }

    private boolean isConstructor(IASTFunctionDefinition ast) {
        IASTDeclSpecifier decl = ast.getDeclSpecifier();
        IASTFunctionDeclarator declarator = ast.getDeclarator();
        IASTNode firstChildOfDeclarator = declarator.getChildren()[0];

        /*
         * Nếu hàm không có kiểu trả về + tên hàm giống tên class/structure
         */
        return decl.getRawSignature().equals("")
                && !firstChildOfDeclarator.getRawSignature().startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR)
                && !firstChildOfDeclarator.getRawSignature().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR);
    }

    private boolean isDestructor(IASTFunctionDefinition ast) {
        IASTDeclSpecifier decl = ast.getDeclSpecifier();
        IASTFunctionDeclarator declarator = ast.getDeclarator();
        IASTNode firstChildOfDeclarator = declarator.getChildren()[0];

        /*
         * Nếu hàm không có kiểu trả về + tên hàm giống tên class/structure
         */
        return decl.getRawSignature().equals("")
                && (firstChildOfDeclarator.getRawSignature().startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR) ||
                firstChildOfDeclarator.getRawSignature().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR));
    }

    private void normalizeFile(File file) {
        String statement = Utils.readFileContent(file);

        List<AbstractNormalizer> normalizers = new ArrayList<>();
        Cpp11ClassNormalizer cpp11Norm = new Cpp11ClassNormalizer();
        cpp11Norm.setOriginalSourcecode(statement);
        normalizers.add(cpp11Norm);

        for (AbstractNormalizer n : normalizers) {
            n.normalize();
            String normalizeSourcecode = n.getNormalizedSourcecode();
            if (file.exists() && !normalizeSourcecode.equals(statement))
                Utils.writeContentToFile(normalizeSourcecode,
                        file.getAbsolutePath());
        }
    }

    public IASTTranslationUnit getTranslationUnit() {
        return translationUnit;
    }

    public void setTranslationUnit(IASTTranslationUnit translationUnit) {
        this.translationUnit = translationUnit;
    }

    @Override
    public File getSourcecodeFile() {
        return new File(sourcecodeNode.getAbsolutePath());
    }

    @Override
    public void setSourcecodeFile(File sourcecodeFile) {
        // nothing to do
    }

    public ISourcecodeFileNode getSourcecodeNode() {
        return sourcecodeNode;
    }

    public void setSourcecodeNode(SourcecodeFileNode sourcecodeNode) {
        this.sourcecodeNode = sourcecodeNode;
    }

    /**
     * Chúng ta cần một stack lưu các node đã thăm, với mục đích tạo quan hệ cha
     * - con giữa các node
     *
     * @author DucAnh
     */
    class CustomCppStack extends Stack<INode> {

        private static final long serialVersionUID = 1L;
        INode rootOfStack;

        public INode getRootOfStack() {
            return rootOfStack;
        }

        /**
         * Khi thêm một node mới vào stack, ta tạo luôn quan hệ cha - con với
         * node trước đó. Ngoài ra, nếu node đó là node đầu tiên thêm vào stack,
         * hiển nhiên node đó là root
         */
        @Override
        public INode push(INode item) {
            if (size() == 0)
                rootOfStack = item;
            else if (item instanceof TemporaryNode) {
                // Khong xem xet virtual (temporary) Node
            } else {

                peek().getChildren().add((Node) item);
                item.setParent(peek());

                if (!peek().getAbsolutePath().endsWith(File.separator))
                    item.setAbsolutePath(peek().getAbsolutePath() + File.separator + item.getNewType());
                else
                    item.setAbsolutePath(peek().getAbsolutePath() + item.getNewType());
            }
            return super.push(item);
        }
    }

    /**
     * Đây là một node tạm thời
     *
     * @author DucAnh
     */
    class TemporaryNode extends Node {
        public TemporaryNode(String name) {
            setName(name);
        }
    }
}
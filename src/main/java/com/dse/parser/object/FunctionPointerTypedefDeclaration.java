package com.dse.parser.object;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;

import java.util.ArrayList;
import java.util.List;

/**
 * Ex: "typedef int (*ListCompareFunc)(ListValue value1, ListValue value2);"
 */
public class FunctionPointerTypedefDeclaration extends TypedefDeclaration {
    private IASTNode returnAst; // "int"
    private List<IASTNode> argumentsAst = new ArrayList<>(); // "ListValue value1", "ListValue value2"
    private IASTNode refNameAst; // "ListCompareFunc"
    private List<IVariableNode> arguments = new ArrayList<>();

    @Override
    public void setAST(IASTSimpleDeclaration aST) {
        super.setAST(aST);
        returnAst = aST.getDeclSpecifier(); // "int"

        if (aST.getDeclarators().length != 1)
            return;

        IASTNode declarator = aST.getDeclarators()[0]; // "(*ListCompareFunc)(ListValue value1, ListValue value2)"
        if (declarator instanceof CPPASTFunctionDeclarator){
            ICPPASTParameterDeclaration[] declarations = ((CPPASTFunctionDeclarator) declarator).getParameters();
            for (ICPPASTParameterDeclaration declaration: declarations)
                argumentsAst.add(declaration);

            refNameAst = aST.getDeclarators()[0].getNestedDeclarator().getName();
        }
    }

    public List<IVariableNode> getArguments() {
        for (IASTNode argument:argumentsAst){

        }
        return arguments;
    }

    public void setArguments(List<IVariableNode> arguments) {
        this.arguments = arguments;
    }

    public IASTNode getReturnAst() {
        return returnAst;
    }

    public void setReturnAst(IASTNode returnAst) {
        this.returnAst = returnAst;
    }

    public List<IASTNode> getArgumentsAst() {
        return argumentsAst;
    }

    public void setArgumentsAst(List<IASTNode> argumentsAst) {
        this.argumentsAst = argumentsAst;
    }

    public IASTNode getRefNameAst() {
        return refNameAst;
    }

    public void setRefNameAst(IASTNode refNameAst) {
        this.refNameAst = refNameAst;
    }

    @Override
    public String getNewType() {
        return getRefNameAst().getRawSignature(); // "ListCompareFunc"
    }

    @Override
    public String getOldType() { //int (*ListCompareFunc)(ListValue value1, ListValue value2);
        return super.getOldType();
    }
}

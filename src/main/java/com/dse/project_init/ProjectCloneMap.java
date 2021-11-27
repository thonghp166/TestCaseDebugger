package com.dse.project_init;

import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ProjectCloneMap implements IProjectCloneMap {
    private IASTTranslationUnit astUnit;

    public ProjectCloneMap(String filePath) {
        String clonedFilePath = ProjectClone.getClonedFilePath(filePath);
        String clonedSource = Utils.readFileContent(clonedFilePath);
        parseClonedSourceFile(clonedSource);
    }

    private void parseClonedSourceFile(String clonedSource) {
        try {
            astUnit = Utils.getIASTTranslationUnitforCpp(clonedSource.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IASTNode getClonedASTNode(IASTNode origin) {
        final int lineBoundary = origin.getFileLocation().getStartingLineNumber();

        IASTNode cloned = null;

        if (origin instanceof IASTFunctionDefinition) {
            List<IASTNode> functionDefinitions = searchNodes(astUnit, lineBoundary, IASTFunctionDefinition.class);

            for (IASTNode node : functionDefinitions) {
                if (isEquals(node, origin)) {
                    cloned = node;
                    break;
                }
            }
        } else if (origin instanceof IASTStatement) {
            List<IASTNode> statements = searchNodes(astUnit, lineBoundary, IASTStatement.class);

            List<IASTStatement> matches = new ArrayList<>();

            for (IASTNode node : statements) {
                if (isEquals(node, origin)) {
                    matches.add((IASTStatement) node);
                }
            }

            if (!matches.isEmpty()) {
                //TODO: multi statement ?
                cloned = matches.get(0);
            }
        }

        return cloned;
    }

    @Override
    public int getStartingLine(IASTNode origin) {
        IASTNode node = getClonedASTNode(origin);

        int line = 0;

        if (node != null) {
            line = node.getFileLocation().getStartingLineNumber();
        }

        return line;
    }

    private boolean isEquals(IASTNode origin, IASTNode clone) {
        if (origin instanceof IASTFunctionDefinition) {
            if (clone instanceof IASTFunctionDefinition) {
                IASTDeclSpecifier originDeclSpec = ((IASTFunctionDefinition) origin).getDeclSpecifier();
                IASTFunctionDeclarator originDeclarator = ((IASTFunctionDefinition) origin).getDeclarator();

                IASTDeclSpecifier cloneDeclSpec = ((IASTFunctionDefinition) clone).getDeclSpecifier();
                IASTFunctionDeclarator cloneDeclarator = ((IASTFunctionDefinition) clone).getDeclarator();

                return originDeclarator.getRawSignature().equals(cloneDeclarator.getRawSignature())
                        && originDeclSpec.getRawSignature().equals(cloneDeclSpec.getRawSignature());
            }
        } else if (origin instanceof IASTStatement) {
            if (clone instanceof IASTStatement) {
                return origin.getRawSignature().equals(clone.getRawSignature());
            }
        }

        return false;
    }

    private synchronized List<IASTNode> searchNodes(IASTNode root, final int lineBoundary, Class<? extends IASTNode> condition) {
        List<IASTNode> output = new ArrayList<>();

        for (IASTNode child : root.getChildren()) {
            if (child != null) {
                if (child.getFileLocation() != null)
                    if (child.getFileLocation().getStartingLineNumber() >= lineBoundary)
                        if (condition.isInstance(child))
                            output.add(child);

                output.addAll(searchNodes(child, lineBoundary, condition));
            }
        }

        return output;
    }
}

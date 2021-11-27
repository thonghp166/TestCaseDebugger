package auto_testcase_generation.testdatagen.se.expander;

import java.util.ArrayList;
import java.util.List;

import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;

import auto_testcase_generation.testdatagen.se.CustomJeval;
import auto_testcase_generation.testdatagen.se.ISymbolicExecution;

/**
 * Add the constraint of denominator to the path constraints.<br/>
 * Ex: "x/y"-------> new constraint: "y>0||y<0" <br/>
 * Ex: "x%y"-------> new constraint: "y>0||y>0"
 *
 * @author DucAnh
 */
public class DenominatorExpander extends AbstractPathConstraintExpander implements IPathConstraintExpander {
    public static void main(String[] args) {
        String[] examples = new String[]{"((tvwb_w)/((tvwhe)*(tvwhe)/10000))<19", "a[i+1]/y==0"};
        AbstractPathConstraintExpander expander = new DenominatorExpander();
        expander.setInputConstraint(examples[0]);
        expander.generateNewConstraints();
        System.out.println(expander.getNewConstraints());
    }

    @Override
    public void generateNewConstraints() {
        List<String> indexConstraints = getDenominatorConstraint(inputConstraint);
        newConstraints.addAll(indexConstraints);

    }

    /**
     * Add the constraint of denominator to the path constraints. Ex:
     * "x/y"-------> new constraint: "y>0"
     *
     * @param constraint
     */
    private List<String> getDenominatorConstraint(String constraint) {
        List<String> newConstraints = new ArrayList<>();

		/*
         * Create a temporary function contain the expression as condition
		 */
        String fn = "void test(){if(" + constraint + ") return 1; else return 0;}";
        ICPPASTFunctionDefinition fnAST = Utils.getFunctionsinAST(fn.toCharArray()).get(0);
        IASTNode firstChild = fnAST.getBody().getChildren()[0];
        List<ICPPASTBinaryExpression> binaryASTs = Utils.getBinaryExpressions(firstChild);

        for (ICPPASTBinaryExpression binaryAST : binaryASTs)
            switch (binaryAST.getOperator()) {
                case IASTBinaryExpression.op_divide:
                case IASTBinaryExpression.op_modulo:
                    String denominator = binaryAST.getOperand2().getRawSignature();

                    String newConstraint = denominator + ">0||" + denominator + "<0";
                    if (new CustomJeval().evaluate(newConstraint).equals(CustomJeval.TRUE)) {
                        // no thing to do
                    } else if (new CustomJeval().evaluate(newConstraint).equals(CustomJeval.FALSE))
                        newConstraints.add(ISymbolicExecution.NO_SOLUTION_CONSTRAINT);
                    else
                        newConstraints.add(newConstraint);
                    break;

                default:
                    break;
            }
        return newConstraints;
    }

}

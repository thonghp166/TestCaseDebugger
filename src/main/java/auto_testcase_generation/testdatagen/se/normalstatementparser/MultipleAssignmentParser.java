package auto_testcase_generation.testdatagen.se.normalstatementparser;

import java.util.List;

import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import auto_testcase_generation.testdatagen.se.ExpressionRewriterUtils;
import auto_testcase_generation.testdatagen.se.memory.PhysicalCell;
import auto_testcase_generation.testdatagen.se.memory.VariableNodeTable;

/**
 * Parse multiple assignments, e.g., "x=y=z+1"
 *
 * @author ducanhnguyen
 */
public class MultipleAssignmentParser extends BinaryAssignmentParser {

    @Override
    public void parse(IASTNode ast, VariableNodeTable table) throws Exception {
        ast = Utils.shortenAstNode(ast);
        if (ast instanceof IASTBinaryExpression) {
            List<String> expressions = Utils.getAllExpressionsInBinaryExpression((IASTBinaryExpression) ast);
            int last = expressions.size() - 1;

            String finalExpression = expressions.get(last);
            finalExpression = ExpressionRewriterUtils.rewrite(table, finalExpression);

			/*
             * All variable corresponding to expressions, except the final
			 * expression, is assigned to the final expression
			 */
            for (int i = 0; i < last; i++) {

                String currentExpression = expressions.get(i);
                PhysicalCell cell = table.findPhysicalCellByName(currentExpression);

                if (cell != null)
                    cell.setValue(finalExpression);
            }
        }
    }

}

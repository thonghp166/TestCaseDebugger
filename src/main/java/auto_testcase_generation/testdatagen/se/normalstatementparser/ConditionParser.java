package auto_testcase_generation.testdatagen.se.normalstatementparser;

import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import auto_testcase_generation.testdatagen.se.ExpressionRewriterUtils;
import auto_testcase_generation.testdatagen.se.memory.VariableNodeTable;

public class ConditionParser extends StatementParser {

    private String newConstraint = "";

    @Override
    public void parse(IASTNode ast, VariableNodeTable table) throws Exception {
        ast = Utils.shortenAstNode(ast);
        newConstraint = ExpressionRewriterUtils.rewrite(table, ast.getRawSignature());
    }

    public String getNewConstraint() {
        return newConstraint;
    }

    public void setNewConstraint(String newConstraint) {
        this.newConstraint = newConstraint;
    }
}

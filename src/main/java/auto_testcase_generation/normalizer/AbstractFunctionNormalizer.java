package auto_testcase_generation.normalizer;

import com.dse.parser.object.IFunctionNode;
import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

/**
 * Abstract class for function normalization level
 *
 * @author ducanhnguyen
 */
public abstract class AbstractFunctionNormalizer extends AbstractNormalizer {
    protected IFunctionNode functionNode;

    public IFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(IFunctionNode functionNode) {
        this.functionNode = functionNode;
        originalSourcecode = functionNode.getAST().getRawSignature();
    }

    public IASTFunctionDefinition getNormalizedAST() {
        return Utils.getFunctionsinAST(normalizeSourcecode.toCharArray()).get(0);
    }

    @Override
    @Deprecated
    public String getOriginalSourcecode() {
        return super.getOriginalSourcecode();
    }

    @Override
    @Deprecated
    public void setOriginalSourcecode(String originalSourcecode) {
        super.setOriginalSourcecode(originalSourcecode);
    }
}

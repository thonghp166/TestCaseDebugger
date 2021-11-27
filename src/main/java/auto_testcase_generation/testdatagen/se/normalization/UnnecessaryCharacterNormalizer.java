package auto_testcase_generation.testdatagen.se.normalization;

import auto_testcase_generation.normalizer.AbstractStatementNormalizer;
import auto_testcase_generation.normalizer.IStatementNormalizer;
import com.dse.util.IRegex;

/**
 * Remove unnecessary characters (e.g., continuous spaces)
 *
 * @author DucAnh
 */
public class UnnecessaryCharacterNormalizer extends AbstractStatementNormalizer
        implements IPathConstraintNormalizer, IStatementNormalizer {

    @Override
    public void normalize() {
        if (originalSourcecode != null && originalSourcecode.length() > 0)
            normalizeSourcecode = deleteUnnecessaryCharacter(originalSourcecode);
        else
            normalizeSourcecode = originalSourcecode;
    }

    /**
     * Remove unnecessary characters (e.g., continuous spaces)
     *
     * @param expression
     * @return
     */
    private String deleteUnnecessaryCharacter(String expression) {
        /*
		 * Ex: s=' ' ----->s="@@@@"
		 */
        expression = expression.replaceAll("' '", "@@@@");
		/*
		 * Remove all unnecessary spaces
		 */
        expression = expression.replaceAll(IRegex.SPACES, "");
		/*
		 * Restore
		 * 
		 * Ex: s="@@@@" -----> s=' '
		 */
        expression = expression.replaceAll("@@@@", "' '");
        return expression;
    }
}

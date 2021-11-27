package auto_testcase_generation.instrument;

import com.dse.parser.ProjectParser;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.object.INode;
import com.dse.parser.object.MacroFunctionNode;
import com.dse.search.Search;
import com.dse.search.condition.MacroFunctionNodeCondition;
import com.dse.util.Utils;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.io.File;
import java.util.List;

/**
 * Instrument macro function.
 * <p>
 * Example:
 *
 * <pre>
 * #define SKIP_SPACES(p, limit)  \
 * char *lim = (limit);         \
 * while (p < lim) {            \
 * if (*p++ != ' ') {         \
 * p--; break; }}
 *
 * </pre>
 */
public class FunctionInstrumentationForMacro extends AbstractFunctionInstrumentation{
    private IASTPreprocessorFunctionStyleMacroDefinition macroFunctionNode;
    private String functionPath;

    public FunctionInstrumentationForMacro(IASTPreprocessorFunctionStyleMacroDefinition macroFunctionNode){
        this.macroFunctionNode = macroFunctionNode;
    }

    public static void main(String[] args) throws Exception {
        ProjectParser parser = new ProjectParser(new File("/home/lamnt/IdeaProjects/akautauto/datatest/fsoft/gcem/tests"));
        parser.setExpandTreeuptoMethodLevel_enabled(true);

        List<INode> nodes = Search.searchNodes(parser.getRootTree(), new MacroFunctionNodeCondition(),
                "GCEM_TEST_COMPARE_VALS(gcem_fn,std_fn,...)");
        System.out.println(nodes.size());
        MacroFunctionNode function = (MacroFunctionNode) nodes.get(3);
//        System.out.printlssssssssssssssssssssssxx-pn(new FunctionInstrumentationForSubCondition(function.convertMacroFunctionToRealFunction(function.getAST()))
//                .generateInstrumentedFunction());

        FunctionInstrumentationForMacro functionInstrumentationForMacro = new FunctionInstrumentationForMacro(function.getAST());
        functionInstrumentationForMacro.setFunctionPath(function.getAbsolutePath());
        System.out.println("instrument = " + functionInstrumentationForMacro.generateInstrumentedFunction());
    }

    @Override
    public String generateInstrumentedFunction() {
        if (macroFunctionNode == null || macroFunctionNode == null)
            return "";

        int type = isMacroFunction(macroFunctionNode);
        if (type != FUNCTION_LIKE_MACROS)
            return "";

//        System.out.println("macroFunctionNode.getFileLocation().getNodeOffset() = " + macroFunctionNode.getFileLocation().getNodeOffset());
//        System.out.println("macroFunctionNode.getFileLocation().getStartingLineNumber() = " + macroFunctionNode.getFileLocation().getStartingLineNumber());
        IASTFunctionDefinition newFunctionAST = new MacroFunctionNode().convertMacroFunctionToRealFunction(macroFunctionNode);
//        System.out.println("newFunctionAST.getFileLocation().getNodeOffset() = " + newFunctionAST.getFileLocation().getNodeOffset());
//        System.out.println("newFunctionAST.getFileLocation().getStartingLineNumber() = " + newFunctionAST.getFileLocation().getStartingLineNumber());

        if (isOneStatementMacro(macroFunctionNode)) {
//            System.out.println("xxx[" + macroFunctionNode.getRawSignature() + "]: " + true);
            return "";
        }

        if (newFunctionAST != null) {
            System.out.println(newFunctionAST.getRawSignature());
            FunctionInstrumentationForAllCoverages instrumentationForAllCoverages =
                    new FunctionInstrumentationForAllCoverages(newFunctionAST, null);
            instrumentationForAllCoverages.setFunctionPath(functionPath);
            String instrumentation = instrumentationForAllCoverages.generateInstrumentedFunction();

            try {
                IASTTranslationUnit newAST = new SourcecodeFileParser().getIASTTranslationUnit(instrumentation.toCharArray());
                IASTFunctionDefinition iastFunctionDefinition = (IASTFunctionDefinition) newAST.getChildren()[0];

                instrumentation = normalizeToMacro(iastFunctionDefinition);
                return instrumentation;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else
            return "";
    }

    private boolean isOneStatementMacro(IASTPreprocessorFunctionStyleMacroDefinition macroDefinition) {
        String body = macroDefinition.getRawSignature().substring(macroDefinition.getRawSignature().indexOf(")") + 1);
        body = body.replace("\\", "").trim();
        if (body.startsWith("{") && body.endsWith("}"))
            body = body.substring(1, body.length() - 1);

        IASTNode ast = Utils.convertToIAST(body);
        if (ast instanceof CPPASTDoStatement || ast instanceof CPPASTWhileStatement || ast instanceof CPPASTForStatement
                || ast instanceof CPPASTIfStatement || ast instanceof CPPASTSwitchStatement)
            return false;
        else if (ast instanceof IASTCompoundStatement)
            if (ast.getChildren().length == 1)
                return true;
            else
                return false;

        return true;
    }

    private String getOriginalName(){
        return macroFunctionNode.getRawSignature().substring(0, macroFunctionNode.getRawSignature().indexOf(")") + 1);
    }

    private String normalizeToMacro(IASTFunctionDefinition instrumentNode) {
        String normalizedBody = instrumentNode.getBody().getRawSignature();
        normalizedBody = normalizedBody.replace("\r", "\n");
        normalizedBody = normalizedBody.replace("\n", "\\\n");
        return this.getOriginalName() + " " + normalizedBody;
    }

    private int isMacroFunction(IASTPreprocessorFunctionStyleMacroDefinition macroDefinition) {
        if (macroDefinition.getParameters().length > 0) {
            return FUNCTION_LIKE_MACROS;
        } else
            return OTHER;
    }

    public IASTPreprocessorFunctionStyleMacroDefinition getMacroFunctionNode() {
        return macroFunctionNode;
    }

    public void setMacroFunctionNode(IASTPreprocessorFunctionStyleMacroDefinition macroFunctionNode) {
        this.macroFunctionNode = macroFunctionNode;
    }

    public String getFunctionPath() {
        return functionPath;
    }

    public void setFunctionPath(String functionPath) {
        this.functionPath = functionPath;
    }

    // https://gcc.gnu.org/onlinedocs/cpp/Object-like-Macros.html#Object-like-Macros
    public static final int OBJECT_LIKE_MACROS = 0;
    // https://gcc.gnu.org/onlinedocs/cpp/Function-like-Macros.html#Function-like-Macros
    public static final int FUNCTION_LIKE_MACROS = 1;

    public static final int OTHER = 2;

}

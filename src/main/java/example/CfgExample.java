package example;

import auto_testcase_generation.cfg.CFGGenerationforBranchvsStatementvsBasispathCoverage;
import auto_testcase_generation.cfg.ICFG;
import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;

import java.io.File;

/**
 * Function:
 *
 *
 * <pre>
 int compare_string(char *first, char *second){
 while(*first==*second){                // visited
    if ( *first == '\0' || *second == '\0' )                // visited
        break;

    first++;
    second++;
 }
 if( *first == '\0' && *second == '\0' )
    return 0;
 else
    return -1;
 }
 * </pre>
 */
public class CfgExample {

    public static void main(String[] args) throws Exception {
        // Generate CFG
        ProjectParser parser = new ProjectParser(new File(Paths.SYMBOLIC_EXECUTION_TEST));
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        INode function = Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), "compare_string(char*,char*)").get(0);
        System.out.println(((IFunctionNode) function).getAST().getRawSignature());

        CFGGenerationforBranchvsStatementvsBasispathCoverage cfgGen = new CFGGenerationforBranchvsStatementvsBasispathCoverage((IFunctionNode) function);
        ICFG cfg = cfgGen.generateCFG();
        cfg.setIdforAllNodes();
        System.out.println(cfg.toString());

        // Set visited statements
        cfg.getAllNodes().get(2).setVisit(true);
        cfg.getAllNodes().get(6).setVisit(true);
        cfg.getAllNodes().get(5).setVisit(true);
    }
}

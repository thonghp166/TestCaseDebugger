package example;

import auto_testcase_generation.cfg.CFGGenerationforBranchvsStatementvsBasispathCoverage;
import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.coverage.CFGUpdater_Mark;
import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.IFunctionNode;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.util.Utils;
import com.dse.util.AkaLogger;

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
public class CfgExample2 {
    final static AkaLogger logger = AkaLogger.get(CfgExample2.class);

    public static void main(String[] args) throws Exception {
        // get visited statements
        String content = Utils.readFileContent(new File("local/working-directory/Algorithms/testpaths/Tritype@0.tp"));
        content = content.replace("\r", "\n");
        content = content.replace("\n\n", "\n");
        String[] lines = content.split("\n");

        // get function
        ProjectParser parser = new ProjectParser(new File(Paths.JOURNAL_TEST));
        parser.setExpandTreeuptoMethodLevel_enabled(true);
        IFunctionNode function = (IFunctionNode) Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), "Tritype(int,int,int)").get(0);
        System.out.println(((IFunctionNode) function).getAST().getRawSignature());

        // Generate CFG of function
        CFGGenerationforBranchvsStatementvsBasispathCoverage cfgGen = new CFGGenerationforBranchvsStatementvsBasispathCoverage();
        cfgGen.setFunctionNode((IFunctionNode) function);
        ICFG cfg = cfgGen.generateCFG();
        cfg.setFunctionNode((IFunctionNode) function);
        cfg.resetVisitedStateOfNodes();
        cfg.setIdforAllNodes();
        System.out.println(cfg.toString());

        //
        TestpathString_Marker testpath = new TestpathString_Marker();
        testpath.setEncodedTestpath(lines);
        logger.debug(testpath.getEncodedTestpath());

        CFGUpdater_Mark updater  = new CFGUpdater_Mark(testpath, cfg);
        updater.updateVisitedNodes();
        logger.debug("visited statements: " + cfg.getVisitedStatements());
        logger.debug("Visited branches: " + cfg.getVisitedBranches());
        logger.debug("Visited nodes: " + updater.getUpdatedCFGNodes());

    }
}
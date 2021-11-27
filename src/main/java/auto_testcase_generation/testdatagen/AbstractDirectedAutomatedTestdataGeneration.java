package auto_testcase_generation.testdatagen;

import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.cfg.object.ConditionCfgNode;
import auto_testcase_generation.cfg.testpath.IStaticSolutionGeneration;
import auto_testcase_generation.cfg.testpath.StaticSolutionGeneration;
import auto_testcase_generation.testdatagen.se.IPathConstraints;
import auto_testcase_generation.testdatagen.se.ISymbolicExecution;
import auto_testcase_generation.testdatagen.se.memory.ISymbolicVariable;
import auto_testcase_generation.testdatagen.strategy.AbstractPathSelectionStrategy;
import auto_testcase_generation.testdatagen.strategy.BFSSelection;
import auto_testcase_generation.testdatagen.strategy.PathSelectionOutput;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.IFunctionNode;
import com.dse.util.Utils;
import com.dse.util.AkaLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
public abstract class AbstractDirectedAutomatedTestdataGeneration extends AbstractAutomatedTestdataGeneration {
    private final static AkaLogger logger = AkaLogger.get(AbstractDirectedAutomatedTestdataGeneration.class);

    /**
     * Contain information about the negation
     *
     * @author Duc Anh Nguyen
     */
    class TheNextTestdata {
        String testdata;
        ConditionCfgNode negatedNode;
        IPathConstraints negatedConstraints;

        public String getTestdata() {
            return testdata;
        }

        public ConditionCfgNode getNegatedNode() {
            return negatedNode;
        }

        public void setTestdata(String testdata) {
            this.testdata = testdata;
        }

        public void setNegatedNode(ConditionCfgNode negatedNode) {
            this.negatedNode = negatedNode;
        }

        public IPathConstraints getNegatedConstraints() {
            return negatedConstraints;
        }

        public void setNegatedConstraints(IPathConstraints negatedConstraints) {
            this.negatedConstraints = negatedConstraints;
        }
    }

    /**
     * Used for the optimization of the next test data generation
     *
     * @author Duc Anh Nguyen
     */
    class Optimization {
        private List<IPathConstraints> solvedPathConstraints = new ArrayList<>();
        private List<String> generatedTestdata = new ArrayList<>();

        public List<IPathConstraints> getSolvedPathConstraints() {
            return solvedPathConstraints;
        }

        public void setSolvedPathConstraints(List<IPathConstraints> solvedPathConstraints) {
            this.solvedPathConstraints = solvedPathConstraints;
        }

        public List<String> getGeneratedTestdata() {
            return generatedTestdata;
        }

        public void setGeneratedTestdata(List<String> generatedTestdata) {
            this.generatedTestdata = generatedTestdata;
        }

    }
    public AbstractDirectedAutomatedTestdataGeneration(IFunctionNode fn) {
        super(fn);
    }

    /**
     * Find the next test data
     *
     * @param se
     * @param originalFunction
     * @return
     * @throws Exception
     */
    protected TheNextTestdata generateTheNextTestdata(ISymbolicExecution se, IFunctionNode originalFunction,
                                                      Optimization optimization) throws Exception {
        TheNextTestdata information = new TheNextTestdata();

        boolean isFoundSolution = false;
        boolean canNegateCondition = true;
        String theNextTestdata = "";
        IPathConstraints negatedConstraints = null;

        int MAXIMUM_TRIES = 30; // by default
        while (!isFoundSolution && canNegateCondition && se.getConstraints().size() >= 1 && MAXIMUM_TRIES >= 0) {
            MAXIMUM_TRIES--;

            AbstractPathSelectionStrategy strategy = new BFSSelection();

            logger.debug("STRATEGY = " + strategy.getClass());
            strategy.setSolvedPathConstraints(optimization.getSolvedPathConstraints());
            strategy.setOriginalConstraints(se.getConstraints());
            PathSelectionOutput pathSelectionOutput = strategy.negateTheOriginalPathConstraints();
            negatedConstraints = pathSelectionOutput.getNegatedPathConstraints();

            optimization.setSolvedPathConstraints(strategy.getSolvedPathConstraints());
            canNegateCondition = !pathSelectionOutput.isNegateAllConditions();

            // logger.debug(
            // "[Optimization] Solved path constraints set = " +
            // optimization.getSolvedPat
            // hConstraints().size());

            if (negatedConstraints != null) {
                // Solve path constraints
                logger.debug("Negated Constraints: \n" + negatedConstraints.toString()
                        .replace(ISymbolicVariable.PREFIX_SYMBOLIC_VALUE, "")
                        .replace(ISymbolicVariable.SEPARATOR_BETWEEN_STRUCTURE_NAME_AND_ITS_ATTRIBUTES, ".")
                        .replace(ISymbolicVariable.ARRAY_CLOSING, "]").replace(ISymbolicVariable.ARRAY_OPENING, "["));
                theNextTestdata = new StaticSolutionGeneration().solve(negatedConstraints, originalFunction);
                logger.debug("Solving done");

                if (theNextTestdata.equals(IStaticSolutionGeneration.NO_SOLUTION)) {
                    logger.info("No solution. Continue negating.");
                    isFoundSolution = false;
                } else if (theNextTestdata.equals(IStaticSolutionGeneration.EVERY_SOLUTION)) {
                    isFoundSolution = true;
                    // Just pick a random test data
                    theNextTestdata = initializeTestdataAtRandom();
                    logger.info("May solution. Choose a solution. Next test data = <b>" + theNextTestdata + "</b>");
                } else {
                    if (optimization.getGeneratedTestdata().contains(theNextTestdata)) {
                        isFoundSolution = false;
                        logger.info("Found a solution but it is duplicated.");
                    } else {
                        isFoundSolution = true;
                        logger.info("Found a solution but it is not duplicated. Next test data = <b>" + theNextTestdata
                                + "</b>");
                    }
                }
            } else {
                logger.info("Can not negate any condition. We start generating test data at random");
                canNegateCondition = false;
                isFoundSolution = true;
                theNextTestdata = initializeTestdataAtRandom();
            }
        }
        if (isFoundSolution) {
            optimization.getGeneratedTestdata().add(theNextTestdata);
            information.setTestdata(theNextTestdata);
            information.setNegatedConstraints(negatedConstraints);
        }
        return information;
    }

    /**
     * Generate normalized control flow graph<br/>
     * The testing function should be normalized into a unique format. <br/>
     * Ex: "int test(int a){a=a>0?1:2;}"---normalize---->"int test(int a){if (a>0)
     * a=1; else a=2;}".
     *
     * @param originalFunction The original function
     * @return
     * @throws Exception
     */
    protected ICFG generateNormalizedCFG(IFunctionNode originalFunction) throws Exception {
        ICFG normalizedCfg = Utils.createCFG(originalFunction, Environment.getInstance().getTypeofCoverage());
        return normalizedCfg;
    }

    /**
     * Execute function under a test data
     *
     * @param fn
     * @param staticSolution
     * @return
     * @throws Exception
     */
    protected ITestdataExecution executeTestdata(IFunctionNode fn, String staticSolution) throws Exception {
//        ITestdataExecution dynamic;
//        int MAX_NUMBER_OF_EXECUTION = 4;
//        int MIN_NUMBER_OF_EXECUTION = 1;
//        do {
//            switch (fn.getFunctionConfig().getTestdataExecStrategy()) {
//                case ITestdataGeneration.TESTDATA_GENERATION_STRATEGIES.MARS2:
//                    dynamic = new FunctionExecution(fn, staticSolution);
//                    break;
//                case ITestdataGeneration.TESTDATA_GENERATION_STRATEGIES.FAST_MARS:
//                    dynamic = new FastFunctionExecution(fn, staticSolution);
//                    break;
//
//                default:
//                    throw new Exception("Wrong test data generation strategy");
//            }
//
//            MIN_NUMBER_OF_EXECUTION++;
//        } while (dynamic.getEncodedTestpath().getEncodedTestpath().length() == 0 && MIN_NUMBER_OF_EXECUTION <= MAX_NUMBER_OF_EXECUTION);
//
//        if (MIN_NUMBER_OF_EXECUTION > MAX_NUMBER_OF_EXECUTION)
//            return null;
//        else
//            return dynamic;
        return null;
    }

//    protected boolean assignPointerToNull() {
//        return new Random().nextInt(2/* default */) == 1;
//    }

    /**
     * Initialize data at random
     *
     * @return
     */
    protected String initializeTestdataAtRandom() {
        StringBuilder testdata = new StringBuilder(); // Ex: a=1;b=2
//        Map<String, String> initialization = new RandomInputGeneration().constructRandomInput(fn.getArguments(), fn.getFunctionConfig(), "");
//        for (String key : initialization.keySet())
//            testdata.append(key).append("=").append(initialization.get(key)).append(";");
        return testdata.toString();
    }

    public static int MAX_ITERATIONS_IN_ONE_DEPTH = 15; // default
    public static int DEPTH = 3; // default
}

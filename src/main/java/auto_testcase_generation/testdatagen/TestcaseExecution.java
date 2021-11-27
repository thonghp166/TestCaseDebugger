package auto_testcase_generation.testdatagen;

import auto_testcase_generation.instrument.FunctionInstrumentationForStatementvsBranch_Markerv2;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.testdataexec.*;
import com.dse.config.CommandConfig;
import com.dse.coverage.SourcecodeCoverageComputation;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.TCExecutionDetailLogger;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.helps.UILogger;
import com.dse.guifx_v3.objects.TestCaseExecutionDataNode;
import com.dse.highlight.SourcecodeHighlighterForCoverage;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.ISourcecodeFileNode;
import com.dse.stub_manager.StubManager;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.util.Utils;
import javafx.scene.control.Alert;

import java.io.File;
import java.util.List;

/**
 * Execute a test case
 */
public class TestcaseExecution extends AbstractTestcaseExecution {
    /**
     * node corresponding with subprogram under test
     */
    private ICommonFunctionNode function;

    @Override
    public void execute() throws Exception {
        if (!(getTestCase() instanceof TestCase)) {
            logger.debug("[" + Thread.currentThread().getName()+"] "+ getTestCase().getName() + " is not a normal test case");
            return;
        }

        TestCase testCase = (TestCase) getTestCase();
        UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Initialize configuration of test case " + testCase.getPath());
        initializeConfigurationOfTestcase(testCase);
        testCase.deleteOldDataExceptValue();
        logger.debug("[" + Thread.currentThread().getName() + "] " + "Start generating test driver for the test case " + getTestCase().getPath());

        // create the right version of test driver generation
        UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Generating test driver of test case " + testCase.getPath());
        testDriverGen = generateTestDriver(testCase);

        if (testDriverGen != null) {
            if (getMode() != IN_AUTOMATED_TESTDATA_GENERATION_MODE) {
                UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Generating stub code of test case " + testCase.getPath());
                StubManager.generateStubCode(testCase);
            }

            CommandConfig testCaseCommandConfig = new CommandConfig().fromJson(testCase.getCommandConfigFile());

            String compileAndLinkMessage = compileAndLink(testCaseCommandConfig);
//            logger.debug("[" + Thread.currentThread().getName()+"] "+ String.format("Compile & Link Message:\n%s\n", compileAndLinkMessage));

            // Run the executable file
            if (new File(testCase.getExecutableFile()).exists()) {
                logger.debug("[" + Thread.currentThread().getName()+"] "+ "Execute " + testCase.getExecutableFile());
                UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Executing " + testCase.getExecutableFile());
                {
                    String message = runExecutableFile(testCaseCommandConfig);
                    logger.debug("[" + Thread.currentThread().getName()+"] "+ "Execute done");
                }

                if (getMode() == IN_DEBUG_MODE) {
                    // nothing to do
                } else {
                    if (new File(testCase.getTestPathFile()).exists()) {
                        analyzeTestpathFile(testCaseCommandConfig, testCase);

                    } else {
                        String msg = "Does not found the test path file when executing " + testCase.getExecutableFile();
                        logger.debug("[" + Thread.currentThread().getName()+"] "+ msg);
                        if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
                                ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
                            UIController.showErrorDialog(msg, "Test case execution", "Fail");
                            UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Execute " + testCase.getPath() + " failed.\nMessage = " + msg);
                            testCase.setStatus(TestCase.STATUS_FAILED);
                            return;
                        }
                        //throw new Exception(msg);
                    }
                }
            } else {
                String msg = "Can not generate executable file " + testCase.getFunctionNode().getAbsolutePath() + "\nError:" + compileAndLinkMessage;
                logger.debug("[" + Thread.currentThread().getName()+"] "+ msg);
                if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
                        ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
                    UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Execute " + testCase.getPath() + " failed.\nMessage = " + msg);
                    UIController.showDetailDialog(Alert.AlertType.ERROR, "Executable file generation","Fail", msg);
                    testCase.setStatus(TestCase.STATUS_FAILED);
                    return;
                }
//                throw new Exception(msg);
            }

        } else {
            String msg = "Can not generate test driver of the test case for the function "
                    + testCase.getFunctionNode().getAbsolutePath();
            logger.debug("[" + Thread.currentThread().getName()+"] "+ msg);
            if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
                    ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
                UIController.showErrorDialog(msg, "Test driver generation", "Fail");
                UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Can not generate test driver for " + testCase.getPath() + ".\nMessage = " + msg);
                testCase.setStatus(TestCase.STATUS_FAILED);
                return;
            }
//            throw new Exception(msg);
        }
        testCase.setStatus(TestCase.STATUS_SUCCESS);
    }

    protected void analyzeTestpathFile(CommandConfig testCaseCommandConfig, TestCase testCase) throws Exception {
        // Read hard disk until the test path is written into file completely
        TestpathString_Marker encodedTestpath = readTestpathFromFile(testCase);

        // shorten test path if it is too long
        encodedTestpath = shortenTestpath(encodedTestpath);

        if (encodedTestpath.getEncodedTestpath().length() > 0) {
            // Only for logging
            computeCoverage(encodedTestpath, testCase);

            logger.debug("[" + Thread.currentThread().getName()+"] "+ "Retrieve the test path file "
                    + testCase.getTestPathFile() + " successfully.");
            logger.debug("[" + Thread.currentThread().getName()+"] "+ "Generate test paths for " + testCase.getName() + " sucessfully");

        } else {
            String msg = "The content of test path file is empty after execution";
            logger.debug("[" + Thread.currentThread().getName()+"] "+ msg);
            if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
                    || */getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
                UIController.showErrorDialog(msg, "Test case execution", "Fail");
                testCase.setStatus(TestCase.STATUS_FAILED);
            }
            throw new Exception(msg);
        }
    }

    public TestDriverGeneration generateTestDriver(ITestCase testCase) throws Exception {
        TestDriverGeneration testDriver = null;

        // create the right version of test driver generation
        switch (getMode()) {
            case IN_AUTOMATED_TESTDATA_GENERATION_MODE:
            /*case IN_EXECUTION_WITHOUT_GTEST_MODE: */{
                initializeCommandConfigToRunTestCase(testCase, true);
                if (Environment.getInstance().isC()){
//                if (Utils.getSourcecodeFile(function) instanceof CFileNode) {
                    testDriver = new TestDriverGenerationforCWithoutGoogleTest();

                } else {
                    testDriver = new TestDriverGenerationForCppWithGoogleTest();
                }
                break;
            }

            case IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE: {
                initializeCommandConfigToRunTestCase(testCase, true);
                if (Environment.getInstance().isC()){
//                if (Utils.getSourcecodeFile(function) instanceof CFileNode) {
//                    testDriver = new TestDriverGenerationforCWithGoogleTest();
                    testDriver = new TestDriverGenerationWithCUnit();
                } else {
                    testDriver = new TestDriverGenerationForCppWithGoogleTest();
                }
                break;
            }

            case IN_DEBUG_MODE: {
                initializeCommandConfigToRunTestCase(testCase, false);
                if (Environment.getInstance().isC()){
//                if (Utils.getSourcecodeFile(function) instanceof CFileNode) {
                    testDriver = new TestDriverGenerationforCDebugger();
                } else {
                    testDriver = new TestDriverGenerationforCppForDebugger();
                }
                break;
            }
        }

        if (testDriver != null) {
            // generate test driver
            testDriver.setTestCase(testCase);
            testDriver.generate();
            String testdriverContent = testDriver.getTestDriver();

            if (testCase.getAdditionalHeaders() != null && testCase.getAdditionalHeaders().length() > 0) {
                testdriverContent = testdriverContent.replace(ITestDriverGeneration.ADDITIONAL_HEADERS, testCase.getAdditionalHeaders());
                Utils.writeContentToFile(testdriverContent, testCase.getSourceCodeFile());
            } else {
                testdriverContent = testdriverContent.replace(ITestDriverGeneration.ADDITIONAL_HEADERS, "");
                Utils.writeContentToFile(testdriverContent, testCase.getSourceCodeFile());
            }
            logger.debug("[" + Thread.currentThread().getName()+"] "+ "Add test driver to " + testCase.getSourceCodeFile() + " done");
        }

        return testDriver;
    }

    protected void computeCoverage(TestpathString_Marker encodedTestpath, TestCase testCase) throws Exception {
        // compute coverage
        logger.debug("[" + Thread.currentThread().getName()+"] "+ "Compute coverage given the test path");

        // coverage computation
        ISourcecodeFileNode srcNode = Utils.getSourcecodeFile(testCase.getFunctionNode());
        String tpContent = Utils.readFileContent(testCase.getTestPathFile());

        SourcecodeCoverageComputation computator = new SourcecodeCoverageComputation();
        computator.setTestpathContent(tpContent);
        computator.setConsideredSourcecodeNode(srcNode);
        computator.setCoverage(Environment.getInstance().getTypeofCoverage());
        computator.compute();

        // highlighter
        SourcecodeHighlighterForCoverage highlighter = new SourcecodeHighlighterForCoverage();
        highlighter.setSourcecode(srcNode.getAST().getRawSignature());
        highlighter.setTestpathContent(tpContent);
        highlighter.setSourcecodePath(srcNode.getAbsolutePath());
        highlighter.setAllCFG(computator.getAllCFG());
        highlighter.setTypeOfCoverage(computator.getCoverage());
        highlighter.highlight();

        // log to details tab of the testcase
        switch (getMode()) {
            case IN_AUTOMATED_TESTDATA_GENERATION_MODE: {
                // get TestCaseExecutionDataNode tuong ung
                TestCaseExecutionDataNode dataNode = TCExecutionDetailLogger.getExecutionDataNodeByTestCase(testCase);

                // log to details tab of the testcase
                StringBuilder tp = new StringBuilder();
                List<String> stms = encodedTestpath
                        .getStandardTestpathByProperty(FunctionInstrumentationForStatementvsBranch_Markerv2.START_OFFSET_IN_FUNCTION);
                if (stms.size() > 0) {
                    for (String stm : stms)
                        tp.append(stm).append("=>");
                    tp = new StringBuilder(tp.substring(0, tp.length() - 2)); //
                    logger.debug("[" + Thread.currentThread().getName() + "] " + "Done. Offsets of execution test path [length=" + stms.size() + "] = " + tp);
                    TCExecutionDetailLogger.logDetailOfTestCase(testCase, "Test case path: " + tp.toString());
                } else{
                    logger.debug("[" + Thread.currentThread().getName() + "] " + "Done. Offsets of execution test path [length=0]");
                    TCExecutionDetailLogger.logDetailOfTestCase(testCase, "No path");
                }
                break;
            }
        }
    }

    public ICommonFunctionNode getFunction() {
        return function;
    }

    public void setFunction(ICommonFunctionNode function) {
        this.function = function;
    }
}
package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.stub_manager.StubManager;
import com.dse.testcase_manager.TestCase;
import com.dse.util.IGTestConstant;

public abstract class TestdriverGenerationWithoutGoogleTest extends TestDriverGeneration {

    @Override
    public String generateTestScript(TestCase testCase, int iterator) throws Exception {
        // STEP 1: assign aka test case name
        String testCaseNameAssign = String.format("%s=\"%s\";", StubManager.AKA_TEST_CASE_NAME, testCase.getName());

        // STEP 2: Generate initialization of variables
        String initialization = "\n/* generateInitialization - begin */\n" +
                generateInitialization(testCase)
                + "\n/* generateInitialization - end */\n";

        // STEP 3: Generate full function call
        String functionCall = "\n/* generateFunctionCall - begin */\n" +
                generateFunctionCall(testCase)
                + "\n/* generateFunctionCall - end*/\n";

        // STEP 4: FCALLS++ - Returned from UUT
        String increaseFcall = "\n/* generateReturnMark - begin */\n" +
                IGTestConstant.INCREASE_FCALLS + generateReturnMark(testCase)
                + "\n/* generateReturnMark - end */\n";

        // STEP 5: repeat iterator
        String singleScript = String.format(
                "\t{\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t}", testCaseNameAssign, initialization, functionCall, increaseFcall);

        String script = "";
        for (int i = 0; i < iterator; i++)
            script += singleScript + "\n";

        // STEP 6: mark beginning and end of test case
        script = wrapScriptInMark(testCase, script);
        script = wrapScriptInTryCatch(script);

        return script;
    }

    @Override
    protected String generateAssertion(TestCase testCase) throws Exception {
        return "";
    }
}

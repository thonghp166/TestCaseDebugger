package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.parser.object.ConstructorNode;
import com.dse.stub_manager.StubManager;
import com.dse.testcase_manager.TestCase;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;

public abstract class TestdriverGenerationWithGoogleTest extends TestDriverGeneration {
    public static String defaultNameOfUnitTest = "defaultNameOfUnitTest";

    @Override
    public String generateTestScript(TestCase testCase, int iterator) throws Exception {
        // STEP 1: assign aka test case name
        String testCaseNameAssign = String.format("%s=\"%s\";", StubManager.AKA_TEST_CASE_NAME, testCase.getName());

        // STEP 2: Generate initialization of variables
        String initialization = generateInitialization(testCase);

        // STEP 3: Generate full function call
        String functionCall = generateFunctionCall(testCase);

        // STEP 4: FCALLS++ - Returned from UUT
        String increaseFcall = "";
        if (testCase.getFunctionNode() instanceof ConstructorNode)
            increaseFcall = "";
        else
            increaseFcall = IGTestConstant.INCREASE_FCALLS + generateReturnMark(testCase);

        // STEP 5: Generation assertion actual & expected values
        String assertion = generateAssertion(testCase);

        // STEP 5: Repeat iterator
        String singleScript = String.format(
                "\t{\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t%s\n" +
                        "\t}", testCaseNameAssign, initialization, functionCall, increaseFcall, assertion);

        String script = "";
        for (int i = 0; i < iterator; i++)
            script += singleScript + "\n";

        // STEP 6: mark beginning and end of test case
        script = wrapScriptInMark(testCase, script);
        script = wrapScriptInTryCatch(script);


        // STEP 7: insert test suite & test case name
        String testSuiteName = testCase.getFunctionNode().getSingleSimpleName();
        String testCaseName = testCase.getName().replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
        testSuiteName = normalizeTestSuiteName(testSuiteName);

        /*
        Google Test:
         * TEST(TestSuite, TestCase) {
         *    AKA_TEST_CASE_NAME = "TestCase";
         *    init variable statements;
         *    function call statements;
         *    assertion statements;
         * }
         */
        return String.format("TEST(%s, %s) {\n%s\n}\n", testSuiteName, testCaseName, script);
    }
}

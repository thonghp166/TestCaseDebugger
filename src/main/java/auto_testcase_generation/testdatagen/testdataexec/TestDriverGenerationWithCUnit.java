package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.parser.object.ConstructorNode;
import com.dse.search.Search2;
import com.dse.stub_manager.StubManager;
import com.dse.testcase_manager.TestCase;
import com.dse.testdata.object.*;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generate test driver for function put in an .c file in executing test data entering by users
 *
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationWithCUnit extends TestDriverGeneration {

	@Override
	public String getTestDriverTemplate() {
		return Utils.readResourceContent(C_WITH_CUNIT_DRIVER_PATH);
	}

	@Override
	public void generate() throws Exception {
		super.generate();

		testDriver = testDriver.replace("{{TEST_CASE_NAME}}", testCase.getName().replace(".", "_"))
				.replace("{{INSERT_PATH_OF_XML_RESULT_HERE}}", testCase.getExecutionResultFile().replace("-Results.xml", ""));
	}

	protected String generateBodyScript(TestCase testCase, int iterator) throws Exception {
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

		return script;
	}

	@Override
	public String generateTestScript(TestCase testCase, int iterator) throws Exception {
		String body = generateBodyScript(testCase, iterator);

//		String testSuiteName = testCase.getFunctionNode().getSingleSimpleName();
		String testCaseName = testCase.getName().replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
//		testSuiteName = normalizeTestSuiteName(testSuiteName);

		return String.format("void AKA_TEST_%s(void) {\n%s\n}\n", testCaseName, body);
        /*
        Google Test:
         * TEST(TestSuite, TestCase) {
         *    AKA_TEST_CASE_NAME = "TestCase";
         *    init variable statements;
         *    function call statements;
         *    assertion statements;
         * }
         */
	}

	protected String generateAssertion(TestCase testCase) throws Exception {
		String assertion = "/* error assertion */";

		IValueDataNode expectedOutputDataNode = Search2.getExpectedOutputNode(testCase.getRootDataNode());

		if (expectedOutputDataNode != null) {// not void function
//			assertion = expectedOutputDataNode.getAssertionForGoogleTest(
//					"CU_ASSERT",
//					IGTestConstant.EXPECTED_OUTPUT,
//					IGTestConstant.ACTUAL_OUTPUT
//			);
//			assertion = assertion.replace(",", "=");
			if (expectedOutputDataNode.getType().equals("void*")){
				assertion = "/*Does not support CU_ASSERT for void pointer comparison*/";
			} else {
				assertion = expectedOutputDataNode.getAssertionForGoogleTest(
					"CU_ASSERT",
					IGTestConstant.EXPECTED_OUTPUT,
					IGTestConstant.ACTUAL_OUTPUT
				);
				assertion = assertion.replace(",", "==");
//				assertion = String.format("CU_ASSERT(%s == %s)", IGTestConstant.EXPECTED_OUTPUT, IGTestConstant.ACTUAL_OUTPUT);
			}
		}
//		else
//			assertion = String.format("%s(1+1 == 2);", "CU_ASSERT");

		// expected values
//		assertion += generateExpectedValueInitialize(testCase);

		return assertion;
	}

	protected String wrapScriptInTryCatch(String script) {
		// no try-catch
		return script;
	}

	private String generateExpectedValueInitialize(TestCase testCase){
		String initialize = "\n/* error expected initialize */";

		SubprogramNode sut = Search2.findSubprogramUnderTest(testCase.getRootDataNode());

		Map<ValueDataNode, ValueDataNode> globalExpectedMap = testCase.getGlobalInputExpOutputMap();

		if (sut != null) {
			initialize = SpecialCharacter.LINE_BREAK;

			List<ValueDataNode> expecteds = new ArrayList<>(sut.getParamExpectedOuputs());
			expecteds.addAll(globalExpectedMap.values());

			for (ValueDataNode expected : expecteds) {
				if (shouldInitializeExpected(expected)) {
					try {
						initialize += expected.getInputForGoogleTest();
						initialize += SpecialCharacter.LINE_BREAK;

						String expectedName = expected.getVituralName();
						String actualName = expected.getVituralName()
								.replaceFirst("\\Q" + IGTestConstant.EXPECTED_PREFIX + "\\E", SpecialCharacter.EMPTY);

						if (globalExpectedMap.containsValue(expected)) {
							for (Map.Entry<ValueDataNode, ValueDataNode> entry : globalExpectedMap.entrySet()) {
								if (entry.getValue() == expected) {
									actualName = entry.getKey().getVituralName();
									break;
								}
							}
						}

						initialize += expected.getAssertionForGoogleTest(IGTestConstant.EXPECT_EQ, expectedName, actualName);

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		return initialize;
	}

	private boolean shouldInitializeExpected(ValueDataNode dataNode) {
		if (dataNode instanceof ArrayDataNode)
			return ((ArrayDataNode) dataNode).isSetSize();

		if (dataNode instanceof PointerDataNode)
			return ((PointerDataNode) dataNode).isSetSize();

		if (dataNode instanceof NormalDataNode)
			return ((NormalDataNode) dataNode).getValue() != null;

		if (dataNode instanceof ClassDataNode) {
			SubClassDataNode subClass = ((ClassDataNode) dataNode).getSubClass();

			if (subClass == null)
				return false;

			ConstructorDataNode constructor = subClass.getConstructorDataNode();

			if (constructor == null)
				return false;

			if (constructor.getChildren().size() == 0)
				return false;

			for (IDataNode argument : constructor.getChildren()) {
				if (!shouldInitializeExpected((ValueDataNode) argument))
					return false;
			}

			return true;
		}

		if (dataNode instanceof EnumDataNode)
			return ((EnumDataNode) dataNode).getValue() != null;

		return true;
	}
}

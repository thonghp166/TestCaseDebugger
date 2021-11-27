package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.testcase_manager.TestCase;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;

/**
 * Generate test driver for function put in an .c file in executing test data entering by users without
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationforCWithoutGoogleTest extends TestDriverGenerationWithCUnit {

	@Override
	protected String generateAssertion(TestCase testCase) throws Exception {
		return SpecialCharacter.EMPTY;
	}

	@Override
	public String generateTestScript(TestCase testCase, int iterator) throws Exception {
		return generateBodyScript(testCase, iterator);
	}

	@Override
	public String getTestDriverTemplate() {
		return Utils.readResourceContent(C_WITHOUT_GTEST_TEST_DRIVER_PATH);
	}
}

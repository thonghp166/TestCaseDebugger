package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.util.Utils;

/**
 * Generate test driver for function put in an .cpp file in executing test data entering by users without
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationforCppWithoutGoogleTest extends TestdriverGenerationWithoutGoogleTest {

    @Override
    public String getTestDriverTemplate() {
        return Utils.readResourceContent(CPP_WITHOUT_GTEST_TEST_DRIVER_PATH);
    }
}

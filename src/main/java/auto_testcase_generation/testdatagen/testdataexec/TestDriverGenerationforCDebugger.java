package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.util.Utils;

/**
 * Generate test driver for function put in an .cpp file in debug mode
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationforCDebugger extends TestDriverGenerationWithCUnit {
    @Override
    public String getTestDriverTemplate() {
        return Utils.readResourceContent(C_DEBUG_TEST_DRIVER_PATH);
    }
}

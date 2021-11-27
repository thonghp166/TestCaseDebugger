package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.testcase_manager.TestCase;
import com.dse.util.Utils;

/**
 * Generate test driver for function put in an .cpp file in executing test data entering by users without
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationforCppForDebugger extends TestDriverGenerationForCppWithGoogleTest {
    @Override
    public String getTestDriverTemplate() {
        return Utils.readResourceContent(CPP_DEBUG_TEST_DRIVER_PATH);
    }

    @Override
    public void generate() throws Exception {
        super.generate();

        testCase.generateDebugCommands();
    }
}

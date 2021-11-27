package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;

import java.util.List;

/**
 * Generate test driver
 *
 * @author Vu + D.Anh
 */
public interface ITestDriverGeneration {
	String TEST_PATH_TAG = "{{INSERT_PATH_OF_TEST_PATH_HERE}}";
	String CLONED_SOURCE_FILE_PATH_TAG = "{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}";
	String TEST_SCRIPTS_TAG = "{{INSERT_TEST_SCRIPTS_HERE}}";

	// just use in C project
	String NAME_TEST_SCRIPT = "{{INSERT_NAME_TEST_SCRIPT_HERE}}";

	// Some test cases needs to include specific headers
	// generated by automated test data generation
	String ADDITIONAL_HEADERS = "/*{{INSERT_ADDITIONAL_HEADER_HERE}}*/";

	String C_DEBUG_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_for_debug.c";
	String CPP_DEBUG_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_for_debug.cpp";
//	String C_WITH_GTEST_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_with_googletest.c";
	String C_WITH_CUNIT_DRIVER_PATH = "/test-driver-templates/testdriver_with_cunit.c";
	String CPP_WITH_GTEST_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_with_googletest.cpp";
	String C_WITHOUT_GTEST_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_without_googletest.c";
	String CPP_WITHOUT_GTEST_TEST_DRIVER_PATH = "/test-driver-templates/testdriver_without_googletest.cpp";

	String getTestDriverTemplate();

	void generate() throws Exception;

	void setTestCase(ITestCase testCases);

	ITestCase getTestCase();

	String generateTestScript(TestCase testCase, int iterator) throws Exception;

	List<String> getTestScripts();

	void setTestScripts(List<String> testScripts);

	String getTestDriver();

	String getTestPathFilePath();

	void setTestPathFilePath(String testPathFilePath);

	List<String> getClonedFilePaths();

	void setClonedFilePaths(List<String> paths);
}

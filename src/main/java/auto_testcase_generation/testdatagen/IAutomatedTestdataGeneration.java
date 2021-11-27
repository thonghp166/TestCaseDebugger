package auto_testcase_generation.testdatagen;

import com.dse.parser.object.ICommonFunctionNode;

/**
 * Generate test data for a function automatically
 *
 * @author ducanhnguyen
 */
public interface IAutomatedTestdataGeneration {
	/**
	 * Generate test data satisfying criterion
	 *
	 * @throws Exception
	 */
	void generateTestdata(ICommonFunctionNode fn) throws Exception;
}
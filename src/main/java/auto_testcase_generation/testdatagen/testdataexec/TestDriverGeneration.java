package auto_testcase_generation.testdatagen.testdataexec;

import com.dse.parser.object.*;
import com.dse.search.Search2;
import com.dse.testcase_manager.*;
import com.dse.testdata.object.*;
import com.dse.util.*;

import java.util.ArrayList;
import java.util.List;

import static com.dse.project_init.ProjectClone.wrapInIncludeGuard;

/**
 * Generate test driver for a function
 *
 * @author Vu + D.Anh
 */
public abstract class TestDriverGeneration implements ITestDriverGeneration {
	protected final static AkaLogger logger = AkaLogger.get(TestDriverGeneration.class);

	protected List<String> testScripts;

	protected ITestCase testCase;

	protected String testPathFilePath;

	protected String testDriver = "";

	protected List<String> clonedFilePaths;

	@Override
	public void generate() throws Exception {
		testPathFilePath = testCase.getTestPathFile();

		testScripts = new ArrayList<>();
		clonedFilePaths = new ArrayList<>();
		String includedPart = "";

		if (testCase instanceof TestCase) {
			String script = generateTestScript((TestCase) testCase, 1);
			testScripts.add(script);

			String path = ((TestCase) testCase).getCloneSourcecodeFilePath();
			clonedFilePaths.add(path);

			includedPart += String.format("#include \"%s\"\n", path);
			includedPart += generateInstanceDeclaration((TestCase) testCase);

		} else if (testCase instanceof CompoundTestCase) {
			List<TestCaseSlot> slots = ((CompoundTestCase) testCase).getSlots();

			for (TestCaseSlot slot : slots) {
				String name = slot.getTestcaseName();
				TestCase element = TestCaseManager.getBasicTestCaseByName(name);

				assert element != null;

				int iterator = slot.getNumberOfIterations();

				String testScript = generateTestScript(element, iterator);
				testScripts.add(testScript);

				String clonedFilePath = element.getCloneSourcecodeFilePath();
				if (!clonedFilePaths.contains(clonedFilePath)) {
					clonedFilePaths.add(clonedFilePath);
					includedPart += String.format("#include \"%s\"\n", Utils.doubleNormalizePath(clonedFilePath));
					includedPart += generateInstanceDeclaration(element);
				}
			}
		}

		String testScriptPart = "";
		for (String item : testScripts) {
			testScriptPart += item + "\n";
		}

		testDriver = getTestDriverTemplate().replace(TEST_PATH_TAG, Utils.doubleNormalizePath(testPathFilePath))
				.replace(CLONED_SOURCE_FILE_PATH_TAG, includedPart)
				.replace(TEST_SCRIPTS_TAG, testScriptPart);
//				.replace(ADDITIONAL_HEADERS,"");

//		if (this instanceof TestDriverGenerationforCWithGoogleTest)
//			testDriver = testDriver.replace(NAME_TEST_SCRIPT,
//					TestdriverGenerationWithGoogleTest.defaultNameOfUnitTest);
	}

	protected String generateReturnMark(TestCase testCase) {
		ICommonFunctionNode sut = testCase.getFunctionNode();

		String markStm;

		if (sut instanceof FunctionNode || sut instanceof MacroFunctionNode) {
			String relativePath = PathUtils.toRelative(sut.getAbsolutePath());
			markStm = String.format("AKA_MARK(\"Return from: %s\");", Utils.doubleNormalizePath(relativePath));
		} else {
			SubprogramNode subprogram = null;

			INode parent = sut.getParent();

			if (sut instanceof IFunctionNode && ((IFunctionNode) sut).getRealParent() != null)
				parent = ((IFunctionNode) sut).getRealParent();

			RootDataNode globalRoot = Search2.findGlobalRoot(testCase.getRootDataNode());

			assert globalRoot != null;
			for (IDataNode globalVar : globalRoot.getChildren()) {
				if (((ValueDataNode) globalVar).getCorrespondingVar() instanceof InstanceVariableNode
						&& ((ValueDataNode) globalVar).getCorrespondingType().equals(parent)
						&& !globalVar.getChildren().isEmpty()
						&& !globalVar.getChildren().get(0).getChildren().isEmpty()) {

					subprogram = (SubprogramNode) globalVar.getChildren().get(0).getChildren().get(0);
				}
			}

			assert subprogram != null;

			String relativePath = PathUtils.toRelative(sut.getAbsolutePath());

			if (subprogram != null)
				markStm = String.format("AKA_MARK(\"Return from: %s|%s\");", Utils.doubleNormalizePath(relativePath), subprogram.getPathFromRoot());
			else
				markStm = String.format("AKA_MARK(\"Return from: %s|%s\");", Utils.doubleNormalizePath(relativePath), "undefined");
		}

		return markStm;
	}

	protected String wrapScriptInTryCatch(String script) {
		return String.format(
				"try {\n" +
						"%s\n" +
						"} catch (std::exception& error) {\n" +
						"AKA_MARK(\"Phat hien loi runtime\");\n" +
						"}", script);
	}

	protected String wrapScriptInMark(TestCase testCase, String script) {
		String beginMark = generateTestPathMark(MarkPosition.BEGIN, testCase);
		String endMark = generateTestPathMark(MarkPosition.END, testCase);

		return beginMark + SpecialCharacter.LINE_BREAK + script + endMark;
	}

	enum MarkPosition {
		BEGIN,
		END
	}

	private String generateInstanceDeclaration(TestCase testCase) {
		RootDataNode root = testCase.getRootDataNode();
		IDataNode globalRoot = Search2.findGlobalRoot(root);

		if (globalRoot != null) {
			for (IDataNode child : globalRoot.getChildren()) {
				if (child instanceof ClassDataNode && !child.getChildren().isEmpty()) {
					VariableNode varNode = ((ClassDataNode) child).getCorrespondingVar();

					if (varNode instanceof InstanceVariableNode) {
						String type = varNode.getRawType();
						String name = child.getVituralName();

						String instanceDefinition = String.format("%s* %s;", type, name);

						return wrapInIncludeGuard(IGTestConstant.GLOBAL_PREFIX + name, instanceDefinition);
					}
				}
			}
		}

		return "";
	}

	private String generateTestPathMark(MarkPosition pos, TestCase testCase) {
		return String.format("AKA_MARK(\"%s OF %s\");", pos, testCase.getName().toUpperCase());
	}

	protected String generateInitialization(TestCase testCase) throws Exception {
		String initialization = "";

		RootDataNode root = testCase.getRootDataNode();
		IDataNode globalRoot = Search2.findGlobalRoot(root);
		IDataNode sutRoot = Search2.findSubprogramUnderTest(root);

		if (globalRoot != null)
			initialization += globalRoot.getInputForGoogleTest();

		if (sutRoot == null)
			initialization = "/* error initialization */";
		else
			initialization += sutRoot.getInputForGoogleTest();

		initialization = initialization.replace("AKA_MARK(\"<<PRE-CALLING>>\");",
				String.format("AKA_MARK(\"<<PRE-CALLING>> Test %s\");", testCase.getName()));

//		initialization = initialization.replace(SpecialCharacter.LINE_BREAK, SpecialCharacter.EMPTY);

		return initialization;
	}

	protected String generateFunctionCall(TestCase testCase) {
		ICommonFunctionNode functionNode = testCase.getFunctionNode();

		String functionCall;

		if (functionNode instanceof ConstructorNode) {
			return SpecialCharacter.EMPTY;
		}

		String returnType = functionNode.getReturnType().trim();
		returnType = VariableTypeUtils.deleteVirtualAndInlineKeyword(returnType);
		returnType = VariableTypeUtils.deleteStorageClasses(returnType);

		if (functionNode instanceof MacroFunctionNode || functionNode.isTemplate()) {
			SubprogramNode sut = Search2.findSubprogramUnderTest(testCase.getRootDataNode());

			if (sut != null)
				returnType = sut.getType();
		}

		if (functionNode instanceof DestructorNode) {
			functionCall = Utils.getFullFunctionCall(functionNode);

		} else if (!returnType.equals(VariableTypeUtils.VOID_TYPE.VOID)) {
			functionCall = returnType + " " + IGTestConstant.ACTUAL_OUTPUT;

			functionCall += "=" + Utils.getFullFunctionCall(functionNode);
		} else
			functionCall = Utils.getFullFunctionCall(functionNode);

		functionCall = functionCall.replaceAll("\\bmain\\b", "AKA_MAIN");

		functionCall = String.format("AKA_MARK(\"<<PRE-CALLING>> Test %s\");%s", testCase.getName(), functionCall);

		return functionCall;
	}

	protected String normalizeTestSuiteName(String testSuiteName) {
		// overloading operator
		testSuiteName = testSuiteName.replace("+", "plus").replace("-", "minus")
				.replace("*", "multiplication").replace("/", "division")
				.replace("%", "mod").replaceAll("[^a-zA-Z_0-9]", "____");
		return testSuiteName;
	}

	protected abstract String generateAssertion(TestCase testCase) throws Exception;

	@Override
	public String toString() {
		return "TestDriverGeneration: " + testDriver;
	}

//	/**
//	 * "static void test(...){...}" ------------> "void test(...){...}"
//	 *
//	 * @param function
//	 */
//	protected String removeStaticInFunctionDefinition(String function) {
//		String oldDefinition = function.substring(0, function.indexOf("("));
//		String newDefinition = oldDefinition.replaceAll("^static\\s*", "");
//		function = function.replace(oldDefinition, newDefinition);
//		return function;
//	}

	@Override
	public List<String> getTestScripts() {
		return testScripts;
	}

	@Override
	public void setTestScripts(List<String> testScripts) {
		this.testScripts = testScripts;
	}

	@Override
	public String getTestDriver() {
		return testDriver;
	}

	public String getTestPathFilePath() {
		return testPathFilePath;
	}

	public void setTestPathFilePath(String testPathFilePath) {
		this.testPathFilePath = testPathFilePath;
	}

	@Override
	public ITestCase getTestCase() {
		return testCase;
	}

	@Override
	public void setTestCase(ITestCase testCase) {
		this.testCase = testCase;
	}

	@Override
	public List<String> getClonedFilePaths() {
		return clonedFilePaths;
	}

	@Override
	public void setClonedFilePaths(List<String> clonedFilePaths) {
		this.clonedFilePaths = clonedFilePaths;
	}
}

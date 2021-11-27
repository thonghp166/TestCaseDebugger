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
 * Old name: TestdriverGenerationforCpp
 *
 * Generate test driver for function put in an .cpp file in executing test data entering by users
 * <p>
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationForCppWithGoogleTest extends TestdriverGenerationWithGoogleTest {
    @Override
    public String getTestDriverTemplate() {
        return Utils.readResourceContent(CPP_WITH_GTEST_TEST_DRIVER_PATH);
    }

    protected String generateAssertion(TestCase testCase) throws Exception {
        String assertion = "/* error assertion */";

        IValueDataNode expectedOutputDataNode = Search2.getExpectedOutputNode(testCase.getRootDataNode());

        if (expectedOutputDataNode != null) // not void function
            assertion = expectedOutputDataNode.getAssertionForGoogleTest(
                    IGTestConstant.ASSERT_EQ,
                    IGTestConstant.EXPECTED_OUTPUT,
                    IGTestConstant.ACTUAL_OUTPUT
            );
        else
            assertion = String.format("%s(1+1, 2);", IGTestConstant.ASSERT_EQ);

        // expected values
        assertion += generateExpectedValueInitialize(testCase);

        return assertion;
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

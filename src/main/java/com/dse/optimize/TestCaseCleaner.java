package com.dse.optimize;

import com.dse.guifx_v3.controllers.TestCasesNavigatorController;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.objects.TestCasesTreeItem;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.testcasescript.TestcaseSearch;
import com.dse.testcasescript.object.ITestcaseNode;
import com.dse.testcasescript.object.TestNewNode;
import com.dse.coverage.TestPathUtils;
import javafx.scene.control.TreeItem;
import com.dse.util.AkaLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseCleaner {
    private static final AkaLogger logger = AkaLogger.get(TestCaseCleaner.class);

    public static List<TestCase> optimizeByFunction(List<TestCase> testCases) {
        List<TestCase> optimizes = new ArrayList<>(testCases);

        int index = 0;

        while (index < optimizes.size()) {
            TestCase testCase = optimizes.get(index);
            List<TestCase> others = optimizes.stream()
                    .filter(tc -> !tc.equals(testCase))
                    .collect(Collectors.toList());

            boolean stop = false;

            int comparision = TestPathUtils.compare(testCase, others);

            switch (Math.abs(comparision)) {
                case TestPathUtils.EQUAL:
                    optimizes.removeAll(others);
                    stop = true;
                    break;
                case TestPathUtils.HAVENT_EXEC:
                    index++;
                    break;
                case TestPathUtils.COMMON_GT:
                    index++;
                    break;
                case TestPathUtils.CONTAIN_GT:
                    // test case contains all others
                    if (comparision > 0) {
                        optimizes.removeAll(others);
                        stop = true;
                    }
                    // others contains current test case
                    // then remove it from list
                    else
                        optimizes.remove(testCase);

                    break;
                case TestPathUtils.SEPARATED_GT:
                    index++;
                    break;
                case TestPathUtils.ERR_COMPARE:
                    stop = true;
                    logger.error("Something wrong between " + testCase.getName() + " and the others");
                    break;
            }

            if (stop)
                break;
        }

        return optimizes;
    }

    // Grouping test case by subprogram under test
    private static Map<ICommonFunctionNode, List<TestCase>> groupTestCaseByFunction(List<ITestCase> testCases) {
        Map<ICommonFunctionNode, List<TestCase>> map = new HashMap<>();

        for (ITestCase testCase : testCases) {
            if (testCase instanceof TestCase) {
                ICommonFunctionNode sut = ((TestCase) testCase).getFunctionNode();

                List<TestCase> list = map.get(sut);
                if (list == null)
                    list = new ArrayList<>();

                if (!list.contains(testCase))
                    list.add((TestCase) testCase);

                map.put(sut, list);
            }
        }

        return map;
    }

    public static void clean(List<ITestCase> testCases) {
        Map<ICommonFunctionNode, List<TestCase>> group = groupTestCaseByFunction(testCases);

        TreeItem<ITestcaseNode> tiRoot = TestCasesNavigatorController
                .getInstance().getTestCasesNavigator().getRoot();
        ITestcaseNode tcRoot = tiRoot.getValue();

        for (Map.Entry<ICommonFunctionNode, List<TestCase>> entry : group.entrySet()) {
            List<TestCase> list = entry.getValue();
            List<TestCase> optimizes = optimizeByFunction(list);
            List<TestCase> unnecessary = list.stream()
                    .filter(tc -> !optimizes.contains(tc))
                    .collect(Collectors.toList());

            for (TestCase testCase : unnecessary) {
                String name = testCase.getName();

                TestNewNode node = TestcaseSearch.getFirstTestNewNodeByName(tcRoot, name);
                TestCasesTreeItem treeItem = TestcaseSearch
                        .searchTestCaseTreeItem((TestCasesTreeItem) tiRoot, node);

                TestCasesNavigatorController.getInstance().deleteTestCase(node, treeItem);
            }
        }

        UIController.showSuccessDialog("Remove redundant test cases at function level successfully", "Clean test case", "Success");
    }
}

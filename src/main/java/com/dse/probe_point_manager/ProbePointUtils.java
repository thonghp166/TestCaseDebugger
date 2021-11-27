package com.dse.probe_point_manager;

import com.dse.compiler.message.ICompileMessage;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.AbstractFunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.probe_point_manager.controllers.FailedToCompilePopupController;
import com.dse.probe_point_manager.objects.ProbePoint;
import com.dse.project_init.ProjectClone;
import com.dse.project_init.ProjectCloneMap2;
import com.dse.testcase_manager.TestCase;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProbePointUtils {
    final static AkaLogger logger = AkaLogger.get(ProbePointUtils.class);

    private static Stage addEditProbePointControllerStage;

//    public static boolean addProbePointInFile(ProbePoint probePoint) {
//        String filePath = ProjectClone.getClonedFilePath(probePoint.getSourcecodeFileNode().getAbsolutePath());
//        insertIncludes(probePoint);
//        return checkCompile(probePoint.getTestCases(), filePath);
//    }

    public static void insertIncludes(ProbePoint probePoint) {
        String filePath = ProjectClone.getClonedFilePath(probePoint.getSourcecodeFileNode().getAbsolutePath());
        Pair<Integer, Integer> pair = calculateLine(probePoint, filePath, true);
        List<String> content = readData(filePath);
        String includeBefore = "#include " + "\"" + probePoint.getBefore() + "\"";
        String includeAfter = "#include " + "\"" + probePoint.getAfter() + "\"";
        content.add(pair.getValue() - 1, includeAfter);
        content.add(pair.getKey(), includeBefore);
        String newContent = String.join("\n", content);
        Utils.writeContentToFile(newContent, filePath);
    }

    public static boolean deleteProbePointInFile(ProbePoint probePoint) {
        removeIncludes(probePoint);
        String filePath = ProjectClone.getClonedFilePath(probePoint.getSourcecodeFileNode().getAbsolutePath());
        return checkCompile(probePoint.getTestCases(), filePath, false);
    }

    public static void removeIncludes(ProbePoint probePoint) {
        String filePath = ProjectClone.getClonedFilePath(probePoint.getSourcecodeFileNode().getAbsolutePath());
        Pair<Integer, Integer> pair = calculateLine(probePoint, filePath, false);
        List<String> content = readData(filePath);
        content.remove(pair.getValue().intValue());
        content.remove(pair.getKey().intValue());
        String newContent = String.join("\n", content);
        Utils.writeContentToFile(newContent, filePath);
    }

    public static boolean checkCompile(List<TestCase> testCases, String filePath, boolean showError) {
        if (testCases.size() == 0) {
            String tempFilePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1) + "temp.cpp";
            StringBuilder builder = new StringBuilder();
            builder.append(fakePreAKA);
            builder.append("#include \"" + filePath + "\"");
            Utils.writeContentToFile(builder.toString(), tempFilePath);
            ICompileMessage compileMessage = Environment.getInstance().getCompiler().compile(tempFilePath);
            if (compileMessage.getType() == ICompileMessage.MessageType.ERROR) {
                logger.debug("Code has bug");
                if (showError) {
                    // show pop up window to log error
                    showCompileError(compileMessage.getMessage());
                }
                Utils.deleteFileOrFolder(new File(tempFilePath));
                return false;
            } else {
                Utils.deleteFileOrFolder(new File(tempFilePath));
                logger.debug("Code is ok");
                return true;
            }
        } else {
            for (TestCase testCase : testCases) {
                if (! checkCompile(testCase, filePath, showError)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void showCompileError(String message) {
        Stage popUpWindow = FailedToCompilePopupController.getPopupWindow(message);

        // block the environment window
        assert popUpWindow != null;
        popUpWindow.initModality(Modality.WINDOW_MODAL);
        if (addEditProbePointControllerStage != null) {
            popUpWindow.initOwner(addEditProbePointControllerStage.getScene().getWindow());
        } else {
            logger.debug("addEditProbePointControllerStage is null");
        }
        popUpWindow.show();
    }

    public static boolean checkCompile(TestCase testCase, String filePath, boolean showError) {
//        String tempFilePath = filePath.substring(0, filePath.lastIndexOf("/") + 1) + "temp.cpp";
        String tempFilePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1) + "temp.cpp";
        StringBuilder builder = new StringBuilder();
        builder.append(fakePreAKA);

        String akaName = "AKA_TC_" + testCase.getName().replace(".", "_").toUpperCase();
        String tempDefinition = "#define " + akaName + " 1\n";
        builder.append(tempDefinition);

        builder.append("#include \"" + filePath + "\"");
        Utils.writeContentToFile(builder.toString(), tempFilePath);
        ICompileMessage compileMessage = Environment.getInstance().getCompiler().compile(tempFilePath);
        if (compileMessage.getType() == ICompileMessage.MessageType.ERROR) {
            logger.debug("Code has bug");
            if (showError) {
                // show pop up window to log error
                showCompileError(compileMessage.getMessage());
            }
            Utils.deleteFileOrFolder(new File(tempFilePath));
            return false;
        } else {
            Utils.deleteFileOrFolder(new File(tempFilePath));
            logger.debug("Code is ok");
            return true;
        }

    }

    private static Pair<Integer, Integer> calculateLine(ProbePoint probePoint, String filePath, boolean isToAdd) {
        IFunctionNode functionNode = probePoint.getFunctionNode();
        ArrayList<ProbePoint> listInFunc = ProbePointManager.getInstance().getFunPpMap().get(functionNode);
        Collections.sort(listInFunc);
        int lineInIgnore = new ProjectCloneMap2(filePath).getLineInFunction((AbstractFunctionNode) functionNode, probePoint.getLineInSourceCodeFile());
        int pos = listInFunc.indexOf(probePoint);
        int preEqualNum = 0;
        int postEqualNum = 0;
        if (pos < 0) pos = 0;
        else {
            int lineInFunc = probePoint.getLineInFunction();
            for (int i = 0; i < listInFunc.size(); i++) {
                ProbePoint temp = listInFunc.get(i);
                if (temp.getLineInFunction() == lineInFunc) {
                    if (i < pos)
                        preEqualNum++;
                    else if (i > pos)
                        postEqualNum++;
                }
            }
        }
        int before = 0;
        int after = 0;
        if (isToAdd) {
            before = lineInIgnore - postEqualNum;
            after = lineInIgnore + preEqualNum + 2;
        } else {
            before = lineInIgnore - 1 - postEqualNum;
            after = lineInIgnore + preEqualNum + 1;
        }
        return new Pair<>(before, after);
    }

    /**
     * Read data from file path
     *
     * @param path path to file
     * @return data in string
     */
    private static List<String> readData(String path) {
        List<String> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                data.add(sCurrentLine);
            }

        } catch (IOException e) {
            // // e.printStackTrace();
        }
        return data;
    }

    private static String fakePreAKA = "#include <string> \n" +
            "bool AKA_MARK(std::string append) {\n" +
            "  return true;\n" +
            "}\n" +
            "\n" +
            "int AKA_FCALLS = 0;\n";

    public static void setAddEditProbePointControllerStage(Stage addEditProbePointControllerStage) {
        ProbePointUtils.addEditProbePointControllerStage = addEditProbePointControllerStage;
    }

    public static Stage getAddEditProbePointControllerStage() {
        return addEditProbePointControllerStage;
    }
}


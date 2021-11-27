package com.dse.guifx_v3.objects.background_task;

import com.dse.compiler.Compiler;
import com.dse.compiler.message.ICompileMessage;
import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.INode;
import com.dse.util.Utils;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.File;
import java.util.List;

public class CompileSourcecodeFilesTask extends Task<Boolean> {
    private List<INode> sourcecodeFileNodes;

    public CompileSourcecodeFilesTask(List<INode> sourcecodeFileNodes) {
        this.sourcecodeFileNodes = sourcecodeFileNodes;
    }
//
//    @Override
//    protected void updateProgress(long workDone, long max) {
//        Platform.runLater(() -> super.updateProgress(workDone, max));
//    }

    /**
     *
     * @return false if we found compilation error
     */
    @Override
    protected Boolean call() throws Exception {
        // some changes make a source code file unable to compile
        String error = "";
        boolean foundError = false;

        int total = sourcecodeFileNodes.size();
        int counter = 0;

        for (INode modifiedSrcFile : sourcecodeFileNodes) {
            if (! isCancelled()) {
                Compiler c = Environment.getInstance().getCompiler();
                ICompileMessage message = c.compile(modifiedSrcFile);
                if (message.getType() == ICompileMessage.MessageType.ERROR) {
                    error += modifiedSrcFile.getAbsolutePath() + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
                    foundError = true;

                }
                counter++;
                updateProgress(counter, total);
            }
        }

        if (foundError) {
            String compilationMessageFile = new WorkspaceConfig().fromJson().getCompilationMessageWhenComplingProject();
            Utils.deleteFileOrFolder(new File(compilationMessageFile));
            Utils.writeContentToFile(error, compilationMessageFile);
            UIController.showDetailDialog(Alert.AlertType.ERROR, "Compilation error", "Unable to compile the environment", error);
        }

        return foundError;
    }

    /**
     *
     * @return false if we found compilation error
     */
//    public static boolean compileSourcecodeFiles(List<INode> sourcecodeFileNodes) {
//
//        // add new Background Task Object to BackgroundTasksMonitor
//        BackgroundTaskObjectController controller = BackgroundTaskObjectController.getNewInstance();
//        controller.setlTitle("Compiling all source code files");
//        controller.getProgressBar().setProgress(0);
//        BackgroundTasksMonitorController.getController().addBackgroundTask(controller);
//
//        // some changes make a source code file unable to compile
//        String error = "";
//        boolean foundError = false;
//
//        int total = sourcecodeFileNodes.size();
//        int counter = 0;
//
//        for (INode modifiedSrcFile : sourcecodeFileNodes) {
//            Compiler c = Environment.getInstance().getCompiler();
//            ICompileMessage message = c.compile(modifiedSrcFile);
//            if (message.getType() == ICompileMessage.MessageType.ERROR) {
//                error += modifiedSrcFile.getAbsolutePath() + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
//                foundError = true;
//            }
//            counter++;
//            int finalCounter = counter;
//            Platform.runLater(() -> controller.getProgressBar().setProgress(finalCounter / total));
//            try {
//                Thread.sleep(2000);
//            } catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//        if (foundError) {
//            String compilationMessageFile = new WorkspaceConfig().fromJson().getCompilationMessageWhenComplingProject();
//            Utils.deleteFileOrFolder(new File(compilationMessageFile));
//            Utils.writeContentToFile(error, compilationMessageFile);
//        }
//        return foundError;
//    }
}

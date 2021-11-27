package auto_testcase_generation.testdatagen;

import auto_testcase_generation.cfg.testpath.ITestpathInCFG;
import auto_testcase_generation.testdata.object.TestpathString_Marker;
import auto_testcase_generation.testdatagen.testdataexec.TestDriverGeneration;
import com.dse.compiler.Terminal;
import com.dse.config.AkaConfig;
import com.dse.config.CommandConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.guifx_v3.helps.UILogger;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.util.CompilerUtils;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTestcaseExecution implements ITestcaseExecution {
    private int mode = IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE; //IN_EXECUTION_WITHOUT_GTEST_MODE; // by default

    private ITestCase testCase;

    protected TestDriverGeneration testDriverGen;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public ITestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(ITestCase testcase) {
        this.testCase = testcase;
    }

    public String compileAndLink(CommandConfig customCommandConfig) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();

        Map<String, String> compilationCommands = customCommandConfig.getCompilationCommands();

        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
        String directory = new File(workspace).getParentFile().getParentFile().getPath();

        // Create an executable file
        logger.debug("[" + Thread.currentThread().getName() + "] " + "Compiling source code files");
        for (String filePath : compilationCommands.keySet()) {
            String compilationCommand = compilationCommands.get(filePath);

            logger.debug("[" + Thread.currentThread().getName()+"] "+"Executing " + compilationCommand);
            UILogger.getUiLogger().log("[" + Thread.currentThread().getName() + "] " + "Executing " + compilationCommand);

            String[] script = CompilerUtils.prepareForTerminal(Environment.getInstance().getCompiler(), compilationCommand);

            String response = new Terminal(script, directory).get();
//            logger.debug("[" + Thread.currentThread().getName()+"] "+"Execute done: " + compilationCommand);

            output.append(response).append("\n");
        }

        logger.debug("[" + Thread.currentThread().getName()+"] "+"Linking object file. Command: " + customCommandConfig.getLinkingCommand());
        UILogger.getUiLogger().log("[" + Thread.currentThread().getName()+"] "+"Linking object file. Command: " + customCommandConfig.getLinkingCommand());

        String[] linkScript = CompilerUtils
                .prepareForTerminal(Environment.getInstance().getCompiler(), customCommandConfig.getLinkingCommand());
        String linkResponse = new Terminal(linkScript, directory).get();
        output.append(linkResponse);

        logger.debug("[" + Thread.currentThread().getName()+"] "+"Execute done: " + customCommandConfig.getLinkingCommand());
        logger.debug("[" + Thread.currentThread().getName()+"] "+"in directory " + directory);


        return output.toString();
    }

    protected CommandConfig initializeCommandConfigToRunTestCase(ITestCase testCase, boolean shouldIncludeGtestLib) {
        /*
         * create the command file of the test case from the original command file
         */
        String rootCommandFile = new WorkspaceConfig().fromJson().getCommandFile();

        CommandConfig commandConfig = testCase.generateCommands(rootCommandFile,
                testCase.getExecutableFile(), shouldIncludeGtestLib);

        commandConfig.exportToJson(new File(testCase.getCommandConfigFile()));

        logger.debug("[" + Thread.currentThread().getName()+"] "+"Create the command file for test case " + testCase.getName() + " at "
                + testCase.getCommandConfigFile());

        return commandConfig;
    }

    protected TestpathString_Marker readTestpathFromFile(ITestCase testCase) throws InterruptedException {
        TestpathString_Marker encodedTestpath = new TestpathString_Marker();

        int MAX_READ_FILE_NUMBER = 10;
        int countReadFile = 0;

        do {
            logger.debug("[" + Thread.currentThread().getName() + "] " + "Finish. We are getting a execution path from hard disk");
            encodedTestpath.setEncodedTestpath(normalizeTestpathFromFile(
                    Utils.readFileContent(testCase.getTestPathFile())));

            if (encodedTestpath.getEncodedTestpath().length() == 0) {
                //initialization = "";
                Thread.sleep(10);
            }

            countReadFile++;
        } while (encodedTestpath.getEncodedTestpath().length() == 0 && countReadFile <= MAX_READ_FILE_NUMBER);

        return encodedTestpath;
    }

    protected String normalizeTestpathFromFile(String testpath) {
        testpath = testpath.replace("\r\n", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
                .replace("\n\r", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
                .replace("\n", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
                .replace("\r", ITestpathInCFG.SEPARATE_BETWEEN_NODES);
        if (testpath.equals(ITestpathInCFG.SEPARATE_BETWEEN_NODES))
            testpath = "";
        return testpath;
    }

    protected TestpathString_Marker shortenTestpath(TestpathString_Marker encodedTestpath) {
        String[] executedStms = encodedTestpath.getEncodedTestpath().split(ITestpathInCFG.SEPARATE_BETWEEN_NODES);
        if (executedStms.length > 0) {
            int THRESHOLD = 200; // by default
            if (executedStms.length >= THRESHOLD) {
                logger.debug("[" + Thread.currentThread().getName()+"] "+"Shorten test path to enhance code coverage computation speed: from "
                        + executedStms.length + " to " + THRESHOLD);
                StringBuilder tmp_shortenTp = new StringBuilder();

                for (int i = 0; i < THRESHOLD - 1; i++) {
                    tmp_shortenTp.append(executedStms[i]).append(ITestpathInCFG.SEPARATE_BETWEEN_NODES);
                }

                tmp_shortenTp.append(executedStms[THRESHOLD - 1]);
                encodedTestpath.setEncodedTestpath(tmp_shortenTp.toString());
            } else {
                logger.debug("[" + Thread.currentThread().getName()+"] "+"No need for shortening test path because it is not too long");
            }
        }
        return encodedTestpath;
    }

    protected void showExecutionResultDialog(ITestCase testCase, String result) {
        Alert.AlertType type;
        String headerText;

        if (result.contains(IGTestConstant.FAILED_FLAG)) {
            type = Alert.AlertType.ERROR;
            headerText = "Fail to execute test case " + testCase.getName();
        } else if (result.contains(IGTestConstant.PASSED_FLAG)) {
            type = Alert.AlertType.INFORMATION;
            headerText = "Execute test case " + testCase.getName() + " successfully";
        } else {
            type = Alert.AlertType.WARNING;
            headerText = "Fail to execute test case " + testCase.getName();

            if (!result.endsWith(SpecialCharacter.LINE_BREAK))
                result += SpecialCharacter.LINE_BREAK;

            result += "Catch a runtime error when execute test case " + testCase.getName();
        }

        String content = result;

        Platform.runLater(() -> UIController.showDetailDialog(type, "Execution Result", headerText, content));
    }

    @Override
    public void initializeConfigurationOfTestcase(ITestCase testCase) {
        /*
         * Update test case
         */
        // test path
        testCase.setTestPathFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"The test path file of test case " + testCase.getName() + ": " + testCase.getTestPathFile());

        // executable file
        testCase.setExecutableFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Executable file of test case " + testCase.getName() + ": " + testCase.getExecutableFile());

        // debug executable file
        testCase.setDebugExecutableFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Debug executable file of test case " + testCase.getName() + ": " + testCase.getDebugExecutableFile());

        // command file
        testCase.setCommandConfigFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Command file of test case " + testCase.getName() + ": " + testCase.getCommandConfigFile());

        // debug file
        testCase.setCommandDebugFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Debug command file of test case " + testCase.getName() + ": " + testCase.getCommandDebugFile());

        // breakpoint
        testCase.setBreakpointPathDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Breakpoint file of test case " + testCase.getName() + ": " + testCase.getBreakpointPath());

        // test case path
        testCase.setTestPathFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Path of the test case " + testCase.getName() + ": " + testCase.getTestPathFile());

        // exec result path
        if (Environment.getInstance().isC()) {
            String path = new WorkspaceConfig().fromJson().getExecutionResultDirectory() + File.separator + testCase.getName() + "-Results.xml";
            testCase.setExecutionResultFile(path);
        } else {
            testCase.setExecutionResultFileDefault();
        }
        logger.debug("[" + Thread.currentThread().getName()+"] "+"Execute Result Path of the test case " + testCase.getName() + ": " + testCase.getExecutionResultFile());

        // source code file path
        testCase.setSourcecodeFileDefault();
        logger.debug("[" + Thread.currentThread().getName()+"] "+"The source code file containing the test case " + testCase.getName() + ": " + testCase.getSourceCodeFile());

        // execution date and time
        testCase.setExecutionDateTime(LocalDateTime.now());

        TestCaseManager.exportTestCaseToFile(testCase);
    }

    protected String runExecutableFile(CommandConfig commandConfig) throws IOException, InterruptedException {
        String executableFilePath = commandConfig.getExecutablePath();

        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
        String directory = new File(workspace).getParentFile().getParentFile().getPath();

        Terminal terminal;

        if (mode == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
            String[] executeCommand = new String[] {executableFilePath, "--gtest_output="
                    + String.format("xml:%s", testCase.getExecutionResultFile())};

            terminal = new Terminal(executeCommand, directory);

        } else
            terminal = new Terminal(executableFilePath, directory);

        Process p = terminal.getProcess();
        p.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop

        if (p.isAlive()) {
            p.destroy(); // tell the process to stop
            p.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
            p.destroyForcibly(); // tell the OS to kill the process
            p.waitFor();
        }

        return  terminal.get();
    }

    public TestDriverGeneration getTestDriverGeneration() {
        return testDriverGen;
    }

    public void setTestDriverGeneration(TestDriverGeneration testDriverGeneration) {
        this.testDriverGen = testDriverGeneration;
    }
}

package com.dse.testcase_manager;

import com.dse.config.CommandConfig;
import com.dse.debugger.utils.DebugCommandConfig;
import com.dse.testcasescript.object.TestNewNode;

import java.time.LocalDateTime;

public interface ITestCase {
    String PROTOTYPE_SIGNAL = "PROTOTYPE_";
    String AKA_SIGNAL = ".";

    String STATUS_NA = "N/A";
    String STATUS_EXECUTING = "executing";
    String STATUS_SUCCESS = "success";
    String STATUS_FAILED = "failed";
    String STATUS_EMPTY = "no status";

    String getName();

    void setName(String name);

    TestNewNode getTestNewNode();

    void setTestNewNode(TestNewNode testNewNode);

    void setCreationDateTime(LocalDateTime creationDateTime);

    LocalDateTime getCreationDateTime();

    String getCreationDate();

    String getCreationTime();

    void setExecutionDateTime(LocalDateTime executionDateTime);

    LocalDateTime getExecutionDateTime();

    String getExecutionDate();

    String getExecutionTime();

    String getStatus();

    void setStatus(String status);

    int[] getExecutionResult();

    void setExecutionResult(int[] result);

    void appendExecutionResult(int[] result);

    String getPath();

    void setPathDefault();

    void setPath(String path);

    String getSourceCodeFile();

    void setSourceCodeFile(String sourcecodeFile);

    String getTestPathFile();

    void setTestPathFileDefault();

    void setTestPathFile(String testPathFile);

    String getExecutableFile();

    void setExecutableFileDefault();

    void setExecutableFile(String executableFile);

    String getCommandConfigFile();

    void setCommandConfigFileDefault();

    void setCommandConfigFile(String commandConfigFile);

    String getCommandDebugFile();

    void setCommandDebugFileDefault();

    void setCommandDebugFile(String commandDebugFile);

    String getBreakpointPath();

    void setBreakpointPathDefault();

    void setBreakpointPath(String breakpointPath);

    void setDebugExecutableFile(String debugExecutableFile);

    void setDebugExecutableFileDefault();

    String getExecutionResultFile();

    void setExecutionResultFile(String executionResultFile);

    void setExecutionResultFileDefault();

    String getDebugExecutableFile();

    void setCurrentCoverageDefault();

    void setCurrentProgressDefault();

    /**
     * Given a test case, we try to generate compilation commands + linking command.
     * <p>
     * The original project has its own commands, all the thing we need to do right now is
     * modify these commands.
     *
     * @return compile command, linker command and execute file path
     */
    CommandConfig generateCommands(String commandFile, String executableFilePath, boolean includeGTest);

    DebugCommandConfig generateDebugCommands();

    void deleteOldData(); // delete all files related to the current test case

    void deleteOldDataExceptValue(); // delete all files related to the current test case except the file storing the value of test cas

//    void generateDebugCommands();

    String getAdditionalHeaders();

    void setAdditionalHeaders(String additionalHeaders);

    void setSourcecodeFileDefault();
}

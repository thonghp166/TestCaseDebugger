package com.dse.debugger.controller;

import auto_testcase_generation.testdatagen.CompoundTestcaseExecution;
import auto_testcase_generation.testdatagen.ITestcaseExecution;
import auto_testcase_generation.testdatagen.TestcaseExecution;
import com.dse.coverage.AbstractCoverageManager;
import com.dse.debugger.TestMode;
import com.dse.debugger.component.breakpoint.BreakPoint;
import com.dse.debugger.gdb.GDB;
import com.dse.debugger.DebugTab;
import com.dse.debugger.gdb.analyzer.GDBStatus;
import com.dse.guifx_v3.controllers.main_view.MDIWindowController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.ISourcecodeFileNode;
import com.dse.project_init.ProjectClone;
import com.dse.report.ExecutionResultReport;
import com.dse.report.ReportManager;
import com.dse.testcase_manager.CompoundTestCase;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class DebugController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(DebugController.class);

    protected static DebugController debugController = null;
    protected static AnchorPane debugPane = null;

    protected static void prepare() {
        FXMLLoader loader = new FXMLLoader((DebugController.class.getResource("/FXML/debugger/Debug.fxml")));
        try {
            debugPane = loader.load();
            debugController = loader.getController();
        } catch (Exception e) {
            logger.debug("Can not load Debugger UI");
            e.printStackTrace();
        }
    }

    public static AnchorPane getDebugPane() {
        if (debugPane == null) prepare();
        return debugPane;
    }

    public static DebugController getDebugController() {
        if (debugController == null) prepare();
        return debugController;
    }

    @FXML
    TabPane codeViewer;

    @FXML
    Pane debugViewer;

    @FXML
    Pane varPane;

    @FXML
    Pane framePane;

    @FXML
    Pane watchesPane;

    @FXML
    Button continueBtn;

    @FXML
    Button nextBtn;

    @FXML
    Button stepInBtn;

    @FXML
    Button stepOutBtn;

    @FXML
    Button stopBtn;

    @FXML
    Button runBtn;

    @FXML
    Tab breakTab;

    protected ITestCase testCase = null;
    protected GDB gdb = null;
    protected TestMode mode = null;

    protected int currentLineHit = 0;

    protected String currentPathHit = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.framePane.getChildren().add(FrameController.getTitledPane());
        this.watchesPane.getChildren().add(WatchController.getTitledPane());
        this.varPane.getChildren().add(VariableController.getTitledPane());
        this.breakTab.setContent(BreakPointController.getPane());
    }

    public boolean loadAndExecuteTestCase(ITestCase testCase) {
        // 1. Load test case
        this.testCase = testCase;
        this.testCase.setPathDefault();
        this.testCase.setBreakpointPathDefault();

        // 2. Execute test case first to generate test driver
        if (this.testCase instanceof TestCase) {
            TestcaseExecution execution = new TestcaseExecution();
            execution.setMode(ITestcaseExecution.IN_DEBUG_MODE);
            execution.setTestCase(this.testCase);
            mode = TestMode.SINGLE;
            try {
                execution.execute();
            } catch (Exception e) {
                logger.debug("Execute single test case failed");
                e.printStackTrace();
            }
        } else if (this.testCase instanceof CompoundTestCase) {
            CompoundTestcaseExecution execution = new CompoundTestcaseExecution();
            execution.setMode(ITestcaseExecution.IN_DEBUG_MODE);
            execution.setTestCase(this.testCase);
            mode = TestMode.COMPOUND;
            try {
                execution.execute();
            } catch (Exception e) {
                logger.debug("Execute compound test case failed");
                e.printStackTrace();
            }
        }

        // 3. Load the saved breakpoint
        String breakPath = this.testCase.getBreakpointPath();
        BreakPointController.getBreakPointController().setup(breakPath);
        return true;
    }

    public void startGDB(ITestCase testCase) {
        setDisableDebugButtons(true);
        this.currentLineHit = 0;
        this.currentPathHit = testCase.getSourceCodeFile();
        GDB gdb = new GDB();
        gdb.setTestcase(testCase);
        gdb.setBreakPointMap(BreakPointController.getBreakPointController().getBreakPointMap());
        new Thread(gdb).start();
        this.gdb = gdb;
        loadFirstTab();
    }

    private void loadFirstTab() {
        String path = "";
        int line = 0;
        if (mode == TestMode.COMPOUND) {
            CompoundTestCase temp = (CompoundTestCase) this.testCase;
            path = temp.getSourceCodeFile();
            // todo: open source code of first slot
            // just open test driver on compound case
        }
        if (mode == TestMode.SINGLE) {
            TestCase temp = (TestCase) this.testCase;
            ICommonFunctionNode functionNode = temp.getFunctionNode();
            line = ProjectClone.getStartLineNumber(functionNode);
            ISourcecodeFileNode cloneFile = Utils.getSourcecodeFile(temp.getFunctionNode());
            path = ProjectClone.getClonedFilePath(cloneFile.getAbsolutePath());

            ObservableList<Tab> tabList = this.codeViewer.getTabs();
            for (Tab tab : tabList) {
                DebugTab debugTab = (DebugTab) tab;
                if (debugTab.getPath().equals(path)) {
                    this.codeViewer.getSelectionModel().select(debugTab);
                    return;
                }
            }
        }
        TreeSet<BreakPoint> listPoint = BreakPointController.getBreakPointController().searchBreaksFromPath(path);
        if (listPoint == null) {
            listPoint = new TreeSet<>();
            BreakPointController.getBreakPointController().getBreakPointMap().put(path, listPoint);
        }
        DebugTab newTab = new DebugTab(path, listPoint);
        if (mode == TestMode.SINGLE) {
            newTab.showLineInViewport(line, false);
        }
        this.codeViewer.getTabs().add(newTab);
        this.codeViewer.getSelectionModel().select(newTab);

    }

    private void setDisableDebugButtons(boolean isDisable) {
        this.stopBtn.setDisable(isDisable);
        this.nextBtn.setDisable(isDisable);
        this.continueBtn.setDisable(isDisable);
        this.stepOutBtn.setDisable(isDisable);
        this.stepInBtn.setDisable(isDisable);
        WatchController.getWatchController().showButton(isDisable);
    }

    public ITestCase getCurrentTestCase() {
        return this.testCase;
    }

    public void turnOff() {
        stop();
        if (gdb != null && testCase != null) {
            gdb.logAllCommands();
            gdb = null;
            testCase = null;
            BreakPointController.getBreakPointController().clearAll();
        }
        debugPane = null;
        debugController = null;
    }

    public GDB getGdb() {
        return this.gdb;
    }

    @FXML
    public void run() {
        if (gdb.isExecuting()) {
            Alert confirmAlert = UIController.showYesNoDialog(Alert.AlertType.CONFIRMATION, "Confirmation",
                    "Rerun debugging",
                    "Stop this session and start new session of debugging");
            Optional<ButtonType> option = confirmAlert.showAndWait();
            if (option.get() == ButtonType.YES) {
                gdb.kill();
                gdb.setExecuting(false);
                run();
            } else {
                confirmAlert.close();
            }
        } else {
            GDBStatus status = gdb.beginDebug();
            handleStatus(status);
            gdb.setExecuting(true);
        }
    }

    @FXML
    public void continueUntilNextBreakPoint() {
        GDBStatus status = gdb.nextBr();
        handleStatus(status);
    }

    @FXML
    public void nextLine() {
        GDBStatus status = gdb.nextLine();
        handleStatus(status);
    }

    @FXML
    public void stepInFunction() {
        GDBStatus status = gdb.stepIn();
        handleStatus(status);
    }

    @FXML
    public void stepOutFunction() {
        GDBStatus status = gdb.stepOut();
        handleStatus(status);
    }

    @FXML
    public void stopExecutingDebug() {
        GDBStatus status = gdb.kill();
        // todo: figure how to handle this case
        gdb.setExecuting(false);
        handleStatus(status);
    }

    public void handleStatus(GDBStatus status) {
        if (status == GDBStatus.EXIT) {
            stop();
            gdb.kill();
            // todo: figure how to handle this case
            gdb.setExecuting(false);
//            if (shouldShowReport) {
            if (testCase instanceof TestCase) {
                AbstractCoverageManager.exportCoveragesOfTestCaseToFile((TestCase) testCase,
                        Environment.getInstance().getTypeofCoverage());
            }
            ExecutionResultReport report = new ExecutionResultReport(testCase, LocalDateTime.now());
            ReportManager.export(report);
            MDIWindowController.getMDIWindowController().viewReport(testCase.getName(), report.toHtml());
//            }
        }
        if (status == GDBStatus.CONTINUABLE) {
            setDisableDebugButtons(false);
            VariableController.getVariableController().updateVariables();
            FrameController.getFrameController().updateFrames();
            WatchController.getWatchController().updateWatches();
        }
        if (status == GDBStatus.ERROR) {
            stop();
            gdb.kill();
            // todo: figure how to handle this case
            gdb.setExecuting(false);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Exit debugger", ButtonType.OK);
                alert.setTitle("Error");
                alert.setHeaderText("Error");
                alert.showAndWait();
            });
        }
    }

    private DebugTab findTabByPath(String path) {
        logger.debug("Finding tab by path: " + path);
        for (Tab tab : this.codeViewer.getTabs()) {
            DebugTab debugTab = (DebugTab) tab;
            if (debugTab.getPath().equals(path)) {
                return debugTab;
            }
        }
        return null;
    }

    public void openCurrentHitLine(int line, String filePath, boolean isHit) {
        DebugTab oldTab = findTabByPath(this.currentPathHit);
        if (oldTab != null && isHit) {
            oldTab.removeStyleAtLine(this.currentLineHit);
        }
        DebugTab curTab = findTabByPath(filePath);
        if (curTab == null) {
            DebugTab newTab = new DebugTab(filePath, BreakPointController.getBreakPointController().getBreakPointMap().get(filePath));
            Platform.runLater(() -> {
                this.codeViewer.getTabs().add(newTab);
                this.codeViewer.getSelectionModel().select(newTab);
                newTab.showLineInViewport(line, isHit);
            });
        } else {
            this.codeViewer.getSelectionModel().select(curTab);
            Platform.runLater(() -> curTab.showLineInViewport(line, isHit));
        }
        this.currentPathHit = filePath;
        this.currentLineHit = line;

    }

    private void stop() {
        DebugTab oldTab = findTabByPath(this.currentPathHit);
        if (oldTab != null) {
            oldTab.removeStyleAtLine(this.currentLineHit);
        }
        this.currentLineHit = 0;
        this.currentPathHit = null;
        VariableController.getVariableController().clearAll();
        FrameController.getFrameController().clearAll();
        WatchController.getWatchController().clearAll();
        setDisableDebugButtons(true);
    }

    public void updateBreakPointFile() {
        logger.debug("Update new break point to file");
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.setPrettyPrinting().create();
        File file = new File(testCase.getBreakpointPath());
        try {
            Type type = new TypeToken<HashMap<String, TreeSet<BreakPoint>>>() {
            }.getType();
            String json = gson.toJson(BreakPointController.getBreakPointController().getBreakPointMap(), type);
            FileWriter writer = new FileWriter(file.getPath());
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            logger.debug("Can not save break point to file");
            e.printStackTrace();
        }
    }

    public void openSource(String nodeName) {
        String envName = Environment.getInstance().getName();
        int start = nodeName.lastIndexOf(File.separator) + 1;
        int end = nodeName.lastIndexOf(".");
        String prefix = nodeName.substring(0, start);
        String name = nodeName.substring(start, end);
        String tail = nodeName.substring(end);
        String newName = prefix + envName + "." + name + ".akaignore" + tail;

        DebugTab newTab = findTabByPath(newName);
        if (newTab == null) {
            ObservableMap<String, TreeSet<BreakPoint>> breakMap = BreakPointController.getBreakPointController().getBreakPointMap();
            breakMap.computeIfAbsent(newName, k -> new TreeSet<>());
            newTab = new DebugTab(newName, breakMap.get(newName));
            this.codeViewer.getTabs().add(newTab);
        }
        this.codeViewer.getSelectionModel().select(newTab);
    }
}

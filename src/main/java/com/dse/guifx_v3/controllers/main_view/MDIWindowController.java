package com.dse.guifx_v3.controllers.main_view;

import com.dse.code_viewer_gui.controllers.FXFileView;
import com.dse.compiler.Compiler;
import com.dse.compiler.message.ICompileMessage;
import com.dse.debugger.controller.DebugController;
import com.dse.guifx_v3.controllers.CompoundTestCaseTreeTableViewController;
import com.dse.guifx_v3.controllers.TestCasesExecutionTabController;
import com.dse.guifx_v3.helps.*;
import com.dse.guifx_v3.objects.SourceCodeViewTab;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.SourcecodeFileNode;
import com.dse.probe_point_manager.ProbePointManager;
import com.dse.probe_point_manager.ProbePointUtils;
import com.dse.probe_point_manager.controllers.AddEditProbePointController;
import com.dse.probe_point_manager.objects.ProbePoint;
import com.dse.probe_point_manager.objects.ProbePointSourceCodeViewTab;
import com.dse.project_init.ProjectClone;
import com.dse.search.Search;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_manager.ITestCase;
import com.dse.testcase_manager.TestCase;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.net.URL;
import java.util.*;

public class MDIWindowController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(MDIWindowController.class);
    /**
     * Singleton pattern like
     */
    private static AnchorPane mdiWindow = null;
    private static MDIWindowController mdiWindowController = null;

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/MDIWindow.fxml"));
        try {
            Parent parent = loader.load();
            mdiWindow = (AnchorPane) parent;
            mdiWindowController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AnchorPane getMDIWindow() {
        if (mdiWindow == null) {
            prepare();
        }
        return mdiWindow;
    }

    public static MDIWindowController getMDIWindowController() {
        if (mdiWindowController == null) {
            prepare();
        }
        return mdiWindowController;
    }

    // mdiTabPane > mdiTabTestCases, mdiTabSourceCode > testCaseViews, sourceCodeView > testCaseView, sourceCodeView
    @FXML
    private TabPane mdiTabPane;
    @FXML
    private Tab mdiTabTestCases;
    @FXML
    private Tab mdiTabSourceCode;
    @FXML
    private Tab mdiTabCompounds;
    @FXML
    private Tab mdiTabDebug;
    @FXML
    private Tab mdiTabFunctionConfiguration;
    @FXML
    private Tab mdiTabTestCasesExecution;
    @FXML
    private Tab mdiTabCoverage;
    @FXML
    private Tab mdiTabReports;
    @FXML
    private Tab mdiTabProbePoint;
    @FXML
    public Tab mdiTabPrototypes;
    @FXML
    private TabPane testCaseViews;
    @FXML
    private TabPane prototypeViews;
    @FXML
    private TabPane sourceCodeViews;
    @FXML
    private TabPane testCasesExecutions;
    @FXML
    private TabPane coverageViews;
    @FXML
    private TabPane functionConfigurations;
    @FXML
    private TabPane compounds; // mdiTabCompounds contains compounds tabpane
    @FXML
    private TabPane reports;
    @FXML
    private TabPane probePointSourceCodeViews;

    @FXML
    private Button bCompileOpeningSourcecodeTab;
    @FXML
    private Button bCompileAllSourcecodeTabs;
    @FXML
    private Button bRefreshAllOpeningSourceCodeFileTabs;
    @FXML
    private Button bSaveOpeningSourcecodeTab;
    @FXML
    private Button bSaveAllOpeningSourcecodeTabs;
    @FXML
    private Button bOpenProbePointMode;

    @FXML
    private ListView<ProbePoint> lvProbePoints;

    private List<String> compoundTestcaseTabNames = new ArrayList<>();
    private List<String> testCasesTabNames = new ArrayList<>();
    private List<String> prototypeTabNames = new ArrayList<>();
    private List<String> sourceCodeTabNames = new ArrayList<>();
    private List<String> functionConfiTabNames = new ArrayList<>();
    private List<String> testCasesExecutionsTabNames = new ArrayList<>();
    private List<String> coverageViewsTabNames = new ArrayList<>();
    private List<String> reportsTabNames = new ArrayList<>();
    private List<String> probePointSourceCodeTabNames = new ArrayList<>();

    private Map<String, CompoundTestCaseTreeTableViewController> compoundTestCaseControllerMap = new HashMap<>();

    @FXML
    public void mdiTabPrototypeClose(Event event) {
        prototypeViews.getTabs().clear();
        prototypeTabNames.clear();
    }

    @FXML
    void mdiTabTestCasesClose() {
        testCaseViews.getTabs().clear();
        testCasesTabNames.clear();
    }

    @FXML
    void mdiTabReportsClose() {
        reports.getTabs().clear();
        reportsTabNames.clear();
    }

    @FXML
    void mdiTabCoverageClose() {
        coverageViews.getTabs().clear();
        coverageViewsTabNames.clear();
    }

    @FXML
    void mdiTabTestCasesExecutionClose() {
        testCasesExecutions.getTabs().clear();
        testCasesExecutionsTabNames.clear();
    }

    @FXML
    void mdiTabFunctionConfigClose() {
        functionConfigurations.getTabs().clear();
        functionConfiTabNames.clear();
    }

    @FXML
    void mdiTabCompoundsClose() {
        compounds.getTabs().clear();
        compoundTestcaseTabNames.clear();
        compoundTestCaseControllerMap.clear();
        Environment.getInstance().setCurrentTestcompoundController(null);
    }

    @FXML
    void mdiTabProbePointClose() {
        probePointSourceCodeViews.getTabs().clear();
        probePointSourceCodeTabNames.clear();
    }

    @FXML
    public void mdiTabSourceCodeClose() {
        sourceCodeViews.getTabs().clear();
        sourceCodeTabNames.clear();
    }

    public void removeViewsAfterChangeCoverageType() {
        mdiTabPane.getTabs().removeAll(mdiTabReports, mdiTabTestCasesExecution, mdiTabCoverage);
        mdiTabReportsClose();
        mdiTabTestCasesExecutionClose();
        mdiTabCoverageClose();
    }

    public void initialize(URL location, ResourceBundle resources) {
        mdiTabPane.getTabs().clear();

        testCaseViews.getTabs().clear();
        sourceCodeViews.getTabs().clear();
        compounds.getTabs().clear();
        functionConfigurations.getTabs().clear();
        testCasesExecutions.getTabs().clear();
        coverageViews.getTabs().clear();
        reports.getTabs().clear();
        probePointSourceCodeViews.getTabs().clear();

        mdiTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        testCaseViews.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        sourceCodeViews.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        prototypeViews.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        compounds.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        functionConfigurations.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        testCasesExecutions.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        coverageViews.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        reports.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        Image compileIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/compile.png"));
        bCompileOpeningSourcecodeTab.setGraphic(new ImageView(compileIcon));
        addCompileASourcecodeFileOption();

        Image compileAllIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/compileAll.png"));
        bCompileAllSourcecodeTabs.setGraphic(new ImageView(compileAllIcon));
        addCompileAllSourcecodeFilesOption();

        Image refreshIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/refresh.png"));
        bRefreshAllOpeningSourceCodeFileTabs.setGraphic(new ImageView(refreshIcon));
        addRefreshAllSourcecodeFileTabsOption();

        Image probpointIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/probe_point_16px.png"));
        bOpenProbePointMode.setGraphic(new ImageView(probpointIcon));
        addOpenProbePointMode();

        Image saveIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/save.png"));
        bSaveOpeningSourcecodeTab.setGraphic(new ImageView(saveIcon));
        addSaveActiveSourcecodeTabOption();

        Image saveAllIcon = new Image(Factory.class.getResourceAsStream("/icons/open_source/saveAll.png"));
        bSaveAllOpeningSourcecodeTabs.setGraphic(new ImageView(saveAllIcon));
        addSaveAllActiveSourcecodeTabOption();

        lvProbePoints.setCellFactory(param -> new ListCell<ProbePoint>() {
            @Override
            protected void updateItem(ProbePoint item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setGraphic(null);
                    setText(null);
                } else if (item.getName() != null) {
                    setText(item.getName());
                    ContextMenu contextMenu = new ContextMenu();
                    setContextMenu(contextMenu);

                    addEditProbePoint(item);
                    addDeleteProbePoint(item);
                }
            }

            private void addEditProbePoint(ProbePoint item) {
                MenuItem mi = new MenuItem("Edit");
                mi.setOnAction(event -> {
                    if (item != null) {
                        Stage window = AddEditProbePointController.getWindow(AddEditProbePointController.TYPE_EDIT, item, lvProbePoints);
                        if (window != null) {
                            window.setResizable(false);
                            window.initModality(Modality.WINDOW_MODAL);
                            window.initOwner(UIController.getPrimaryStage().getScene().getWindow());
                            window.show();
                        }
                    }
                });
                getContextMenu().getItems().add(mi);
            }

            private void addDeleteProbePoint(ProbePoint item) {
                MenuItem mi = new MenuItem("Delete");
                mi.setOnAction(event -> {
//                    if (ProbePointUtils.deleteProbePointInFile(item)) {
//                        lvProbePoints.getItems().remove(item);
//                        ProbePointManager.getInstance().remove(item);
//                        lvProbePoints.refresh();
//                    }

                    ProbePointUtils.deleteProbePointInFile(item);
                    lvProbePoints.getItems().remove(item);
                    ProbePointManager.getInstance().remove(item);
                    lvProbePoints.refresh();
                });
                getContextMenu().getItems().add(mi);
            }
        });
    }

    private void addSaveActiveSourcecodeTabOption() {
        bSaveOpeningSourcecodeTab.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveActiveSourcecodeTab();
            }
        });
    }

    private void addOpenProbePointMode() {
        bOpenProbePointMode.setOnMouseClicked(event -> {
            openProbePointManager();
        });
    }

    private void saveActiveSourcecodeTab() {
        Tab currentTab = sourceCodeViews.getSelectionModel().getSelectedItem();
        INode activeSrcNode = Environment.getInstance().getActiveSourcecodeTabs().get(currentTab);

        String content = getContentOfSourceCodeFileTab(currentTab);
        Utils.writeContentToFile(content, activeSrcNode);

        ProjectClone.cloneASourceCodeFile(activeSrcNode); // ????????????
    }

    private String getContentOfSourceCodeFileTab(Tab tab) {
        // get text inside a tab
        String content = "";
        AnchorPane anchorPane = (AnchorPane) tab.getContent();
        Node firstChild = anchorPane.getChildren().get(0);
        if (firstChild instanceof VirtualizedScrollPane) {
            Object codeArea = ((VirtualizedScrollPane) firstChild).getContent();
            if (codeArea instanceof CodeArea) {
                content = ((CodeArea) codeArea).getText();
            }
        }
        return content;
    }

    private void addSaveAllActiveSourcecodeTabOption() {
        bSaveAllOpeningSourcecodeTabs.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveAllActiveSourcecodeTab();
            }
        });
    }

    private void saveAllActiveSourcecodeTab() {
        for (Tab currentTab : Environment.getInstance().getActiveSourcecodeTabs().keySet()) {
            INode activeSrcNode = Environment.getInstance().getActiveSourcecodeTabs().get(currentTab);
            String content = getContentOfSourceCodeFileTab(currentTab);
            Utils.writeContentToFile(content, activeSrcNode);

            ProjectClone.cloneASourceCodeFile(activeSrcNode);
        }
    }

    private void addRefreshAllSourcecodeFileTabsOption() {
        bRefreshAllOpeningSourceCodeFileTabs.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                for (Tab currentTab : Environment.getInstance().getActiveSourcecodeTabs().keySet()) {
                    INode activeSrcNode = Environment.getInstance().getActiveSourcecodeTabs().get(currentTab);
                    if (activeSrcNode instanceof SourcecodeFileNode) {
                        FXFileView fileView = new FXFileView(activeSrcNode);
                        AnchorPane acp = fileView.getAnchorPane(true);
                        currentTab.setContent(acp);
                    }
                }
            }
        });
    }

    private void addCompileAllSourcecodeFilesOption() {
        bCompileAllSourcecodeTabs.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveAllActiveSourcecodeTab();

                Compiler c = Environment.getInstance().getCompiler();

                for (INode currentSrcFile : Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition())) {
                    UILogger.getUiLogger().log("Compiling " + currentSrcFile.getAbsolutePath());
                    ICompileMessage message = c.compile(currentSrcFile);

                    if (message.getType() == ICompileMessage.MessageType.ERROR) {
                        String error = "Source code file: " + currentSrcFile.getAbsolutePath()
                                + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
                        UIController.showDetailDialog(Alert.AlertType.ERROR, "Compilation message", "Compile message", error);
                        return;
                    }
                }
                UIController.showSuccessDialog("Compile all source code files successfully"
                        , "Compilation message", "Compile message");
            }
        });
    }

    private void addCompileASourcecodeFileOption() {
        bCompileOpeningSourcecodeTab.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveActiveSourcecodeTab();

                Compiler c = Environment.getInstance().getCompiler();
                Tab currentTab = sourceCodeViews.getSelectionModel().getSelectedItem();
                INode currentSrcFile = Environment.getInstance().getActiveSourcecodeTabs().get(currentTab);
                UILogger.getUiLogger().log("Compiling " + currentSrcFile.getAbsolutePath());

                // compile
                ICompileMessage message = c.compile(currentSrcFile);
                if (message.getType() == ICompileMessage.MessageType.ERROR) {
                    String error = "Source code file: " + currentSrcFile.getAbsolutePath()
                            + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
                    UIController.showDetailDialog(Alert.AlertType.ERROR, "Compilation message", "Compile message", error);
                } else {
                    UIController.showSuccessDialog("Compile the opening source code file " + currentSrcFile.getAbsolutePath() + " successfully"
                            , "Compilation message", "Compile message");
                }
            }
        });
    }

    private Tab getCompoundTestcaseTabByName(String name) {
        for (Tab tab : compounds.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getCoverageViewTabByName(String name) {
        for (Tab tab : coverageViews.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getProbePointSourceCodeViewTabByName(String name) {
        for (Tab tab : probePointSourceCodeViews.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getPrototypeTabByName(String name) {
        for (Tab tab : prototypeViews.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getTestCasesTabByName(String name) {
        for (Tab tab : testCaseViews.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getTestCasesExecutionTabByName(String name) {
        for (Tab tab : testCasesExecutions.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getSourceCodeTabByName(String name) {
        for (Tab tab : sourceCodeViews.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getFunctionConfigTabByName(String name) {
        for (Tab tab : functionConfigurations.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    private Tab getReportsTabByName(String name) {
        for (Tab tab : reports.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    public Map<String, CompoundTestCaseTreeTableViewController> getCompoundTestCaseControllerMap() {
        return compoundTestCaseControllerMap;
    }

    public void viewCompoundTestCase(Tab tab, String name) {
        // Add a tab which is used to contain Compounds tabs if it does not exist
        if (!mdiTabPane.getTabs().contains(mdiTabCompounds)) {
            mdiTabPane.getTabs().add(mdiTabCompounds);
        }

        if (compoundTestcaseTabNames.contains(name)) {
            Tab ctcTab = getCompoundTestcaseTabByName(name);
            if (ctcTab != null) {
                compounds.getSelectionModel().select(ctcTab);
                mdiTabPane.getSelectionModel().select(mdiTabCompounds);
            }
        } else {
            // Add a new compound test case tab

            tab.setOnClosed(event -> {
                compoundTestcaseTabNames.remove(name);
                compoundTestCaseControllerMap.remove(name);
                // when there are no compound testcase views open
                if (compoundTestCaseControllerMap.size() == 0) {
                    Environment.getInstance().setCurrentTestcompoundController(null);
                }
                logger.debug("Size of compound testcase controller map: " + compoundTestCaseControllerMap.size());
            });

            compounds.getTabs().add(tab);
            compoundTestcaseTabNames.add(name);
            compounds.getSelectionModel().select(tab);
            mdiTabPane.getSelectionModel().select(mdiTabCompounds);
        }
    }

    public void viewTestCase(AnchorPane testCaseView, String name) {
        // Add a tab which is used to contain test case tabs if it does not exist
        if (!mdiTabPane.getTabs().contains(mdiTabTestCases)) {
            mdiTabPane.getTabs().add(mdiTabTestCases);
        }

        if (testCasesTabNames.contains(name)) { // the test case is opened
            Tab tab = getTestCasesTabByName(name);
            if (tab != null) {
                testCaseViews.getSelectionModel().select(tab);
                mdiTabPane.getSelectionModel().select(mdiTabTestCases);
            }
        } else {
            // Add a new test case tab
            Tab tcTab = new Tab(name);
            tcTab.setContent(testCaseView);
            testCaseViews.getTabs().add(tcTab);
            testCaseViews.getSelectionModel().select(tcTab);
            mdiTabPane.getSelectionModel().select(mdiTabTestCases);
            testCasesTabNames.add(name);

            // event when closing the test case tab
            tcTab.setOnClosed(event -> testCasesTabNames.remove(name));
            tcTab.setOnSelectionChanged(arg0 -> Environment.getInstance().setCurrentTestcaseTab(tcTab.getText()));
        }
    }

    public void viewPrototype(AnchorPane prototypeView, String name) {
        // Add a tab which is used to contain test case tabs if it does not exist
        if (!mdiTabPane.getTabs().contains(mdiTabPrototypes)) {
            mdiTabPane.getTabs().add(mdiTabPrototypes);
        }

        if (prototypeTabNames.contains(name)) { // the test case is opened
            Tab tab = getPrototypeTabByName(name);
            if (tab != null) {
                prototypeViews.getSelectionModel().select(tab);
                mdiTabPane.getSelectionModel().select(mdiTabPrototypes);
            }
        } else {
            // Add a new test case tab
            Tab tcTab = new Tab(name);
            tcTab.setContent(prototypeView);
            prototypeViews.getTabs().add(tcTab);
            prototypeViews.getSelectionModel().select(tcTab);
            mdiTabPane.getSelectionModel().select(mdiTabPrototypes);
            prototypeTabNames.add(name);

            // event when closing the test case tab
            tcTab.setOnClosed(event -> prototypeTabNames.remove(name));
            tcTab.setOnSelectionChanged(arg0 -> Environment.getInstance().setCurrentPrototypeTab(tcTab.getText()));
        }
    }

    public void viewTestCasesExecution(ICommonFunctionNode functionNode) {
        String name = functionNode.getSingleSimpleName();
        if (!mdiTabPane.getTabs().contains(mdiTabTestCasesExecution)) {
            mdiTabPane.getTabs().add(mdiTabTestCasesExecution);
        }
        mdiTabPane.getSelectionModel().select(mdiTabTestCasesExecution);

        if (testCasesExecutionsTabNames.contains(name)) {
            Tab tab = getTestCasesExecutionTabByName(name);
            if (tab != null) {
                testCasesExecutions.getSelectionModel().select(tab);
            }
        } else {
            TestCasesExecutionTabController controller = TCExecutionDetailLogger.getTCExecTabControllerByFunction(functionNode);
            if (controller == null) {
                // init
                TCExecutionDetailLogger.initFunctionExecutions(functionNode);
            }
            controller = TCExecutionDetailLogger.getTCExecTabControllerByFunction(functionNode);
            if (controller != null) {
                Tab tab = controller.getTab();
                testCasesExecutions.getTabs().add(tab);
                testCasesExecutions.getSelectionModel().select(tab);
                testCasesExecutionsTabNames.add(name);

                // event when closing the function configuration tab
                tab.setOnClosed(event -> testCasesExecutionsTabNames.remove(name));
            }
        }
    }

    public void removeTestCasesExecutionTabByFunction(ICommonFunctionNode functionNode) {
        String name = functionNode.getSingleSimpleName();
        Tab tab = getTestCasesExecutionTabByName(name);
        testCasesExecutions.getTabs().remove(tab);
        testCasesExecutionsTabNames.remove(name);
    }

    public void viewFunctionConfiguration(ICommonFunctionNode functionNode) {
        String nameOfFunctionConfigureTab = functionNode.getNameOfFunctionConfigTab();

        if (functionConfiTabNames.contains(nameOfFunctionConfigureTab)) {
            Tab tab = getFunctionConfigTabByName(nameOfFunctionConfigureTab);
            if (tab != null) {
                if (!mdiTabPane.getTabs().contains(mdiTabFunctionConfiguration)) {
                    mdiTabPane.getTabs().add(mdiTabFunctionConfiguration);
                }
                mdiTabPane.getSelectionModel().select(mdiTabFunctionConfiguration);
                functionConfigurations.getSelectionModel().select(tab);
            }
        } else {
            Tab tab = Factory.generateFunctionConfigTab(functionNode);
            if (tab != null) {
                if (!mdiTabPane.getTabs().contains(mdiTabFunctionConfiguration)) {
                    mdiTabPane.getTabs().add(mdiTabFunctionConfiguration);
                }
                mdiTabPane.getSelectionModel().select(mdiTabFunctionConfiguration);

                functionConfigurations.getTabs().add(tab);
                functionConfigurations.getSelectionModel().select(tab);
                functionConfiTabNames.add(nameOfFunctionConfigureTab);

                // event when closing the function configuration tab
                tab.setOnClosed(event -> functionConfiTabNames.remove(nameOfFunctionConfigureTab));
            }
        }
    }

    public void openProbePointManager() {
        if (!mdiTabPane.getTabs().contains(mdiTabProbePoint)) {
            mdiTabPane.getTabs().add(mdiTabProbePoint);
        }
        mdiTabPane.getSelectionModel().select(mdiTabProbePoint);

        Tab currentSourceCodeViewTab = sourceCodeViews.getSelectionModel().getSelectedItem();
        if (currentSourceCodeViewTab != null) {
            SourcecodeFileNode sourcecodeFileNode = (SourcecodeFileNode) ((SourceCodeViewTab) currentSourceCodeViewTab).getSourceCodeFileNode();
            String name = sourcecodeFileNode.getName();

            // index to display tab source code of probe points
            int index = probePointSourceCodeViews.getTabs().size();
            if (probePointSourceCodeTabNames.contains(name)) {
                Tab tab = getProbePointSourceCodeViewTabByName(name);
                if (tab != null) {
                    index = probePointSourceCodeViews.getTabs().indexOf(tab);
                    probePointSourceCodeViews.getTabs().remove(index);
                    probePointSourceCodeTabNames.remove(name);
                }
            }

            ProbePointSourceCodeViewTab scTab = new ProbePointSourceCodeViewTab(sourcecodeFileNode, ProbePointManager.getInstance().getListOfProbePointLines(sourcecodeFileNode));
            probePointSourceCodeViews.getTabs().add(index, scTab);
            probePointSourceCodeTabNames.add(name);
            probePointSourceCodeViews.getSelectionModel().select(scTab);
            scTab.setOnClosed(event -> probePointSourceCodeTabNames.remove(name));
        }
    }

    // hoan_tmp, should be deleted
    public void viewSourceCode(SourcecodeFileNode sourcecodeFileNode) {
        if (!mdiTabPane.getTabs().contains(mdiTabProbePoint)) {
            mdiTabPane.getTabs().add(mdiTabProbePoint);
        }
        mdiTabPane.getSelectionModel().select(mdiTabProbePoint);

        ProbePointSourceCodeViewTab scTab = new ProbePointSourceCodeViewTab(sourcecodeFileNode, new TreeSet<>());
        probePointSourceCodeViews.getTabs().add(scTab);
        probePointSourceCodeViews.getSelectionModel().select(scTab);

//        if (!(sourcecodeFileNode instanceof SourcecodeFileNode))
//            return;
//        String name = sourcecodeFileNode.getName();
//        if (!mdiTabPane.getTabs().contains(mdiTabSourceCode)) {
//            mdiTabPane.getTabs().add(mdiTabSourceCode);
//        }
//
//        if (sourceCodeTabNames.contains(name)) {
//            Tab tab = getSourceCodeTabByName(name);
//            sourceCodeViews.getTabs().remove(tab);
//        }
//
//        TabContent scTab = new TabContent(sourcecodeFileNode.getAbsolutePath(), new TreeSet<>());
//        sourceCodeViews.getTabs().add(scTab);
//        sourceCodeViews.getSelectionModel().select(scTab);
//        sourceCodeTabNames.add(name);
//
//        scTab.setOnClosed(new EventHandler<Event>() {
//            @Override
//            public void handle(Event event) {
//                sourceCodeTabNames.remove(name);
//            }
//        });
    }

    // hoan_tmp
    public void updateLVProbePoints() {
        lvProbePoints.getItems().clear();
        List<ProbePoint> probePoints = ProbePointManager.getInstance().getAllProbePoint();
        lvProbePoints.getItems().addAll(probePoints);
    }

    public void viewSourceCode(INode sourcecodeFileNode, AnchorPane sourceCodeView) {
        if (!(sourcecodeFileNode instanceof SourcecodeFileNode))
            return;
        String name = sourcecodeFileNode.getAbsolutePath().replace(Environment.getInstance().getProjectNode().getAbsolutePath(), "");
        if (!mdiTabPane.getTabs().contains(mdiTabSourceCode)) {
            mdiTabPane.getTabs().add(mdiTabSourceCode);
        }

        mdiTabPane.getSelectionModel().select(mdiTabSourceCode);

        if (sourceCodeTabNames.contains(name)) {
            Tab tab = getSourceCodeTabByName(name);
            sourceCodeViews.getTabs().remove(tab);
        }

        mdiTabPane.getSelectionModel().select(mdiTabSourceCode);

        SourceCodeViewTab scTab = new SourceCodeViewTab(sourcecodeFileNode);
        scTab.setText(name);
        sourceCodeViews.getTabs().add(scTab);
        scTab.setContent(sourceCodeView);
        sourceCodeViews.getSelectionModel().select(scTab);
        sourceCodeTabNames.add(name);

        Environment.getInstance().getActiveSourcecodeTabs().put(scTab, sourcecodeFileNode);

        scTab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                sourceCodeTabNames.remove(name);
                Environment.getInstance().getActiveSourcecodeTabs().remove(event.getSource());
            }
        });
    }


    public void removeAndCreateNewCoverageTab(String tabName, Tab tab) {
        // add coverage tabpane and open it automatically
        if (!mdiTabPane.getTabs().contains(mdiTabCoverage)) {
            mdiTabPane.getTabs().add(mdiTabCoverage);
        }
        mdiTabPane.getSelectionModel().select(mdiTabCoverage);

        // remove the current coverage tab opening this test case
        int index = coverageViews.getTabs().size();
        if (coverageViewsTabNames.contains(tabName)) {
            Tab tmpTab = getCoverageViewTabByName(tabName);
            if (tmpTab != null) {
                index = coverageViews.getTabs().indexOf(tmpTab);
                coverageViews.getTabs().remove(index);
                coverageViewsTabNames.remove(tabName);
            }
        }

        if (tab != null) {
            // add new coverage tab of this test case
            coverageViews.getTabs().add(index, tab);
            coverageViews.getSelectionModel().select(tab);
            coverageViewsTabNames.add(tab.getText());
            // event when closing the function coverages tab
            tab.setOnClosed(event -> coverageViewsTabNames.remove(tab.getText()));
        }
    }

//    public void viewCoverageOfATestcase(TestCase testCase) {
//        Tab tab = Factory.generateCoverageTab(testCase);
//        if (tab != null)
//            removeAndCreateNewCoverageTab(testCase.getName(), tab);
//    }

    public void viewCoverageOfMultipleTestcase(String tabName, List<TestCase> testCases) {
        if (testCases.size() > 0) {
            Tab tab = Factory.generateCoverageTab(tabName, testCases);
            if (tab != null)
                removeAndCreateNewCoverageTab(tabName, tab);
            else {
                UIController.showErrorDialog("Can not open coverage of multiple test cases", "Coverage tab", "Can not open");
            }
        }
    }

    public void viewReport(ITestCase testCase) {
        String name = testCase.getName();
        if (!mdiTabPane.getTabs().contains(mdiTabReports)) {
            mdiTabPane.getTabs().add(mdiTabReports);
        }

        mdiTabPane.getSelectionModel().select(mdiTabReports);
        // index to display tab of the function
        int index = reports.getTabs().size();
        if (reportsTabNames.contains(name)) {
            Tab tab = getReportsTabByName(name);
            if (tab != null) {
                index = reports.getTabs().indexOf(tab);
                reports.getTabs().remove(index);
                reportsTabNames.remove(name);
            }
        }

        Tab tab = Factory.generateReportTab(name, testCase);
        if (tab != null) {
            reports.getTabs().add(index, tab);
            reports.getSelectionModel().select(tab);
            reportsTabNames.add(tab.getText());

            // event when closing the function coverages tab
            tab.setOnClosed(event -> reportsTabNames.remove(tab.getText()));
        }
    }

    public void viewReport(String name, String content) {
        if (!mdiTabPane.getTabs().contains(mdiTabReports)) {
            mdiTabPane.getTabs().add(mdiTabReports);
        }

        mdiTabPane.getSelectionModel().select(mdiTabReports);
        // index to display tab of the function
        int index = reports.getTabs().size();
        if (reportsTabNames.contains(name)) {
            Tab tab = getReportsTabByName(name);
            if (tab != null) {
                index = reports.getTabs().indexOf(tab);
                reports.getTabs().remove(index);
                reportsTabNames.remove(name);
            }
        }

        // Add a new test case report tab
        Tab tab = Factory.generateReportTab(name, content);
        reports.getTabs().add(index, tab);
        reports.getSelectionModel().select(tab);
        reportsTabNames.add(tab.getText());

        // event when closing the function coverages tab
        tab.setOnClosed(event -> reportsTabNames.remove(tab.getText()));
    }

    public boolean checkDebugOpen() {
        return mdiTabPane.getTabs().contains(mdiTabDebug);
    }

    public void viewDebug(ITestCase testCase) {
        if (!mdiTabPane.getTabs().contains(mdiTabDebug)) {
            mdiTabDebug.setContent(DebugController.getDebugPane());
            boolean isExecutable = DebugController.getDebugController().loadAndExecuteTestCase(testCase);
            if (isExecutable) {
                DebugController.getDebugController().startGDB(testCase);
                mdiTabPane.getTabs().add(mdiTabDebug);
            }
        } else {
            if (DebugController.getDebugController().getCurrentTestCase() != testCase) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setGraphic(null);
                alert.setTitle("Notification Dialog");
                alert.setContentText("Change the test case in debug mode?");

                ButtonType confirmBtn = new ButtonType("Yes");
                ButtonType buttonTypeCancel = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(confirmBtn, buttonTypeCancel);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == confirmBtn) {
                    DebugController.getDebugController().turnOff();
                    mdiTabDebug.setContent(DebugController.getDebugPane());
                    boolean isExecutable = DebugController.getDebugController().loadAndExecuteTestCase(testCase);
                    if (isExecutable) {
                        DebugController.getDebugController().startGDB(testCase);
                        mdiTabPane.getTabs().add(mdiTabDebug);
                    }
                } else {
                    alert.close();
                }
            }
        }

        mdiTabDebug.setOnClosed(e -> DebugController.getDebugController().turnOff());
        mdiTabPane.getSelectionModel().select(mdiTabDebug);
    }


    public void removeTestCaseTab(String name) {
        Tab tab = getTestCasesTabByName(name);
        testCaseViews.getTabs().remove(tab);
        testCasesTabNames.remove(name);
    }

    public void removePrototypeTab(String name) {
        Tab tab = getPrototypeTabByName(name);
        prototypeViews.getTabs().remove(tab);
        prototypeTabNames.remove(name);
    }

    public void removeCompoundTestCaseTab(String name) {
        Tab tab = getCompoundTestcaseTabByName(name);
        compounds.getTabs().remove(tab);
        compoundTestcaseTabNames.remove(name);
        compoundTestCaseControllerMap.remove(name);
    }

    public void clear() {
        mdiTabPane.getTabs().clear();

        testCaseViews.getTabs().clear();
        sourceCodeViews.getTabs().clear();
        compounds.getTabs().clear();
        coverageViews.getTabs().clear();
        reports.getTabs().clear();
        prototypeViews.getTabs().clear();
        probePointSourceCodeViews.getTabs().clear();

        compoundTestcaseTabNames.clear();
        testCasesTabNames.clear();
        sourceCodeTabNames.clear();
        coverageViewsTabNames.clear();
        reportsTabNames.clear();
        prototypeTabNames.clear();
        probePointSourceCodeTabNames.clear();
    }
}

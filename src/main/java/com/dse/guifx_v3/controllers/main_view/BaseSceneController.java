package com.dse.guifx_v3.controllers.main_view;

import com.dse.config.AkaConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UILogger;
import com.dse.guifx_v3.objects.background_task.BackgroundTasksMonitorController;
import com.dse.regression.controllers.MessagesPaneTabContentController;
import com.dse.regression.objects.RegressionScript;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.dse.util.AkaLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class BaseSceneController implements Initializable {
    final static AkaLogger logger = AkaLogger.get(BaseSceneController.class);
    /**
     * Singleton patern like
     */
    private static BaseSceneController baseSceneController = null;
    // parent is the container used to create the scene for the primary stage
    private static Scene baseScene;

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/BaseScene.fxml"));
        try {
            Parent parent = loader.load();
            baseScene = new Scene(parent);
            baseSceneController = loader.getController();
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    public static Scene getBaseScene() {
        if (baseScene == null) {
            prepare();
        }
        return baseScene;
    }

    public static BaseSceneController getBaseSceneController() {
        if (baseSceneController == null) {
            prepare();
        }
        return baseSceneController;
    }

    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane spOverall;
    @FXML
    private VBox topVBox;

    @FXML
    private Label lblCurrentWorkspace;

    @FXML
    private Label lCoverageType;
    @FXML
    private Label lRunningProcesses;
    @FXML
    private AnchorPane apMessagesPane;
    @FXML
    private AnchorPane apTerminalPane;
    @FXML
    private TabPane tpMessagesTabPane;

    private Map<String, MessagesPaneTabContentController> nameToMessagesTabControllerMap = new HashMap<>();
    private MessagesPaneTabContentController akaMessagesTabController = null;

//    private double dividerPositionTerminalPane = 0.3;
//    private double dividerPositionMessagesPane = 0.3;
    private double dividerPosition = 0.7;

    public void initialize(URL location, ResourceBundle resources) {
        // set up the local folder
        // the local folder will contain the configuration file of application level, the default working directory, etc.
        setUpLocalFolder();

        // init setting file if it does not exist
        setUpSettingFile();

        // display the working directory
//        setupWorkingDirectory();

        lCoverageType.setText(null);

        // init background Tasks gui controller
        BackgroundTasksMonitorController.getController();
        lRunningProcesses.setText(null);
        lRunningProcesses.setVisible(false);

        // spOverall is the split pane that can contains the main view and the Messages Pane, the TerminalPane
        // spOverall is always contains the main view
        Node main = spOverall.getItems().get(0);
        spOverall.getItems().clear();
        spOverall.getItems().add(main);
        tpMessagesTabPane.getTabs().clear();

        // tab to display messages of aka tool
        akaMessagesTabController = MessagesPaneTabContentController.getInstance();
        if (akaMessagesTabController != null) {
            Tab akaMessagesTab = akaMessagesTabController.getTab();
            akaMessagesTab.setText("AKA_MESSAGES");
            tpMessagesTabPane.getTabs().add(akaMessagesTab);

            UILogger.initializeUiLogger(akaMessagesTabController.getTextArea());
        }

        // Add menu bar
        MenuBar menuBar = MenuBarController.getMenuBar();
        topVBox.getChildren().add(menuBar);

        //LeftPane init
        AnchorPane leftPane = LeftPaneController.getLeftPane();
        mainSplitPane.getItems().add(leftPane);

        // MDI Window init
        AnchorPane mdiWindow = MDIWindowController.getMDIWindow();
        mainSplitPane.getItems().add(mdiWindow);
        double DIVIDER_POSITION = 0.3;
        mainSplitPane.getDividers().get(0).setPosition(DIVIDER_POSITION);
    }

    @FXML
    public void renderBackgroundTasks() {
        if (BackgroundTasksMonitorController.getController().isRendering()) {
            BackgroundTasksMonitorController.getController().hide();
        } else {
            BackgroundTasksMonitorController.getController().render();
        }
    }

    public MessagesPaneTabContentController viewMessagesTab(RegressionScript regressionScript) {
        showMessagesPane();
        String name = regressionScript.getName();
        // index number is location of the tab
        int index = tpMessagesTabPane.getTabs().size();
        if (nameToMessagesTabControllerMap.containsKey(name)) {
            Tab tab = getMessagesTabByName(name);
            if (tab != null) {
                index = tpMessagesTabPane.getTabs().indexOf(tab);
                tpMessagesTabPane.getTabs().remove(index);
                nameToMessagesTabControllerMap.remove(name);
            }
        }

        // Add a new messages tab
        MessagesPaneTabContentController controller = MessagesPaneTabContentController.getInstance();
        if (controller != null) {
            Tab tab = controller.getTab();
            tab.setText(name);

            tpMessagesTabPane.getTabs().add(index, tab);
            tpMessagesTabPane.getSelectionModel().select(tab);
            nameToMessagesTabControllerMap.put(tab.getText(), controller);

            // event when closing the function coverages tab
            tab.setOnClosed(event -> nameToMessagesTabControllerMap.remove(tab.getText()));

            return controller;
        } else {
            return null;
        }
    }

    @FXML
    public void showOrHideMessagesPane() {
        if (spOverall.getItems().size() > 1 && spOverall.getItems().get(1) == apMessagesPane) {
            hidePane();
        } else {
            showMessagesPane();
        }
    }

    @FXML
    public void showOrHideTerminalPane() {
        if (spOverall.getItems().size() > 1 && spOverall.getItems().get(1) == apTerminalPane) {
            hidePane();
        } else {
            showTerminalPane();
        }
    }

    private void hidePane() {
        Node main = spOverall.getItems().get(0);
        dividerPosition = spOverall.getDividers().get(0).getPosition();
        spOverall.getItems().clear();
        spOverall.getItems().add(main);
    }

    public void showMessagesPane() {
        // spOverall is the split pane that can contains the main view and the Messages Pane, the TerminalPane
        // spOverall is always contains the main view
        Node main = spOverall.getItems().get(0);
        if (spOverall.getItems().size() > 1) {
            dividerPosition = spOverall.getDividers().get(0).getPosition();
        }
        spOverall.getItems().clear();
        spOverall.getItems().add(main);
        // add the Messages Pane into the spOverall
        spOverall.getItems().add(apMessagesPane);
        spOverall.getDividers().get(0).setPosition(dividerPosition);
    }

    public void showTerminalPane() {
        // spOverall is the split pane that can contains the main view and the Messages Pane, the TerminalPane
        // spOverall is always contains the main view
        Node main = spOverall.getItems().get(0);
        if (spOverall.getItems().size() > 1) {
            dividerPosition = spOverall.getDividers().get(0).getPosition();
        }
        spOverall.getItems().clear();
        spOverall.getItems().add(main);
        // add the Messages Pane into the spOverall
        spOverall.getItems().add(apTerminalPane);
        spOverall.getDividers().get(0).setPosition(dividerPosition);
    }

    public Label getlRunningProcesses() {
        return lRunningProcesses;
    }

    private void displayCoverageType() {
        String type = Environment.getInstance().getTypeofCoverage();
        lCoverageType.setText(type);
        Tooltip tooltip = new Tooltip("Coverage type: " + type);
        hackTooltipStartTiming(tooltip);
        lCoverageType.setTooltip(tooltip);
    }

//    public void setupWorkingDirectory() {
//        String workingDirectory = new AkaConfig().fromJson().getWorkingDirectory();
//        logger.debug("The working directory stored in " + workingDirectory);
//
//        if (workingDirectory != null && new File(workingDirectory).exists()) {
//            logger.debug("The working directory exists");
//
//            Tooltip tooltip = new Tooltip(workingDirectory);
//            hackTooltipStartTiming(tooltip);
//            lblCurrentWorkspace.setTooltip(tooltip);
//
//            lblCurrentWorkspace.setText("Working directory: " + new File(workingDirectory).getName());
//            lblCurrentWorkspace.setTextFill(Color.web("black"));
//
//        }
//        else {
//            logger.debug("The working directory " + workingDirectory + " does not exist");
//            lblWorkingDirectory.setTooltip(null);
//            lblWorkingDirectory.setText("No working directory");
//            lblWorkingDirectory.setTextFill(Color.web("red"));
//
//            //
//            showInvalidWorkingDirectoryStatus();
//
//            // create a default working directory
//            new File(workingDirectory + File.separator).mkdir();
//            logger.debug("The working directory " + workingDirectory + " is created successfully");
//        }
//    }

    // control tooltip as need
    private static void hackTooltipStartTiming(Tooltip tooltip) {
        try {
            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
            fieldBehavior.setAccessible(true);
            Object objBehavior = fieldBehavior.get(tooltip);

            Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
            fieldTimer.setAccessible(true);
            Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

            objTimer.getKeyFrames().clear();
            objTimer.getKeyFrames().add(new KeyFrame(new Duration(100)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpLocalFolder() {
        File localFolder = AkaConfig.LOCAL_DIRECTORY;
        new AkaConfig().fromJson().setLocalDirectory(localFolder.getAbsolutePath()).exportToJson();
        if (localFolder.exists()) {
            try {
                logger.debug("The local file " + localFolder.getCanonicalPath() + " exists");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                logger.debug("The local file " + localFolder.getCanonicalPath() + " does not exist. Created one.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            localFolder.mkdirs();
            showInvalidLocalFolderStatus();
        }
    }

    private void setUpSettingFile() {
        File settingFile = AkaConfig.SETTING_PROPERTIES_PATH;
        if (settingFile.exists()) {
            try {
                logger.debug("The setting file " + settingFile.getCanonicalPath() + " exists");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                logger.debug("The setting file " + settingFile.getCanonicalPath() + " does not exist. Create one!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            showInvalidSettingFileStatus();

            new AkaConfig().fromJson().exportToJson();
        }
    }

    private void showInvalidLocalFolderStatus() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText("Local folder does not found!");
        alert.setContentText("The local folder does not exist! Create one automatically at " + new AkaConfig().fromJson().getLocalDirectory());
        alert.showAndWait();
    }

//    private void showInvalidWorkingDirectoryStatus() {
//        Alert alert = new Alert(Alert.AlertType.WARNING);
//        alert.setTitle("Warning Dialog");
//        alert.setHeaderText("Working directory does not found!");
//        alert.setContentText("The working directory does not exist! Create one automatically at " + new AkaConfig().fromJson().getWorkingDirectory());
//        alert.showAndWait();
//    }

    private void showInvalidSettingFileStatus() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText("Setting.properties does not found!");
        alert.setContentText("The setting.properties does not exist! Initialize one at " + AkaConfig.SETTING_PROPERTIES_PATH);
        alert.showAndWait();
    }

    public void clear() {
        MDIWindowController.getMDIWindowController().clear();
        LeftPaneController.getLeftPaneController().clear();
        tpMessagesTabPane.getTabs().clear();

        Node main = spOverall.getItems().get(0);
        spOverall.getItems().clear();
        spOverall.getItems().add(main);
        tpMessagesTabPane.getTabs().clear();

        // tab to display messages of aka tool
        akaMessagesTabController = MessagesPaneTabContentController.getInstance();
        if (akaMessagesTabController != null) {
            Tab akaMessagesTab = akaMessagesTabController.getTab();
            akaMessagesTab.setText("AKA_MESSAGES");
            tpMessagesTabPane.getTabs().add(akaMessagesTab);
        }
    }

    private Tab getMessagesTabByName(String name) {
        for (Tab tab : tpMessagesTabPane.getTabs()) {
            if (tab.getText().equals(name)) return tab;
        }
        return null;
    }

    public MessagesPaneTabContentController getMessagesTabControllerByName(String name) {
        return nameToMessagesTabControllerMap.get(name);
    }

    public void updateInformation() {
        displayCoverageType();
    }

    public Label getLblCurrentWorkspace() {
        return lblCurrentWorkspace;
    }

    public void setLblCurrentWorkspace(Label lblCurrentWorkspace) {
        this.lblCurrentWorkspace = lblCurrentWorkspace;
    }

    public MessagesPaneTabContentController getAkaMessagesTabController() {
        return akaMessagesTabController;
    }
}

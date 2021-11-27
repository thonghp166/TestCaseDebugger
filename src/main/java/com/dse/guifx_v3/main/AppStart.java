package com.dse.guifx_v3.main;

import com.dse.config.AkaConfig;
import com.dse.guifx_v3.controllers.main_view.BaseSceneController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.thread.AkaThreadManager;
import com.dse.util.AkaLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AppStart extends Application {

    @Override
    public void start(Stage primaryStage) {
        UIController.setPrimaryStage(primaryStage);
        Scene baseScene = BaseSceneController.getBaseScene();
        baseScene.getStylesheets().add("/css/treetable.css");
        primaryStage.setTitle("Aka Automation Tool");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/FXML/bamboo.png")));
        primaryStage.setScene(baseScene);

        // set size to maximized and not resizable
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();

        //
        new AkaConfig().fromJson().setOpenWorkspaceConfig("").setOpeningWorkspaceDirectory("").exportToJson();

        // close aka
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {
                        // Shut down all automated test data generation threads
                        AkaThreadManager.stopAutomatedTestdataGenerationForAll(Environment.getInstance().getProjectNode());
                        System.exit(0);
                    }
                });
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

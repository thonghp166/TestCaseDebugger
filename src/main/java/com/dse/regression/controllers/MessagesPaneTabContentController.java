package com.dse.regression.controllers;

import com.dse.util.MessagesPaneLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

public class MessagesPaneTabContentController {
    @FXML
    private TextArea textArea;
    @FXML
    private Tab tab;

    private MessagesPaneLogger logger;


    public static MessagesPaneTabContentController getInstance() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/regression_script/MessagesPaneTabContent.fxml"));
        try {
            loader.load();
            MessagesPaneTabContentController controller = loader.getController();
            MessagesPaneLogger logger = new MessagesPaneLogger(MessagesPaneTabContentController.class.getSimpleName(), controller.getTextArea());
            controller.setLogger(logger);

            return controller;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Tab getTab() {
        return tab;
    }

    public void setLogger(MessagesPaneLogger logger) {
        this.logger = logger;
    }

    public MessagesPaneLogger getLogger() {
        return logger;
    }

    public TextArea getTextArea() {
        return textArea;
    }
}

package com.dse.guifx_v3.controllers;

import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXML;


public class TestCaseTabController {
    @FXML
    private SplitPane splitPane;

    @FXML
    private AnchorPane testcaseTreeTable;

    @FXML
    private Tab parameterTree;

    @FXML
    private Tab controlFlow;

    @FXML
    private Tab options;

    @FXML
    private Tab testcaseUserCode;

    @FXML
    private Tab executionReport;

    public void setTestcaseTreeTable(AnchorPane tcTreeTable) {
        // to keep the divider as designed
        splitPane.getItems().add(0, tcTreeTable);
        splitPane.getItems().remove(1);
    }

}

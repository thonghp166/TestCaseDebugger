package com.dse.guifx_v3.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.dse.util.AkaLogger;

import java.net.URL;
import java.util.ResourceBundle;

public class CoverageViewTabController implements Initializable {

    private final static AkaLogger logger = AkaLogger.get(CoverageViewTabController.class);

    @FXML
    private Tab tCoverageTab1;

    @FXML
    private Tab tCoverageTab2;

    @FXML
    private ProgressBar pBCoverageProgress; // display percentage

    @FXML
    private ProgressBar pBCoverageProgressInTab2; // display percentage

    @FXML
    private ScrollPane spCoverage; // display the highlight source code

    @FXML
    private ScrollPane spCoverageInTab2; // display the highlight source code

    @FXML
    private Label lbPercentage;

    @FXML
    private Label lbPercentageInTab2;

    @FXML
    private Label lProgressDetail;

    @FXML
    private Label lProgressDetailInTab2;

    public void initialize(URL location, ResourceBundle resources) {
    }

    // TAB 1-----------------------------
    public void updateProgress(float progress) {
        pBCoverageProgress.setProgress(progress);
        float percentage = progress * 100;
        lbPercentage.setText(percentage + "%");
    }

    public void loadContentToCoverageViewInTab1(String nameTab, String content) {
        tCoverageTab1.setText(nameTab);

        final WebView coverage = new WebView();
        final WebEngine webEngine = coverage.getEngine();
        webEngine.loadContent(content);
        spCoverage.setContent(coverage);

        spCoverage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Double width = (Double) newValue;
                coverage.setPrefWidth(width);
            }
        });
        spCoverage.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Double height = (Double) newValue;
                coverage.setPrefHeight(height);
            }
        });
    }

    public void updateProgressDetail(String ratio) {
        lProgressDetail.setText(ratio);
    }

    // TAB 2-----------------------------
    public void updateProgressInTab2(float progress) {
        pBCoverageProgressInTab2.setProgress(progress);
        float percentage = progress * 100;
        lbPercentageInTab2.setText(percentage + "%");
    }

    public void loadContentToCoverageViewInTab2(String nameTab, String content) {
        tCoverageTab2.setText(nameTab);

        final WebView coverage = new WebView();
        final WebEngine webEngine = coverage.getEngine();
        webEngine.loadContent(content);
        spCoverageInTab2.setContent(coverage);

        spCoverageInTab2.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Double width = (Double) newValue;
                coverage.setPrefWidth(width);
            }
        });
        spCoverageInTab2.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Double height = (Double) newValue;
                coverage.setPrefHeight(height);
            }
        });
    }

    public void updateProgressDetailInTab2(String ratio) {
        lProgressDetailInTab2.setText(ratio);
    }

    public Label getLbPercentage() {
        return lbPercentage;
    }
}

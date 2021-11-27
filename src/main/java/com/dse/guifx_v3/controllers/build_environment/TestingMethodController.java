package com.dse.guifx_v3.controllers.build_environment;

import com.dse.guifx_v3.helps.UIController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class TestingMethodController extends AbstractCustomController implements Initializable {
    @FXML
    private ToggleGroup rmiTestingMethod;
    @FXML
    private Label lLinkOptionsObjectFileTesting;
    @FXML
    private TextField tfObjectFileTesting;
    @FXML
    private Button bLoadObjectFileTesting;
    @FXML
    private Label lLinkOptionsLibraryInterfaceTesting;
    @FXML
    private TextField tfLibraryInterfaceTesting;
    @FXML
    private Button bLoadLibraryInterfaceTesting;

    public void initialize(URL location, ResourceBundle resources) {
        setValid(true);
    }

    private void disableAll() {
        lLinkOptionsLibraryInterfaceTesting.setDisable(true);
        tfLibraryInterfaceTesting.setDisable(true);
        bLoadLibraryInterfaceTesting.setDisable(true);

        lLinkOptionsObjectFileTesting.setDisable(true);
        tfObjectFileTesting.setDisable(true);
        bLoadObjectFileTesting.setDisable(true);
    }

    @FXML
    public void chooseTraditionalUnitTesting() {
        disableAll();
    }

    @FXML
    public void chooseObjectFileTesting() {
        disableAll();
        lLinkOptionsObjectFileTesting.setDisable(false);
        tfObjectFileTesting.setDisable(false);
        bLoadObjectFileTesting.setDisable(false);
    }

    @FXML
    public void chooseLibraryInterfaceTesting() {
        disableAll();
        lLinkOptionsLibraryInterfaceTesting.setDisable(false);
        tfLibraryInterfaceTesting.setDisable(false);
        bLoadLibraryInterfaceTesting.setDisable(false);
    }

    @FXML
    public void chooseTestDrivenDevelopment() {
        disableAll();
    }

    @FXML
    public void loadLinkOptionsForObjectFileTesting() {
        FileChooser fileChooser = new FileChooser();
        setWorkingDirectory(fileChooser);

        // load
        Stage envBuilderStage = UIController.getEnvironmentBuilderStage();
        File linkOptions = fileChooser.showOpenDialog(envBuilderStage);
        if (linkOptions != null) {
            tfObjectFileTesting.setText(linkOptions.getAbsolutePath());
        } else {
            System.out.println("Error when load link options");
        }
    }

    @FXML
    public void loadLinkOptionsForLibraryInterfaceTesting() {
        FileChooser fileChooser = new FileChooser();
        setWorkingDirectory(fileChooser);

        // load
        Stage envBuilderStage = UIController.getEnvironmentBuilderStage();
        File linkOptions = fileChooser.showOpenDialog(envBuilderStage);
        if (linkOptions != null) {
            tfLibraryInterfaceTesting.setText(linkOptions.getAbsolutePath());
        } else {
            System.out.println("Error when load link options");
        }
    }

    @Override
    public void validate() {
        // nothing to do
    }

    @Override
    public void save() {
        // nothing to do
    }

    @Override
    public void loadFromEnvironment() {
        // nothing to do
    }
}

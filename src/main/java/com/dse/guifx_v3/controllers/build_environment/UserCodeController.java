package com.dse.guifx_v3.controllers.build_environment;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class UserCodeController extends AbstractCustomController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setValid(true);
    }

    @Override
    public void validate() {
        // nothing to do
    }

    public void save() {
        updateSummary();
    }

    @Override
    public void loadFromEnvironment() {
        // nothing to do
    }

    private void updateSummary() {
        BaseController.getBaseController().updateSummary();
    }
}

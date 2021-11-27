package com.dse.guifx_v3.controllers.build_environment;

import com.dse.config.AkaConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.EnviroNameNode;
import com.dse.environment.object.EnvironmentRootNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.util.Utils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import com.dse.util.AkaLogger;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class NameEnvironmentController extends AbstractCustomController implements Initializable {
    final static AkaLogger logger = AkaLogger.get(NameEnvironmentController.class);
    @FXML
    private TextField tfEnvironmentName;

    // use when updating environment
    private String ignoredEnvName;

    public void initialize(URL location, ResourceBundle resources) {
        // nothing to do
    }

    @Override
    public void loadFromEnvironment() {
        tfEnvironmentName.setDisable(true);

        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroNameNode());

        if (nodes.size() == 1) {
            String name = ((EnviroNameNode) nodes.get(0)).getName();
            tfEnvironmentName.setText(name);
            ignoredEnvName = name;
            validate();

            logger.debug("Load Environment's name from current environment");
        } else if (nodes.size() == 0) {
            logger.error("Invalid Environment. There are no EnviroNameNode");
        } else {
            logger.debug("There are more than one name options!");
        }
    }

    @Override
    public void save() {
        String newEnvName = tfEnvironmentName.getText().trim();

        if (!isValid()) {
            UIController.showErrorDialog("The new name of the environment " + newEnvName + " is invalid"
                    , "Save", "Could not save the new name of the environment");
            return;
        }

        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroNameNode());

        if (nodes.size() >= 2) {
            logger.error("Error when saving the name of environment");
            UIController.showErrorDialog("Could not save the configuration of this window"
                    , "Save", "Could not save");
            return;
        }

        if (nodes.size() == 1) {
            ((EnviroNameNode) nodes.get(0)).setName(newEnvName);
            logger.debug("Environment configuration:\n" + Environment.getInstance().getEnvironmentRootNode().exportToFile());

        } else if (nodes.size() == 0) {
            EnviroNameNode nameNode = new EnviroNameNode();
            nameNode.setName(newEnvName);
            root.addChild(nameNode);
            logger.debug("Environment configuration:\n" + Environment.getInstance().getEnvironmentRootNode().exportToFile());
        }
        validate();
    }

    private boolean validateTFEnvironmentName() {
        String name = tfEnvironmentName.getText().trim();
        // check if empty
        if (name.equals("")) {
            tfEnvironmentName.setStyle("-fx-border-color: red");
            return false;
        }
        // check if the name starts with number
        if (Character.isDigit(name.charAt(0))) {
            tfEnvironmentName.setStyle("-fx-border-color: red");
            return false;
        }

        // check if the name existed
        File wd = new File(new AkaConfig().fromJson().getWorkingDirectory());
        if (wd.exists() && wd.isDirectory()) {
            for (File file : Objects.requireNonNull(wd.listFiles())) {
                if (file.getName().equals(ignoredEnvName)
                        || file.getName().equals(ignoredEnvName + ".env"))
                    // use when updating environment
                    continue;
                else if (file.getName().equals(name + ".env")) {
                    tfEnvironmentName.setStyle("-fx-border-color: red");
                    return false;
                }
                }
            }

        tfEnvironmentName.setStyle("");
        return true;
    }

    @FXML
    public void validate() {
        if (!validateTFEnvironmentName()) {
            setValid(false);
        } else {
            setValid(true);
        }
        highlightInvalidStep();
    }

}

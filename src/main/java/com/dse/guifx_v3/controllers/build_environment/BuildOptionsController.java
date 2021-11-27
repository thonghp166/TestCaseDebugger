package com.dse.guifx_v3.controllers.build_environment;

import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.environment.object.EnviroWhiteBoxNode;
import com.dse.environment.object.EnvironmentRootNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.dse.util.AkaLogger;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class BuildOptionsController extends AbstractCustomController implements Initializable {
    final static AkaLogger logger = AkaLogger.get(BuildOptionsController.class);
    @FXML
    private CheckBox chbWhitebox;
    @FXML
    private ComboBox<String> cbCoverageType;
//    @FXML
//    private Button bLoad;
//    @FXML
//    private CheckBox chbUseBuildSettingsFromRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbCoverageType.getItems().addAll(
                EnviroCoverageTypeNode.BASIS_PATH,
                EnviroCoverageTypeNode.BRANCH,
                EnviroCoverageTypeNode.MCDC,
                EnviroCoverageTypeNode.STATEMENT,
                EnviroCoverageTypeNode.STATEMENT_AND_BRANCH,
                EnviroCoverageTypeNode.STATEMENT_AND_MCDC);
        cbCoverageType.setValue(EnviroCoverageTypeNode.STATEMENT);

//        setValid(true);
    }

    @Override
    public void validate() {
        // nothing to do
    }

    @Override
    public void save() {
        saveWhitebox(chbWhitebox.isSelected());
        saveCoverageType(cbCoverageType.getValue());
    }

    private void saveWhitebox(boolean whiteboxEnabled) {
        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroWhiteBoxNode());

        if (nodes.size() == 1) {
            ((EnviroWhiteBoxNode) nodes.get(0)).setActive(whiteboxEnabled);
            logger.debug("Environment configuration:\n" + root.exportToFile());
        } else if (nodes.size() == 0) {
            EnviroWhiteBoxNode node = new EnviroWhiteBoxNode();
            node.setActive(whiteboxEnabled);
            root.addChild(node);
            logger.debug("Environment configuration:\n" + Environment.getInstance().getEnvironmentRootNode().exportToFile());
        } else {
            logger.error("Error");
        }
    }

    private void saveCoverageType(String coverageType) {
        if (coverageType != null) {
            EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
            List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroCoverageTypeNode());

            if (nodes.size() == 1) {
                ((EnviroCoverageTypeNode) nodes.get(0)).setCoverageType(coverageType);

            } else if (nodes.size() == 0) {
                EnviroCoverageTypeNode node = new EnviroCoverageTypeNode();
                node.setCoverageType(coverageType);
                root.addChild(node);

            } else {
                logger.error("Invalid environment configuration. Found more than one coverage type node: " + nodes);
            }
        } else {
            logger.error("Coverage type is not set up");
        }
        logger.debug("Environment configuration:\n" + Environment.getInstance().getEnvironmentRootNode().exportToFile());
    }

    @Override
    public void loadFromEnvironment() {
        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        // load coverage type
        List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroCoverageTypeNode());
        if (nodes.size() == 1) {
            String type = ((EnviroCoverageTypeNode) nodes.get(0)).getCoverageType();
            // TODO: valid the type
            cbCoverageType.setValue(type);
        } else if (nodes.size() == 0) {
            cbCoverageType.setValue(EnviroCoverageTypeNode.STATEMENT);
        } else {
            logger.error("Invalid environment configuration. Found more than one coverage type node: " + nodes);
        }

        // load check box white-box
        nodes = EnvironmentSearch.searchNode(root, new EnviroWhiteBoxNode());
        if (nodes.size() == 1) {
            boolean selected = ((EnviroWhiteBoxNode) nodes.get(0)).isActive();
            chbWhitebox.setSelected(selected);
        } else if (nodes.size() == 0) {
            chbWhitebox.setSelected(false);
        } else {
            logger.error("Invalid environment configuration. Found more than white box node: " + nodes);
        }
    }

//    @FXML
//    public void loadFromRepository() {
//        FileChooser fileChooser = new FileChooser();
//        setWorkingDirectory(fileChooser);
//
//        // load
//        Stage envBuilderStage = UIController.getEnvironmentBuilderStage();
//        File repositoy = fileChooser.showOpenDialog(envBuilderStage);
//        if (repositoy != null) {
//            EnvironmentAnalyzer analyzer = new EnvironmentAnalyzer();
//            analyzer.analyze(repositoy);
//            IEnvironmentNode root = analyzer.getRoot();
//            if (root instanceof EnvironmentRootNode) {
//                // load the coverage type
//                List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroCoverageTypeNode());
//                if (nodes.size() > 1) {
//                    logger.error("Error");
//                    return;
//                }
//                if (nodes.size() == 1) {
//                    EnviroCoverageTypeNode typeNode = (EnviroCoverageTypeNode) nodes.get(0);
//                    // todo: volidate the type
//                    cbCoverageType.setValue(typeNode.getCoverageType());
//                }
//
//                // load the check box white box
//                nodes = EnvironmentSearch.searchNode(root, new EnviroWhiteBoxNode());
//                if (nodes.size() > 1) {
//                    logger.error("Error");
//                    return;
//                }
//                if (nodes.size() == 1) {
//                    EnviroWhiteBoxNode whiteBoxNode = (EnviroWhiteBoxNode) nodes.get(0);
//                    // todo: volidate the type
//                    chbWhitebox.setSelected(whiteBoxNode.isActive());
//                }
//            } else {
//                logger.error("Error");
//            }
//        }
//    }
//
//    public void useBuildSettingsFromRepository() {
//        if (chbUseBuildSettingsFromRepository.isSelected()) {
//            bLoad.setDisable(false);
//        } else {
//            bLoad.setDisable(true);
//        }
//
//    }

}

package com.dse.guifx_v3.controllers.build_environment;

import com.dse.environment.EnvironmentSearch;
import com.dse.environment.object.*;
import com.dse.guifx_v3.controllers.object.build_environment.LocateSourceFilesCellFactory;
import com.dse.guifx_v3.controllers.object.build_environment.SourcePath;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.Factory;
import com.dse.guifx_v3.helps.UIController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LocateSourceFilesController extends AbstractCustomController implements Initializable {
    @FXML
    private ListView<SourcePath> lvPaths; // contain all added source code files
    @FXML
    private CheckBox chbUseRelativePath;
    @FXML
    private Button bAdd;
    @FXML
    private Button bDelete;
    @FXML
    private Button bAddRecursive;

    private List<String> absolutePaths = new ArrayList<>();

    public void initialize(URL location, ResourceBundle resources) {
        lvPaths.setCellFactory(new LocateSourceFilesCellFactory());
        bAdd.setText(null);
        Image add = new Image(Factory.class.getResourceAsStream("/icons/add.png"));
        bAdd.setGraphic(new ImageView(add));
        bDelete.setText(null);
        Image delete = new Image(Factory.class.getResourceAsStream("/icons/delete.png"));
        bDelete.setGraphic(new ImageView(delete));
        bAddRecursive.setText(null);
        Image addRecursive = new Image(Factory.class.getResourceAsStream("/icons/addRecursive.png"));
        bAddRecursive.setGraphic(new ImageView(addRecursive));
    }

    @FXML
    public void addTheRootFolder() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        Stage envBuilderStage = UIController.getEnvironmentBuilderStage();
        File file = directoryChooser.showDialog(envBuilderStage);
        if (file != null){
            addToListViewPaths(file.getAbsolutePath());
        }

        validate();
    }

    @FXML
    public void addRecursiveFolders() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        Stage envBuilderStage = UIController.getEnvironmentBuilderStage();
        File file = directoryChooser.showDialog(envBuilderStage);
        addRecursive(file);
        validate();
    }

    private void addToListViewPaths(String absolutePath) {
        if (!absolutePaths.contains(absolutePath)) {
            if (!absolutePaths.contains(absolutePath)) {
                absolutePaths.add(absolutePath);
                SourcePath item = new SourcePath(absolutePath);
                item.setType(SourcePath.SEARCH_DIRECTORY); // by default
                lvPaths.getItems().add(item);
            }
        }
    }

    private void addRecursive(File parentDirectory) {
        if (parentDirectory != null){
            addToListViewPaths(parentDirectory.getAbsolutePath());
            for (File file : parentDirectory.listFiles(File::isDirectory)) {
                addRecursive(file);
            }
        }
    }

    public void delete() {
        int itemId = lvPaths.getSelectionModel().getSelectedIndex();
        if (itemId >= 0) {
            absolutePaths.remove(lvPaths.getSelectionModel().getSelectedItem().getAbsolutePath());
            lvPaths.getItems().remove(itemId);
        }

        validateLVPaths();
    }

    @Override
    public void save() {
        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> children = Environment.getInstance().getEnvironmentRootNode().getChildren();

        // Remove all the existing nodes in the current environment
        List<IEnvironmentNode> searchListNodes = EnvironmentSearch.searchNode(root, new EnviroSearchListNode());
        children.removeAll(searchListNodes);

        List<IEnvironmentNode> enviroTypeHandledSourceNodes = EnvironmentSearch.searchNode(root, new EnviroTypeHandledSourceDirNode());
        children.removeAll(enviroTypeHandledSourceNodes);

        List<IEnvironmentNode> enviroLibraryIncludeDirNodes = EnvironmentSearch.searchNode(root, new EnviroLibraryIncludeDirNode());
        children.removeAll(enviroLibraryIncludeDirNodes);

        // Get all new search directories
        List<IEnvironmentNode> newSearchListNodes = new ArrayList<>();
        for (SourcePath item : lvPaths.getItems()) {
            switch (item.getType()) {
                case SourcePath.SEARCH_DIRECTORY: {
                    EnviroSearchListNode node = new EnviroSearchListNode();
                    node.setSearchList(item.getAbsolutePath());
                    Environment.getInstance().getEnvironmentRootNode().addChild(node);

                    newSearchListNodes.add(node);
                    break;
                }
                case SourcePath.TYPE_HANDLED_DIRECTORY: {
                    EnviroTypeHandledSourceDirNode node = new EnviroTypeHandledSourceDirNode();
                    node.setTypeHandledSourceDir(item.getAbsolutePath());
                    Environment.getInstance().getEnvironmentRootNode().addChild(node);
                    break;
                }
                case SourcePath.LIBRARY_INCLUDE_DIRECTORY: {
                    EnviroLibraryIncludeDirNode node = new EnviroLibraryIncludeDirNode();
                    node.setLibraryIncludeDir(item.getAbsolutePath());
                    Environment.getInstance().getEnvironmentRootNode().addChild(node);
                    break;
                }
            }
        }

        // Save the state of modifying search directories
        if (newSearchListNodes.containsAll(searchListNodes) && searchListNodes.containsAll(newSearchListNodes))
            Environment.WindowState.isSearchListNodeUpdated(false); // to avoid analyzing search directories again
        else
            Environment.WindowState.isSearchListNodeUpdated(true);

        logger.debug("Environment script:\n" + Environment.getInstance().getEnvironmentRootNode().exportToFile());

        logger.debug("Updating the next window (Choose UUTs)");
        BaseController.getBaseController().updateChooseUUT();
        logger.debug("Update done!");

        validate();
    }

    @Override
    public void loadFromEnvironment() {
        lvPaths.getItems().clear();

        // get search nodes and add to screen
        EnvironmentRootNode root = Environment.getInstance().getEnvironmentRootNode();
        List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(root, new EnviroSearchListNode());
        for (IEnvironmentNode node : nodes) {
            String path = ((EnviroSearchListNode) node).getSearchList();
            SourcePath sourcePath = new SourcePath(path);
            sourcePath.setType(SourcePath.SEARCH_DIRECTORY);
            lvPaths.getItems().add(sourcePath);
        }

        // get type handled nodes and add to screen
        nodes = EnvironmentSearch.searchNode(root, new EnviroTypeHandledSourceDirNode());
        for (IEnvironmentNode node : nodes) {
            String path = ((EnviroTypeHandledSourceDirNode) node).getTypeHandledSourceDir();
            SourcePath sourcePath = new SourcePath(path);
            sourcePath.setType(SourcePath.TYPE_HANDLED_DIRECTORY);
            lvPaths.getItems().add(sourcePath);
        }

        // get library include nodes and add to screen
        nodes = EnvironmentSearch.searchNode(root, new EnviroLibraryIncludeDirNode());
        for (IEnvironmentNode node : nodes) {
            String path = ((EnviroLibraryIncludeDirNode) node).getLibraryIncludeDir();
            SourcePath sourcePath = new SourcePath(path);
            sourcePath.setType(SourcePath.LIBRARY_INCLUDE_DIRECTORY);
            lvPaths.getItems().add(sourcePath);
        }

        // check the correctness of these above nodes
        validate();
    }

    @FXML
    public void useRelativePath() {
        if (chbUseRelativePath.isSelected()) {
            new SourcePath().setUseRelativePath(true);
        } else {
            new SourcePath().setUseRelativePath(false);
        }
        lvPaths.refresh();
    }

    private boolean validateLVPaths() {
        if (lvPaths.getItems().size() == 0) return false;
        for (SourcePath path : lvPaths.getItems()) {
            if (!path.isExisted()) {
                return false;
            }
        }
        return true;
    }

    public void validate() {
        if (validateLVPaths()) {
            setValid(true);
        } else {
            setValid(false);
        }

        // highlight the label of this dialog if we found any error
        highlightInvalidStep();
    }
}

package com.dse.compiler.gui;

import com.dse.compiler.Compiler;
import com.dse.compiler.ICompiler;
import com.dse.compiler.message.CompileMessage;
import com.dse.compiler.message.ICompileMessage;
import com.dse.compiler.message.error_tree.CompileErrorSearch;
import com.dse.compiler.message.error_tree.CompileMessageParser;
import com.dse.compiler.message.error_tree.node.*;
import com.dse.config.AkaConfig;
import com.dse.config.CommandConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.controllers.build_environment.BaseController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.IncludeHeaderDependency;
import com.dse.parser.object.*;
import com.dse.project_init.ProjectClone;
import com.dse.resolver.*;
import com.dse.search.Search;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.util.AkaLogger;
import com.dse.util.CompilerUtils;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SrcResolverController implements Initializable {
    private final static AkaLogger logger = AkaLogger.get(SrcResolverController.class);
    private String directory;
    private List<INode> sourceFiles;
    private AtomicInteger startIdx;
    private ICompiler compiler;
    private Stage stage;
    private Map<String, String> compilationCommands = new HashMap<>();
    private String linkingCommand = "";

    private boolean isSuggestSolution = true;

    @FXML
    private Text txtFilePath;
    @FXML
    private TextArea txtCompileMessage;
    @FXML
    private ListView<IErrorNode> lvMissing;
    @FXML
    public ListView<ResolvedSolution> lvResolved;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtCompileMessage.setWrapText(true);
        txtCompileMessage.setEditable(false);

        lvMissing.setOnMouseClicked(event -> {
            IErrorNode errorNode = lvMissing.getSelectionModel().getSelectedItem();
            displayResolveMessage(errorNode);
        });

        lvResolved.setCellFactory(lv -> new ResolvedSolutionCell());
    }

    public class ResolvedSolutionCell extends ListCell<ResolvedSolution> {
        private final ContextMenu contextMenu = new ContextMenu();

        private void setupContextMenu() {
            MenuItem addToSourceItem = new MenuItem("Add to source");

            addToSourceItem.setOnAction(event -> {
                ResolvedSolution solution = getItem();
                useSolution(solution);
            });

            contextMenu.getItems().clear();
            contextMenu.getItems().add(addToSourceItem);

//            emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
//                if (isNowEmpty) {
//                    setContextMenu(null);
//                } else {
                    setContextMenu(contextMenu);
//                }
//            });
        }

        private void useSolution(ResolvedSolution solution) {
            SolutionManager.getInstance().use(solution);

            // re-parse
            BaseController.addAnalysisInformationToWorkspace(
                    new AkaConfig().fromJson().getOpeningWorkspaceDirectory(),
                    new WorkspaceConfig().fromJson().getDependencyDirectory(),
                    new WorkspaceConfig().fromJson().getPhysicalJsonFile(),
                    new WorkspaceConfig().fromJson().getElementDirectory());

            // re-compile
            ICompileMessage message = findErrorSourcecodeFile(compiler, sourceFiles, startIdx);

            if (message.getType() != ICompileMessage.MessageType.ERROR) {
                getStage().close();
                createSucessAlertWhenBuilding();
                exportInformationExternally();
            }

//            if (startIdx.get() >= sourceFiles.size()) {
//                // all source code files are compiled. Now we need to create executable files
//                logger.debug("Linking source code file to create an executable file");
//                boolean linkageSuccesses = linkSourceFiles();
//
//                if (linkageSuccesses) {
//                    getStage().close();
//                    createSucessAlertWhenBuilding();
//                    exportInformationExternally();
//                } else {
//                    createFailureAlertWhenBuilding(new CompileMessage("Can not create executable file", ""));
//
//                    // reset the index. Users may modify the source code file and build again.
//                    startIdx.set(0);
//                }
//            }
        }

        @Override
        protected void updateItem(ResolvedSolution item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                ResolvedSolution solution = getItem();

                if (solution != null) {
                    setText(solution.getResolvedSourceCode());
                    setupContextMenu();
                }
            }
        }
    }

    public int findProblemInTestedProject(Node projectRootNode) {
        if (projectRootNode != null) {
            AtomicInteger startIdx = new AtomicInteger(0);
            setStartIdx(startIdx);

            List<INode> sourceFiles = findAllSourceCodeFiles(projectRootNode);
            setSourceFiles(sourceFiles);

            ICompiler compiler = updateCompiler();
            setCompiler(compiler);

            if (sourceFiles.isEmpty())
                return BaseController.BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION;

            ICompileMessage message = findErrorSourcecodeFile(compiler, sourceFiles, startIdx);
            if (message.getType() == ICompileMessage.MessageType.ERROR)
                return BaseController.BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION;
            else return BaseController.BUILD_NEW_ENVIRONMENT.SUCCESS.COMPILATION;
        }
        else
            return BaseController.BUILD_NEW_ENVIRONMENT.FAILURE.COMPILATION;
    }

    private ICompiler updateCompiler() {
        // set compiler
        Compiler compiler = Environment.getInstance().getCompiler();

//        EnvironmentRootNode environmentRootNode = Environment.getInstance().getEnvironmentRootNode();
//
//        List<IEnvironmentNode> typeHandles = EnvironmentSearch
//                .searchNode(environmentRootNode, new EnviroTypeHandledSourceDirNode());
//        List<IEnvironmentNode> libraries = EnvironmentSearch
//                .searchNode(environmentRootNode, new EnviroLibraryIncludeDirNode());
//
//        for (IEnvironmentNode include : libraries) {
//            String path = ((EnviroLibraryIncludeDirNode) include).getLibraryIncludeDir();
//            compiler.getIncludePaths().add(path);
//        }
//        for (IEnvironmentNode include : typeHandles) {
//            String path = ((EnviroTypeHandledSourceDirNode) include).getTypeHandledSourceDir();
//            compiler.getIncludePaths().add(path);
//        }

        return compiler;
    }

    private List<INode> findAllSourceCodeFiles(INode projectRootNode) {
        // search for source code file nodes in source code file lists and compile them
//        List<INode> sourceFiles = Search.searchNodes(projectRootNode, new CppFileNodeCondition());
//        sourceFiles.addAll(Search.searchNodes(projectRootNode, new CFileNodeCondition()));
        List<INode> sourceFiles = Search.searchNodes(projectRootNode, new SourcecodeFileNodeCondition());
        List<INode> ignores = Environment.getInstance().getIgnores();
        List<INode> uuts = Environment.getInstance().getUUTs();
        List<INode> sbfs = Environment.getInstance().getSBFs();
        List<String> libraries = ProjectClone.getLibraries();
        sourceFiles.removeIf(f -> ignores.contains(f) || libraries.contains(f.getAbsolutePath())
                || (f instanceof HeaderNode && (!uuts.contains(f) && !sbfs.contains(f))));
        sourceFiles.forEach(sourceFile -> {
            try {
                IASTTranslationUnit iastNode = new SourcecodeFileParser().getIASTTranslationUnit(Utils.readFileContent(
                        sourceFile.getAbsolutePath()).toCharArray());
                if (sourceFile instanceof SourcecodeFileNode) {
                    ((SourcecodeFileNode) sourceFile).setAST(iastNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return sourceFiles;
    }



    private boolean isIncludedFile(INode node) {
        for (Dependency d : node.getDependencies()) {
            if (d instanceof IncludeHeaderDependency) {
                if (d.getEndArrow() == node)
                    return true;
            }
        }

        return false;
    }

    @FXML
    private void onBtnIgnoreAllClick() {
        if (compiler != null && sourceFiles != null && startIdx.get() >= 0) {

            if (startIdx.get() < sourceFiles.size() - 1) {
                // move to the next source code file
                startIdx.set(startIdx.get() + 1);

                // compile all source code files and do not care whether they has problems or not
                while (startIdx.get() < sourceFiles.size()) {
                    findErrorSourcecodeFile(compiler, sourceFiles, startIdx);
                    startIdx.set(startIdx.get() + 1);
                }
            }

            logger.debug("Linking source code file to create an executable file");
            boolean linkageSuccessed = linkSourceFiles();
            if (linkageSuccessed) {
                getStage().close();
                createSucessAlertWhenBuilding();
                exportInformationExternally();
            } else {
                createFailureAlertWhenBuilding(new CompileMessage("Can not create executable file from object files", ""));

                // reset the index. Users may modify the source code file and build again.
                startIdx.set(0);
            }
        }
    }

    private String[] getAllOutfilePaths(List<INode> nodes) {
        String[] filePaths = nodes.stream()
                .filter(file -> !isIncludedFile(file))
                .map(f -> CompilerUtils.getOutfilePath(f.getAbsolutePath(), compiler.getOutputExtension()))
                .toArray(String[]::new);

        return filePaths;
    }

    @FXML
    private void onBtnIgnoreClick() {
        if (startIdx.get() >= 0 && startIdx.get() < sourceFiles.size() - 1) {
            // move to the next source code file
            startIdx.set(startIdx.get() + 1);

            // just compile the current source code file and do not do anything
            findErrorSourcecodeFile(compiler, sourceFiles, startIdx);
        }

        if (startIdx.get() >= sourceFiles.size()) {
            // all source code files are compiled. Now we need to create executable files
            logger.debug("Linking source code file to create an executable file");
            boolean linkageSuccessed = linkSourceFiles();

            if (linkageSuccessed) {
                getStage().close();
                createSucessAlertWhenBuilding();
                exportInformationExternally();
            } else {
                createFailureAlertWhenBuilding(new CompileMessage("Can not create executable file", ""));

                // reset the index. Users may modify the source code file and build again.
                startIdx.set(0);
            }
        }
    }

    private void exportInformationExternally() {
        // TODO:
    }
    @FXML
    private void onBtnAbortClick() {
        getStage().close();
    }

    @FXML
    private void onBtnHelpClick() {
        //TODO: open help window
    }

    @FXML
    private void onBtnOpenFileClick() {
        if (sourceFiles != null) {
            INode currentFile = sourceFiles.get(startIdx.get());
            try {
                UIController.openTheLocation(currentFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean linkSourceFiles() {
        String executablePath = directory + (directory.endsWith(File.separator) ? "" : File.separator) + "program.exe";
        String[] filePaths = getAllOutfilePaths(sourceFiles);


        if (compiler != null) {
            ICompileMessage linkeMessage = compiler.link(executablePath, filePaths);

            if (linkeMessage == null)
                linkeMessage = new CompileMessage("Can not create executable file", "");

                logger.debug("Linking command: " + linkeMessage.getLinkingCommand());
                new CommandConfig().fromJson().setLinkingCommand(linkeMessage.getLinkingCommand()).exportToJson();

            return (linkeMessage.getType() == ICompileMessage.MessageType.EMPTY);
        } else{
            logger.error("Can not linkage because the directory is not set up");
            return false;
        }
    }

    private Alert createSucessAlertWhenBuilding() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Build Environment");

        alert.setHeaderText(null);
        alert.setContentText("Build environment successfully");
        alert.showAndWait();
        return alert;
    }

    private Alert createFailureAlertWhenBuilding(ICompileMessage message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Build Environment");

        alert.setHeaderText("Can not build environment");

        if (message.getMessage() != null && message.getMessage().length() > 0) {
            TextArea textArea = new TextArea();
            textArea.setText(message.getMessage());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            alert.getDialogPane().setContent(textArea);
        }

        alert.showAndWait();
        return alert;
    }

    private ICompileMessage findErrorSourcecodeFile(ICompiler compiler, List<INode> sourceFiles, AtomicInteger startIdx) {
        ICompileMessage message = null;

        if (startIdx.get() >= 0 && startIdx.get() < sourceFiles.size()) {
            do {
                INode fileNode = sourceFiles.get(startIdx.get());
                String filePath = fileNode.getAbsolutePath();
                logger.debug("Compiling " + filePath);
                message = compiler.compile(fileNode);

                // save the compilation commands to file
                String relativePath = PathUtils.toRelative(filePath);
                if (!compilationCommands.containsKey(relativePath)) {
                    compilationCommands.put(relativePath, message.getCompilationCommand());
                    new CommandConfig().fromJson().setCompilationCommands(compilationCommands).exportToJson();
                }

                //
                if (message.getType() == ICompileMessage.MessageType.ERROR) {
                    displayProblemOnScreen(filePath, message);
//                    if (isSuggestSolution) {
////                        lvMissing.getItems().clear();
//
//                        List<ICommonFunctionNode> resolved = new MissingFunctionFinder(sourceFiles.get(startIdx.get()))
//                                .getMissingFuntion();
//                        displayResolveMessage(resolved);
//                    }
                    break;
                } else {
                    // just warning or compile successfully
                    startIdx.set(startIdx.get() + 1);
                }
            } while (startIdx.get() < sourceFiles.size());
        }

        logger.debug("Linking source code file to create an executable file");
        boolean linkageSuccessed = linkSourceFiles();
        if (linkageSuccessed) {
            getStage().close();
            createSucessAlertWhenBuilding();
            exportInformationExternally();
        } else {
            //createFailureAlertWhenBuilding(new CompileMessage("Can not create executable file", ""));
        }
        return message;
    }

    private List<IErrorNode> getAllUndeclaredErrors(ICompileMessage compileMessage) {
        CompileMessageParser parser = new CompileMessageParser(compileMessage);

        RootErrorNode rootErrorNode = parser.parse();

        return CompileErrorSearch.searchNodes(rootErrorNode, UndeclaredErrorNode.class);
    }

    private void displayProblemOnScreen(String title, ICompileMessage message) {
        txtFilePath.setText(title);
        txtCompileMessage.setText(message.getMessage());

        List<IErrorNode> undeclaredErrors = getAllUndeclaredErrors(message);

        lvMissing.getItems().clear();
        lvMissing.getItems().addAll(undeclaredErrors);

        lvResolved.getItems().clear();
        lvResolved.refresh();
    }

    private void displayResolveMessage(IErrorNode errorNode) {
        if (errorNode instanceof UndeclaredErrorNode) {
            IUndeclaredResolver resolver = null;

            if (errorNode instanceof IUndeclaredFunctionErrorNode) {
                resolver = new UndeclaredFunctionResolver((IUndeclaredFunctionErrorNode) errorNode);
            } else if (errorNode instanceof IUndeclaredVariableErrorNode) {
                resolver = new UndeclaredVariableResolver((IUndeclaredVariableErrorNode) errorNode);
            } else if (errorNode instanceof IUndeclaredTypeErrorNode) {
                resolver = new UndeclaredTypeResolver((IUndeclaredTypeErrorNode) errorNode);
            }

            if (resolver != null) {
                try {
                    resolver.resolve();
                    List<ResolvedSolution> solutions = resolver.getSolutions();
                    lvResolved.getItems().clear();
                    lvResolved.getItems().addAll(solutions);
                } catch (Exception ex) {
                    lvResolved.getItems().clear();
                    logger.debug(ex);
                } finally {
                    lvResolved.refresh();

                }
            }
        }

//        for (ICommonFunctionNode functionNode : resolved) {
//            lvMissing.getItems().add(functionNode);
//        }
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    private void setSourceFiles(List<INode> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    private List<INode> getSourceFiles() {
        return sourceFiles;
    }

    public void setCompiler(ICompiler compiler) {
        this.compiler = compiler;
    }

    public ICompiler getCompiler() {
        return compiler;
    }

    private AtomicInteger getStartIdx() {
        return startIdx;
    }

    private void setStartIdx(AtomicInteger startIdx) {
        this.startIdx = startIdx;
    }

    public Map<String, String> getCompilationCommands() {
        return compilationCommands;
    }

    public void setCompilationCommands(Map<String, String> compilationCommands) {
        this.compilationCommands = compilationCommands;
    }
}

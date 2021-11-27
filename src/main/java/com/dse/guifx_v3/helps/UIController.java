package com.dse.guifx_v3.helps;

import auto_testcase_generation.testdata.object.SE;
import com.dse.code_viewer_gui.controllers.FXFileView;
import com.dse.compiler.message.ICompileMessage;
import com.dse.config.IProjectType;
import com.dse.config.Paths;
import com.dse.exception.OpenFileException;
import com.dse.guifx_v3.controllers.CompoundTestCaseTreeTableViewController;
import com.dse.guifx_v3.controllers.build_environment.BaseController;
import com.dse.guifx_v3.controllers.main_view.BaseSceneController;
import com.dse.guifx_v3.controllers.main_view.LeftPaneController;
import com.dse.guifx_v3.controllers.main_view.MDIWindowController;
import com.dse.guifx_v3.objects.AbstractTableCell;
import com.dse.guifx_v3.objects.ParameterColumnCellFactory;
import com.dse.guifx_v3.objects.UserCodeDialog;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.DefinitionFunctionNodeCondition;
import com.dse.search.condition.MacroFunctionNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_manager.*;
import com.dse.testcasescript.object.ITestcaseNode;
import com.dse.testcasescript.object.TestUnitNode;
import com.dse.testdata.object.MacroSubprogramDataNode;
import com.dse.testdata.object.TemplateSubprogramDataNode;
import com.dse.testdata.object.UnitNode;
import com.dse.testdata.object.UnresolvedDataNode;
import com.dse.util.CompilerUtils;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIController {
//    final static AkaLogger logger = AkaLogger.get(UIController.class);
    private static Stage primaryStage = null;
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static Stage environmentBuilderStage = null;
    public static void setEnvironmentBuilderStage(Stage stage) {
        environmentBuilderStage = stage;
    }
    public static Stage getEnvironmentBuilderStage() {
        return environmentBuilderStage;
    }

    /**
     * Create clone project
     *
     * @return
     * @throws IOException
     */

    public static File createCloneProject(String path) {
        String cloneProjectPath;
        /*
         * Ten hien tai dang /tmp/cloneProject_0.
         */
        try {
            String property = "java.io.tmpdir";
            cloneProjectPath = System.getProperty(property);

            // Create unique name for clone project
            cloneProjectPath = FilenameUtils.concat(cloneProjectPath, Paths.CURRENT_PROJECT.CLONE_PROJECT_NAME);

            int cloneProjectId = 0;
            cloneProjectPath += "_";
            while (new File(cloneProjectPath + cloneProjectId).exists())
                cloneProjectId++;

            cloneProjectPath += Integer.toString(cloneProjectId);

            // Clone project
            Utils.copyFolder(new File(Paths.CURRENT_PROJECT.ORIGINAL_PROJECT_PATH), new File(cloneProjectPath));

            // Set "chmod 777" for the clone project
            new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH).setExecutable(true);
            new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH).setReadable(true);
            new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH).setWritable(true);
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }

        return new File(cloneProjectPath);
    }

    /**
     *
     *
     * @return true if we need to parse the original project; false otherwise
     * @throws IOException
     */
    public static boolean shouldParseTheOriginalProject(int typeofProject) {
        switch (typeofProject) {
            case IProjectType.PROJECT_ECLIPSE: {
                return true;
            }
            case IProjectType.PROJECT_DEV_CPP:
            case IProjectType.PROJECT_UNKNOWN_TYPE:
            case IProjectType.PROJECT_CUSTOMMAKEFILE:
            case IProjectType.PROJECT_VISUALSTUDIO:
            case IProjectType.PROJECT_CODEBLOCK:
            default: {
                return false;
            }
        }
    }

    public static void viewPrototype(TestCase testCase) {
        AnchorPane tab = Factory.generateTestcaseTab(testCase);
        MDIWindowController.getMDIWindowController().viewPrototype(tab, testCase.getName());
    }

    public static void viewTestCase(TestCase testCase) {
        AnchorPane tab = Factory.generateTestcaseTab(testCase);
        MDIWindowController.getMDIWindowController().viewTestCase(tab, testCase.getName());
    }

    public static void viewTestCase(CompoundTestCase compoundTestCase) {
        Tab tab = Factory.generateCompoundTestCaseTab(compoundTestCase);
        MDIWindowController.getMDIWindowController().viewCompoundTestCase(tab, compoundTestCase.getName());
    }

    public static ISourcecodeFileNode searchSourceCodeFileNodeByPath(TestUnitNode unitNode, ProjectNode projectNode) {
        if (unitNode.getSrcNode() == null) {
            List<INode> sourcecodeNodes = Search.searchNodes(projectNode, new SourcecodeFileNodeCondition());
            for (INode sourcecodeNode : sourcecodeNodes)
                if (sourcecodeNode instanceof SourcecodeFileNode)
                    if (sourcecodeNode.getAbsolutePath().equals(unitNode.getName())) {
                        unitNode.setSrcNode((ISourcecodeFileNode) sourcecodeNode);
                        break;
                    }

        }
        return unitNode.getSrcNode();
    }
    public static ICommonFunctionNode searchFunctionNodeByPathInBackupEnvironment(String path) throws FunctionNodeNotFoundException {
        ICommonFunctionNode matchedFunctionNode;

        // create conditions to search (both complete function & prototype function).
        List<INode> functionNodes = new ArrayList<>();
        functionNodes.addAll(Search.searchNodes(Environment.getBackupEnvironment().getProjectNode(), new AbstractFunctionNodeCondition()));
        functionNodes.addAll(Search.searchNodes(Environment.getBackupEnvironment().getProjectNode(), new DefinitionFunctionNodeCondition()));
        functionNodes.addAll(Search.searchNodes(Environment.getBackupEnvironment().getProjectNode(), new MacroFunctionNodeCondition()));

        SystemLibraryRoot libraryRoot = Environment.getBackupEnvironment().getSystemLibraryRoot();
        if (libraryRoot != null)
            functionNodes.addAll(Search.searchNodes(libraryRoot, new AbstractFunctionNodeCondition()));

        for (INode functionNode : functionNodes) {
            if (functionNode instanceof ICommonFunctionNode) {
                if (Utils.normalizePath(functionNode.getAbsolutePath()).equals(Utils.normalizePath(path))) {
                    matchedFunctionNode = (ICommonFunctionNode) functionNode;
                    return matchedFunctionNode;
                }
            }
        }

        throw new FunctionNodeNotFoundException(path);
    }

    public static ICommonFunctionNode searchFunctionNodeByPath(String path) throws FunctionNodeNotFoundException {
        ICommonFunctionNode matchedFunctionNode;

        // create conditions to search (both complete function & prototype function).
        List<INode> functionNodes = new ArrayList<>();
        functionNodes.addAll(Search.searchNodes(Environment.getInstance().getProjectNode(), new AbstractFunctionNodeCondition()));
        functionNodes.addAll(Search.searchNodes(Environment.getInstance().getProjectNode(), new DefinitionFunctionNodeCondition()));
        functionNodes.addAll(Search.searchNodes(Environment.getInstance().getProjectNode(), new MacroFunctionNodeCondition()));

        SystemLibraryRoot libraryRoot = Environment.getInstance().getSystemLibraryRoot();
        if (libraryRoot != null)
            functionNodes.addAll(Search.searchNodes(libraryRoot, new AbstractFunctionNodeCondition()));

        for (INode functionNode : functionNodes) {
            if (functionNode instanceof ICommonFunctionNode) {
                if (Utils.normalizePath(functionNode.getAbsolutePath()).equals(Utils.normalizePath(path))) {
                    matchedFunctionNode = (ICommonFunctionNode) functionNode;
                    return matchedFunctionNode;
                }
            }
        }

//        int lastFileSeparator = path.lastIndexOf(File.separatorChar);
//        if (lastFileSeparator > 0) {
//            String prototype = path.substring(lastFileSeparator + 1);
//
//            IASTNode astNode = Utils.convertToIAST(prototype);
//
//            if (astNode instanceof IASTDeclarationStatement) {
//                astNode = ((IASTDeclarationStatement) astNode).getDeclaration();
//            }
//
//            if (astNode instanceof CPPASTSimpleDeclaration) {
//                DefinitionFunctionNode functionNode = new DefinitionFunctionNode();
//                functionNode.setAbsolutePath(path);
//                functionNode.setAST((CPPASTSimpleDeclaration) astNode);
//                functionNode.setName(functionNode.getNewType());
//                return functionNode;
//            }
//        }

        throw new FunctionNodeNotFoundException(path);
    }

    public static void refreshCompoundTestcaseViews() {
        MDIWindowController mdiWindowController = MDIWindowController.getMDIWindowController();
        Map<String, CompoundTestCaseTreeTableViewController> map = mdiWindowController.getCompoundTestCaseControllerMap();
        for (CompoundTestCaseTreeTableViewController controller : map.values()) {
            controller.refresh();
        }
    }

    public static void viewCoverageOfMultipleTestcases(String functionName, List<TestCase> testCases) {
        MDIWindowController.getMDIWindowController().viewCoverageOfMultipleTestcase(functionName, testCases);
    }

    public static void viewCoverageOfMultipleTestcasesFromAnotherThread(String functionName, List<TestCase> testCases) {
        Platform.runLater(() -> viewCoverageOfMultipleTestcases(functionName, testCases));
    }

    public static void viewCoverageOfATestcase(TestCase testCase) {
        String tabName = testCase.getName();
        List<TestCase> testCases = new ArrayList<>();
        testCases.add(testCase);
        MDIWindowController.getMDIWindowController().viewCoverageOfMultipleTestcase(tabName, testCases);
    }

    public static void viewSourceCode(INode node){
            // Get the source code file node
            INode sourcecodeFileNode = node;
            while (!(sourcecodeFileNode instanceof SourcecodeFileNode) && sourcecodeFileNode != null) {
                sourcecodeFileNode = sourcecodeFileNode.getParent();
            }

            if (sourcecodeFileNode != null) {
                // Set the option of viewing source code
                FXFileView fileView = new FXFileView(node);
                AnchorPane acp = fileView.getAnchorPane(true);
                MDIWindowController.getMDIWindowController().viewSourceCode(sourcecodeFileNode, acp);
            }
//        }else{
//            throw new OpenFileException("The target " + node.getAbsolutePath() + " does not exist");
//        }
    }

    public static void openTheLocation(INode node) throws OpenFileException {
        assert (node != null);

        if (node instanceof FolderNode || node instanceof SourcecodeFileNode || node instanceof ProjectNode || node instanceof UnknowObjectNode) {
            String path = node.getAbsolutePath();
            Utils.openFolderorFileOnExplorer(path);
        }
    }

    public static void loadTestCasesNavigator(File testcasesScript) {
        // load test cases script to Environment
        Environment.getInstance().loadTestCasesScript(testcasesScript);
        ITestcaseNode root = Environment.getInstance().getTestcaseScriptRootNode();
        LeftPaneController.getLeftPaneController().renderNavigator(root);
    }

    public static void loadProjectStructureTree(IProjectNode root) {
        LeftPaneController.getLeftPaneController().renderProjectTree(root);
    }

    public static void clear() {
        BaseSceneController.getBaseSceneController().clear();
        UILogger.reinitializeUiLogger();
    }

    public static void debugAndGetLog(ITestCase testCase) {
//        DucAnhDebugController.getDebugController().turnOff();
//        DebugController.getDebugController().loadAndExecuteTestCase(testCase);
//        ((DucAnhDebugController) DucAnhDebugController.getDebugController()).debugAllLine(testCase);

        SE se = new SE();
        se.setExe(testCase.getExecutableFile());
        se.setTestdriver(testCase.getSourceCodeFile());
        se.setSrc2("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/Algorithm/gb.Utils.akaignore.cpi");
        try {
            se.compile();
            se.debug(se.getExe());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void executeTestCaseWithDebugMode(ITestCase testCase) {
        MDIWindowController.getMDIWindowController().viewDebug(testCase);
    }

    public static void newCCPPEnvironment() {
        Environment.createNewEnvironment();
        showEnvironmentBuilderWindow();
    }

    public static void updateCCPPEnvironment() {
        // clone the current environment to modify for updating
        Environment.backupEnvironment();
        showEnvironmentBuilderWindow();
    }

    private static void showEnvironmentBuilderWindow(){
        Scene scene = BaseController.getBaseScene();
        environmentBuilderStage = new Stage();
        environmentBuilderStage.setTitle("Build Environment");
        environmentBuilderStage.setResizable(false);
        environmentBuilderStage.setScene(scene);
        environmentBuilderStage.setOnCloseRequest(event -> {
            BaseController.resetStatesInAllWindows();
            Environment.restoreEnvironment();
        });

        // block the primary window
        environmentBuilderStage.initModality(Modality.WINDOW_MODAL);
        environmentBuilderStage.initOwner(getPrimaryStage().getScene().getWindow());

        environmentBuilderStage.show();
    }
    public static void showErrorDialog(String content, String title, String headText) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(headText);
            alert.showAndWait();
        });
    }

    public static void showDetailDialog(Alert.AlertType type, String title, String headText, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(headText);

            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            textArea.setEditable(false);
            textArea.setMinHeight(350);
            textArea.setText(content);

            alert.getDialogPane().setContent(textArea);

            alert.showAndWait();
        });
    }

    public static void showDetailDialogInMainThread(Alert.AlertType type, String title, String headText, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headText);

        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setMinHeight(350);
        textArea.setText(content);

        alert.getDialogPane().setContent(textArea);

        alert.showAndWait();
    }

    public static void showSuccessDialog(String content, String title, String headText) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(headText);
            alert.showAndWait();
        });
    }

    public static void showInformationDialog(String content, String title, String headText) {
       showSuccessDialog(content, title, headText);
    }

    public static CodeArea showCodeEditorDialog(UnresolvedDataNode dataNode, AbstractTableCell cell) {
        String displayName = dataNode.getDisplayNameInParameterTree();
        String virtualName = dataNode.getVituralName();
        String headerText = String.format("Define %s (code: %s)", displayName, virtualName);

        UserCodeDialog dialog = new UserCodeDialog(headerText, dataNode.getUserCode()) {
            @Override
            public void setOnOkRequest() {
                String newUserCode = getContent();
                dataNode.setUserCode(newUserCode);
                cell.updateItem(newUserCode, false);
                TestCaseManager.exportBasicTestCaseToFile(cell.getTestCase());
            }

            @Override
            public void setOnCloseRequest() {
                cell.updateItem(dataNode.getUserCode(), false);
            }

            @Override
            public String getTemplate() {
                return "#include \"" + dataNode.getContextPath() + "\"\n" +
                        "\n" +
                        "int main() {\n" +
                            "%s\n" +
                            "return 0;" +
                        "}\n";
            }

            @Override
            public String getArchivePath() {
                return dataNode.getTemporaryPath();
            }
        };

        Platform.runLater(dialog::showAndWait);

        return dialog.getCodeArea();
    }

    public static Alert showYesNoDialog(Alert.AlertType type, String title, String headText, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headText);

        double width = alert.getDialogPane().getWidth();
        double padding = 10;
        Text text = new Text(content);
        text.setWrappingWidth(width - padding * 2);
        alert.getDialogPane().setPadding(new Insets(padding));
        alert.getDialogPane().setContent(text);

        ButtonType okButton = ButtonType.YES;
        ButtonType noButton = ButtonType.NO;
        alert.getButtonTypes().setAll(okButton, noButton);

//        alert.getDialogPane().getScene().getWindow().sizeToScene();

        return alert;
    }

    public static void showMacroTypeDefinitionDialog(MacroSubprogramDataNode macroSubprogram,
                                                     ParameterColumnCellFactory.ParameterColumnCell cell) {

        MacroFunctionNode functionNode = (MacroFunctionNode) macroSubprogram.getFunctionNode();

        Alert alert = new TypeDeclarationDialog(functionNode) {
            @Override
            public void onOkClick(Map<String, String> typeMap) throws Exception {
                macroSubprogram.getChildren().clear();
                macroSubprogram.setRealFunctionNode(typeMap);
                macroSubprogram.setRealTypeMapping(typeMap);
                cell.refresh();
            }

            @Override
            public ICompileMessage onTestCompile(Map<String, String> typeMap) {
                UnitNode unitNode = macroSubprogram.getUnit();
                INode context = unitNode.getSourceNode();

                String prototype = macroSubprogram.generatePrototype(typeMap);

                ICompileMessage compileMessage = CompilerUtils.testCompile(typeMap.get("RETURN"), prototype, functionNode, context);
                if (compileMessage.getType() == ICompileMessage.MessageType.ERROR)
                    UIController.showDetailDialog(AlertType.ERROR, "Compilation message",
                            "Fail", compileMessage.getMessage());
                else
                    UIController.showSuccessDialog("The source code file "
                                    + Utils.getSourcecodeFile(functionNode).getAbsolutePath() + " is compile successfully with the given types",
                            "Compilation message", "Success");
                return compileMessage;
            }
        };

        Platform.runLater(alert::showAndWait);
    }

    public static void showTemplateTypeDefinitionDialog(TemplateSubprogramDataNode templateSubprogram,
                                                        ParameterColumnCellFactory.ParameterColumnCell cell) {

        ICommonFunctionNode functionNode = (ICommonFunctionNode) templateSubprogram.getFunctionNode();

        final String[] prototype = {SpecialCharacter.EMPTY};

        Alert alert = new TypeDeclarationDialog(functionNode) {
            Map<String, String> typeMap;

            @Override
            public void onOkClick(Map<String, String> typeMap) throws Exception {
                templateSubprogram.getChildren().clear();
                templateSubprogram.setRealFunctionNode(prototype[0]);
                templateSubprogram.setRealTypeMapping(typeMap);
                cell.refresh();
            }

            @Override
            public ICompileMessage onTestCompile(Map<String, String> typeMap) {
                this.typeMap = typeMap;
                String returnType = functionNode.getReturnType();

                prototype[0] = returnType;
                prototype[0] += " ";
                prototype[0] += "AKA_TEMPLATE_" + functionNode.getSingleSimpleName();
                prototype[0] += "(";

                for (IVariableNode arg : functionNode.getArguments()) {
                    prototype[0] += Utils.generateVariableDeclaration(arg.getRawType(), arg.getName()) + ", ";
                }

                prototype[0] += ")";
                prototype[0] = prototype[0].replace(", )", ")");

                for (Map.Entry<String, String> entry : typeMap.entrySet()) {
                    prototype[0] = prototype[0].replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
                }

                UnitNode unitNode = templateSubprogram.getUnit();
                INode context = unitNode.getSourceNode();

                ICompileMessage compileMessage = CompilerUtils.testCompile(returnType, prototype[0], functionNode, context);
                if (compileMessage.getType() == ICompileMessage.MessageType.ERROR)
                    UIController.showDetailDialog(AlertType.ERROR, "Compilation message",
                            "Fail", compileMessage.getMessage());
                else
                    UIController.showSuccessDialog("The source code file "
                                    + Utils.getSourcecodeFile(functionNode).getAbsolutePath() + " is compile successfully with the given types",
                            "Compilation message", "Success");
                return compileMessage;
            }
        };

        Platform.runLater(alert::showAndWait);
    }

    public static void showArrayExpanderDialog(ParameterColumnCellFactory.ParameterColumnCell cell) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setTitle("Expand Array Item");
            alert.setHeaderText("Expand children of " + cell.getText() + " by index");

            TextField textField = new TextField();
            alert.getDialogPane().setContent(textField);

            final Button btnOk = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            btnOk.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    cell.expandArrayItems(textField.getText());
                } catch (Exception e) {
                    showErrorDialog("Error: " + e.getMessage(), "Invalid input", "Something wrong with input");
//                    e.printStackTrace();/**/
                }
            });

            alert.showAndWait();
        });
    }
}

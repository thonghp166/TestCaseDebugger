package com.dse.environment;

import com.dse.config.WorkspaceConfig;
import com.dse.environment.object.*;
import com.dse.guifx_v3.controllers.main_view.MenuBarController;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.objects.background_task.BackgroundTaskObjectController;
import com.dse.guifx_v3.objects.background_task.BackgroundTasksMonitorController;
import com.dse.guifx_v3.objects.background_task.CompileSourcecodeFilesTask;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.TypeDependency;
import com.dse.parser.object.*;
import com.dse.project_init.ProjectClone;
import com.dse.regression.AbstractDifferenceDetecter;
import com.dse.regression.ChangesBetweenSourcecodeFiles;
import com.dse.regression.SimpleDifferenceDetecter;
import com.dse.regression.UnresolvedDependency;
import com.dse.search.Search;
import com.dse.search.condition.AbstractFunctionNodeCondition;
import com.dse.search.condition.NodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.stub_manager.SystemLibrary;
import com.dse.util.AkaLogger;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Load the existing workspace including the dependency files, the hierarchical structure, etc.
 */
public class WorkspaceLoader {
    public final static AkaLogger logger = AkaLogger.get(WorkspaceLoader.class);

    private boolean shouldCompileAgain = false;
    private String elementFolderOfOldVersion;
    private File physicalTreePath;

    private Node root;
    private boolean isLoaded = false;
    private boolean isCancel = false;

    public static void main(String[] args) {
//        WorkspaceLoader loader = new WorkspaceLoader();
//        loader.setPhysicalTreePath(new File("/Users/ducanhnguyen/Documents/akautauto/local/working-directory/1/physical_tree.json"));
//        loader.setElementFolderOfOldVersion("...");
//        Node root = loader.load(loader.getPhysicalTreePath(), loader.getElementFolderOfOldVersion());
//        System.out.println(new DependencyTreeDisplayer(root).getTreeInString());
    }

    public static List<INode> getCompilableSourceNodes() {
        List<INode> sourcecodeFileNodes = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());

        EnvironmentRootNode envRoot = Environment.getInstance().getEnvironmentRootNode();

        List<String> uuts = EnvironmentSearch
                .searchNode(envRoot, new EnviroUUTNode())
                .stream().map(node -> ((EnviroUUTNode) node).getName())
                .collect(Collectors.toList());
        List<String> ignores = EnvironmentSearch
                .searchNode(envRoot, new EnviroIgnoreNode())
                .stream().map(node -> ((EnviroIgnoreNode) node).getName())
                .collect(Collectors.toList());
        List<String> sbfs = EnvironmentSearch
                .searchNode(envRoot, new EnviroSBFNode())
                .stream().map(node -> ((EnviroSBFNode) node).getName())
                .collect(Collectors.toList());

        List<String> libraries = ProjectClone.getLibraries();

        sourcecodeFileNodes.removeIf(f -> {
            String path = f.getAbsolutePath();
            return ignores.contains(path) || libraries.contains(path)
                    || (f instanceof HeaderNode && (!uuts.contains(path) && !sbfs.contains(path)));
        });

        return sourcecodeFileNodes;
    }

    /**
     *
     * @param physicalTreePath
     */
    public void load(File physicalTreePath) {
        /*
         * Load the physical tree of the original testing project.
         * During the use of aka, the original testing project may be changed by adding some files.
         * When loading the existing environment, we just parse the file in the original testing project
         */
        logger.debug("Construct the initial tree from json " + physicalTreePath);
        isLoaded = false;
        isCancel = false;
        root = new PhysicalTreeImporter().importTree(physicalTreePath);
        Environment.getInstance().setProjectNode((ProjectNode) root);

        if (shouldCompileAgain) {
            List<INode> sourcecodeFileNodes = getCompilableSourceNodes();
            CompileSourcecodeFilesTask task = new CompileSourcecodeFilesTask(sourcecodeFileNodes);
            // add new Background Task Object to BackgroundTasksMonitor
            BackgroundTaskObjectController controller = BackgroundTaskObjectController.getNewInstance();
            if (controller != null) {
                // parent children relationship synchronize stopping
                controller.setParent(MenuBarController.getMenuBarController().getRebuildEnvironmentBGController());
                MenuBarController.getMenuBarController().getRebuildEnvironmentBGController().getChildren().add(controller);

                controller.setlTitle("Compiling All Source Code Files");
                // set task to controller to cancel as need when processing
                controller.setTask(task);
                controller.setCancelTitle("Stopping Compile Source Code Files");
                controller.getProgressBar().progressProperty().bind(task.progressProperty());
                controller.getProgressIndicator().progressProperty().bind(task.progressProperty());
                Platform.runLater(() -> BackgroundTasksMonitorController.getController().addBackgroundTask(controller));

                task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                        if (!task.getValue()) {
                            /*
                             * Parse every source code file to construct the structure of the testing project again.
                             * If there is any change to source code, e.g. someone add/modify a function, we can detect this change.
                             */
                            expand(root);

                            isLoaded = true;
                            isCancel = false;
                        } else {
                            isLoaded = true;
                            isCancel = true;
                        }

                        // remove task if done
                        Platform.runLater(() -> BackgroundTasksMonitorController.getController().removeBackgroundTask(controller));
                    }
                });

                task.setOnCancelled(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                        isLoaded = true;
                        isCancel = true;
                    }
                });
                new Thread(task).start();
            }
        } else {
            expand(root);
            isLoaded = true;
            isCancel = false;
        }
    }

    private void expand(Node root) {
        logger.debug("Start expanding the initial tree down to method level");
        List<INode> sourcecodeFiles = Search.searchNodes(root, new SourcecodeFileNodeCondition());
        int N = sourcecodeFiles.size() - 1;

        for (int idx = N; idx >= 0; idx--) {
            INode sourcecodeFile = sourcecodeFiles.get(idx);

            logger.debug("\tParsing " + sourcecodeFile.getAbsolutePath());
            String content = Utils.readFileContent(sourcecodeFile.getAbsolutePath());
            try {
                IASTTranslationUnit ast = Utils.getIASTTranslationUnitforCpp(content.toCharArray());
                ((SourcecodeFileNode) sourcecodeFile).setAST(ast);

                // expand the tree down to method level
                SourcecodeFileParser cppParser = new SourcecodeFileParser();
                cppParser.setSourcecodeNode((SourcecodeFileNode) sourcecodeFile);
                INode newRoot = cppParser.parseSourcecodeFile(new File(sourcecodeFile.getAbsolutePath()));
                for (Node child : newRoot.getChildren()) {
                    child.setParent(sourcecodeFile);
                    sourcecodeFile.getChildren().add(child);
                }
                logger.debug("Size of children in " + sourcecodeFile.getAbsolutePath() + ": " + sourcecodeFile.getChildren().size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.debug("Finish expanding the initial tree down to method level");

        /*
         * Load stub library physical tree
         */
        SystemLibraryRoot sysLibRoot = SystemLibrary.parseFromFile();
        Environment.getInstance().setSystemLibraryRoot(sysLibRoot);
        logger.debug("Load stub library from file");

        /*
         * Load dependencies
         */
        logger.debug("Load dependencies from file");
        loadDependenciesFromAllFiles(root, sysLibRoot);

        detectChangeInSourcecodeFilesWhenOpeningEnv(root, elementFolderOfOldVersion);

        // load unit under test for enviro node
        loadUnitTestableState();

        exportVersionComparisonToFile();
    }

    private void loadUnitTestableState() {
        List<INode> sources = Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition());

        List<IEnvironmentNode> uuts = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroUUTNode());
        for (IEnvironmentNode uut : uuts) {
            for (INode source : sources) {
                if (((EnviroUUTNode) uut).getName().equals(source.getAbsolutePath())) {
                    ((EnviroUUTNode) uut).setUnit(source);
                    break;
                }
            }
        }

        List<IEnvironmentNode> sbfs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroSBFNode());
        for (IEnvironmentNode sbf : sbfs) {
            for (INode source : sources) {
                if (((EnviroSBFNode) sbf).getName().equals(source.getAbsolutePath())) {
                    ((EnviroSBFNode) sbf).setUnit(source);
                    break;
                }
            }
        }

        List<IEnvironmentNode> dontStubs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroDontStubNode());
        for (IEnvironmentNode dontStub : dontStubs) {
            for (INode source : sources) {
                if (((EnviroDontStubNode) dontStub).getName().equals(source.getAbsolutePath())) {
                    ((EnviroDontStubNode) dontStub).setUnit(source);
                    break;
                }
            }
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isCancel() {
        return isCancel;
    }

    //    /**
//     *
//     * @return false if we found compilation error
//     */
//    private boolean compileAllSourcecodeFile() {
//        // some changes make a source code file unable to compile
//        String error = "";
//        boolean foundError = false;
//        for (INode modifiedSrcFile : Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition())) {
//            Compiler c = Environment.getInstance().getCompiler();
//            ICompileMessage message = c.compile(modifiedSrcFile);
//            if (message.getType() == ICompileMessage.MessageType.ERROR) {
//                error += modifiedSrcFile.getAbsolutePath() + "\nMESSSAGE:\n" + message.getMessage() + "\n----------------\n";
//                foundError = true;
//            }
//        }
//        if (foundError) {
//            String compilationMessageFile = new WorkspaceConfig().fromJson().getCompilationMessageWhenComplingProject();
//            Utils.deleteFileOrFolder(new File(compilationMessageFile));
//            Utils.writeContentToFile(error, compilationMessageFile);
//        }
//        return foundError;
//    }


    public Node getRoot() {
        return root;
    }

    private void detectChangeInSourcecodeFilesWhenOpeningEnv(Node root, String elementFolderOfOldVersion) {
        AbstractDifferenceDetecter changeDetecter = new SimpleDifferenceDetecter();
        changeDetecter.detectChanges(root, elementFolderOfOldVersion);

        ChangesBetweenSourcecodeFiles.modifiedSourcecodeFiles = changeDetecter.getModifiedSourcecodeFiles();
        ChangesBetweenSourcecodeFiles.addedNodes = changeDetecter.getAddedNodes();
        ChangesBetweenSourcecodeFiles.deletedPaths = changeDetecter.getDeletedPaths();
        ChangesBetweenSourcecodeFiles.modifiedNodes = changeDetecter.getModifiedNodes();
    }

    private void exportVersionComparisonToFile() {
        // export the report of difference to files
        {
            String changeFile = new WorkspaceConfig().fromJson().getFileContainingUnresolvedDependenciesWhenComparingSourcecode();
            Utils.deleteFileOrFolder(new File(changeFile));
            Utils.writeContentToFile(ChangesBetweenSourcecodeFiles.getUnresolvedDepdendenciesInString(), changeFile);
        }

        // export the report of difference to files
        {
            String changeFile = new WorkspaceConfig().fromJson().getFileContainingChangesWhenComparingSourcecode();
            Utils.deleteFileOrFolder(new File(changeFile));
            Utils.writeContentToFile(ChangesBetweenSourcecodeFiles.getReportOfDifferences(), changeFile);
        }

        // export the report of changed source code to file
        {
            String changeFile = new WorkspaceConfig().fromJson().getFileContainingChangedSourcecodeFileWhenComparingSourcecode();
            Utils.deleteFileOrFolder(new File(changeFile));
            Utils.writeContentToFile(ChangesBetweenSourcecodeFiles.getModifiedSourcecodeFilesInString(), changeFile);
        }
    }
    /**
     * Load dependencies from all dependency files
     *
     * @param root
     */
    private void loadDependenciesFromAllFiles(Node root, Node libRoot) {
        File dependenciesFolder = new File(new WorkspaceConfig().fromJson().getDependencyDirectory());
        logger.debug("Load dependency from folder " + dependenciesFolder.getAbsolutePath());

        if (dependenciesFolder.exists()) {
            if (root != null) {
                List<INode> allNodes = Search.searchNodes(root, new NodeCondition());
                allNodes.addAll(Search.searchNodes(libRoot, new AbstractFunctionNodeCondition()));

                for (String filePath : Utils.getAllFiles(dependenciesFolder.getAbsolutePath()))
                    if (filePath.endsWith(WorkspaceConfig.AKA_EXTENSION)) {
                        loadDependenciesFromFile(new File(filePath), allNodes);
                    }
            }
        }
    }

    /**
     * Load depdendencies stored in a depdendency file
     *
     * @param file     a file containing a set of dependencies
     * @param allNodes all nodes in the tree which we use to search for
     */
    private void loadDependenciesFromFile(File file, List<INode> allNodes) {
        if (file.getAbsolutePath().endsWith(WorkspaceConfig.AKA_EXTENSION)) {
            // parse the content of dependency file
            String json = Utils.readFileContent(file);
            JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
            String path = jsonObject.get("path").getAsString();
            JsonArray dependecies = jsonObject.get("dependency").getAsJsonArray();

            if (path != null && dependecies != null) {
                logger.debug("Loading dependency from: " + path);

                // iterate over all dependencies
                for (JsonElement element : dependecies) {
                    JsonObject cast = (JsonObject) element;

                    // get start node of dependency
                    String start = cast.get("start").getAsString();
                    start = PathUtils.toAbsolute(start);
                    String start_md5 = null;
                    if (cast.get("start-md5") != null)
                        start_md5 = cast.get("start-md5").getAsString();

                    // get end node of dependency
                    String end = cast.get("end").getAsString();
                    end = PathUtils.toAbsolute(end);
                    String end_md5 = null;
                    if (cast.get("end-md5") != null)
                        end_md5 = cast.get("end-md5").getAsString();

                    // get type of the dependency
                    String type = cast.get("type").getAsString();

                    logger.debug("Load a dependency from file: [" + type + "]" + start + " -> " + end);

                    // check matching
                    if (start != null && end != null && type != null) {
                        boolean isMatchingSuccess = findMatching(type, start, end, start_md5, end_md5, allNodes);
                        if (!isMatchingSuccess) {
                            UnresolvedDependency unresolvedDependency = new UnresolvedDependency(start, end, type);
                            if (!ChangesBetweenSourcecodeFiles.unresolvedDependencies.contains(unresolvedDependency))
                                ChangesBetweenSourcecodeFiles.unresolvedDependencies.add(unresolvedDependency);
                        } else{
                            logger.debug("Match successfully [" + type + "] " + start + " -> " + end);
                        }
                    } else {
                        logger.error("The dependency file is invalid: " + file.getAbsolutePath());
                        logger.error("Can not get start, end, or type of dependency.");
                    }
                }
            } else {
                logger.error("The dependency file is invalid: " + file.getAbsolutePath());
                logger.error("Can not get path or dependencies");
            }
        }else{
            logger.debug("Ignore " + file.getAbsolutePath() + " because it is not dependency file (.aka)");
        }
    }

    /**
     * @param type      the type of the dependency
     * @param start     the absolute path of the start node
     * @param end       the absolute path of the end node
     * @param start_md5 the md5 of the content corresponding to the start node (if it has)
     * @param end_md5   the md5 of the content corresponding to the end node (if it has)
     * @param allNodes  all nodes in the tree
     * @return
     */
    private boolean findMatching(String type, String start, String end, String start_md5, String end_md5, List<INode> allNodes) {
        String typeID = "com.dse.parser.dependency." + type;

        if (start.length() == 0 || end.length() == 0 || allNodes.size() == 0)
            return false;

        // find the starting nod
        INode startNode = findNode(start, start_md5, allNodes);
        if (startNode == null) {
            logger.error("Can not find the corresponding node having path " + start);
            return false;
        }

        // find the last node
        INode endNode = findNode(end, end_md5, allNodes);
        if (endNode == null) {
            logger.error("Can not find the corresponding node having path " + end);
            return false;
        }

        // perform matching
        logger.debug("Find the corresponding nodes of path " + start + " and " + end);
        try {
            Constructor c = Class.forName(typeID).getConstructor(INode.class, INode.class);
            Dependency dependency = (Dependency) c.newInstance(startNode, endNode);

            // Save dependency
            if (!startNode.getDependencies().contains(dependency))
                startNode.getDependencies().add(dependency);

            if (!endNode.getDependencies().contains(dependency))
                endNode.getDependencies().add(dependency);

            if (type.equals(TypeDependency.class.getSimpleName())) {
                if (startNode instanceof IVariableNode) {
                    ((IVariableNode) startNode).setCorrespondingNode(endNode);
                    ((IVariableNode) startNode).setTypeDependencyState(true);
                } else if (endNode instanceof IVariableNode) {
                    ((IVariableNode) endNode).setCorrespondingNode(startNode);
                    ((IVariableNode) endNode).setTypeDependencyState(true);
                }
            }

            List<Dependency> dependencies = Environment.getInstance().getDependencies();
            if (!dependencies.contains(dependency))
                dependencies.add(dependency);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected INode findNode(String absoluteOfNode, String correspondingMd5Checksum, List<INode> allNodes) {
        INode startNode = null;
        for (INode n : allNodes)
            if (n.getAbsolutePath().equals(absoluteOfNode)) {
                // find a candidate
                if (correspondingMd5Checksum == null) {
                    startNode = n;
                    break;
                } else {
                    // compare md5 if we have
                    if (n instanceof CustomASTNode && ((CustomASTNode) n).getAST() != null)
                        if (Utils.computeMd5(((CustomASTNode) n).getAST().getRawSignature()).equals(correspondingMd5Checksum)) {
                            startNode = n;
                            break;
                        }
                }
            }

        return startNode;
    }

    public File getPhysicalTreePath() {
        return physicalTreePath;
    }

    public void setElementFolderOfOldVersion(String elementFolderOfOldVersion) {
        this.elementFolderOfOldVersion = elementFolderOfOldVersion;
    }

    public String getElementFolderOfOldVersion() {
        return elementFolderOfOldVersion;
    }

    public void setPhysicalTreePath(File physicalTreePath) {
        this.physicalTreePath = physicalTreePath;
    }

    public void setShouldCompileAgain(boolean shouldCompileAgain) {
        this.shouldCompileAgain = shouldCompileAgain;
    }
}

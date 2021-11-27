package com.dse.guifx_v3.helps;

import com.dse.compiler.AvailableCompiler;
import com.dse.compiler.Compiler;
import com.dse.config.FunctionConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.PhysicalTreeImporter;
import com.dse.environment.object.*;
import com.dse.guifx_v3.controllers.CompoundTestCaseTreeTableViewController;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.object.*;
import com.dse.project_init.ProjectClone;
import com.dse.search.Search;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcasescript.TestcaseAnalyzer;
import com.dse.testcasescript.object.ITestcaseNode;
import com.dse.testcasescript.object.TestcaseRootNode;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;
import com.dse.util.bound.BoundOfDataTypes;
import com.dse.util.bound.BoundOfDataTypesDeserializer;
import com.dse.util.bound.BoundOfDataTypesSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.control.Tab;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Environment {
    private final static AkaLogger logger = AkaLogger.get(Environment.class);

    /**
     * Singleton pattern
     */
    private static Environment instance = null;
    private static Environment backupEnvironment = null;

    public static Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
            instance.initialize_by_default();
        }
        return instance;
    }

    public static Environment getBackupEnvironment(){
        return backupEnvironment;
    }
    private Map<Tab, INode> activeSourcecodeTabs = new HashMap<>(); // save the active source code tab
    private CompoundTestCaseTreeTableViewController currentTestcompoundController; // name of the current test compound opening, it is unique
    private String currentTestcaseTab; // name of the current test case tab, it is unique
    private String currentPrototypeTab; // name of the current prototype tab, it is unique (used to set the real type of argument in template function, macro function)
    private EnvironmentRootNode environmentRootNode = null; // must be null
    private TestcaseRootNode testcaseScriptRootNode = null; // must be null
    private List<ProjectNode> projectRoots = new ArrayList<>();
    private ProjectNode projectNode;
    private List<Dependency> dependencies = new ArrayList<>();
    private SystemLibraryRoot systemLibraryRoot = new SystemLibraryRoot();
    private ProjectNode systemRoot;
    private Compiler compiler;
    // save all resolved nodes to save loading cost
    private Map<String, INode> resolvedNodes = new HashMap<>();
    private static boolean coverageModeActive = false;

    private FunctionConfig defaultFunctionConfig = null;

    public void createACloneVersionToModify(){
        // we will update the original env file and tst file
        EnvironmentAnalyzer environmentAnalyzer = new EnvironmentAnalyzer();
        environmentAnalyzer.analyze(new File(new WorkspaceConfig().fromJson().getEnvironmentFile()));
        environmentRootNode = (EnvironmentRootNode) environmentAnalyzer.getRoot();

        TestcaseAnalyzer tstAnalyzer = new TestcaseAnalyzer();
        testcaseScriptRootNode = (TestcaseRootNode) tstAnalyzer.analyze(new File(new WorkspaceConfig().fromJson().getTestscriptFile()));

//        WorkspaceLoader loader = new WorkspaceLoader();
//        projectNode = (ProjectNode) loader.load(new File(new WorkspaceConfig().fromJson().getPhysicalJsonFile()));

        Node root = new PhysicalTreeImporter().importTree(new File(new WorkspaceConfig().fromJson().getPhysicalJsonFile()));
        projectNode = (ProjectNode) root;
    }

    public void initialize_by_default(){
        Environment.getInstance().setProjectNode(null);

//        logger.debug("Initialize the environment by default");
//
//        logger.debug("Overall configuration is saved in " + AkaConfig.SETTING_PROPERTIES_PATH);
//
//        logger.debug("+ Create the initial version of environment script");
        environmentRootNode = new EnvironmentRootNode();
        environmentRootNode.addChild(new EnviroNewNode());

//        // be default, all dependent source code files are stub_by_prototype
//        logger.debug("+ By default, all dependent units are stub by prototyle");
//        EnviroStubNode stubNode = new EnviroStubNode();
//        stubNode.setStub(EnviroStubNode.ALL_BY_PROTOTYPE);
//        stubNode.setParent(getEnvironmentRootNode());
//        environmentRootNode.addChild(stubNode);

        environmentRootNode.addChild(new EnviroEndNode());
    }

    public boolean isC() {
        return !getCompiler().getName().contains("C++");
    }

    /**
     * Save the current Env
     *
     * Create a new Env and switch to the new env
     */
    public static void createNewEnvironment() {
        backupEnvironment = instance;
        instance = new Environment();
        instance.initialize_by_default();
    }

    public static void backupEnvironment() {
        backupEnvironment = instance;
        instance = new Environment();
        instance.createACloneVersionToModify();
    }

    public static void restoreEnvironment() {
        instance = backupEnvironment;
        backupEnvironment = null;
    }

    public String getName(){
        if (getEnvironmentRootNode() != null){
            List<IEnvironmentNode> nodes = EnvironmentSearch.searchNode(getEnvironmentRootNode(), new EnviroNameNode());
            if (nodes.size() == 1) {
                return ((EnviroNameNode) nodes.get(0)).getName();
            }
        }
        return "";
    }

    public void setSystemLibraryRoot(SystemLibraryRoot systemLibraryRoot) {
        this.systemLibraryRoot = systemLibraryRoot;
    }

    public SystemLibraryRoot getSystemLibraryRoot() {
        return systemLibraryRoot;
    }

    public void loadTestCasesScript(File scriptFile) {
        //testcasesScriptFilePath = scriptFile.getAbsolutePath();
        TestcaseAnalyzer analyzer = new TestcaseAnalyzer();
        ITestcaseNode root = analyzer.analyze(scriptFile);
        testcaseScriptRootNode = (TestcaseRootNode) root;
    }

    public void saveTestcasesScriptToFile() {
        String content = testcaseScriptRootNode.exportToFile();
        try {
            FileWriter writer = new FileWriter(new WorkspaceConfig().fromJson().getTestscriptFile());
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.error("IOEXception when save test cases cript to file");
        }
    }

    public void saveEnvironmentScriptToFile() {
        String content = environmentRootNode.exportToFile();
        try {
            FileWriter writer = new FileWriter(new WorkspaceConfig().fromJson().getEnvironmentFile());
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.error("IOEXception when save test cases cript to file");
        }
    }

    public ProjectNode getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(ProjectNode systemRoot) {
        this.systemRoot = systemRoot;
    }

    public static class WindowState {
        private static boolean isSearchListNodeUpdated;

        public static void isSearchListNodeUpdated(boolean isSearchListNodeUpdated) {
            WindowState.isSearchListNodeUpdated = isSearchListNodeUpdated;
        }

        public static boolean isIsSearchListNodeUpdated() {
            return isSearchListNodeUpdated;
        }
    }

    public TestcaseRootNode getTestcaseScriptRootNode() {
        if (testcaseScriptRootNode == null)
            loadTestCasesScript(new File(new WorkspaceConfig().fromJson().getTestscriptFile()));
        return testcaseScriptRootNode;
    }

    public EnvironmentRootNode getEnvironmentRootNode() {
        if (environmentRootNode == null) {
//            EnvironmentAnalyzer analyzer = new EnvironmentAnalyzer();
//            analyzer.analyze(new File(new WorkspaceConfig().fromJson().getEnvironmentFile()));
//            environmentRootNode = (EnvironmentRootNode) analyzer.getRoot();
//            logger.error("Environment root node is null");
            environmentRootNode = new EnvironmentRootNode();
        }
        return environmentRootNode;
    }

    public void setEnvironmentRootNode(EnvironmentRootNode newRoot) {
        environmentRootNode = newRoot;
    }

    public void setProjectRoots(List<ProjectNode> projectRoots) {
        this.projectRoots = projectRoots;
    }
    public List<ProjectNode> getProjectRoots() {
        return projectRoots;
    }

    public String getCurrentTestcaseTab() {
        return currentTestcaseTab;
    }

    public void setCurrentPrototypeTab(String currentPrototypeTab) {
        logger.debug("Current prototype tab = " + currentPrototypeTab);
        this.currentPrototypeTab = currentPrototypeTab;
    }

    public void setCurrentTestcaseTab(String currentTestcaseTab) {
        logger.debug("Current test case tab = " + currentTestcaseTab);
        this.currentTestcaseTab = currentTestcaseTab;
    }

    public ProjectNode getProjectNode() {
        return projectNode;
    }

    public void setProjectNode(ProjectNode projectNode) {
        this.projectNode = projectNode;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public CompoundTestCaseTreeTableViewController getCurrentTestcompoundController() {
        return currentTestcompoundController;
    }

    public void setCurrentTestcompoundController(CompoundTestCaseTreeTableViewController currentTestcompoundController) {
        this.currentTestcompoundController = currentTestcompoundController;
    }

    public boolean isCoverageModeActive() {
        return coverageModeActive;
    }

    public void setCoverageModeActive(boolean coverageModeActive) {
        Environment.coverageModeActive = coverageModeActive;
    }

    /**
     * The coverage of the environment is changed over time. The best way is that we need to load from
     * the environment when we need.
     */
    public String getTypeofCoverage(){
        List<IEnvironmentNode> envNodes = EnvironmentSearch.searchNode(getEnvironmentRootNode(), new EnviroCoverageTypeNode());
        if (envNodes.size()==1){
            EnviroCoverageTypeNode coverageNode = (EnviroCoverageTypeNode) envNodes.get(0);
            return coverageNode.getCoverageType();
        }

        return ""; // unspecified type of coverage
    }

    public boolean isOnWhiteBoxMode() {
        List<IEnvironmentNode> whiteBoxes = EnvironmentSearch
                .searchNode(getEnvironmentRootNode(), new EnviroWhiteBoxNode());

        boolean whiteBox = false;
        for (IEnvironmentNode node : whiteBoxes) {
            if (((EnviroWhiteBoxNode) node).isActive()) {
                whiteBox = true;
                break;
            }
        }

        return whiteBox;
    }

    /**
     * Get all Unit Under Test physical node in project tree.
     * @return list of uut
     */
    public List<INode> getUUTs() {
        List<INode> sources = new ArrayList<>();

        List<IEnvironmentNode> uuts = EnvironmentSearch
                .searchNode(getEnvironmentRootNode(), new EnviroUUTNode());

        for (IEnvironmentNode uut : uuts)
            sources.add(((EnviroUUTNode) uut).getUnit());

        return sources;
    }

    /**
     * Get all SBF Unit Under Test physical node in project tree.
     * @return list of sbf
     */
    public List<INode> getSBFs() {
        List<INode> sources = new ArrayList<>();

        List<IEnvironmentNode> sbfs = EnvironmentSearch
                .searchNode(getEnvironmentRootNode(), new EnviroSBFNode());

        for (IEnvironmentNode sbf : sbfs)
            sources.add(((EnviroSBFNode) sbf).getUnit());

        return sources;
    }

    public FunctionConfig getDefaultFunctionConfig() {
        // todo: khi chua co moi truong thi khong dc lay default
        if (defaultFunctionConfig == null) {
            defaultFunctionConfig = new FunctionConfig();
        }
        return defaultFunctionConfig;
    }

    /**
     * Get all ignore unit physical node in project tree.
     * @return list of ignore unit
     */
    public List<INode> getIgnores() {
        List<INode> sources = new ArrayList<>();

        List<IEnvironmentNode> units = EnvironmentSearch
                .searchNode(getEnvironmentRootNode(), new EnviroIgnoreNode());

        for (IEnvironmentNode unit : units)
            sources.add(((EnviroIgnoreNode) unit).getUnit());

        return sources;
    }

    /**
     * Get all Stub Unit physical node in project tree.
     * @return list of stubs
     */
    public List<INode> getStubs() {
        List<INode> sources = Search.searchNodes(getProjectNode(), new SourcecodeFileNodeCondition());

        // Remove header file
        sources.removeIf(s -> s instanceof HeaderNode);

        // Remove uut
        List<INode> uuts = getUUTs();
        List<INode> sbfs = getSBFs();
        sources.removeIf(s -> uuts.contains(s) || sbfs.contains(s));

        List<String> libsAndTypes = ProjectClone.getLibraries();
        sources.removeIf(s -> libsAndTypes.contains(s.getAbsolutePath()));

        // Remove dont stub
        List<IEnvironmentNode> dontStubs = EnvironmentSearch
                .searchNode(getEnvironmentRootNode(), new EnviroDontStubNode());

        for (IEnvironmentNode dontStub : dontStubs)
            sources.remove(((EnviroDontStubNode) dontStub).getUnit());

        return sources;
    }

    public void setCompiler(Compiler _compiler) {
        compiler = _compiler;
    }

    public Compiler getCompiler() {
//        if (compiler == null) {
            List<IEnvironmentNode> envirCompilerNode = EnvironmentSearch
                    .searchNode(environmentRootNode, new EnviroCompilerNode());

            if (!envirCompilerNode.isEmpty()) {
                compiler = importCompiler((EnviroCompilerNode) envirCompilerNode.get(0));
            }

            List<IEnvironmentNode> libraries = EnvironmentSearch
                    .searchNode(environmentRootNode, new EnviroLibraryIncludeDirNode());

            for (IEnvironmentNode include : libraries) {
                String path = ((EnviroLibraryIncludeDirNode) include).getLibraryIncludeDir();
                compiler.getIncludePaths().add(path);
            }

            List<IEnvironmentNode> typeHandles = EnvironmentSearch
                    .searchNode(environmentRootNode, new EnviroTypeHandledSourceDirNode());

            for (IEnvironmentNode include : typeHandles) {
                String path = ((EnviroTypeHandledSourceDirNode) include).getTypeHandledSourceDir();
                compiler.getIncludePaths().add(path);
            }

            List<IEnvironmentNode> defines = EnvironmentSearch
                    .searchNode(environmentRootNode, new EnviroDefinedVariableNode());

            for (IEnvironmentNode variable : defines) {
                String definition = ((EnviroDefinedVariableNode) variable).getName();
                String value = ((EnviroDefinedVariableNode) variable).getValue();

                if (value != null && !value.isEmpty())
                    definition = definition + "=" + value;

                compiler.getDefines().add(definition);
            }
//        }

        return compiler;
    }

    public Compiler importCompiler(EnviroCompilerNode envNode) {
        Compiler compiler = new Compiler();

        String name = envNode.getName();

        for (Class<?> c : AvailableCompiler.class.getClasses()) {
            try {
                String n = c.getField("NAME").get(null).toString();

                if (name.equals(n)) {
                    compiler = new Compiler(c);
                    break;
                }

            } catch (Exception ex) {
                logger.error("Cant parse " + c.toString() + " compiler setting");
            }
        }

        compiler.setCompileCommand(envNode.getCompileCmd());
        compiler.setPreprocessCommand(envNode.getPreprocessCmd());
        compiler.setLinkCommand(envNode.getLinkCmd());
        compiler.setDebugCommand(envNode.getDebugCmd());
        compiler.setIncludeFlag(envNode.getIncludeFlag());
        compiler.setDefineFlag(envNode.getDefineFlag());
        compiler.setOutputFlag(envNode.getOutputFlag());
        compiler.setDebugFlag(envNode.getDebugFlag());
        compiler.setOutputExtension(envNode.getOutputExt());

        return compiler;
    }

    public Map<Tab, INode> getActiveSourcecodeTabs() {
        return activeSourcecodeTabs;
    }

    public void setActiveSourcecodeTabs(Map<Tab, INode> activeSourcecodeTabs) {
        this.activeSourcecodeTabs = activeSourcecodeTabs;
    }

    public static void setInstance(Environment instance) {
        Environment.instance = instance;
    }

    public static BoundOfDataTypes getBoundOfDataTypes() {
        BoundOfDataTypes boundOfDataTypes;
        String boundOfDataTypeDirectory = new WorkspaceConfig().fromJson().getBoundOfDataTypeFile();
        if (new File(boundOfDataTypeDirectory).exists()) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(BoundOfDataTypes.class, new BoundOfDataTypesDeserializer());
            Gson customGson = gsonBuilder.create();
            boundOfDataTypes = customGson.fromJson(Utils.readFileContent(boundOfDataTypeDirectory),
                    BoundOfDataTypes.class);

        } else {
            boundOfDataTypes = new BoundOfDataTypes();
            boundOfDataTypes.setBounds(boundOfDataTypes.createLP32());
            Environment.exportBoundofDataTypeToFile(boundOfDataTypes);
        }

        return boundOfDataTypes;
    }

    public static void exportBoundofDataTypeToFile(BoundOfDataTypes boundOfDataTypes) {
        String boundOfDataTypeDirectory = new WorkspaceConfig().fromJson().getBoundOfDataTypeFile();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(BoundOfDataTypes.class, new BoundOfDataTypesSerializer());
        Gson gson = builder.setPrettyPrinting().create();
        String json = gson.toJson(boundOfDataTypes, BoundOfDataTypes.class);
        Utils.writeContentToFile(json, boundOfDataTypeDirectory);
    }

    public Map<String, INode> getResolvedNodes() {
        return resolvedNodes;
    }

    public void setResolvedNodes(Map<String, INode> resolvedNodes) {
        this.resolvedNodes = resolvedNodes;
    }
}

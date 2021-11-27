package com.dse.testcase_manager;

import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.CacheHelper;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.objects.TestCasesTreeItem;
import com.dse.parser.funcdetail.FunctionDetailTree;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.INode;
import com.dse.project_init.ProjectClone;
import com.dse.testdata.DataTree;
import com.dse.testdata.object.*;
import com.dse.util.*;
import com.dse.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represent a single test case
 */
public class TestCase extends AbstractTestCase {
    private final static AkaLogger logger = AkaLogger.get(TestCase.class);

    private RootDataNode rootDataNode;
    private ICommonFunctionNode functionNode;
    // map input to expected output of global variables
    private Map<ValueDataNode, ValueDataNode> globalInputExpOutputMap = new HashMap<>();

    public TestCase(ICommonFunctionNode functionNode, String name) {
        setName(name);
        FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
        DataTree dataTree = new DataTree(functionDetailTree);
        rootDataNode = dataTree.getRoot();
        setFunctionNode(functionNode);
    }

    public TestCase(){}

    @Override
    public void setPathDefault() {
        String testcasePath = new WorkspaceConfig().fromJson().getTestcaseDirectory() + File.separator + getName() + ".json";
        setPath(testcasePath);
    }

    public Map<ValueDataNode, ValueDataNode> getGlobalInputExpOutputMap() {
        return globalInputExpOutputMap;
    }

    // is only called when create new testcase
    public void initGlobalInputExpOutputMap(RootDataNode rootDataNode, Map<ValueDataNode, ValueDataNode> globalInputExpOutputMap) {
        try {
            globalInputExpOutputMap.clear();
            FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
            DataTree dataTree = new DataTree(functionDetailTree);
            RootDataNode root = dataTree.getRoot();
//            RootDataNode root = getRootDataNode();

            // children of the global data node of the root are used to be expected output values
            RootDataNode newGlobalDataNode = getGlobalDataNode(root);
            RootDataNode globalDataNode = getGlobalDataNode(rootDataNode);
            if (newGlobalDataNode != null && globalDataNode != null) {
                mapGlobalInputToExpOutput(globalDataNode.getChildren(), newGlobalDataNode.getChildren());
                globalDataNode.setGlobalInputExpOutputMap(globalInputExpOutputMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private RootDataNode getGlobalDataNode(RootDataNode root) {
        for (IDataNode uut : root.getChildren()) {
            if (uut instanceof UnitUnderTestNode) {
                for (IDataNode dataNode : uut.getChildren()) {
                    if (dataNode instanceof RootDataNode && ((RootDataNode) dataNode).getLevel().equals(NodeType.GLOBAL)) {
                        return (RootDataNode) dataNode;
                    }
                }
            }
        }
        return null;
    }

    private void mapGlobalInputToExpOutput(List<IDataNode> inputs, List<IDataNode> expOutputs) {
        if (inputs != null) {
            for (IDataNode inputValue : inputs) {
                for (IDataNode expectedOutput : expOutputs) {
                    if (expectedOutput.getName().equals(inputValue.getName())) {
                        ((ValueDataNode) expectedOutput).setExternel(false);
                        expectedOutput.setParent(inputValue.getParent());
                        globalInputExpOutputMap.put((ValueDataNode) inputValue, (ValueDataNode) expectedOutput);
                    }
                }
            }
        }
    }

    public void updateGlobalInputExpOutputAfterInportFromFile() {
        RootDataNode globalRootDataNode = getGlobalDataNode(rootDataNode);
        if (globalRootDataNode != null)
            globalInputExpOutputMap = globalRootDataNode.getGlobalInputExpOutputMap();
    }

    public RootDataNode getRootDataNode() {
        return rootDataNode;
    }

    public void setRootDataNode(RootDataNode rootDataNode) {
        this.rootDataNode = rootDataNode;
    }

    public void setName(String name) {
        super.setName(name);
        if (getPath() == null) {
            setPathDefault();
            setBreakpointPathDefault();
            setCurrentCoverageDefault();
            setCurrentProgressDefault();
        }
        updateTestNewNode(name);
    }

    public boolean initParameterExpectedOuputs(RootDataNode rootDataNode) {
        if (rootDataNode != null) {
            UnitUnderTestNode unitUnderTestNode = null;
            for (IDataNode dataNode : rootDataNode.getChildren()) {
                if (dataNode instanceof UnitUnderTestNode) {
                    unitUnderTestNode = (UnitUnderTestNode) dataNode;
                    break;
                }
            }

            if (unitUnderTestNode != null) {
                SubprogramNode subprogramNode = null;
                for (IDataNode dataNode1 : unitUnderTestNode.getChildren()) {
                    if (dataNode1 instanceof SubprogramNode) {
                        subprogramNode = (SubprogramNode) dataNode1;
                        break;
                    }
                }

                if (subprogramNode != null) {
                    // init parameter expected output datanodes
                    subprogramNode.initInputToExpectedOutputMap();
                    return true;
                }
            }
        }

        return false;
    }

    public String getCloneSourcecodeFilePath() {
        // find the source code file containing the tested function
        INode sourcecodeFile = Utils.getSourcecodeFile(functionNode);

        // set up path for the cloned source code file
//        int lastExtensionPos = sourcecodeFile.getAbsolutePath().lastIndexOf(".");
//        return sourcecodeFile.getAbsolutePath()
//                .substring(0, lastExtensionPos) + ITestCase.AKA_SIGNAL + getName() + sourcecodeFile.getAbsolutePath()
//                .substring(lastExtensionPos);
        return ProjectClone.getClonedFilePath(sourcecodeFile.getAbsolutePath());
    }

//    public CommandConfig generateCommands(String commandFile, String executableFilePath, boolean includeGTest) {
//        if (commandFile != null && commandFile.length() > 0 && new File(commandFile).exists()) {
//
//            CommandConfig config = new CommandConfig();
////            Map<String, String> compilationCommand = config.getCompilationCommands();
////            String originalSourcecodeFile = Utils.getSourcecodeFile(functionNode).getAbsolutePath();
//
//            Compiler compiler = Environment.getCompiler();
//
//            // modify the original commands
//            // step 1: modify the compilation command
//            String newCompileCommand = compiler.generateCompileCommand(getSourceCodeFile());
//            newCompileCommand = IGTestConstant.getGTestCommand(newCompileCommand, includeGTest);
//
//            config.getCompilationCommands().put(getSourceCodeFile(), newCompileCommand);
//
//            String outputFilePath = Compiler.getOutfilePath(getSourceCodeFile(), compiler.getOutputExtension());
////            String oldCompilation = compilationCommand.get(originalSourcecodeFile);
////            String newCompilation = oldCompilation
////                    // replace test case file path
////                    .replace(originalSourcecodeFile, getSourceCodeFile());
////
////            if (includeGTest) {
////                // include gtest library
////                if (!newCompilation.contains(IGTestConstant.COMPILE_FLAG))
////                    newCompilation += IGTestConstant.COMPILE_FLAG;
////            } else {
////                // the new compilation is modified from a version which contains google test flag
////                newCompilation = newCompilation.replace(IGTestConstant.COMPILE_FLAG, SpecialCharacter.EMPTY);
////            }
////
////            compilationCommand.clear();
////            compilationCommand.put(getSourceCodeFile(), newCompilation);
////
////            // step 2: gtest main file compile command
////            String outputFlag = Environment.getCompiler().getOutputFlag();
////            String outputExtension = Environment.getCompiler().getOutputExtension();
////
////            String gtestMainPath = new WorkspaceConfig().fromJson().getTestDriverDirectory()
////                    + File.separator + new File(getSourceCodeFile()).getName();
////            String gtestOutPath = gtestMainPath.substring(0, gtestMainPath.lastIndexOf(SpecialCharacter.DOT)) + outputExtension;
////            String gtestMainCompilation = oldCompilation
////                    .replace(originalSourcecodeFile, gtestMainPath);
////
////            gtestMainCompilation = gtestMainCompilation
////                    .substring(0, gtestMainCompilation.indexOf(outputFlag) + outputFlag.length()) + gtestOutPath;
////
////            if (includeGTest) {
////                // include gtest library
////                if (!gtestMainCompilation.contains(IGTestConstant.COMPILE_FLAG))
////                    gtestMainCompilation += IGTestConstant.COMPILE_FLAG;
////            } else {
////                // the new compilation is modified from a version which contains google test flag
////                gtestMainCompilation = gtestMainCompilation.replace(IGTestConstant.COMPILE_FLAG, SpecialCharacter.EMPTY);
////            }
////
////            compilationCommand.put(gtestMainPath, gtestMainCompilation);
//
//            // step 3: modify the linking command
////            String defaultExecutableFilePath = Utils.getRoot(functionNode).getAbsolutePath() + File.separator + "program.exe";
////
////            String outputFilePath = originalSourcecodeFile;
////            int lastDotPos = outputFilePath.lastIndexOf(SpecialCharacter.DOT);
////            outputFilePath = outputFilePath.substring(0, lastDotPos) + IGNUCompiler.EXT_O;
//
//            String newLinkingCommand = compiler.generateLinkCommand(executableFilePath, outputFilePath);
//            newLinkingCommand = IGTestConstant.getGTestCommand(newLinkingCommand, includeGTest);
//
//            config.setLinkingCommand(newLinkingCommand);
//
////            if (config.getLinkingCommand().contains(defaultExecutableFilePath)) {
//////                String oldLinkingCommand = config.getLinkingCommand();
//////
//////                String newLinkingCommand = oldLinkingCommand.substring(0, oldLinkingCommand.indexOf(' ') + 1)
//////                        + gtestOutPath + " "
//////                        + oldLinkingCommand.substring(oldLinkingCommand.indexOf(IGNUCompiler.FLG_OUTPUT))
//////                        // replace test case file path
//////                        .replace(defaultExecutableFilePath, executableFilePath);
////
////                String newLinkingCommand = Environment.getCompiler().getLinkCommand() + SpecialCharacter.SPACE
////                        + gtestOutPath + SpecialCharacter.SPACE + outputFlag + executableFilePath;
////
////                if (includeGTest)
////                    // include gtest library
////                    if (!newLinkingCommand.contains(googleTestFlag))
////                        newLinkingCommand += googleTestFlag;
////                    else{
////                        // the new compilation is modified from a version which contains google test flag
////                        newLinkingCommand = newLinkingCommand.replace(googleTestFlag, "");
////                    }
////
////                config.setLinkingCommand(newLinkingCommand);
////            } else {
////                logger.error("The " + defaultExecutableFilePath + " does not in the linking command at " + commandFile);
////            }
//
//            config.setExecutablePath(executableFilePath);
//
//            File compilationDir = new File(new WorkspaceConfig().fromJson().getTestcaseCommandsDirectory());
//            compilationDir.mkdirs();
//            String commandFileOfTestcase = compilationDir.getAbsolutePath() + File.separator + getName() + ".json";
//            config.exportToJson(new File(commandFileOfTestcase));
//            logger.debug("Export the compilation of test case to file " + commandFileOfTestcase);
//
//            this.setCommandConfigFile(commandFileOfTestcase);
//            return config;
//        } else {
//            logger.error("The root command file " + commandFile + " does not exist");
//            return null;
//        }
//    }
    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    @Override
    public void setStatus(String status) {
        super.setStatus(status);
        if (status.equals(STATUS_NA)) {
            deleteOldDataExceptValue();
        }
    }

    public void updateToTestCasesNavigatorTree() {
        ICommonFunctionNode functionNode = this.getRootDataNode().getFunctionNode();
        TestCasesTreeItem treeItem = CacheHelper.getFunctionToTreeItemMap().get(functionNode);
        treeItem.addTestNewNode(treeItem, this);

        Environment.getInstance().saveTestcasesScriptToFile();
        logger.debug("[" + Thread.currentThread().getName()+"] " + "Append the name of testcase " + this.getName() + " to " + Environment.getInstance().getTestcaseScriptRootNode().getAbsolutePath());
    }

    @Override
    protected String generateDefinitionCompileCmd() {
        String defineName = getName().toUpperCase()
                .replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);

        return String.format("-DAKA_TC_%s", defineName);
    }
}

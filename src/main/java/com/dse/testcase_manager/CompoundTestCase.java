package com.dse.testcase_manager;

import com.dse.config.WorkspaceConfig;
import com.dse.util.AkaLogger;
import com.dse.util.SpecialCharacter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a compound test case
 */
public class CompoundTestCase extends AbstractTestCase {
    private final static AkaLogger logger = AkaLogger.get(CompoundTestCase.class);

    private List<TestCaseSlot> slots = new ArrayList<>();

    // this constructor is used for development
    public CompoundTestCase() {
    }

    public CompoundTestCase(String name) {
        setName(name);
    }

    public void setName(String name) {
        super.setName(name);
        if (getPath() == null)
            setPathDefault();
        updateTestNewNode(name);
    }

    @Override
    public void setPathDefault() {
        String testcasePath = new WorkspaceConfig().fromJson().getCompoundTestcaseDirectory() + File.separator + getName() + ".json";
        setPath(testcasePath);
    }

    public List<TestCaseSlot> getSlots() {
        return slots;
    }

    // for development
    public void setNameAndPath(String name, String path) {
        super.setName(name);
        setPath(path);
    }

    public void changeOrder(int source, int target) {
        TestCaseSlot slotSource = getSlotBySlotNumber(source);
        if (slotSource != null) {
            if (source < target) { // move down
                for (TestCaseSlot slot : getSlots()) {
                    int num = slot.getSlotNum();
                    if (num > source && num <= target) {
                        slot.setSlotNum(num - 1);
                    }
                    slotSource.setSlotNum(target);
                }
            } else if (source > target) { // move up
                for (TestCaseSlot slot : getSlots()) {
                    int num = slot.getSlotNum();
                    if (num < source && num >= target) {
                        slot.setSlotNum(num + 1);
                    }
                    slotSource.setSlotNum(target);
                }
            }
        }
    }

    private TestCaseSlot getSlotBySlotNumber(int num) {
        for (TestCaseSlot slot: slots) {
            if (slot.getSlotNum() == num) {
                return slot;
            }
        }
        logger.error("slot not found.");
        return null;
    }

    private void deleteSlot(TestCaseSlot slot) {
        changeOrder(slot.getSlotNum(), slots.size() - 1);
        slots.remove(slot);
    }

    public void validateSlots() {
        List<TestCaseSlot> shouldBeDelete = new ArrayList<>();
        for (TestCaseSlot slot : slots) {
            if (! slot.validate()) {
                shouldBeDelete.add(slot);
            }
        }

        for (TestCaseSlot slot : shouldBeDelete) {
            deleteSlot(slot);
        }
    }

    @Override
    protected String generateDefinitionCompileCmd() {
        StringBuilder output = new StringBuilder();

        for (TestCaseSlot slot : slots) {
            String testCaseName = slot.getTestcaseName();

            String defineName = testCaseName.toUpperCase()
                    .replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);

            String define = String.format("-DAKA_TC_%s", defineName);

            if (slots.indexOf(slot) != 0)
                output.append(SpecialCharacter.SPACE);

            output.append(define);
        }

        return output.toString();
    }

    //    @Override
//    public CommandConfig generateCommands(String commandFile, String executableFilePath, boolean includeGTest) {
//        if (commandFile != null && commandFile.length() > 0 && new File(commandFile).exists()) {
//            // load the original commands
//            CommandConfig config = new CommandConfig().fromJson(commandFile);
//
//            // modify the original commands
//            Map<String, String> compilationCommand = config.getCompilationCommands();
//            compilationCommand.clear();
//
//            if (slots.size() > 0) {
//                // step 1: get the first compilation command
//                String lastCompilation = "";
//                String lastSourceCodeFile = "";
//
//                TestCase element = TestCaseManager.getBasicTestCaseByName(slots.get(0).getTestcaseName());
//
//                if (element != null) {
//                    // load the corresponding commands of test case
////                    CommandConfig elementConfig = new CommandConfig().fromJson(element.getCommandConfigFile());
////                    String elementCompilationCommand = elementConfig.getCompilationCommands()
////                            .get(element.getSourceCodeFile());
//////                    compilationCommand.put(element.getSourceCodeFile(), elementCompilationCommand);
////
////                    if (includeGTest) {
////                        // include gtest library
////                        if (!elementCompilationCommand.contains(googleTestFlag))
////                            elementCompilationCommand += googleTestFlag;
////                    } else {
////                        // the new compilation is modified from a version which contains google test flag
////                        elementCompilationCommand = elementCompilationCommand.replace(googleTestFlag, "");
////                    }
//                    CommandConfig elementConfig = element.generateCommands(commandFile,
//                            element.getExecutableFile(), includeGTest);
//
//                    lastSourceCodeFile = element.getSourceCodeFile();
//                    lastCompilation = elementConfig.getCompilationCommands().get(lastSourceCodeFile);
//                }
//
//                // step 2: gtest main file compile command
//                String gtestMainPath = new WorkspaceConfig().fromJson().getTestDriverDirectory()
//                        + File.separator + new File(getSourceCodeFile()).getName();
//                String gtestOutPath = gtestMainPath
//                        .substring(0, gtestMainPath.lastIndexOf(SpecialCharacter.DOT)) + IGNUCompiler.EXT_O;
//                String gtestMainCompilation = lastCompilation.replace(lastSourceCodeFile, gtestMainPath);
//                gtestMainCompilation = gtestMainCompilation
//                        .substring(0, gtestMainCompilation.indexOf(IGNUCompiler.FLG_OUTPUT) + 3) + gtestOutPath;
//
//                if (includeGTest) {
//                    // include gtest library
//                    if (!gtestMainCompilation.contains(googleTestFlag))
//                        gtestMainCompilation += googleTestFlag;
//                } else {
//                    // the new compilation is modified from a version which contains google test flag
//                    gtestMainCompilation = gtestMainCompilation.replace(googleTestFlag, "");
//                }
//
//                compilationCommand.put(gtestMainPath, gtestMainCompilation);
//
//                // step 3: modify the linking command
//                String defaultExecutableFilePath = "program.exe";
//
//                String oldLinkingCommand = config.getLinkingCommand();
//                if (oldLinkingCommand.contains(defaultExecutableFilePath)) {
//
//                    String newLinkingCommand = oldLinkingCommand.substring(0, oldLinkingCommand.indexOf(' ') + 1)
//                            + gtestOutPath + " " + IGNUCompiler.FLG_OUTPUT + " " + executableFilePath;
//
//                    if (includeGTest)
//                        // include gtest library
//                        if (!newLinkingCommand.contains(googleTestFlag))
//                            newLinkingCommand += googleTestFlag;
//                        else {
//                            // the new compilation is modified from a version which contains google test flag
//                            newLinkingCommand = newLinkingCommand.replace(googleTestFlag, "");
//                        }
//
//                    config.setLinkingCommand(newLinkingCommand);
//                } else {
//                    logger.error("The default executable file path" + " does not in the linking command at " + commandFile);
//                }
//
//                config.setExecutablePath(executableFilePath);
//            } else {
//                config.setExecutablePath("");
//                config.setLinkingCommand("");
//            }
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
}

package com.dse.project_init;

import auto_testcase_generation.instrument.AbstractFunctionInstrumentation;
import auto_testcase_generation.instrument.FunctionInstrumentationForAllCoverages;
import auto_testcase_generation.instrument.FunctionInstrumentationForMacro;
import com.dse.config.AkaConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.EnvironmentSearch;
import com.dse.environment.WorkspaceCreation;
import com.dse.environment.object.*;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.ProjectParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.dependency.FunctionCallDependency;
import com.dse.parser.dependency.IncludeHeaderDependency;
import com.dse.parser.dependency.IncludeHeaderDependencyGeneration;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.SearchCondition;
import com.dse.search.condition.*;
import com.dse.stub_manager.StubManager;
import com.dse.stub_manager.SystemLibrary;
import com.dse.util.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.eclipse.cdt.core.dom.ast.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProjectClone {
    private static final AkaLogger logger = AkaLogger.get(ProjectNode.class);

    public static final String CLONED_FILE_EXTENSION = ".akaignore";
    public static final String MAIN_REFACTOR_NAME = "AKA_MAIN";
    private static List<String> libraries;

    public static void main(String[] args){
        ProjectParser projectParser = new ProjectParser(new File("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/macro"));
        projectParser.setExpandTreeuptoMethodLevel_enabled(true);
        ProjectNode projectRoot = projectParser.getRootTree();
        Environment.getInstance().setProjectNode(projectRoot);
        Environment.getInstance().getProjectRoots().add(projectRoot);

        EnvironmentAnalyzer analyzer = new EnvironmentAnalyzer();
        analyzer.analyze(new File("/Users/ducanhnguyen/Documents/akautauto/local/working-directory/demo.env"));
        IEnvironmentNode root = analyzer.getRoot();
        Environment.getInstance().setEnvironmentRootNode((EnvironmentRootNode) root);

        ProjectClone.cloneEnvironment();
    }
    /**
     * Clone all source code file with extension in project directories.
     */
    public static void cloneEnvironment() {
        List<ProjectNode> projectNodes = Environment.getInstance().getProjectRoots();

        if (projectNodes == null || projectNodes.isEmpty())
            projectNodes = Collections.singletonList(Environment.getInstance().getProjectNode());

        List<IEnvironmentNode> stubLibraries = EnvironmentSearch
                .searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroLibraryStubNode());

        List<INode> stubUnits = Environment.getInstance().getStubs();
        List<INode> sbfUnits = Environment.getInstance().getSBFs();
        List<INode> uutUnits = Environment.getInstance().getUUTs();

        List<String> libraries = getLibraries();

        boolean onWhiteBoxMode = Environment.getInstance().isOnWhiteBoxMode();

        for (ProjectNode projectRoot : projectNodes) {
            List<INode> sources = Search.searchNodes(projectRoot, new SourcecodeFileNodeCondition());
            sources.removeIf(source -> libraries.contains(source.getAbsolutePath()));

            ProjectClone clone = new ProjectClone();

            for (INode sourceCode : sources) {
                boolean isWhiteBoxOnUnit = (uutUnits.contains(sourceCode) || sbfUnits.contains(sourceCode)) && onWhiteBoxMode;
                boolean isStubByUnit = stubUnits.contains(sourceCode) || sbfUnits.contains(sourceCode);

                String newContent = clone.generateFileContent(sourceCode, isWhiteBoxOnUnit, stubLibraries, isStubByUnit, libraries);

                Utils.writeContentToFile(newContent, getClonedFilePath(sourceCode.getAbsolutePath()));
            }
        }
    }

    public static List<String> getLibraries() {
        if (libraries == null) {
            libraries = new ArrayList<>();

            List<IEnvironmentNode> libs = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroLibraryIncludeDirNode());
            libs.forEach(lib -> {
                String path = ((EnviroLibraryIncludeDirNode) lib).getLibraryIncludeDir();
                File[] files = new File(path).listFiles();
                if (files != null) {
                    for (File file : files) {
                        libraries.add(file.getAbsolutePath());
                    }
                }
            });
            List<IEnvironmentNode> types = EnvironmentSearch.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroTypeHandledSourceDirNode());
            types.forEach(lib -> {
                String path = ((EnviroTypeHandledSourceDirNode) lib).getTypeHandledSourceDir();
                File[] files = new File(path).listFiles();
                if (files != null) {
                    for (File file : files) {
                        libraries.add(file.getAbsolutePath());
                    }
                }
            });
        }

        return libraries;
    }

    public static void cloneASourceCodeFile(INode sourceCode){
        if (!(sourceCode instanceof SourcecodeFileNode))
            return;
        List<IEnvironmentNode> stubLibraries = EnvironmentSearch
                .searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroLibraryStubNode());

        List<INode> stubUnits = Environment.getInstance().getStubs();
        List<INode> sbfUnits = Environment.getInstance().getSBFs();
        List<INode> uutUnits = Environment.getInstance().getUUTs();

        ProjectClone clone = new ProjectClone();

        List<String> libraries = getLibraries();

        boolean onWhiteBoxMode = Environment.getInstance().isOnWhiteBoxMode();
        boolean isWhiteBoxOnUnit = (uutUnits.contains(sourceCode) || sbfUnits.contains(sourceCode)) && onWhiteBoxMode;
        boolean isStubByUnit = stubUnits.contains(sourceCode) || sbfUnits.contains(sourceCode);

        String newContent = clone.generateFileContent(sourceCode, isWhiteBoxOnUnit, stubLibraries, isStubByUnit, libraries);

        Utils.writeContentToFile(newContent, getClonedFilePath(sourceCode.getAbsolutePath()));
    }

    /**
     * Remove all cloned file with extension ".akaignore" in project directories.
     */
    public static void removeClonedFile() {
        List<ProjectNode> projectNodes = Environment.getInstance().getProjectRoots();

        if (projectNodes == null)
            projectNodes = Collections.singletonList(Environment.getInstance().getProjectNode());

        for (ProjectNode projectRoot : projectNodes) {
            List<INode> sources = Search.searchNodes(projectRoot, new SourcecodeFileNodeCondition());

            for (INode sourceCode : sources) {
                String clonedFilePath = getClonedFilePath(sourceCode.getAbsolutePath());
                File clonedFile = new File(clonedFilePath);
                if (clonedFile.exists())
                    Utils.deleteFileOrFolder(clonedFile);
            }
        }
    }

    /**
     * Change all private and protected labels in source code to public
     */
    private static String refactorWhiteBox(String oldContent) {
        oldContent = oldContent.replaceAll("\\bprivate\\b", "public");
        oldContent = oldContent.replaceAll("\\bprotected\\b", "public");
        oldContent = oldContent.replaceAll("\\bstatic \\b", SpecialCharacter.EMPTY);

        return oldContent;
    }

    /**
     * remove all mark function in source code
     */
    public static String simplify(String source) {
        String simplify = source.replaceAll("\\bAKA_MARK\\(\"[^\"]+\"\\);", SpecialCharacter.EMPTY);
        simplify = simplify.replaceAll("\\bAKA_MARK\\(\"[^\"]+\"\\)\\s*&&\\s*", SpecialCharacter.EMPTY);
        simplify = simplify.replaceAll("#include \".+probepoints", "<~ \"");
        simplify = simplify.replace("AKA_FCALLS++;", SpecialCharacter.EMPTY);
        return simplify;
    }

    /**
     * Generate clone file content
     *
     * @param sourceCode origin source code file content
     * @return cloned file content
     */
    private String generateFileContent(INode sourceCode, boolean whiteBox, List<IEnvironmentNode> stubLibraries, boolean stubable, List<String> libraries) {
        String oldContent = Utils.readFileContent(sourceCode.getAbsolutePath());

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new IncludeHeaderNodeCondition());
        conditions.add(new GlobalVariableNodeCondition());
        conditions.add(new AbstractFunctionNodeCondition());
        conditions.add(new MacroFunctionNodeCondition());
        conditions.add(new DefinitionFunctionNodeCondition());

        List<INode> redefines = Search.searchNodes(sourceCode, conditions);

        List<String> globalDeclarations = new ArrayList<>();

        for (INode child : redefines) {
            logger.debug("Clone & instrument " + child.getAbsolutePath());

            if (child instanceof IncludeHeaderNode) {
                oldContent = refactorInclude(oldContent, (IncludeHeaderNode) child, sourceCode, stubLibraries, libraries);

            } else if (child instanceof ExternalVariableNode)
                oldContent = guardGlobalDeclaration(oldContent, child, globalDeclarations);
            else if (child instanceof ICommonFunctionNode) {
                ICommonFunctionNode function = (ICommonFunctionNode) child;
                oldContent = refactorFunction(oldContent, function, stubLibraries, stubable);
            }
        }

        for (String globalDeclaration : globalDeclarations)
            oldContent = oldContent.replace("#endif\n\n" + globalDeclaration, "#endif");

        if (whiteBox)
            oldContent = refactorWhiteBox(oldContent);

        String defineSourceCodeName = IGTestConstant.SRC_PREFIX + sourceCode.getAbsolutePath().toUpperCase()
                .replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);

        return wrapInIncludeGuard(defineSourceCodeName, oldContent);
    }

    /**
     * Refactor include to clone akaignore file and stub libraries source files.
     *
     * @param oldContent    of current source code
     * @param includeHeader node
     * @param sourceCode    node
     * @param stubLibraries chosen by user
     * @return new source code file content
     */
    private String refactorInclude(String oldContent, IncludeHeaderNode includeHeader,
                                   INode sourceCode, List<IEnvironmentNode> stubLibraries, List<String> libraries) {

        // Guard all include statement in source code
        oldContent = guardIncludeHeader(oldContent, includeHeader);

        // find header node from include header statement
        List<INode> headerNodes = new IncludeHeaderDependencyGeneration().findIncludeNodes(includeHeader, sourceCode);
        headerNodes.removeIf(headerNode -> libraries.contains(headerNode.getAbsolutePath()));

        // get prev include statement
        String oldIncludeStatement = includeHeader.getAST().getRawSignature();

        // Modify include dependency to clone akaignore file
        if (!headerNodes.isEmpty()) {
            INode headerNode = headerNodes.get(0);
            IncludeHeaderDependency d = new IncludeHeaderDependency(sourceCode, headerNode);
            if (sourceCode.getDependencies().contains(d)) {
                String clonedFilePath = getClonedFilePath(headerNode.getAbsolutePath());
                Path parentDirPath = Paths.get(sourceCode.getAbsolutePath()).getParent();
                Path clonedPath = Paths.get(clonedFilePath);
                Path relativePath = parentDirPath.relativize(clonedPath);
                String newIncludeStatement = oldIncludeStatement.replace(includeHeader.getNewType(), relativePath.toString());
                oldContent = oldContent.replace(oldIncludeStatement, newIncludeStatement);
            }
        }
        // STUB LIBRARY case
        else {
            String library = includeHeader.getNewType();

            for (IEnvironmentNode stubLibrary : stubLibraries) {

                if (((EnviroLibraryStubNode) stubLibrary).getLibraries().containsValue(library)) {

                    String clonedFilePath = SystemLibrary.getLibrariesDirectory() + library + SystemLibrary.LIBRARY_EXTENSION;
                    String newIncludeStatement = oldIncludeStatement.replace(includeHeader.getNewType(), clonedFilePath);

                    oldContent = oldContent.replace(oldIncludeStatement, newIncludeStatement);
                }
            }
        }

        return oldContent;
    }

    /**
     * Refactor function content
     *
     * @param oldContent    of souce code file
     * @param function      node
     * @param stubLibraries all libraries stubed by user
     * @return new source code file content
     */
    private String refactorFunction(String oldContent, ICommonFunctionNode function, List<IEnvironmentNode> stubLibraries, boolean stubable) {
        if (function instanceof AbstractFunctionNode || function instanceof MacroFunctionNode) {
            IASTNode functionAST = null;
            if (function instanceof AbstractFunctionNode) {
                functionAST = ((AbstractFunctionNode) function).getAST();
            } else if (function instanceof MacroFunctionNode){
                functionAST = ((MacroFunctionNode) function).getAST();
            }
            String oldFunctionCode = functionAST.getRawSignature();

            // generate instrumented function content
            String newFunctionCode = "";
            if (function instanceof MacroFunctionNode)
                newFunctionCode = generateInstrumentedFunction((MacroFunctionNode) function);
            else if (function instanceof AbstractFunctionNode)
                newFunctionCode = generateInstrumentedFunction((IFunctionNode) function);

            // include stub code in function scope
            if (function instanceof FunctionNode && stubable) {
                newFunctionCode = includeStubFile(newFunctionCode, (FunctionNode) function);
                StubManager.initializeStubCode((FunctionNode) function);
            }

            // modify all function call expression to stub libraries
            for (IEnvironmentNode stubLibrary : stubLibraries) {
                if (stubLibrary instanceof EnviroLibraryStubNode) {
                    Map<String, String> libraries = ((EnviroLibraryStubNode) stubLibrary).getLibraries();

                    for (String libraryName : libraries.keySet()) {
                        if (SystemLibrary.isUseLibrary(libraryName, function)) {
                            String newLibraryCall = IGTestConstant.STUB_PREFIX + libraryName;

                            newFunctionCode = newFunctionCode
                                    .replaceAll("\\b" + Pattern.quote(VariableTypeUtils.STD_SCOPE + libraryName) + "\\b", newLibraryCall);

                            newFunctionCode = newFunctionCode
                                    .replaceAll("\\b" + Pattern.quote(libraryName) + "\\b", newLibraryCall);

                            // update function call dependency
                            if (newFunctionCode.contains(newLibraryCall)) {
                                List<INode> possibles = Search.searchNodes(
                                        Environment.getInstance().getSystemLibraryRoot(),
                                        new FunctionNodeCondition()
                                );

                                possibles.removeIf(f -> !((IFunctionNode) f).getSimpleName().equals(newLibraryCall));

                                if (!possibles.isEmpty()) {
                                    INode libraryFunction = possibles.get(0);
                                    Dependency d = new FunctionCallDependency(function, libraryFunction);

                                    if (!function.getDependencies().contains(d)
                                            && !libraryFunction.getDependencies().contains(d)) {
                                        function.getDependencies().add(d);
                                        libraryFunction.getDependencies().add(d);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // refactor
            if (oldContent.startsWith(oldFunctionCode))
                oldContent = oldContent.replace(oldFunctionCode, newFunctionCode);
            else if (oldContent.startsWith(" " + oldFunctionCode))
                oldContent = oldContent.replace(" " + oldFunctionCode, " " + newFunctionCode);
            else if (oldContent.contains("\n" + oldFunctionCode))
                oldContent = oldContent.replace("\n" + oldFunctionCode, "\n" + newFunctionCode);
            else if (oldContent.contains("\t" + oldFunctionCode))
                oldContent = oldContent.replace("\t" + oldFunctionCode, "\t" + newFunctionCode);
            else
                oldContent = oldContent.replace(oldFunctionCode, newFunctionCode);
        }

        // change main function to AKA_MAIN
        if (function.getSingleSimpleName().equals("main"))
            oldContent = refactorMain(oldContent, function);

        return oldContent;
    }

    /**
     * Ex: int main() -> int AKA_MAIN()
     *
     * @param oldContent of current source code file
     * @param main       function node
     * @return new source code file
     */
    private String refactorMain(String oldContent, ICommonFunctionNode main) {
        String oldMainPrototype = "";

        if (main instanceof DefinitionFunctionNode) {
            oldMainPrototype = ((DefinitionFunctionNode) main).getAST().getRawSignature();
        } else if (main instanceof FunctionNode) {
            oldMainPrototype = ((FunctionNode) main).getAST().getRawSignature();
            int openBrace = oldMainPrototype.indexOf(SpecialCharacter.OPEN_BRACE);
            oldMainPrototype = oldMainPrototype.substring(0, openBrace).trim();
        }

        String newMainPrototype = oldMainPrototype.replace("main", MAIN_REFACTOR_NAME);

        return oldContent.replace(oldMainPrototype, newMainPrototype);
    }

    /**
     * Ex: int foo() {
     * int x, y;
     * ...
     * return x + y;
     * }
     * To int foo() {
     * #include "Utils.foo.23.324.stub"
     * int x, y;
     * ...
     * return x + y;
     * }
     *
     * @param oldContent of function source code
     * @param function   needed to insert stub code
     * @return new function source code
     */
    private String includeStubFile(String oldContent, FunctionNode function) {
        String markFunctionStm = generateCallingMark(function.getAbsolutePath());
        int bodyBeginPos = oldContent.indexOf(SpecialCharacter.OPEN_BRACE) + markFunctionStm.length() + 1;

        String stubDirectory = new WorkspaceConfig().fromJson().getStubCodeDirectory();
        String stubFilePath = stubDirectory + (stubDirectory.endsWith(File.separator) ? "" : File.separator)
                + StubManager.getStubCodeFileName(function);

        Path parentDirPath = Paths.get(Utils.getSourcecodeFile(function).getAbsolutePath()).getParent();
        Path stubPath = Paths.get(stubFilePath);

        String relativePath = parentDirPath.relativize(stubPath).toString();

        StubManager.addStubFile(function.getAbsolutePath(), stubFilePath);

        return oldContent.substring(0, bodyBeginPos)
                + String.format("\n\t/** Include stub source code */\n\t#include \"%s\"\n", relativePath)
                + oldContent.substring(bodyBeginPos);
    }

    public static int getStartLineNumber(ICommonFunctionNode functionNode) {
        INode sourceNode = Utils.getSourcecodeFile(functionNode);

        String clonedFilePath = getClonedFilePath(sourceNode.getAbsolutePath());

        String clonedFile = Utils.readFileContent(clonedFilePath);

        if (functionNode instanceof IFunctionNode) {
            IASTFunctionDefinition functionDefinition = ((IFunctionNode) functionNode).getAST();

            String rawSource = functionDefinition.getRawSignature();

            int openIndex = rawSource.indexOf('{') + 1;

            String declaration = rawSource.substring(0, openIndex)
                    .replaceAll("\\bmain\\b", "AKA_MAIN")
                    .replaceAll("\\s+\\{","{");

            int offsetInClonedFile = clonedFile.indexOf(declaration);

            return (int) clonedFile
                    .substring(0, offsetInClonedFile)
                    .chars()
                    .filter(c -> c == '\n')
                    .count();
        } else if (functionNode instanceof MacroFunctionNode) {
            IASTNode ast = ((MacroFunctionNode) functionNode).getAST();
            return ast.getFileLocation().getStartingLineNumber();

        } else if (functionNode instanceof DefinitionFunctionNode) {
            IASTNode ast = ((DefinitionFunctionNode) functionNode).getAST();
            return ast.getFileLocation().getStartingLineNumber();

        }

        return 0;
    }

    /**
     * Ex: Utils.cpp -> Utils.akaignore.cpp
     *
     * @param origin file path
     * @return cloned file path
     */
    public static String getClonedFilePath(String origin) {
        String originName = new File(origin).getName();

        int lastDotPos = originName.lastIndexOf(SpecialCharacter.DOT);

        String clonedName = Environment.getInstance().getName() + SpecialCharacter.DOT
                + originName.substring(0, lastDotPos) + CLONED_FILE_EXTENSION + originName.substring(lastDotPos);

        return origin.replace(originName, clonedName);
    }

    /**
     * Ex: #include "class.h"
     * To #ifdef AKA_INCLUDE_CLASS_H
     * #define AKA_INCLUDE_CLASS_H
     * #include "class.h"
     * #endif
     *
     * @param oldContent of source code
     * @param child      corresponding external variable node
     * @return new guarded include statement
     */
    private String guardIncludeHeader(String oldContent, INode child) {
        if (child instanceof IncludeHeaderNode) {
            String oldIncludeHeader = ((IncludeHeaderNode) child).getAST().getRawSignature();

            oldContent = oldContent
                    .replace(oldIncludeHeader.replace(" ", ""), oldIncludeHeader);

            String header = child.getName().replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE).toUpperCase();

            String newIncludeHeader = wrapInIncludeGuard(IGTestConstant.INCLUDE_PREFIX + header, oldIncludeHeader);

            oldContent = oldContent.replace(oldIncludeHeader, newIncludeHeader);
        }

        return oldContent;
    }

    /**
     * Ex: int x;
     * To #ifdef AKA_GLOBAL_X
     * #define AKA_GLOBAL_X
     * int x;
     * #endif
     *
     * @param oldContent   of source code
     * @param child        corresponding external variable node
     * @param declarations of external variable node
     * @return new declaration of external variable node
     */
    private String guardGlobalDeclaration(String oldContent, INode child, List<String> declarations) {
        IASTNodeLocation[] tempAstLocations = ((ExternalVariableNode) child).getASTType().getNodeLocations();
        if (tempAstLocations.length > 0) {
            IASTNodeLocation astNodeLocation = tempAstLocations[0];
            if (astNodeLocation instanceof IASTCopyLocation) {
                IASTNode declaration = ((IASTCopyLocation) astNodeLocation).getOriginalNode().getParent();
                if (declaration instanceof IASTDeclaration) {
                    String originDeclaration = declaration.getRawSignature();

                    if (!declarations.contains(originDeclaration))
                        declarations.add(originDeclaration);

                    String oldDeclaration = ((ExternalVariableNode) child).getASTType().getRawSignature() + " "
                            + ((ExternalVariableNode) child).getASTDecName().getRawSignature() + ";";

                    String header = child.getAbsolutePath();

                    if (header.startsWith(File.separator))
                        header = header.substring(1);

                    header = header.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE).toUpperCase();

                    String newDeclaration = wrapInIncludeGuard(IGTestConstant.GLOBAL_PREFIX + header, oldDeclaration);

                    oldContent = oldContent.replace(originDeclaration, newDeclaration + "\n" + originDeclaration);
                }
            }
        }

        return oldContent;
    }

    /**
     * Ex: int x;
     * To #ifdef AKA_GLOBAL_X
     * #define AKA_GLOBAL_X
     * int x;
     * #endif
     *
     * @param name    define name
     * @param content needed to be guard
     * @return new source code
     */
    public static String wrapInIncludeGuard(String name, String content) {
        return String.format("/** Guard statement to avoid multiple declaration */\n" +
                "#ifndef %s\n#define %s\n%s\n#endif\n", name, name, content);
    }

    private String generateInstrumentedFunction(MacroFunctionNode functionNode) {
        final String success = String.format("/** Instrumented function %s */\n", functionNode.getName());
        final String fail = String.format("/** Can not instrument function %s */\n", functionNode.getName());

        String instrumentedSourceCode;

        AbstractFunctionInstrumentation fnInstrumentation =
                new FunctionInstrumentationForMacro(((MacroFunctionNode) functionNode).getAST());

        if (fnInstrumentation == null)
            instrumentedSourceCode = "";
        else {
            fnInstrumentation.setFunctionPath(functionNode.getAbsolutePath());

//            String instrument = fnInstrumentation.generateInstrumentedFunction(); // just for release
            String instrument = "";
            if (instrument == null || instrument.length() == 0){
                // can not instrument
                instrumentedSourceCode = fail + functionNode.getAST().getRawSignature();
            } else {
                instrumentedSourceCode = success + instrument;
            }
        }

        return instrumentedSourceCode;
    }

    /**
     * Perform on instrumentation on the original function
     */
    private String generateInstrumentedFunction(IFunctionNode functionNode) {
        final String success = String.format("/** Instrumented function %s */\n", functionNode.getName());
        final String fail = String.format("/** Can not instrument function %s */\n", functionNode.getName());

        String instrumentedSourceCode;

        IASTNode astInstrumentedFunction = functionNode.getAST();
        AbstractFunctionInstrumentation fnInstrumentation = new FunctionInstrumentationForAllCoverages(
                (IASTFunctionDefinition) astInstrumentedFunction, functionNode);

        if (fnInstrumentation == null)
            instrumentedSourceCode = "";
        else {
            fnInstrumentation.setFunctionPath(functionNode.getAbsolutePath());

            String instrument = fnInstrumentation.generateInstrumentedFunction();
            if (instrument == null || instrument.length() == 0){
                // can not instrument
                instrumentedSourceCode = fail + functionNode.getAST().getRawSignature();
            } else {
                instrumentedSourceCode = success + instrument;
                int bodyIdx = instrumentedSourceCode.indexOf(SpecialCharacter.OPEN_BRACE) + 1;

                instrumentedSourceCode = instrumentedSourceCode.substring(0, bodyIdx)
                        + generateCallingMark(functionNode.getAbsolutePath()) // insert mark start function
                        + instrumentedSourceCode.substring(bodyIdx);
            }
        }


        return instrumentedSourceCode;
    }
//    /**
//     * Perform on instrumentation on the original function
//     */
//    private String generateInstrumentedFunction(ICommonFunctionNode functionNode) {
//        String instrumentedSourceCode = "";
//        AbstractFunctionInstrumentation fnInstrumentation = null;
//
//        if (functionNode instanceof AbstractFunctionNode) {
//            IASTNode astInstrumentedFunction = ((AbstractFunctionNode) functionNode).getAST();
//            fnInstrumentation = new FunctionInstrumentationForAllCoverages((IASTFunctionDefinition) astInstrumentedFunction,
//                    astInstrumentedFunction.getFileLocation().getStartingLineNumber(),
//                    astInstrumentedFunction.getFileLocation().getNodeOffset());
//        } else if (functionNode instanceof MacroFunctionNode) {
//            fnInstrumentation = new FunctionInstrumentationForMacro(((MacroFunctionNode) functionNode).getAST());
//        }
//
//        if (fnInstrumentation == null) {
//            final String comment = String.format("/** Can not instrument function %s */\n", functionNode.getName());
//            instrumentedSourceCode = comment + ((CustomASTNode) functionNode).getAST().getRawSignature();
//        } else {
//            fnInstrumentation.setFunctionPath(functionNode.getAbsolutePath());
//            String instrument = fnInstrumentation.generateInstrumentedFunction();
//
//            if (instrument != null && instrument.length() > 0) {
//                final String comment = String.format("/** Instrumented function %s */\n", functionNode.getName());
//                instrumentedSourceCode = comment + instrument;
//                int bodyIdx = instrumentedSourceCode.indexOf(SpecialCharacter.OPEN_BRACE) + 1;
//
//                instrumentedSourceCode = instrumentedSourceCode.substring(0, bodyIdx)
//                        + generateCallingMark(functionNode.getAbsolutePath()) // insert mark start function
//                        + instrumentedSourceCode.substring(bodyIdx);
//            } else {
//                // can not generate instrumentation
//                final String comment = String.format("/** Can not instrument function %s */\n", functionNode.getName());
//                instrumentedSourceCode = comment + ((CustomASTNode) functionNode).getAST().getRawSignature();
//            }
//        }
//
//        return instrumentedSourceCode;
//    }

    public static String generateCallingMark(String content) {
        content = PathUtils.toRelative(content);
        content = Utils.doubleNormalizePath(content);
        return String.format("AKA_MARK(\"Calling: %s\");AKA_FCALLS++;", content);
    }

    public static class CloneThread extends Task<Void> {
//        @Override
//        public void run() {
//            // stub system libraries
//            SystemLibrary.generateStubCode();
//            SystemLibraryRoot libraryRoot = SystemLibrary.parseFromFile();
//            Environment.getInstance().setSystemLibraryRoot(libraryRoot);
//
//            // clone tested source code
//            ProjectClone.cloneEnvironment();
//
//            StubManager.exportListToFile();
//
//            // create workspace
//            WorkspaceCreation wk = new WorkspaceCreation();
//            wk.setWorkspace(new AkaConfig().fromJson().getOpeningWorkspaceDirectory());
//            wk.setDependenciesFolder(new WorkspaceConfig().fromJson().getDependencyDirectory());
//            wk.setElementFolder(new WorkspaceConfig().fromJson().getElementDirectory());
//            wk.setPhysicalTreePath(new WorkspaceConfig().fromJson().getPhysicalJsonFile());
//            wk.setRoot(Environment.getInstance().getProjectNode());
//            wk.create(wk.getRoot(), wk.getElementFolder(), wk.getDependenciesFolder(), wk.getPhysicalTreePath());
//        }

        @Override
        protected Void call() throws Exception {
            // stub system libraries
            SystemLibrary.generateStubCode();
            SystemLibraryRoot libraryRoot = SystemLibrary.parseFromFile();
            Environment.getInstance().setSystemLibraryRoot(libraryRoot);

            // clone tested source code
            ProjectClone.cloneEnvironment();

            StubManager.exportListToFile();

            return null;
        }
    }
}

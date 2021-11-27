package com.dse.guifx_v3.helps;

import com.dse.config.Paths;
import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.controllers.main_view.LeftPaneController;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.IProcessNotify;
import com.dse.parser.object.IProjectNode;
import com.dse.thread.AbstractAkaTask;
import com.dse.util.Utils;
import javafx.application.Platform;
import com.dse.util.AkaLogger;

import java.io.File;

public class ProjectLoadThread extends AbstractAkaTask<IProjectNode> {
    final static AkaLogger logger = AkaLogger.get(ProjectLoadThread.class);
    private File path;
    private UILogger uiLogger = UILogger.getUiLogger();
    public ProjectLoadThread(File path) {
        this.path = path;
    }

    @Override
    protected IProjectNode call() throws Exception {
        logger.debug("Saving the path of project to configuration");
        Paths.CURRENT_PROJECT.ORIGINAL_PROJECT_PATH = path.getAbsolutePath();
        new WorkspaceConfig().fromJson().setTestingProject(path.getAbsolutePath());

        // Get type of the clone project
        Paths.CURRENT_PROJECT.TYPE_OF_PROJECT = Utils.getTypeOfProject(Paths.CURRENT_PROJECT.ORIGINAL_PROJECT_PATH);
        uiLogger.log("Type of project = " + Paths.CURRENT_PROJECT.TYPE_OF_PROJECT);
        logger.debug("Type of project = " + Paths.CURRENT_PROJECT.TYPE_OF_PROJECT);

        // Prepare the parsing project
        boolean shouldParseTheOriginalProject = UIController.shouldParseTheOriginalProject(Paths.CURRENT_PROJECT.TYPE_OF_PROJECT);
        File parsingProject;
        if (shouldParseTheOriginalProject) {
            // we must parse on the original project
            // If we need to generate test data automatically, we must use the original project
            // TODO: any idea to resolve this problem.
            parsingProject = new File(Paths.CURRENT_PROJECT.ORIGINAL_PROJECT_PATH);
        } else {
            // Create a clone project
            uiLogger.log("Creating a clone project");
            logger.debug("Creating a clone project");
            Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH = UIController.createCloneProject(path.getAbsolutePath())
                    .getAbsolutePath();
            uiLogger.log("Created! Path of the clone project = " + Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH);
            logger.debug("Created! Path of the clone project = " + Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH);
            parsingProject = new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH);
        }

        // Parse the project
        if (parsingProject != null) {
            uiLogger.log("Start parsing project");
            logger.debug("Start parsing project");
            ProjectParser projectParser = new ProjectParser(parsingProject);

            //Run when the thread is done
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    // Display the tree of project
                    projectParser.setExpandTreeuptoMethodLevel_enabled(true);
                    projectParser.setCpptoHeaderDependencyGeneration_enabled(true);
                    projectParser.setParentReconstructor_enabled(false);
                    projectParser.setExtendedDependencyGeneration_enabled(false);
                    projectParser.setFuncCallDependencyGeneration_enabled(false);
                    projectParser.setGenerateSetterandGetter_enabled(false);
                    projectParser.setGlobalVarDependencyGeneration_enabled(false);
                    IProjectNode current_project_root = projectParser.getRootTree();
                    uiLogger.log("Finish parsing project");
                    logger.debug("Finish parsing project");

                    if (current_project_root != null) {
                        uiLogger.log("Start rendering on project structure tree");
                        logger.debug("Start rendering on project structure tree");
                        LeftPaneController.getLeftPaneController().renderProjectTree(current_project_root);
                        uiLogger.log("Finish rendering on project structure tree");
                        logger.debug("Finish rendering on project structure tree");
                    }
                }
            });
        } else {
            logger.debug("Can not parse the project " + Paths.CURRENT_PROJECT.ORIGINAL_PROJECT_PATH);
        }
        return null;
    }

    // ! Chưa biết dùng notify
    private IProcessNotify notify = new IProcessNotify() {

        @Override
        public void notify(int status) {
        }

        @Override
        public void notify(String message) {
        }
    };
}

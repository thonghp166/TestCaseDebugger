package com.dse.cli;

import com.dse.config.AkaConfig;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.EnvironmentAnalyzer;
import com.dse.environment.WorkspaceLoader;
import com.dse.environment.object.EnvironmentRootNode;
import com.dse.environment.object.IEnvironmentNode;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.INode;
import com.dse.regression.ChangesBetweenSourcecodeFiles;
import com.dse.regression.WorkspaceUpdater;
import com.dse.testcase_manager.TestCaseManager;

import java.io.File;

public class CliEnvironmentLoader {
    public static void openEnvironment(File scriptFile) throws Exception {
        // analyze environment script file
        EnvironmentAnalyzer analyzer = new EnvironmentAnalyzer();
        analyzer.analyze(scriptFile);
        IEnvironmentNode root = analyzer.getRoot();

        if (root instanceof EnvironmentRootNode) {
            Environment.getInstance().setEnvironmentRootNode((EnvironmentRootNode) root);

            // update config at application level
            AkaConfig akaConfig = new AkaConfig().fromJson();
            String wd = akaConfig.getWorkingDirectory();

            // update workspace directory
            String workspaceDirectory = wd + File.separator + Environment.getInstance().getName();
            akaConfig.setOpeningWorkspaceDirectory(workspaceDirectory);

            // update workspace configuration
            String workspaceConfig = wd + File.separator + Environment.getInstance().getName()
                    + File.separator + WorkspaceConfig.WORKSPACE_CONFIG_NAME;
            akaConfig.setOpenWorkspaceConfig(workspaceConfig);

            // export to file
            akaConfig.exportToJson();

        } else
            throw new Exception("Failed to open environment " + scriptFile.getName());
    }

    public static void rebuildEnvironment() throws Exception {
        // STEP 1: load project
        boolean findCompilationError = getProjectNode() == null;

        if (findCompilationError)
            throw new Exception("Found compilation error");

        // STEP 2: check whether we need to
        if (ChangesBetweenSourcecodeFiles.modifiedSourcecodeFiles.size() == 0)
            return;

        // STEP 3: show a dialog to inform changes
        new WorkspaceUpdater().update();
    }

    private static INode getProjectNode() {
        ChangesBetweenSourcecodeFiles.reset();

        WorkspaceLoader loader = new WorkspaceLoader();
        String physicalTreePath = new WorkspaceConfig().fromJson().getPhysicalJsonFile();
        loader.setPhysicalTreePath(new File(physicalTreePath));
        loader.setElementFolderOfOldVersion(new WorkspaceConfig().fromJson().getElementDirectory());

        // todo: co can backup environment??
        // the load method below will call one thread, so use while loop to wait for the thread done
        loader.load(loader.getPhysicalTreePath());
        while (!loader.isLoaded()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loader.getRoot();
    }

    public static void loadTestCaseTree() {
        TestCaseManager.clearMaps();

        String testScriptFilePath = new WorkspaceConfig().fromJson().getTestscriptFile();
        File testScriptFile = new File(testScriptFilePath);

        Environment.getInstance().loadTestCasesScript(testScriptFile);

        TestCaseManager.initializeMaps();
    }
}

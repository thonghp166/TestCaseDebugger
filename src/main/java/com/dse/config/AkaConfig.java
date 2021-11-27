package com.dse.config;

import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.dse.util.AkaLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AkaConfig {
    final static AkaLogger logger = AkaLogger.get(AkaConfig.class);

    public static final File LOCAL_DIRECTORY = new File("local");
//    public static final File DEFAULT_WORKING_DIRECTORY = new File("local/working-directory");
    public static final File SETTING_PROPERTIES_PATH = new File("local/application.aka");

    @Expose
    private String localDirectory = LOCAL_DIRECTORY.getAbsolutePath();

    @Expose
    private String workingDirectory = "";

    @Expose
    private String openingWorkspaceDirectory = "";

    @Expose
    private String openWorkspaceConfig = "";

    @Expose
    private String z3Path = "";

//    @Expose
//    private String creatingWorkspaceDirectory = "";// not null when we create new environment
//
//    @Expose
//    private String creatingWorkspaceConfig = "";// not null when we create new environment

    @Expose
    private List<String> recentEnvironments = new ArrayList<>(); // path to env files

    public AkaConfig(){
    }

    synchronized public AkaConfig fromJson() {
        if (SETTING_PROPERTIES_PATH.exists()) {
            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.setPrettyPrinting().create();
            AkaConfig setting = gson.fromJson(Utils.readFileContent(SETTING_PROPERTIES_PATH), AkaConfig.class);
            return setting;
        } else {
            logger.error("The " + SETTING_PROPERTIES_PATH.getAbsolutePath() + " does not exist!");
            AkaConfig config = new AkaConfig();
//            config.setWorkingDirectory(DEFAULT_WORKING_DIRECTORY.getAbsolutePath());
            config.exportToJson();
            return config;
        }
    }

    synchronized public void exportToJson() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.setPrettyPrinting().create();
        String json = gson.toJson(this);
        Utils.writeContentToFile(json, SETTING_PROPERTIES_PATH.getAbsolutePath());
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public AkaConfig setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        new File(workingDirectory).mkdirs();
        return this;
    }

    public String getOpeningWorkspaceDirectory() {
        return openingWorkspaceDirectory;
    }

    public AkaConfig setOpeningWorkspaceDirectory(String openingWorkspaceDirectory) {
        this.openingWorkspaceDirectory = openingWorkspaceDirectory;
        new File(openingWorkspaceDirectory).mkdirs();

        this.workingDirectory = new File(openingWorkspaceDirectory).getParent();

        return this;
    }

    public String getLocalDirectory() {
        return localDirectory;
    }

    public AkaConfig setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
        new File(localDirectory).mkdirs();
        return this;
    }

    public String getOpenWorkspaceConfig() {
        return openWorkspaceConfig;
    }

    public AkaConfig setOpenWorkspaceConfig(String openWorkspaceConfig) {
        this.openWorkspaceConfig = openWorkspaceConfig;
        return this;
    }

    public List<String> getRecentEnvironments() {
        for (int i = recentEnvironments.size() - 1; i >=0 ;i --)
            if (!new File(recentEnvironments.get(i)).exists())
                recentEnvironments.remove(i);
        return recentEnvironments;
    }

    public AkaConfig setRecentEnvironments(List<String> recentEnvironments) {
        this.recentEnvironments = recentEnvironments;
        return this;
    }


    public String getZ3Path() {
        return z3Path;
    }

    public AkaConfig setZ3Path(String z3Path) {
        this.z3Path = z3Path;
        return this;
    }
}

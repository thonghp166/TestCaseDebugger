package com.dse.config;

import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.dse.util.AkaLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommandConfig {
    final static AkaLogger logger = AkaLogger.get(CommandConfig.class);

    @Expose
    private Map<String, String> compilationCommands = new HashMap<>();

    @Expose
    private String linkingCommand = "";

    @Expose
    private String executablePath = "";


    public CommandConfig(){

    }

    public CommandConfig fromJson(String compilationFile){

        if (compilationFile != null && compilationFile.length() > 0 && new File(compilationFile).exists()){
            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.setPrettyPrinting().create();
            CommandConfig setting = gson.fromJson(Utils.readFileContent(compilationFile), CommandConfig.class);
            return setting;
        } else {
            logger.error("The command file is not set up");
            return new CommandConfig();
        }
    }

    public CommandConfig fromJson(){
        return fromJson(new WorkspaceConfig().fromJson().getCommandFile());
    }

    synchronized public void exportToJson(File compilationFile) {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.setPrettyPrinting().create();
        String json = gson.toJson(this);

        Utils.writeContentToFile(json, compilationFile.getAbsolutePath());
    }

    synchronized public void exportToJson() {
        exportToJson(new File(new WorkspaceConfig().fromJson().getCommandFile()));
    }

    public Map<String, String> getCompilationCommands() {
        return compilationCommands;
    }

    public CommandConfig setCompilationCommands(Map<String, String> compilationCommands) {
        this.compilationCommands = compilationCommands;
        return this;
    }

    public String getLinkingCommand() {
        return linkingCommand;
    }

    public CommandConfig setLinkingCommand(String linkingCommand) {
        this.linkingCommand = linkingCommand;
        return this;
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }
}

package com.dse.debugger.gdb;

import com.dse.debugger.component.breakpoint.BreakPoint;
import com.dse.debugger.component.frame.GDBFrame;
import com.dse.debugger.component.variable.GDBVar;
import com.dse.debugger.component.watches.WatchPoint;
import com.dse.debugger.gdb.analyzer.GDBStatus;
import com.dse.debugger.gdb.analyzer.GDBToken;
import com.dse.debugger.gdb.analyzer.OutputGDB;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;

import java.util.ArrayList;

import static com.dse.debugger.gdb.analyzer.OutputSyntax.*;

public class OutputAnalyzer {
    private static final Logger logger = Logger.getLogger(OutputAnalyzer.class);
    private final static Gson gson = new Gson();

    public static GDBStatus analyze(String output, String command) {
        if (output != null) {
            String removedOutput = removeUnnecessary(output);
            String json = parseOutputToJson(removedOutput, command);
            logger.debug("Parsing the result of command " +  command +":\n" + json);
            JsonObject jsonOb = JsonParser.parseString(json).getAsJsonObject();
            GDBStatus res;
            if (jsonOb.get("reason") != null) {
                String reason = jsonOb.get("reason").getAsString();
                if (reason.equals(EXIT_HIT.getSyntax())) {
                    res = GDBStatus.EXIT;
                    res.setReason(reason);
                    return res;
                }
                if (reason.equals(EXIT_ERROR.getSyntax())) {
                    res = GDBStatus.ERROR;
                    res.setReason(reason);
                    return res;
                }
                res = GDBStatus.CONTINUABLE;
                res.setReason(reason);
                return res;
            } else {
                if (command.equals(GDB.GDB_STEP_OUT)) {
                    return GDBStatus.CONTINUABLE;
                } else if (command.equals(GDB.GDB_KILL)) {
                    return GDBStatus.EXIT;
                }
                return GDBStatus.ERROR;
            }
        } else
            return GDBStatus.ERROR;
    }

    public static ArrayList<GDBFrame> analyzeFrames(String output, String command) {
        String removeOutput = removeUnnecessary(output);
        String json = parseOutputToJson(removeOutput, command);
        JsonObject jsonOb = JsonParser.parseString(json).getAsJsonObject();
        if (command.equals(GDB.GDB_FRAME_LIST))
            return extractFrame(jsonOb.getAsJsonArray("stack"));
        return null;
    }

    private static ArrayList<GDBFrame> extractFrame(JsonArray frameArr) {
        ArrayList<GDBFrame> res = new ArrayList<>();
            frameArr.forEach(jsonElement -> {
            JsonObject jo = jsonElement.getAsJsonObject();
            res.add(gson.fromJson(jo, GDBFrame.class));
        });
        return res;
    }

    public static GDBVar getVarCreated(String output) {
        String removeOutput = removeUnnecessary(output);
        if (removeOutput.startsWith(DONE_RESULT.getSyntax())){
            String json = parseOutputToJson(removeOutput, "");
            JsonObject jsonOb = JsonParser.parseString(json).getAsJsonObject();
            return gson.fromJson(jsonOb,GDBVar.class);
        }
        return null;
    }


    public static GDBVar analyzeInternalVariable(String output) {
        String removedOutput = removeUnnecessary(output);
        String json = parseOutputToJson(removedOutput, "");
        JsonObject jsonOb = JsonParser.parseString(json).getAsJsonObject();
        return gson.fromJson(jsonOb, GDBVar.class);
    }

    public static ArrayList<GDBVar> analyzeChildInternalVariable(String childOutput) {
        String removedOutput = removeUnnecessary(childOutput).replaceAll("child=\\{", "\\{");
        String json = parseOutputToJson(removedOutput, "");
        JsonObject jsonOb = JsonParser.parseString(json).getAsJsonObject();
        ArrayList<GDBVar> res = new ArrayList<>();
        JsonArray jsonArray = jsonOb.getAsJsonArray("children");
        if(jsonArray != null){
            jsonArray.forEach(jsonElement -> {
                JsonObject jo = jsonElement.getAsJsonObject();
                res.add(gson.fromJson(jo, GDBVar.class));
            });
        }
        return res;
    }

    public static ArrayList<GDBVar> analyzeVariables(String output, String command) {
        String removeOutput = removeUnnecessary(output);
        String json = parseOutputToJson(removeOutput, command);
        JsonArray jsonArr = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("variables");
        if (jsonArr == null) {
            return new ArrayList<>();
        }
        return extractVar(jsonArr);
    }


    public static OutputGDB analyzeOutput(String output,String cmd) {
        String removedOutput = removeUnnecessary(output);
        if (cmd.equals(GDB.GDB_DEL_POINT) || cmd.equals(GDB.GDB_SELECT_FRAME) || cmd.equals(GDB.GDB_DATA_EVALUATE_EXPRESSION) ||
                cmd.equals(GDB.GDB_DISABLE) || cmd.equals(GDB.GDB_ENABLE) || cmd.equals(GDB.GDB_BREAK_CONDITION)){
            if (removedOutput.equals("")){
                return new OutputGDB(false,"");
            }
        }
        if (removedOutput.startsWith(ERROR_RESULT.getSyntax())){
            String json = parseOutputToJson(removedOutput,"");
            return new OutputGDB(true,json);
        } else if (removedOutput.startsWith(DONE_RESULT.getSyntax())) {
            String json = parseOutputToJson(removedOutput,"");
            return new OutputGDB(false,json);
        }
        return null;
    }

    public static WatchPoint analyzeWatchPoint(OutputGDB outputGDB) {
        String json = outputGDB.getJson();
        JsonObject watch = JsonParser.parseString(json).getAsJsonObject().get("wpt").getAsJsonObject();
        return gson.fromJson(watch,WatchPoint.class);
    }

    public static BreakPoint analyzeBreakAdd(OutputGDB outputGDB) {
        if (outputGDB.isError()){
            return null;
        } else {
            String json = outputGDB.getJson();
            JsonObject breakpoint = JsonParser.parseString(json).getAsJsonObject().get("bkpt").getAsJsonObject();
            return gson.fromJson(breakpoint, BreakPoint.class);
        }
    }


    private static ArrayList<GDBVar> extractVar(JsonArray jsonArray) {
        ArrayList<GDBVar> res = new ArrayList<>();
        jsonArray.forEach(jsonElement -> {
            JsonObject jo = jsonElement.getAsJsonObject();
            res.add(gson.fromJson(jo, GDBVar.class));
        });
        return res;
    }

    private static String parseOutputToJson(String output, String cmd) {
        switch (cmd) {
            case GDB.GDB_FRAME_LIST:
                output = output.replaceAll("frame=\\{", "\\{");
                return makeJsonFromRes(output);
            case GDB.GDB_RUN:
            case GDB.GDB_NEXT:
            case GDB.GDB_FRAME_VARIABLES:
            default:
                return makeJsonFromRes(output);
        }

    }

    private static String makeJsonFromRes(String res) {
        int start = res.indexOf(",");
        String json = res.substring(start + 1).replaceAll("=", ":");
        json = "{" + json + "}";
        return json;
    }

    private static String removeUnnecessary(String output) {
        String[] lines = output.split(System.getProperty("line.separator"));
        StringBuilder res = new StringBuilder();
        for (String line : lines) {
            if (!(isUseless(line) || isNotContainingInfo(line))) {
                res.append(line).append("\n");
            }
        }
        return res.toString();
    }

    private static boolean isUseless(String line) {
        return line.startsWith(GDBToken.NOTIFY_ASYNC_OUTPUT.getToken()) ||
                line.startsWith(GDBToken.CONSOLE_STREAM_OUTPUT.getToken()) ||
                line.startsWith(GDBToken.TARGET_STREAM_OUTPUT.getToken()) ||
                line.startsWith(GDBToken.LOG_STREAM_OUTPUT.getToken()) ||
                line.startsWith(END_LOG.getSyntax());
    }

    private static boolean isNotContainingInfo(String line) {
        if (line.startsWith(DONE_RESULT.getSyntax() + ",time="))
            return true;
        return line.startsWith(GDBToken.RESULT_RECORD.getToken() + RUNNING_RESULT.getSyntax()) ||
                line.startsWith(GDBToken.EXEC_ASYNC_OUTPUT.getToken() + RUNNING_RESULT.getSyntax());
    }

}

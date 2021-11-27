package com.dse.testcase_manager;

import com.dse.util.DateTimeUtils;
import com.dse.util.PathUtils;
import com.dse.util.Utils;
import com.google.gson.*;

import java.io.File;
import java.time.LocalDateTime;

public class CompoundTestCaseImporter {

    public static void main(String[] args) {
        String path = "datatest/hoannv/compound_testcase/COMPOUND.97322.json";
        File file = new File(path);
        CompoundTestCaseImporter importer = new CompoundTestCaseImporter();
        CompoundTestCase compoundTestCase = importer.importCompoundTestCase(file);
        System.out.println(compoundTestCase.getName());
    }

    public CompoundTestCase importCompoundTestCase(File file) {
        JsonDeserializer<CompoundTestCase> deserializer = (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            CompoundTestCase compoundTestCase = new CompoundTestCase();
            String name = jsonObject.get("name").getAsString();
            if (name != null) {
                compoundTestCase.setName(name);
            }

            if (jsonObject.get("result") != null) {
                String[] tempList = jsonObject.get("result").getAsString().split("/");
                int[] result = new int[tempList.length];

                for (int i = 0; i < tempList.length; i++)
                    result[i] = Integer.parseInt(tempList[i]);

                compoundTestCase.setExecutionResult(result);
            }

            if (jsonObject.get("path") != null)
                compoundTestCase.setPath(PathUtils.toAbsolute(jsonObject.get("path").getAsString()));

            if (jsonObject.get("sourceCode") != null)
                compoundTestCase.setSourceCodeFile(PathUtils.toAbsolute(jsonObject.get("sourceCode").getAsString()));

            if (jsonObject.get("testPath") != null)
                compoundTestCase.setTestPathFile(PathUtils.toAbsolute(jsonObject.get("testPath").getAsString()));

            if (jsonObject.get("executable") != null)
                compoundTestCase.setExecutableFile(PathUtils.toAbsolute(jsonObject.get("executable").getAsString()));

            if (jsonObject.get("commandConfig") != null)
                compoundTestCase.setCommandConfigFile(PathUtils.toAbsolute(jsonObject.get("commandConfig").getAsString()));

            if (jsonObject.get("commandDebug") != null)
                compoundTestCase.setCommandDebugFile(PathUtils.toAbsolute(jsonObject.get("commandDebug").getAsString()));

            if (jsonObject.get("breakPoint") != null)
                compoundTestCase.setBreakpointPath(PathUtils.toAbsolute(jsonObject.get("breakPoint").getAsString()));

            if (jsonObject.get("debugExecutable") != null)
                compoundTestCase.setDebugExecutableFile(PathUtils.toAbsolute(jsonObject.get("debugExecutable").getAsString()));

            if (jsonObject.get("executionResult") != null)
                compoundTestCase.setExecutionResultFile(PathUtils.toAbsolute(jsonObject.get("executionResult").getAsString()));

            if (jsonObject.get("creationDate") != null) {
                LocalDateTime dt = DateTimeUtils.parse(jsonObject.get("creationDate").getAsString());
                compoundTestCase.setCreationDateTime(dt);
            }

            Gson gson = new Gson();
            JsonArray jsonSlots = jsonObject.get("slots").getAsJsonArray();
            if (jsonSlots != null) {
                for (JsonElement jsonSlot : jsonSlots) {
                    TestCaseSlot slot = gson.fromJson(jsonSlot, TestCaseSlot.class);
                    compoundTestCase.getSlots().add(slot);
                }
            }
            return compoundTestCase;
        };

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(CompoundTestCase.class, deserializer);
        Gson customGson = gsonBuilder.create();
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(Utils.readFileContent(file));
//        JsonElement json = JsonParser.parseString(Utils.readFileContent(file));
        return customGson.fromJson(json, CompoundTestCase.class);
    }
}

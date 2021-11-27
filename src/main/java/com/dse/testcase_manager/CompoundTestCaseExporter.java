package com.dse.testcase_manager;

import com.dse.util.PathUtils;
import com.dse.util.Utils;
import com.google.gson.*;

import java.io.File;
import java.lang.reflect.Type;

public class CompoundTestCaseExporter {

    public static void main(String[] args) {
        CompoundTestCase compoundTestCase = new CompoundTestCase();

        compoundTestCase.setNameAndPath("TestCompoundTestCaseExporter.001", "datatest/hoannv/compound_testcase/TestCompoundTestCaseExporter.001");
        CompoundTestCaseExporter exporter = new CompoundTestCaseExporter();
        exporter.export(new File(compoundTestCase.getPath()), compoundTestCase);
    }

    public void export(File path, CompoundTestCase compoundTestCase) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder
                .registerTypeAdapter(CompoundTestCase.class, new CustomSerializer())
                .setPrettyPrinting().create();
        String json = gson.toJson(compoundTestCase, CompoundTestCase.class);
        Utils.writeContentToFile(json, path.getAbsolutePath());
    }

    private static class CustomSerializer implements JsonSerializer<CompoundTestCase> {

        @Override
        public JsonElement serialize(CompoundTestCase compoundTestCase, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", compoundTestCase.getName());

            int[] result = compoundTestCase.getExecutionResult();
            if (result != null)
                jsonObject.addProperty("result", String.format("%d/%d", result[0], result[1]));

            jsonObject.addProperty("path", PathUtils.toRelative(compoundTestCase.getPath()));
            if (compoundTestCase.getSourceCodeFile() != null)
                jsonObject.addProperty("sourceCode", PathUtils.toRelative(compoundTestCase.getSourceCodeFile()));
            if (compoundTestCase.getTestPathFile() != null)
                jsonObject.addProperty("testPath", PathUtils.toRelative(compoundTestCase.getTestPathFile()));
            if (compoundTestCase.getExecutableFile() != null)
                jsonObject.addProperty("executable", PathUtils.toRelative(compoundTestCase.getExecutableFile()));
            if (compoundTestCase.getCommandConfigFile() != null)
                jsonObject.addProperty("commandConfig", PathUtils.toRelative(compoundTestCase.getCommandConfigFile()));
            if (compoundTestCase.getCommandDebugFile() != null)
                jsonObject.addProperty("commandDebug", PathUtils.toRelative(compoundTestCase.getCommandDebugFile()));
            if (compoundTestCase.getBreakpointPath() != null)
                jsonObject.addProperty("breakPoint", PathUtils.toRelative(compoundTestCase.getBreakpointPath()));
            if (compoundTestCase.getDebugExecutableFile() != null)
                jsonObject.addProperty("debugExecutable", PathUtils.toRelative(compoundTestCase.getDebugExecutableFile()));
            if (compoundTestCase.getExecutionResultFile() != null)
                jsonObject.addProperty("executionResult", PathUtils.toRelative(compoundTestCase.getExecutionResultFile()));
            if (compoundTestCase.getCreationDateTime() != null)
                jsonObject.addProperty("creationDate", PathUtils.toRelative(compoundTestCase.getCreationDateTime().toString()));

            JsonArray slots = new JsonArray();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.setPrettyPrinting().create();
            for (TestCaseSlot slot : compoundTestCase.getSlots()) {
                JsonElement slotElement = gson.toJsonTree(slot, TestCaseSlot.class);
                slots.add(slotElement);
            }
            jsonObject.add("slots", slots);
            return jsonObject;
        }
    }
}

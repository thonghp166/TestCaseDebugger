package com.dse.probe_point_manager.objects;

import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.FunctionNode;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.SourcecodeFileNode;
import com.dse.search.Search;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testcase_manager.TestCase;
import com.dse.testcase_manager.TestCaseManager;
import com.dse.util.Utils;
import com.google.gson.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ProbePointImporter {
    private final static Logger logger = Logger.getLogger(ProbePointImporter.class);

    public ProbePoint importProbePoint(JsonObject jsonObject) {
        JsonDeserializer<ProbePoint> deserializer = ((jsonElement, type, jsonDeserializationContext) -> {
            ProbePoint probePoint = new ProbePoint();

            try {
                String name = jsonObject.get("name").getAsString();
                String before = jsonObject.get("before").getAsString();
                String content = jsonObject.get("content").getAsString();
                String after = jsonObject.get("after").getAsString();
                String sourceCodeFileNodePath = jsonObject.get("sourceCodeFileNode").getAsString();
                String lineInSourceCodeFile = jsonObject.get("lineInSourceCodeFile").getAsString();
                String functionNodePath = jsonObject.get("functionNode").getAsString();
                String path = jsonObject.get("path").getAsString();
                String lineInFunction = jsonObject.get("lineInFunction").getAsString();
                JsonArray testCaseNames = jsonObject.get("testCases").getAsJsonArray();

                probePoint.setName(name);
                probePoint.setBefore(before);
                probePoint.setContent(content);
                probePoint.setAfter(after);
                probePoint.setPath(path);
                probePoint.setLineInSourceCodeFile(Integer.parseInt(lineInSourceCodeFile));
                probePoint.setLineInFunctionNode(Integer.parseInt(lineInFunction));

                for (JsonElement tcName : testCaseNames) {
                    TestCase testCase = TestCaseManager.getBasicTestCaseByName(tcName.getAsString());
                    if (testCase != null) probePoint.getTestCases().add(testCase);
                }

                IFunctionNode iFunctionNode = (IFunctionNode) UIController.searchFunctionNodeByPath(functionNodePath);
                probePoint.setFunctionNode(iFunctionNode);
                SourcecodeFileNode sourcecodeFileNode = searchSourceCodeFileNodeByPath(sourceCodeFileNodePath);
                if (sourcecodeFileNode != null) {
                    probePoint.setSourcecodeFileNode(sourcecodeFileNode);
                } else {
                    logger.debug("Can not search SourceCodeFileNode when import ProbePoint");
                    return null;
                }

            } catch (NullPointerException ne) {
                logger.debug("Null pointer exception");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return probePoint;
        });

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ProbePoint.class, deserializer);
        Gson customGson = gsonBuilder.create();
        return customGson.fromJson(jsonObject, ProbePoint.class);
    }

    private SourcecodeFileNode searchSourceCodeFileNodeByPath(String path) {
        SourcecodeFileNode matchedNode;

        // create conditions to search (both complete function & prototype function).
        List<INode> nodes = new ArrayList<>(Search.searchNodes(Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition()));

        for (INode node : nodes) {
            if (node instanceof SourcecodeFileNode) {
                if (Utils.normalizePath(node.getAbsolutePath()).equals(Utils.normalizePath(path))) {
                    matchedNode = (SourcecodeFileNode) node;
                    return matchedNode;
                }
            }
        }

        return null;
    }
}

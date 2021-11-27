package com.dse.testcase_manager;

import com.dse.parser.object.DefinitionFunctionNode;
import com.dse.parser.object.ICommonFunctionNode;
import com.dse.parser.object.INode;
import com.dse.testdata.Iterator;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.*;
import com.dse.util.AkaLogger;
import com.dse.util.NodeType;
import com.dse.util.PathUtils;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Export a tree of folders and files to json
 */
public class TestCaseDataExporter {
    private final static AkaLogger logger = AkaLogger.get(TestCaseDataExporter.class);

    public TestCaseDataExporter() {

    }

//    public String export(DataNode node) {
//        GsonBuilder builder = new GsonBuilder();
//        Gson gson = builder
//                .registerTypeAdapter(DataNode.class, new FunctionDetailTreeSerializer())
//                .setPrettyPrinting().create();
//        return gson.toJson(node, DataNode.class);
//    }
//
//    public JsonElement exportToJsonElement(DataNode node) {
//        GsonBuilder builder = new GsonBuilder();
//        Gson gson = builder
//                .registerTypeAdapter(DataNode.class, new FunctionDetailTreeSerializer())
//                .setPrettyPrinting().create();
//        return gson.toJsonTree(node, DataNode.class);
//    }
//
//    public void export(File path, DataNode node) {
//        String json = export(node);
//        System.out.println(json);
//        Utils.writeContentToFile(json, path.getAbsolutePath());
//    }

    public JsonElement exportToJsonElement(TestCase testCase) {
        DataNode node = testCase.getRootDataNode();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder
                .registerTypeAdapter(DataNode.class, new FunctionDetailTreeSerializer(testCase))
                .setPrettyPrinting().create();
        return gson.toJsonTree(node, DataNode.class);
    }

    static class FunctionDetailTreeSerializer implements JsonSerializer<DataNode> {

        // used for retrieve subprogram under test
        private TestCase testCase;

        public FunctionDetailTreeSerializer(TestCase testCase) {
            this.testCase = testCase;
        }

        @Override
        public JsonElement serialize(DataNode node, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject json = new JsonObject();
            json.addProperty("type", node.getClass().getName());
            json.addProperty("name", node.getName());

            if (node instanceof ValueDataNode) {
                json.addProperty("dataType", ((ValueDataNode) node).getType());

                if (!(node instanceof SubprogramNode))
                    json.addProperty("external", ((ValueDataNode) node).isExternel());
            }

                json.addProperty("virtual_name", node.getVituralName());

            if (node instanceof RootDataNode) {
                RootDataNode root = (RootDataNode) node;

                if (root.getLevel().equals(NodeType.ROOT)) {
                    String relativePath = PathUtils.toRelative(root.getFunctionNode().getAbsolutePath());
                    json.addProperty("functionNode", relativePath);
                } else if (root.getLevel().equals(NodeType.GLOBAL)) {
                    JsonArray eoArray = new JsonArray();
                    if (root.getGlobalInputExpOutputMap() == null) {
                        logger.debug("GlobalInputExpOutputMap equals null.");
                    } else {
                        for (IDataNode eoDataNode : root.getGlobalInputExpOutputMap().values()) {
                            eoArray.add(jsonSerializationContext.serialize(eoDataNode, DataNode.class));
                        }
                    }
                    json.add("paramExpectedOuputs", eoArray);
                }

                json.addProperty("level", root.getLevel().name());

            } else if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;
                String relativePath = PathUtils.toRelative(unit.getSourceNode().getAbsolutePath());
                json.addProperty("sourceNode", relativePath);
//
            } else if (node instanceof ConstructorDataNode) {
                INode functionNode = ((ConstructorDataNode) node).getFunctionNode();

                String path = "";

                if (functionNode != null) {
                    if (node.getParent() instanceof SmartPointerDataNode) {
                        if (functionNode instanceof DefinitionFunctionNode) {
                            path = ((DefinitionFunctionNode) functionNode).getAST().getRawSignature();
                            String relativePath = PathUtils.toRelative(((SmartPointerDataNode) node.getParent())
                                    .getCorrespondingVar().getAbsolutePath());
                            json.addProperty("pointer_path", relativePath);
                        }

                    } else {
                        path = functionNode.getAbsolutePath();
                        path = PathUtils.toRelative(path);
                    }
                }

                json.addProperty("functionNode", path);

            } else if (node instanceof SubprogramNode) {
                String path = PathUtils.toRelative(((SubprogramNode) node).getFunctionNode().getAbsolutePath());
                json.addProperty("functionNode", path);

                if (node instanceof TemplateSubprogramDataNode) {
                    JsonArray realTypesJson = new JsonArray();

                    // if the function is template, we should store the defined type of template parameters
                    Map<String, String> realTypeMapping = ((TemplateSubprogramDataNode) node).getRealTypeMapping();
                    for (String key : realTypeMapping.keySet()) {
                        realTypesJson.add(key + "->" + realTypeMapping.get(key));
                    }
                    json.add(TemplateSubprogramDataNode.NAME_TEMPLATE_TYPE, realTypesJson);

                } else if (node instanceof MacroSubprogramDataNode) {
                    JsonArray realTypesJson = new JsonArray();

                    // if the function is template, we should store the defined type of template parameters
                    Map<String, String> realTypeMapping = ((MacroSubprogramDataNode) node).getRealTypeMapping();
                    for (String key : realTypeMapping.keySet()) {
                        realTypesJson.add(key + "->" + realTypeMapping.get(key));
                    }
                    json.add(MacroSubprogramDataNode.NAME_MACRO_TYPE, realTypesJson);
                }

                // if SubprogramNode is subprogram under test
                // then export Expected Output if not existed yet
                ICommonFunctionNode sut = testCase.getFunctionNode();
                if (sut.equals(((SubprogramNode) node).getFunctionNode())) {
                    if (node.getChildren().size() > 0) {
                        JsonArray eoArray = new JsonArray();
                        for (IDataNode eoDataNode : ((SubprogramNode) node).getParamExpectedOuputs()) {
                            eoArray.add(jsonSerializationContext.serialize(eoDataNode, DataNode.class));
                        }
                        json.add("paramExpectedOuputs", eoArray);
                    }
                }

            } else if (node instanceof NormalCharacterDataNode
                    || node instanceof NormalNumberDataNode
                    || node instanceof NormalStringDataNode) {
                NormalDataNode dataNode = (NormalDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                if (dataNode.getCorrespondingType() != null)
                    json.addProperty("correspondingType", dataNode.getType());

                String value = "null";
                if (dataNode.getValue() != null)
                    value = ((NormalDataNode) node).getValue();

                json.addProperty("value", value);

            } else if (node instanceof PointerDataNode) {
                PointerDataNode dataNode = (PointerDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                if (dataNode.getCorrespondingType() != null) {
                    String relativePath = PathUtils.toRelative(dataNode.getCorrespondingType().getAbsolutePath());
                    json.addProperty("correspondingType", relativePath);
                }

                json.addProperty("level", dataNode.getLevel());

                if (dataNode.isSetSize())
                    json.addProperty("size", dataNode.getAllocatedSize());

            } else if (node instanceof OneDimensionDataNode) {
                OneDimensionDataNode dataNode = (OneDimensionDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                if (dataNode.getCorrespondingType() != null){
                    String relativePath = PathUtils.toRelative(dataNode.getCorrespondingType().getAbsolutePath());
                    json.addProperty("correspondingType", relativePath);
                }

                if (((OneDimensionDataNode) node).isSetSize())
                    json.addProperty("size", ((OneDimensionDataNode) node).getSize());

            } else if (node instanceof MultipleDimensionDataNode) {
                MultipleDimensionDataNode dataNode = (MultipleDimensionDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);
                if (dataNode.getCorrespondingType() != null){
                    String relativePath = PathUtils.toRelative(dataNode.getCorrespondingType().getAbsolutePath());
                    json.addProperty("correspondingType", relativePath);
                }

                json.addProperty("dimensions", dataNode.getDimensions());

                if (dataNode.isSetSize()) {
                    int[] sizes = dataNode.getSizes();
                    StringBuilder sizesInString = new StringBuilder();
                    for (int i = 0; i < sizes.length - 1; i++)
                        sizesInString.append(sizes[i]).append(", ");
                    sizesInString.append(sizes[sizes.length - 1]);

                    json.addProperty("size", sizesInString.toString());
                }

            } else if (node instanceof SubClassDataNode) {
                SubClassDataNode dataNode = (SubClassDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                INode correspondingType = dataNode.getCorrespondingType();

                String typeRelativePath = PathUtils.toRelative(correspondingType.getAbsolutePath());
                json.addProperty("correspondingType", typeRelativePath);

                json.addProperty("rawType", dataNode.getType());

                ConstructorDataNode constructorDataNode = dataNode.getConstructorDataNode();
                if (constructorDataNode != null) {
                    String constructor = constructorDataNode.getName();
                    json.addProperty("selectedConstructor", constructor);
                    String variableType = dataNode.getType();
                    json.addProperty("variableType", variableType);
                }

            } else if (node instanceof ClassDataNode) {
                ClassDataNode dataNode = (ClassDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                INode correspondingType = dataNode.getCorrespondingType();
                String typeRelativePath = PathUtils.toRelative(correspondingType.getAbsolutePath());
                json.addProperty("correspondingType", typeRelativePath);

            } else if (node instanceof EnumDataNode) {
                EnumDataNode dataNode = (EnumDataNode) node;

                json.addProperty("value", dataNode.getValue());
                json.addProperty("valueIsSet", dataNode.isSetValue());

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                INode correspondingType = dataNode.getCorrespondingType();
                String typeRelativePath = PathUtils.toRelative(correspondingType.getAbsolutePath());
                json.addProperty("correspondingType", typeRelativePath);

            } else if (node instanceof UnionDataNode) {
                UnionDataNode dataNode = (UnionDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                INode correspondingType = dataNode.getCorrespondingType();
                String typeRelativePath = PathUtils.toRelative(correspondingType.getAbsolutePath());
                json.addProperty("correspondingType", typeRelativePath);

            } else if (node instanceof StructDataNode) {
                StructDataNode dataNode = (StructDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                INode correspondingType = dataNode.getCorrespondingType();
                String typeRelativePath = PathUtils.toRelative(correspondingType.getAbsolutePath());
                json.addProperty("correspondingType", typeRelativePath);

            } else if (node instanceof STLDataNode) {
                STLDataNode dataNode = (STLDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                json.addProperty("correspondingType", "STLListBaseType");

                if (dataNode instanceof ListBaseDataNode) {
                    ListBaseDataNode listBaseDataNode = (ListBaseDataNode) dataNode;
                    json.addProperty("templateArg", listBaseDataNode.getTemplateArgument());

                    if (listBaseDataNode.isSetSize()) {
                        json.addProperty("size", listBaseDataNode.getSize());
                    }

                } else if (dataNode instanceof PairDataNode) {
                    PairDataNode pairDataNode = (PairDataNode) dataNode;

                    JsonObject templateArg = new JsonObject();
                    templateArg.addProperty("first", pairDataNode.getFirstType());
                    templateArg.addProperty("second", pairDataNode.getSecondType());
                    json.add("templateArg", templateArg);

                } else if (dataNode instanceof SmartPointerDataNode) {
                    SmartPointerDataNode smartPointerDataNode = (SmartPointerDataNode) dataNode;
                    json.addProperty("templateArg", smartPointerDataNode.getTemplateArgument());

                } else if (dataNode instanceof DefaultDeleteDataNode) {
                    DefaultDeleteDataNode deleteDataNode = (DefaultDeleteDataNode) dataNode;

                    List<String> arguments = deleteDataNode.getArguments();

                    if (arguments != null && !arguments.isEmpty())
                        json.addProperty("templateArg", arguments.get(0));

                } else if (dataNode instanceof AllocatorDataNode) {
                    AllocatorDataNode allocatorDataNode = (AllocatorDataNode) dataNode;

                    List<String> arguments = allocatorDataNode.getArguments();

                    if (arguments != null && !arguments.isEmpty())
                        json.addProperty("templateArg", arguments.get(0));
                }

            } else if (node instanceof VoidPointerDataNode) {
                VoidPointerDataNode dataNode = (VoidPointerDataNode) node;
                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                json.addProperty("userCode", dataNode.getUserCode());

            } else if (node instanceof OtherUnresolvedDataNode) {
                OtherUnresolvedDataNode dataNode = (OtherUnresolvedDataNode) node;

                String varRelativePath = PathUtils.toRelative(dataNode.getCorrespondingVar().getAbsolutePath());
                json.addProperty("correspondingVar", varRelativePath);

                json.addProperty("userCode", dataNode.getUserCode());

            } else {
                logger.error("Do not support to export the value of node \"" + node.getName() + "\", class = \"" + node.getClass() + "\" to json file in this version");
            }

            // add children
            if (node.getChildren().size() > 0) {
                JsonArray childrenArr = new JsonArray();
                for (IDataNode child : node.getChildren())
                    childrenArr.add(jsonSerializationContext.serialize(child, DataNode.class));
                json.add("children", childrenArr);
            }

            if (node instanceof ValueDataNode && ((ValueDataNode) node).isStubArgument()) {
                ValueDataNode valueNode = (ValueDataNode) node;

                if (valueNode.getIterators().get(0).getDataNode() == valueNode) {
                    JsonArray iterators = new JsonArray();

                    for (Iterator iterator : valueNode.getIterators()) {
                        JsonObject iteratorJsonObj = new JsonObject();
                        iteratorJsonObj.addProperty("start", iterator.getStartIdx());
                        iteratorJsonObj.addProperty("repeat", iterator.getRepeat());

                        if (iterator.getDataNode() != node) {
                            JsonElement dataNodeJsonObj = jsonSerializationContext
                                    .serialize(iterator.getDataNode(), DataNode.class);
                            iteratorJsonObj.add("dataNode", dataNodeJsonObj);
                        }

                        iterators.add(iteratorJsonObj);
                    }

                    json.add("iterators", iterators);
                }
            }

            return json;
        }
    }

}

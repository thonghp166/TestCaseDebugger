//package example.gson.polynomic.example2;
//
//import com.dse.gson.NodeGsonUtil;
//import com.dse.parser.ProjectParser;
//import com.dse.parser.object.INode;
//import com.dse.parser.object.Node;
//import com.dse.parser.object.ProjectNode;
//import com.dse.search.Search;
//import com.dse.search.condition.FunctionNodeCondition;
//import com.dse.search.condition.SourcecodeFileNodeCondition;
//import com.dse.util.Utils;
//import com.dse.util.tostring.NameDisplayer;
//
//import java.io.File;
//import java.util.List;
//
//public class NodeGson {
//    public static void main(String[] args) {
//        ProjectParser projectParser = new ProjectParser(new File(
//                "datatest/duc-anh/CombinedStaticAndDynamicGen"));
//        projectParser.setExpandTreeuptoMethodLevel_enabled(true);
//        projectParser.setGlobalVarDependencyGeneration_enabled(true);
//        projectParser.setFuncCallDependencyGeneration_enabled(true);
//        projectParser.setExtendedDependencyGeneration_enabled(true);
//        projectParser.setParentReconstructor_enabled(true);
//        projectParser.setGenerateSetterandGetter_enabled(true);
//        ProjectNode projectRoot = (ProjectNode) projectParser.getRootTree();
//
//        List<INode> nodes = Search.searchNodes(projectRoot, new SourcecodeFileNodeCondition());
//        for (INode node : nodes) {
//            if (node.getDependencies().size() != 0) {
//                System.out.println("dependency");
//            }
//        }
//        nodes = Search.searchNodes(projectRoot, new FunctionNodeCondition());
//        for (INode node : nodes) {
//            if (node.getDependencies().size() != 0) {
//                System.out.println("dependency");
//            }
//        }
//
//        /**
//         * Export to json
//         */
//        String json = NodeGsonUtil.create().toJson(projectRoot, Node.class);
//        Utils.writeContentToFile(json, "gson/node.json");
//
//        /**
//         * Deserialize json
//         */
//        Node root = NodeGsonUtil.create().fromJson(Utils.readFileContent(new File("gson/node.json")), Node.class);
//        System.out.println(new NameDisplayer(root).getTreeInString());
//    }
//}

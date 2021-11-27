package auto_testcase_generation.testdatagen;

import com.dse.config.Paths;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.ProjectParser;
import com.dse.parser.funcdetail.FunctionDetailTree;
import com.dse.parser.funcdetail.IFunctionDetailTree;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.INode;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.testdata.DataTree;
import com.dse.testdata.IDataTree;
import com.dse.testdata.InputCellHandler;
import com.dse.testdata.gen.module.AbstractDataTreeGeneration;
import com.dse.testdata.gen.module.SimpleTreeDisplayer;
import com.dse.testdata.gen.module.subtree.*;
import com.dse.testdata.object.*;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.dse.util.NodeType.*;

public class DataTreeGenerationForAutomation extends AbstractDataTreeGeneration {
    private IDataTree dataTree;
    private IFunctionDetailTree functionTree;

    public static void main(String[] args) {
        ProjectParser parser = new ProjectParser(new File(Paths.JOURNAL_TEST));
        parser.setExpandTreeuptoMethodLevel_enabled(true);

        String name = "linear_search1(long[],long,long)";
        IFunctionNode function = (IFunctionNode) Search
                .searchNodes(parser.getRootTree(), new FunctionNodeCondition(), name).get(0);

        Map<String, String> staticSolution = new HashMap<>();
        staticSolution.put("a", "sizeof(a)=5");
        staticSolution.put("n", "5");
        staticSolution.put("find", "23");
        staticSolution.put("a[0]", "2");
        staticSolution.put("a[2]", "-32");
        staticSolution.put("a[1]", "23");
        staticSolution.put("a[3]", "52");
        staticSolution.put("a[4]", "12");

        IDataTree dataTree = new DataTree();
        dataTree.setFunctionNode(function);

        try {
            new DataTreeGenerationForAutomation(dataTree, staticSolution).generateTree();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println(new SimpleTreeDisplayer().toString(dataTree.getRoot()));
    }

    public DataTreeGenerationForAutomation(IDataTree dataTree, Map<String, String> staticSolution)  {
        this.dataTree = dataTree;
        this.values = staticSolution;
        setRoot(dataTree.getRoot());
        functionNode = dataTree.getFunctionNode();
        functionTree = new FunctionDetailTree(functionNode);
    }

    @Override
    public void generateTree() throws Exception {
        root.setFunctionNode(functionNode);
        INode sourceCode = Utils.getSourcecodeFile(functionNode);


        // generate uut branch
        new InitialUUTBranchGen().generateCompleteTree(root, functionTree);
        recursiveExpandUutBranch(root);

        // generate other sbf
        for (INode sbf : Environment.getInstance().getSBFs()) {
            if (!sourceCode.equals(sbf))
                new InitialStubUnitBranchGen().generate(root, sbf);
        }

        // generate stub branch
        RootDataNode stubRoot = new RootDataNode(STUB);
        root.addChild(stubRoot);
        stubRoot.setParent(root);
        new InitialStubTreeGen().generateCompleteTree(stubRoot, functionTree);
    }

    private void recursiveExpandUutBranch(DataNode node) throws Exception {
        // STEP 1: set & get virtual name of current node
        node.setVirtualName();
        String key = node.getVituralName();

        // STEP 2: get raw value from static solutions
        String value = values.get(key);

        // STEP 3: commit edit with value
        if (value != null) {
            if (node instanceof ArrayDataNode || node instanceof PointerDataNode) {
                // Ex: key = "p", value="sizeof(p)=1"
                // get the size of array
                value = value.substring(value.indexOf(SpecialCharacter.EQUAL) + 1);
            } else if (node instanceof ClassDataNode && !(node instanceof SubClassDataNode)) {
                // Ex: key = "sv", value="Student(int,int)"
                // get name of the constructor
                if (value.contains("("))
                    value = value.substring(0, value.indexOf('('));
            }

            if (node instanceof ValueDataNode)
                new InputCellHandler().commitEdit((ValueDataNode) node, value);
        }

        // STEP 4: repeat with its children
        for (IDataNode child : node.getChildren())
            recursiveExpandUutBranch((DataNode) child);
    }
}

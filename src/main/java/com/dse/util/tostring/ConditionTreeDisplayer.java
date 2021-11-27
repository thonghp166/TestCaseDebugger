package com.dse.util.tostring;

import java.io.File;

import auto_testcase_generation.parser.projectparser.ProjectLoader;
import com.dse.config.Paths;
import com.dse.parser.object.INode;
import com.dse.parser.object.IProjectNode;
import com.dse.parser.object.Node;

public class ConditionTreeDisplayer extends ToString {

    public ConditionTreeDisplayer(INode root) {
        super(root);
    }

    public static void main(String[] args) {
        // Project tree generation
        IProjectNode projectRootNode = new ProjectLoader().load(new File(Paths.TSDV_R1));

        // display tree of project
        ToString treeDisplayer = new ConditionTreeDisplayer(projectRootNode);
        System.out.println(treeDisplayer.getTreeInString());
    }

    private void displayTree(INode n, int level) {
        if (n == null)
            return;
        else {
            treeInString += genTab(level) + "[" + n.getClass().getSimpleName() + "] " + n.getNewType() + "\n";

        }
        for (Object child : n.getChildren()) {
            displayTree((Node) child, ++level);
            level--;
        }
    }

    @Override
    public String toString(INode n) {
        displayTree(n, 0);
        return treeInString;
    }
}

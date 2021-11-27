package auto_testcase_generation.testdatagen.se;

import java.util.ArrayList;
import java.util.List;

import com.dse.parser.object.INode;
import com.dse.parser.object.Node;
import com.dse.parser.object.VariableNode;

/**
 * Represent the paramaters of a function including the arguments + external
 * variables
 *
 * @author ducanhnguyen
 */
public class Parameter extends ArrayList<INode> {

    /**
     *
     */
    private static final long serialVersionUID = -2583457982870539611L;

    public Parameter() {
    }

    public Parameter(List<VariableNode> paramaters) {
        this.addAll(paramaters);
    }
}

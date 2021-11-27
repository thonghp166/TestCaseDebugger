package auto_testcase_generation.utils;

import auto_testcase_generation.testdatagen.testdatainit.VariableTypes;
import com.dse.config.Paths;
import com.dse.parser.ProjectParser;
import com.dse.parser.object.EnumNode;
import com.dse.parser.object.INode;
import com.dse.parser.object.TypedefDeclaration;
import com.dse.search.Search;
import com.dse.search.SearchCondition;
import com.dse.search.condition.EnumNodeCondition;
import com.dse.search.condition.TypedefNodeCondition;
import com.dse.util.VariableTypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DucToan on 27/07/2017
 */
public class VariableTypesUtils {
    public static boolean isEnumNode(String type) {
        ProjectParser parser = new ProjectParser(new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH));

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new EnumNodeCondition());

        List<INode> mydefines = Search.searchNodes(parser.getRootTree(), conditions);

        for (INode mydefine : mydefines) {
            if (mydefine.getNewType().equals(type)) {
                return true;
            }
        }

        return false;
    }

    public static EnumNode findEnumNode(String type) {
        ProjectParser parser = new ProjectParser(new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH));

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new EnumNodeCondition());

        List<INode> mydefines = Search.searchNodes(parser.getRootTree(), conditions);

        for (INode mydefine : mydefines) {
            if (mydefine.getNewType().equals(type)) {
                return (EnumNode) mydefine;
            }
        }

        return null;
    }

    public static boolean isDefineNode(String type) {
        ProjectParser parser = new ProjectParser(new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH));

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new TypedefNodeCondition());

        List<INode> mydefines = Search.searchNodes(parser.getRootTree(), conditions);

        for (INode mydefine : mydefines) {
            if (mydefine.getNewType().equals(type)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDefineNodeOfBasicType(String type) {
        ProjectParser parser = new ProjectParser(new File(Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH));

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new TypedefNodeCondition());

        List<INode> mydefines = Search.searchNodes(parser.getRootTree(), conditions);

        for (INode mydefine : mydefines) {
            if (mydefine.getNewType().equals(type)) {
                String oldType = ((TypedefDeclaration) mydefine).getOldType();
                if (VariableTypeUtils.isBasic(oldType))
                    return true;
            }
        }

        return false;
    }
}

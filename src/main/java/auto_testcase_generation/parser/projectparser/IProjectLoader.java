package auto_testcase_generation.parser.projectparser;

import java.io.File;

import com.dse.parser.object.INode;
import com.dse.parser.object.ProjectNode;

/**
 * @author DucAnh
 */
public interface IProjectLoader {

    int OBJECT = 6;
    int EXE = 5;
    int FOLDER = 3;
    int CPP_FILE = 2;
    int C_FILE = 1;
    int HEADER_FILE = 0;
    int UNDEFINED_COMPONENT = -1;
    String EXE_SYMBOL = ".exe";

    /**
     * Contains signals for detecting kind of project
     */
    // Represent project that has created by using Dev-Cpp
    String MAKEFILE_IN_DEVCPP_SYMBOL = "Makefile.win";
    // Represent project that has created by using Visual studio
    String MAKEFILE_IN_VISUAL_LEVEL2 = ".vcxproj";
    String MAKEFILE_IN_VISUAL_LEVEL1 = ".sln";
    // Represent project that has an own makefile named Makefile. In Unix, we
    // use command "make all" to run this makefile.
    String MAKEFILE_IN_PROJECT_SYMBOL = "Makefile";

    /**
     * Contains signals for detecting kind of file
     */
    String CPP_FILE_SYMBOL = ".cpp";
    String C_FILE_SYMBOL = ".c";
    String OBJECT_FILE_SYMBOL = ".o";
    String HEADER_FILE_SYMBOL_TYPE_1 = ".h";
    String HEADER_FILE_SYMBOL_TYPE_2 = ".hh";
    String[] IGNORED_FILE_SYMBOLS = {};

    /**
     * Construct structure path for the given C/C++ project
     *
     * @param projectPath project directory path
     * @return
     */
    ProjectNode load(File projectPath);

    /**
     * Generate an unique id for all nodes in the structure tree.
     *
     * @param root
     */
    void generateId(INode root);
}

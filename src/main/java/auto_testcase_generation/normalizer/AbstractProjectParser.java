package auto_testcase_generation.normalizer;

import com.dse.parser.object.IProcessNotify;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for parsing project
 *
 * @author ducanhnguyen
 */
public abstract class AbstractProjectParser extends AbstractParser {
    protected File projectPath;
    protected List<File> ignoreFolders = new ArrayList<>();
    protected IProcessNotify notify = null;

    public IProcessNotify getNotify() {
        return notify;
    }

    public void setNotify(IProcessNotify notify) {
        this.notify = notify;
    }

    public File getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(File projectPath) {
        this.projectPath = projectPath;
    }

    public List<File> getIgnoreFolders() {
        return ignoreFolders;
    }

    public void setIgnoreFolders(List<File> ignoreFolders) {
        this.ignoreFolders = ignoreFolders;
    }
}

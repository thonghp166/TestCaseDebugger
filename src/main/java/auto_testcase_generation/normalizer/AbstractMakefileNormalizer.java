package auto_testcase_generation.normalizer;

import java.io.File;

public abstract class AbstractMakefileNormalizer extends AbstractNormalizer {
    protected File makefilePath;

    public File getMakefilePath() {
        return makefilePath;
    }

    public void setMakefilePath(File makefilePath) {
        this.makefilePath = makefilePath;
    }
}

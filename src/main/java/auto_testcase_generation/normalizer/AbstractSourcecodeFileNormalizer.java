package auto_testcase_generation.normalizer;

/**
 * Abstract class for source code file normalization level
 *
 * @author ducanhnguyen
 */
public abstract class AbstractSourcecodeFileNormalizer extends AbstractNormalizer {

    @Override
    public boolean shouldWriteToFile() {
        return true;
    }
}

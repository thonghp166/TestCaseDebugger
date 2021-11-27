package auto_testcase_generation.normalizer;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cpp11ClassNormalizer extends AbstractSourcecodeFileNormalizer implements IClassNormalizer {

    public static void main(String[] args) {
        String test = "class LOGCPP Category final : Base { int x; }";
        Cpp11ClassNormalizer norm = new Cpp11ClassNormalizer();
        norm.setOriginalSourcecode(test);
        norm.normalize();
        System.out.println(norm.getNormalizedSourcecode());
    }

    @Override
    public void normalize() {
        if (originalSourcecode != null && originalSourcecode.length() > 0) {
            normalizeSourcecode = originalSourcecode;
            // class-key attr class-head-name base-clause {
            Pattern p = Pattern.compile("(?<key>class|struct)" + "(?<attr>(\\s+(?!class|struct)\\w+)*)"
                    + "(?<name>\\s+(?!final)\\w+(\\s+final)?)" + "(?<base>(\\s*:[^\\{]+)?)" + "(?<open>\\s*\\{)");
            Matcher m = p.matcher(originalSourcecode);
            StringBuffer sb = new StringBuffer(originalSourcecode.length());

            while (m.find()) {
                String key = m.group("key"), attr = m.group("attr"), name = m.group("name"), base = m.group("base"),
                        open = m.group("open");

                m.appendReplacement(sb, Matcher.quoteReplacement(key + fillWithEmpty(attr) + name + base + open));
            }

            m.appendTail(sb);
            normalizeSourcecode = sb.toString();
        }
    }

    private String fillWithEmpty(String origin) {
        char[] s = new char[origin.length()];
        Arrays.fill(s, ' ');
        return new String(s);
    }

    @Override
    public boolean shouldWriteToFile() {
        return true;
    }
}

package auto_testcase_generation.testdatagen;

import auto_testcase_generation.testdatagen.se.CustomJeval;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use when we do not know the size of array/pointer
 */
public class ValueToTestcaseConverter_UnknownSize {
    final static AkaLogger logger = AkaLogger.get(RandomValue.class);

    private Map<String, String> testcases = new HashMap<>();

    public static void main(String[] args) {
        String testcases = "a[2]=1;a[<other indexes>]=-1;a[6]=90;"; // means a[0]=a[1]=-1, a[2]=1
        ValueToTestcaseConverter_UnknownSize converter = new ValueToTestcaseConverter_UnknownSize(testcases);
        List<RandomValue> randomValues = converter.convert();
        System.out.println(randomValues);
    }

    public List<RandomValue> convert() {
        List<RandomValue> output = new ArrayList<>();
        findSizeOfPointerandArray(getTestcases(), output);
        updateDefaultValue(getTestcases(), output);
        findValueOfElement(getTestcases(), output);
        return output;
    }

    public ValueToTestcaseConverter_UnknownSize(String testcases) {
        if (testcases != null && testcases.length() > 0) {
            String[] tc = testcases.split(DELIMITER_BETWEEN_TESTCASES);
            for (String item : tc)
                if (item.contains(DELIMITER_BETWEEN_KEY_AND_VALUE)) {
                    String key = item.split(DELIMITER_BETWEEN_KEY_AND_VALUE)[0];
                    String value = item.split(DELIMITER_BETWEEN_KEY_AND_VALUE)[1];

                    value = new CustomJeval().evaluate(value);
                    this.testcases.put(key, value);
                }
            logger.debug(this.testcases);
        }
    }

    public List<RandomValue> findSizeOfPointerandArray(Map<String, String> testcases, List<RandomValue> output) {
        Map<String, List<String>> varToIndexes = new HashMap<>();

        for (String key : testcases.keySet()) {

            if (isArray(key) && !key.contains("<other indexes>")) {
                List<String> indexes = Utils.getIndexOfArray(key);
                String nameVar = key.substring(0, key.indexOf("["));

                if (!varToIndexes.containsKey(nameVar)) {
                    // initialize max size of array/pointer
                    List<String> initialIndexes = new ArrayList<>();
                    for (int i = 0; i < indexes.size(); i++)
                        initialIndexes.add("0"); // default size
                    varToIndexes.put(nameVar, initialIndexes);
                }

                List<String> values = varToIndexes.get(nameVar);
                for (int i = 0; i < indexes.size(); i++) {
                    Long newIndexNum = Long.parseLong(indexes.get(i)) + 1;

                    Long maxIndex = Long.parseLong(values.get(i));
                    if (newIndexNum > maxIndex) {
                        values.remove(i);
                        values.add(i, newIndexNum + "");

                        varToIndexes.remove(nameVar);
                        varToIndexes.put(nameVar, values);
                    }
                }
            }
        }

        // convert to sizeof
        for (String nameVar : varToIndexes.keySet()) {
            List<String> indexes = varToIndexes.get(nameVar);

            if (indexes.size() == 1) {
                long m = Long.parseLong(indexes.get(0));
                output.add(new RandomValue(nameVar, String.format("sizeof(%s)=%s", nameVar, m)));


                // create possible elements
                for (int i = 0; i < m; i++) {
                    String nameArrayWithIndex = nameVar + "[" + i + "]";
                    RandomValue rv = new RandomValue(nameArrayWithIndex, "");
                    if (!output.contains(rv))
                        output.add(rv);
                }

            } else if (indexes.size() == 2) {
                long m = Long.parseLong(indexes.get(0));
                long n = Long.parseLong(indexes.get(1));

                for (int i = 0; i < m; i++) {
                    String nameArrayWithIndex = nameVar + "[" + i + "]";
                    RandomValue rv = new RandomValue(nameVar, String.format("sizeof(%s)=%s", nameArrayWithIndex, n));
                    if (!output.contains(rv))
                        output.add(rv);
                }

                // create possible elements
                for (int i = 0; i < m; i++)
                    for (int j = 0; j < n; j++) {
                        String nameArrayWithIndex = nameVar + "[" + i + "][" + j + "]";
                        RandomValue rv = new RandomValue(nameArrayWithIndex, "");
                        if (!output.contains(rv))
                            output.add(rv);
                    }

            } else if (indexes.size() == 3) {
                long m = Long.parseLong(indexes.get(0));
                long n = Long.parseLong(indexes.get(1));
                long k = Long.parseLong(indexes.get(2));

                output.add(new RandomValue(nameVar, String.format("sizeof(%s)=%s", nameVar, m)));

                for (int i = 0; i < m; i++) {
                    String nameArrayWithIndex = nameVar + "[" + i + "]";
                    RandomValue rv = new RandomValue(nameVar, String.format("sizeof(%s)=%s", nameArrayWithIndex, n));
                    if (!output.contains(rv))
                        output.add(rv);
                }

                // create possible elements
                for (int i = 0; i < m; i++) {
                    for (int j = 0; j < n; j++) {
                        String nameArrayWithIndex = nameVar + "[" + i + "][" + j + "]";
                        RandomValue rv = new RandomValue(nameVar, String.format("sizeof(%s)=%s", nameArrayWithIndex, k));
                        if (!output.contains(rv))
                            output.add(rv);
                    }
                }

                for (int i = 0; i < m; i++)
                    for (int j = 0; j < n; j++)
                        for (int h = 0; h < n; h++) {
                            String nameArrayWithIndex = nameVar + "[" + i + "][" + j + "][" + h + "]";
                            RandomValue rv = new RandomValue(nameArrayWithIndex, "");
                            if (!output.contains(rv))
                                output.add(rv);
                        }
            }
        }

        return output;
    }

    private void updateDefaultValue(Map<String, String> testcases, List<RandomValue> randomValues) {
        // find default values
        Map<String, String> defaults = new HashMap<>();
        for (String key : testcases.keySet()) {
            if (isArray(key) && key.contains("<other indexes>")) {
                String value = testcases.get(key);
                defaults.put(key.substring(0, key.indexOf("[")), value);
            }
        }
        logger.debug("Defaults value = " + defaults);

        // update the elements which do not have any value
        for (RandomValue randomValue : randomValues)
            if (isArray(randomValue.getNameUsedInExpansion())) {
                String name = randomValue.getNameUsedInExpansion()
                        .substring(0, randomValue.getNameUsedInExpansion().indexOf("["));

                String v = defaults.get(name);
                if (v != null)
                    randomValue.setValue(v);
            }
    }

    public List<RandomValue> findValueOfElement(Map<String, String> testcases, List<RandomValue> output) {
        for (String key : testcases.keySet()) {
            if (key.contains("<other indexes>")) {
                // ignore
            } else {
                RandomValue randomValue = new RandomValue(key, testcases.get(key));

                int index = output.indexOf(randomValue);
                if (index>=0) {
                    output.remove(index);
                    output.add(randomValue);
                }
            }
        }
        return output;
    }

    private boolean isArray(String v) {
        return v.contains("[");
    }

    public Map<String, String> getTestcases() {
        return testcases;
    }

    public void setTestcases(Map<String, String> testcases) {
        this.testcases = testcases;
    }

    public static final String DELIMITER_BETWEEN_TESTCASES = ";";
    public static final String DELIMITER_BETWEEN_KEY_AND_VALUE = "=";
}

package com.dse.probe_point_manager;

import com.dse.config.WorkspaceConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.IFunctionNode;
import com.dse.parser.object.SourcecodeFileNode;
import com.dse.probe_point_manager.objects.ProbePoint;
import com.dse.probe_point_manager.objects.ProbePointExporter;
import com.dse.probe_point_manager.objects.ProbePointImporter;
import com.dse.testcase_manager.TestCase;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Type;
import java.util.*;

public class ProbePointManager {

    private final static Logger logger = Logger.getLogger(ProbePointManager.class);

    /**
     * Singleton partern
     */
    private static ProbePointManager instance = null;

    private static String PROBE_FOLDER = new WorkspaceConfig().fromJson().getProbePointDirectory();
    private static String INFO_JSON = "info.json";

    public static ProbePointManager getInstance() {
        if (instance == null) {
            instance = new ProbePointManager();
        }
        return instance;
    }

    private Map<String, ProbePoint> nameToProbePointMap = new HashMap<>();
    private ObservableMap<String, TreeSet<Integer>> probePointMap = FXCollections.observableHashMap();
    private Map<TestCase, List<ProbePoint>> testCaseToProbePointMap = new HashMap<>();
    private HashMap<IFunctionNode, ArrayList<ProbePoint>> funPpMap = new HashMap<>();

    public void clear() {
        nameToProbePointMap.clear();
        probePointMap.clear();
        funPpMap.clear();
    }

    // called when open existed Environment or after creating an Environment
    public void loadProbePoints() {

        // todo: synchronize PP locations file and probe point files

        boolean importSuccess = importProbePointsLocation();
        if (!importSuccess)
            return;

        File probePointDirectory = new File(PROBE_FOLDER);
        FilenameFilter ppFilter = (f, name) -> name.endsWith("pp");
        FilenameFilter jsonFilter = (f, name) -> name.endsWith("json");

        for (File file : Objects.requireNonNull(probePointDirectory.listFiles(ppFilter))) {
            for (File jsonFile : Objects.requireNonNull(file.listFiles(jsonFilter))) {
                String content = Utils.readFileContent(jsonFile);
                JsonObject jsonObject = (JsonObject) JsonParser.parseString(content);
                ProbePoint probePoint = new ProbePointImporter().importProbePoint(jsonObject);
                IFunctionNode functionNode = probePoint.getFunctionNode();
                if (funPpMap.containsKey(functionNode)) {
                    funPpMap.get(functionNode).add(probePoint);
                } else {
                    ArrayList<ProbePoint> newSet = new ArrayList<>();
                    funPpMap.put(functionNode, newSet);
                }
                nameToProbePointMap.put(probePoint.getName(), probePoint);
                updateTestCasesPPMapAfterAdd(probePoint);
            }
        }
    }

    public void updateTestCasesPPMapAfterAdd(ProbePoint probePoint) {
        updateTestCasesProbePointMap(probePoint, new ArrayList<>(), probePoint.getTestCases());
    }

    public void updateTestCasesPPMapAfterEdit(ProbePoint probePoint, List<TestCase> oldTestCases, List<TestCase> newTestCases) {
        updateTestCasesProbePointMap(probePoint, oldTestCases, newTestCases);
    }

    public void updateTestCasesPPMapAfterRemove(ProbePoint probePoint) {
        updateTestCasesProbePointMap(probePoint, probePoint.getTestCases(), new ArrayList<>());
    }

    private void updateTestCasesProbePointMap(ProbePoint probePoint, List<TestCase> oldTestCases, List<TestCase> newTestCases) {
        for (TestCase testCase : oldTestCases) {
            if (testCaseToProbePointMap.get(testCase) == null) {
                logger.error("An old test case is not in testCaseToProbePointMap.");
                return;
            } else {
                if (!newTestCases.contains(testCase)) {
                    testCaseToProbePointMap.get(testCase).remove(probePoint);
                    if (testCaseToProbePointMap.get(testCase).size() == 0) {
                        testCaseToProbePointMap.remove(testCase);
                    }
                }
            }
        }

        for (TestCase testCase : newTestCases) {
            if (testCaseToProbePointMap.get(testCase) == null) {
                testCaseToProbePointMap.put(testCase, new ArrayList<>());
            }

            if (!testCaseToProbePointMap.get(testCase).contains(probePoint)) {
                testCaseToProbePointMap.get(testCase).add(probePoint);
            }
        }
    }

    public boolean add(ProbePoint probePoint) {
        if (!isExisted(probePoint)) {
            nameToProbePointMap.put(probePoint.getName(), probePoint);

            String path = probePoint.getSourcecodeFileNode().getAbsolutePath();
            if (probePointMap.get(path) == null) {
                probePointMap.put(path, new TreeSet<>());
            }

            probePointMap.get(path).add(probePoint.getLineInSourceCodeFile());
            exportProbePointsLocation();
            return true;
        } else {
            logger.debug("The name of ProbePoint is existed. " + probePoint.getName());
            return false;
        }
    }

    public void addToFunctionMap(ProbePoint probePoint) {
        IFunctionNode functionNode = probePoint.getFunctionNode();
        if (funPpMap.containsKey(functionNode)) {
            funPpMap.get(functionNode).add(probePoint);
        } else {
            ArrayList<ProbePoint> newSet = new ArrayList<ProbePoint>() {{
                add(probePoint);
            }};
            funPpMap.put(functionNode, newSet);
        }
    }

    public void removeFromFunctionMap(ProbePoint probePoint) {
        IFunctionNode functionNode = probePoint.getFunctionNode();
        if (funPpMap.containsKey(functionNode)) {
            funPpMap.get(functionNode).remove(probePoint);
            if (funPpMap.get(functionNode).size() == 0) {
                funPpMap.remove(functionNode);
            }
        } else {
            // maybe never happen
        }
    }

    public boolean isExisted(ProbePoint probePoint) {
        return nameToProbePointMap.containsKey(probePoint.getName());
    }

    public void remove(ProbePoint probePoint) {
        if (nameToProbePointMap.containsKey(probePoint.getName())) {
            nameToProbePointMap.remove(probePoint.getName());
            funPpMap.get(probePoint.getFunctionNode()).remove(probePoint);
            deleteProbePointFile(probePoint);
            updateTestCasesPPMapAfterRemove(probePoint);

            String path = probePoint.getSourcecodeFileNode().getAbsolutePath();
            if (searchProbePoints((SourcecodeFileNode) probePoint.getSourcecodeFileNode(), probePoint.getLineInSourceCodeFile()).size() == 0) {
                probePointMap.get(path).remove(probePoint.getLineInSourceCodeFile());

                if (searchProbePoints((SourcecodeFileNode) probePoint.getSourcecodeFileNode()).size() == 0) {
                    probePointMap.remove(path);
                }
                exportProbePointsLocation();
            }
        }
    }

    public ProbePoint getProbePointByName(String name) {
        return nameToProbePointMap.get(name);
    }

    public List<ProbePoint> searchProbePoints(SourcecodeFileNode sourcecodeFileNode) {
        List<ProbePoint> probePoints = new ArrayList<>();
        for (ProbePoint pp : nameToProbePointMap.values()) {
            if (pp.getSourcecodeFileNode().getAbsolutePath().equals(sourcecodeFileNode.getAbsolutePath())) {
                probePoints.add(pp);
            }
        }
        return probePoints;
    }

    public List<ProbePoint> searchProbePoints(SourcecodeFileNode sourcecodeFileNode, int lineNumber) {
        List<ProbePoint> probePoints = new ArrayList<>();
        for (ProbePoint pp : searchProbePoints(sourcecodeFileNode)) {
            if (pp.getLineInSourceCodeFile() == lineNumber) {
                probePoints.add(pp);
            }
        }
        return probePoints;
    }

    public List<ProbePoint> getAllProbePoint() {
        return new ArrayList<>(nameToProbePointMap.values());
    }

    public void exportProbePointsLocation() {
        String probePointPath = PROBE_FOLDER + File.separator
                + Environment.getInstance().getName()
                + ".pplocations.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(probePointMap);
        Utils.writeContentToFile(json, probePointPath);
    }

    private boolean importProbePointsLocation() {
        String probePointPath = PROBE_FOLDER + File.separator
                + Environment.getInstance().getName()
                + ".pplocations.json";
        if (new File(probePointPath).exists()) {
            String content = Utils.readFileContent(probePointPath);
            Gson gson = new Gson();
            Type empMapType = new TypeToken<HashMap<String, TreeSet<Integer>>>() {
            }.getType();
            probePointMap = FXCollections.observableMap(gson.fromJson(content, empMapType));
            return true;
        } else {
            return false;
        }
    }

    public TreeSet<Integer> getListOfProbePointLines(SourcecodeFileNode sourcecodeFileNode) {
        TreeSet<Integer> treeSet = probePointMap.get(sourcecodeFileNode.getAbsolutePath());
        if (treeSet != null)
            return treeSet;
        else
            return new TreeSet<>();
    }

    public void exportProbePointToFile(ProbePoint probePoint) {
        String content = new ProbePointExporter().export(probePoint);
        Utils.writeContentToFile(content, probePoint.getPath() + File.separator + INFO_JSON);
    }

    public void deleteProbePointFile(ProbePoint probePoint) {
        Utils.deleteFileOrFolder(new File(probePoint.getPath()));
    }

    public List<ProbePoint> getProbePointsByTestCase(TestCase testCase) {
        List<ProbePoint> list = testCaseToProbePointMap.get(testCase);
        if (list == null)
            list = new ArrayList<>();

        return list;
    }

    public HashMap<IFunctionNode, ArrayList<ProbePoint>> getFunPpMap() {
        return funPpMap;
    }
}

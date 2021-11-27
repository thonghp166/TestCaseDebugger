package com.dse.debugger.controller;

import com.dse.debugger.gdb.analyzer.OutputGDB;
import com.dse.debugger.component.breakpoint.BreakPoint;
import com.dse.debugger.component.breakpoint.GDBBreakpointCell;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.apache.log4j.Logger;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class BreakPointController implements Initializable {

    private Logger logger = Logger.getLogger(BreakPointController.class);

    private static BreakPointController breakPointController = null;
    private static AnchorPane pane = null;

    private static void prepare() {
        FXMLLoader loader = new FXMLLoader(Object.class.getResource("/FXML/debugger/BreakpointManager.fxml"));
        try {
            pane = loader.load();
            breakPointController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BreakPointController getBreakPointController(){
        if (breakPointController == null) prepare();
        return breakPointController;
    }

    public static AnchorPane getPane() {
        if (pane == null) prepare();
        return pane;
    }

    private String breakFilePath;

    @FXML
    ListView<BreakPoint> breakListView;

    private ObservableList<BreakPoint> breakList = FXCollections.observableArrayList();

    private ObservableMap<String, TreeSet<BreakPoint>> breakPointMap = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.breakListView.setItems(this.breakList);
        Callback<BreakPoint, ObservableValue<Boolean>> itemToBoolean = BreakPoint::selectedProperty;
        this.breakListView.setCellFactory(e -> new GDBBreakpointCell(itemToBoolean));
        this.breakListView.getSelectionModel().selectedItemProperty()
                .addListener((((observable, oldValue, newValue) -> {
                    if (newValue != null){
                        if (oldValue != newValue){
                            DebugController.getDebugController().openCurrentHitLine(newValue.getLine(),newValue.getFull(),false);
                        }
                    }
                })));
    }

    public void disable(BreakPoint breakPoint){
        OutputGDB outputGDB = DebugController.getDebugController().getGdb().disableBreakPoint(breakPoint);
        if (outputGDB == null){
            logger.debug("Can not get result from GDB");
        } else {
            if (outputGDB.isError()){
                logger.debug("Disable breakpoint number " + breakPoint.getNumber() + " failed");
                // todo: show error dialog
            } else {
                logger.debug("Disable breakpoint number " + breakPoint.getNumber() + " successfully");
                breakPoint.setEnabled("n");
                breakPoint.setSelected(false);
            }
        }
    }

    public void enable(BreakPoint breakPoint){
        OutputGDB outputGDB = DebugController.getDebugController().getGdb().enableBreakPoint(breakPoint);
        if (outputGDB == null){
            logger.debug("Can not get result from GDB");
        } else {
            if (outputGDB.isError()){
                logger.debug("Enable breakpoint number " + breakPoint.getNumber() + " failed");
                // todo: show error dialog
            } else {
                logger.debug("Enable breakpoint number " + breakPoint.getNumber() + " successfully");
                breakPoint.setEnabled("y");
                breakPoint.setSelected(true);
            }
        }
    }

    public void addBreakPoint(BreakPoint breakPoint){
        Platform.runLater(() -> breakList.add(breakPoint));
    }

    public void deleteBreakPoint(BreakPoint breakPoint){
        breakList.remove(breakPoint);
    }

    public BreakPoint searchBreakPoint(int line, String filePath){
        for (BreakPoint e : breakList) {
            if (e.getLine() == line + 1 && e.getFull().equals(filePath)) {
                return e;
            }
        }
        return null;
    }

    public void clearAll() {
        this.breakList.clear();
        this.breakListView.getItems().clear();
        this.breakListView.setItems(this.breakList);
    }

    public void setup(String path) {
        this.breakFilePath = path;
        File breakFile = new File(path);
        if (breakFile.exists()) {
            Gson gson = new Gson();
            String json = Utils.readFileContent(path);
            Type mapType = new TypeToken<HashMap<String, TreeSet<BreakPoint>>>(){}.getType();
            breakPointMap = FXCollections.observableMap(gson.fromJson(json,mapType));
        } else {
            GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.setPrettyPrinting().create();
            String jsonString = gson.toJson(breakPointMap);
            breakPointMap = FXCollections.observableHashMap();
            Utils.writeContentToFile(jsonString,path);
        }
    }

    public ObservableMap<String, TreeSet<BreakPoint>> getBreakPointMap() {
        return breakPointMap;
    }

    public TreeSet<BreakPoint> searchBreaksFromPath(String path) {
        return breakPointMap.get(path);
    }
    public BreakPoint searchBreakFromLineAndPath(String path, int line){
        TreeSet<BreakPoint> breakSet = breakPointMap.get(path);
        if (breakSet == null) return null;
        for (BreakPoint br : breakSet) {
            if (br.getLine() == line) {
                return br;
            }
        }
        return null;
    }

    public void updateList() {
        this.breakList.clear();
        breakPointMap.keySet().forEach(key -> {
            TreeSet<BreakPoint> set = breakPointMap.get(key);
            set.forEach(br -> {
                this.breakList.add(br);
            });
        });
    }
}

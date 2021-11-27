package com.dse.debugger;

import com.dse.debugger.component.breakpoint.BreakPoint;
import com.dse.debugger.controller.DebugController;
import com.dse.debugger.utils.CodeViewHelpers;
import com.dse.debugger.utils.FXCodeView;
import com.dse.project_init.ProjectClone;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DebugTab extends Tab {
    private final CodeArea codeEditor;
    private final String path;

    public String getPath() {
        return path;
    }

    public DebugTab(String path, TreeSet<BreakPoint> breakSet) {
        super();
        String tempPath = path.substring(path.lastIndexOf(File.separator) + 1);
        if (!path.contains("COMPOUND")) {
            tempPath = tempPath.replace(".akaignore.",".");
        } 
        this.setText(tempPath);
        this.path = path;
        StackPane codeAreaPane = new StackPane();
        if (breakSet == null){
            breakSet = new TreeSet<>();
        }
        ObservableSet<BreakPoint> observableBreakpointLines = FXCollections.synchronizedObservableSet(FXCollections.observableSet(breakSet));
        observableBreakpointLines.addListener((SetChangeListener<BreakPoint>) change -> DebugController.getDebugController().updateBreakPointFile());
        codeEditor = FXCodeView.getCodeEditor(codeAreaPane, observableBreakpointLines,path);
        codeEditor.replaceText(0, 0, this.readData(path));
        codeEditor.setEditable(false);
        codeEditor.getStylesheets().add(Object.class.getResource("/css/keywords.css").toExternalForm());
        this.setContent(codeAreaPane);
    }

    public void showLineInViewport(int line, boolean isHit) {
        int start = line - 1;
        int bias = 0;
//        if (start > 5)
//            bias = 5;
        if (isHit){
            CodeViewHelpers.addParagraphStyle(codeEditor,start,"currentPoint");
        }
        double h = codeEditor.getViewportHeight();
        codeEditor.showParagraphInViewport(start);
    }

    public void removeStyleAtLine(int line){
        int start = line -1;
        CodeViewHelpers.tryRemoveParagraphStyle(codeEditor,start,"currentPoint");
    }


    private String readData(String path) {
        List<String> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                data.add(sCurrentLine);
            }

        } catch (IOException e) {
              e.printStackTrace();
        }
        String res = String.join("\n", data);
        res = ProjectClone.simplify(res);
        return res;
    }

}
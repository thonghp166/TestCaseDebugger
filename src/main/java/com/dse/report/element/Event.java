package com.dse.report.element;

import com.dse.gtest.Failure;
import com.dse.parser.object.INode;
import com.dse.report.converter.*;
import com.dse.testdata.Iterator;
import com.dse.testdata.object.SubprogramNode;
import com.dse.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class Event implements IElement {
    private List<IElement> elements = new ArrayList<>();

    private SubprogramNode subprogram;
    private List<Failure> failures;
    private int index;
    private Position pos;
    private int iterator = 1;

    public enum Position {
        FIRST,
        LAST,
        UNKNOWN,
        MIDDLE
    }

    /**
     * Result PASS/ALL
     */
    private int[] results = new int[] {0, 0};

    public Event(SubprogramNode subprogram, List<Failure> failures, int index, Position pos) {
        this.subprogram = subprogram;
        this.failures = failures;
        this.index = index;
        this.pos = pos;
        generate();
    }

    public Event(SubprogramNode subprogram, List<Failure> failures, int index, Position pos, int iterator) {
        this.subprogram = subprogram;
        this.failures = failures;
        this.index = index;
        this.pos = pos;
        this.iterator = iterator;
        generate();
    }

    private void generate() {
        INode sourceNode = Utils.getSourcecodeFile(subprogram.getFunctionNode());
        String unitName = sourceNode.getName();
        String subprogramName = subprogram.getDisplayNameInParameterTree();

        generateEventHeader(elements, index, unitName, subprogramName);

        AssertionConverter assertConverter;

        if (pos == Position.FIRST)
            assertConverter = new InitialAssertionConverter(failures, index);
        else if (pos == Position.LAST)
            assertConverter = new LastAssertionConverter(failures, index);
        else
            assertConverter = new MiddleAssertionConverter(failures, index, iterator);

        Table assertTable = assertConverter.execute(subprogram);

        results = assertConverter.getResults();
        elements.add(assertTable);
    }

    public static void generateEventHeader(List<IElement> body, int index, String unitName, String subprogramName) {
        Section.Line title = new Section.CenteredLine(new Text("Event " + index, TEXT_STYLE.BOLD), COLOR.MEDIUM);
        body.add(title);

//        Table headerRow = new Table();
//        headerRow.getRows().add(new Table.HeaderRow("Parameter", "Type", "Expected Value", "Actual Value"));
//        body.add(headerRow);
//
//        body.add(new Section.Line("Calling UUT: " + unitName, COLOR.LIGHT));
//        body.add(new Section.Line(Converter.generateTab(1) + "Subprogram: " + subprogramName, COLOR.LIGHT));
    }

    public int[] getResults() {
        return results;
    }

    @Override
    protected Event clone() {
        return new Event(subprogram, failures, index, pos);
    }

    public List<IElement> getElements() {
        return elements;
    }

    @Override
    public String toHtml() {
        String html = "";

        for (IElement element : elements) {
            html += element.toHtml();
        }

        return html;
    }
}

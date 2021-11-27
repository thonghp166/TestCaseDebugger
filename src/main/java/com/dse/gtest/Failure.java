package com.dse.gtest;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.Arrays;
import java.util.List;


@XmlType(propOrder = {"message"})
public class Failure {

    @XmlAttribute
    private String message;

    public String[] getMessageLines() {
        if (message == null)
            return null;

        String[] lines = message.split("\\R");

        for (int i = 0; i < lines.length; i++)
            lines[i] = lines[i].trim();

        return lines;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        String output = "Failure: ";

        for (String line : getMessageLines()) {
            output += line + "\n";
        }

        return output;
    }
}

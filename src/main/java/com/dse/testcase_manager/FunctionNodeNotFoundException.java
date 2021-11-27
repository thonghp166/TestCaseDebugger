package com.dse.testcase_manager;

public class FunctionNodeNotFoundException extends Exception{
    private String functionPath;

    public FunctionNodeNotFoundException(String functionPath) {
        this.functionPath = functionPath;
    }

    public String getFunctionPath() {
        return functionPath;
    }
}

package com.dse.testdata.object;

import com.dse.project_init.ProjectClone;
import com.dse.stub_manager.SystemLibrary;

import java.io.File;

/**
 * Other unsupported data types
 */
public class OtherUnresolvedDataNode extends UnresolvedDataNode {
    private String userCode = null;

    @Override
    public String getInputForGoogleTest() {
        if (userCode == null || userCode.trim().length() == 0)
            return "";

        if (userCode.trim().endsWith(DEFAULT_USER_CODE)) {
            return "/*No code*/";
        } else {
            String normalize = userCode.replace(VALUE_TAG, getVituralName());
            return normalize;
        }
    }

    @Override
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    @Override
    public String getUserCode() {
        if (userCode == null)
            userCode = generateInitialUserCode() + DEFAULT_USER_CODE;

        return userCode;
    }

    @Override
    public String getContextPath() {
        UnitNode unitNode = getUnit();
        String filePath;

        if (unitNode != null) {
            filePath = unitNode.getSourceNode().getAbsolutePath();
        } else {
            String[] pathItems = getCorrespondingVar().getAbsolutePath().split(File.separator);
            filePath = pathItems[1];
        }

        return filePath;
    }

    /**
     * [0] included path
     * [1] temporary path
     */
    @Override
    public String getTemporaryPath() {
        UnitNode unitNode = getUnit();
        String filePath;
        String temporaryPath;

        if (unitNode != null) {
            filePath = unitNode.getSourceNode().getAbsolutePath();
            temporaryPath = ProjectClone.getClonedFilePath(filePath);
        } else {
            String[] pathItems = getCorrespondingVar().getAbsolutePath().split(File.separator);
            filePath = pathItems[1];
            temporaryPath = SystemLibrary.getLibrariesDirectory() + filePath + SystemLibrary.LIBRARY_EXTENSION;
        }

        int lastSeparator = temporaryPath.lastIndexOf(File.separator) + 1;
        temporaryPath = temporaryPath.substring(0, lastSeparator) + "temporary.cpp";

        return temporaryPath;
    }

    @Override
    public String generateInitialUserCode() {
        String input = "";

        String typeVar = getType();

        if (isExternel())
            typeVar = "";

        // generate the statement
        if (this.isPassingVariable()) {
            input += typeVar + " " + getVituralName() + " = ";

        } else if (this.isAttribute()) {
            input += getVituralName() + " = ";

        } else if (this.isArrayElement()) {
            input += getVituralName() + " = ";

        } else if (isSTLListBaseElement()) {
            input += typeVar + " " + getVituralName() + " = ";

        } else if (this.isInConstructor()) {
            input += typeVar + " " + getVituralName() + " = ";

        }

        return input;
    }
}

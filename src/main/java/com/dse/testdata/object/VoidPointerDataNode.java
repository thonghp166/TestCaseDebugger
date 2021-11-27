package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.project_init.ProjectClone;
import com.dse.stub_manager.SystemLibrary;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.io.File;

/**
 * for "void*"
 */
public class VoidPointerDataNode extends UnresolvedDataNode {
    private String userCode = null;

    @Override
    public String getInputForGoogleTest() {
        if (userCode == null || userCode.trim().length() == 0)
            return "";

        if (userCode.trim().endsWith(DEFAULT_USER_CODE)) {
            return "/*No code*/";
        } else {
            if (userCode.contains("=") && userCode.indexOf("=") == userCode.lastIndexOf("=")) {

                if (Environment.getInstance().getCompiler().isGPlusPlusCommand()){
                    // Case 1: parameter "void* data = 2222;"---> "auto xxx = 222; void* data = &xxx";
                    // Case 2: parameter "s.data = 2222;" (s.data is void*)---> "auto xxx = 222; s.data = &xxx";
                    String initialization = userCode.substring(userCode.indexOf("=") + 1).trim();

                    String newVar = "voidPointerTmp" + new RandomDataGenerator().nextInt(0, 999999);
                    String stm = "auto " + newVar + "=" + initialization;
                    if (!stm.endsWith(";"))
                        stm += ";";

                    String normalize = "";
                    if (this.isPassingVariable())
                        normalize = String.format("%s \n %s %s = &%s;", stm, getType(), getVituralName(), newVar);
                    else
                        normalize = String.format("%s \n %s = &%s;", stm, getVituralName(), newVar);
                    return normalize;

                } else if (Environment.getInstance().getCompiler().isGccCommand()){
                    // Case 1: parameter "void* data = 2222;"---> "void* xxx = 222; void* data = xxx";
                    // Case 2: parameter "s.data = 2222;" (s.data is void*)---> "void* xxx = 222; s.data = xxx";

                    String initialization = userCode.substring(userCode.indexOf("=") + 1).trim();

                    String newVar = "voidPointerTmp" + new RandomDataGenerator().nextInt(0, 999999);
                    String stm = "void* " + newVar + "=" + initialization;
                    if (!stm.endsWith(";"))
                        stm += ";";

                    String normalize = "";
                    if (this.isPassingVariable()) // case 1
                        normalize = String.format("%s \n %s %s = %s;", stm, getType(), getVituralName(), newVar);
                    else // case 2
                        normalize = String.format("%s \n %s = %s;", stm, getVituralName(), newVar);
                    return normalize;
                }
            }
                return "/* Do not know how to create initialization of void pointer */";
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

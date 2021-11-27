package com.dse.testdata.object;

public interface IUserCodeNode {
    String DEFAULT_USER_CODE = "/* Write your own definition */";
    String VALUE_TAG = "<<value>>";

    /**
     * User Code should write in a specific context
     * Ex: unit under test
     */
    String getContextPath();

    /**
     * @return temporary file path where archive user code file
     */
    String getTemporaryPath();

    /**
     * @return initial user code with only declaration
     */
    String generateInitialUserCode();

    void setUserCode(String userCode);
    String getUserCode();
}

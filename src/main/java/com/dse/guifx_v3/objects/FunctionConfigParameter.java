package com.dse.guifx_v3.objects;

import auto_testcase_generation.config.PointerOrArrayBound;
import auto_testcase_generation.config.PrimitiveBound;
import com.dse.config.FunctionConfig;
import com.dse.config.IFunctionConfig;
import com.dse.config.IFunctionConfigBound;
import com.dse.config.UndefinedBound;
import com.dse.guifx_v3.helps.Environment;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.object.IVariableNode;
import com.dse.util.AkaLogger;
import com.dse.util.bound.DataSizeModel;

import java.util.ArrayList;
import java.util.List;

public class FunctionConfigParameter {
    private final static AkaLogger logger = AkaLogger.get(FunctionConfigParameter.class);
    private FunctionConfig functionConfig;
    private String param;
    private String value;

    public FunctionConfigParameter(FunctionConfig functionConfig, String param, String value) {
        if (functionConfig != null) {
            this.functionConfig = functionConfig;
            if (validateParam(param)) {
                this.param = param;
                setValue(value);
            }
        }
    }

    public String getParam() {
        return param;
    }

    public String getValue() {
        return value;
    }


    public boolean setValue(String value) {
        switch (param) {
            case TEST_DATA_GEN_STRATEGY: {
//                if (value.equals(IFunctionConfig.TEST_DATA_GENERATION_STRATEGIES.DART)
//                        || value.equals(IFunctionConfig.TEST_DATA_GENERATION_STRATEGIES.DIRECTED_DIJKSTRA)) {
//                    UIController.showErrorDialog("Does not support this option in this version", "Function configuration", "Do not support");
//                    return false;
//                } else {
                    functionConfig.setTestdataGenStrategy(value);
                    this.value = value;
                    return true;
//                }
            }

            case THE_MAXIMUM_NUMBER_OF_ITERATIONS: {
                boolean isOK = checkNumberOfIteration(param, value, functionConfig);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case CHARACTER_BOUND_LOWER: {
                String upper = functionConfig.getBoundOfOtherCharacterVars().getUpper();
                boolean isOK = checkBoundOfCharacterAndNumber(param, value, upper, functionConfig.getBoundOfOtherCharacterVars(), null);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case CHARACTER_BOUND_UPPER: {
                String lower = functionConfig.getBoundOfOtherCharacterVars().getLower();
                boolean isOK = checkBoundOfCharacterAndNumber(param, lower, value, functionConfig.getBoundOfOtherCharacterVars(), null);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case NUMBER_BOUND_LOWER: {
                String upper = functionConfig.getBoundOfOtherNumberVars().getUpper();
                boolean isOK = checkBoundOfCharacterAndNumber(param, value, upper, functionConfig.getBoundOfOtherNumberVars(), null);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case NUMBER_BOUND_UPPER: {
                String lower = functionConfig.getBoundOfOtherNumberVars().getLower();
                boolean isOK = checkBoundOfCharacterAndNumber(param, lower, value, functionConfig.getBoundOfOtherNumberVars(), null);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case LOWER_BOUND_OF_OTHER_ARRAYS: {
                String upper = functionConfig.getBoundOfArray().getUpper();
                boolean isOK = checkBoundOfArrayorPointer(param, value, upper, functionConfig.getBoundOfArray(), 1);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }
            case UPPER_BOUND_OF_OTHER_ARRAYS: {
                String lower = functionConfig.getBoundOfArray().getLower();
                boolean isOK = checkBoundOfArrayorPointer(param, lower, value, functionConfig.getBoundOfArray(), 1);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case LOWER_BOUND_OF_OTHER_POINTERS: {
                String upper = functionConfig.getBoundOfPointer().getUpper();
                boolean isOK = checkBoundOfArrayorPointer(param, value, upper, functionConfig.getBoundOfPointer(), 0);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            case UPPER_BOUND_OF_OTHER_POINTERS: {
                String lower = functionConfig.getBoundOfPointer().getLower();
                boolean isOK = checkBoundOfArrayorPointer(param, lower, value, functionConfig.getBoundOfPointer(), 0);
                if (isOK) {
                    this.value = value;
                    return true;
                } else
                    return false;
            }

            default: {
                if (param != null && param.length() > 0) {
                    boolean isOK = setValueOfArguments(functionConfig, param, value);
                    if (isOK) {
                        this.value = value;
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private boolean setValueOfArguments(FunctionConfig functionConfig, String param, String value) {
        value = value.trim();
        String nameArg = param.replace(IFunctionConfigBound.ARGUMENT_SIZE, "");

        // Find the corresponding argument
        IVariableNode correspondingVar = null;
        List<IVariableNode> arguments = functionConfig.getFunctionNode().getArguments();
        for (IVariableNode argument : arguments)
            if (argument.getName().equals(nameArg)) {
                correspondingVar = argument;
                break;
            }

        //
        IFunctionConfigBound params = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(nameArg);
        if (params != null && correspondingVar != null) {
            // CASE 1.
            if (params instanceof PrimitiveBound) {
                String lower = null, upper = null;
                DataSizeModel dataSizeModel = Environment.getBoundOfDataTypes().getBounds();
                PrimitiveBound dataSize = dataSizeModel.get(correspondingVar.getReducedRawType());

                if (value.contains(IFunctionConfigBound.RANGE_DELIMITER)) {
                    lower = value.split(IFunctionConfigBound.RANGE_DELIMITER)[0];
                    upper = value.split(IFunctionConfigBound.RANGE_DELIMITER)[1];
                    boolean isOK = checkBoundOfCharacterAndNumber(param, lower, upper, (PrimitiveBound) params, dataSize);
                    return isOK;
                } else {
                    try {
                        Double tmp = Double.parseDouble(value);
                        lower = value;
                        upper = value;

                        boolean isOK = checkBoundOfCharacterAndNumber(param, lower, upper, (PrimitiveBound) params, dataSize);
                        return isOK;
                    } catch (NumberFormatException e) {
                        UIController.showErrorDialog("The bound of variable " + nameArg + " is not valid"
                                , "Invalid bound", "Wrong bound");
                        return false;
                    }
                }


            } else if (params instanceof PointerOrArrayBound) {
                List<String> indexes = new ArrayList<>();
                if (value.contains(IFunctionConfigBound.INDEX_DELIMITER))
                    for (String token : value.split(IFunctionConfigBound.INDEX_DELIMITER)) {
                        indexes.add(token);
                    }
                else indexes.add(value);
                ((PointerOrArrayBound) params).setIndexes(indexes);
                return true;

            } else if (params instanceof UndefinedBound){
                return true;
            }

        }
        return false;
    }

    private boolean checkNumberOfIteration(String param, String iteration, FunctionConfig functionConfig) {
        try {
            long iterationNum = Long.parseLong(iteration);
            if (iterationNum <= 0) {
                UIController.showErrorDialog("The number of iterations " + param + " must be a positive integer"
                        , "Invalid value", "Wrong number of iteration");
                return false;
            } else {
                functionConfig.setTheMaximumNumberOfIterations(iterationNum);
                return true;
            }
        } catch (NumberFormatException e) {
            UIController.showErrorDialog("The number of iterations " + param + " must be a positive integer"
                    , "Invalid value", "Wrong number of iteration");
            return false;
        }
    }

    private boolean checkBoundOfArrayorPointer(String param, String lower, String upper, PrimitiveBound bound, int validMin) {
        try {
            long lowerTmp = Long.parseLong(lower);
            long upperTmp = Long.parseLong(upper);

            if (lowerTmp < validMin || upperTmp < validMin) {
                UIController.showErrorDialog(
                        "Reason: The lower value and the upper value must be greater or equal to " + validMin +
                                " (lower value = " + lower + ", upper value = " + upper + ")"
                        , "Invalid value of " + param, "Wrong value of " + param);
                return false;
            } else if (lowerTmp <= upperTmp) {
                bound.setUpper(upper);
                bound.setLower(lower);
                return true;
            } else {
                UIController.showErrorDialog(
                        "Reason: The lower value and the upper value are not matched! " +
                                "(lower value = " + lower + ", upper value = " + upper + ")"
                        , "Invalid value of " + param, "Wrong value of " + param);
                return false;
            }

        } catch (NumberFormatException e) {
            UIController.showErrorDialog("The upper value (" + upper + ") and the lower value (" + lower + ") of array bound must be integer"
                    , "Invalid value of " + param, "Wrong value of " + param);
            return false;
        }
    }

    private boolean checkBoundOfCharacterAndNumber(String param, String lower, String upper, PrimitiveBound bound, PrimitiveBound validRange) {
        if (upper == null || lower == null) {
            UIController.showErrorDialog("Error"
                    , "Invalid value of " + param, "Wrong value of " + param);
            return false;

        }
        // CASE 1
        else if (lower.equals(IFunctionConfigBound.MIN_VARIABLE_TYPE)) {
            if (upper.equals(IFunctionConfigBound.MIN_VARIABLE_TYPE) || upper.equals(IFunctionConfigBound.MAX_VARIABLE_TYPE)) {
                bound.setLower(lower);
                bound.setUpper(upper);
                return true;
            } else {
                try {
                    Double upperDouble = Double.parseDouble(upper);
                    bound.setLower(lower);
                    bound.setUpper(upper);
                    return true;
                } catch (Exception e) {
                    UIController.showErrorDialog(
                            "Reason: The lower value and the upper value are not matched! " +
                                    "(lower value = " + lower + ", upper value = " + upper + ")"
                            , "Invalid value of " + param, "Wrong value of " + param);
                    return false;
                }
            }
        }
        // CASE 2
        else if (lower.equals(IFunctionConfigBound.MAX_VARIABLE_TYPE)) {
            if (upper.equals(IFunctionConfigBound.MAX_VARIABLE_TYPE)) {
                bound.setLower(lower);
                bound.setUpper(upper);
                return true;
            } else {
                UIController.showErrorDialog(
                        "Reason: The lower value and the upper value are not matched! " +
                                "(lower value = " + lower + ", upper value = " + upper + ")"
                        , "Invalid value of " + param, "Wrong value of " + param);
                return false;
            }
        }
        // CASE 3. Lower value is a number or a string
        else {
            try {
                Double lowerDouble = Double.parseDouble(lower);
                if (validRange != null && lowerDouble < validRange.getLowerAsLong()) {
                    UIController.showErrorDialog(
                            "Lower " + lower + " must be >= " + validRange.getLowerAsLong()
                            , "Invalid value of " + param, "Wrong value of " + param);
                    return false;
                } else
                    try {
                        if (upper.equals(IFunctionConfigBound.MIN_VARIABLE_TYPE)) {
                            UIController.showErrorDialog(
                                    "Reason: The lower value and the upper value are not matched! "
                                            + "(lower value = " + lower + ", upper value = " + upper + ")"
                                    , "Invalid value of " + param, "Wrong value of " + param);
                            return false;

                        } else if (upper.equals(IFunctionConfigBound.MAX_VARIABLE_TYPE)) {
                            bound.setLower(lower);
                            bound.setUpper(upper);
                            return true;

                        } else {
                            Double upperDouble = Double.parseDouble(upper);

                            if (validRange != null && upperDouble > validRange.getUpperAsLong()) {
                                UIController.showErrorDialog(
                                        "Upper " + upper + " must be <=" + validRange.getUpperAsLong()
                                        , "Invalid value of " + param, "Wrong value of " + param);
                                return false;
                            } else if (lowerDouble <= upperDouble) {
                                bound.setLower(lower);
                                bound.setUpper(upper);
                                return true;
                            } else {
                                UIController.showErrorDialog(
                                        "Reason: The lower value and the upper value are not matched! "
                                                + "(lower value = " + lower + ", upper value = " + upper + ")"
                                        , "Invalid value of " + param, "Wrong value of " + param);
                                return false;
                            }
                        }
                    } catch (Exception e1) {
                        UIController.showErrorDialog("Reason: The value of upper bound " + upper + " is a string"
                                , "Invalid value of " + param, "Wrong value of " + param);
                        return false;
                    }
            } catch (Exception e2) {
                UIController.showErrorDialog("Reason: The value of lower bound " + lower + " is a string"
                        , "Invalid value of " + param, "Wrong value of " + param);
                return false;
            }
        }
    }

    private boolean validateParam(String param) {
        return true;
    }

    static String[] getTestDataExecStrategies() {
        return new String[]{IFunctionConfig.TEST_DATA_EXECUTION_STRATEGIES.MULTIPLE_COMPILATION, IFunctionConfig.TEST_DATA_EXECUTION_STRATEGIES.SINGlE_COMPILATION};
    }

    static String[] getSolvingStrategies() {
        return new String[]{IFunctionConfig.SUPPORT_SOLVING_STRATEGIES.USER_BOUND_STRATEGY, IFunctionConfig.SUPPORT_SOLVING_STRATEGIES.Z3_STRATEGY};
    }

    static String[] getTestDataGenStrategies() {
        return new String[]{IFunctionConfig.TEST_DATA_GENERATION_STRATEGIES.RANDOM, IFunctionConfig.TEST_DATA_GENERATION_STRATEGIES.DIRECTED_DIJKSTRA};
    }

    // parameters'name
    public static final String CHARACTER_BOUND_LOWER = "CHARACTER_BOUND_LOWER";
    public static final String CHARACTER_BOUND_UPPER = "CHARACTER_BOUND_UPPER";
    public static final String NUMBER_BOUND_LOWER = "INTEGER_BOUND_LOWER";
    public static final String NUMBER_BOUND_UPPER = "INTEGER_BOUND_UPPER";
    public static final String SOLVING_STRATEGY = "SOLVING_STRATEGY";
    public static final String TEST_DATA_GEN_STRATEGY = "STRATEGY";
    public static final String TEST_DATA_EXEC_STRATEGY = "TEST_DATA_EXEC_STRATEGY";
    public static final String THE_MAXIMUM_NUMBER_OF_ITERATIONS = "THE_MAXIMUM_NUMBER_OF_ITERATIONS";

    public static final String LOWER_BOUND_OF_OTHER_ARRAYS = "ARRAY_SIZE_LOWER";
    public static final String UPPER_BOUND_OF_OTHER_ARRAYS = "ARRAY_SIZE_UPPER";

    public static final String LOWER_BOUND_OF_OTHER_POINTERS = "POINTER_SIZE_LOWER";
    public static final String UPPER_BOUND_OF_OTHER_POINTERS = "POINTER_SIZE_UPPER";

}

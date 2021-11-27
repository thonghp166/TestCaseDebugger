package com.dse.config;

import auto_testcase_generation.config.PrimitiveBound;
import com.dse.parser.object.ICommonFunctionNode;

import java.util.Map;

/**
 * Represent configuration of a function
 *
 * @author DucAnh
 */
public interface IFunctionConfig {

    class SUPPORT_SOLVING_STRATEGIES {
        public static final String Z3_STRATEGY = "Z3_STRATEGY";
        public static final String USER_BOUND_STRATEGY = "USER_BOUND_STRATEGY";
    }

    class TEST_DATA_GENERATION_STRATEGIES {
        public static final String DIRECTED_DIJKSTRA = "DIRECTED_DIJKSTRA";
        public static final String RANDOM = "RANDOM";
    }

    class TEST_DATA_EXECUTION_STRATEGIES {
        public static final String SINGlE_COMPILATION = "SINGLE_COMPILATION";
        public static final String MULTIPLE_COMPILATION = "MULTIPLE_COMPILATION";
    }

    String getSolvingStrategy();

    void setSolvingStrategy(String solvingStrategy);

    void setTestdataGenStrategy(String testdataGenStrategy);

    String getTestdataGenStrategy();

    void setTestdataExecStrategy(String testdataExecStrategy);

    ICommonFunctionNode getFunctionNode();

    void setFunctionNode(ICommonFunctionNode functionNode);

    long getTheMaximumNumberOfIterations();

    void setTheMaximumNumberOfIterations(long theMaximumNumberOfIterations);

    Map<String, IFunctionConfigBound> getBoundOfArgumentsAndGlobalVariables();

    void setBoundOfArguments(Map<String, IFunctionConfigBound> boundOfArguments);

    PrimitiveBound getBoundOfOtherNumberVars();

    void setBoundOfOtherNumberVars(PrimitiveBound boundOfOtherNumberVars);

    void setBoundOfOtherCharacterVars(PrimitiveBound boundOfOtherCharacterVars);

    PrimitiveBound getBoundOfOtherCharacterVars();

    PrimitiveBound getBoundOfArray();

    void setBoundOfArray(PrimitiveBound boundOfArray);

    PrimitiveBound getBoundOfPointer();

    void setBoundOfPointer(PrimitiveBound boundOfPointer);

    void createDefaultConfig(ICommonFunctionNode functionNode);
}
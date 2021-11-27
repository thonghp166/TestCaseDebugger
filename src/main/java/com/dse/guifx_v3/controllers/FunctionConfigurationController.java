package com.dse.guifx_v3.controllers;

import auto_testcase_generation.config.PointerOrArrayBound;
import auto_testcase_generation.config.PrimitiveBound;
import com.dse.config.FunctionConfig;
import com.dse.config.IFunctionConfigBound;
import com.dse.config.UndefinedBound;
import com.dse.guifx_v3.objects.ConfigFunctionValueColumnCellFactory;
import com.dse.guifx_v3.objects.FunctionConfigParameter;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FunctionConfigurationController implements Initializable {

    @FXML
    private TreeTableView<FunctionConfigParameter> ttvFunctionConfiguration;

    @FXML
    private TreeTableColumn<FunctionConfigParameter, String> colParameter;

    @FXML
    private TreeTableColumn<FunctionConfigParameter, String> colValue;

    private TreeItem<FunctionConfigParameter> root = new TreeItem<>();

    public void initialize(URL location, ResourceBundle resources) {
        ttvFunctionConfiguration.setRoot(root);

        colParameter.setCellValueFactory(param -> {
            if (param.getValue().getValue() != null) {
                String name = param.getValue().getValue().getParam();
                return new SimpleStringProperty(name);
            } else return new SimpleStringProperty();
        });
    }

    public void loadContent(FunctionConfig functionConfig) {
        colValue.setCellFactory(new ConfigFunctionValueColumnCellFactory(functionConfig));

        List<FunctionConfigParameter> parameters = new ArrayList<>();

        // bound
        Map<String, IFunctionConfigBound> bounds = functionConfig.getBoundOfArgumentsAndGlobalVariables();
        for (String key : bounds.keySet()) {
            IFunctionConfigBound b = bounds.get(key);
            if (b instanceof PrimitiveBound)
                parameters.add(new FunctionConfigParameter(functionConfig, key,
                        ((PrimitiveBound) b).getLower() + IFunctionConfigBound.RANGE_DELIMITER + ((PrimitiveBound) b).getUpper()));
            else if (b instanceof PointerOrArrayBound) {
                parameters.add(new FunctionConfigParameter(functionConfig, key + IFunctionConfigBound.ARGUMENT_SIZE, ((PointerOrArrayBound) b).showIndexes()));
            } else if (b instanceof UndefinedBound)
                parameters.add(new FunctionConfigParameter(functionConfig, key, ((UndefinedBound) b).show()));
        }


        // others
        parameters.add(new FunctionConfigParameter(functionConfig,"", ""));

        String testDataGenStrategy = functionConfig.getTestdataGenStrategy();
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.TEST_DATA_GEN_STRATEGY, testDataGenStrategy));

        String theMaximumNumerOfIterations = String.valueOf(functionConfig.getTheMaximumNumberOfIterations());
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.THE_MAXIMUM_NUMBER_OF_ITERATIONS, theMaximumNumerOfIterations));

        parameters.add(new FunctionConfigParameter(functionConfig,"", ""));
        String characterBoundLower = String.valueOf(functionConfig.getBoundOfOtherCharacterVars().getLower());
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.CHARACTER_BOUND_LOWER, characterBoundLower));

        String characterBoundUpper = String.valueOf(functionConfig.getBoundOfOtherCharacterVars().getUpper());
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.CHARACTER_BOUND_UPPER, characterBoundUpper));

        parameters.add(new FunctionConfigParameter(functionConfig,"", ""));
        String numberBoundLower = String.valueOf(functionConfig.getBoundOfOtherNumberVars().getLower());
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.NUMBER_BOUND_LOWER, numberBoundLower));

        String numberBoundUpper = String.valueOf(functionConfig.getBoundOfOtherNumberVars().getUpper());
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.NUMBER_BOUND_UPPER, numberBoundUpper));

        parameters.add(new FunctionConfigParameter(functionConfig,"", ""));
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.LOWER_BOUND_OF_OTHER_ARRAYS,
                String.valueOf(functionConfig.getBoundOfArray().getLower())));

        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.UPPER_BOUND_OF_OTHER_ARRAYS,
                String.valueOf(functionConfig.getBoundOfArray().getUpper())));

        parameters.add(new FunctionConfigParameter(functionConfig,"", ""));
        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.LOWER_BOUND_OF_OTHER_POINTERS,
                String.valueOf(functionConfig.getBoundOfPointer().getLower())));

        parameters.add(new FunctionConfigParameter(functionConfig, FunctionConfigParameter.UPPER_BOUND_OF_OTHER_POINTERS,
                String.valueOf(functionConfig.getBoundOfPointer().getUpper())));
        //
        for (FunctionConfigParameter param : parameters) {
            TreeItem<FunctionConfigParameter> item = new TreeItem<>(param);
            root.getChildren().add(item);
        }
    }
}

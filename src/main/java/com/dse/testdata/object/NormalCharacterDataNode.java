package com.dse.testdata.object;

import com.dse.util.AkaLogger;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

import java.util.HashMap;
import java.util.Map;

public class NormalCharacterDataNode extends NormalDataNode {
//    public static String SPECIAL_CHAR = "\\\\[ntvbrfa0]";
//    public static String OCTAL_NUMBER_PREFIX = "/";
    public static String VISIBLE_CHARACTER_PREFIX = "'";
//    public static String OCTAL_NUMBER = OCTAL_NUMBER_PREFIX + "[0-9]+";
//    public static String HEXA_NUMBER = "0x[0-9A-Za-z]+"; // ???

    private final static AkaLogger logger = AkaLogger.get(NormalCharacterDataNode.class);

    public static String RULE = "\nRules:" +
            "\n\t- For visible character, type with `'`. Example: 'a', 'b', 'c'" +
            "\n\t- For ascii, its value. Example: 100, 200, 300" +
            "\n\t- For special character such as tab or line break, type '\\t' or '\\n'";

    @Override
    public String getInputForGoogleTest() throws Exception {
        String input = super.getInputForGoogleTest();

        // get type of variable
        String typeVar = VariableTypeUtils.deleteStorageClasses(this.getType())
                .replace(IDataNode.REFERENCE_OPERATOR, "");
        typeVar = VariableTypeUtils.deleteStorageClasses(typeVar);
        typeVar = VariableTypeUtils.deleteStructKeyword(typeVar);
        typeVar = VariableTypeUtils.deleteUnionKeyword(typeVar);

        if (this.getValue() != null) {
            if (isExternel())
                typeVar = "";

            String valueVar = getValue();
//            String valueVar = "";
//            if (getValue().matches(OCTAL_NUMBER))
//                valueVar = getValue().substring(1);
//
//            else if (getValue().matches(SPECIAL_CHAR)){
//                valueVar = NormalDataNode.CHARACTER_QUOTE + "\\" + getValue() + NormalDataNode.CHARACTER_QUOTE;
//
//            } else if (getValue().equals("\\")) {
//                valueVar = 92 + "";
//
//            } else if (getValue().equals("\"")) {
//                valueVar = 34 + "";
//
//            }  else if (getValue().equals("'")) {
//                valueVar = 39 + "";
//
//            } else if (getValue().length() == 1)
//                // character
//                valueVar = NormalDataNode.CHARACTER_QUOTE + getValue() + NormalDataNode.CHARACTER_QUOTE;

            // generate the statement
            if (this.isPassingVariable()) {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isAttribute()) {
                input += this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isArrayElement()) {
                input += this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (isSTLListBaseElement()) {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isInConstructor()){
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

            } else {
                input += typeVar + " " + this.getVituralName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;
            }
        } else if (isPassingVariable()) {
            input += typeVar + " " + getVituralName() + SpecialCharacter.END_OF_STATEMENT;
        }


        return input + SpecialCharacter.LINE_BREAK;
    }

    @Override
    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
        String assertion = "";//super.getAssertionForGoogleTest();

        if (getValue() != null || getVituralName().equals(IGTestConstant.EXPECTED_OUTPUT)) {
            String actualOutputName = getVituralName().replace(source, target);

            assertion += method + "(" + getVituralName() + "," + actualOutputName + ")" + IGTestConstant.LOG_FUNCTION_CALLS;
        }

        return assertion;
    }

    public static Map<String, String> getCharacterToACSIIMapping() {
        Map<String, String> mapping = new HashMap<>();
        for (long ascii = 33; ascii <= 126; ascii++)
            mapping.put((char) ascii + "", ascii +"");

        mapping.put("\\t", "9");
        mapping.put("\\n", "10");
        mapping.put("\\r", "11");
        mapping.put(" ", "32");
        return mapping;
    }

    public static String toASCII(String character) throws Exception{
        if (character.startsWith(VISIBLE_CHARACTER_PREFIX)) {
            character = character.substring(1, character.length() - 1);
            Map<String, String> mapping = getCharacterToACSIIMapping();
            return mapping.get(character);
        } else
            return Long.parseLong(character) + "";
    }
}

package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.experiment2.generateAssertions.AssertionReport;
import es.us.isa.jsonmutator.experiment2.generateAssertions.MutantTestCaseReport;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import org.checkerframework.checker.units.qual.A;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadVariablesValues.getVariableValues;
import static es.us.isa.jsonmutator.experiment2.ReadVariablesValues.jsonNodeToList;

public class GenerateAssertions {

    // Given a test case/operation
    // Given a set of invariants
    // For each invariant
    //      Extract the variable values
    //      Generate an assertion
    //      Consider null values

    public static List<String> valuesToConsiderNull = Arrays.asList("N/A", null);

    private static String invariantsPath = "src/test/resources/test_suites/Spotify/createPlaylist/invariants_100_modified.csv";
//    private static String testCasesPath = "src/test/resources/test-case-omdb.csv";
    private static String testCasesPath = "src/test/resources/test_suites/Spotify/createPlaylist/Spotify_CreatePlaylist_50.csv";


    public static void main(String[] args) throws Exception {

        List<TestCase> testCases = readTestCasesFromPath(testCasesPath);
        List<InvariantData> invariantDataList = getInvariantsDataFromPath(invariantsPath);

        // TODO: Manage the exceptions properly
        // For every test case, check all the assertions
        for(TestCase testCase: testCases) {
            MutantTestCaseReport mutantTestCaseReport = generateAssertions(testCase, invariantDataList);
            // TODO: Add the assertion report as row to the report csv file

//            if(mutantTestCaseReport.isKilled()) {
            System.out.println("MUTANT TEST CASE REPORT:");
            System.out.println("Killed: " + mutantTestCaseReport.isKilled());
            System.out.println("Killed by: " + mutantTestCaseReport.getKilledBy());
            System.out.println("Description: " + mutantTestCaseReport.getDescription());
//            }

        }

    }

    // TODO: BOOLEANS ARE CONVERTED INTO NUMBERS (ints)


    // For all the invariants of a test case
    private static MutantTestCaseReport generateAssertions(TestCase testCase, List<InvariantData> invariantDataList) throws Exception {

        // Check whether any of the invariants is able to kill the mutant
        for(InvariantData invariantData: invariantDataList) {
            try {
                System.out.println(invariantData.getInvariant());
                // If a single invariant is violated, the mutant is killed
                // Return AssertionReport where killed=true and killedBy=invariantData
                MutantTestCaseReport mutantTestCaseReport = generateAssertionsForSingleInvariantData(testCase, invariantData);
                if(mutantTestCaseReport.isKilled()) {
                    return mutantTestCaseReport;
                }

            } catch (Exception e) {
                throw new Exception(e);
            }


        }


        return new MutantTestCaseReport();
    }

    // For a single invariant (InvariantData)
    private static MutantTestCaseReport generateAssertionsForSingleInvariantData(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariantType = invariantData.getInvariantType();

        // Based on the invariantType value, call a specific function
        try {

            // Read configuration file
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonProperties = objectMapper.readTree(new String(Files.readAllBytes(Paths.get("src/main/java/es/us/isa/jsonmutator/experiment2/assertionFunctions.json"))));

            // Obtain the function assigned to the string value
            String functionName = jsonProperties.get(invariantType).textValue();

            // Invoke the corresponding assertion function
//            Method method = findAssertionMethod(functionName);    // TODO: IMPLEMENT
            Method method = GenerateAssertions.class.getMethod(functionName, TestCase.class, InvariantData.class);
            AssertionReport assertionReport = (AssertionReport) method.invoke(null, testCase, invariantData);

            System.out.println(assertionReport.isSatisfied());
            System.out.println("Description: " + assertionReport.getDescription());

            // If the Assertion is not satisfied
            if(!assertionReport.isSatisfied()) {
                // Return a MutantTestCaseReport that specifies that the mutant has been KILLED by InvariantData provided as input
                // Return AssertionReport where killed=true and killedBy=invariantData
                return new MutantTestCaseReport(invariantData, assertionReport);
            }

        } catch (Exception e) {
            throw new Exception("Could not find the assertion function for " + invariantType);
        }


        // Return AssertionReport where killed=false and killedBy=null
        return new MutantTestCaseReport();
    }

    // ############################# UTILS #############################
    private static Integer getIntegerValue(JsonNode variableValue) {
        Integer res;
        if(variableValue.isInt()) {
            res = variableValue.intValue();
        } else if(variableValue.isBoolean()) {
            res = variableValue.booleanValue() ? 1:0;
        } else {    // If string
            String textValue = variableValue.textValue();
            if(textValue.equals("true")) {
                return 1;
            } else if(textValue.equals("false")) {
                return 0;
            } else {
                return Integer.parseInt(textValue);
            }
        }
        return res;
    }

    // TODO: Move to a different class/package
    // ############################# UNARY #############################
    // ############################# UNARY STRING #############################
    public static AssertionReport oneOfStringAssertion(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariant = invariantData.getInvariant();
        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Extract accepted variable values from the invariant
        List<String> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1) {
            // Format: return.Source one of { "value1", "value2", "value3" }
            String valuesString = invariant.substring(startIndex + 1, endIndex);
            String[] values = valuesString.split(", ");
            for (String value : values) {
                acceptedValues.add(value.replace("\"", "").trim());
            }
        } else if(invariant.startsWith(variables.get(0) + " ==")) {
            // Format: return.variableName == "value1"
            startIndex = invariant.indexOf("\"");
            endIndex = invariant.lastIndexOf("\"");

            if (startIndex != -1 && endIndex != -1) {
                String value = invariant.substring(startIndex + 1, endIndex);
                acceptedValues.add(value);
            }
        }

        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null && !valuesToConsiderNull.contains(variableValue.textValue())) { // Check that the value is not null
                    String variableValueString = variableValue.textValue(); // Convert to string (textNode)
                    if(!acceptedValues.contains(variableValueString)) {
                        // Return false if assertion is violated
                        String description = "Expected one of " + acceptedValues + ", got " + variableValueString;
                        // Assertion report where satisfied = false and description = description
                        return new AssertionReport(description);
                    }
                }

            }
        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();

    }

    public static AssertionReport fixedLengthStringAssertion(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariant = invariantData.getInvariant();
        int expectedLength = Integer.parseInt(invariant.split("==")[1].trim());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !valuesToConsiderNull.contains(variableValue.textValue())) {
                    // Obtain value length
                    int variableValueLength = variableValue.textValue().length();

                    // Return false if the assertion is violated
                    if(variableValueLength != expectedLength) {
                        String description = "Expected length of " + expectedLength + " for variable " + variableName +
                                ", got " + variableValueLength + " instead.";

                        return new AssertionReport(description);
                    }

                }
            }
        }

        return new AssertionReport();
    }

    public static AssertionReport isUrlAssertion(TestCase testCase, InvariantData invariantData) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[\\w\\x{00a1}-\\x{ffff}0-9]+-?)*[\\w\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[\\w\\x{00a1}-\\x{ffff}0-9]+-)*[\\w\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-zA-Z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !valuesToConsiderNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();
                    Matcher matcher = pattern.matcher(variableValueString);

                    // Return false if the assertion is violated
                    if(!matcher.matches()) {
                        String description = "The value " + variableValueString + " for the variable " + variableName
                                + " is not a valid URL";
                        return new AssertionReport(description);
                    }


                }
            }
        }

        return new AssertionReport();
    }


    public static AssertionReport isNumericAssertion(TestCase testCase, InvariantData invariantData) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^[+-]{0,1}(0|([1-9](\\d*|\\d{0,2}(,\\d{3})*)))?(\\.\\d*[0-9])?$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !valuesToConsiderNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();

                    if(variableValueString.length() != 0) {
                        Matcher matcher = pattern.matcher(variableValueString);

                        // Return false if the assertion is violated
                        if (!matcher.matches()) {
                            String description = "The value " + variableValueString + " for the variable " + variableName
                                    + " is not a valid number";
                            return new AssertionReport(description);
                        }
                    }
                }
            }
        }

        return new AssertionReport();

    }

    // ############################# UNARY STRING SEQUENCE #############################
    public static AssertionReport stringSequenceEltOneOfString(TestCase testCase, InvariantData invariantData) throws Exception {

        // TODO: Take into account null values
        // TODO: Take into account empty arrays

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        String invariant = invariantData.getInvariant();
        if(variables.size() != 1) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Extract accepted variable values from the invariant
        List<String> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        String valuesString = invariant.substring(startIndex+1, endIndex);
        String[] values = valuesString.split(", ");
        for (String value : values) {
            acceptedValues.add(value.replace("\"", "").trim());
        }
        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null) {

                    List<String> variableValuesString = jsonNodeToList(variableValue);

                    for(String variableValueString: variableValuesString) {
                        // If the assertion is not satisfied
                        if(!acceptedValues.contains(variableValueString)) {
                            String description = "The value " + variableValueString +
                                    " is not contained in the list of accepted values " + acceptedValues;
                            return new AssertionReport(description);
                        }
                    }

                }

            }
        }

        return new AssertionReport();
    }

    // ############################# UNARY SCALAR #############################
    public static AssertionReport unaryScalarLowerBound(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariant = invariantData.getInvariant();
        int lowerBound = Integer.parseInt(invariant.split(">=")[1].trim());

        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null) {
                    // Obtain Integer value
                    int variableValueInt = variableValue.asInt();

                    // Return false if the assertion has been violated
                    if(!(variableValueInt>=lowerBound)) {
                        String description = "The value of " + variableName + " should be greater or equal than "
                                + lowerBound + " but got " + variableValueInt;
                        return new AssertionReport(description);

                    }
                }
            }
        }

        // Return true
        return new AssertionReport();
    }


    // ############################# BINARY #############################
    // ############################# BINARY STRING #############################
    public static AssertionReport twoStringEqualAssertion(TestCase testCase, InvariantData invariantData) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        if(variables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Get the names of the variables
        String inputVariableName = variables.get(0);
        String returnVariableName = variables.get(1);

        // Get the value of the input variable
        List<JsonNode> inputVariableValueList = variableValuesMap.get(inputVariableName);
        if(inputVariableValueList.size() != 1) {
            throw new Exception("The input variable should only have one value");
        }

        // Take null values into account
        if(inputVariableValueList.get(0) != null && !valuesToConsiderNull.contains(inputVariableValueList.get(0).textValue())) {

            String inputVariableValue = inputVariableValueList.get(0).textValue();
            // Check that the assertion is satisfied for every possible value of the RETURN variable
            for(JsonNode returnVariableValue: variableValuesMap.get(returnVariableName)) {

                // Take null values into account
                if(returnVariableValue != null && !valuesToConsiderNull.contains(returnVariableValue.textValue())) {
                    // If the input and return values are NOT equal, the assertion is not satisfied
                    if(!inputVariableValue.equals(returnVariableValue.textValue())){
                        String description = "Expected value of " + returnVariableName + " to be " + inputVariableValue +
                                ", but got " + returnVariableValue.textValue() + " instead";
                        return new AssertionReport(description);
                    }
                }
            }
        }


        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    // ############################# BINARY SCALAR #############################
    public static AssertionReport twoScalarIntGreaterEqual(TestCase testCase, InvariantData invariantData) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);


        if(variables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Get the names of the variables
        String inputVariableName = variables.get(0);
        String returnVariableName = variables.get(1);

        // Get the value of the input variable
        List<JsonNode> inputVariableValueList = variableValuesMap.get(inputVariableName);
        if(inputVariableValueList.size() != 1) {
            throw new Exception("The input variable should only have one value");
        }

        if(inputVariableValueList.get(0) != null) {

            Integer inputVariableValue;
            if(inputVariableValueList.get(0).isInt()) {
                inputVariableValue = inputVariableValueList.get(0).intValue();
            } else {
                inputVariableValue = Integer.parseInt(inputVariableValueList.get(0).textValue());
            }


            for(JsonNode returnVariableValue: variableValuesMap.get(returnVariableName)) {
                // Take null values into account
                if(returnVariableValue != null) {
                    Integer returnVariableValueInteger;
                    if(returnVariableValue.isInt()) {
                        returnVariableValueInteger = returnVariableValue.intValue();
                    } else {
                        returnVariableValueInteger = Integer.parseInt(returnVariableValue.textValue());
                    }

                    // If assertion is not satisfied, return false
                    if (!(inputVariableValue >= returnVariableValueInteger)) {
                        String description = "The value of " + returnVariableName + " should be lesser or equal than " +
                                inputVariableName + " (" +  inputVariableValue + "), but got " + returnVariableValueInteger;
                        return new AssertionReport(description);
                    }
                }
            }

        }


        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }


    public static AssertionReport twoScalarIntEqual(TestCase testCase, InvariantData invariantData) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(variables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = variables.get(0);
        String secondVariableName = variables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);

            if(firstVariableValue != null) {
                // Get value as integer
                Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                for (JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {
                    // Take null values into account
                    if(secondVariableValue != null) {
                        // Get variable value as integer
                        Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                        // If assertion is not satisfied, return false
                        String description = twoScalarIntEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }

            }

        } else {    // We are comparing multiple return values
            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                    Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                    // If assertion is not satisfied, return false
                    String description = twoScalarIntEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String twoScalarIntEqualAssertion(Integer firstVariableValue, Integer secondVariableValue,
                                                     String firstVariableName, String secondVariableName) {

        if(!firstVariableValue.equals(secondVariableValue)) {
            return firstVariableName + " should be equal to " + secondVariableName + ", but got different values (" +
                    firstVariableValue + " and " + secondVariableValue + ")";
        }

        return null;

    }






}

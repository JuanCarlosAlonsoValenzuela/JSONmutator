package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.experiment2.generateAssertions.AssertionReport;
import es.us.isa.jsonmutator.experiment2.generateAssertions.MutantTestCaseReport;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import org.checkerframework.checker.units.qual.A;
import org.reflections.Reflections;


import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadVariablesValues.getVariableValues;

public class GenerateAssertions {

    // Given a test case/operation
    // Given a set of invariants
    // For each invariant
    //      Extract the variable values
    //      Generate an assertion
    //      Consider null values

    private static String invariantsPath = "src/test/resources/test_suites/OMDb/byIdOrTitle/invariants_100_modified.csv";
    private static String testCasesPath = "src/test/resources/test-case-omdb.csv";
//    private static String testCasesPath = "src/test/resources/test_suites/OMDb/byIdOrTitle/OMDb_byIdOrTitle_50.csv";


    public static void main(String[] args) throws Exception {

        List<TestCase> testCases = readTestCasesFromPath(testCasesPath);
        List<InvariantData> invariantDataList = getInvariantsDataFromPath(invariantsPath);

        // TODO: Manage the exceptions properly
        // For every test case, check all the assertions
        for(TestCase testCase: testCases) {
            MutantTestCaseReport mutantTestCaseReport = generateAssertions(testCase, invariantDataList);
            // TODO: Add the assertion report as row to the report csv file
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

    // TODO: Take null values into account
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

            // If the Assertion is not satisfied
            if(!assertionReport.isSatisfied()) {
                // Return a MutantTestCaseReport that specifies that the mutant has been KILLED by InvariantData provided as input
                // Return AssertionReport where killed=true and killedBy=invariantData
                return new MutantTestCaseReport(invariantData);
            }

            System.out.println(assertionReport.isSatisfied());

        } catch (Exception e) {
            throw new Exception("Could not find the assertion function for " + invariantType);
        }


        // Return AssertionReport where killed=false and killedBy=null
        return new MutantTestCaseReport();
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
        int endIndex = invariant.indexOf("}");
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
                if(variableValue != null) { // Check that the value is not null
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
                if(variableValue != null) {
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
        String inputVariableValue = inputVariableValueList.get(0).textValue();

        // Check that the assertion is satisfied for every possible value of the RETURN variable
        for(JsonNode returnVariableValue: variableValuesMap.get(returnVariableName)) {

            // Take null values into account
            if(returnVariableValue != null) {
                // If the input and return values are NOT equal, the assertion is not satisfied
                if(!inputVariableValue.equals(returnVariableValue.textValue())){
                    String description = "Expected value of " + returnVariableName + " to be " + inputVariableValue +
                            ", but got " + returnVariableValue.textValue() + " instead";
                    return new AssertionReport(description);
                }
            }
        }


        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }


}

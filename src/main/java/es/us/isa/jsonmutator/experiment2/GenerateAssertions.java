package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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

        // TODO: Convert into for loops
        TestCase testCase = testCases.get(0);
        InvariantData invariantData = invariantDataList.get(0);

        //

        // TODO: Refactorization
//        GenerateAssertions generateAssertions = new GenerateAssertions();
//        generateAssertions.generateAssertions(testCase, invariantData);
        generateAssertions(testCase, invariantData);

    }

    // TODO: Take null values into account
    // TODO: Convert InvariantData input parameter into a list
    // TODO: Change return type
    private static String generateAssertions(TestCase testCase, InvariantData invariantData) throws Exception {



        String invariantType = invariantData.getInvariantType();

        // Based on the invariantType value, call a specific function
        try {

            // Read configuration file
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonProperties = objectMapper.readTree(new String(Files.readAllBytes(Paths.get("src/main/java/es/us/isa/jsonmutator/experiment2/assertionFunctions.json"))));

            // Obtain the function assigned to the string value
            String functionName = jsonProperties.get(invariantType).textValue();


            System.out.println();
            // Invocar la función utilizando reflexión
            Method method = GenerateAssertions.class.getMethod(functionName, TestCase.class, InvariantData.class);
            method.invoke(null, testCase, invariantData);


        } catch (Exception e) {
            throw new Exception("Could not find the assertion function");
        }



        return null;
    }


    // TODO: Change type
    public static void oneOfStringAssertion(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariant = invariantData.getInvariant();
        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValues = getVariableValues(testCase, invariantData);

        System.out.println(variableValues);
//        for()

    }






}

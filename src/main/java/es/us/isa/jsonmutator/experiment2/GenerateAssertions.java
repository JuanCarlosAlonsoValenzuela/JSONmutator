package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;
import static es.us.isa.jsonmutator.experiment2.main.getJsonNode;

public class GenerateAssertions {

    private static String invariantsPath = "src/test/resources/test_suites/OMDb/byIdOrTitle/invariants_100_modified.csv";
    private static String testCasesPath = "src/test/resources/test-case-omdb.csv";



    public static void main(String[] args) throws Exception {

        // Get a specific JSON
        // TODO: Get input values too
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(new String(Files.readAllBytes(Paths.get("src/test/resources/test-object-omdb.json"))));

        // TODO: These test cases are supposed to be mutated
        List<TestCase> testCases = readTestCasesFromPath(testCasesPath);
        List<InvariantData> invariantDataList = getInvariantsDataFromPath(invariantsPath);

        // Locate exit variables in JSON
//        System.out.println(jsonNode);
//        for(InvariantData invariantData: invariantDataList) {
//            System.out.println(invariantData);
//        }

        TestCase testCase = testCases.get(0);

        getVariableValue(testCase, invariantDataList.get(0));




    }

    // TODO: This is not a string
    private static String getVariableValue(TestCase testCase, InvariantData invariantData) throws Exception {

        // TODO: An invariant can contain more than one variable
        // TODO: Variables can be part of input. or return.
        // TODO: size(x) is also a possible variable value

        // TODO: Example of variable splitted by dots: return.owner.url
        String variableName = invariantData.getVariables().get(0);

        String res = null;

        if(variableName.startsWith("input.")){

        } else if(variableName.startsWith("return.")) {

            // Locate variable in path established by ppt (.array and &)
            String pptname = invariantData.getPptname();
            System.out.println(pptname);

            // TODO: Consider .array
            // Get route to variable
            List<String> jsonHierarchy = Arrays.asList(pptname.substring(pptname.indexOf(testCase.getStatusCode()+"&")+4, pptname.indexOf('(')).split("&"));
            List<String> variableHierarchy = Arrays.asList(variableName.substring("return.".length()).split("\\."));

            // Get response body
            String responseString = testCase.getResponseBody();
            JsonNode responseJsonNode = getJsonNode(responseString);

            // Get value in hierarchy
//            String value = null;
            for(String jsonLevel: jsonHierarchy) {
                responseJsonNode = responseJsonNode.get(jsonLevel);     // TODO: This is an array (recursivity)
            }

            for(String variableLevel: variableHierarchy) {
                responseJsonNode = responseJsonNode.get(variableLevel);
            }

            System.out.println(responseJsonNode);






        } else {
            throw new Exception("Invalid variable format");
        }


        return res;
    }



}

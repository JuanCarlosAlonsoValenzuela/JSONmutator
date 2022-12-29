package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;


import java.util.*;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;
import static es.us.isa.jsonmutator.experiment2.generateAssertions.DeclsVariable.getEnterParameterValue;
import static es.us.isa.jsonmutator.experiment2.main.getJsonNode;

public class ReadVariablesValues {

    private static String invariantsPath = "src/test/resources/test_suites/OMDb/byIdOrTitle/invariants_100_modified.csv";
//    private static String testCasesPath = "src/test/resources/test-case-omdb.csv";
    private static String testCasesPath = "src/test/resources/test_suites/OMDb/byIdOrTitle/OMDb_byIdOrTitle_50.csv";



    public static void main(String[] args) throws Exception {


        // TODO: These test cases are supposed to be mutated
        List<TestCase> testCases = readTestCasesFromPath(testCasesPath);
        List<InvariantData> invariantDataList = getInvariantsDataFromPath(invariantsPath);

        TestCase testCase = testCases.get(0);

        for(InvariantData invariantData: invariantDataList) {
            System.out.println("#########################################");
            System.out.println(invariantData.getInvariant());

            // Function
            Map<String, List<JsonNode>> variableValues = getVariableValues(testCase, invariantData);

            for(String variableName: variableValues.keySet()) {
                System.out.println("Variable: " + variableName);

                for(JsonNode node: variableValues.get(variableName)){
                    System.out.println(" -" + node);
                }
            }


        }

    }


    private static Map<String, List<JsonNode>> getVariableValues(TestCase testCase, InvariantData invariantData) throws Exception {
        Map<String, List<JsonNode>> res = new HashMap<>();

        for(String variableName: invariantData.getVariables()) {
            List<JsonNode> variableValues = getSingleVariableValues(testCase, invariantData, variableName);
            res.put(variableName, variableValues);
        }

        return res;
    }



    private static List<JsonNode> getSingleVariableValues(TestCase testCase, InvariantData invariantData, String variableName) throws Exception {
        // TODO: size(x) is also a possible variable value
        // TODO: Example of variable splitted by dots: return.owner.url

        List<JsonNode> res = new ArrayList<>();

        if(variableName.startsWith("input.")){

            System.out.println(testCase);

            List<String> hierarchy = Arrays.asList(variableName.split("\\."));

            String value = getEnterParameterValue(testCase, hierarchy);
            JsonNode jsonNode = JsonNodeFactory.instance.textNode(value);
            res.add(jsonNode);

            return res;



        } else if(variableName.startsWith("return.")) {

            // Locate variable in path established by ppt (.array and &)
            String pptname = invariantData.getPptname();
            System.out.println(pptname);

            // TODO: Consider .array
            // TODO: An array variable name contains either "[]" or "[..]"
            // Get route to variable
            String reducedPptname = pptname.substring(pptname.indexOf(testCase.getStatusCode())+3, pptname.indexOf('('));
            List<String> jsonHierarchy = new ArrayList<>();
            if(reducedPptname.length() > 0 && reducedPptname.charAt(0) == '&') {
                jsonHierarchy = Arrays.asList(reducedPptname.substring(1, reducedPptname.length()).split("&"));
            }


            List<String> variableHierarchy = Arrays.asList(variableName.substring("return.".length()).split("\\."));

            // Get response body
            String responseString = testCase.getResponseBody();
            JsonNode responseJsonNode = getJsonNode(responseString);

            // Get value in hierarchy
            List<JsonNode> nestingLevels = getNestingLevels(responseJsonNode, jsonHierarchy);

            res = getVariableValuesFromHierarchy(variableHierarchy, nestingLevels);

            for(JsonNode node: res) {
                System.out.println(node);
            }
            System.out.println(res);

        } else {
            throw new Exception("Invalid variable format");
        }

        return res;
    }


    // TODO: Change function name
    // Only for the response
    private static List<JsonNode> getNestingLevels(JsonNode responseJsonNode, List<String> jsonHierarchy) {

        List<JsonNode> res = new ArrayList<>();

        // If there is no nesting (Base case 2)
        if(jsonHierarchy.size() == 0) {
            res.add(responseJsonNode);
            return res;
        }

        JsonNode subElement = responseJsonNode.get(jsonHierarchy.get(0));
        // Base case 1
        if(jsonHierarchy.size() == 1){

            if(subElement.isArray()) {
                ArrayNode subElementArray = (ArrayNode) subElement;
                for(JsonNode node: subElementArray) {
                    res.add(node);
                }
            } else{
                res.add(subElement);
            }

        } else {

            if(subElement.isArray()) {
                // TODO: Each subElement could be another array (i.e., nested arrays)
                ArrayNode subElementArray = (ArrayNode) subElement;

                for(JsonNode node: subElementArray) {
                    res.addAll(getNestingLevels(node, jsonHierarchy.subList(1, jsonHierarchy.size())));
                }
            } else {
                res.addAll(getNestingLevels(subElement, jsonHierarchy.subList(1, jsonHierarchy.size())));
            }

        }

        return res;

    }

    private static List<JsonNode> getVariableValuesFromHierarchy(List<String> variableHierarchy, List<JsonNode> nestingLevels) {

        List<JsonNode> res = new ArrayList<>();

        for(JsonNode nestingLevel: nestingLevels) {

            JsonNode element = nestingLevel;
            for(String level: variableHierarchy) {
                element = element.get(level);
            }

            res.add(element);

        }

        return res;
    }





}

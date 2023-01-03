package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        // TODO: convert into a for loop
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


    public static Map<String, List<JsonNode>> getVariableValues(TestCase testCase, InvariantData invariantData) throws Exception {
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

            res = getValueOfInputVariable(testCase, variableName);

        } else if(variableName.startsWith("return.")) {

            res = getValueOfReturnVariable(invariantData, testCase, variableName);

        } else if(variableName.startsWith("size(input.")) {
            String newVariableName = variableName.substring("size(".length(), variableName.length()-1);
            List<JsonNode> arrays = getValueOfInputVariable(testCase, newVariableName);
            for(JsonNode array: arrays) {
                // Add the size of the array to the list to return
                ObjectMapper mapper = new ObjectMapper();
                res.add(mapper.valueToTree(((ArrayNode) array).size()));
            }
        } else if(variableName.startsWith("size(return.")) {
            // [] characters and remove size()
            String newVariableName = variableName.substring("size(".length(), variableName.length()-1);
            System.out.println(newVariableName);
            List<JsonNode> arrays = getValueOfReturnVariable(invariantData, testCase, newVariableName);
            for(JsonNode array: arrays) {
                // Add the size of the array to the list to return
                if(array != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    res.add(mapper.valueToTree(((ArrayNode) array).size()));
                } else {
                    res.add(null);
                }

            }

        } else {
            throw new Exception("Invalid variable format");
        }

        return res;
    }

    public static List<String> jsonNodeToList(JsonNode jsonNode) throws Exception {
        List<String> list = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                list.add(node.textValue().trim());
            }
        } else {
            throw new Exception("Invalid format");
        }
        return list;
    }

    private static List<JsonNode> getValueOfInputVariable(TestCase testCase, String variableName) {

        List<JsonNode> res = new ArrayList<>();

        // If array
        if(variableName.contains("[..]") || variableName.contains("[]")) {
            variableName = variableName.replace("[..]", "");
            variableName = variableName.replace("[]", "");

            List<String> hierarchy = Arrays.asList(variableName.split("\\."));

            String value = getEnterParameterValue(testCase, hierarchy);
            if(value == null){
                res.add(null);
                return res;
            }
            List<String> values = Arrays.asList(value.split("%2C"));

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arrayNode = mapper.createArrayNode();
            for (String item : values) {
                arrayNode.add(item.trim());
            }

            res.add(arrayNode);
            return res;


        } else {

            List<String> hierarchy = Arrays.asList(variableName.split("\\."));
            String value = getEnterParameterValue(testCase, hierarchy);
            JsonNode jsonNode = JsonNodeFactory.instance.textNode(value);
            res.add(jsonNode);
            return res;

        }

    }

    private static List<JsonNode> getValueOfReturnVariable(InvariantData invariantData, TestCase testCase, String variableName) {
        // Locate variable in path established by ppt (.array and &)
        String pptname = invariantData.getPptname();

        // TODO: Consider .array
        // TODO: An array variable name contains either "[]" or "[..]"
        // Get route to variable
        String reducedPptname = pptname.substring(pptname.indexOf(testCase.getStatusCode())+3, pptname.indexOf('('));
        List<String> jsonHierarchy = new ArrayList<>();
        if(reducedPptname.length() > 0 && reducedPptname.charAt(0) == '&') {
            jsonHierarchy = Arrays.asList(reducedPptname.substring(1).split("&"));
        }


        variableName = variableName.replace("[..]", "");
        variableName = variableName.replace("[]", "");
        List<String> variableHierarchy = Arrays.asList(variableName.substring("return.".length()).split("\\."));

        // Get response body
        String responseString = testCase.getResponseBody();
        JsonNode responseJsonNode = getJsonNode(responseString);

        // Get value in hierarchy
        List<JsonNode> nestingLevels = getNestingLevels(responseJsonNode, jsonHierarchy);

        return getVariableValuesFromHierarchy(variableHierarchy, nestingLevels);
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
        if(subElement==null) {      // If the nesting level is not present for this response
            return new ArrayList<>();
        }
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
                if(element != null) {
                    element = element.get(level);
                }
            }

            res.add(element);

        }

        return res;
    }


}

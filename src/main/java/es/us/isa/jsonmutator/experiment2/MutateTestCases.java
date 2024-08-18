package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.JsonMutator;
import es.us.isa.jsonmutator.experiment2.mutationReports.ElementMutationResult;
import es.us.isa.jsonmutator.experiment2.mutationReports.MutationResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.GenerateAssertions.getOutputPath;
import static es.us.isa.jsonmutator.experiment2.readTestCases.CSVManager.readCSV;
import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;

import static org.junit.Assert.fail;

public class MutateTestCases {


    // Mutates a set of test cases and returns its path
    public static String mutateTestCases(String mutatedFileName, String testCasesPath) throws IOException {

        String mutatedCsvPath = getOutputPath(mutatedFileName, testCasesPath);

        // Create csv writer for the mutated test cases
        FileWriter csvFile = null;
        try {
            csvFile = new FileWriter(mutatedCsvPath);
        } catch (IOException e) {
            throw new IOException("Could not read the CSV file");
        }

        BufferedWriter csvBuffer = new BufferedWriter(csvFile);

        // Create objectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Read test cases file
        List<List<String>> testCases = readCSV(testCasesPath, true, ',');
        List<String> headers = testCases.get(0);

        // Write csv header
        String headersString = headers.toString().replace("[", "").replace("]","").replace(", ", ",");
        // TODO: Property name can also be an array index
        headersString = headersString + ",jsonNodeType,variableHierarchy,mutationOperatorName,originalValue,mutatedValue";

        csvBuffer.write(headersString);

        int responseBodyIndex = headers.indexOf("responseBody");
        int bodyParameterIndex = headers.indexOf("bodyParameter");

        for(int i=1; i<testCases.size(); i++) {

            boolean mutatedIsEqual = true;
            List<String> testCase = testCases.get(i);

            // Original response body string
            String responseBody = testCase.get(responseBodyIndex);
            String mutatedJsonString = null;

            // Read JSON file
            JsonNode jsonNode = null;
            try{
                jsonNode = objectMapper.readTree(responseBody);
            } catch (IOException e) {
                System.out.println("Unable to read JSON");
                e.printStackTrace();
                fail();
            }

            String variableHierarchy = "";
            ElementMutationResult elementMutationResult = null;
            while(mutatedIsEqual) {
                // Create mutator
                JsonMutator jsonMutator = new JsonMutator();

                MutationResult mutationResult = jsonMutator.mutateJson(jsonNode, true);
                JsonNode mutatedJsonNode = mutationResult.getMutatedJsonNode();
                elementMutationResult = mutationResult.getElementMutationResult();
                variableHierarchy = mutationResult.getVariableHierarchy();

                try {
                    // Mutated response body string
                    mutatedJsonString = objectMapper.writeValueAsString(mutatedJsonNode);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    fail();
                }

                mutatedIsEqual = responseBody.equals(mutatedJsonString);

                // This happens when JSON mutator unsuccessfully tries to mutate an empty array and fails because
                // the addElement mutator is disabled
                if (elementMutationResult.getMutationOperatorResult().getMutationOperatorName() == null) {
                    mutatedIsEqual = true;
                }

            }

            // Rewrite as a separate file
            testCase.set(responseBodyIndex, mutatedJsonString);

            String row = testCase.get(0);
            for(int j=1; j<testCase.size(); j++){
                if(j==responseBodyIndex || j==bodyParameterIndex) {
                    row = row + "," + escapeCsv(testCase.get(j));
                } else {
                    row = row + "," + testCase.get(j);
                }
            }

            row = row + "," + elementMutationResult.getMutatedPropertyDatatype() +
                    "," + variableHierarchy +
                    "," + elementMutationResult.getMutationOperatorResult().getMutationOperatorName() +
                    "," + escapeCsv(elementMutationResult.getMutationOperatorResult().getOriginalValue()) +
                    "," + escapeCsv(elementMutationResult.getMutationOperatorResult().getMutatedValue());

            // Write the test case as csv row
            csvBuffer.newLine();
            csvBuffer.write(row);

        }

        csvBuffer.close();


        // Returns the path of the mutated file
        return mutatedCsvPath;

    }


    public static JsonNode getJsonNode(String jsonString) {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;

        // Read JSON file and create JSON node and JSON string
        try {
            jsonNode = objectMapper.readTree(jsonString);
//            jsonString = objectMapper.writeValueAsString(jsonNode);

        } catch (IOException e) {
            System.out.println("Unable to read JSON");
            e.printStackTrace();
            fail();
        }

        return jsonNode;

    }

}

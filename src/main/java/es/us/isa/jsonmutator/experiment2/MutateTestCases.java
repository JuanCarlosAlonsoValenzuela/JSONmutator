package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.JsonMutator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.GenerateAssertions.getOutputPath;
import static es.us.isa.jsonmutator.experiment2.readTestCases.CSVManager.readCSV;
import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;
import static org.junit.Assert.fail;

public class MutateTestCases {

    public static String[] stringsToConsiderAsNull = {"N/A"};

    private static ObjectMapper objectMapper;
    private static JsonNode jsonNode = null;

    // Test cases file to mutate
    private static String testCasesPath = "src/test/resources/test_suites/GitHub/getOrganizationRepositories/GitHub_GetOrganizationRepositories_50.csv";


    public static void main(String[] args) throws IOException {
        String csvPath = getOutputPath("mutated_testCases.csv", testCasesPath);

        // Create csv writer for the mutated test cases
        FileWriter csvFile = new FileWriter(csvPath);
        BufferedWriter csvBuffer = new BufferedWriter(csvFile);

        // Create objectMapper
        objectMapper = new ObjectMapper();

        // Read test cases file
        List<List<String>> testCases = readCSV(testCasesPath, true, ',');
        List<String> headers = testCases.get(0);

        // Write csv header
        String headersString = headers.toString().replace("[", "").replace("]","").replace(", ", ",");
        csvBuffer.write(headersString);

        int responseBodyIndex = headers.indexOf("responseBody");

        for(int i=1; i<testCases.size(); i++) {
            List<String> testCase = testCases.get(i);
            String responseBody = testCase.get(responseBodyIndex);

            // Read JSON file
            try{
                jsonNode = objectMapper.readTree(responseBody);
            } catch (IOException e) {
                System.out.println("Unable to read JSON");
                e.printStackTrace();
                fail();
            }

            // Create mutator
            JsonMutator jsonMutator = new JsonMutator();

            // TODO: Enable and disable mutation operators
            JsonNode mutatedJsonNode = jsonMutator.mutateJson(jsonNode, true);

            String mutatedJsonString = null;
            try {
                mutatedJsonString = objectMapper.writeValueAsString(mutatedJsonNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }

            // Rewrite as a separate file
            testCase.set(responseBodyIndex, mutatedJsonString);

            String row = testCase.get(0);
            for(int j=1; j<testCase.size(); j++){
                if(j==responseBodyIndex) {
                    row = row + "," + escapeCsv(testCase.get(j));
                } else {
                    row = row + "," + testCase.get(j);
                }
            }

            // Write the test case as csv row
            csvBuffer.newLine();
            csvBuffer.write(row);

        }

        csvBuffer.close();

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



    // TODO: Read JSON from a csv file
    // TODO: Maximum number of mutations?
    // TODO: Read test cases file
    // TODO: Enable and disable specific mutation operators (e.g., add object element)
    // TODO: Check that the JSON is read correctly (e.g., multiple "" characters or multiline JSON)
    // TODO: Mutate each test case (between 1 and 10 mutations per JSON)
    // TODO: Compute metrics automatically. Percentage of mutants killed?
    // TODO: Take special null values (e.g., N/A in OMDb) into account
    // TODO: Take null values into account when creating the assertions

}

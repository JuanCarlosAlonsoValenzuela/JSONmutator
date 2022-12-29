package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.JsonMutator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

public class main {

//    private static ObjectMapper objectMapper;   // TODO: Consider removing static
//    private static JsonNode jsonNode = null;
//    private static String jsonString = null;
//
//    private static String jsonPath = "src/test/resources/test-object2.json";
//
//
//    public static void main(String[] args) {
//
//        // TODO: Convert all this into a function
//        objectMapper = new ObjectMapper();
//
//        // Read JSON file and create JSON node and JSON string
//        try {
//            jsonNode = objectMapper.readTree(new String(Files.readAllBytes(Paths.get(jsonPath))));
//            jsonString = objectMapper.writeValueAsString(jsonNode);
//
//        } catch (IOException e) {
//            System.out.println("Unable to read JSON");
//            e.printStackTrace();
//            fail();
//        }
//
//        // Create mutator
//        JsonMutator jsonMutator = new JsonMutator();
//
//        // TODO: Enable and disable mutation operators
//
//        JsonNode mutatedJsonNode = jsonMutator.mutateJson(jsonNode, false);
//
//        System.out.println(jsonNode);
//
//        System.out.println(mutatedJsonNode);
//
//
//
//
//    }


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
    // TODO: Generate assertions automatically
    // TODO: Compute metrics automatically. Percentage of mutants killed?
    // TODO: Take special null values (e.g., N/A in OMDb) into account
    // TODO: Take null values into account when creating the assertions

}

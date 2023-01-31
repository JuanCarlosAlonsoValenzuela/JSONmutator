package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Draft {

    public static void main(String[] args) throws Exception {
        // Read invariants and test cases paths from JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode operationPathList = null;
        try {
            operationPathList = objectMapper.readValue(new String(Files.readAllBytes(Paths.get("src/test/resources/delete/operationsPaths.json"))), ArrayNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(JsonNode operationPath: operationPathList) {

            String operationName = operationPath.get("operationName").textValue();
            String testCasesPath = operationPath.get("testCasesPath").textValue();
            String invariantsPath = operationPath.get("invariantsPath").textValue();

            //            System.out.println(operationPath);
            System.out.println(operationName);

            List<String> stringsToConsiderAsNull = new ArrayList<>();
            JsonNode jsonNullStrings = operationPath.get("stringsToConsiderAsNull");
            if (jsonNullStrings.isArray()) {
                for (JsonNode node : jsonNullStrings) {
                    stringsToConsiderAsNull.add(node.textValue());
                }
            }

            // Assert that no mutants are killed in the original file (without mutate)
            GenerateAssertions originalAssertions = new GenerateAssertions(testCasesPath, invariantsPath, stringsToConsiderAsNull);
            Double percentageKilledOriginal = originalAssertions.generateAssertions(operationName + "_original_mutation_report.csv");
            if (percentageKilledOriginal != 0.0) {
                throw new Exception("The mutation score in the original test suite should be 0%");
            }
        }


    }
}

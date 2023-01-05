package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.MutateTestCases.mutateTestCases;

public class MainExperiment2 {

    private static int nExecutions = 10;
    private static String mutatedTestCasesFolder = "mutated_test_cases";
    private static String mutationReportFolder = "mutation_report";


    public static void main(String[] args) throws IOException {

        // TODO: Read invariants and test cases paths from JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode operationPathList = null;
        try {
            operationPathList = objectMapper.readValue(new String(Files.readAllBytes(Paths.get("src/main/resources/experiment2/operationsPaths.json"))), ArrayNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(operationPathList);


        // TODO: Assert that no mutants are killed in the original file (without mutate)

        // For every API operation
        for(JsonNode operationPath: operationPathList) {
            System.out.println(operationPath);

            String operationName = operationPath.get("operationName").textValue();
            String testCasesPath = operationPath.get("testCasesPath").textValue();
            String invariantsPath = operationPath.get("invariantsPath").textValue();
            // TODO: IMPLEMENT (AND REMOVE PUBLIC STATIC VARIABLE)
            List<String> stringsToConsiderAsNull = new ArrayList<>();

            // TODO: REMOVE FOLDER BEFOREHAND

            // Generate and kill mutants, total of nExecutions
            for(int i = 0; i<nExecutions; i++) {
                // Generate mutated_testCases.csv
                String mutatedTestCasesPath = mutateTestCases(mutatedTestCasesFolder + "/" + operationName + "_mutants_" + i + ".csv", testCasesPath);

                // Generate mutation reports
                


            }




        }





        // TODO: For every mutated_testCases.csv, generate a mutation report (see GenerateAssertions.java)

        // TODO: Compute the average mutation score


        // TODO: Generate report as Table

        // TODO: ADD STRINGS TO CONSIDER AS NULL


    }
}

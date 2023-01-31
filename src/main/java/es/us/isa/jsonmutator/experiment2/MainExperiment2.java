package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.GenerateAssertions.getOutputPath;
import static es.us.isa.jsonmutator.experiment2.MutateTestCases.mutateTestCases;

public class MainExperiment2 {

    private static int nExecutions = 10;
    private static String mutatedTestCasesFolder = "mutated_test_cases";
    private static String mutationReportFolder = "mutation_reports";


    public static void main(String[] args) throws Exception {

        // Read invariants and test cases paths from JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode operationPathList = null;
        try {
            operationPathList = objectMapper.readValue(new String(Files.readAllBytes(Paths.get("src/main/resources/experiment2/operationsPaths.json"))), ArrayNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(operationPathList);

        // For every API operation
        for(JsonNode operationPath: operationPathList) {

            String operationName = operationPath.get("operationName").textValue();
            String testCasesPath = operationPath.get("testCasesPath").textValue();
            String invariantsPath = operationPath.get("invariantsPath").textValue();

            //            System.out.println(operationPath);
            System.out.println(operationName);

            List<String> stringsToConsiderAsNull = new ArrayList<>();
            JsonNode jsonNullStrings = operationPath.get("stringsToConsiderAsNull");
            if(jsonNullStrings.isArray()) {
              for(JsonNode node: jsonNullStrings) {
                  stringsToConsiderAsNull.add(node.textValue());
              }
            }

            // Assert that no mutants are killed in the original file (without mutate)
            GenerateAssertions originalAssertions = new GenerateAssertions(testCasesPath, invariantsPath, stringsToConsiderAsNull);
            Double percentageKilledOriginal = originalAssertions.generateAssertions(operationName + "_original_mutation_report.csv");
            if(percentageKilledOriginal != 0.0) {
                throw new Exception("The mutation score in the original test suite should be 0%");
            }

            // Remove directories if exist
            File mutatedTestCasesFile = new File(getOutputPath(mutatedTestCasesFolder, testCasesPath));
            File mutationReportFile = new File(getOutputPath(mutationReportFolder, testCasesPath));

            FileUtils.deleteDirectory(mutatedTestCasesFile);
            FileUtils.deleteDirectory(mutationReportFile);

            // Create empty directories
            mutatedTestCasesFile.mkdir();
            mutationReportFile.mkdir();

            List<Double> mutationScores = new ArrayList<>();
            // Generate and kill mutants, total of nExecutions
            for(int i = 0; i<nExecutions; i++) {
                // Generate mutated_testCases.csv, returns the path of the mutants file
                String mutatedTestCasesPath = mutateTestCases(mutatedTestCasesFolder + "/" + operationName + "_mutants_" + i + ".csv", testCasesPath);

                // Generate mutation reports
                GenerateAssertions generateAssertions = new GenerateAssertions(mutatedTestCasesPath, invariantsPath, stringsToConsiderAsNull);
                try {
                    // Returns the percentage of mutants killed and generates a report in csv format
                    Double percentageKilled = generateAssertions.generateAssertions(mutationReportFolder + "/" + operationName + "_mutation_report_" + i + ".csv");
                    mutationScores.add(percentageKilled);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            // Mutation scores for 10 executions
//            System.out.println(mutationScores);
            Double averageMutationScore = mutationScores.stream().mapToDouble(val ->val).average().orElse(0.0);
            System.out.println(mutationScores);
            System.out.println(averageMutationScore);




        }



        // TODO: Generate report as Table



    }
}

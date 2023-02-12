package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import es.us.isa.jsonmutator.JsonMutator;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCaseFileManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.readTestCases.CSVManager.getCSVRecord;
import static es.us.isa.jsonmutator.experiment2.MutateTestCases.getJsonNode;

public class ReadTestCases {



    public static List<TestCase> readTestCasesFromPath(String testCasesFilePath) {

        List<TestCase> res = new ArrayList<>();

        try {
            // Read test cases
            File testCasesFile = new File(testCasesFilePath);
            FileReader testCasesFileReader = new FileReader(testCasesFile);
            BufferedReader testCasesBR = new BufferedReader(testCasesFileReader);
            String testCasesLine = "";

            // The first line must be the header
            String header = testCasesBR.readLine();
            if (header == null) {
                throw new NullPointerException("The csv file containing the test cases is empty");
            }

            TestCaseFileManager testCaseFileManager = new TestCaseFileManager(header);

            while((testCasesLine = testCasesBR.readLine()) != null) {
                TestCase testCase = testCaseFileManager.getTestCase(getCSVRecord(testCasesLine));

                res.add(testCase);

            }


        } catch (IOException e){
            e.printStackTrace();
        }

        return res;
    }


    private static String testCasesFilePath = "src/test/resources/test_suites/OMDb/byIdOrTitle/OMDb_byIdOrTitle_50.csv";

    public static void main(String[] args) {


        try {
            // Read test cases
            File testCasesFile = new File(testCasesFilePath);
            FileReader testCasesFileReader = new FileReader(testCasesFile);
            BufferedReader testCasesBR = new BufferedReader(testCasesFileReader);
            String testCasesLine = "";

            // The first line must be the header
            String header = testCasesBR.readLine();
            if (header == null) {
                throw new NullPointerException("The csv file containing the test cases is empty");
            }

            TestCaseFileManager testCaseFileManager = new TestCaseFileManager(header);

            while((testCasesLine = testCasesBR.readLine()) != null) {
                TestCase testCase = testCaseFileManager.getTestCase(getCSVRecord(testCasesLine));

                String responseBody = testCase.getResponseBody();
                System.out.println(responseBody);

                JsonNode responseJsonNode = getJsonNode(responseBody);

                // Create mutator
                JsonMutator jsonMutator = new JsonMutator();
                JsonNode mutatedJsonNode = jsonMutator.mutateJson(responseJsonNode, false);
                System.out.println(mutatedJsonNode);

                System.out.println("#####################################");

            }

        } catch (IOException e){
            e.printStackTrace();
        }

    }
}

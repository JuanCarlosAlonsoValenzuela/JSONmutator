package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.jsonmutator.experiment2.generateAssertions.AssertionReport;
import es.us.isa.jsonmutator.experiment2.generateAssertions.MutantTestCaseReport;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;

public class GenerateAssertions {


    private static String header = "testCaseId;killed;killedBy_invariant;description;killedBy_pptname;killedBy_invariantType";

    private String mutatedTestCasesPath;
    private String invariantsPath;
    private List<String> stringsToConsiderAsNull;

    private JsonNode assertionFunctions;
    private JsonNode invariantsWithShift;

    private static String assertionFunctionsPath = "src/main/resources/experiment2/assertionFunctions.json";

    private static String invariantsWithShiftPath = "src/main/resources/experiment2/invariantsWithShift.json";


    public GenerateAssertions(String mutatedTestCasesPath, String invariantsPath, List<String> stringsToConsiderAsNull) throws IOException {
        this.mutatedTestCasesPath = mutatedTestCasesPath;
        this.invariantsPath = invariantsPath;

        stringsToConsiderAsNull.add(null);
        this.stringsToConsiderAsNull = stringsToConsiderAsNull;

        // File with assertion functions
        ObjectMapper objectMapperAssertions = new ObjectMapper();
        this.assertionFunctions = objectMapperAssertions.readTree(new String(Files.readAllBytes(Paths.get(assertionFunctionsPath))));

        // File with assertion functions associated to invariants with shift
        ObjectMapper objectMapperShift = new ObjectMapper();
        this.invariantsWithShift = objectMapperShift.readTree(new String(Files.readAllBytes(Paths.get(invariantsWithShiftPath))));
    }

    public Double generateAssertions(String destinationPath) throws Exception {

        List<TestCase> testCases = readTestCasesFromPath(mutatedTestCasesPath);
        List<InvariantData> invariantDataList = getInvariantsDataFromPath(invariantsPath);

        String csvPath = getOutputPath(destinationPath, invariantsPath);

        Double nKilled = 0.0;

        // Create csv writer for the report
        FileWriter csvFile = new FileWriter(csvPath);
        BufferedWriter csvBuffer = new BufferedWriter(csvFile);

        // Write csv header
        csvBuffer.write(header);

        // For every test case, check all the assertions
        for(TestCase testCase: testCases) {
            // Generate test case report
            MutantTestCaseReport mutantTestCaseReport = generateAssertionsOfSingleTestCase(testCase, invariantDataList);

            if(mutantTestCaseReport.isKilled()){
                nKilled = nKilled + 1.0;
            }

            // Add the assertion report as row to the report csv file
            // COLUMNS: testCaseId; killed(boolean); killedBy_invariant; description; killedBy_pptname; killedBy_invariantType
            csvBuffer.newLine();
            csvBuffer.write(getReportCsvRow(testCase, mutantTestCaseReport));

        }

        csvBuffer.close();

        return nKilled/testCases.size();

    }


    private String getReportCsvRow(TestCase testCase, MutantTestCaseReport mutantTestCaseReport) {
        if(mutantTestCaseReport.isKilled()) {
            return testCase.getTestCaseId() + ";" + mutantTestCaseReport.isKilled() + ";" + mutantTestCaseReport.getKilledBy().getInvariant() + ";" + mutantTestCaseReport.getDescription() + ";" +
                    mutantTestCaseReport.getKilledBy().getPptname() + ";" + mutantTestCaseReport.getKilledBy().getInvariantType();
        } else {
            return testCase.getTestCaseId() + ";" + mutantTestCaseReport.isKilled() + ";" + ";" + ";" + ";";
        }
    }


    // For all the invariants of a test case
    private MutantTestCaseReport generateAssertionsOfSingleTestCase(TestCase testCase, List<InvariantData> invariantDataList) throws Exception {

        // Check whether any of the invariants is able to kill the mutant
        for(InvariantData invariantData: invariantDataList) {
            try {
//                System.out.println(invariantData.getInvariant());
                // If a single invariant is violated, the mutant is killed
                // Return AssertionReport where killed=true and killedBy=invariantData
                MutantTestCaseReport mutantTestCaseReport = generateAssertionsForSingleInvariantData(testCase, invariantData);

                if(mutantTestCaseReport.isKilled()) {
                    return mutantTestCaseReport;
                }

            } catch (Exception e) {
                throw new Exception(e);
            }


        }


        return new MutantTestCaseReport();
    }

    // For a single invariant (InvariantData)
    private MutantTestCaseReport generateAssertionsForSingleInvariantData(TestCase testCase, InvariantData invariantData) throws Exception {

        String invariantType = invariantData.getInvariantType();

        // Based on the invariantType value, call a specific function
        try {

            // Read configuration file
            ObjectMapper objectMapper = new ObjectMapper();

            // Invoke the corresponding assertion function
            AssertionReport assertionReport = null;
            if(invariantsWithShift.has(invariantData.getInvariant())) {      // Invariants that contain shift
                String functionName = invariantsWithShift.get(invariantData.getInvariant()).textValue();
                Method method = GenerateAssertions.class.getMethod(functionName, TestCase.class, InvariantData.class, List.class);
                assertionReport = (AssertionReport) method.invoke(null, testCase, invariantData, stringsToConsiderAsNull);
            } else {
                // Obtain the function assigned to the string value
                // Read function name from assertionFunctions file
                String functionName = assertionFunctions.get(invariantType).textValue();
                Method method = GenerateAssertions.class.getMethod(functionName, TestCase.class, InvariantData.class, List.class);
                assertionReport = (AssertionReport) method.invoke(null, testCase, invariantData, stringsToConsiderAsNull);
            }

            // If the Assertion is not satisfied
            if(!assertionReport.isSatisfied()) {
                // Return a MutantTestCaseReport that specifies that the mutant has been KILLED by InvariantData provided as input
                // Return AssertionReport where killed=true and killedBy=invariantData
                return new MutantTestCaseReport(invariantData, assertionReport);
            }

        } catch (Exception e) {
            throw new Exception("Could not find the assertion function for " + invariantType);
        }


        // Return AssertionReport where killed=false and killedBy=null
        return new MutantTestCaseReport();
    }

    // ############################# UTILS #############################
    private static Integer getIntegerValue(JsonNode variableValue) {
        Integer res;
        if(variableValue.isInt()) {
            res = variableValue.intValue();
        } else if(variableValue.isBoolean()) {
            res = variableValue.booleanValue() ? 1:0;
        } else {    // If string
            String textValue = variableValue.textValue();
            if(textValue.equals("true")) {
                return 1;
            } else if(textValue.equals("false")) {
                return 0;
            } else {
                return Integer.parseInt(textValue);
            }
        }
        return res;
    }

    private static Float getFloatValue(JsonNode variableValue) {
        if(variableValue.isFloat()) {
            return variableValue.floatValue();
        } else if (variableValue.isInt()) {
          Integer intValue = variableValue.intValue();
          return intValue.floatValue();
        } else { // If string
            String textValue = variableValue.textValue();
            return Float.parseFloat(textValue);
        }
    }

    private static List<String> getSorted(List<String> variables, String invariant) {
        Collections.sort(variables, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                int index1 = invariant.indexOf(s1.replace("[..]", "[]"));
                int index2 = invariant.indexOf(s2.replace("[..]", "[]"));
                if (index1 != -1 && index2 != -1) {
                    return Integer.compare(index1, index2);
                } else {
                    return s1.compareTo(s2);
                }
            }
        });
        return variables;
    }

    public static String getOutputPath(String filename, String folder) {
        Path path = java.nio.file.Paths.get(folder);      // openApiSpecPath
        Path dir = path.getParent();
        Path fn = path.getFileSystem().getPath(filename);
        Path target = (dir == null) ? fn : dir.resolve(fn);

        return target.toString();
    }

}

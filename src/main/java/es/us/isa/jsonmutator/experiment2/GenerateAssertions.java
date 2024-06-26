package es.us.isa.jsonmutator.experiment2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import es.us.isa.jsonmutator.experiment2.generateAssertions.AssertionReport;
import es.us.isa.jsonmutator.experiment2.generateAssertions.MutantTestCaseReport;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.checkerframework.checker.units.qual.A;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.us.isa.jsonmutator.experiment2.ReadInvariants.getInvariantsDataFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadTestCases.readTestCasesFromPath;
import static es.us.isa.jsonmutator.experiment2.ReadVariablesValues.*;

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


    // ############################# UNARY #############################
    // ############################# UNARY STRING #############################
    public static AssertionReport oneOfStringAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Extract accepted variable values from the invariant
        List<String> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1) {
            // Format: return.Source one of { "value1", "value2", "value3" }
            String valuesString = invariant.substring(startIndex + 1, endIndex);
            String[] values = valuesString.split(", ");
            for (String value : values) {
                acceptedValues.add(value.replace("\"", "").trim());
            }
        } else if(invariant.startsWith(variables.get(0) + " ==")) {
            // Format: return.variableName == "value1"
            startIndex = invariant.indexOf("\"");
            endIndex = invariant.lastIndexOf("\"");

            if (startIndex != -1 && endIndex != -1) {
                String value = invariant.substring(startIndex + 1, endIndex);
                acceptedValues.add(value);
            }
        }

        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) { // Check that the value is not null
                    String variableValueString = variableValue.textValue(); // Convert to string (textNode)
                    if(!acceptedValues.contains(variableValueString)) {
                        // Return false if assertion is violated
                        String description = "Expected one of " + acceptedValues + ", got " + variableValueString;
                        // Assertion report where satisfied = false and description = description
                        return new AssertionReport(description);
                    }
                }

            }
        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();

    }

    public static AssertionReport fixedLengthStringAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        int expectedLength = Integer.parseInt(invariant.split("==")[1].trim());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) {
                    // Obtain value length
                    int variableValueLength = variableValue.textValue().length();

                    // Return false if the assertion is violated
                    if(variableValueLength != expectedLength) {
                        String description = "Expected length of " + expectedLength + " for variable " + variableName +
                                ", got " + variableValueLength + " instead.";

                        return new AssertionReport(description);
                    }

                }
            }
        }

        return new AssertionReport();
    }

    public static AssertionReport isUrlAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[\\w\\x{00a1}-\\x{ffff}0-9]+-?)*[\\w\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[\\w\\x{00a1}-\\x{ffff}0-9]+-)*[\\w\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-zA-Z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();
                    Matcher matcher = pattern.matcher(variableValueString);

                    // Return false if the assertion is violated
                    if(!matcher.matches()) {
                        String description = "The value " + variableValueString + " for the variable " + variableName
                                + " is not a valid URL";
                        return new AssertionReport(description);
                    }


                }
            }
        }

        return new AssertionReport();
    }

    public static AssertionReport isDateYYYYMMDD(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^(?:19\\d{2}|20[01234][0-9]|2050)[-/.](?:0[1-9]|1[012])[-/.](?:0[1-9]|[12][0-9]|3[01])$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();
                    Matcher matcher = pattern.matcher(variableValueString);

                    // Return false if the assertion is violated
                    if(!matcher.matches()) {
                        String description = "The value " + variableValueString + " for the variable " + variableName
                                + " is not a valid Date (format: YYYYMMDD)";
                        return new AssertionReport(description);
                    }

                }
            }
        }

        return new AssertionReport();
    }

    public static AssertionReport isTimestampYYYYMMHHThhmmssmm(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^[0-9]{4}-((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01])|(0[469]|11)-(0[1-9]|[12][0-9]|30)|(02)-(0[1-9]|[12][0-9]))T(0[0-9]|1[0-9]|2[0-3]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\\.[0-9]{3}){0,1}Z$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();
                    Matcher matcher = pattern.matcher(variableValueString);

                    // Return false if the assertion is violated
                    if(!matcher.matches()) {
                        String description = "The value " + variableValueString + " for the variable " + variableName
                                + " is not a valid timestamp (format: YYYYMMHHThhmmssmm)";
                        return new AssertionReport(description);
                    }

                }
            }
        }

        return new AssertionReport();
    }


    public static AssertionReport isNumericAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^[+-]{0,1}(0|([1-9](\\d*|\\d{0,2}(,\\d{3})*)))?(\\.\\d*[0-9])?$");

        // Check that the assertion is satisfied for every value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null && !stringsToConsiderAsNull.contains(variableValue.textValue())) {
                    String variableValueString = variableValue.textValue();

                    if(variableValueString.length() != 0) {
                        Matcher matcher = pattern.matcher(variableValueString);

                        // Return false if the assertion is violated
                        if (!matcher.matches()) {
                            String description = "The value " + variableValueString + " for the variable " + variableName
                                    + " is not a valid number";
                            return new AssertionReport(description);
                        }
                    }
                }
            }
        }

        return new AssertionReport();

    }

    // ############################# UNARY STRING SEQUENCE #############################
    public static AssertionReport stringSequenceEltOneOfString(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        String invariant = invariantData.getInvariant();
        if(variables.size() != 1) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Extract accepted variable values from the invariant
        List<String> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        String valuesString = invariant.substring(startIndex+1, endIndex);
        String[] values = valuesString.split(", ");
        for (String value : values) {
            acceptedValues.add(value.replace("\"", "").trim());
        }
        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null) {

                    List<String> variableValuesString = jsonNodeToList(variableValue);

                    for(String variableValueString: variableValuesString) {
                        // If the assertion is not satisfied
                        if(!acceptedValues.contains(variableValueString)) {
                            String description = "The value " + variableValueString +
                                    " is not contained in the list of accepted values " + acceptedValues;
                            return new AssertionReport(description);
                        }
                    }

                }

            }
        }

        return new AssertionReport();
    }

    public static AssertionReport sequenceFixedLengthString(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        int expectedLength = Integer.parseInt(invariant.split("have LENGTH=")[1].trim());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue !=null) {
                    // Get as an array
                    ArrayNode arrayNode = (ArrayNode) variableValue;
                    // Iterate over arrayNode
                    for(JsonNode item: arrayNode) {
                        if(item != null && !stringsToConsiderAsNull.contains(item.textValue())) {
                            int itemLength = item.textValue().length();
                            if(itemLength != expectedLength) {
                                String description = "The length of all the elements of " + variableName + " should be " + expectedLength
                                        + ", but the length of the element " + item.textValue() + " is " + itemLength;
                                return new AssertionReport(description);
                            }
                        }
                    }

                }
            }

        }

        return new AssertionReport();
    }

    public static AssertionReport sequenceStringElementsAreUrl(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        Pattern pattern = Pattern.compile("^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$");


        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue !=null) {
                    // Get as an array
                    ArrayNode arrayNode = (ArrayNode) variableValue;
                    // Iterate over arrayNode
                    for(JsonNode item: arrayNode) {

                        String itemString = item.textValue();
                        Matcher matcher = pattern.matcher(itemString);

                        if(!matcher.matches()) {
                            String description = "The item " + itemString + " of the " + variableName + " array is not a valid URL";
                            return new AssertionReport(description);
                        }

                    }

                }

            }
        }


        return new AssertionReport();
    }

    // ############################# UNARY SCALAR #############################
    public static AssertionReport unaryScalarLowerBound(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        int lowerBound = Integer.parseInt(invariant.split(">=")[1].trim());

        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null) {
                    // Obtain Integer value
                    int variableValueInt = variableValue.asInt();

                    // Return false if the assertion has been violated
                    if(!(variableValueInt>=lowerBound)) {
                        String description = "The value of " + variableName + " should be greater or equal than "
                                + lowerBound + " but got " + variableValueInt;
                        return new AssertionReport(description);

                    }
                }
            }
        }

        // Return true
        return new AssertionReport();
    }

    public static AssertionReport oneOfScalar(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Extract accepted variable values from the invariant
        List<Integer> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1) {
            // Format: return.year one of { 2020, 2021, 2022 }
            String valuesString = invariant.substring(startIndex + 1, endIndex);
            String[] values = valuesString.split(", ");
            for (String value : values) {
                acceptedValues.add(Integer.parseInt(value.trim()));
            }
        } else if(invariant.startsWith(variables.get(0) + " ==")) {
            String stringValue = invariant.split("==")[1].trim();

            if(stringValue.equals("null")){ // If the value is null
                acceptedValues.add(null);
            } else {    // If the value is a number
                if(stringValue.equals("true")) {
                    acceptedValues.add(1);
                } else if(stringValue.equals("false")) {
                    acceptedValues.add(0);
                } else {
                    acceptedValues.add(Integer.parseInt(stringValue.trim()));
                }
            }

        }

        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null) { // Check that the value is not null
                    if(acceptedValues.contains(null)) {
                        return new AssertionReport("The value of " + variableName + " should be null, but got " + variableValue);
                    }
                    Integer variableValueInteger = getIntegerValue(variableValue);
                    if(!acceptedValues.contains(variableValueInteger)) {
                        String description = "Expected one of " + acceptedValues + ", got " + variableValueInteger;
                        return new AssertionReport(description);
                    }

                }
            }
        }

        return new AssertionReport();
    }

    // ############################# UNARY FLOAT #############################
    public static AssertionReport oneOfFloat(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Extract accepted variable values from the invariant
        List<Float> acceptedValues = new ArrayList<>();
        int startIndex = invariant.indexOf("{");
        int endIndex = invariant.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1) {
            // Format: return.year one of { 2020, 2021, 2022 }
            String valuesString = invariant.substring(startIndex + 1, endIndex);
            String[] values = valuesString.split(", ");
            for (String value : values) {
                acceptedValues.add(Float.parseFloat(value.trim()));
            }
        } else if(invariant.startsWith(variables.get(0) + " ==")) {
            String stringValue = invariant.split("==")[1].trim();
            acceptedValues.add(Float.parseFloat(stringValue.trim()));
        }

        // Check that the number of values is correct
        if(acceptedValues.size() == 0 || acceptedValues.size() > 3) {
            throw new Exception("Invalid invariant, no variables found");
        }


        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                if(variableValue != null) { // Check that the value is not null
                    Float variableValueFloat = getFloatValue(variableValue);
                    if(!acceptedValues.contains(variableValueFloat)) {
                        String description = "Expected one of " + acceptedValues + ", got " + variableValueFloat;
                        return new AssertionReport(description);
                    }

                }
            }
        }



        return new AssertionReport();
    }

    public static AssertionReport unaryScalarLowerBoundFloat(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        float lowerBound = Float.parseFloat(invariant.split(">=")[1].trim());

        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null) {
                    float variableValueFloat = variableValue.floatValue();
                    // Return false if the assertion has been violated
                    if(!(variableValueFloat >= lowerBound)) {
                        String description = "The value of " + variableName + " should be greater or equal than "
                                + lowerBound + " but got " + variableValueFloat;
                        return new AssertionReport(description);
                    }
                }
            }
        }

        // Return true
        return new AssertionReport();
    }


    // ############################# UNARY SEQUENCE #############################
    public static AssertionReport sequenceOneOfSequence(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        String arrayValue = invariant.split("==")[1].trim();

        if(!arrayValue.equals("[]")) {
            throw new Exception("Found an invariant in which the array was not empty");
        }

        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null) {
                    ArrayNode arrayNode = (ArrayNode) variableValue;
                    int size = arrayNode.size();
                    if(size!=0) {
                        String description = "The size of " + variableName + " should be 0, but got " + size;
                        return new AssertionReport(description);
                    }
                }
            }
        }

        return new AssertionReport();
    }


    public static AssertionReport sequenceOneOfStringSequence(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        String invariant = invariantData.getInvariant();
        String arrayValue = invariant.split("==")[1].trim();

        if(!arrayValue.equals("[]")) {
            throw new Exception("Found an invariant in which the array was not empty");
        }

        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        // Check that there is only one variable
        if(variableValuesMap.keySet().size() != 1) {
            throw new Exception("Invalid number of variables");
        }

        // Check that the assertion is satisfied for every possible value of the variable
        for(String variableName: variableValuesMap.keySet()) {
            List<JsonNode> variableValues = variableValuesMap.get(variableName);
            for(JsonNode variableValue: variableValues) {
                // Take null values into account
                if(variableValue != null) {
                    ArrayNode arrayNode = (ArrayNode) variableValue;
                    int size = arrayNode.size();
                    if(size!=0) {
                        String description = "The size of " + variableName + " should be 0, but got " + size;
                        return new AssertionReport(description);
                    }
                }
            }
        }

        return new AssertionReport();
    }


    // ############################# BINARY #############################
    // ############################# BINARY STRING #############################
    public static AssertionReport twoStringEqual(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);
        if(variables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = variables.get(0);
        String secondVariableName = variables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);

            // Take null values into account
            if(firstVariableValue != null && !stringsToConsiderAsNull.contains(firstVariableValue.textValue())) {
                // Get value as string
                String firstVariableValueString = firstVariableValue.textValue();
                // Check that the assertion is satisfied for every possible value of the RETURN variable
                for(JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {

                    // Take null values into account
                    if(secondVariableValue != null && !stringsToConsiderAsNull.contains(secondVariableValue.textValue())) {
                        String secondVariableValueString = secondVariableValue.textValue();
                        String description = twoStringEqualAssertion(firstVariableValueString, secondVariableValueString,
                                firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }

                    }

                }

            }

        } else { // We are comparing multiple return values
            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null && !stringsToConsiderAsNull.contains(firstVariableValue.textValue())
                        && !stringsToConsiderAsNull.contains(secondVariableValue.textValue())) {
                    String firstVariableValueString = firstVariableValue.textValue();
                    String secondVariableValueString = secondVariableValue.textValue();

                    // If assertion is not satisfied, return false
                    String description = twoStringEqualAssertion(firstVariableValueString, secondVariableValueString,
                            firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String twoStringEqualAssertion(String firstVariableValue, String secondVariableValue,
                                                  String firstVariableName, String secondVariableName) {
        if(!firstVariableValue.equals(secondVariableValue)) {
            return "Expected value of " + secondVariableName + " to be " + firstVariableValue +
                    ", but got " + secondVariableValue + " instead";
        }
        return null;
    }


    public static AssertionReport twoStringSubString(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        List<String> sortedVariables = getSorted(invariantData.getVariables(), invariantData.getInvariant());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(sortedVariables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + sortedVariables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = sortedVariables.get(0);
        String secondVariableName = sortedVariables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);

            // Take null values into account
            if(firstVariableValue != null && !stringsToConsiderAsNull.contains(firstVariableValue.textValue())) {
                // Get value as string
                String firstVariableValueString = firstVariableValue.textValue();
                // Check that the assertion is satisfied for every possible value of the RETURN variable
                for(JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {

                    // Take null values into account
                    if(secondVariableValue != null && !stringsToConsiderAsNull.contains(secondVariableValue.textValue())) {
                        String secondVariableValueString = secondVariableValue.textValue();
                        String description = twoStringSubstringAssertion(firstVariableValueString, secondVariableValueString,
                                firstVariableName, secondVariableName);

                        if(description != null) {
                            return new AssertionReport(description);
                        }

                    }

                }

            }

        } else {    // We are comparing multiple return values
            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null && !stringsToConsiderAsNull.contains(firstVariableValue.textValue())
                        && !stringsToConsiderAsNull.contains(secondVariableValue.textValue())) {
                    String firstVariableValueString = firstVariableValue.textValue();
                    String secondVariableValueString = secondVariableValue.textValue();

                    // If assertion is not satisfied, return false
                    String description = twoStringSubstringAssertion(firstVariableValueString, secondVariableValueString,
                            firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }


        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String twoStringSubstringAssertion(String firstVariableValue, String secondVariableValue,
                                                      String firstVariableName, String secondVariableName) {

        if( !secondVariableValue.contains(firstVariableValue)) {
            return "Expected value of " + firstVariableName + " (" + firstVariableValue + ") to be a substring of " +
                    secondVariableName + " (" + secondVariableValue + ")";
        }

        return null;
    }

    // ############################# BINARY SCALAR #############################
    public static AssertionReport twoScalarIntGreaterEqual(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> sortedVariables = getSorted(invariantData.getVariables(), invariantData.getInvariant());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(sortedVariables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + sortedVariables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = sortedVariables.get(0);
        String secondVariableName = sortedVariables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);
            if(firstVariableValue != null) {
                // Get value as integer
                Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                for (JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {
                    // Take null values into account
                    if(secondVariableValue != null) {
                        // Get variable value as integer
                        Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                        // If assertion is not satisfied, return false
                        String description = twoScalarIntGreaterEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }
            }

        } else { // We are comparing multiple return values

            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                    Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                    // If assertion is not satisfied, return false
                    String description = twoScalarIntGreaterEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String twoScalarIntGreaterEqualAssertion(Integer firstVariableValue, Integer secondVariableValue,
                                                            String firstVariableName, String secondVariableName) {
        if (!(firstVariableValue >= secondVariableValue)) {
            return "The value of " + secondVariableName + " should be lesser or equal than " +
                    firstVariableName + " (" +  firstVariableValue + "), but got " + secondVariableValue;
        }
        return null;
    }


    public static AssertionReport twoScalarIntGreaterThan(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> sortedVariables = getSorted(invariantData.getVariables(), invariantData.getInvariant());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(sortedVariables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + sortedVariables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = sortedVariables.get(0);
        String secondVariableName = sortedVariables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);
        List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);
            if(firstVariableValue != null) {
                // Get value as integer
                Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                for (JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {
                    // Take null values into account
                    if(secondVariableValue != null) {
                        // Get variable value as integer
                        Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                        // If assertion is not satisfied, return false
                        String description = twoScalarIntGreaterThanAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }
            }

        } else if (secondVariableValueList.size() == 1) {
            JsonNode secondVariableValue = secondVariableValueList.get(0);
            if(secondVariableValue != null) {
                // Get value as integer
                Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);
                for(JsonNode firstVariableValue: variableValuesMap.get(firstVariableName)) {
                    // Take null values into account
                    if(firstVariableValue != null) {
                        // Get variable value as integer
                        Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);

                        // If assertion not satisfied, return false
                        String description = twoScalarIntGreaterThanAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }
            }

        } else { // We are comparing multiple return values

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                    Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                    // If assertion is not satisfied, return false
                    String description = twoScalarIntGreaterThanAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();

    }

    private static String twoScalarIntGreaterThanAssertion(Integer firstVariableValue, Integer secondVariableValue,
                                                            String firstVariableName, String secondVariableName) {
        if (!(firstVariableValue > secondVariableValue)) {
            return "The value of " + secondVariableName + " should be lesser than " +
                    firstVariableName + " (" +  firstVariableName + "), but got " + secondVariableName;
        }
        return null;
    }

    public static AssertionReport twoScalarIntLessThan(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> sortedVariables = getSorted(invariantData.getVariables(), invariantData.getInvariant());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(sortedVariables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + sortedVariables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = sortedVariables.get(0);
        String secondVariableName = sortedVariables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);
            if(firstVariableValue != null) {
                // Get value as integer
                Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                for (JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {
                    // Take null values into account
                    if(secondVariableValue != null) {
                        // Get variable value as integer
                        Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                        // If assertion is not satisfied, return false
                        String description = twoScalarIntLessThanAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }
            }

        } else { // We are comparing multiple return values

            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                    Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                    // If assertion is not satisfied, return false
                    String description = twoScalarIntLessThanAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();

    }

    private static String twoScalarIntLessThanAssertion(Integer firstVariableValue, Integer secondVariableValue,
                                                           String firstVariableName, String secondVariableName) {
        if (!(firstVariableValue < secondVariableValue)) {
            return firstVariableName + " (" + firstVariableValue + ") should be less than " + secondVariableName +
                    " (" + secondVariableValue + ")";
        }
        return null;
    }


    public static AssertionReport twoScalarIntEqual(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> variables = invariantData.getVariables();
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(variables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + variables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = variables.get(0);
        String secondVariableName = variables.get(1);

        // Get the value of the first variable
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);

        if(firstVariableValueList.size() == 1) {    // We are comparing one input value with one or more return values
            JsonNode firstVariableValue = firstVariableValueList.get(0);

            if(firstVariableValue != null) {
                // Get value as integer
                Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                for (JsonNode secondVariableValue: variableValuesMap.get(secondVariableName)) {
                    // Take null values into account
                    if(secondVariableValue != null) {
                        // Get variable value as integer
                        Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                        // If assertion is not satisfied, return false
                        String description = twoScalarIntEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }
                    }
                }

            }

        } else {    // We are comparing multiple return values
            List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){
                JsonNode firstVariableValue = firstVariableValueList.get(i);
                JsonNode secondVariableValue = secondVariableValueList.get(i);

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    Integer firstVariableValueInteger = getIntegerValue(firstVariableValue);
                    Integer secondVariableValueInteger = getIntegerValue(secondVariableValue);

                    // If assertion is not satisfied, return false
                    String description = twoScalarIntEqualAssertion(firstVariableValueInteger, secondVariableValueInteger, firstVariableName, secondVariableName);
                    if(description != null) {
                        return new AssertionReport(description);
                    }
                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String twoScalarIntEqualAssertion(Integer firstVariableValue, Integer secondVariableValue,
                                                     String firstVariableName, String secondVariableName) {

        if(!firstVariableValue.equals(secondVariableValue)) {
            return firstVariableName + " should be equal to " + secondVariableName + ", but got different values (" +
                    firstVariableValue + " and " + secondVariableValue + ")";
        }

        return null;

    }

    // ############################# BINARY SEQUENCE STRING #############################
    public static AssertionReport sequenceStringMemberString(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {

        List<String> sortedVariables = getSorted(invariantData.getVariables(), invariantData.getInvariant());
        Map<String, List<JsonNode>> variableValuesMap = getVariableValues(testCase, invariantData);

        if(sortedVariables.size() != 2) {
            throw new Exception("Unexpected number of variables (expected 2, got " + sortedVariables.size() + ")");
        }

        // Get the names of the variables
        String firstVariableName = sortedVariables.get(0);      // String
        String secondVariableName = sortedVariables.get(1);     // Array of strings

        // Get values of the first variable (string)
        List<JsonNode> firstVariableValueList = variableValuesMap.get(firstVariableName);
        List<JsonNode> secondVariableValueList = variableValuesMap.get(secondVariableName);

        if(firstVariableValueList.size() == 1) {    // If there is only one string value, we are comparing one string with multiple arrays
            JsonNode firstVariableValue = firstVariableValueList.get(0);
            if(firstVariableValue != null) {
                // Get value as string
                String firstVariableValueString = firstVariableValue.textValue();

                if (firstVariableValueString != null && !stringsToConsiderAsNull.contains(firstVariableValueString)) {
                    // Check that the assertion is satisfied for every possible value of the RETURN variable
                    for (JsonNode secondVariableValue : variableValuesMap.get(secondVariableName)) {

                        // Take null values into account
                        if (secondVariableValue != null) {
                            ArrayNode arrayNode = (ArrayNode) secondVariableValue;

                            List<String> secondVariableList = new ArrayList<>();
                            // Convert into a list of strings
                            for (JsonNode item : arrayNode) {
                                secondVariableList.add(item.textValue());
                            }

                            String description = sequenceStringMemberStringAssertion(firstVariableValueString,
                                    secondVariableList, firstVariableName, secondVariableName);
                            if (description != null) {
                                return new AssertionReport(description);
                            }
                        }

                    }

                }

            }
        } else if (secondVariableValueList.size() == 1) {       // If there is only one array value, we are comparing one array with multiple strings
            JsonNode secondVariableValue = secondVariableValueList.get(0);
            if(secondVariableValue != null) {
                // Second variable as list of strings
                ArrayNode arrayNode = (ArrayNode) secondVariableValue;
                List<String> secondVariableList = new ArrayList<>();
                // Convert into a list of strings
                for(JsonNode item: arrayNode) {
                    secondVariableList.add(item.textValue());
                }

                for(JsonNode firstVariableValue: firstVariableValueList) {
                    if(firstVariableValue != null) {
                        String firstVariableValueString = firstVariableValue.textValue();

                        if(firstVariableValueString != null && !stringsToConsiderAsNull.contains(firstVariableValueString)) {

                            String description = sequenceStringMemberStringAssertion(firstVariableValueString,
                                    secondVariableList, firstVariableName, secondVariableName);
                            if(description != null) {
                                return new AssertionReport(description);
                            }
                        }
                    }
                }

            }

        } else {    // We are comparing multiple return values


            // The first and second variable lists should have the same size
            if(firstVariableValueList.size() != secondVariableValueList.size()) {
                throw new Exception("The two lists should have the same size");
            }

            for(int i=0; i<firstVariableValueList.size();i++){

                JsonNode firstVariableValue = firstVariableValueList.get(i);        // String
                JsonNode secondVariableValue = secondVariableValueList.get(i);      // Array

                // Take null values into account
                if(firstVariableValue != null && secondVariableValue != null) {
                    // First variable as string
                    String firstVariableValueString = firstVariableValue.textValue();

                    if(firstVariableValueString != null && !stringsToConsiderAsNull.contains(firstVariableValueString)) {

                        // Second variable as list of strings
                        ArrayNode arrayNode = (ArrayNode) secondVariableValue;
                        List<String> secondVariableList = new ArrayList<>();
                        // Convert into a list of strings
                        for(JsonNode item: arrayNode) {
                            secondVariableList.add(item.textValue());
                        }

                        String description = sequenceStringMemberStringAssertion(firstVariableValueString,
                                secondVariableList, firstVariableName, secondVariableName);
                        if(description != null) {
                            return new AssertionReport(description);
                        }

                    }

                }

            }

        }

        // Return true if the assertion has been satisfied
        // Assertion report where satisfied = true and description = null
        return new AssertionReport();
    }

    private static String sequenceStringMemberStringAssertion(String firstVariableValueString, List<String> secondVariableList,
                                                              String firstVariableName, String secondVariableName){
        if(!secondVariableList.contains(firstVariableValueString)) {
            return "Expected " + secondVariableName + " (" + secondVariableList + ") to contain " +
                    firstVariableName + " (" + firstVariableValueString + ")";
        }

        return null;
    }

    // API: Marvel
    // Assertion: return.data.results[return.data.offset] == return.data.results[return.data.total-1]
    public static AssertionReport marvelOffsetAndTotalAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        // Check ppt-name
        if(!invariantData.getPptname().equals("main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT")) {
            throw new Exception("Incorrect ppt-name, expected: main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT");
        }
        List<JsonNode> offsetValues = getSingleVariableValues(testCase, invariantData, "return.data.offset");
        List<JsonNode> totalValues = getSingleVariableValues(testCase, invariantData, "return.data.total");

        List<JsonNode> resultsValues = getSingleVariableValues(testCase, invariantData, "return.data.results");

        if(offsetValues.size() != totalValues.size() || offsetValues.size() != resultsValues.size()) {
            throw new Exception("Different sizes for lists that should have the same size");
        }

        for(int i = 0; i<offsetValues.size(); i++) {
            int offsetValue = offsetValues.get(i).intValue();
            int totalValue = totalValues.get(i).intValue() - 1;


            ArrayNode resultsValue = (ArrayNode) resultsValues.get(i);

            // If one of the values is less than 0
            if(offsetValue<0){
                return new AssertionReport("return.data.offset cannot be less than zero, got: " + offsetValue);
            }
            if(totalValue<0) {
                return new AssertionReport("return.data.total-1 cannot be less than zero, got: " + totalValue);
            }

            // If one of the values is greater than the size of the array
            if(totalValue > (resultsValue.size() - 1)) {
                return new AssertionReport("The value of return.data.total-1 (" + totalValue + ") cannot be used to access an array of size " + resultsValue.size());
            }
            if(offsetValue > (resultsValue.size() - 1)) {
                return new AssertionReport("The value of return.data.offset (" + offsetValue + ") cannot be used to access an array of size " + resultsValue.size());
            }

            if((totalValue != offsetValue) && (!resultsValue.get(totalValue).equals(resultsValue.get(offsetValue)))) {
                return new AssertionReport("The array elements are not equal: return.data.results[return.data.offset] == return.data.results[return.data.total-1]");
            }

        }

        return new AssertionReport();
    }


    // API: Marvel
    // return.data.results[return.data.offset] == return.data.results[return.data.count-1]
    public static AssertionReport marvelOffsetAndCountAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        // Check ppt-name
        if(!invariantData.getPptname().equals("main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT")) {
            throw new Exception("Incorrect ppt-name, expected: main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT");
        }
        List<JsonNode> offsetValues = getSingleVariableValues(testCase, invariantData, "return.data.offset");
        List<JsonNode> countValues = getSingleVariableValues(testCase, invariantData, "return.data.count");

        List<JsonNode> resultsValues = getSingleVariableValues(testCase, invariantData, "return.data.results");

        if(offsetValues.size() != countValues.size() || offsetValues.size() != resultsValues.size()) {
            throw new Exception("Different sizes for lists that should have the same size");
        }

        for(int i = 0; i<offsetValues.size(); i++) {
            int offsetValue = offsetValues.get(i).intValue();
            int countValue = countValues.get(i).intValue() - 1;

            ArrayNode resultsValue = (ArrayNode) resultsValues.get(i);

            // If one of the values is less than 0
            if(offsetValue<0){
                return new AssertionReport("return.data.offset cannot be less than zero, got: " + offsetValue);
            }
            if(countValue<0) {
                return new AssertionReport("return.data.count-1 cannot be less than zero, got: " + countValue);
            }

            // If one of the values is greater than the size of the array
            if(countValue > (resultsValue.size() - 1)) {
                return new AssertionReport("The value of return.data.count-1 (" + countValue + ") cannot be used to access an array of size " + resultsValue.size());
            }
            if(offsetValue > (resultsValue.size() - 1)) {
                return new AssertionReport("The value of return.data.offset (" + offsetValue + ") cannot be used to access an array of size " + resultsValue.size());
            }

            if((countValue != offsetValue) && (!resultsValue.get(countValue).equals(resultsValue.get(offsetValue)))) {
                return new AssertionReport("The array elements are not equal: return.data.results[return.data.offset] == return.data.results[return.data.count-1]");
            }

        }

        return new AssertionReport();
    }

    // API: Marvel
    // return.data.results[] elements == return.data.results[return.data.offset]
    public static AssertionReport marvelResultsElementsOffsetAssertion(TestCase testCase, InvariantData invariantData, List<String> stringsToConsiderAsNull) throws Exception {
        // Check ppt-name
        if(!invariantData.getPptname().equals("main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT")) {
            throw new Exception("Incorrect ppt-name, expected: main.v1publiccomics{comicId}.getComicIndividual&200(main.getComicIndividual&Input):::EXIT");
        }

        List<JsonNode> offsetValues = getSingleVariableValues(testCase, invariantData, "return.data.offset");
        List<JsonNode> resultsValues = getSingleVariableValues(testCase, invariantData, "return.data.results");

        if(offsetValues.size() != resultsValues.size()) {
            throw new Exception("Different sizes for lists that should have the same size");
        }

        for(int i = 0; i<offsetValues.size(); i++) {
            int offsetValue = offsetValues.get(i).intValue();

            ArrayNode resultsValue = (ArrayNode) resultsValues.get(i);

            // If one of the values is less than 0
            if(offsetValue<0){
                return new AssertionReport("return.data.offset cannot be less than zero, got: " + offsetValue);
            }

            // If one of the values is greater than the size of the array
            if(offsetValue > (resultsValue.size() - 1)) {
                return new AssertionReport("The value of return.data.offset (" + offsetValue + ") cannot be used to access an array of size " + resultsValue.size());
            }

            for(JsonNode arrayElement: resultsValue) {
                if(!arrayElement.equals(resultsValue.get(offsetValue))) {
                    return new AssertionReport("The array elements are not equal: return.data.results[] elements == return.data.results[return.data.offset]");
                }
            }

        }


        return new AssertionReport();
    }



}

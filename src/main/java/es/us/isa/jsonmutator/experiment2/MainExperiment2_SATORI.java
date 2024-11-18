package es.us.isa.jsonmutator.experiment2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static es.us.isa.jsonmutator.experiment2.MutateTestCases.mutateTestCases;

/**
 * @author Juan C. Alonso
 * JSONMutator main method used in the SATORI paper. Generates a single mutated test suite.
 */
public class MainExperiment2_SATORI {

    // Number used to identify the mutated test suite
    private static int nTestSuite = 1;

    // Location of the CSV file containing the original test cases
    private static String originalTestCasesPath = "C:\\Users\\jcav\\Documents\\GitHub\\JSONmutator\\src\\test\\resources\\test_delete\\github_original_test_suite.csv";

    // Directory that will store the resulting mutated test suites (one per execution)
    private static String mutatedTestCasesDirectory = "C:\\Users\\jcav\\Documents\\GitHub\\JSONmutator\\src\\test\\resources\\test_delete\\mutants";


    public static void main(String[] args) throws IOException {

        if (args.length == 3) {
            nTestSuite = Integer.parseInt(args[0]);
            originalTestCasesPath = args[1];

            mutatedTestCasesDirectory = args[2];
        }

        // Delete previous mutants (if any) and create an empty one
        File mutatedTestCasesFile = new File(mutatedTestCasesDirectory);
        FileUtils.deleteDirectory(mutatedTestCasesFile);
        mutatedTestCasesFile.mkdir();

        // Generate mutated test suite, a total of nExecutions times
        Path mutatedTestCasesPath = Paths.get(mutatedTestCasesDirectory, String.format("mutants_%04d.csv", nTestSuite));

        // Generate mutated test cases
        mutateTestCases(mutatedTestCasesPath.toString(), originalTestCasesPath);



    }
}

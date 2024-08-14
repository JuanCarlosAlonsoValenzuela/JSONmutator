package es.us.isa.jsonmutator.experiment2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static es.us.isa.jsonmutator.experiment2.MutateTestCases.mutateTestCases;

/**
 * @author Juan C. Alonso
 */
public class MainExperiment2 {

    // Number of mutated versions of the original test suite to generate
    private static int nExecutions = 1;

    // Location of the CSV file containing the original test cases
    private static String originalTestCasesPath = "C:\\Users\\jcav\\Documents\\GitHub\\JSONmutator\\src\\test\\resources\\test_delete\\original_test_suite.csv";

    // Directory that will store the resulting mutated test suites (one per execution)
    private static String mutatedTestCasesDirectory = "C:\\Users\\jcav\\Documents\\GitHub\\JSONmutator\\src\\test\\resources\\test_delete\\mutants";


    public static void main(String[] args) throws IOException {

        if (args.length == 3) {
           nExecutions = Integer.parseInt(args[0]);
           originalTestCasesPath = args[1];

           mutatedTestCasesDirectory = args[2];
        }

        // Delete previous mutants (if any) and create an empty one
        File mutatedTestCasesFile = new File(mutatedTestCasesDirectory);
        FileUtils.deleteDirectory(mutatedTestCasesFile);
        mutatedTestCasesFile.mkdir();

        // Generate mutant files, a total of nExecutions times
        for (int i = 1; i <= nExecutions; i++) {

            Path mutatedTestCasesPath = Paths.get(mutatedTestCasesDirectory, String.format("mutants_%04d.csv", i));

            // Generate mutated test cases
            mutateTestCases(mutatedTestCasesPath.toString(), originalTestCasesPath);

        }

    }
}

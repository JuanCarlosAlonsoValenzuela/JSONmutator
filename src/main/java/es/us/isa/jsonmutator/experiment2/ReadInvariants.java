package es.us.isa.jsonmutator.experiment2;

import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;
import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantDataFileManager;

import java.io.IOException;
import java.util.List;

import static es.us.isa.jsonmutator.experiment2.readTestCases.CSVManager.readCSV;

public class ReadInvariants {


    public static List<InvariantData> getInvariantsDataFromPath(String invariantsPath) {

        // Read the csv file as a list of rows
        List<List<String>> rows = readCSV(invariantsPath, true, ';');

        List<String> header = rows.get(0);

        rows = rows.subList(1, rows.size());

        InvariantDataFileManager invariantDataFileManager = null;
        try {
            invariantDataFileManager = new InvariantDataFileManager(header);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<InvariantData> invariantsData = invariantDataFileManager.getInvariantsData(rows);

        return invariantsData;

    }


}

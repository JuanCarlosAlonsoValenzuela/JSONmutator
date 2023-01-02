package es.us.isa.jsonmutator.experiment2.generateAssertions;

import es.us.isa.jsonmutator.experiment2.readInvariants.InvariantData;

public class MutantTestCaseReport {

    private boolean killed;
    private InvariantData killedBy;
    private String description;

    public MutantTestCaseReport() {
        this.killed = false;
        this.killedBy = null;
        this.description = null;
    }

    public MutantTestCaseReport(InvariantData invariantData) {
        this.killed = true;
        this.killedBy = invariantData;
        this.description = null;
    }


    public boolean isKilled() {
        return killed;
    }

    public InvariantData getKilledBy() {
        return killedBy;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return "AssertionReport{" +
                "killed=" + killed +
                ", killedBy=" + killedBy +
                ", description='" + description + '\'' +
                '}';
    }
}

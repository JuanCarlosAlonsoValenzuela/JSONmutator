package es.us.isa.jsonmutator.experiment2.generateAssertions;

public class AssertionReport {

    private boolean satisfied;
    private String description;

    public AssertionReport() {
        this.satisfied = true;
        this.description = null;
    }

    public AssertionReport(String description) {
        this.satisfied = false;
        this.description = description;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public String getDescription() {
        return description;
    }
}

package es.us.isa.jsonmutator.experiment2.readInvariants;

import java.util.List;

public class InvariantData {

    private String pptname;
    private String invariant;
    private String invariantType;
    private List<String> variables;

    public InvariantData(String pptname, String invariant, String invariantType, List<String> variables) {
        this.pptname = pptname;
        this.invariant = invariant;
        this.invariantType = invariantType;
        this.variables = variables;
    }

    public String getPptname() {
        return pptname;
    }

    public String getInvariant() {
        return invariant;
    }

    public String getInvariantType() {
        return invariantType;
    }

    public List<String> getVariables() {
        return variables;
    }

    public String toString() {
        return "InvariantData{" +
                "pptname='" + pptname + '\'' +
                ", invariant='" + invariant + '\'' +
                ", invariantType='" + invariantType + '\'' +
                ", variables=" + variables +
                '}';
    }
}

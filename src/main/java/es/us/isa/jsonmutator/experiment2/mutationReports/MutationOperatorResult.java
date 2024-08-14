package es.us.isa.jsonmutator.experiment2.mutationReports;

/**
 * @author Juan C. Alonso
 * Contains specific information about the results of executing a mutation operator
 */
public class MutationOperatorResult {

    private boolean success;
    private String mutationOperatorName;
    private String originalValue;
    private String mutatedValue;

    // Used when the mutation is successfully performed
    public MutationOperatorResult(String mutationOperatorName, Object originalValue, Object mutatedValue) {
        this.success = true;
        this.mutationOperatorName = mutationOperatorName;

        this.originalValue = (originalValue != null) ? originalValue.toString() : null;
        this.mutatedValue = (mutatedValue != null) ? mutatedValue.toString() : null;

    }

    // Used when something goes wrong
    public MutationOperatorResult() {
        this.success = false;
        this.mutationOperatorName = null;
        this.originalValue = null;
        this.mutatedValue = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMutationOperatorName() {
        return mutationOperatorName;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getMutatedValue() {
        return mutatedValue;
    }

}

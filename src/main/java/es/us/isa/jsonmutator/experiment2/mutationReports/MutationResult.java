package es.us.isa.jsonmutator.experiment2.mutationReports;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Juan C. Alonso
 */
public class MutationResult {

    private ElementMutationResult elementMutationResult;
    private JsonNode mutatedJsonNode;
    private String variableHierarchy;

    public MutationResult(ElementMutationResult elementMutationResult, JsonNode mutatedJsonNode,
                          String variableHierarchy) {
        this.elementMutationResult = elementMutationResult;
        this.mutatedJsonNode = mutatedJsonNode;
        this.variableHierarchy = variableHierarchy;
    }

    public ElementMutationResult getElementMutationResult() {
        return elementMutationResult;
    }

    public JsonNode getMutatedJsonNode() {
        return mutatedJsonNode;
    }

    public String getVariableHierarchy() {
        return variableHierarchy;
    }
}

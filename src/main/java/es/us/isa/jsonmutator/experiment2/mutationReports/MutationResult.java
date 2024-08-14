package es.us.isa.jsonmutator.experiment2.mutationReports;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Juan C. Alonso
 */
public class MutationResult {

    private ElementMutationResult elementMutationResult;
    private JsonNode mutatedJsonNode;

    public MutationResult(ElementMutationResult elementMutationResult, JsonNode mutatedJsonNode) {
        this.elementMutationResult = elementMutationResult;
        this.mutatedJsonNode = mutatedJsonNode;
    }

    public ElementMutationResult getElementMutationResult() {
        return elementMutationResult;
    }

    public JsonNode getMutatedJsonNode() {
        return mutatedJsonNode;
    }

}

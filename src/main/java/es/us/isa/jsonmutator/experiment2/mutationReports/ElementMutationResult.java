package es.us.isa.jsonmutator.experiment2.mutationReports;

/**
 * @author Juan C. Alonso
 */
public class ElementMutationResult {

    private MutationOperatorResult mutationOperatorResult;
    private String mutatedPropertyDatatype;
    private String elementType;             // Type of the JSON element to mutate (either array or object)

    private Integer elementIndex;           // Only if the element to mutate is an array
    private String propertyName;            // Only if the element to mutate is an object

    // Used when the element to mutate is an array
    public ElementMutationResult(MutationOperatorResult mutationOperatorResult, String mutatedPropertyDatatype,
                                 Integer elementIndex) {
        this.mutationOperatorResult = mutationOperatorResult;
        this.mutatedPropertyDatatype = mutatedPropertyDatatype;
        this.elementType = "array";

        this.elementIndex = elementIndex;
        this.propertyName = null;
    }

    // Used when the element to mutate is an object
    public ElementMutationResult(MutationOperatorResult mutationOperatorResult, String mutatedPropertyDatatype,
                                 String propertyName) {
        this.mutationOperatorResult = mutationOperatorResult;
        this.mutatedPropertyDatatype = mutatedPropertyDatatype;
        this.elementType = "object";

        this.elementIndex = null;
        this.propertyName = propertyName;

    }

    public MutationOperatorResult getMutationOperatorResult() {
        return mutationOperatorResult;
    }

    public String getElementType() {
        return elementType;
    }

    public Integer getElementIndex() {
        return elementIndex;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getMutatedPropertyDatatype() {
        return mutatedPropertyDatatype;
    }

}

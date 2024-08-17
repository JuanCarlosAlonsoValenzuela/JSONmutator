package es.us.isa.jsonmutator.experiment2.mutationReports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Juan C. Alonso
 */
public class TreeNode {

    private String id;
    private String value;
    private List<TreeNode> children;

    public TreeNode(String value) {
        this.id = UUID.randomUUID().toString();
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public List<TreeNode> getChildren() {
        return children;
    }
}

package es.us.isa.jsonmutator.experiment2.mutationReports;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Juan C. Alonso
 */
public class TreePathFinder {

    public static String findPath(TreeNode root, String targetId) {
        List<String> path = new ArrayList<>();
        if (findPathHelper(root, targetId, path)) {
            return String.join(".", path.subList(1, path.size()));
        }
        return "";  // Return empty string if the target is not found

    }

    private static boolean findPathHelper(TreeNode node, String targetId, List<String> path) {
        if (node == null) {
            return false;
        }

        // Add the current node's value to the path
        path.add(node.getValue());

        // Check if the current node is the target node by ID
        if (node.getId().equals(targetId)) {
            return true;
        }

        // Recur for each child
        for (TreeNode child : node.getChildren()) {
            if (findPathHelper(child, targetId, path)) {
                return true;
            }
        }

        // If the target is not found in the subtree rooted at the current node,
        // remove the current node's value from the path and return false
        path.remove(path.size() - 1);
        return false;
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode("root");
        TreeNode nodeA1 = new TreeNode("A");
        TreeNode nodeA2 = new TreeNode("A");
        TreeNode nodeB = new TreeNode("B");
        TreeNode nodeC = new TreeNode("C");
        TreeNode nodeD = new TreeNode("D");

        root.addChild(nodeA1);
        root.addChild(nodeB);
        nodeB.addChild(nodeA2);
        nodeA1.addChild(nodeC);
        nodeA2.addChild(nodeD);

        // Assuming we want to find the path to nodeA2
        String path = findPath(root, nodeA2.getId());
        System.out.println(path);

    }

}

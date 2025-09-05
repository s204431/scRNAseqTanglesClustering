package clustering;

import util.BitSet;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class TangleSearchTree {

    //NOTE: This file is from the bachelor project.

    //This class represents a tangle search tree.

    private static final boolean USE_HASHING = false; //Determines if hashing of intersections is used.
    private final int a;
    protected Node root;
    protected List<Node> lowestDepthNodes = new ArrayList<>();
    private int currentDepth = -1;

    private final BitSet[] cuts;
    private final double[] cutCosts;
    private double minCost;
    private double maxCost;
    private final int integerBits; //Number of bits to represent the index of an orientation.
    private final Hashtable<Long, Integer> hashtable = new Hashtable<>();
    protected double[][] softClustering;
    protected int[] hardClustering;

    //Constructor receiving a, all cuts and the cost for each cut.
    protected TangleSearchTree(int a, BitSet[] cuts, double[] cutCosts) {
        this.a = a;
        this.cuts = cuts;
        this.cutCosts = cutCosts;
        integerBits = (int)(Math.log(cutCosts.length)/Math.log(2))+1;
        root = new Node();
        lowestDepthNodes.add(root);
    }

    //Adds an orientation as a child of the specified node with direction specified by "left". orientationIndex is the index of the cut in the "cuts" array.
    protected boolean addOrientation(Node node, int orientationIndex, boolean left) {
        Node newNode = new Node(orientationIndex, left);
        newNode.parent = node;
        if (left) {
            node.leftChild = newNode;
        }
        else {
            node.rightChild = newNode;
        }
        boolean consistent = isConsistent(newNode);
        if (!consistent) {
            newNode.parent = null;
            if (left) {
                node.leftChild = null;
            }
            else {
                node.rightChild = null;
            }
        }
        else {
            int depth = getDepth(newNode);
            if (depth != currentDepth) {
                lowestDepthNodes = new ArrayList<>();
                currentDepth = depth;
            }
            lowestDepthNodes.add(newNode);
        }
        return consistent;
    }

    //Checks whether the tree is still consistent after adding "newNode".
    private boolean isConsistent(Node newNode) {
        int depth = getDepth(newNode);
        if (depth < 2) {
            return cuts[newNode.originalOrientation].size() >= a;
        }
        if (depth == 2) {
            int intersection = BitSet.intersectionEarlyStop(cuts[newNode.originalOrientation], cuts[newNode.parent.originalOrientation], newNode.side, newNode.parent.side, a);
            return intersection >= a;
        }
        Node[] otherNodes = new Node[depth-1];
        otherNodes[0] = newNode.parent;
        for (int i = 1; i < depth-1; i++) {
            otherNodes[i] = otherNodes[i-1].parent;
        }
        for (int i = 0; i < depth-1; i++) {
            for (int j = i+1; j < depth-1; j++) {
                int intersection;
                if (USE_HASHING) {
                    int hashed = getHashValue(newNode.originalOrientation, otherNodes[i].originalOrientation, otherNodes[j].originalOrientation, newNode.side, otherNodes[i].side, otherNodes[j].side);
                    if (hashed >= 0) {
                        intersection = hashed;
                    }
                    else {
                        intersection = BitSet.intersectionEarlyStop(cuts[newNode.originalOrientation], cuts[otherNodes[i].originalOrientation], cuts[otherNodes[j].originalOrientation], newNode.side, otherNodes[i].side, otherNodes[j].side, a);
                        addToHash(newNode.originalOrientation, otherNodes[i].originalOrientation, otherNodes[j].originalOrientation, newNode.side, otherNodes[i].side, otherNodes[j].side, intersection);
                    }
                }
                else {
                    intersection = BitSet.intersectionEarlyStop(cuts[newNode.originalOrientation], cuts[otherNodes[i].originalOrientation], cuts[otherNodes[j].originalOrientation], newNode.side, otherNodes[i].side, otherNodes[j].side, a);
                }
                if (intersection < a) {
                    return false;
                }
            }
        }
        return true;
    }

    //Returns the depth of the specified node.
    private int getDepth(Node node) {
        int depth = 0;
        while (node.parent != null) {
            node = node.parent;
            depth++;
        }
        return depth;
    }

    //Calculates the hard clustering of each point.
    protected void calculateHardClustering() {
        if (softClustering == null) {
            calculateSoftClustering();
        }
        int[] hardClustering = new int[softClustering.length];
        for (int i = 0; i < softClustering.length; i++) {
            int maxCluster = 0;
            double max = 0;
            for (int j = 0; j < softClustering[i].length; j++) {
                if (softClustering[i][j] > max) {
                    max = softClustering[i][j];
                    maxCluster = j;
                }
            }
            hardClustering[i] = maxCluster;
        }
        this.hardClustering = hardClustering;
    }

    //Calculates the soft clustering of each point.
    protected void calculateSoftClustering() {
        calculateMinMaxCost(cutCosts);
        int clusters = getNumberOfClusters(root);
        double[][] result = new double[cuts[0].size()][clusters];
        for (int i = 0; i < cuts[0].size(); i++) {
            getSoftClustering(root, i, 0, 1, result[i]);
        }
        softClustering = result;
    }

    //Calculates the minimum and maximum cost of a cut.
    private void calculateMinMaxCost(double[] cutCosts) {
        minCost = Double.MAX_VALUE;
        maxCost = Double.MIN_VALUE;
        for (double cutCost : cutCosts) {
            if (cutCost > 0 && cutCost < minCost) {
                minCost = cutCost;
            }
            if (cutCost > maxCost) {
                maxCost = cutCost;
            }
        }
    }

    //Generates a default clustering with one cluster.
    protected void generateDefaultClustering() {
        double[][] result = new double[cuts[0].size()][1];
        for (int i = 0; i < cuts[0].size(); i++) {
            result[i][0] = 1;
        }
        softClustering = result;
        calculateHardClustering();
    }

    //Returns the weight assigned to a specific cost.
    private double getWeight(double cost) {
        return cost == 0.0 || (maxCost-minCost) == 0.0 ? 1.0 : Math.exp(-((cost-minCost)/(maxCost-minCost)));
    }

    //Calculates the soft clustering for a specific data point recursively.
    private int getSoftClustering(Node node, int datapoint, int index, double accumulated, double[] result) {
        if (node.getChildCount() == 0) {
            result[index] = accumulated;
            return index+1;
        }
        else {
            double sum1 = 0;
            double sum2 = 0;
            for (int distinguished : node.distinguishedCuts) {
                if (node.leftChild.condensedOrientations.get(distinguished)) {
                    sum2 += getWeight(cutCosts[distinguished]);
                    if (!cuts[distinguished].get(datapoint)) {
                        sum1 += getWeight(cutCosts[distinguished]);
                    }
                }
                if (node.leftChild.condensedOrientations.get(distinguished+node.leftChild.condensedOrientations.size()/2)) {
                    sum2 += getWeight(cutCosts[distinguished]);
                    if (cuts[distinguished].get(datapoint)) {
                        sum1 += getWeight(cutCosts[distinguished]);
                    }
                }
            }
            double prob = sum1/sum2;
            index = getSoftClustering(node.leftChild, datapoint, index, accumulated*prob, result);
            index = getSoftClustering(node.rightChild, datapoint, index, accumulated*(1-prob), result);
            return index;
        }
    }

    //Returns the number of clusters found by the algorithm.
    private int getNumberOfClusters(Node node) {
        if (node.getChildCount() == 0) {
            return 1;
        }
        else {
            return getNumberOfClusters(node.leftChild) + getNumberOfClusters(node.rightChild);
        }
    }

    //Contracts the tree.
    protected void contractTree() {
        contractTree(root);
    }

    //Recursively contracts the tree.
    private void contractTree(Node node) {
        if (node.getChildCount() > 0) { //This is not a leaf.
            contractTree(node.leftChild);
            contractTree(node.rightChild);
            int size = node.leftChild.condensedOrientations.size();
            for (int i = 0; i < size; i++) {
                if (node.leftChild.condensedOrientations.get(i)) {
                    if (node.rightChild.condensedOrientations.get(i)) { //Left and right child orient this cut the same way.
                        node.condensedOrientations.add(i);
                    }
                    else if ((i < size/2 && node.rightChild.condensedOrientations.get(i+size/2)) || (i >= size/2 && node.rightChild.condensedOrientations.get(i-size/2))) { //Oriented different ways.
                        node.distinguishedCuts.add(i < size/2 ? i : i-size/2);
                    }
                }
            }
        }
    }

    //Removes internal nodes with exactly one child and removes branches of length "pruneDepth" or lower from the tree.
    protected void condenseTree(int pruneDepth) {
        removeInternalNodes(root);
        pruneBranches(root, pruneDepth);
    }

    //Removes branches of length "pruneDepth" or lower from the tree.
    private void pruneBranches(Node node, int pruneDepth) {
        if (node.getChildCount() == 0) { //This is a leaf.
            if (node.originalDepth <= pruneDepth) {
                if (node.parent.leftChild == node) {
                    node.parent.leftChild = null;
                }
                else {
                    node.parent.rightChild = null;
                }
                if (node.parent.getChildCount() == 1) {
                    removeNode(node.parent);
                }
            }
        }
        else { //This is not a leaf.
            pruneBranches(node.leftChild, pruneDepth);
            pruneBranches(node.rightChild, pruneDepth);
        }
    }

    //Removes the specified node from the tree.
    private void removeNode(Node node) {
        Node child = node.leftChild == null ? node.rightChild : node.leftChild;
        child.originalDepth++;
        child.parent = node.parent;
        if (node.parent != null) { //Not root.
            if (node.parent.leftChild == node) {
                node.parent.leftChild = child;
            }
            else{
                node.parent.rightChild = child;
            }
        }
        else {
            root = child;
        }
        child.condensedOrientations.unionWith(node.condensedOrientations);
    }

    //Removes internal nodes with exactly one child.
    private void removeInternalNodes(Node node) {
        if (node.leftChild != null) {
            removeInternalNodes(node.leftChild);
        }
        if (node.rightChild != null) {
            removeInternalNodes(node.rightChild);
        }
        if (node.getChildCount() == 1) { //Remove node.
            removeNode(node);
        }
    }

    //Prints the side of the cut for each node in the tree (for debugging).
    protected void printTree(boolean asGraphviz, boolean contracted) {
        if (asGraphviz) {
            System.out.println("digraph G {");
        }
        List<Node> currentNodes = new ArrayList<>();
        currentNodes.add(root);
        int depth = 0;
        while (!currentNodes.isEmpty()) {
            for (Node currentNode : currentNodes) {
                if (!asGraphviz) {
                    System.out.print(currentNode.side + " " + currentNode.getChildCount());
                }
            }
            int index1 = 0;
            int index2 = 0;
            List<Node> newNodes = new ArrayList<>();
            for (Node node : currentNodes) {
                if (node.leftChild != null) {
                    newNodes.add(node.leftChild);
                    if (asGraphviz) {
                        String extra1 = contracted ? "/" + node.distinguishedCuts.size() + "/" + node.condensedOrientations.count() : "";
                        String extra2 = contracted ? "/" + node.leftChild.distinguishedCuts.size() + "/" + node.leftChild.condensedOrientations.count() : "";
                        System.out.println("\""+depth+"/"+index1+"/"+(node.side ? "L" : "R")+extra1+"\""+" -> "+"\""+(depth+1)+"/"+index2+"/"+(node.leftChild.side ? "L" : "R")+extra2+"\"");
                    }
                    index2++;
                }
                if (node.rightChild != null) {
                    newNodes.add(node.rightChild);
                    if (asGraphviz) {
                        String extra1 = contracted ? "/" + node.distinguishedCuts.size() + "/" + node.condensedOrientations.count() : "";
                        String extra2 = contracted ? "/" + node.rightChild.distinguishedCuts.size() + "/" + node.rightChild.condensedOrientations.count() : "";
                        System.out.println("\""+depth+"/"+index1+"/"+(node.side ? "L" : "R")+extra1+"\""+" -> "+"\""+(depth+1)+"/"+index2+"/"+(node.rightChild.side ? "L" : "R")+extra2+"\"");
                    }
                    index2++;
                }
                index1++;
            }
            currentNodes = newNodes;
            depth++;
            System.out.println();
        }
        if (asGraphviz) {
            System.out.println("}");
        }
    }

    //Adds an intersection to the hash table (not used).
    private void addToHash(long cut1, long cut2, long cut3, boolean side1, boolean side2, boolean side3, int value) {
        long hashKey = getHashKey(cut1, cut2, cut3, side1, side2, side3);
        hashtable.put(hashKey, value);
    }

    //Returns the hashed value of an intersection (not used).
    private int getHashValue(long cut1, long cut2, long cut3, boolean side1, boolean side2, boolean side3) {
        long hashKey = getHashKey(cut1, cut2, cut3, side1, side2, side3);
        Integer hashValue = hashtable.get(hashKey);
        if (hashValue != null) {
            return hashValue;
        }
        return -1;
    }

    //Calculates the hash key of an intersection (not used).
    private long getHashKey(long cut1, long cut2, long cut3, boolean side1, boolean side2, boolean side3) {
        long l1 = ((side1 ? 0L : 1L) << ((integerBits+1)*3-1)) | (cut1 << (integerBits+1)*2);
        long l2 = ((side2 ? 0L : 1L) << ((integerBits+1)*2-1)) | (cut2 << (integerBits+1));
        long l3 = ((side3 ? 0L : 1L) << integerBits) | cut3;
        return l1 | l2 | l3;
    }

    //Returns the root of the tree
    public Node getRoot() {
        return root;
    }

    public class Node {

        //This class represents the node of the tree.

        public int originalOrientation;
        public final BitSet condensedOrientations;
        public final List<Integer> distinguishedCuts = new ArrayList<>();
        public Node leftChild;
        public Node rightChild;
        public Node parent;
        public boolean side;
        public int originalDepth = 1;

        //Creates a default node (used to generate the root).
        private Node() {
            condensedOrientations = new BitSet(cuts.length*2);
        }

        //Creates a node with a specific orientation.
        private Node(int orientationIndex, boolean side) {
            this.originalOrientation = orientationIndex;
            this.side = side;
            condensedOrientations = new BitSet(cuts.length*2);
            condensedOrientations.add(side ? orientationIndex : orientationIndex+condensedOrientations.size()/2);
        }

        //Returns the child count of the node.
        private int getChildCount() {
            int count = 0;
            if (leftChild != null) {
                count++;
            }
            if (rightChild != null) {
                count++;
            }
            return count;
        }
    }
}

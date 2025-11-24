package clustering;

import util.BitSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class TangleSearchTree {

    //NOTE: This file is from the bachelor project.

    //This class represents a tangle search tree.

    private static final boolean USE_HASHING = false; //Determines if hashing of intersections is used.
    protected int a;
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

    protected List<double[]> branchCosts;

    //Constructor receiving a, all cuts and the cost for each cut.
    protected TangleSearchTree(int a, BitSet[] cuts, double[] cutCosts) {
        this.a = a;
        this.cuts = cuts;
        this.cutCosts = cutCosts;
        integerBits = (int)(Math.log(cutCosts.length)/Math.log(2))+1;
        root = new Node();
        root.intersection = new BitSet(cuts[0].size());
        root.intersection.setAll();
        lowestDepthNodes.add(root);
    }

    //Adds an orientation as a child of the specified node with direction specified by "left". orientationIndex is the index of the cut in the "cuts" array.
    protected boolean addOrientation(Node node, int orientationIndex, boolean left, boolean alternateConsistencyCheck) {
        Node newNode = new Node(orientationIndex, left);
        newNode.parent = node;
        if (left) {
            node.leftChild = newNode;
        }
        else {
            node.rightChild = newNode;
        }
        boolean consistent = alternateConsistencyCheck ? isConsistentOscarWerner(newNode) : isConsistent(newNode);
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

            newNode.branchId = newNode.parent.branchId;
            newNode.intersection = cuts[orientationIndex].clone();
            if (left) {
                newNode.intersection.flipALl();
            }

            if (newNode.parent.intersection != null) {
                if (newNode.parent.leftChild != null && newNode.parent.rightChild != null) {
                    newNode.intersection.intersectWith(newNode.parent.intersection);

                    // First child did not know if sibling was going to be added, so compute intersection for first child in this case.
                    newNode.parent.leftChild.intersection = cuts[newNode.parent.leftChild.originalOrientation].clone();
                    newNode.parent.leftChild.intersection.flipALl();
                    newNode.parent.leftChild.intersection.intersectWith(newNode.parent.intersection);
                }
                else {
                    newNode.intersection = newNode.parent.intersection;
                }
            }

            if (TangleClusterer.earlyStop) {
                int depth = getDepth(newNode);
                if (depth != currentDepth) {
                    lowestDepthNodes = new ArrayList<>();
                    currentDepth = depth;
                }
            }
            else {
                lowestDepthNodes.remove(node);
            }
            lowestDepthNodes.add(newNode);
        }
        return consistent;
    }

    //Checks whether the tree is still consistent after adding "newNode".
    protected boolean isConsistent(Node newNode) {
        int depth = getDepth(newNode);
        if (depth < 2) {
            return cuts[newNode.originalOrientation].countFlipped(newNode.side) >= a;
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

    protected boolean isConsistentOscarWerner(Node newNode) {
        int depth = getDepth(newNode);

        if (depth < 2) {
            return BitSet.intersectionEarlyStop(cuts[newNode.originalOrientation], newNode.parent.intersection, newNode.side, false, a) >= a;
        }
        if (depth == 2) {
            int intersection = BitSet.intersectionEarlyStop(cuts[newNode.originalOrientation], cuts[newNode.parent.originalOrientation], newNode.parent.intersection, newNode.side, newNode.parent.side, false, a);
            return intersection >= a;
        }

        BitSet cut = cuts[newNode.originalOrientation].clone();
        if (newNode.side) {
            cut.flipALl();
        }
        cut.intersectWith(newNode.parent.intersection);

        Node[] otherNodes = new Node[depth-1];
        otherNodes[0] = newNode.parent;
        for (int i = 1; i < depth-1; i++) {
            otherNodes[i] = otherNodes[i-1].parent;
        }
        for (int i = 0; i < depth-1; i++) {
            for (int j = i+1; j < depth-1; j++) {
                int intersection;
                intersection = BitSet.intersectionEarlyStop(cut, cuts[otherNodes[i].originalOrientation], cuts[otherNodes[j].originalOrientation], false, otherNodes[i].side, otherNodes[j].side, a);
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
                    if (branchCosts == null) {
                        sum2 += getWeight(cutCosts[distinguished]);
                        if (!cuts[distinguished].get(datapoint)) {
                            sum1 += getWeight(cutCosts[distinguished]);
                        }
                    } else {
                        //Use localized branch costs as weight
                        sum2 += getWeight(branchCosts.get(node.branchId)[distinguished]);
                        if (!cuts[distinguished].get(datapoint)) {
                            sum1 += getWeight(branchCosts.get(node.branchId)[distinguished]);
                        }
                    }
                }
                if (node.leftChild.condensedOrientations.get(distinguished+node.leftChild.condensedOrientations.size()/2)) {
                    if (branchCosts == null) {
                        sum2 += getWeight(cutCosts[distinguished]);
                        if (cuts[distinguished].get(datapoint)) {
                            sum1 += getWeight(cutCosts[distinguished]);
                        }
                    } else {
                        //Use localized branch costs as weight
                        sum2 += getWeight(branchCosts.get(node.branchId)[distinguished]);
                        if (cuts[distinguished].get(datapoint)) {
                            sum1 += getWeight(branchCosts.get(node.branchId)[distinguished]);
                        }
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

    protected void limitSplitCosts(List<Double> splitCostsList, double[][] reducedPoints, boolean tuningActivated) {
        //Remove all costs below the first cost.
        List<Double> newSplitCosts = new ArrayList<>();
        for (int i = 0; i < splitCostsList.size(); i++) {
            if (splitCostsList.get(i) >= splitCostsList.getFirst()) {
                newSplitCosts.add(splitCostsList.get(i));
            }
        }

        if (newSplitCosts.size() < 2) { //If there is only 0 or 1 split do not limit the cost.
            return;
        }

        double[] splitCosts = new double[newSplitCosts.size()];
        for (int i = 0; i < splitCosts.length; i++) {
            splitCosts[i] = newSplitCosts.get(i);
        }

        Arrays.sort(splitCosts);

        if (tuningActivated) {
            double bestScore = -1.0;
            double bestSplitCost = 0.0;
            double scoreSum = 0.0;
            for (int i = 0; i < splitCosts.length; i++) {
                double splitCost = splitCosts[i];
                TangleSearchTree tree = copy();
                try {
                    tree.limitSplitCosts(tree.root, splitCost);
                    tree.condenseTree(0);
                    tree.contractTree();
                    tree.calculateSoftClustering();
                    tree.calculateHardClustering();
                } catch (NullPointerException e) {
                    tree.generateDefaultClustering();
                }
                double silhouetteScore = Model.silhouetteScore(reducedPoints, tree.hardClustering);
                if (silhouetteScore >= 1.0) {
                    silhouetteScore = 0.0;
                }
                if (silhouetteScore > bestScore) {
                    bestScore = silhouetteScore;
                    bestSplitCost = splitCost;
                    scoreSum += silhouetteScore;
                }
            }
            System.out.println("Found max split cost: " + bestSplitCost);
            System.out.println("Silhouette score: " + bestScore);
            limitSplitCosts(root, bestSplitCost);
        }
        else {
            //double maxSplitCost = calculateMaxSplitCost(splitCosts);
            int index = Model.findElbow(splitCosts)-1;
            double maxSplitCost = splitCosts[index];
            limitSplitCosts(root, maxSplitCost);
        }
    }

    protected void limitSplitCosts(Node node, double maxCost) {

        // Split node
        if (node.leftChild != null && node.rightChild != null) {
            if ((branchCosts != null && branchCosts.get(node.branchId)[node.leftChild.originalOrientation] > maxCost) || (branchCosts == null && cutCosts[node.leftChild.originalOrientation] > maxCost)) { //Local cost
                //Remove child nodes since
                node.leftChild = null;
                node.rightChild = null;
            }
        }


        /*// Vote cuts (internal nodes)
        else {
            if (node.parent != null) {

                // Search for lowest node in parent branch
                Node lowestParentBranchNode = node.parent;
                while (lowestParentBranchNode.leftChild == null || lowestParentBranchNode.rightChild == null && lowestParentBranchNode.parent != null) {
                    lowestParentBranchNode = lowestParentBranchNode.parent;
                }

                if (node.leftChild != null) {
                    if ((branchCosts != null && branchCosts.get(lowestParentBranchNode.branchId)[node.leftChild.originalOrientation] > maxCost) || (branchCosts == null && cutCosts[node.leftChild.originalOrientation] > maxCost)) { //Local cost
                        node.leftChild = null;
                    }
                }
                if (node.rightChild != null) {
                    if ((branchCosts != null && branchCosts.get(lowestParentBranchNode.branchId)[node.rightChild.originalOrientation] > maxCost) || (branchCosts == null && cutCosts[node.rightChild.originalOrientation] > maxCost)) { //Local cost
                        node.rightChild = null;
                    }
                }
            }
        }*/

        // Recursive calls
        if (node.leftChild != null) {
            limitSplitCosts(node.leftChild, maxCost);
        }
        if (node.rightChild != null) {
            limitSplitCosts(node.rightChild, maxCost);
        }
    }

    //Calculates the maximum split cost to keep based on the mean cost in a window of a certain size.
    private double calculateMaxSplitCost(double[] splitCosts) {
        int windowSize = 4;

        double[] sumArray = new double[splitCosts.length]; //Sum of costs in window ending on a given index.
        sumArray[0] = splitCosts[0];
        for (int i = 1; i < sumArray.length; i++) {
            sumArray[i] = sumArray[i-1] + splitCosts[i] - (i-windowSize < 0 ? 0 : splitCosts[i-windowSize]);
        }

        //Find maximum difference from the mean in a window.
        double maxDifference = -1;
        int maxDifferenceIndex = -1;

        double sum = 0.0;

        for (int i = 1; i < sumArray.length; i++) {
            double mean = sumArray[i-1] / Math.min(i, windowSize);
            double difference = splitCosts[i] - mean;
            sum += difference;
            if (difference > maxDifference) {
                maxDifference = difference;
                maxDifferenceIndex = i;
            }
        }

        //Return split cost for cut before the max difference;
        System.out.println("Found max split cost: " + splitCosts[maxDifferenceIndex-1]);
        System.out.println("Certainty: " + (maxDifference/sum));
        return splitCosts[maxDifferenceIndex-1];
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

    //Returns a copy of the tree with only information used to draw the tree in the GUI
    public TangleSearchTree copy() {
        TangleSearchTree tree = new TangleSearchTree(a, cuts, cutCosts);
        tree.branchCosts = branchCosts;
        copyNode(root, tree.getRoot());
        return tree;
    }

    //Helper for recursively copying the tree
    public Node copyNode(Node oldNode, Node newNode) {
        newNode.originalOrientation = oldNode.originalOrientation;
        newNode.condensedOrientations = oldNode.condensedOrientations.clone();
        newNode.side = oldNode.side;
        newNode.branchId = oldNode.branchId;
        newNode.cost = oldNode.cost;
        if (oldNode.intersection != null) {
            newNode.intersection = oldNode.intersection.clone();
        }
        if (oldNode.leftChild != null) {
            newNode.leftChild = new Node();
            newNode.leftChild = copyNode(oldNode.leftChild, newNode.leftChild);
            newNode.leftChild.parent = newNode;
        }
        if (oldNode.rightChild != null) {
            newNode.rightChild = new Node();
            newNode.rightChild = copyNode(oldNode.rightChild, newNode.rightChild);
            newNode.rightChild.parent = newNode;
        }
        return newNode;
    }

    public class Node {

        //This class represents the node of the tree.

        public int originalOrientation;
        public BitSet condensedOrientations;
        public List<Integer> distinguishedCuts = new ArrayList<>();
        public Node leftChild;
        public Node rightChild;
        public Node parent;
        public boolean side;
        public int originalDepth = 1;
        public int branchId;
        public BitSet intersection;
        public double cost;

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

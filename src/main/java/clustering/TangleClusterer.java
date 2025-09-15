package clustering;

import clustering.TangleSearchTree.Node;
import monitor.Monitor;
import util.BitSet;
import util.Tuple;
import datasets.ScRNAseqDataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TangleClusterer {

    //NOTE: This file is from the bachelor project.

    //This class is used to generate a clustering with tangles.

    protected static boolean earlyStop = false;

    private TangleSearchTree tangleSearchTree;

    private Monitor monitor;

    //Ensure that it can only be created within this package.
    public TangleClusterer() {}

    //Generates a soft- and hard clustering for the provided dataset with a specific value of a and psi, and a specific initial cut generator and cost function.
    public void generateClusters(ScRNAseqDataset dataset, int a, double psi, String initialCutGenerator, String costFunctionName) {
        dataset.setA(a);
        BitSet[] initialCuts = dataset.getInitialCuts(initialCutGenerator);
        double[] costs = dataset.getCutCosts(costFunctionName);
        Tuple<BitSet[], double[]> redundancyRemoved = removeRedundantCuts(initialCuts, costs, 0.9); //Set factor to 1 to turn it off.
        initialCuts = redundancyRemoved.x;
        costs = redundancyRemoved.y;
        //TangleSearchTree tree = generateTangleSearchTree(initialCuts, costs, a, psi);
        TangleSearchTree tree = oscarWerner(initialCuts, costs, a, psi, dataset.data, costFunctionName);
        tangleSearchTree = tree;
        monitor.setUncondensedTree(tree.copy());
        try {
            tree.condenseTree(1);
        } catch (NullPointerException e) {
            tree.generateDefaultClustering();
            return;
        }
        monitor.setCondensedTree(tree.copy());
        tree.contractTree();
        tree.calculateSoftClustering();
        tree.calculateHardClustering();
    }

    //Returns the last generated soft clustering.
    public double[][] getSoftClustering() {
        return tangleSearchTree.softClustering;
    }

    //Returns the last generated hard clustering.
    public int[] getHardClustering() {
        return tangleSearchTree.hardClustering;
    }

    //Generates the tangle search tree by ordering the cuts and adding nodes one at a time to the tree.
    private TangleSearchTree generateTangleSearchTree(BitSet[] initialCuts, double[] costs, int a, double psi) {
        int[] indices = new int[costs.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        double[] costsOrdered = new double[costs.length];
        System.arraycopy(costs, 0, costsOrdered, 0, costs.length);
        quicksort(costsOrdered, indices, 0, costsOrdered.length-1);
        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);
        for (int i = 0; i < costsOrdered.length; i++) {
            if (psi > 0 && costsOrdered[i] > psi) {
                break;
            }
            boolean consistent = false;
            List<Node> lowestDepthNodesCopy = new ArrayList<>(tree.lowestDepthNodes);
            for (Node node : lowestDepthNodesCopy) {
                consistent = tree.addOrientation(node, indices[i], true) || consistent;
                consistent = tree.addOrientation(node, indices[i], false) || consistent;
            }
            if (earlyStop && !consistent) { //Stop if no nodes were added to the tree.
                break;
            }
        }
        return tree;
    }

    private TangleSearchTree oscarWerner(BitSet[] initialCuts, double[] costs, int a, double psi, double[][] data, String costFunctionName) {
        int n = costs.length;

        //Costs ordered for each branch ID.
        List<double[]> branchCosts = new ArrayList<>();
        double[] initialCosts = new double[n];
        for (int i = 0; i < n; i++) {
            initialCosts[i] = costs[i];
        }
        branchCosts.add(initialCosts);

        //Indices for ordered cost for each branch ID.
        List<int[]> indicesOrdered = new ArrayList<>();
        int[] initialIndices = new int[n];
        for (int i = 0; i < n; i++) {
            initialIndices[i] = i;
        }
        indicesOrdered.add(initialIndices);

        //Pointers for unused cuts for each branch
        List<Integer> branchPointers = new ArrayList<>();
        branchPointers.add(0);

        //Sets of cuts that we want to consider for each branch (cuts that are not redundant).
        List<HashSet<Integer>> branchCutSets = new ArrayList<>();
        HashSet<Integer> initialBranchCuts = new HashSet<>();
        for (int i = 0; i < initialCuts.length; i++) {
            initialBranchCuts.add(i);
        }
        branchCutSets.add(initialBranchCuts);

        for (int i = 0; i < n; i++) indicesOrdered.getFirst()[i] = i;
        System.arraycopy(costs, 0, branchCosts.getFirst(), 0, n);
        quicksort(branchCosts.getFirst(), indicesOrdered.getFirst(), 0, n - 1);

        /*int[] debugIndices = new int[n];
        for (int i = 0; i < n; i++) {
            debugIndices[initialIndices[i]] = i;
        }*/

        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);

        int branchId = 0;

        List<Node> lowestBranchNodes = new ArrayList<>();
        lowestBranchNodes.add(tree.root);
        while (!lowestBranchNodes.isEmpty()) {
            List<Node> newLowestBranchNodes = new ArrayList<>();

            for (Node node : lowestBranchNodes) {
                int[] branchIndicesOrdered = indicesOrdered.get(node.branchId);
                int branchPointer = branchPointers.get(node.branchId);
                HashSet<Integer> branchCuts = branchCutSets.get(node.branchId);

                for (int i = branchPointer; i < initialCuts.length; i++) {
                    boolean consistent = false;

                    if (psi != 0 && branchCosts.get(node.branchId)[branchIndicesOrdered[i]] > psi) {
                        break;
                    }

                    if (!branchCuts.contains(branchIndicesOrdered[i])) {
                        //System.out.println("Node " + debugIndices[node.originalOrientation]  + (node.side ? "L" : "R") + " in branch: " + node.branchId + " has skipped cut: " + debugIndices[branchIndicesOrdered[i]]);
                        branchPointer++;
                        continue;
                    }

                    consistent = tree.addOrientation(node, branchIndicesOrdered[i], true) || consistent;
                    consistent = tree.addOrientation(node, branchIndicesOrdered[i], false) || consistent;
                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                        //Left side
                        branchId++;
                        node.leftChild.branchId = branchId;
                        branchPointers.add(i + 1);

                        //Remove points that are not included in the intersection for that branch
                        Tuple<double[][], BitSet[]> leftRedundantPointsRemoved = removeRedundantPoints(data, initialCuts, node.leftChild.intersection);
                        double[][] newData = leftRedundantPointsRemoved.x;
                        BitSet[] newCuts = leftRedundantPointsRemoved.y;
                        ScRNAseqDataset newDataset = new ScRNAseqDataset(newData);
                        newDataset.setInitialCuts(newCuts);
                        double[] newCosts = newDataset.getCutCosts(costFunctionName);
                        branchCosts.add(newCosts);

                        //Reorder cuts and costs based on the cost order for the parent branch
                        int[] newIndices = branchIndicesOrdered.clone();
                        double[] reorderedCosts = new double[newCosts.length];
                        for (int j = 0; j < reorderedCosts.length; j++) {
                            reorderedCosts[j] = newCosts[newIndices[j]];
                        }

                        // Order costs and indices
                        quicksort(reorderedCosts, newIndices, i + 1, reorderedCosts.length - 1);
                        indicesOrdered.add(newIndices);

                        BitSet[] reorderedCuts = new BitSet[newCosts.length];
                        for (int j = 0; j < reorderedCosts.length; j++) {
                            reorderedCuts[j] = newCuts[newIndices[j]];
                        }

                        //Remove redundant cuts
                        int[] originalIndices = new int[newIndices.length];
                        for (int j = 0; j < newIndices.length; j++) {
                            originalIndices[j] = j;
                        }
                        Tuple<BitSet[], double[]> leftRedundantCutsRemoved = removeRedundantCuts(reorderedCuts, Arrays.stream(originalIndices).mapToDouble(k -> (double) k).toArray(), 0.95);
                        int[] ints = Arrays.stream(leftRedundantCutsRemoved.y).mapToInt(k -> (int) Math.round(k)).toArray();

                        /*System.out.println("Node: " + debugIndices[node.leftChild.originalOrientation] + (node.leftChild.side ? "L" : "R"));
                        for (int j = 0; j < newIndices.length; j++) {
                            System.out.print(debugIndices[newIndices[j]] + ", ");
                        }
                        System.out.println();
                        System.out.println(Arrays.toString(newIndices));
                        for (int j = 0; j < ints.length; j++) {
                            System.out.print(newIndices[ints[j]] + ", ");
                        }
                        System.out.println();*/

                        HashSet<Integer> leftCuts = new HashSet<>();
                        for (int j = 0; j < ints.length; j++) {
                            leftCuts.add(newIndices[ints[j]]);
                        }
                        branchCutSets.add(leftCuts);

                        newLowestBranchNodes.add(node.leftChild);


                        // Right side
                        branchId++;
                        node.rightChild.branchId = branchId;
                        branchPointers.add(i + 1);

                        // Remove points that are not included in the intersection for that branch
                        Tuple<double[][], BitSet[]> rightRedundancyRemoved = removeRedundantPoints(data, initialCuts, node.rightChild.intersection);
                        double[][] newData2 = rightRedundancyRemoved.x;
                        BitSet[] newCuts2 = rightRedundancyRemoved.y;
                        ScRNAseqDataset newDataset2 = new ScRNAseqDataset(newData2);
                        newDataset2.setInitialCuts(newCuts2);
                        double[] newCosts2 = newDataset2.getCutCosts(costFunctionName);
                        branchCosts.add(newCosts2);

                        // Reorder cuts and costs based on the cost order for the parent branch
                        int[] newIndices2 = branchIndicesOrdered.clone();
                        double[] reorderedCosts2 = new double[newCosts2.length];
                        for (int j = 0; j < newCosts2.length; j++) {
                            reorderedCosts2[j] = newCosts2[newIndices2[j]];
                        }

                        // Order costs and indices
                        quicksort(reorderedCosts2, newIndices2, i + 1, reorderedCosts2.length - 1);
                        indicesOrdered.add(newIndices2);

                        BitSet[] reorderedCuts2 = new BitSet[newCuts2.length];
                        for (int j = 0; j < newCosts2.length; j++) {
                            reorderedCuts2[j] = newCuts2[newIndices2[j]];
                        }

                        // Remove redundant cuts
                        Tuple<BitSet[], double[]> rightRedundantCutsRemoved = removeRedundantCuts(reorderedCuts2, Arrays.stream(originalIndices).mapToDouble(k -> (double) k).toArray(), 0.95);
                        int[] ints2 = Arrays.stream(rightRedundantCutsRemoved.y).mapToInt(k -> (int) Math.round(k)).toArray();

                        /*System.out.println("Node: " + debugIndices[node.rightChild.originalOrientation] + (node.rightChild.side ? "L" : "R"));
                        for (int j = 0; j < newIndices2.length; j++) {
                            System.out.print(debugIndices[newIndices2[j]] + ", ");
                        }
                        System.out.println();
                        System.out.println(Arrays.toString(newIndices2));
                        for (int j = 0; j < ints2.length; j++) {
                            System.out.print(newIndices2[ints2[j]] + ", ");
                        }
                        System.out.println();*/

                        HashSet<Integer> rightCuts = new HashSet<>();
                        for (int j = 0; j < ints2.length; j++) {
                            rightCuts.add(newIndices2[ints2[j]]);
                        }
                        branchCutSets.add(rightCuts);

                        newLowestBranchNodes.add(node.rightChild);

                        break;
                    }

                    branchPointer++;

                    if (consistent) {
                        //A single child was added to the branch
                        branchPointers.set(node.branchId, branchPointer);
                        if (node.leftChild != null) {
                            node = node.leftChild;
                        } else if (node.rightChild != null) {
                            node = node.rightChild;
                        }
                    }
                }
            }
            lowestBranchNodes = newLowestBranchNodes;
        }
        tree.branchCosts = branchCosts;
        return tree;
    }

    //Creates new data set and cuts of only the points included in mask.
    public static Tuple<double[][], BitSet[]> removeRedundantPoints(final double[][] data, final BitSet[] initialCuts, final BitSet mask) {
        BitSet[] newCuts = new BitSet[initialCuts.length];
        for (int i = 0; i < initialCuts.length; i++) {
            newCuts[i] = new BitSet(mask.count());
            int idx = 0;
            for (int j = 0; j < data.length; j++) {
                if (!mask.get(j)) continue;
                newCuts[i].setValue(idx, initialCuts[i].get(j));
                idx++;
            }
        }

        double[][] newData = new double[mask.count()][data[0].length];
        int idx = 0;
        for (int i = 0; i < data.length; i++) {
            if (!mask.get(i)) continue;
            for (int j = 0; j < data[i].length; j++) {
                newData[idx][j] = data[i][j];
            }
            idx++;
        }

        return new Tuple<>(newData, newCuts);
    }

    //Removes redundant cuts that agree on factor% of their elements.
    public static Tuple<BitSet[], double[]> removeRedundantCuts(BitSet[] initialCuts, double[] costs, double factor) {
        boolean[] toBeRemoved = new boolean[initialCuts.length]; //true indicates that the corresponding cut should be removed.
        for (int i = 0; i < initialCuts.length; i++) {
            for (int j = 0; j < initialCuts.length; j++) {
                if (i != j && !toBeRemoved[i] && !toBeRemoved[j] && (BitSet.XNor(initialCuts[i], initialCuts[j]) > initialCuts[i].size()*factor || BitSet.XOR(initialCuts[i], initialCuts[j]) > initialCuts[i].size()*factor)) {
                    //Remove cut with the largest cost.
                    int largest = costs[i] > costs[j] ? i : j;
                    toBeRemoved[largest] = true;
                }
            }
        }
        int count = 0;
        for (boolean b : toBeRemoved) {
            if (!b) {
                count++;
            }
        }
        double[] newCosts = new double[count];
        BitSet[] newInitialCuts = new BitSet[count];
        int index = 0;
        for (int i = 0; i < initialCuts.length; i++) {
            if (!toBeRemoved[i]) {
                newCosts[index] = costs[i];
                newInitialCuts[index] = initialCuts[i];
                index++;
            }
        }
        return new Tuple<>(newInitialCuts, newCosts);
    }

    //Runs the quicksort algorithm on the costs. Ensures that indices follows the same ordering as costs.
    private void quicksort(double[] costs, int[] indices, int l, int h) {
        if (l >= h || l < 0) {
            return;
        }
        int p = partition(costs, indices, l, h);
        quicksort(costs, indices, l, p-1);
        quicksort(costs, indices, p+1, h);
    }

    //The partition part of the quicksort algorithm.
    private int partition(double[] costs, int[] indices, int l, int h) {
        double pivot = costs[h];
        int i = l-1;
        for (int j = l; j < h; j++) {
            if (costs[j] <= pivot) {
                i = i + 1;
                double temp = costs[i];
                costs[i] = costs[j];
                costs[j] = temp;
                int temp2 = indices[i];
                indices[i] = indices[j];
                indices[j] = temp2;
            }
        }
        i = i + 1;
        double temp = costs[i];
        costs[i] = costs[h];
        costs[h] = temp;
        int temp2 = indices[i];
        indices[i] = indices[h];
        indices[h] = temp2;
        return i;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

}

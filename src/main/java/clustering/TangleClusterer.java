package clustering;

import clustering.TangleSearchTree.Node;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import monitor.Monitor;
import util.BitSet;
import util.Tuple;
import datasets.ScRNAseqDataset;

import java.util.ArrayList;
import java.util.Arrays;
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
        TangleSearchTree tree = generateTangleSearchTree(initialCuts, costs, a, psi);
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
        double[][] costsOrdered = new double[n][n];

        int[][] indices = new int[n][n];
        for (int i = 0; i < n; i++) indices[0][i] = i;
        System.arraycopy(costs, 0, costsOrdered[0], 0, n);
        quicksort(costsOrdered[0], indices[0], 0, n - 1);

        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);

        int branchIdx = 0;
        for (int i = 0; i < n; i++) {
            if (psi > 0 && costsOrdered[branchIdx][i] > psi) {
                break;
            }

            boolean consistent = false;
            List<Node> lowestDepthNodesCopy = new ArrayList<>(tree.lowestDepthNodes);
            for (Node node : lowestDepthNodesCopy) {
                consistent = tree.addOrientation(node, indices[node.branchIdx][i], true) || consistent;
                consistent = tree.addOrientation(node, indices[node.branchIdx][i], false) || consistent;

                if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                    branchIdx++;
                    node.leftChild.branchIdx = branchIdx;
                    // Reorder costs based on the subset of points on the left side of the cut
                    Tuple<double[][], BitSet[]> leftRedundancyRemoved = removeRedundantPoints(data, initialCuts, node.leftChild.intersection);
                    ScRNAseqDataset newDataset = new ScRNAseqDataset(leftRedundancyRemoved.x);

                    double[] newCosts = newDataset.getCutCosts(costFunctionName);
                    costsOrdered[branchIdx] = new double[n];
                    for (int j = 0; j < newCosts.length; j++) costsOrdered[branchIdx][j] = newCosts[j];
                    for (int j = newCosts.length; j < costsOrdered[branchIdx].length; j++) costsOrdered[branchIdx][j] = Integer.MAX_VALUE;
                    int[][] leftIndices = new int[n][n];
                    for (int j = 0; j < n; i++) leftIndices[0][j] = i;
                    System.arraycopy(costs, 0, costsOrdered[branchIdx], 0, n);
                    quicksort(costsOrdered[branchIdx], leftIndices[0], 0, n - 1);

                    branchIdx++;
                    node.rightChild.branchIdx = branchIdx;
                    // Reorder costs based on the subset of points on the right side of the cut
                    Tuple<double[][], BitSet[]> rightRedundancyRemoved = removeRedundantPoints(data, initialCuts, node.rightChild.intersection);

                }
            }

            if (earlyStop && !consistent) { //Stop if no nodes were added to the tree.
                break;
            }
        }

        return tree;
    }

    //Creates new data set and cuts of only the points included in mask.
    private Tuple<double[][], BitSet[]> removeRedundantPoints(double[][] data, BitSet[] initialCuts, BitSet mask) {
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
            newData[idx] = new double[data[i].length];
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

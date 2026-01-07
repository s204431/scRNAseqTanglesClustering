package clustering;

import clustering.TangleSearchTree.Node;
import datasets.CostFunctions;
import util.*;
import datasets.ScRNAseqDataset;
import util.BitSet;

import java.util.*;

public class TangleClusterer {

    //NOTE: This file is from the bachelor project.

    //This class is used to generate a clustering with tangles.

    protected static boolean earlyStop = false;
    protected boolean removeRedundantCuts = true;
    protected boolean useAlternateConsistencyCheck = false;
    protected boolean useOscarWerner = false;
    protected boolean useSplitFirst = false;
    protected boolean removeRedundantCutsIteratively = false;
    protected boolean autoLimitSplitCosts = false;

    private TangleSearchTree tangleSearchTreeNotPruned;
    private TangleSearchTree tangleSearchTree;

    private CostFunctions costFunctions;

    private Monitor monitor;

    private List<Double> splitCosts; //The cost for each splitting cut in the tree.

    //Ensure that it can only be created within this package.
    public TangleClusterer(Monitor monitor) {
        this.monitor = monitor;
    }

    //Generates a soft- and hard clustering for the provided dataset with a specific value of a and psi, and a specific initial cut generator and cost function.
    public void generateClusters(ScRNAseqDataset dataset, Config config) {
        updateClusteringParameters(config);

        splitCosts = new ArrayList<>();
        costFunctions = new CostFunctions(monitor);
        dataset.setCostFunctions(costFunctions);
        dataset.setA(config.getA());

        BitSet[] initialCuts = dataset.getInitialCuts(
                config.getHighLevelCutGeneratorName(),
                config.getLowLevelCutGeneratorName(),
                config.getSplitSizeCutGeneration(),
                config.isUsePcaCutGeneration(),
                config.getPcaComponentsCutGeneration(),
                config.isUseTSNECutGeneration(),
                config.getTsneComponentsCutGeneration());

        double[] costs = dataset.getCutCosts(
                config.getHighLevelCostFunctionName(),
                config.getLowLevelCostFunctionName(),
                config.isUseCache(),
                config.getSplitSizeCostFunction(),
                config.isUsePcaCostFunction(),
                config.getPcaComponentsCostFunction(),
                config.isUseTSNECostFunction(),
                config.getTsneComponentsCostFunction());

        if (removeRedundantCuts) {
            Tuple<BitSet[], double[]> redundancyRemoved = removeRedundantCuts(initialCuts, costs, config.getRedundancyFactor()); //Set factor to 1 to turn it off.
            initialCuts = redundancyRemoved.x;
            costs = redundancyRemoved.y;
        }

        //Run the main clustering pipeline without precomputed inputs
        runClusteringPipeline(dataset, config, initialCuts, costs, null);
    }

    //This is used when tuning parameters in order to reuse initial cuts and costs
    public void generateClusters(ScRNAseqDataset dataset, Config config, BitSet[] initialCuts, double[] costs, CostFunctions costFunctions, double[][] reducedPoints) {
        updateClusteringParameters(config);

        splitCosts = new ArrayList<>();
        this.costFunctions = costFunctions;
        dataset.setCostFunctions(costFunctions);
        dataset.setA(config.getA());

        //Run main clustering pipeline with precomputed inputs
        runClusteringPipeline(dataset, config, initialCuts, costs, reducedPoints);
    }

    private void runClusteringPipeline(ScRNAseqDataset dataset,
                                       Config config,
                                       BitSet[] cuts,
                                       double[] costs,
                                       double[][] reducedPoints) {

        //Build tree
        if (useOscarWerner) {
            tangleSearchTree = useSplitFirst
                    ? splitFirst(cuts, costs, dataset.data, config)
                    : splitLast(cuts, costs, dataset.data, config);
        } else {
            tangleSearchTree = generateTangleSearchTree(cuts, costs, config.getA(), config.getPsi());
        }
        monitor.setUncondensedTree(tangleSearchTree.copy());
        tangleSearchTreeNotPruned = tangleSearchTree.copy();

        //Optional split pruning
        if (autoLimitSplitCosts) {
            boolean usePerformanceMetric = config.isTuneParameters() || (config.isUseSplitPruning() && config.getSplitPruneMethod().equals(GlobalConstants.SPLIT_PRUNE_PERFORMANCE_METRIC));
            tangleSearchTree.limitSplitCosts(splitCosts, reducedPoints, usePerformanceMetric, config.getPerformanceMetric());
            monitor.setSplitPrunedTree(tangleSearchTree.copy());
        } else {
            monitor.setSplitPrunedTree(null);
        }

        //Condense tangle search tree
        try {
            tangleSearchTree.condenseTree(autoLimitSplitCosts ? 0 : 1);
        } catch (NullPointerException e) {
            tangleSearchTree.generateDefaultClustering();
            return;
        }
        monitor.setCondensedTree(tangleSearchTree.copy());

        //Contract tangle search tree
        tangleSearchTree.contractTree();

        //Compute hard and soft clustering
        tangleSearchTree.calculateSoftClustering();
        tangleSearchTree.calculateHardClustering();
    }

    public void clusterWithNewPsi(double psi) {
        tangleSearchTree = tangleSearchTreeNotPruned.copy();
        tangleSearchTree.limitPsi(tangleSearchTree.root, psi);
        monitor.setUncondensedTree(tangleSearchTree.copy());
        //Condense tangle search tree
        try {
            tangleSearchTree.condenseTree(1);
        } catch (NullPointerException e) {
            tangleSearchTree.generateDefaultClustering();
            return;
        }
        monitor.setCondensedTree(tangleSearchTree.copy());

        //Contract tangle search tree
        tangleSearchTree.contractTree();

        //Compute hard and soft clustering
        tangleSearchTree.calculateSoftClustering();
        tangleSearchTree.calculateHardClustering();
    }

    //Extracts clustering parameters from config
    private void updateClusteringParameters(Config config) {
        earlyStop = config.isUseEarlyStop();
        useAlternateConsistencyCheck = config.isUseAlternateConsistencyCheck();
        useOscarWerner = config.isUseWernerModification();
        useSplitFirst = config.isUseSplitFirst();
        autoLimitSplitCosts = config.isUseSplitPruning();
        removeRedundantCuts = config.isRemoveRedundantCuts();
        removeRedundantCutsIteratively = config.isRemoveRedundantCutsIteratively();
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
                consistent = tree.addOrientation(node, indices[i], true, useAlternateConsistencyCheck) || consistent;
                consistent = tree.addOrientation(node, indices[i], false, useAlternateConsistencyCheck) || consistent;
                if (node.leftChild != null && node.rightChild != null) {
                    node.leftChild.cost = costsOrdered[i];
                    node.rightChild.cost = costsOrdered[i];
                    splitCosts.add(costs[indices[i]]);
                }
                else if (node.leftChild != null) {
                    node.leftChild.cost = costsOrdered[i];
                    if (autoLimitSplitCosts) {
                        int newA = (int) (node.intersection.count() * 0.667);
                        tree.a = newA;
                        node.leftChild = null;
                        consistent = tree.addOrientation(node, indices[i], true, useAlternateConsistencyCheck) || consistent;
                        tree.a = a;
                    }
                }
                else if (node.rightChild != null) {
                    node.rightChild.cost = costsOrdered[i];
                    if (autoLimitSplitCosts) {
                        int newA = (int) (node.intersection.count() * 0.667);
                        tree.a = newA;
                        node.rightChild = null;
                        consistent = tree.addOrientation(node, indices[i], false, useAlternateConsistencyCheck) || consistent;
                        tree.a = a;
                    }
                }
            }
            if (earlyStop && !consistent) { //Stop if no nodes were added to the tree.
                break;
            }
        }
        return tree;
    }

    private TangleSearchTree oscarWerner(BitSet[] initialCuts, double[] costs, double[][] data, Config config) {
        int n = costs.length;
        int a = config.getA();
        double psi = config.getPsi();
        String highLevelCostFunctionName = config.getHighLevelCostFunctionName();
        String lowLevelCostFunctionName = config.getLowLevelCostFunctionName();
        boolean useCache = config.isUseCache();

        //Costs for each branch ID (in order of initial cuts).
        List<double[]> branchCosts = new ArrayList<>();
        double[] initialCosts = new double[n];
        System.arraycopy(costs, 0, initialCosts, 0, n);
        branchCosts.add(initialCosts.clone());

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
        quicksort(initialCosts, indicesOrdered.getFirst(), 0, n - 1);

        int[] debugIndices = new int[n];
        for (int i = 0; i < n; i++) {
            debugIndices[initialIndices[i]] = i;
        }

        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);

        int branchId = 0;

        List<Node> lowestBranchNodes = new ArrayList<>();
        lowestBranchNodes.add(tree.root);
        while (!lowestBranchNodes.isEmpty()) {
            List<Node> newLowestBranchNodes = new ArrayList<>();

            for (Node node : lowestBranchNodes) {
                int[] branchIndicesOrdered = indicesOrdered.get(node.branchId);
                int branchPointer = branchPointers.get(node.branchId);
                HashSet<Integer> branchCuts = null;
                if (removeRedundantCutsIteratively) branchCuts = branchCutSets.get(node.branchId);

                for (int i = branchPointer; i < initialCuts.length; i++) {
                    int cutIndex = branchIndicesOrdered[i];
                    double cutCost = branchCosts.get(node.branchId)[cutIndex];

                    boolean consistent = false;

                    if (psi != 0 && cutCost > psi) {
                        break;
                    }

                    if (removeRedundantCutsIteratively && !branchCuts.contains(cutIndex)) {
                        //System.out.println("Node " + debugIndices[node.originalOrientation]  + (node.side ? "L" : "R") + " in branch: " + node.branchId + " has skipped cut: " + debugIndices[branchIndicesOrdered[i]]);
                        branchPointer++;
                        continue;
                    }

                    consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                    consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting

                        splitCosts.add(branchCosts.get(node.branchId)[cutIndex]);

                        for (int j = 0; j < 2; j++) {

                            Node childNode = j == 0 ? node.leftChild : node.rightChild;
                            childNode.cost = cutCost;

                            branchId++;
                            childNode.branchId = branchId;
                            branchPointers.add(i + 1);
                            newLowestBranchNodes.add(childNode);

                            //Remove points that are not included in the intersection for that branch
                            Tuple<double[][], BitSet[]> redundantPointsRemoved = removeRedundantPoints(data, initialCuts, childNode.intersection);
                            double[][] newData = redundantPointsRemoved.x;
                            BitSet[] newCuts = redundantPointsRemoved.y;
                            ScRNAseqDataset newDataset = new ScRNAseqDataset(newData, monitor);
                            newDataset.setCostFunctions(costFunctions);
                            costFunctions.setMask(childNode.intersection);
                            newDataset.setInitialCuts(newCuts);
                            double[] newCosts = newDataset.getCutCosts(
                                    highLevelCostFunctionName,
                                    lowLevelCostFunctionName,
                                    useCache,
                                    config.getSplitSizeCostFunction(),
                                    config.isUsePcaCostFunction(),
                                    config.getPcaComponentsCostFunction(),
                                    config.isUseTSNECostFunction(),
                                    config.getTsneComponentsCostFunction());
                            branchCosts.add(newCosts);

                            //Reorder cuts and costs based on the cost order for the parent branch
                            int[] newIndices = branchIndicesOrdered.clone();
                            double[] reorderedCosts = new double[newCosts.length];
                            for (int k = 0; k < reorderedCosts.length; k++) {
                                reorderedCosts[k] = newCosts[newIndices[k]];
                            }

                            // Order costs and indices
                            quicksort(reorderedCosts, newIndices, i + 1, reorderedCosts.length - 1);
                            indicesOrdered.add(newIndices);

                            int[] originalIndices = new int[newIndices.length];
                            if (removeRedundantCutsIteratively) {
                                for (int k = 0; k < newIndices.length; k++) {
                                    originalIndices[k] = k;
                                }

                                BitSet[] reorderedCuts = new BitSet[newCosts.length];
                                for (int k = 0; k < reorderedCosts.length; k++) {
                                    reorderedCuts[k] = newCuts[newIndices[k]];
                                }

                                Tuple<BitSet[], double[]> redundantCutsRemoved = removeRedundantCuts(reorderedCuts, Arrays.stream(originalIndices).mapToDouble(k -> (double) k).toArray(), config.getRedundancyFactor());
                                int[] ints = Arrays.stream(redundantCutsRemoved.y).mapToInt(k -> (int) Math.round(k)).toArray();

                                HashSet<Integer> cuts = new HashSet<>();
                                for (int k = 0; k < ints.length; k++) {
                                    cuts.add(newIndices[ints[k]]);
                                }
                                branchCutSets.add(cuts);
                            }
                        }

                        break;
                    }
                    else if (autoLimitSplitCosts && node.leftChild != null) {
                        int newA = (int) (node.intersection.count()*0.667);
                        tree.a = newA;
                        node.leftChild = null;
                        consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                        tree.a = a;
                    }
                    else if (autoLimitSplitCosts && node.rightChild != null) {
                        int newA = (int) (node.intersection.count()*0.667);
                        tree.a = newA;
                        node.rightChild = null;
                        consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                        tree.a = a;
                    }

                    branchPointer++;

                    if (consistent) {
                        //A single child was added to the branch
                        branchPointers.set(node.branchId, branchPointer);
                        if (node.leftChild != null) {
                            node.leftChild.cost = cutCost;
                            node = node.leftChild;
                        } else if (node.rightChild != null) {
                            node.rightChild.cost = cutCost;
                            node = node.rightChild;
                        }
                    } else if (earlyStop) break;    // Early stop if cut was not consistent
                }
            }
            lowestBranchNodes = newLowestBranchNodes;
        }
        tree.branchCosts = branchCosts;
        monitor.setBranchCosts(branchCosts);
        return tree;
    }

    private TangleSearchTree splitFirst(BitSet[] initialCuts, double[] costs, double[][] data, Config config) {
        int n = costs.length;
        int a = config.getA();
        double psi = config.getPsi();
        String highLevelCostFunctionName = config.getHighLevelCostFunctionName();
        String lowLevelCostFunctionName = config.getLowLevelCostFunctionName();
        boolean useCache = config.isUseCache();

        //Hashset containing every cut that was added to the tree
        List<HashSet<Integer>> branchUsedCuts = new ArrayList<>();
        HashSet<Integer> initialUsedCuts = new HashSet<>();
        branchUsedCuts.add(initialUsedCuts);

        //Costs for each branch ID (in order of initial cuts).
        List<double[]> branchCosts = new ArrayList<>();
        double[] initialCosts = new double[n];
        System.arraycopy(costs, 0, initialCosts, 0, n);
        branchCosts.add(initialCosts.clone());

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

        quicksort(initialCosts, indicesOrdered.getFirst(), 0, n - 1);

        int[] debugIndices = new int[n];
        for (int i = 0; i < n; i++) {
            debugIndices[initialIndices[i]] = i;
        }

        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);

        int branchId = 0;

        List<Node> lowestBranchNodes = new ArrayList<>();
        lowestBranchNodes.add(tree.root);
        while (!lowestBranchNodes.isEmpty()) {
            List<Node> newLowestBranchNodes = new ArrayList<>();

            for (Node node : lowestBranchNodes) {
                boolean usePsi = psi > 0;

                HashSet<Integer> usedCuts = branchUsedCuts.get(node.branchId);
                HashSet<Integer> branchCuts = null;
                if (removeRedundantCutsIteratively) branchCuts = branchCutSets.get(node.branchId);

                double[] localCosts = branchCosts.get(node.branchId);
                int[] branchIndicesOrdered = indicesOrdered.get(node.branchId);
                int branchPointer = branchPointers.get(node.branchId);

                boolean hasParent = node.parent != null;
                double[] parentCosts = hasParent ? branchCosts.get(node.parent.branchId) : null;
                int votePointer = 0;

                // ========== SPLIT CUTS ==========
                HashSet<Integer> leftUsedCuts = new HashSet<>(usedCuts);
                HashSet<Integer> rightUsedCuts = new HashSet<>(usedCuts);
                boolean splitCutAdded = false;
                while (branchPointer < localCosts.length) {
                    int cutIndex = branchIndicesOrdered[branchPointer];

                    if (usePsi && localCosts[cutIndex] >= psi) {
                        break;
                    }

                    if (usedCuts.contains(cutIndex)) {
                        branchPointer++;
                        continue;
                    }

                    if (removeRedundantCutsIteratively && !branchCuts.contains(cutIndex)) {
                        //System.out.println("Skipped cut: " + cutIndex);
                        branchPointer++;
                        continue;
                    }

                    // Try to add split cut
                    boolean consistent = false;
                    consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                    consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting

                        splitCutAdded = true;
                        splitCosts.add(branchCosts.get(node.branchId)[cutIndex]);
                        leftUsedCuts.add(cutIndex);
                        rightUsedCuts.add(cutIndex);

                        for (int j = 0; j < 2; j++) {

                            boolean left = j == 0;
                            Node childNode = left ? node.leftChild : node.rightChild;
                            childNode.cost = localCosts[cutIndex];

                            branchId++;
                            childNode.branchId = branchId;
                            branchPointers.add(branchPointer + 1);
                            branchUsedCuts.add(left ? leftUsedCuts : rightUsedCuts);
                            newLowestBranchNodes.add(childNode);

                            //Remove points that are not included in the intersection for that branch
                            Tuple<double[][], BitSet[]> redundantPointsRemoved = removeRedundantPoints(data, initialCuts, childNode.intersection);
                            double[][] newData = redundantPointsRemoved.x;
                            BitSet[] newCuts = redundantPointsRemoved.y;
                            ScRNAseqDataset newDataset = new ScRNAseqDataset(newData, monitor);
                            newDataset.setCostFunctions(costFunctions);
                            costFunctions.setMask(childNode.intersection);
                            newDataset.setInitialCuts(newCuts);
                            double[] newCosts = newDataset.getCutCosts(
                                    highLevelCostFunctionName,
                                    lowLevelCostFunctionName,
                                    useCache,
                                    config.getSplitSizeCostFunction(),
                                    config.isUsePcaCostFunction(),
                                    config.getPcaComponentsCostFunction(),
                                    config.isUseTSNECostFunction(),
                                    config.getTsneComponentsCostFunction());
                            branchCosts.add(newCosts);

                            //Reorder cuts and costs based on the cost order for the parent branch
                            int[] newIndices = branchIndicesOrdered.clone();
                            double[] reorderedCosts = new double[newCosts.length];
                            for (int k = 0; k < reorderedCosts.length; k++) {
                                reorderedCosts[k] = newCosts[newIndices[k]];
                            }

                            // Order costs and indices
                            quicksort(reorderedCosts, newIndices, branchPointer + 1, reorderedCosts.length - 1);
                            indicesOrdered.add(newIndices);

                            int[] originalIndices = new int[newIndices.length];
                            if (removeRedundantCutsIteratively) {
                                for (int k = 0; k < newIndices.length; k++) {
                                    originalIndices[k] = k;
                                }

                                BitSet[] reorderedCuts = new BitSet[newCosts.length];
                                for (int k = 0; k < reorderedCosts.length; k++) {
                                    reorderedCuts[k] = newCuts[newIndices[k]];
                                }

                                Tuple<BitSet[], double[]> redundantCutsRemoved = removeRedundantCuts(reorderedCuts, Arrays.stream(originalIndices).mapToDouble(k -> (double) k).toArray(), config.getRedundancyFactor());
                                int[] ints = Arrays.stream(redundantCutsRemoved.y).mapToInt(k -> (int) Math.round(k)).toArray();

                                HashSet<Integer> cuts = new HashSet<>();
                                for (int k = 0; k < ints.length; k++) {
                                    cuts.add(newIndices[ints[k]]);
                                }
                                branchCutSets.add(cuts);
                            }
                        }
                        break;
                    }

                    // Split node was not added
                    branchPointer++;
                    node.leftChild = null;
                    node.rightChild = null;
                }

                if (!hasParent) {
                    break;
                }

                // ========== VOTE CUTS ==========
                double splitCost = splitCutAdded ? localCosts[branchIndicesOrdered[branchPointer]] : Double.MAX_VALUE;
                double[] parentCostsOrdered = parentCosts.clone();
                int[] parentIndices = new int[parentCostsOrdered.length];
                for (int i = 0; i < parentIndices.length; i++) parentIndices[i] = i;
                quicksort(parentCostsOrdered, parentIndices, 0, parentCostsOrdered.length - 1);

                while (votePointer < parentCosts.length) {
                    int cutIndex = parentIndices[votePointer];
                    double cutCost = parentCostsOrdered[votePointer];

                    if (cutCost >= splitCost || (usePsi && cutCost >= psi)) {
                        break;
                    }

                    votePointer++;

                    if (leftUsedCuts.contains(cutIndex)) {
                        continue;
                    }

                    if (removeRedundantCutsIteratively && !branchCuts.contains(cutIndex)) {
                        //System.out.println("Skipped vote cut: " + cutIndex);
                        continue;
                    }

                    Node leftChild = node.leftChild;
                    Node rightChild = node.rightChild;
                    node.leftChild = null;
                    node.rightChild = null;

                    // Try to add vote cut
                    if (autoLimitSplitCosts) tree.a = (int) (node.intersection.count()*0.667);
                    boolean consistent = false;
                    consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                    consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                    tree.a = a;

                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                        node.leftChild = leftChild;
                        node.rightChild = rightChild;
                        continue;
                    } else if (node.leftChild == null && node.rightChild == null || !consistent) {     // No child was added
                        node.leftChild = leftChild;
                        node.rightChild = rightChild;
                        continue;
                    }

                    // Single vote cut was added
                    if (node.leftChild != null) {
                        node.leftChild.cost = cutCost;
                        if (splitCutAdded) {
                            node.leftChild.leftChild = leftChild;
                            node.leftChild.rightChild = rightChild;
                            leftChild.parent = node.leftChild;
                            rightChild.parent = node.leftChild;

                            consistent = useAlternateConsistencyCheck ? tree.isConsistentOscarWerner(leftChild) : tree.isConsistent(leftChild);
                            consistent = (useAlternateConsistencyCheck ? tree.isConsistentOscarWerner(rightChild) : tree.isConsistent(rightChild)) && consistent;
                            if (!consistent) {
                                node.leftChild = leftChild;
                                node.rightChild = rightChild;
                                leftChild.parent = node;
                                rightChild.parent = node;
                            }
                        }
                        if (consistent) {
                            leftUsedCuts.add(cutIndex);
                            rightUsedCuts.add(cutIndex);
                            node = node.leftChild;
                        }

                    } else if (node.rightChild != null) {
                        node.rightChild.cost = cutCost;
                        if (splitCutAdded) {
                            node.rightChild.leftChild = leftChild;
                            node.rightChild.rightChild = rightChild;
                            leftChild.parent = node.rightChild;
                            rightChild.parent = node.rightChild;

                            consistent = useAlternateConsistencyCheck ? tree.isConsistentOscarWerner(leftChild) : tree.isConsistent(leftChild);
                            consistent = (useAlternateConsistencyCheck ? tree.isConsistentOscarWerner(rightChild) : tree.isConsistent(rightChild)) && consistent;
                            if (!consistent) {
                                node.leftChild = leftChild;
                                node.rightChild = rightChild;
                                leftChild.parent = node;
                                rightChild.parent = node;
                            }
                        }
                        if (consistent) {
                            leftUsedCuts.add(cutIndex);
                            rightUsedCuts.add(cutIndex);
                            node = node.rightChild;
                        }
                    }
                }
            }
            lowestBranchNodes = newLowestBranchNodes;
        }
        tree.branchCosts = branchCosts;
        monitor.setBranchCosts(branchCosts);
        return tree;
    }

    private TangleSearchTree splitLast(BitSet[] initialCuts, double[] costs, double[][] data, Config config) {
        int n = costs.length;
        int a = config.getA();
        double psi = config.getPsi();
        String highLevelCostFunctionName = config.getHighLevelCostFunctionName();
        String lowLevelCostFunctionName = config.getLowLevelCostFunctionName();
        boolean useCache = config.isUseCache();

        //Hashset containing every cut that was added to the tree
        List<HashSet<Integer>> branchUsedCuts = new ArrayList<>();
        HashSet<Integer> initialUsedCuts = new HashSet<>();
        branchUsedCuts.add(initialUsedCuts);

        //Costs for each branch ID (in order of initial cuts).
        List<double[]> branchCosts = new ArrayList<>();
        double[] initialCosts = new double[n];
        System.arraycopy(costs, 0, initialCosts, 0, n);
        branchCosts.add(initialCosts.clone());

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

        quicksort(initialCosts, indicesOrdered.getFirst(), 0, n - 1);

        int[] debugIndices = new int[n];
        for (int i = 0; i < n; i++) {
            debugIndices[initialIndices[i]] = i;
        }

        TangleSearchTree tree = new TangleSearchTree(a, initialCuts, costs);

        int branchId = 0;

        List<Node> lowestBranchNodes = new ArrayList<>();
        lowestBranchNodes.add(tree.root);
        while (!lowestBranchNodes.isEmpty()) {
            List<Node> newLowestBranchNodes = new ArrayList<>();

            for (Node node : lowestBranchNodes) {
                boolean usePsi = psi > 0;

                HashSet<Integer> usedCuts = branchUsedCuts.get(node.branchId);
                HashSet<Integer> branchCuts = null;
                if (removeRedundantCutsIteratively) branchCuts = branchCutSets.get(node.branchId);

                double[] localCosts = branchCosts.get(node.branchId);
                int[] branchIndicesOrdered = indicesOrdered.get(node.branchId);
                int branchPointer = branchPointers.get(node.branchId);

                boolean hasParent = node.parent != null;
                double[] parentCosts = hasParent ? branchCosts.get(node.parent.branchId) : null;
                int votePointer = 0;

                // ========== SPLIT CUTS ==========
                HashSet<Integer> leftUsedCuts = new HashSet<>(usedCuts);
                HashSet<Integer> rightUsedCuts = new HashSet<>(usedCuts);
                boolean splitCutFound = false;
                int splitCut = -1;
                while (branchPointer < localCosts.length) {
                    int cutIndex = branchIndicesOrdered[branchPointer];

                    if (usePsi && localCosts[cutIndex] >= psi) {
                        break;
                    }

                    if (usedCuts.contains(cutIndex)) {
                        branchPointer++;
                        continue;
                    }

                    if (removeRedundantCutsIteratively && !branchCuts.contains(cutIndex)) {
                        //System.out.println("Skipped split cut: " + cutIndex);
                        branchPointer++;
                        continue;
                    }

                    // Try to find next split cut
                    boolean consistent = false;
                    consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                    consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                        splitCutFound = true;
                        splitCut = cutIndex;
                        node.leftChild = null;
                        node.rightChild = null;
                        break;
                    }

                    // Split node was not added
                    branchPointer++;
                    node.leftChild = null;
                    node.rightChild = null;
                }

                // ========== VOTE CUTS ==========
                boolean addSplitCut = splitCutFound;
                if (parentCosts != null) {
                    double splitCost = splitCutFound ? localCosts[branchIndicesOrdered[branchPointer]] : Double.MAX_VALUE;
                    double[] parentCostsOrdered = parentCosts.clone();
                    int[] parentIndices = new int[parentCostsOrdered.length];
                    for (int i = 0; i < parentIndices.length; i++) parentIndices[i] = i;
                    quicksort(parentCostsOrdered, parentIndices, 0, parentCostsOrdered.length - 1);

                    while (hasParent && votePointer < parentCosts.length) {
                        int cutIndex = parentIndices[votePointer];
                        double cutCost = parentCostsOrdered[votePointer];

                        if (cutCost >= splitCost || (usePsi && cutCost >= psi)) {
                            break;
                        }

                        votePointer++;

                        if (leftUsedCuts.contains(cutIndex)) {
                            continue;
                        }

                        if (removeRedundantCutsIteratively && !branchCuts.contains(cutIndex)) {
                            //System.out.println("Skipped vote cut: " + cutIndex);
                            continue;
                        }

                        // Try to add vote cut
                        if (autoLimitSplitCosts) tree.a = (int) (node.intersection.count()*0.667);
                        boolean consistent = false;
                        consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                        consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                        tree.a = a;

                        if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                            continue;
                        } else if (node.leftChild == null && node.rightChild == null || !consistent) {     // No child was added
                            if (earlyStop) {
                                //System.out.println("Stopping on branch: " + node.branchId);
                                addSplitCut = false;
                                break;
                            }
                            continue;
                        }

                        // Single vote cut was added
                        if (node.leftChild != null) {
                            node.leftChild.cost = cutCost;
                            if (consistent) {
                                leftUsedCuts.add(cutIndex);
                                rightUsedCuts.add(cutIndex);
                                node = node.leftChild;
                            }
                        } else if (node.rightChild != null) {
                            node.rightChild.cost = cutCost;
                            if (consistent) {
                                leftUsedCuts.add(cutIndex);
                                rightUsedCuts.add(cutIndex);
                                node = node.rightChild;
                            }
                        }
                    }
                }

                // Try to add split cut
                if (addSplitCut) {
                    int cutIndex = splitCut;
                    boolean consistent = false;
                    consistent = tree.addOrientation(node, cutIndex, true, useAlternateConsistencyCheck) || consistent;
                    consistent = tree.addOrientation(node, cutIndex, false, useAlternateConsistencyCheck) || consistent;
                    if (node.leftChild != null && node.leftChild.intersection.count() == 0) {
                        node.leftChild = null;
                    }
                    if (node.rightChild != null && node.rightChild.intersection.count() == 0) {
                        node.rightChild = null;
                    }

                    if (node.leftChild != null && node.rightChild != null) {    // Node is splitting
                        splitCosts.add(branchCosts.get(node.branchId)[cutIndex]);
                        leftUsedCuts.add(cutIndex);
                        rightUsedCuts.add(cutIndex);

                        for (int j = 0; j < 2; j++) {

                            boolean left = j == 0;
                            Node childNode = left ? node.leftChild : node.rightChild;
                            childNode.cost = localCosts[cutIndex];

                            branchId++;
                            childNode.branchId = branchId;
                            branchPointers.add(branchPointer + 1);
                            branchUsedCuts.add(left ? leftUsedCuts : rightUsedCuts);
                            newLowestBranchNodes.add(childNode);

                            //Remove points that are not included in the intersection for that branch
                            Tuple<double[][], BitSet[]> redundantPointsRemoved = removeRedundantPoints(data, initialCuts, childNode.intersection);
                            double[][] newData = redundantPointsRemoved.x;
                            BitSet[] newCuts = redundantPointsRemoved.y;
                            ScRNAseqDataset newDataset = new ScRNAseqDataset(newData, monitor);
                            newDataset.setCostFunctions(costFunctions);
                            costFunctions.setMask(childNode.intersection);
                            newDataset.setInitialCuts(newCuts);
                            double[] newCosts = newDataset.getCutCosts(
                                    highLevelCostFunctionName,
                                    lowLevelCostFunctionName,
                                    useCache,
                                    config.getSplitSizeCostFunction(),
                                    config.isUsePcaCostFunction(),
                                    config.getPcaComponentsCostFunction(),
                                    config.isUseTSNECostFunction(),
                                    config.getTsneComponentsCostFunction());
                            branchCosts.add(newCosts);

                            //Reorder cuts and costs based on the cost order for the parent branch
                            int[] newIndices = branchIndicesOrdered.clone();
                            double[] reorderedCosts = new double[newCosts.length];
                            for (int k = 0; k < reorderedCosts.length; k++) {
                                reorderedCosts[k] = newCosts[newIndices[k]];
                            }

                            // Order costs and indices
                            quicksort(reorderedCosts, newIndices, branchPointer + 1, reorderedCosts.length - 1);
                            indicesOrdered.add(newIndices);

                            int[] originalIndices = new int[newIndices.length];
                            if (removeRedundantCutsIteratively) {
                                for (int k = 0; k < newIndices.length; k++) {
                                    originalIndices[k] = k;
                                }

                                BitSet[] reorderedCuts = new BitSet[newCosts.length];
                                for (int k = 0; k < reorderedCosts.length; k++) {
                                    reorderedCuts[k] = newCuts[newIndices[k]];
                                }

                                Tuple<BitSet[], double[]> redundantCutsRemoved = removeRedundantCuts(reorderedCuts, Arrays.stream(originalIndices).mapToDouble(k -> (double) k).toArray(), config.getRedundancyFactor());
                                int[] ints = Arrays.stream(redundantCutsRemoved.y).mapToInt(k -> (int) Math.round(k)).toArray();

                                HashSet<Integer> cuts = new HashSet<>();
                                for (int k = 0; k < ints.length; k++) {
                                    cuts.add(newIndices[ints[k]]);
                                }
                                branchCutSets.add(cuts);
                            }
                        }
                    }
                }
            }
            lowestBranchNodes = newLowestBranchNodes;
        }
        tree.branchCosts = branchCosts;
        monitor.setBranchCosts(branchCosts);
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
    public static Tuple<BitSet[], double[]> removeRedundantCuts2(BitSet[] initialCuts, double[] costs, double factor) {
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

    public static Tuple<BitSet[], double[]> removeRedundantCuts(BitSet[] initialCuts, double[] costs, double factor) {
        int maxCutsToKeep = -1;
        Integer[] sortedIndices = new Integer[costs.length]; //Sorted by costs
        for (int i = 0; i < costs.length; i++) {
            sortedIndices[i] = i;
        }

        Arrays.sort(sortedIndices, Comparator.comparingDouble(i -> costs[i]));

        boolean[] toBeRemoved = new boolean[initialCuts.length]; //true indicates that the corresponding cut should be removed.
        List<Integer> keptCuts = new ArrayList<>(); //Indices of all cuts that we will keep
        for (int i = 0; i < sortedIndices.length; i++) {
            if (maxCutsToKeep > 0 && keptCuts.size() >= maxCutsToKeep) {
                toBeRemoved[sortedIndices[i]] = true;
                continue;
            }
            BitSet cut = initialCuts[sortedIndices[i]];
            for (int j = keptCuts.size()-1; j >= 0; j--) {
                int otherIndex = keptCuts.get(j);
                if (BitSet.XNor(cut, initialCuts[otherIndex]) > cut.size()*factor || BitSet.XOR(cut, initialCuts[otherIndex]) > cut.size()*factor) {
                    toBeRemoved[sortedIndices[i]] = true;
                    break;
                }
            }
            if (!toBeRemoved[sortedIndices[i]]) {
                keptCuts.add(sortedIndices[i]);
            }
        }

        int count = keptCuts.size();
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
}

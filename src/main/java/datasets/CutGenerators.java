package datasets;

import clustering.Model;
import main.Main;
import org.bytedeco.javacv.JavaCvErrorCallback;
import util.BitSet;
import util.Distance;
import util.GlobalConstants;

import java.util.*;

public class CutGenerators {

    //NOTE: The content in this file is from the bachelor project.

    private static final double precision = 1.0; //Determines the number of cuts generated.

    public double[] cutCosts; //For local means only


    public BitSet[] splitCutGenerator(double[][] dataPoints, String lowLevelCutGenerator, int a, boolean useFastVersion) {
        int splitSize = 1000;
        int nSplits = (int)Math.ceil(dataPoints[0].length/(double)splitSize);

        List<double[][]> splits = new ArrayList<>();
        List<List<Double>[]> splitsList = new ArrayList<>();


        splits.add(new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length)]);

        int index = 0;
        for (int i = 0; i < dataPoints[0].length; i++) {
            for (int j = 0; j < dataPoints.length; j++) {
                splits.getLast()[j][index] = dataPoints[j][i];
            }
            index++;
            if (index == splitSize && i < dataPoints[0].length-1) {
                index = 0;
                splits.add(new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length - i - 1)]);
            }
        }

        /*for (int i = 0; i < nSplits; i++) {
            splitsList.add(new List[dataPoints.length]);
            for (int j = 0; j < dataPoints.length; j++) {
                splitsList.get(i)[j] = new ArrayList<>();
            }
        }

        for (int i = 0; i < dataPoints.length; i++) {
            for (int j = 0; j < dataPoints[i].length; j++) {
                splitsList.get(j%nSplits)[i].add(dataPoints[i][j]);
            }
        }

        for (int i = 0; i < splitsList.size(); i++) {
            splits.add(new double[dataPoints.length][splitsList.get(i)[0].size()]);
            for (int j = 0; j < splitsList.get(i).length; j++) {
                for (int k = 0; k < splitsList.get(i)[j].size(); k++) {
                    splits.get(i)[j][k] = splitsList.get(i)[j].get(k);
                }
            }
        }*/

        List<BitSet[]> bitSets = new ArrayList<>();

        SplitParallelRunner[] runnables = new SplitParallelRunner[splits.size()];
        Thread[] threads = new Thread[splits.size()];
        for (int i = 0; i < splits.size(); i++) {
            runnables[i] = new SplitParallelRunner();
            runnables[i].data = splits.get(i);
            runnables[i].a = a;
            runnables[i].useFastVersion = useFastVersion;
            runnables[i].lowLevelCutGenerator = lowLevelCutGenerator;
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        /*for (int i = 0; i < splits.size(); i++) {
            System.out.println(splits.get(i)[0].length);
            bitSets.add(combinedCutGenerator(splits.get(i), a));
        }*/

        for (int i = 0; i < splits.size(); i++) {
            try {
                threads[i].join();
                bitSets.add(runnables[i].result);
            }
            catch (Exception e) {

            }
        }

        return mergeCuts(bitSets);
    }

    public class SplitParallelRunner implements Runnable {

        public BitSet[] result;
        public double[][] data;
        public int a;
        public boolean useFastVersion;
        public String lowLevelCutGenerator;

        @Override
        public void run() {
            result = combinedCutGenerator(data, lowLevelCutGenerator, a, useFastVersion);
        }
    }

    public BitSet[] combinedCutGenerator(double[][] dataPoints, String lowLevelCutGenerator, int a, boolean useFastVersion) {

        //dataPoints = Model.pca(dataPoints, 100);

        int nComponents = 3;

        List<BitSet[]> bitSets = new ArrayList<>();

        /*try {
            double[][] reducedPoints = Model.pca(dataPoints, nComponents);
            for (int i = a; i < dataPoints.length; i *= 2) {
                bitSets.add(getInitialCutsKNN(reducedPoints, i));
            }
        }
        catch (Exception e) {

        }*/
        if (!useFastVersion) {
            double[][] reducedPoints = Model.tsne(dataPoints, nComponents);
            //for (int i = a; i < dataPoints.length; i *= 2) {
                bitSets.add(runLowLevelCutGenerator(reducedPoints, lowLevelCutGenerator, a));
            //}
        }
        /*try {
            bitSets.add(getInitialCutsLocalMeans(Model.umap(dataPoints, nComponents), a));
        }
        catch (Exception e) {

        }*/
        double[][] reducedPoints = Model.svd(dataPoints, nComponents);
        //for (int i = a; i < dataPoints.length; i *= 2) {
            bitSets.add(runLowLevelCutGenerator(reducedPoints, lowLevelCutGenerator, a));
        //}

        return mergeCuts(bitSets);
    }

    public BitSet[] singleCutGenerator(double[][] dataPoints, String cutGeneratorName, int a, boolean useFastVersion) {
        int nComponents = 3;
        List<BitSet[]> bitsets = new ArrayList<>();

        // T-sne
        if (!useFastVersion) {
            double[][] reducedPoints = Model.tsne(dataPoints, nComponents);
            bitsets.add(runLowLevelCutGenerator(reducedPoints, cutGeneratorName, a));
        }

        // PCA
        double[][] reducedPoints = Model.svd(dataPoints, nComponents);
        bitsets.add(runLowLevelCutGenerator(reducedPoints, cutGeneratorName, a));

        return mergeCuts(bitsets);
    }

    public BitSet[] runLowLevelCutGenerator(double[][] reducedData, String cutGeneratorName, int a) {
        int timesMoreCuts = 18; //Generate this many times more cuts using shifting
        return switch (cutGeneratorName) {
            case GlobalConstants.LOW_LEVEL_CUT_GENERATOR_KNN -> getInitialCutsKNN(reducedData, a);
            case GlobalConstants.LOW_LEVEL_CUT_GENERATOR_SIMPLE -> getInitialCutsSimple(reducedData, a, timesMoreCuts);
            case GlobalConstants.LOW_LEVEL_CUT_GENERATOR_RANGE -> getInitialCutsRange(reducedData, a, timesMoreCuts);
            case GlobalConstants.LOW_LEVEL_CUT_GENERATOR_LOCAL_MEANS -> getInitialCutsLocalMeans(reducedData, a, timesMoreCuts);
            case GlobalConstants.LOW_LEVEL_CUT_GENERATOR_DISTANCE_BETWEEN_MEANS -> getInitialCutsDistanceBetweenMeans(reducedData, a, timesMoreCuts);
            default -> getInitialCutsKNN(reducedData, a);
        };
    }

    public BitSet[] mergeCuts(List<BitSet[]> bitSets) {
        int length = 0;
        for (int i = 0; i < bitSets.size(); i++) {
            length += bitSets.get(i).length;
        }
        BitSet[] merged = new BitSet[length];
        int index = 0;
        for (int i = 0; i < bitSets.size(); i++) {
            for (int j = 0; j < bitSets.get(i).length; j++) {
                merged[index] = bitSets.get(i)[j];
                index++;
            }
        }
        return merged;
    }

    public BitSet[] getInitialCutsKNN(double[][] dataPoints, int a) {
        dataPoints = Main.zScoreNorm(dataPoints);
        boolean directed = true;
        int nIterations = 5;

        List<BitSet[]> bitSets = new ArrayList<>();
        KNNGraph knnGraph = new KNNGraph(dataPoints, 25);
        for (int k = 15; k <= 25; k++) {
            for (int iteration = 0; iteration < nIterations; iteration++) {
                double[] heuristicRepresentation = new double[dataPoints.length];
                int[] addedOrder = new int[dataPoints.length];
                int addedOrderIndex = 0;
                //List<List<Integer>> connectedComponents = knnGraph.getConnectedComponents();

                //Greedy best first search
                //boolean[] visited = new boolean[dataPoints.length];
                boolean[] finished = new boolean[dataPoints.length]; //Visited and no longer in frontier
                int[] indexInQueue = new int[dataPoints.length];
                List<Integer> originalIndices = new ArrayList<>();
                int currentUniqueIndex = 0;
                PriorityQueue<Integer> frontier = new PriorityQueue<>(Comparator.comparingDouble(i -> heuristicRepresentation[originalIndices.get(i)]));

                //Collections.shuffle(connectedComponents);
                List<Integer> orderedIndices = new ArrayList<>();
                for (int i = 0; i < dataPoints.length; i++) {
                    orderedIndices.add(i);
                }
                Collections.shuffle(orderedIndices);

                int componentSize = 0;
                for (int startVertex : orderedIndices) { //In case the graph contains multiple connected components
                    if (finished[startVertex]) {
                        continue;
                    }
                    //visited[startVertex] = true;
                    heuristicRepresentation[startVertex] = k; //Choose value larger than any heuristic value
                    originalIndices.add(startVertex);
                    indexInQueue[startVertex] = currentUniqueIndex;
                    frontier.add(currentUniqueIndex);
                    currentUniqueIndex++;
                    while (!frontier.isEmpty()) {
                        componentSize++;
                        int uniqueIndex = frontier.poll();
                        int vertex = originalIndices.get(uniqueIndex);
                        if (finished[vertex] || indexInQueue[vertex] != uniqueIndex) {
                            continue;
                        }
                        finished[vertex] = true;
                        addedOrder[addedOrderIndex] = vertex;
                        addedOrderIndex++;
                        List<Integer> neighbours = knnGraph.getNeighbours(vertex, k, directed)[0];
                        for (int i = 0; i < neighbours.size(); i++) {
                            int neighbor = neighbours.get(i);
                            if (!finished[neighbor]) {
                                double heuristic = knnSearchHeuristic(knnGraph, k, finished, neighbor, directed);
                                heuristicRepresentation[neighbor] = heuristic;
                                originalIndices.add(neighbor);
                                indexInQueue[neighbor] = currentUniqueIndex;
                                //visited[neighbor] = true;
                                frontier.add(currentUniqueIndex);
                                currentUniqueIndex++;
                            }
                        }
                    }
                    //Avoid small outlier connected components
                    if (componentSize <= 5) {
                        heuristicRepresentation[startVertex] = 0.0;
                    }
                }

                //Create 1D representation based on the traversed order and heuristic values
                double[][] oneDRepresentation = new double[dataPoints.length][1];
                double minimum = Double.MAX_VALUE; //We shift heuristic values by minimum so they are non-negative
                for (int i = 0; i < heuristicRepresentation.length; i++) {
                    minimum = Math.min(minimum, heuristicRepresentation[i]);
                }
                //The value of each point is the sum of heuristic values added up to and including the point
                double sum = 0.0;
                for (int i = 0; i < addedOrder.length; i++) {
                    double heuristicValue = heuristicRepresentation[addedOrder[i]] + (minimum < 0.0 ? -minimum : 0.0) + 1;
                    sum += heuristicValue;
                    oneDRepresentation[addedOrder[i]][0] = sum;
                }

                BitSet[] cuts = getInitialCutsRange(oneDRepresentation, a, 1); //Use a standard cut generator on 1D representation
                bitSets.add(cuts);
            }
        }

        return mergeCuts(bitSets);
    }

    //Calculates the heuristic value for a given vertex (for KNN initial cut generator)
    private double knnSearchHeuristic(KNNGraph knnGraph, int k, boolean[] inSet, int vertex, boolean directed) {
        double heuristicValue = 0.0;
        //int inSetCount = 0;
        //int notInSetCount = 0;
        List[] neighboursAndDistances = knnGraph.getNeighbours(vertex, k, directed);
        List<Integer> neighbours = neighboursAndDistances[0];
        List<Double> distances = neighboursAndDistances[1];
        /*for (int i = 0; i < neighbours.size(); i++) {
            int neighbor = neighbours.get(i);
            if (inSet[neighbor]) {
                inSetCount++;
            }
            else {
                notInSetCount++;
            }
        }*/
        double sum = 0.0;
        for (int i = 0; i < neighbours.size(); i++) {
            int neighbor = neighbours.get(i);
            double weight = distances.get(i);
            if (inSet[neighbor]) {
                //heuristicValue += (weight/inSetCount)*notInSetCount;
                sum += Math.exp(-weight);
            }
            else {
                //heuristicValue -= (weight/notInSetCount)*inSetCount;
                //heuristicValue += Math.exp(-weight);
            }
        }
        heuristicValue = k - sum;
        return heuristicValue;
    }



    public double[][] moveTowardsNearestNeighbours(double[][] dataPoints) {
        int iterations = 1;
        int k = 5;

        //KNNGraph knnGraph = new KNNGraph(dataPoints, k);
        /*List<List<Integer>> connectedComponents = knnGraph.getConnectedComponents();
        double[][] componentCenters = new double[connectedComponents.size()][dataPoints[0].length];
        for (int i = 0; i < connectedComponents.size(); i++) {
            for (int j = 0; j < connectedComponents.get(i).size(); j++) {
                double[] point = dataPoints[connectedComponents.get(i).get(j)];
                for (int l = 0; l < point.length; l++) {
                    componentCenters[i][l] += point[l];
                }
            }
        }
        for (int i = 0; i < componentCenters.length; i++) {
            for (int j = 0; j < componentCenters[i].length; j++) {
                componentCenters[i][j] /= connectedComponents.get(i).size();
            }
        }
        int[] componentIDs = new int[dataPoints.length];
        for (int i = 0; i < connectedComponents.size(); i++) {
            for (int j = 0; j < connectedComponents.get(i).size(); j++) {
                componentIDs[connectedComponents.get(i).get(j)] = i;
            }
        }
        return componentCenters;*/

        for (int iteration = 0; iteration < iterations; iteration++) {
            KNNGraph knnGraph = new KNNGraph(dataPoints, k);
            double[][] newPoints = new double[dataPoints.length][];
            for (int i = 0; i < newPoints.length; i++) {
                List<Integer> neighbors = knnGraph.graph.get(i);
                double[] average = Arrays.copyOf(dataPoints[i], dataPoints[i].length);
                for (int n : neighbors) {
                    for (int j = 0; j < dataPoints[0].length; j++) {
                        average[j] += dataPoints[n][j];
                    }
                }
                for (int j = 0; j < average.length; j++) {
                    average[j] /= (k+1);
                }
                newPoints[i] = average;
            }
            dataPoints = newPoints;
        }
        return dataPoints;
    }

    public BitSet[] getInitialCutsDistanceBetweenMeans(double[][] dataPoints, int a, int timesMoreCuts) {
        int shiftAmount = (int) Math.max((a/precision)/timesMoreCuts, 1);

        List<BitSet> cuts = new ArrayList<>();
        for (int shift = 0; shift < a; shift += shiftAmount) {
            double[][] copy = new double[dataPoints.length][dataPoints[0].length];
            int[] originalIndices = new int[dataPoints.length];
            for (int i = 0; i < dataPoints.length; i++) {
                originalIndices[i] = i;
                System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
            }
            for (int i = 0; i < dataPoints[0].length; i++) {
                mergeSort(copy, originalIndices, i, 0, dataPoints.length-1);
                BitSet currentBitSet = new BitSet(dataPoints.length);
                currentBitSet.setAll();
                cuts.add(currentBitSet);
                BitSet accumulated = new BitSet(dataPoints.length);
                accumulated.setAll();
                int cutIndex = 0;
                for (int j = 0; j < dataPoints.length; j++) {
                    accumulated.remove(originalIndices[j]);
                    if (j <= cutIndex) {
                        currentBitSet.remove(originalIndices[j]);
                    }
                    if (j > 0 && j % (a/precision) == shift) {
                        if (dataPoints.length - j <= (a/precision) - 1) {
                            break;
                        }
                        currentBitSet = new BitSet(dataPoints.length);
                        currentBitSet.unionWith(accumulated);
                        cuts.add(currentBitSet);
                        //Find where to put the cut.
                        double sum1 = 0.0;
                        double sum2 = 0.0;
                        int count1 = 0;
                        int count2 = 0;
                        for (int k = j+1; k < j+a/precision; k++) {
                            sum2 += copy[k][i];
                            count2++;
                        }
                        double bestDensity = -1;
                        for (int k = j+1; k < j+a/precision-1; k++) {
                            sum2 -= copy[k][i];
                            sum1 += copy[k][i];
                            count2--;
                            count1++;
                            if (count1 > 0 && count2 > 0 && (sum2/count2) - (sum1/count1) > bestDensity) {
                                bestDensity = (sum2/count2) - (sum1/count1);
                                cutIndex = k;
                            }
                        }
                    }
                }
            }
        }
        BitSet[] result = new BitSet[cuts.size()];
        for (int i = 0; i < cuts.size(); i++) {
            result[i] = cuts.get(i);
        }
        return result;
    }

    //Original initial cut generator using simple axis parallel cuts with specific amount of points between them.
    public BitSet[] getInitialCutsSimple(double[][] dataPoints, int a, int timesMoreCuts) {
        int shiftAmount = (int) Math.max((a/precision)/timesMoreCuts, 1);

        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
        for (int shift = 0; shift < a; shift += shiftAmount) {
            double[][] copy = new double[dataPoints.length][dataPoints[0].length];
            int[] originalIndices = new int[dataPoints.length];
            for (int i = 0; i < dataPoints.length; i++) {
                originalIndices[i] = i;
                System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
            }
            for (int i = 0; i < dataPoints[0].length; i++) {
                axisParallelCuts[i] = new ArrayList<>();
                mergeSort(copy, originalIndices, i, 0, dataPoints.length-1);
                //BitSet first = new BitSet(dataPoints.length);
                //first.add(originalIndices[0]);
                //cuts.add(first);
                BitSet currentBitSet = new BitSet(dataPoints.length);
                cuts.add(currentBitSet);
                axisParallelCuts[i].add(dataPoints[originalIndices[0]][i]);
                for (int j = 0; j < dataPoints.length-1; j++) {
                    currentBitSet.add(originalIndices[j]);
                    if (j > 0 && j % (a/precision) == shift) {
                        if (dataPoints.length - j <= (a/precision) - 1) {
                            break;
                        }
                        axisParallelCuts[i].add(dataPoints[originalIndices[j]][i]);
                        BitSet newBitSet = new BitSet(dataPoints.length);
                        newBitSet.unionWith(currentBitSet);
                        currentBitSet = newBitSet;
                        cuts.add(currentBitSet);
                    }
                }
            }
        }
        BitSet[] result = new BitSet[cuts.size()];
        for (int i = 0; i < cuts.size(); i++) {
            result[i] = cuts.get(i);
        }
        /*initialCuts = result;
        this.axisParallelCuts = new double[axisParallelCuts.length][];
        for (int i = 0; i < axisParallelCuts.length; i++) {
            this.axisParallelCuts[i] = new double[axisParallelCuts[i].size()];
            for (int j = 0; j < axisParallelCuts[i].size(); j++) {
                this.axisParallelCuts[i][j] = axisParallelCuts[i].get(j);
            }
        }
        cutsAreAxisParallel = true;*/
        return result;
    }

    //Initial cut generator using axis parallel cuts. Has a number of intervals with the same amount of points in each.
    //Each interval has one cut and each cut is placed at the largest range between two points in the interval.
    public BitSet[] getInitialCutsRange(double[][] dataPoints, int a, int timesMoreCuts) {
        int shiftAmount = (int) Math.max((a/precision)/timesMoreCuts, 1);

        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
        for (int shift = 0; shift < a; shift += shiftAmount) {
            double[][] copy = new double[dataPoints.length][dataPoints[0].length];
            int[] originalIndices = new int[dataPoints.length];
            for (int i = 0; i < dataPoints.length; i++) {
                originalIndices[i] = i;
                System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
            }
            for (int i = 0; i < dataPoints[0].length; i++) {
                axisParallelCuts[i] = new ArrayList<>();
                mergeSort(copy, originalIndices, i, 0, dataPoints.length-1);
                BitSet currentBitSet = new BitSet(dataPoints.length);
                currentBitSet.setAll();
                cuts.add(currentBitSet);
                BitSet accumulated = new BitSet(dataPoints.length);
                accumulated.setAll();
                axisParallelCuts[i].add(dataPoints[originalIndices[0]][i]);
                int cutIndex = 0;
                for (int j = 0; j < dataPoints.length; j++) {
                    accumulated.remove(originalIndices[j]);
                    if (j <= cutIndex) {
                        currentBitSet.remove(originalIndices[j]);
                    }
                    if (j > 0 && j % (a/precision) == shift) {
                        if (dataPoints.length - j <= (a/precision) - 1) {
                            break;
                        }
                        currentBitSet = new BitSet(dataPoints.length);
                        currentBitSet.unionWith(accumulated);
                        cuts.add(currentBitSet);
                        //Find where to put the cut.
                        double maxRange = -1;
                        for (int k = j+1; k < j+a/precision-1; k++) {
                            if (copy[k+1][i] - copy[k][i] > maxRange) {
                                maxRange = copy[k+1][i] - copy[k][i];
                                cutIndex = k;
                            }
                        }
                        axisParallelCuts[i].add(dataPoints[originalIndices[cutIndex]][i]);
                    }
                }
            }
        }
        BitSet[] result = new BitSet[cuts.size()];
        for (int i = 0; i < cuts.size(); i++) {
            result[i] = cuts.get(i);
        }
        /*initialCuts = result;
        this.axisParallelCuts = new double[axisParallelCuts.length][];
        for (int i = 0; i < axisParallelCuts.length; i++) {
            this.axisParallelCuts[i] = new double[axisParallelCuts[i].size()];
            for (int j = 0; j < axisParallelCuts[i].size(); j++) {
                this.axisParallelCuts[i][j] = axisParallelCuts[i].get(j);
            }
        }
        cutsAreAxisParallel = true;*/
        return result;
    }

    //Initial cut generator that uses axis parallel cuts and adjusts them using distances to local means in the interval on each side of the cut. Generates non axis parallel cuts.
    //This initial cut generator also has its own cost function built in.
    public BitSet[] getInitialCutsLocalMeans(double[][] dataPoints, int a, int timesMoreCuts) {
        int shiftAmount = (int) Math.max((a/precision)/timesMoreCuts, 1);

        double range = getMaxRange(dataPoints);
        List<Double> costs = new ArrayList<>();
        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
        for (int shift = 0; shift < a; shift += shiftAmount) {
            double[][] copy = new double[dataPoints.length][dataPoints[0].length];
            int[] originalIndices = new int[dataPoints.length];
            for (int i = 0; i < dataPoints.length; i++) {
                originalIndices[i] = i;
                System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
            }
            for (int i = 0; i < dataPoints[0].length; i++) {
                axisParallelCuts[i] = new ArrayList<>();
                mergeSort(copy, originalIndices, i, 0, dataPoints.length-1);
                BitSet currentBitSet = new BitSet(dataPoints.length);
                currentBitSet.setAll();
                cuts.add(currentBitSet);
                BitSet accumulated = new BitSet(dataPoints.length);
                accumulated.setAll();
                axisParallelCuts[i].add(dataPoints[originalIndices[0]][i]);
                int cutIndex = 0;
                double[] mean1 = null;
                double[] mean2 = null;
                double cost = 0.0;
                for (int j = 0; j < dataPoints.length; j++) {
                    accumulated.remove(originalIndices[j]);
                    if (j <= cutIndex) {
                        if (mean1 == null || getDistance(dataPoints[originalIndices[j]], mean1) < getDistance(dataPoints[originalIndices[j]], mean2)) {
                            currentBitSet.remove(originalIndices[j]);
                        }
                    }
                    else if (mean1 != null && getDistance(dataPoints[originalIndices[j]], mean1) < getDistance(dataPoints[originalIndices[j]], mean2)) {
                        currentBitSet.remove(originalIndices[j]);
                    }
                    if (mean1 != null) {
                        cost += Math.exp(-((1.0/range)*getDistance(dataPoints[originalIndices[j]], (currentBitSet.get(originalIndices[j]) ? mean1 : mean2))));
                    }
                    if (j > 0 && j % (a/precision) == shift) {
                        if (dataPoints.length - j <= (a/precision) - 1) {
                            break;
                        }
                        currentBitSet = new BitSet(dataPoints.length);
                        currentBitSet.unionWith(accumulated);
                        cuts.add(currentBitSet);
                        costs.add(cost);
                        cost = 0.0;
                        //Find where to put the cut.
                        double maxRange = -1;
                        for (int k = j+1; k < j+a/precision-1; k++) {
                            if (copy[k+1][i] - copy[k][i] > maxRange) {
                                maxRange = copy[k+1][i] - copy[k][i];
                                cutIndex = k;
                            }
                        }
                        axisParallelCuts[i].add(dataPoints[originalIndices[cutIndex]][i]);
                        //Calculate means.
                        mean1 = new double[dataPoints[0].length];
                        mean2 = new double[dataPoints[0].length];
                        int n1 = 0;
                        int n2 = 0;
                        for (int k = j+1; k < j+a/precision-1; k++) {
                            for (int l = 0; l < dataPoints[originalIndices[k]].length; l++) {
                                if (k <= cutIndex) {
                                    mean1[l] += dataPoints[originalIndices[k]][l];
                                    if (l == 0) {
                                        n1++;
                                    }
                                }
                                else {
                                    mean2[l] += dataPoints[originalIndices[k]][l];
                                    if (l == 0) {
                                        n2++;
                                    }
                                }
                            }
                        }
                        for (int k = 0; k < mean1.length; k++) {
                            mean1[k] /= n1;
                            mean2[k] /= n2;
                        }
                    }
                }
                costs.add(cost);
            }
        }
        BitSet[] result = new BitSet[cuts.size()];
        for (int i = 0; i < cuts.size(); i++) {
            result[i] = cuts.get(i);
        }
        /*initialCuts = result;
        this.axisParallelCuts = new double[axisParallelCuts.length][];
        for (int i = 0; i < axisParallelCuts.length; i++) {
            this.axisParallelCuts[i] = new double[axisParallelCuts[i].size()];
            for (int j = 0; j < axisParallelCuts[i].size(); j++) {
                this.axisParallelCuts[i][j] = axisParallelCuts[i].get(j);
            }
        }*/
        /*cutCosts = new double[costs.size()];
        for (int i = 0; i < costs.size(); i++) {
            cutCosts[i] = costs.get(i);
        }*/
        //cutsAreAxisParallel = false;
        return result;
    }

    //Sorts data points by a specific dimension.
    public static void mergeSort(double[][] points, int[] originalIndices, int dimension, int l, int h) {
        if (l >= h) {
            return;
        }
        mergeSort(points, originalIndices, dimension, l, (l+h)/2);
        mergeSort(points, originalIndices, dimension, (l+h)/2+1, h);
        merge(points, originalIndices, dimension, l, h);
    }

    //Merge part of the merge sort algorithm.
    private static void merge(double[][] points, int[] originalIndices, int dimension, int l, int h) {
        double[][] L = new double[(h-l)/2+1][];
        double[][] R = new double[(h-l) % 2 == 0 ? (h-l)/2 : (h-l)/2+1][];
        int[] L2 = new int[L.length];
        int[] R2 = new int[R.length];
        for (int i = 0; i < L.length; i++) {
            L[i] = points[l+i];
            L2[i] = originalIndices[l+i];
        }
        for (int i = 0; i < R.length; i++) {
            R[i] = points[l+L.length+i];
            R2[i] = originalIndices[l+L.length+i];
        }
        int p1 = 0;
        int p2 = 0;
        for (int i = l; i <= h; i++) {
            if ((p2 >= R.length) || (p1 < L.length && L[p1][dimension] < R[p2][dimension])) {
                points[i] = L[p1];
                originalIndices[i] = L2[p1];
                p1++;
            }
            else {
                points[i] = R[p2];
                originalIndices[i] = R2[p2];
                p2++;
            }
        }
    }

    //Calculates the largest range in a dimension between two points.
    private double getMaxRange(double[][] dataPoints) {
        double maxRange = 0;
        for (int i = 0; i < dataPoints.length; i++) {
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            for (int j = 0; j < dataPoints[i].length; j++) {
                if (dataPoints[i][j] < minValue) {
                    minValue = dataPoints[i][j];
                }
                if (dataPoints[i][j] > maxValue) {
                    maxValue = dataPoints[i][j];
                }
            }
            if (maxValue - minValue > maxRange) {
                maxRange = maxValue - minValue;
            }
        }
        return maxRange;
    }

    //Returns the euclidean distance between two points.
    private double getDistance(double[] point1, double[] point2) {
        /*double length = 0;
        for (int i = 0; i < point1.length; i++) {
            length += (point1[i]-point2[i])*(point1[i]-point2[i]);
        }
        return Math.sqrt(length);*/
        return Distance.euclidean().distance(point1, point2);
    }
}

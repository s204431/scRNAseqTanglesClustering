package datasets;

import clustering.Model;
import main.Main;
import util.BitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CutGenerators {

    //NOTE: The content in this file is from the bachelor project.

    private static final double precision = 1; //Determines the number of cuts generated.

    public double[] cutCosts; //For local means only


    public BitSet[] splitCutGenerator(double[][] dataPoints, int a) {
        int splitSize = 1000;

        List<double[][]> splits = new ArrayList<>();


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

        List<BitSet[]> bitSets = new ArrayList<>();

        for (int i = 0; i < splits.size(); i++) {
            System.out.println(splits.get(i)[0].length);
            bitSets.add(combinedCutGenerator(splits.get(i), a));
        }

        return mergeCuts(bitSets);
    }

    public BitSet[] combinedCutGenerator(double[][] dataPoints, int a) {
        int nComponents = 3;

        List<BitSet[]> bitSets = new ArrayList<>();

        //bitSets.add(getInitialCutsLocalMeans(Model.pca(dataPoints, nComponents), a));

        bitSets.add(getInitialCutsLocalMeans(Model.tsne(dataPoints, nComponents), a));
        try {
            bitSets.add(getInitialCutsLocalMeans(Model.umap(dataPoints, nComponents), a));
        }
        catch (Exception e) {

        }
        bitSets.add(getInitialCutsLocalMeans(Model.svd(dataPoints, nComponents), a));

        return mergeCuts(bitSets);
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

    //Original initial cut generator using simple axis parallel cuts with specific amount of points between them.
    public BitSet[] getInitialCutsSimple(double[][] dataPoints, int a) {
        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
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
                if (j > 0 && j % (a/precision) == 0) {
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
    public BitSet[] getInitialCutsRange(double[][] dataPoints, int a) {
        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
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
                if (j > 0 && j % (a/precision) == 0) {
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
        System.out.println(result.length);
        return result;
    }

    //Initial cut generator that uses axis parallel cuts and adjusts them using distances to local means in the interval on each side of the cut. Generates non axis parallel cuts.
    //This initial cut generator also has its own cost function built in.
    public BitSet[] getInitialCutsLocalMeans(double[][] dataPoints, int a) {
        double range = getMaxRange(dataPoints);
        List<Double> costs = new ArrayList<>();
        List<BitSet> cuts = new ArrayList<>();
        List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
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
                if (j > 0 && j % (a/precision) == 0) {
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
        cutCosts = new double[costs.size()];
        for (int i = 0; i < costs.size(); i++) {
            cutCosts[i] = costs.get(i);
        }
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
        double length = 0;
        for (int i = 0; i < point1.length; i++) {
            length += (point1[i]-point2[i])*(point1[i]-point2[i]);
        }
        return Math.sqrt(length);
    }
}

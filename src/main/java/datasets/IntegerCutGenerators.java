package datasets;

import util.BitSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static datasets.CutGenerators.mergeSort;

public class IntegerCutGenerators {

    public static double precision = 1;

    //Assumes integer values in data.
    public BitSet[] getInitialCutsIntegerRange(double[][] dataPoints) {
        BitSet[] cuts = new BitSet[dataPoints[0].length];
        //List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
        double[][] copy = new double[dataPoints.length][dataPoints[0].length];
        int[] originalIndices = new int[dataPoints.length];
        for (int i = 0; i < dataPoints.length; i++) {
            originalIndices[i] = i;
            System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
        }
        for (int i = 0; i < dataPoints[0].length; i++) {
            //axisParallelCuts[i] = new ArrayList<>();
            mergeSort(copy, originalIndices, i, 0, dataPoints.length - 1);
            BitSet currentBitSet = new BitSet(dataPoints.length);
            currentBitSet.setAll();


            List<Double> uniqueVals = new ArrayList<>();
            List<Integer> uniqueCounts = new ArrayList<>();
            int currentCount = 0;
            uniqueVals.add(copy[0][i]);
            for (int j = 0; j < dataPoints.length; j++) {
                if (copy[j][i] == uniqueVals.getLast()) {
                    currentCount++;
                } else {
                    uniqueCounts.add(currentCount);
                    currentCount = 1;
                    uniqueVals.add(copy[j][i]);
                }
            }
            uniqueCounts.add(currentCount);

            int largestCountDiff = -1;
            double largestCountDiffVal = -1;
            for (int j = 1; j < uniqueCounts.size(); j++) {
                if (Math.abs(uniqueCounts.get(j) - uniqueCounts.get(j - 1)) > largestCountDiff) {
                    largestCountDiff = Math.abs(uniqueCounts.get(j) - uniqueCounts.get(j - 1));
                    largestCountDiffVal = uniqueVals.get(j);
                }
            }

            for (int j = 0; j < dataPoints.length; j++) {
                if (copy[j][i] == largestCountDiffVal) {
                    break;
                }
                currentBitSet.remove(originalIndices[j]);
            }

            cuts[i] = currentBitSet;

        }
        return cuts;


            /*cuts.add(currentBitSet);
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
        cutsAreAxisParallel = true;
        return result;*/
    }

    public BitSet[] getInitialCutsRandom(double[][] dataPoints) {
        BitSet[] cuts = new BitSet[dataPoints[0].length];
        //List<Double>[] axisParallelCuts = new ArrayList[dataPoints[0].length]; //For visualization.
        double[][] copy = new double[dataPoints.length][dataPoints[0].length];
        int[] originalIndices = new int[dataPoints.length];
        for (int i = 0; i < dataPoints.length; i++) {
            originalIndices[i] = i;
            System.arraycopy(dataPoints[i], 0, copy[i], 0, dataPoints[0].length);
        }
        for (int i = 0; i < dataPoints[0].length; i++) {
            //axisParallelCuts[i] = new ArrayList<>();
            mergeSort(copy, originalIndices, i, 0, dataPoints.length - 1);
            BitSet currentBitSet = new BitSet(dataPoints.length);
            currentBitSet.setAll();

            int randomPoint = new Random().nextInt(dataPoints.length);

            for (int j = 0; j < randomPoint; j++) {
                currentBitSet.remove(originalIndices[j]);
            }

            cuts[i] = currentBitSet;

        }
        return cuts;
    }
}

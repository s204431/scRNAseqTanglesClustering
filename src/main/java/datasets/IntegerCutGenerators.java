package datasets;

import clustering.Model;
import util.BitSet;

import java.util.*;

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

    public BitSet[] getInitialCutsDimensionSimilarity(double[][] dataPoints, int a) {
        int maxDimensions = 1000;

        double[][] reducedPoints = new double[dataPoints.length][Math.min(dataPoints[0].length, maxDimensions)];
        for (int i = 0; i < reducedPoints.length; i++) {
            for (int j = 0; j < reducedPoints[i].length; j++) {
                reducedPoints[i][j] = dataPoints[i][j];
            }
        }

        BitSet[] cuts = new BitSet[reducedPoints[0].length];

        int[][] sortedLocation = new int[reducedPoints.length][reducedPoints[0].length];
        int[][] dataPointLocation = new int[reducedPoints.length][reducedPoints[0].length];

        for (int i = 0; i < reducedPoints[0].length; i++) {
            Integer[] pointerArray = new Integer[reducedPoints.length];
            for (int j = 0; j < pointerArray.length; j++) {
                pointerArray[j] = j;
            }
            int index = i;
            Arrays.sort(pointerArray, Comparator.comparingDouble(v -> reducedPoints[v][index]));
            for (int j = 0; j < pointerArray.length; j++) {
                sortedLocation[pointerArray[j]][i] = j;
                dataPointLocation[j][i] = pointerArray[j];
            }
        }

        for (int i = 0; i < reducedPoints[0].length; i++) {
            double totalCost = 0;
            double minCost = Double.MAX_VALUE;
            int bestIndex = -1;
            for (int j = 0; j < dataPointLocation.length-a; j++) {
                int dataPoint = dataPointLocation[j][i];
                for (int d = 0; d < reducedPoints[0].length; d++) {
                    if (d == i) {
                        continue;
                    }
                    int location = sortedLocation[dataPoint][d];
                    int original1 = location-1 >= 0 ? sortedLocation[dataPointLocation[location-1][d]][i] : -1;
                    int original2 = location+1 < dataPointLocation.length ? sortedLocation[dataPointLocation[location+1][d]][i] : -1;
                    if (original1 >= 0 && j > original1) {
                        totalCost--;
                    }
                    else if (original1 >= 0) {
                        totalCost++;
                    }
                    if (original2 >= 0 && j > original2) {
                        totalCost--;
                    }
                    else if (original2 >= 0) {
                        totalCost++;
                    }
                }
                double costNormalized = totalCost/Math.min(j+1, reducedPoints.length-j);
                //double costNormalized = totalCost;
                //System.out.print(costNormalized + ", ");
                if (j >= a && costNormalized < minCost) {
                    minCost = costNormalized;
                    bestIndex = j;
                }
            }
            //System.out.println(bestIndex + " " + minCost);
            BitSet cut = new BitSet(reducedPoints.length);
            for (int j = 0; j <= bestIndex; j++) {
                cut.setValue(dataPointLocation[j][i], true);
            }
            cuts[i] = cut;
        }
        return cuts;
    }
}

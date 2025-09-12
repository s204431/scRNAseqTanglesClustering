package datasets;

import clustering.Model;
import main.Main;
import util.BitSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CostFunctions {

    //NOTE: The content in this file is from the bachelor project.

    public double[] averageCostFunction(double[][] dataPoints, BitSet[] initialCuts) {
        int splitSize = 1000;

        double[] costs = new double[initialCuts.length];

        int nSplits = (int)Math.ceil(dataPoints[0].length/(double)splitSize);

        List<double[][]> splits = new ArrayList<>();
        List<List<Double>[]> splitsList = new ArrayList<>();
        /*double[][] currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length)];
        int index = 0;

        for (int i = 0; i < dataPoints[0].length; i++) {
            for (int j = 0; j < dataPoints.length; j++) {
                currentSplit[j][index] = dataPoints[j][i];
            }
            index++;
            if (index == splitSize && i < dataPoints[0].length-1) {
                index = 0;
                splits.add(currentSplit);
                currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length - i - 1)];
            }
        }

        splits.add(currentSplit);*/

        for (int i = 0; i < nSplits; i++) {
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
        }

        AverageParallelRunner[] runnables = new AverageParallelRunner[splits.size()];
        Thread[] threads = new Thread[splits.size()];
        for (int i = 0; i < splits.size(); i++) {
            runnables[i] = new AverageParallelRunner();
            runnables[i].data = splits.get(i);
            runnables[i].initialCuts = initialCuts;
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
                double[] splitCosts = runnables[i].result;
                for (int j = 0; j < costs.length; j++) {
                    costs[j] += splitCosts[j];
                }
            }
            catch (Exception e) {

            }
        }

        return costs;
    }

    public double[] bestFirstCostFunction(double[][] dataPoints, BitSet[] initialCuts) {
        int splitSize = 1000;

        double[] costs = new double[initialCuts.length];
        Arrays.fill(costs, Double.MAX_VALUE);

        double[][] currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length)];
        List<double[][]> splits = new ArrayList<>();
        int index = 0;

        for (int i = 0; i < dataPoints[0].length; i++) {
            for (int j = 0; j < dataPoints.length; j++) {
                currentSplit[j][index] = dataPoints[j][i];
            }
            index++;
            if (index == splitSize && i < dataPoints[0].length-1) {
                index = 0;
                splits.add(currentSplit);
                /*double[] splitCosts = shortestDistanceCostFunction(currentSplit, initialCuts);
                for (int j = 0; j < costs.length; j++) {
                    costs[j] += splitCosts[j];
                }*/
                currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length - i - 1)];
            }
        }

        splits.add(currentSplit);
        /*double[] splitCosts = pairwiseDistanceCostFunction(currentSplit, initialCuts);
        for (int j = 0; j < costs.length; j++) {
            costs[j] += splitCosts[j];
        }*/

        AverageParallelRunner[] runnables = new AverageParallelRunner[splits.size()];
        Thread[] threads = new Thread[splits.size()];
        for (int i = 0; i < splits.size(); i++) {
            runnables[i] = new AverageParallelRunner();
            runnables[i].data = splits.get(i);
            runnables[i].initialCuts = initialCuts;
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
                double[] splitCosts = runnables[i].result;
                for (int j = 0; j < costs.length; j++) {
                    costs[j] = Math.min(costs[j], splitCosts[j]);
                }
            }
            catch (Exception e) {

            }
        }

        return costs;
    }

    public class AverageParallelRunner implements Runnable {

        public double[] result;
        public double[][] data;
        public BitSet[] initialCuts;
        @Override
        public void run() {
            result = shortestDistanceCostFunction(data, initialCuts);
        }
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] shortestDistanceCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        //dataPoints = Model.pca(dataPoints, 100);

        dataPoints = Model.tsne(dataPoints, 5);
        Main.zScoreNorm(dataPoints);

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            int count = initialCuts[i].count();
            if (count == dataPoints.length || count == 0) {
                costs[i] = 1000000;
                continue;
            }
            double cost = Double.MAX_VALUE;
            for (int j = 0; j < dataPoints.length; j++) {
                if (initialCuts[i].get(j)) {
                    continue;
                }
                for (int k = j; k < dataPoints.length; k++) {
                    if (!initialCuts[i].get(k)) {
                        continue;
                    }
                    cost = Math.min(getDistance(dataPoints[j], dataPoints[k]), cost);
                }
            }
            costs[i] = Math.exp(-5.0*(1.0/maxRange)*cost);
        }
        //cutCosts = costs;
        return costs;
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] pairwiseDistanceCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        dataPoints = Model.tsne(dataPoints, 2);
        Main.zScoreNorm(dataPoints);

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            int count = initialCuts[i].count();
            if (count == dataPoints.length || count == 0) {
                costs[i] = 1000000;
                continue;
            }
            double cost = 0;
            for (int j = 0; j < dataPoints.length; j++) {
                if (initialCuts[i].get(j)) {
                    continue;
                }
                for (int k = j; k < dataPoints.length; k++) {
                    if (!initialCuts[i].get(k)) {
                        continue;
                    }
                    cost += Math.exp(-1.0*(1.0/maxRange)*getDistance(dataPoints[j], dataPoints[k]));
                }
            }
            costs[i] = cost/(initialCuts[i].count()*(initialCuts[i].size()-initialCuts[i].count()));
        }
        //cutCosts = costs;
        return costs;
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] pairwiseClosestCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        dataPoints = Model.tsne(dataPoints, 5);
        Main.zScoreNorm(dataPoints);

        double threshold = 1.0;

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            int count = initialCuts[i].count();
            if (count == dataPoints.length || count == 0) {
                costs[i] = 1000000;
                continue;
            }
            double cost = 0;
            int size1 = initialCuts[i].count();
            int size0 = dataPoints.length - size1;
            int nBad = 0;
            boolean smallest = false;
            if (size0 > size1) {
                smallest = true;
            }
            for (int j = 0; j < dataPoints.length; j++) {
                //if (initialCuts[i].get(j) != smallest) {
                //    continue;
                //}
                double closestDist = Double.MAX_VALUE;
                for (int k = 0; k < dataPoints.length; k++) {
                    if (initialCuts[i].get(j) == initialCuts[i].get(k)) {
                        continue;
                    }
                    closestDist = Math.min(closestDist, getDistance(dataPoints[j], dataPoints[k]));
                }
                if (closestDist < threshold) {
                    cost += Math.exp(-5.0*(1.0/maxRange)*closestDist);
                    nBad++;
                }
            }
            if (nBad == 0) {
                costs[i] = 0.0;
            }
            else {
                costs[i] = cost;//cost/nBad;// / (smallest ? size1 : size0);//(initialCuts[i].count()*(initialCuts[i].size()-initialCuts[i].count()));
            }
        }
        //cutCosts = costs;
        return costs;
    }

    //Distance to mean cost function, which uses the sum of the distance to the opposite side mean for every point (has linear time complexity).
    public double[] distanceToMeanCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        dataPoints = Model.tsne(dataPoints, 5);
        Main.zScoreNorm(dataPoints);

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);

        for (int i = 0; i < initialCuts.length; i++) {
            int count = initialCuts[i].count();
            if (count == dataPoints.length || count == 0) {
                costs[i] = 1000000;
                continue;
            }
            int cutCount = initialCuts[i].count();
            double[] mean1 = new double[dataPoints[0].length];
            double[] mean2 = new double[dataPoints[0].length];
            //Calculate means of the two sides of the cut.

            for (int j = 0; j < initialCuts[i].size(); j++) {
                for (int k = 0; k < dataPoints[0].length; k++) {
                    if (initialCuts[i].get(j)) {
                        mean1[k] += dataPoints[j][k];
                    }
                    else {
                        mean2[k] += dataPoints[j][k];
                    }
                }
            }
            for (int j = 0; j < mean1.length; j++) {
                mean1[j] /= cutCount;
                mean2[j] /= initialCuts[i].size() - cutCount;
            }
            //Sum up distances from the means.
            for (int j = 0; j < initialCuts[i].size(); j++) {
                double[] mean = initialCuts[i].get(j) ? mean2 : mean1;
                costs[i] += Math.exp(-(1.0/maxRange)*getDistance(dataPoints[j], mean));
            }
        }
        //cutCosts = costs;
        return costs;
    }

    //Dimensional error cost function
    public double[] dimensionalErrorCostFunction(double[][] dataPoints, BitSet[] initialCuts) {
        long time1 = System.nanoTime();

        int cuts = initialCuts.length;
        double[] costs = new double[cuts];

        for (int dimension = 0; dimension < dataPoints[0].length; dimension++) {

            Integer[] sortedIndices = new Integer[dataPoints.length];
            for (int i = 0; i < sortedIndices.length; i++) sortedIndices[i] = i;
            final int dim = dimension;
            Arrays.sort(sortedIndices, Comparator.comparingDouble(i -> dataPoints[i][dim]));

            for (int cutIndex = 0; cutIndex < cuts; cutIndex++) {
                BitSet cut = initialCuts[cutIndex];
                int cutCount = cut.count();
                //Flip the cut if the majority of points are 0
                boolean flip = cutCount < (cut.size() / 2);
                int highIndex = cut.countFlipped(flip);
                int lowIndex = cut.size() - highIndex;
                double highValue = dataPoints[sortedIndices[highIndex]][dimension];
                double lowValue = dataPoints[sortedIndices[lowIndex]][dimension];

                int errors = 0;
                for (int pointIndex = 0; pointIndex < dataPoints.length; pointIndex++) {
                    double value = dataPoints[pointIndex][dimension];
                    if (value == 0) continue;

                    // Cut majority
                    if ((!flip && cut.get(pointIndex)) || (flip && !cut.get(pointIndex))) {
                        if (value < lowValue) errors++;
                        if (value > highValue) errors++;

                        // Cut minority
                    } else {
                        if (value > lowValue) errors++;
                        if (value < highValue) errors++;
                    }
                }
                costs[cutIndex] += (double) errors / dataPoints.length;
            }
        }

        System.out.println((System.nanoTime() - time1)/1000000.0);

        return costs;
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

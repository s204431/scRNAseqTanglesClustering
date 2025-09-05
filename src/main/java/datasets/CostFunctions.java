package datasets;

import clustering.Model;
import main.Main;
import util.BitSet;

import java.util.ArrayList;
import java.util.List;

public class CostFunctions {

    //NOTE: The content in this file is from the bachelor project.

    public double[] averageCostFunction(double[][] dataPoints, BitSet[] initialCuts) {
        int splitSize = 1000;

        double[] costs = new double[initialCuts.length];
        double[][] currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length)];
        int index = 0;

        for (int i = 0; i < dataPoints[0].length; i++) {
            for (int j = 0; j < dataPoints.length; j++) {
                currentSplit[j][index] = dataPoints[j][i];
            }
            index++;
            if (index == splitSize && i < dataPoints[0].length-1) {
                index = 0;
                double[] splitCosts = shortestDistanceCostFunction(currentSplit, initialCuts);
                for (int j = 0; j < costs.length; j++) {
                    costs[j] += splitCosts[j];
                }
                currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length - i - 1)];
            }
        }

        double[] splitCosts = pairwiseDistanceCostFunction(currentSplit, initialCuts);
        for (int j = 0; j < costs.length; j++) {
            costs[j] += splitCosts[j];
        }

        return costs;
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] shortestDistanceCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        dataPoints = Model.tsne(dataPoints, 3);

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
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

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            double cost = 0;
            for (int j = 0; j < dataPoints.length; j++) {
                if (initialCuts[i].get(j)) {
                    continue;
                }
                for (int k = j; k < dataPoints.length; k++) {
                    if (!initialCuts[i].get(k)) {
                        continue;
                    }
                    cost += Math.exp(-5.0*(1.0/maxRange)*getDistance(dataPoints[j], dataPoints[k]));
                }
            }
            costs[i] = cost/(initialCuts[i].count()*(initialCuts[i].size()-initialCuts[i].count()));
        }
        //cutCosts = costs;
        return costs;
    }

    //Distance to mean cost function, which uses the sum of the distance to the opposite side mean for every point (has linear time complexity).
    public double[] distanceToMeanCostFunction(double[][] dataPoints, BitSet[] initialCuts) {
        long time1 = System.nanoTime();

        dataPoints = Model.tsne(dataPoints, 2);

        long expTime = 0;

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);

        for (int i = 0; i < initialCuts.length; i++) {
            int cutCount = initialCuts[i].count();
            double[] mean1 = new double[dataPoints[0].length];
            double[] mean2 = new double[dataPoints[0].length];
            //Calculate means of the two sides of the cut.

            long time2 = System.nanoTime();
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
            expTime += System.nanoTime() - time2;
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
        System.out.println(expTime/1000000.0);
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

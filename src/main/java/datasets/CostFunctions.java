package datasets;

import clustering.Model;
import elki.math.statistics.distribution.GeneralizedLogisticAlternateDistribution;
import main.Main;
import smile.classification.KNN;
import util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CostFunctions {

    //NOTE: The content in this file is from the bachelor project.

    private BitSet mask; //Mask of remaining points in case some have been removed.

    public List<double[][]> reducedPoints; //Storing reduced points for reusing.
    public List<KNNGraph> cachedKNNGraphs;

    private Monitor monitor;

    public CostFunctions(Monitor monitor) {
        this.monitor = monitor;
    }

    public void setMask(BitSet mask) {
        this.mask = mask;
    }

    public double[] averageCostFunction(double[][] dataPoints,
                                        BitSet[] initialCuts,
                                        String lowLevelCostFunctionName,
                                        boolean useCache,
                                        int splitSize,
                                        boolean usePca,
                                        int pcaComponents,
                                        boolean useTsne,
                                        int tsneComponents) {

        double[] costs = new double[initialCuts.length];

        int nSplits = (int)Math.ceil(dataPoints[0].length/(double)splitSize);

        List<double[][]> splits = new ArrayList<>();
        boolean cacheUsed;

        if (useCache && reducedPoints != null) {
            splits = loadFromCache();
            cacheUsed = true;
        }
        else {
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
            if (useCache) {
                reducedPoints = new ArrayList<>();
                if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                    cachedKNNGraphs = new ArrayList<>();
                }
            }
            cacheUsed = false;
        }

        AverageParallelRunner[] runnables = new AverageParallelRunner[splits.size()];
        Thread[] threads = new Thread[splits.size()];
        for (int i = 0; i < splits.size(); i++) {
            runnables[i] = new AverageParallelRunner();
            runnables[i].data = splits.get(i);
            runnables[i].initialCuts = initialCuts;
            runnables[i].lowLevelCostFunctionName = lowLevelCostFunctionName;
            runnables[i].index = i;
            runnables[i].cacheUsed = cacheUsed;
            runnables[i].usePca = usePca;
            runnables[i].pcaComponents = pcaComponents;
            runnables[i].useTsne = useTsne;
            runnables[i].tsneComponents = tsneComponents;
            if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN) && cachedKNNGraphs != null && cachedKNNGraphs.size() == splits.size()) {
                runnables[i].localKNNGraph = cachedKNNGraphs.get(i).applyMask(mask);
            }
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        /*for (int i = 0; i < splits.size(); i++) {
            System.out.println(splits.get(i)[0].length);
            bitSets.add(combinedCutGenerator(splits.get(i), a));
        }*/

        long maxTime = 0;
        for (int i = 0; i < splits.size(); i++) {
            try {
                threads[i].join();
                maxTime = Math.max(maxTime, runnables[i].dimReductionTime);
                double[] splitCosts = runnables[i].result;
                for (int j = 0; j < costs.length; j++) {
                    costs[j] += splitCosts[j];
                }
                if (useCache && reducedPoints.size() < splits.size()) {
                    reducedPoints.add(runnables[i].localReducedPoints);
                    if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                        cachedKNNGraphs.add(runnables[i].localKNNGraph);
                    }
                }
            }
            catch (Exception e) {

            }
        }
        monitor.addDimReductionTime(maxTime);

        for (int i = 0; i < costs.length; i++) {
            costs[i] /= splits.size();
        }

        return costs;
    }

    public double[] bestFirstCostFunction(double[][] dataPoints,
                                          BitSet[] initialCuts,
                                          String lowLevelCostFunctionName,
                                          boolean useCache,
                                          int splitSize,
                                          boolean usePca,
                                          int pcaComponents,
                                          boolean useTsne,
                                          int tsneComponents) {

        double[] costs = new double[initialCuts.length];
        Arrays.fill(costs, Double.MAX_VALUE);

        List<double[][]> splits = new ArrayList<>();
        boolean cacheUsed;
        if (useCache && reducedPoints != null) {
            splits = loadFromCache();
            cacheUsed = true;
        }
        else {
            double[][] currentSplit = new double[dataPoints.length][Math.min(splitSize, dataPoints[0].length)];
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
            if (useCache) {
                reducedPoints = new ArrayList<>();
                if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                    cachedKNNGraphs = new ArrayList<>();
                }
            }
            cacheUsed = false;
        }

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
            runnables[i].lowLevelCostFunctionName = lowLevelCostFunctionName;
            runnables[i].index = i;
            runnables[i].cacheUsed = cacheUsed;
            runnables[i].usePca = usePca;
            runnables[i].pcaComponents = pcaComponents;
            runnables[i].useTsne = useTsne;
            runnables[i].tsneComponents = tsneComponents;
            if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN) && cachedKNNGraphs != null && cachedKNNGraphs.size() == splits.size()) {
                runnables[i].localKNNGraph = cachedKNNGraphs.get(i).applyMask(mask);
            }
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        /*for (int i = 0; i < splits.size(); i++) {
            System.out.println(splits.get(i)[0].length);
            bitSets.add(combinedCutGenerator(splits.get(i), a));
        }*/

        long maxTime = 0;
        for (int i = 0; i < splits.size(); i++) {
            try {
                threads[i].join();
                maxTime = Math.max(maxTime, runnables[i].dimReductionTime);
                double[] splitCosts = runnables[i].result;
                for (int j = 0; j < costs.length; j++) {
                    costs[j] = Math.min(costs[j], splitCosts[j]);
                }
                if (useCache && reducedPoints.size() < splits.size()) {
                    reducedPoints.add(runnables[i].localReducedPoints);
                    if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                        cachedKNNGraphs.add(runnables[i].localKNNGraph);
                    }
                }
            }
            catch (Exception e) {

            }
        }
        monitor.addDimReductionTime(maxTime);

        return costs;
    }

    public class AverageParallelRunner implements Runnable {

        public double[] result;
        public double[][] data;
        public BitSet[] initialCuts;
        public String lowLevelCostFunctionName;
        public boolean cacheUsed;
        public int index;
        public boolean usePca;
        public int pcaComponents;
        public boolean useTsne;
        public int tsneComponents;
        public long dimReductionTime;

        public double[][] localReducedPoints; //For caching.
        public KNNGraph localKNNGraph; //For caching.

        @Override
        public void run() {
            if (!cacheUsed) {
                long startTime = System.currentTimeMillis();
                data = usePca ? Model.svdWithElbow(data) : Model.tsne(data, tsneComponents);
                dimReductionTime = System.currentTimeMillis() - startTime;

                data = Main.zScoreNorm(data);
                localReducedPoints = data;
                if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                    localKNNGraph = createKNNGraph(data);
                }
            }
            result = runLowLevelCostFunction(data, initialCuts, lowLevelCostFunctionName, localKNNGraph);
        }
    }

    //This cost function reduces points and runs a single other cost function.
    public double[] singleCostFunction(double[][] dataPoints,
                                       BitSet[] initialCuts,
                                       String lowLevelCostFunctionName,
                                       boolean useCache,
                                       boolean usePca,
                                       int pcaComponents,
                                       boolean useTsne,
                                       int tsneComponents) {
        KNNGraph knnGraph = null;
        if (useCache && reducedPoints != null) {
            dataPoints = loadFromCache().getFirst();
            if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN) && cachedKNNGraphs != null) {
                knnGraph = cachedKNNGraphs.getFirst().applyMask(mask);
            }
        }
        else {
            long startTime = System.currentTimeMillis();
            dataPoints = usePca ? Model.svdWithElbow(dataPoints) : Model.tsne(dataPoints, tsneComponents);
            monitor.addDimReductionTime(System.currentTimeMillis() - startTime);

            dataPoints = Main.zScoreNorm(dataPoints);
            if (useCache) {
                reducedPoints = new ArrayList<>();
                reducedPoints.add(dataPoints);
                if (lowLevelCostFunctionName.equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                    cachedKNNGraphs = new ArrayList<>();
                    knnGraph = createKNNGraph(dataPoints);
                    cachedKNNGraphs.add(knnGraph);
                }
            }
        }

        return runLowLevelCostFunction(dataPoints, initialCuts, lowLevelCostFunctionName, knnGraph);
    }

    public double[] runLowLevelCostFunction(double[][] dataPoints, BitSet[] initialCuts, String costFunctionName, KNNGraph knnGraph) {
        return switch (costFunctionName) {
            case GlobalConstants.LOW_LEVEL_COST_FUNCTION_PAIRWISE -> pairwiseDistanceCostFunction(dataPoints, initialCuts);
            case GlobalConstants.LOW_LEVEL_COST_FUNCTION_SHORTEST -> shortestDistanceCostFunction(dataPoints, initialCuts);
            case GlobalConstants.LOW_LEVEL_COST_FUNCTION_PAIRWISE_CLOSEST -> pairwiseClosestCostFunction(dataPoints, initialCuts);
            case GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN -> knnCostFunction(dataPoints, initialCuts, knnGraph);
            case GlobalConstants.LOW_LEVEL_COST_FUNCTION_DISTANCE_TO_MEAN -> distanceToMeanCostFunction(dataPoints, initialCuts);
            default -> knnCostFunction(dataPoints, initialCuts, knnGraph);
        };
    }

    private List<double[][]> loadFromCache() {
        List<double[][]> result = new ArrayList<>();
        for (int i = 0; i < reducedPoints.size(); i++) {
            double[][] maskedPoints = new double[mask.count()][];
            int index = 0;
            for (int j = 0; j < mask.size(); j++) {
                if (mask.get(j)) {
                    maskedPoints[index] = reducedPoints.get(i)[j];
                    index++;
                }
            }
            result.add(maskedPoints);
        }
        return result;
    }

    public double[] knnCostFunction(double[][] dataPoints, BitSet[] initialCuts, KNNGraph knnGraph) {
        double[] costs = new double[initialCuts.length];
        if (knnGraph == null) {
            knnGraph = createKNNGraph(dataPoints);
        }
        for (int i = 0; i < initialCuts.length; i++) {
            List<Double> distances = knnGraph.getDistancesBetween(initialCuts[i]);
            for (int j = 0; j < distances.size(); j++) {
                costs[i] += Math.exp(-1.0*distances.get(j));
            }
            //costs[i] = distances.size();
            costs[i] /= initialCuts[i].size();
        }
        normalizeCosts(costs);
        //System.out.println(Arrays.toString(costs));
        return costs;
    }

    public void normalizeCosts(double[] costs) {
        double max = getMaxCost(costs);
        for (int i = 0; i < costs.length; i++) {
            costs[i] /= max <= 0.0 ? 1.0 : max;
        }
    }

    public double getMaxCost(double[] costs) {
        double max = -1.0;
        for (int i = 0; i < costs.length; i++) {
            max = Math.max(max, costs[i]);
        }
        return max;
    }

    //Creates a KNN graph for KNN cost function. Ensures that the same parameters are used everywhere.
    public KNNGraph createKNNGraph(double[][] dataPoints) {
        int k = Math.min(dataPoints.length-1, 15);
        return new KNNGraph(dataPoints, k);
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] shortestDistanceCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            double cost = Double.MAX_VALUE;
            for (int j = 0; j < dataPoints.length; j++) {
                if (initialCuts[i].get(j)) {
                    continue;
                }
                for (int k = 0; k < dataPoints.length; k++) {
                    if (!initialCuts[i].get(k)) {
                        continue;
                    }
                    cost = Math.min(getDistance(dataPoints[j], dataPoints[k]), cost);
                }
            }
            costs[i] = Math.exp(-1.0*cost);
        }
        normalizeCosts(costs);
        //cutCosts = costs;
        return costs;
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] pairwiseDistanceCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            double cost = 0;
            for (int j = 0; j < dataPoints.length; j++) {
                if (initialCuts[i].get(j)) {
                    continue;
                }
                for (int k = 0; k < dataPoints.length; k++) {
                    if (!initialCuts[i].get(k)) {
                        continue;
                    }
                    cost += Math.exp(-1.0*getDistance(dataPoints[j], dataPoints[k]));
                }
            }
            costs[i] = cost/(initialCuts[i].count()*(initialCuts[i].size()-initialCuts[i].count()));
        }
        normalizeCosts(costs);
        //cutCosts = costs;
        return costs;
    }

    //Pairwise distance cost function, which uses the sum of the pairwise distances of every pair on different sides of the cut.
    public double[] pairwiseClosestCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        //double threshold = 1.0;

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);
        for (int i = 0; i < initialCuts.length; i++) {
            double cost = 0;
            int size1 = initialCuts[i].count();
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
                cost += Math.exp(-1.0*closestDist);
            }
            costs[i] = cost/initialCuts[i].size();
        }
        normalizeCosts(costs);
        //cutCosts = costs;
        return costs;
    }

    //Distance to mean cost function, which uses the sum of the distance to the opposite side mean for every point (has linear time complexity).
    public double[] distanceToMeanCostFunction(double[][] dataPoints, BitSet[] initialCuts) {

        double[] costs = new double[initialCuts.length];
        double maxRange = getMaxRange(dataPoints);

        for (int i = 0; i < initialCuts.length; i++) {
            int cutCount = initialCuts[i].count();
            if (cutCount == 0 || cutCount == initialCuts[i].size()) {
                costs[i] = 1;
                continue;
            }
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
                double[] meanOther = initialCuts[i].get(j) ? mean1 : mean2;
                costs[i] += Math.exp(-1.0*(getDistance(dataPoints[j], mean) - getDistance(dataPoints[j], meanOther)));
                //costs[i] += Math.exp(-(1.0/maxRange)*getDistance(dataPoints[j], mean));
            }
            costs[i] /= initialCuts[i].size();
        }
        normalizeCosts(costs);
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
        /*double length = 0;
        for (int i = 0; i < point1.length; i++) {
            length += (point1[i]-point2[i])*(point1[i]-point2[i]);
        }
        return Math.sqrt(length);*/
        return Distance.euclidean().distance(point1, point2);
    }
}

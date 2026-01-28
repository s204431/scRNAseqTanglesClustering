package clustering;

import datasets.CostFunctions;
import datasets.ScRNAseqDataset;
import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.StaticArrayDatabase;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.projection.AffinityMatrixBuilder;
import elki.projection.BarnesHutTSNE;
import elki.projection.PerplexityAffinityMatrixBuilder;
import elki.utilities.random.RandomFactory;
import elki.distance.minkowski.EuclideanDistance;
import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import main.Main;
import util.*;
import smile.math.matrix.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static clustering.TangleClusterer.removeRedundantCuts;
import util.BitSet;
import visualization.test.TestProgressManager;


public class Model {
    private double[][] hvgData;
    private ScRNAseqDataset dataset;
    private int seed;
    private int[] groundTruth;
    private int[] shuffledGroundTruth;
    private double[][] softClustering;
    private int[] hardClustering;

    private TestSet runningTestSet;

    private Monitor monitor;

    private TangleClusterer tangleClusterer;

    public Model(Monitor monitor) {
        this.monitor = monitor;
        tangleClusterer = new TangleClusterer(monitor);
    }

    //Loads a data set from a file.
    public void loadDataset(String observedFilePath, int hvg, boolean normalizeData) {
        monitor.setFilePath(observedFilePath);

        Tuple<float[][], int[]> data = loadData(observedFilePath);
        float[][] originalData = data.x;
        groundTruth = data.y;

        shuffledGroundTruth = groundTruth.clone();
        Random r = new Random();
        seed = r.nextInt(Integer.MAX_VALUE);
        shuffleArray(originalData, seed);
        shuffleArray(shuffledGroundTruth, seed);

        if (normalizeData) logNormalize(originalData);

        int maxGenes = originalData[0].length;
        hvg = (hvg <= 0 || hvg >= maxGenes) ? maxGenes : hvg;
        hvgData = highlyVariableGenes(originalData, hvg);
        System.out.println("Finished loading data");

        dataset = new ScRNAseqDataset(hvgData, monitor);
        dataset.setSparsity(computeSparsity(originalData));
    }

    // Fisher-Yayes shuffle to limit space usage
    // https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
    public void shuffleArray(float[][] array, int seed) {
        Random rand = new Random(seed);
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            float[] temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public void shuffleArray(double[][] array, int seed) {
        Random rand = new Random(seed);
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            double[] temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public void shuffleArray(int[] array, int seed) {
        Random rand = new Random(seed);
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public int[] computeUnShuffledArray(int[] shuffledArray, int seed) {
        int n = shuffledArray.length;
        int[] unShuffledArray = new int[n];
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        shuffleArray(indices, seed);
        for (int i = 0; i < n; i++) {
            unShuffledArray[indices[i]] = shuffledArray[i];
        }
        return unShuffledArray;
    }

    public double[][] computeUnShuffledArray(double[][] shuffledArray, int seed) {
        int n = shuffledArray.length;
        double[][] unShuffledArray = new double[n][shuffledArray[0].length];
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        shuffleArray(indices, seed);
        for (int i = 0; i < n; i++) {
            unShuffledArray[indices[i]] = shuffledArray[i];
        }
        return unShuffledArray;
    }

    public float[][] computeUnShuffledArray(float[][] shuffledArray, int seed) {
        int n = shuffledArray.length;
        float[][] unShuffledArray = new float[n][shuffledArray[0].length];
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        shuffleArray(indices, seed);
        for (int i = 0; i < n; i++) {
            unShuffledArray[indices[i]] = shuffledArray[i];
        }
        return unShuffledArray;
    }

    public void runTestset() {
        TestSet testSet = new TestSet(this, "data/testset");
        testSet.run(10, true);
    }

    public void runTestset(File[] selectedFiles,
                           Config[] configs,
                           int runs,
                           boolean compareWithStandardPipeline,
                           TestProgressManager progressManager) {
        runningTestSet = new TestSet(this, selectedFiles);
        runningTestSet.runWIthUI(configs, runs, compareWithStandardPipeline, progressManager);
        runningTestSet = null;
    }

    public static double getDistance(double[] point1, double[] point2) {
        double length = 0;
        for (int i = 0; i < point1.length; i++) {
            length += (point1[i]-point2[i])*(point1[i]-point2[i]);
        }
        return Math.sqrt(length);
    }

    public static double silhouetteScore(double[][] data, int[] labels) {
        int n = data.length;

        //Precompute all distances
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = getDistance(data[i], data[j]);
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }

        //Group indices by cluster
        Map<Integer, List<Integer>> clusterMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusterMap.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(i);
        }

        double scoreSum = 0.0;

        for (int i = 0; i < n; i++) {
            int cluster = labels[i];
            List<Integer> sameCluster = clusterMap.get(cluster);

            //Intra-cluster average distance
            double a = 0.0;
            int sameClusterCount = 0;

            for (int j : sameCluster) {
                if (j != i) {
                    a += dist[i][j];
                    sameClusterCount++;
                }
            }
            if (sameClusterCount > 0) {
                a /= sameClusterCount;
            }

            //Minimum average distance to another cluster
            double b = Double.MAX_VALUE;

            for (var entry : clusterMap.entrySet()) {
                int otherCluster = entry.getKey();
                if (otherCluster == cluster) continue;

                List<Integer> members = entry.getValue();
                double distSum = 0.0;

                for (int idx : members) {
                    distSum += dist[i][idx];
                }

                double avg = distSum / members.size();
                if (avg < b) b = avg;
            }

            double s = (sameClusterCount == 0) ? 0 : (b - a) / Math.max(a, b);
            scoreSum += s;
        }

        return scoreSum / n;
    }


    public static double daviesBouldinIndex(double[][] data, int[] labels) {
        int k = -1;
        for (int i = 0; i < labels.length; i++) {
            k = Math.max(k, labels[i]);
        }
        k++; //Assuming zero-indexed labels

        double[][] centroids = new double[k][data[0].length];
        int[] counts = new int[k];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                centroids[labels[i]][j] += data[i][j];
            }
            counts[labels[i]]++;
        }
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < centroids[i].length; j++) {
                centroids[i][j] /= counts[i];
            }
        }

        double[] intraClusterDistances = new double[k];
        for (int i = 0; i < data.length; i++) {
            intraClusterDistances[labels[i]] += getDistance(data[i], centroids[labels[i]]);
        }
        for (int i = 0; i < k; i++) {
            intraClusterDistances[i] /= counts[i];
        }

        double dbi = 0.0;
        int nNonEmpty = 0;
        for (int i = 0; i < k; i++) {
            if (counts[i] == 0) {
                continue;
            }
            nNonEmpty++;
            double maxVal = -1;
            for (int j = 0; j < k; j++) {
                if (i != j && counts[j] > 0) {
                    double val = (intraClusterDistances[i] + intraClusterDistances[j])/getDistance(centroids[i], centroids[j]);
                    maxVal = Math.max(maxVal, val);
                }
            }
            dbi += maxVal;
        }
        dbi /= nNonEmpty;
        return dbi;
    }

    //Runs a clustering using a config file.
    public int[] cluster(ScRNAseqDataset dataset, Config config) {
        monitor.setDataset(dataset);
        monitor.setDimReductionTime(0);

        monitor.setClusterStartTime(System.currentTimeMillis());
        tangleClusterer.generateClusters(dataset, config);
        monitor.setClusterEndTime(System.currentTimeMillis());

        softClustering = tangleClusterer.getSoftClustering();
        hardClustering = tangleClusterer.getHardClustering();

        System.out.println("Dimensionality reduction time: " + monitor.getDimReductionTime());

        return hardClustering;
    }

    //Runs using a config file when a performance metric is used (grid search, split pruning).
    public int[] clusterWithPerformanceMetric(ScRNAseqDataset dataset, Config config) {
        monitor.setClusterStartTime(System.currentTimeMillis());
        monitor.setDimReductionTime(0);

        int maxClusters = config.getMaxClusters();
        int minA = Math.max((int)((dataset.data.length/(double)maxClusters)*0.55), 1);

        double[][] reducedPoints;
        if (config.isUsePcaCostFunction()) {
            long startTime = System.currentTimeMillis();
            reducedPoints = svdWithElbow(dataset.data);
            monitor.addDimReductionTime(System.currentTimeMillis() - startTime);
        }
        else {
            long startTime = System.currentTimeMillis();
            reducedPoints = tsne(dataset.data, config.getTsneComponentsCostFunction());
            monitor.addDimReductionTime(System.currentTimeMillis() - startTime);
        }
        reducedPoints = Main.zScoreNorm(reducedPoints);

        dataset.setA(minA);
        CostFunctions costFunctions = new CostFunctions(monitor);
        if (config.isUseCache() && config.getHighLevelCostFunctionName().equals(GlobalConstants.HIGH_LEVEL_COST_FUNCTION_NORMAL)) {
            costFunctions.reducedPoints = new ArrayList<>();
            costFunctions.reducedPoints.add(reducedPoints);
            BitSet mask = new BitSet(reducedPoints.length);
            mask.setAll();
            costFunctions.setMask(mask);
            if (config.getLowLevelCostFunctionName().equals(GlobalConstants.LOW_LEVEL_COST_FUNCTION_KNN)) {
                costFunctions.cachedKNNGraphs = new ArrayList<>();
                costFunctions.cachedKNNGraphs.add(costFunctions.createKNNGraph(reducedPoints));
            }
        }
        dataset.setCostFunctions(costFunctions);
        monitor.setDataset(dataset);

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


        if (config.isRemoveRedundantCuts()) {
            Tuple<BitSet[], double[]> redundancyRemoved = removeRedundantCuts(initialCuts, costs, config.getRedundancyFactor()); //Set factor to 1 to turn it off.
            initialCuts = redundancyRemoved.x;
            costs = redundancyRemoved.y;
        }

        boolean useSilhouette = config.getPerformanceMetric().equals(GlobalConstants.PERFORMANCE_METRIC_SIL);
        double[][] bestSoftClustering = null;
        int[] bestHardClustering = null;
        double bestScore = useSilhouette ? -1 : Integer.MAX_VALUE;
        int bestA = -1;
        double bestPsi = -1;
        TangleSearchTree[] bestTrees = null;
        long metricTime = 0;

        double maxPsi = config.isUseSplitPruning() ? 0.0 : 0.96;
        int minClusters = (config.isTuneParameters()) ? 2 : maxClusters;

        int run = 1;
        for (int nClusters = minClusters; nClusters <= maxClusters; nClusters++) {
            int a2 = Math.max((int)((dataset.data.length/(double)nClusters)*0.55), 1);
            config.setA(a2);
            for (double psi = 0; psi <= maxPsi; psi += 0.05) {
                System.out.println("Run " + run + " n = " + nClusters + " psi = " + psi);
                run++;

                config.setPsi(psi);

                if (psi == 0) {
                    tangleClusterer.generateClusters(dataset, config, initialCuts, costs, costFunctions, reducedPoints);
                }
                else {
                    tangleClusterer.clusterWithNewPsi(psi);
                }

                softClustering = tangleClusterer.getSoftClustering();
                hardClustering = tangleClusterer.getHardClustering();
                //double NMIScore = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
                //double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);

                //System.out.println(NMIScore);
                //System.out.println(randIndex);
                long startTime = System.currentTimeMillis();

                if (useSilhouette) {
                    double silhouetteScore = silhouetteScore(reducedPoints, hardClustering);
                    metricTime += (System.currentTimeMillis() - startTime);

                    if (silhouetteScore < 1.0 && silhouetteScore > bestScore) {
                        bestScore = silhouetteScore;
                        bestSoftClustering = softClustering;
                        bestHardClustering = hardClustering;
                        bestA = a2;
                        bestPsi = psi;
                        bestTrees = new TangleSearchTree[]{monitor.getUncondensedTree(), monitor.getSplitPrunedTree(), monitor.getCondensedTree()};
                    }
                    //System.out.println("Silhouette score: " + silhouetteScore);
                } else {
                    double dbi = daviesBouldinIndex(reducedPoints, hardClustering);
                    metricTime += (System.currentTimeMillis() - startTime);

                    if (Double.isFinite(dbi) && dbi > 0 && dbi < bestScore) {
                        bestScore = dbi;
                        bestSoftClustering = softClustering;
                        bestHardClustering = hardClustering;
                        bestA = a2;
                        bestPsi = psi;
                        bestTrees = new TangleSearchTree[]{monitor.getUncondensedTree(), monitor.getSplitPrunedTree(), monitor.getCondensedTree()};
                    }
                    //System.out.println("Davies-Boldin Index: " + dbi);
                }
            }
        }

        monitor.setSilhouetteTime(metricTime);
        monitor.setClusterEndTime(System.currentTimeMillis());
        monitor.setUncondensedTree(bestTrees[0]);
        monitor.setSplitPrunedTree(bestTrees[1]);
        monitor.setCondensedTree(bestTrees[2]);

        softClustering = bestSoftClustering;
        hardClustering = bestHardClustering;

        //double NMIScore = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
        //double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);

        System.out.println("Best a: " + bestA);
        System.out.println("Best psi: " + bestPsi);
        System.out.println("Best score: " + bestScore);
        //System.out.println(NMIScore);
        //System.out.println(randIndex);
        System.out.println("Dimensionality reduction time: " + monitor.getDimReductionTime());
        System.out.println("Silhouette time: " + monitor.getSilhouetteTime());
        return hardClustering;
    }

    //PCA dimensionality reduction with SVD.
    public static Tuple<double[][], double[]> svd(double[][] data, int nComponents) {
        int n = data.length;
        int d = data[0].length;

        //Center data
        double[] mean = new double[d];
        for (int j = 0; j < d; j++) {
            double sum = 0.0;
            for (int i = 0; i < n; i++) sum += data[i][j];
            mean[j] = sum / n;
        }

        double[][] centered = new double[n][d];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < d; j++)
                centered[i][j] = data[i][j] - mean[j];

        //SVD
        Matrix X = Matrix.of(centered);
        Matrix.SVD svd = X.svd();

        double[] singularValues = svd.s;
        int k = Math.min(nComponents, singularValues.length);

        //Take first k columns of V
        Matrix V = svd.V;
        double[][] V_k_array = new double[X.ncol()][k];
        for (int i = 0; i < X.ncol(); i++) {
            for (int j = 0; j < k; j++) {
                V_k_array[i][j] = V.get(i, j);
            }
        }
        Matrix V_k = Matrix.of(V_k_array);
        Matrix projectedData = X.mm(V_k);

        //Compute variance explained
        double[] eigenvalues = new double[singularValues.length];
        double totalVariance = 0.0;
        for (int i = 0; i < singularValues.length; i++) {
            double lambda = (singularValues[i] * singularValues[i]) / (n - 1);
            eigenvalues[i] = lambda;
            totalVariance += lambda;
        }

        double[] explainedVarianceRatio = new double[k];
        for (int i = 0; i < k; i++) {
            explainedVarianceRatio[i] = eigenvalues[i] / totalVariance;
        }

        return new Tuple<>(projectedData.toArray(), explainedVarianceRatio);
    }

    public static int findElbow(double[] evr) {
        int n = evr.length;
        if (n < 3) return n;

        //Line from first to last point
        double x1 = 0, y1 = evr[0];
        double x2 = n - 1, y2 = evr[n - 1];

        double maxDist = -1.0;
        int elbowIndex = 0;

        for (int i = 1; i < n - 1; i++) {
            double x0 = i;
            double y0 = evr[i];

            //Distance from point to line
            double num = Math.abs((y2 - y1)*x0 - (x2 - x1)*y0 + x2*y1 - y2*x1);
            double den = Math.sqrt((y2 - y1)*(y2 - y1) + (x2 - x1)*(x2 - x1));
            double dist = num/den;

            if (dist > maxDist) {
                maxDist = dist;
                elbowIndex = i;
            }
        }

        return elbowIndex + 1;
    }

    //Reduces using PCA with knee detection.
    public static double[][] svdWithElbow(double[][] data) {
        Tuple<double[][], double[]> pcaResult = Model.svd(data, GlobalConstants.MAX_PCS_COMPONENTS);
        double[][] projectedData = pcaResult.x;
        double[] varianceRatios = pcaResult.y;
        int pcaComponents = Model.findElbow(varianceRatios);
        System.out.println("Elbow found at: " + pcaComponents + " components");
        double[][] reducedPoints = new double[projectedData.length][pcaComponents];
        for (int i = 0; i < projectedData.length; i++) {
            System.arraycopy(projectedData[i], 0, reducedPoints[i], 0, pcaComponents);
        }
        return reducedPoints;
    }

    //t-SNE dimensionality reduction.
    public static double[][] tsne(double[][] data, int nComponents) {
        long time = System.currentTimeMillis();

        int initialDims = data[0].length;
        double perplexity = 20.0;
        int maxIterations = 100;

        //Wrap raw data into ELKI database
        Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data), null);
        db.initialize();
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

        AffinityMatrixBuilder<DoubleVector> affinity =
                new PerplexityAffinityMatrixBuilder<>(EuclideanDistance.STATIC, perplexity);

        BarnesHutTSNE<DoubleVector> tsne = new BarnesHutTSNE<>(
                affinity,
                nComponents,
                0.8,
                200.0,
                maxIterations,
                RandomFactory.DEFAULT,
                false,
                0.5
        );

        //Run algorithm
        Relation<DoubleVector> projected = tsne.run(db, rel);

        //Collect results
        List<double[]> resultList = new ArrayList<>();
        for (DBIDIter iter = projected.getDBIDs().iter(); iter.valid(); iter.advance()) {
            DoubleVector vec = projected.get(iter);
            double[] coords = new double[nComponents];
            for (int j = 0; j < nComponents; j++) {
                coords[j] = vec.doubleValue(j);
            }
            resultList.add(coords);
        }

        double[][] output = new double[resultList.size()][nComponents];
        for (int i = 0; i < output.length; i++) {
            output[i] = resultList.get(i);
        }

        //System.out.println("TSNE time: " + (System.currentTimeMillis() - time));

        return output;
    }

    //Loads data from a file path.
    public Tuple<float[][], int[]> loadData(String observedFilePath) {
        String labelFilePath = "";
        if (observedFilePath.contains("observed_counts")) labelFilePath = observedFilePath.replace("observed_counts", "labels");
        else labelFilePath = observedFilePath.replace("obs", "labels");

        if (observedFilePath.endsWith(".csv")) {
            return new Tuple<>(readCSV(observedFilePath), loadGroundTruthCSV(labelFilePath));
        }
        else if (observedFilePath.endsWith(".h5ad")) {
            return readH5AD(observedFilePath);
        }
        System.out.println("File type not supported");
        return null;
    }

    //Loads ground truth from a CSV file.
    public int[] loadGroundTruthCSV(String filePath) {
        float[][] temp = readCSV(filePath);
        if (temp == null) {
            return null;
        }
        int[] gt = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            gt[i] = (int)temp[i][0];
        }
        return gt;
    }

    //Filter genes by only keeping a number of highly variable genes.
    public double[][] highlyVariableGenes(float[][] data, int nTopGenes) {
        double[] dispersions = new double[data[0].length];
        double[] means = new double[data[0].length];
        Integer[] indices = new Integer[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double sum = 0.0;
            for (int j = 0; j < data.length; j++) {
                sum += data[j][i];
            }
            double mean = sum/data.length;
            double varSum = 0.0;
            for (int j = 0; j < data.length; j++) {
                varSum += (data[j][i] - mean)*(data[j][i] - mean);
            }
            double variance = varSum/data.length;
            dispersions[i] = mean == 0.0 ? 0.0 : Math.log(variance/mean);
            means[i] = Math.log(1+mean);
            indices[i] = i;
        }

        Arrays.sort(indices, Comparator.comparingDouble(a -> means[a]));

        int nBins = 20;
        int binSize = Math.max(1, data[0].length / nBins);

        double[] binMeanDispersions = new double[nBins];
        double[] binStdDispersions = new double[nBins];
        double[] zScores = new double[data[0].length];

        for (int b = 0; b < nBins; b++) {
            int start = b*binSize;
            int end = (b == nBins - 1) ? data[0].length : start + binSize;

            for (int i = start; i < end; i++) {
                binMeanDispersions[b] += dispersions[indices[i]];
            }
            binMeanDispersions[b] /= (end - start);

            for (int i = start; i < end; i++) {
                int index = indices[i];
                binStdDispersions[b] += (dispersions[index] - binMeanDispersions[b])*(dispersions[index] - binMeanDispersions[b]);
            }
            binStdDispersions[b] = Math.sqrt(binStdDispersions[b]/(end-start));

            for (int i = start; i < end; i++) {
                int index = indices[i];
                if (binStdDispersions[b] == 0.0) {
                    zScores[index] = 0.0;
                }
                else {
                    zScores[index] = (dispersions[index] - binMeanDispersions[b])/binStdDispersions[b];
                }
            }
        }

        indices = new Integer[data[0].length];
        for (int i = 0; i < data[0].length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(a -> -zScores[a]));
        double[][] newData = new double[data.length][nTopGenes];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < nTopGenes; j++) {
                newData[i][j] = data[i][indices[j]];
            }
        }
        return newData;
    }

    //Log normalizes the data (modifies the input).
    public float[][] logNormalize(float[][] data) {
        int nZeros = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = (float)Math.log(1.0 + data[i][j]);
                if (data[i][j] == 0.0) {
                    nZeros++;
                }
            }
        }
        System.out.println("Sparsity: " + ((double)nZeros)/(data.length*data[0].length));
        //System.out.println("Dimension: " + normalized.length + " " + normalized[0].length);
        return data;
    }

    //Reads a CSV file.
    public float[][] readCSV(String filePath) {
        ArrayList<float[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            // Skip first row
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] stringValues = line.split(",");

                // Skip first value in the row
                if (stringValues.length <= 1) continue; // skip row if no data after first value

                float[] values = new float[stringValues.length - 1];
                for (int i = 1; i < stringValues.length; i++) { // start from index 1
                    String cleaned = stringValues[i].replaceAll("\"", "").trim();

                    if (cleaned.isEmpty()) {
                        values[i - 1] = 0; // empty cell → 0
                    } else {
                        values[i - 1] = Float.parseFloat(cleaned);
                    }
                }
                rows.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number in CSV: " + e.getMessage());
            return null;
        }

        // Convert ArrayList<int[]> to int[][]
        float[][] data = new float[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }

        return data;
    }

    //Loads from an h5ad file.
    public Tuple<float[][], int[]> readH5AD(String filePath) {
        File file = new File(filePath);
        try (HdfFile hdfFile = new HdfFile(file)) {
            Group xGroup = (Group) hdfFile.getChildren().get("X");
            /*for (Map.Entry<String, Node> entry : xGroup.getChildren().entrySet()) {
                System.out.println("Key: " + entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
            }*/

            float[] data;
            if (((Dataset) xGroup.getChildren().get("data")).getJavaType() == int.class) {
                int[] intData = (int[]) ((Dataset) xGroup.getChildren().get("data")).getData();
                data = new float[intData.length];
                for (int i = 0; i < data.length; i++) {
                    data[i] = intData[i];
                }
            }
            else {
                data = (float[]) ((Dataset) xGroup.getChildren().get("data")).getData();
            }
            int[] indices = (int[]) ((Dataset) xGroup.getChildren().get("indices")).getData();
            int[] indptr = (int[]) ((Dataset) xGroup.getChildren().get("indptr")).getData();

            int nRows = indptr.length - 1;
            int nCols = Arrays.stream(indices).max().orElse(-1) + 1;

            float[][] dense = new float[nRows][nCols];

            for (int row = 0; row < nRows; row++) {
                int start = indptr[row];
                int end = indptr[row + 1];
                for (int i = start; i < end; i++) {
                    int col = indices[i];
                    dense[row][col] = data[i];
                }
            }

            int[] groundTruth = null;
            try {
                byte[] codes = (byte[]) hdfFile.getDatasetByPath("obs/cell_ontology_class/codes").getData();
                groundTruth = new int[codes.length];
                for (int i = 0; i < codes.length; i++) {
                    groundTruth[i] = codes[i];
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return new Tuple<>(dense, groundTruth);
        }
    }

    public BitSet[] getCuts() {
        return dataset.getLastCuts();
    }

    public double[] getCutCosts() {
        return dataset.getLastCosts();
    }

    public int[] getGroundTruth() {
        return groundTruth;
    }

    public int[] getShuffledGroundTruth() {
        return shuffledGroundTruth;
    }

    public double[][] getHvgData() {
        return hvgData;
    }

    public int[] getHardClustering() {
        return hardClustering;
    }

    public ScRNAseqDataset getDataset() {
        return dataset;
    }

    public int getSeed() {
        return seed;
    }

    public double[][] getSoftClustering() {
        return softClustering;
    }

    //Compute sparsity (percent zeros) in the data.
    public double computeSparsity(float[][] originalData) {
        int nZeros = 0;
        for (int i = 0; i < originalData.length; i++) {
            for (int j = 0; j < originalData[i].length; j++) {
                if (originalData[i][j] == 0.0) {
                    nZeros++;
                }
            }
        }
        return ((double)nZeros)/(originalData.length*originalData[0].length);
    }

    public Monitor getMonitor() {
        return monitor;
    }
}

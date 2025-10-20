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
import util.Monitor;
import smile.feature.extraction.PCA;
import smile.manifold.UMAP;
import smile.math.matrix.Matrix;
import smile.validation.metric.NormalizedMutualInformation;
import smile.validation.metric.AdjustedRandIndex;
import util.BitSet;
import util.Config;
import util.TestSet;
import util.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static clustering.TangleClusterer.removeRedundantCuts;


import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.lossfunctions.LossFunctions;
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

    private TangleClusterer tangleClusterer = new TangleClusterer();

    public Model() {
        //loadDataset("data/symsim_observed_counts_5000genes_1000cells_complex.csv");
    }

    public void loadDataset(String observedFilePath, int hvg) {
        monitor.setFilePath(observedFilePath);

        String labelFilePath = observedFilePath.replace("observed_counts", "labels");

        Tuple<float[][], int[]> data = loadData(observedFilePath, labelFilePath);
        float[][] originalData = data.x;
        groundTruth = data.y;
        shuffledGroundTruth = groundTruth.clone();

        Random r = new Random();
        seed = r.nextInt();
        shuffleArray(originalData, seed);
        shuffleArray(shuffledGroundTruth, seed);

        logNormalize(originalData);

        int maxGenes = originalData[0].length;
        hvg = (hvg <= 0 || hvg >= maxGenes) ? maxGenes : hvg;
        hvgData = highlyVariableGenes(originalData, hvg);
        System.out.println("Finished loading data");
        /*double[][] newHvgData = new double[hvgData.length][2];
        for (int i = 0; i < hvgData.length; i++) {
            newHvgData[i][0] = hvgData[i][4];
            newHvgData[i][1] = hvgData[i][5];
        }
        hvgData = newHvgData;*/


        //projectedData = tsne(hvgData, 2);

        dataset = new ScRNAseqDataset(hvgData);
        //cluster(dataset, 70, 0, "Range", "Distance To Mean");


        /*Tuple<int[], Integer> pythonResult = runPython(observedFilePath);
        double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, pythonResult.x);
        System.out.println("NMI python: " + NMIPython);
        System.out.println("Rand index python: " + randIndex);*/

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
        double[] silhouettes = new double[n];

        for (int i = 0; i < n; i++) {
            double[] point = data[i];
            int cluster = labels[i];

            double a = 0.0;
            int sameClusterCount = 0;
            for (int j = 0; j < n; j++) {
                if (i != j && labels[j] == cluster) {
                    a += getDistance(point, data[j]);
                    sameClusterCount++;
                }
            }
            if (sameClusterCount > 0) {
                a /= sameClusterCount;
            }

            double b = Double.MAX_VALUE;
            Map<Integer, List<Integer>> clusterMembers = new HashMap<>();

            for (int j = 0; j < n; j++) {
                if (labels[j] != cluster) {
                    clusterMembers.computeIfAbsent(labels[j], k -> new ArrayList<>()).add(j);
                }
            }

            for (int otherCluster : clusterMembers.keySet()) {
                double distSum = 0.0;
                List<Integer> members = clusterMembers.get(otherCluster);
                for (int idx : members) {
                    distSum += getDistance(point, data[idx]);
                }
                double avgDist = distSum / members.size();
                b = Math.min(b, avgDist);
            }

            double s;
            if (sameClusterCount == 0) {
                s = 0;
            } else {
                s = (b - a) / Math.max(a, b);
            }
            silhouettes[i] = s;
        }

        double total = 0.0;
        for (double s : silhouettes) {
            total += s;
        }
        return total / n;
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

    public void cluster(ScRNAseqDataset dataset, Config config) {
        monitor.setDataset(dataset);

        boolean prev0 = TangleClusterer.earlyStop;
        boolean prev1 = tangleClusterer.useAlternateConsistencyCheck;
        boolean prev2 = tangleClusterer.useOscarWerner;
        boolean prev3 = tangleClusterer.useSplitFirst;
        boolean prev4 = tangleClusterer.autoLimitSplitCosts;
        boolean prev5 = tangleClusterer.removeRedundantCuts;

        TangleClusterer.earlyStop = config.isUseEarlyStop();
        tangleClusterer.useAlternateConsistencyCheck = config.isUseAlternateConsistencyCheck();
        tangleClusterer.useOscarWerner = config.isUseWernerModification();
        tangleClusterer.useSplitFirst = config.isUseSplitFirst();
        tangleClusterer.autoLimitSplitCosts = config.isAutoComputePsi();
        tangleClusterer.removeRedundantCuts = config.isRemoveRedundant();

        tangleClusterer.generateClusters(dataset, config);

        TangleClusterer.earlyStop = prev0;
        tangleClusterer.useAlternateConsistencyCheck = prev1;
        tangleClusterer.useOscarWerner = prev2;
        tangleClusterer.useSplitFirst = prev3;
        tangleClusterer.autoLimitSplitCosts = prev4;
        tangleClusterer.removeRedundantCuts = prev5;

        softClustering = tangleClusterer.getSoftClustering();
        hardClustering = tangleClusterer.getHardClustering();
        double NMIScore = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
        double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);
        System.out.println(NMIScore);
        System.out.println(randIndex);
    }

    public int[] clusterAndReturn(ScRNAseqDataset dataset, Config config) {
        monitor.setDataset(dataset);

        boolean prev0 = TangleClusterer.earlyStop;
        boolean prev1 = tangleClusterer.useAlternateConsistencyCheck;
        boolean prev2 = tangleClusterer.useOscarWerner;
        boolean prev3 = tangleClusterer.useSplitFirst;
        boolean prev4 = tangleClusterer.autoLimitSplitCosts;
        boolean prev5 = tangleClusterer.removeRedundantCuts;

        TangleClusterer.earlyStop = config.isUseEarlyStop();
        tangleClusterer.useAlternateConsistencyCheck = config.isUseAlternateConsistencyCheck();
        tangleClusterer.useOscarWerner = config.isUseWernerModification();
        tangleClusterer.useSplitFirst = config.isUseSplitFirst();
        tangleClusterer.autoLimitSplitCosts = config.isAutoComputePsi();
        tangleClusterer.removeRedundantCuts = config.isRemoveRedundant();

        tangleClusterer.generateClusters(dataset, config);

        TangleClusterer.earlyStop = prev0;
        tangleClusterer.useAlternateConsistencyCheck = prev1;
        tangleClusterer.useOscarWerner = prev2;
        tangleClusterer.useSplitFirst = prev3;
        tangleClusterer.autoLimitSplitCosts = prev4;
        tangleClusterer.removeRedundantCuts = prev5;

        softClustering = tangleClusterer.getSoftClustering();
        hardClustering = tangleClusterer.getHardClustering();

        return hardClustering;
    }

    public int[] clusterAuto(ScRNAseqDataset dataset, Config config) {
        String initialCutsGenerator = config.getCutGeneratorName();
        String highLevelCostFunctionName = config.getHighLevelCostFunctionName();
        String lowLevelCostFunctionName = config.getLowLevelCostFunctionName();
        boolean useCache = config.isUseCache();
        int splitSize = config.getSplitSize();
        int tsneComponents = config.getTsneComponents();
        boolean useFastVersion = config.isUseFastVersion();

        int maxClusters = 10;

        int minA = Math.max((int)((dataset.data.length/(double)maxClusters)*0.667), 1);


        double[][] reducedPoints;

        if (config.isUseFastVersion()) {
            reducedPoints = svd(dataset.data, tsneComponents);
        }
        else {
            reducedPoints = tsne(dataset.data, tsneComponents);
        }

        dataset.setA(minA);
        CostFunctions costFunctions = new CostFunctions();
        dataset.setCostFunctions(costFunctions);
        BitSet[] initialCuts = dataset.getInitialCuts(initialCutsGenerator, useFastVersion);
        double[] costs = dataset.getCutCosts(highLevelCostFunctionName, lowLevelCostFunctionName, useCache, splitSize, tsneComponents, useFastVersion);
        Tuple<BitSet[], double[]> redundancyRemoved = removeRedundantCuts(initialCuts, costs, 0.9); //Set factor to 1 to turn it off.
        initialCuts = redundancyRemoved.x;
        costs = redundancyRemoved.y;
        monitor.setDataset(dataset);

        double[][] bestSoftClustering = null;
        int[] bestHardClustering = null;
        double bestSilhuetteScore = -1;
        int bestA = -1;
        double bestPsi = -1;

        for (double psi = 0; psi <= 1; psi += 0.05) {
            for (int nClusters = 2; nClusters <= maxClusters; nClusters++) {
                int a2 = Math.max((int)((dataset.data.length/(double)nClusters)*0.667), 1);
                config.setA(a2);
                config.setPsi(psi);
                tangleClusterer.generateClusters(dataset, config, initialCuts, costs, costFunctions);
                softClustering = tangleClusterer.getSoftClustering();
                hardClustering = tangleClusterer.getHardClustering();
                //double NMIScore = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
                //double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);

                //System.out.println(NMIScore);
                //System.out.println(randIndex);
                double silhouetteScore = silhouetteScore(reducedPoints, hardClustering);
                if (silhouetteScore < 1.0 && silhouetteScore > bestSilhuetteScore) {
                    bestSilhuetteScore = silhouetteScore;
                    bestSoftClustering = softClustering;
                    bestHardClustering = hardClustering;
                    bestA = a2;
                    bestPsi = psi;
                }
                //System.out.println(silhouetteScore);
            }
        }

        softClustering = bestSoftClustering;
        hardClustering = bestHardClustering;

        //double NMIScore = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
        //double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);

        System.out.println("Best a: " + bestA);
        System.out.println("Best psi: " + bestPsi);
        //System.out.println(NMIScore);
        //System.out.println(randIndex);
        return hardClustering;
    }

    public static double[][] pca(double[][] data, int nComponents) {
        PCA pca = PCA.cor(data);
        return pca.getProjection(nComponents).apply(data);
    }

    public static double[][] svd(double[][] data, int nComponents) {
        int n = data.length;
        int d = data[0].length;

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

        Matrix X = Matrix.of(centered);
        Matrix.SVD svd = X.svd();

        Matrix V = svd.V;
        double[][] V_k_array = new double[X.ncol()][nComponents];
        for (int i = 0; i < X.ncol(); i++) {
            for (int j = 0; j < nComponents; j++) {
                V_k_array[i][j] = V.get(i, j);
            }
        }
        Matrix V_k = Matrix.of(V_k_array);

        Matrix projectedData = X.mm(V_k);
        return projectedData.toArray();
    }

    public static double[][] tsne(double[][] data, int nComponents) {
        long time = System.currentTimeMillis();

        int initialDims = data[0].length;
        double perplexity = 20.0;
        int maxIterations = 100;
        /*BarnesHutTSne tsne = new BHTSne();
        TSneConfiguration config = TSneUtils.buildConfig(data, nComponents, initialDims, perplexity, maxIterations);

        double[][] output = tsne.tsne(config);
        System.out.println("TSNE time: " + (System.currentTimeMillis() - time));
        return output;*/


        // Wrap raw data into ELKI database
        Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data), null);
        db.initialize();
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

        // Affinity matrix builder (perplexity 30, Euclidean distance)
        AffinityMatrixBuilder<DoubleVector> affinity =
                new PerplexityAffinityMatrixBuilder<>(EuclideanDistance.STATIC, perplexity);

        // Construct Barnes-Hut t-SNE
        BarnesHutTSNE<DoubleVector> tsne = new BarnesHutTSNE<>(
                affinity,
                nComponents,
                0.8,              // finalMomentum
                200.0,            // learningRate
                maxIterations,             // maxIterations
                RandomFactory.DEFAULT,
                false,            // keep original data
                0.5               // theta (Barnes-Hut approximation)
        );

        // Run algorithm
        Relation<DoubleVector> projected = tsne.run(db, rel);

        // Collect results using DBIDIter
        List<double[]> resultList = new ArrayList<>();
        for (DBIDIter iter = projected.getDBIDs().iter(); iter.valid(); iter.advance()) {
            DoubleVector vec = projected.get(iter);
            double[] coords = new double[nComponents];
            for (int j = 0; j < nComponents; j++) {
                coords[j] = vec.doubleValue(j);
            }
            resultList.add(coords);
        }

        // Convert list to array
        double[][] output = new double[resultList.size()][nComponents];
        for (int i = 0; i < output.length; i++) {
            output[i] = resultList.get(i);
        }

        //System.out.println("TSNE time: " + (System.currentTimeMillis() - time));

        return output;
    }

    public static double[][] umap(double[][] data, int nComponents) {
        return UMAP.fit(data, new UMAP.Options(2, nComponents, 200, 1, 0.1, 1.0, 5, 1.0, 2));
    }

    public static double[][] vae(double[][] data, int nComponents) {

        INDArray input = Nd4j.create(data);

        MultiLayerNetwork model = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
                .seed(123)
                .list()

                .layer(0, new VariationalAutoencoder.Builder()
                        .nIn(data[0].length)
                        .nOut(100)
                        .activation(Activation.RELU)
                        .build())

                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(100)
                        .nOut(data[0].length)
                        .activation(Activation.SIGMOID)
                        .build())
                .build());

        model.init();
        model.setListeners(new ScoreIterationListener(10));

        DataSet ds = new DataSet(input, input);

        for (int i = 0; i < 1000; i++) {
            model.fit(ds);
        }

        INDArray encoded = model.feedForwardToLayer(0, input, false).get(1);

        return tsne(encoded.toDoubleMatrix(), nComponents);
    }

    public Tuple<float[][], int[]> loadData(String observedFilePath, String labelsFilePath) {
        if (observedFilePath.endsWith(".csv")) {
            return new Tuple<>(readCSV(observedFilePath), loadGroundTruthCSV(labelsFilePath));
        }
        else if (observedFilePath.endsWith(".h5ad")) {
            return readH5AD(observedFilePath);
        }
        System.out.println("File type not supported");
        return null;
    }

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

    public double[][] highlyVariableGenes(float[][] data, int nTopGenes) {
        int nGenes = data[0].length;
        int nCells = data.length;

        double[] dispersions = new double[nGenes];

        for (int g = 0; g < nGenes; g++) {
            double sum = 0.0;
            for (int c = 0; c < nCells; c++) {
                sum += data[c][g];
            }
            double mean = sum / nCells;

            double sqDiff = 0.0;
            for (int c = 0; c < nCells; c++) {
                sqDiff += Math.pow(data[c][g] - mean, 2);
            }
            double variance = sqDiff / (nCells - 1);

            dispersions[g] = mean > 0 ? -variance / mean : 0.0;
        }

        // Get indices sorted by dispersion (descending)
        Integer[] indices = new Integer[nGenes];
        for (int i = 0; i < nGenes; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(a -> dispersions[a]));

        // Take top nTopGenes
        int[] indc =  Arrays.stream(indices)
                .limit(nTopGenes)
                .mapToInt(Integer::intValue)
                .toArray();

        double[][] newData = new double[nCells][nTopGenes];
        for (int i = 0; i < nTopGenes; i++) {
            for (int j = 0; j < nCells; j++) {
                newData[j][i] = data[j][indc[i]];
            }
        }
        //System.out.println("Dimension after HVG: " + newData.length + " " + newData[0].length);
        return newData;
    }

    public double[][] highlyVariableGenes2(float[][] data, int nTopGenes) {
        double[] dispersions = new double[data[0].length];
        double[] means = new double[data[0].length];
        Integer[] indices = new Integer[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double sum = 0.0;
            for (int j = 0; j < data.length; j++) {
                sum += data[j][i];
            }
            double mean = sum/data.length;
            if (mean == 0) {
                mean = 0.000000001;
            }
            double varSum = 0.0;
            for (int j = 0; j < data.length; j++) {
                varSum += (data[j][i] - mean)*(data[j][i] - mean);
            }
            double variance = varSum/(data.length - 1);
            dispersions[i] = Math.log(variance/mean);
            means[i] = Math.log(1+mean);
            indices[i] = i;
        }

        Arrays.sort(indices, Comparator.comparingDouble(a -> means[a]));

        int nBins = 20;
        int currentBin = 0;
        int currentBinSize = 0;
        int[] binIndices = new int[indices.length];

        int binSize = data[0].length/nBins;
        int leftOver = data[0].length%nBins;

        for (int i = 0; i < indices.length; i++) {
            if (currentBinSize >= (binSize + (leftOver > 0 ? 1 : 0))) {
                currentBinSize = 0;
                currentBin++;
                leftOver--;
            }
            binIndices[i] = currentBin;
            currentBinSize++;
        }

        leftOver = data[0].length%nBins;

        double[] binMeanDispersions = new double[nBins];
        for (int i = 0; i < indices.length; i++) {
            binMeanDispersions[binIndices[i]] += dispersions[indices[i]];
        }

        for (int i = 0; i < binMeanDispersions.length; i++) {
            binMeanDispersions[i] /= binSize + (i < leftOver ? 1 : 0);
        }

        double[] binStdDispersions = new double[nBins];
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            int binIndex = binIndices[i];
            binStdDispersions[binIndex] += (dispersions[index] - binMeanDispersions[binIndex])*(dispersions[index] - binMeanDispersions[binIndex]);
        }

        for (int i = 0; i < binStdDispersions.length; i++) {
            binStdDispersions[i] /= (binSize + (i < leftOver ? 1 : 0) - 1);
            binStdDispersions[i] = Math.sqrt(binStdDispersions[i]);
        }

        double[] zScores = new double[data[0].length];

        for (int i = 0; i < data[0].length; i++) {
            zScores[i] = (dispersions[i] - binMeanDispersions[binIndices[indices[i]]])/binStdDispersions[binIndices[indices[i]]];
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
        //System.out.println("Sparsity: " + ((double)nZeros)/(normalized.length*normalized[0].length));
        //System.out.println("Dimension: " + normalized.length + " " + normalized[0].length);
        return data;
    }

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

    public Tuple<float[][], int[]> readH5AD(String filePath) {
        File file = new File(filePath);
        try (HdfFile hdfFile = new HdfFile(file)) {
            Group xGroup = (Group) hdfFile.getChildren().get("X");
            /*for (Map.Entry<String, Node> entry : xGroup.getChildren().entrySet()) {
                System.out.println("Key: " + entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
            }*/

            float[] data = (float[]) ((Dataset) xGroup.getChildren().get("data")).getData();
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

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
        tangleClusterer.setMonitor(monitor);
    }

    public double[][] getSoftClustering() {
        return softClustering;
    }

}

package clustering;

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
import visualization.test.TestEditPanel;


public class Model {
    private double[][] originalData;
    private double[][] normalizedData;
    private double[][] hvgData;
    private double[][] projectedData;
    private ScRNAseqDataset dataset;
    private int[] groundTruth;
    private int[] hardClustering;

    private Monitor monitor;

    private TangleClusterer tangleClusterer = new TangleClusterer();

    public Model() {
        //loadDataset("data/symsim_observed_counts_5000genes_1000cells_complex.csv");
    }

    public void loadDataset(String observedFilePath, int hvg) {
        String labelFilePath = observedFilePath.replace("observed_counts", "labels");

        originalData = loadData(observedFilePath);
        groundTruth = loadGroundTruth(labelFilePath);
        normalizedData = logNormalize(originalData);

        int maxGenes = normalizedData[0].length;
        hvg = (hvg <= 0 || hvg >= maxGenes) ? maxGenes : hvg;
        hvgData = highlyVariableGenes(normalizedData, hvg);
        System.out.println("Finished loading data");
        /*double[][] newHvgData = new double[hvgData.length][2];
        for (int i = 0; i < hvgData.length; i++) {
            newHvgData[i][0] = hvgData[i][4];
            newHvgData[i][1] = hvgData[i][5];
        }
        hvgData = newHvgData;*/


        //projectedData = tsne(hvgData, 2);
        projectedData = hvgData;

        dataset = new ScRNAseqDataset(projectedData);
        //cluster(dataset, 70, 0, "Range", "Distance To Mean");


        /*Tuple<int[], Integer> pythonResult = runPython(observedFilePath);
        double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, pythonResult.x);
        System.out.println("NMI python: " + NMIPython);
        System.out.println("Rand index python: " + randIndex);*/

    }

    public void runTestset() {
        TestSet testSet = new TestSet(this, "data/testset");
        testSet.run(10, true);
    }

    public void runTestset(File[] selectedFiles,
                           Config config,
                           int runs,
                           boolean compareWithStandardPipeline,
                           TestEditPanel.TestProgressManager progressManager) {
        TestSet testSet = new TestSet(this, selectedFiles);
        testSet.runWIthUI(config, runs, compareWithStandardPipeline, progressManager);
    }

    public static double getDistance(double[] point1, double[] point2) {
        double length = 0;
        for (int i = 0; i < point1.length; i++) {
            length += (point1[i]-point2[i])*(point1[i]-point2[i]);
        }
        return Math.sqrt(length);
    }

    public static double silhuetteScore(double[][] data, int[] labels) {
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

    public void cluster(ScRNAseqDataset dataset, Config config) {
        monitor.setDataset(dataset);

        boolean prev1 = tangleClusterer.useAlternateConsistencyCheck;
        boolean prev2 = tangleClusterer.useOscarWerner;
        boolean prev3 = tangleClusterer.autoLimitSplitCosts;
        tangleClusterer.useAlternateConsistencyCheck = config.isUseAlternateConsistencyCheck();
        tangleClusterer.useOscarWerner = config.isUseWernerModification();
        tangleClusterer.autoLimitSplitCosts = config.isAutoComputePsi();
        tangleClusterer.generateClusters(dataset, config);
        tangleClusterer.useAlternateConsistencyCheck = prev1;
        tangleClusterer.useOscarWerner = prev2;
        tangleClusterer.autoLimitSplitCosts = prev3;

        hardClustering = tangleClusterer.getHardClustering();
        double NMIScore = NormalizedMutualInformation.joint(hardClustering, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);
        System.out.println(NMIScore);
        System.out.println(randIndex);
    }

    public int[] clusterAndReturn(ScRNAseqDataset dataset, Config config) {
        monitor.setDataset(dataset);

        boolean prev1 = tangleClusterer.useAlternateConsistencyCheck;
        boolean prev2 = tangleClusterer.useOscarWerner;
        boolean prev3 = tangleClusterer.autoLimitSplitCosts;
        tangleClusterer.useAlternateConsistencyCheck = config.isUseAlternateConsistencyCheck();
        tangleClusterer.useOscarWerner = config.isUseWernerModification();
        tangleClusterer.autoLimitSplitCosts = config.isAutoComputePsi();
        tangleClusterer.generateClusters(dataset, config);
        tangleClusterer.useAlternateConsistencyCheck = prev1;
        tangleClusterer.useOscarWerner = prev2;
        tangleClusterer.autoLimitSplitCosts = prev3;

        hardClustering = tangleClusterer.getHardClustering();
        return hardClustering;
    }

    public int[] clusterAuto(ScRNAseqDataset dataset, Config config) {
        int a = config.getA();
        double psi = config.getPsi();
        String initialCutsGenerator = config.getCutGeneratorName();
        String highLevelCostFunctionName = config.getHighLevelCostFunctionName();
        String lowLevelCostFunctionName = config.getLowLevelCostFunctionName();
        boolean useCache = config.isUseCache();
        int splitSize = config.getSplitSize();
        int tsneComponents = config.getTsneComponents();

        int maxClusters = 10;

        int minA = Math.max((dataset.data.length/maxClusters)/2, 1);
        int maxA = dataset.data.length/2;

        double[][] reducedPoints = tsne(dataset.data, tsneComponents);

        dataset.setA(minA);
        BitSet[] initialCuts = dataset.getInitialCuts(initialCutsGenerator);
        double[] costs = dataset.getCutCosts(highLevelCostFunctionName, lowLevelCostFunctionName, useCache, splitSize, tsneComponents);
        Tuple<BitSet[], double[]> redundancyRemoved = removeRedundantCuts(initialCuts, costs, 0.9); //Set factor to 1 to turn it off.
        initialCuts = redundancyRemoved.x;
        costs = redundancyRemoved.y;
        monitor.setDataset(dataset);

        int[] bestHardClustering = null;
        double bestSilhuetteScore = -1;
        int bestA = -1;

        for (int a2 = minA; a2 < maxA; a2 += 5) {
            tangleClusterer.generateClusters(a2, psi, initialCuts, costs);
            hardClustering = tangleClusterer.getHardClustering();
            double NMIScore = NormalizedMutualInformation.joint(hardClustering, groundTruth);
            double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);

            System.out.println(NMIScore);
            System.out.println(randIndex);
            double silhuetteScore = silhuetteScore(reducedPoints, hardClustering);
            if (silhuetteScore < 1.0 && silhuetteScore > bestSilhuetteScore) {
                bestSilhuetteScore = silhuetteScore;
                bestHardClustering = hardClustering;
                bestA = a2;
            }
            System.out.println(silhuetteScore);
        }

        hardClustering = bestHardClustering;

        double NMIScore = NormalizedMutualInformation.joint(hardClustering, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);

        System.out.println("Best a: " + bestA);
        System.out.println(NMIScore);
        System.out.println(randIndex);
        return hardClustering;
    }

    public static double[][] pca(double[][] data, int nComponents) {
        PCA pca = PCA.cor(data);
        return pca.getProjection(nComponents).apply(data);
    }

    public static double[][] svd(double[][] data, int nComponents) {
        Matrix X = Matrix.of(data);
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

    public double[][] loadData(String filePath) {
        return readCSV(filePath);
    }

    public int[] loadGroundTruth(String filePath) {
        double[][] temp = loadData(filePath);
        int[] gt = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            gt[i] = (int)temp[i][0];
        }
        return gt;
    }

    public double[][] highlyVariableGenes(double[][] data, int nTopGenes) {
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

    public double[][] highlyVariableGenes2(double[][] data, int nTopGenes) {
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

    public double[][] logNormalize(double[][] data) {
        double[][] normalized = new double[data.length][data[0].length];
        int nZeros = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                normalized[i][j] = Math.log(1.0 + data[i][j]);
                if (data[i][j] == 0.0) {
                    nZeros++;
                }
            }
        }
        //System.out.println("Sparsity: " + ((double)nZeros)/(normalized.length*normalized[0].length));
        //System.out.println("Dimension: " + normalized.length + " " + normalized[0].length);
        return normalized;
    }

    public double[][] readCSV(String filePath) {
        ArrayList<double[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            // Skip first row
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] stringValues = line.split(",");

                // Skip first value in the row
                if (stringValues.length <= 1) continue; // skip row if no data after first value

                double[] values = new double[stringValues.length - 1];
                for (int i = 1; i < stringValues.length; i++) { // start from index 1
                    String cleaned = stringValues[i].replaceAll("\"", "").trim();

                    if (cleaned.isEmpty()) {
                        values[i - 1] = 0; // empty cell → 0
                    } else {
                        values[i - 1] = Double.parseDouble(cleaned);
                    }
                }
                rows.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid number in CSV: " + e.getMessage());
        }

        // Convert ArrayList<int[]> to int[][]
        double[][] data = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }

        return data;
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

    public double[][] getProjectedData() {
        return projectedData;
    }

    public double[][] getHvgData() {
        return hvgData;
    }

    public double[][] getNormalizedData() {
        return normalizedData;
    }

    public double[][] getOriginalData() {
        return originalData;
    }

    public int[] getHardClustering() {
        return hardClustering;
    }

    public ScRNAseqDataset getDataset() {
        return dataset;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
        tangleClusterer.setMonitor(monitor);
    }

}

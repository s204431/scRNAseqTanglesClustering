package util;

import clustering.Model;
import datasets.ScRNAseqDataset;
import smile.validation.metric.AdjustedRandIndex;
import smile.validation.metric.NormalizedMutualInformation;
import visualization.test.TestProgressManager;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

public class TestSet {

    private static final boolean SAVE_CLUSTERINGS = true;

    private String dirPath;
    public String[] observedPaths;
    private Model model;

    public double[] averageNMIScores;
    public double[] averageRandIndexScores;
    public double[] averageTimes;
    public double[] NMIPythonResults;
    public double[] randIndexPythonResults;
    public double[] pythonTimes;

    public TestSet(Model model, String directoryPath) {
        this.model = model;
        dirPath = directoryPath;
        File dir = new File(directoryPath);
        String[] filePaths = dir.list();
        observedPaths = new String[filePaths.length/2]; //Assuming that only the required files are there.

        //Find all observed and labels file paths.
        int index = 0;
        for (int i = 0; i < filePaths.length; i++) {
            if (filePaths[i].contains("observed_counts")) {
                observedPaths[index] = filePaths[i];
                index++;
            }
        }
    }

    public TestSet(Model model, File[] files) {
        this.model = model;
        dirPath = files[0].getParent();

        observedPaths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            observedPaths[i] = files[i].getAbsolutePath().replace(dirPath + "\\", "");
        }
    }

    public void run(int nRunsPerDataset, boolean runPython) {

        System.out.println("Testing on " + observedPaths.length + " datasets with " + nRunsPerDataset + " runs");

        averageNMIScores = new double[observedPaths.length];
        averageRandIndexScores = new double[observedPaths.length];
        if (runPython) {
            NMIPythonResults = new double[observedPaths.length];
            randIndexPythonResults = new double[observedPaths.length];
        }

        for (int i = 0; i < observedPaths.length; i++) {
            String observedFilePath = observedPaths[i];
            Tuple<float[][], int[]> loaded = model.loadData(dirPath + "/" +  observedFilePath);
            float[][] originalData = loaded.x;
            int[] groundTruth = loaded.y;

            Random r = new Random();
            int seed = r.nextInt();
            int[] shuffledGroundTruth = groundTruth.clone();
            model.shuffleArray(shuffledGroundTruth, seed);
            model.shuffleArray(originalData, seed);

            float[][] normalizedData = model.logNormalize(originalData);
            double[][] hvgData = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            int nClusters = getNumberOfClusters(groundTruth);
            for (int j = 0; j < nRunsPerDataset; j++) {

                ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData, model.getMonitor());

                int a = (int)(((double)dataset.data.length/nClusters)*0.667);
                Config config = new Config();
                config.setA(a);
                int[] hardClustering = model.cluster(dataset, config);
                double NMI = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
                double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);
                averageNMIScores[i] += NMI;
                averageRandIndexScores[i] += randIndex;
            }
            averageNMIScores[i] /= nRunsPerDataset;
            averageRandIndexScores[i] /= nRunsPerDataset;
            System.out.println("Average results for dataset " + observedPaths[i].replace("observed_counts_", ""));
            System.out.println("NMI score: " + averageNMIScores[i]);
            System.out.println("Rand Index score: " + averageRandIndexScores[i]);
            System.out.println();

            if (runPython) {
                ScanpyRunner.startScanpy();
                Tuple<int[], Double> pythonResult = ScanpyRunner.runClustering(dirPath + "/" + observedFilePath);
                ScanpyRunner.stopScanpy();

                double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
                double randIndexPython = AdjustedRandIndex.of(groundTruth, pythonResult.x);
                NMIPythonResults[i] = NMIPython;
                randIndexPythonResults[i] = randIndexPython;
                pythonTimes[i] = pythonResult.y;
                System.out.println("NMI python: " + NMIPython);
                System.out.println("Rand index python: " + randIndexPython);
                System.out.println();
            }
        }

        double overallAverageNMI = 0.0;
        double overallAverageRandIndex = 0.0;

        double pythonAverageNMI = 0.0;
        double pythonAverageRandIndex = 0.0;

        for (int i = 0; i < averageNMIScores.length; i++) {
            overallAverageNMI += averageNMIScores[i];
            overallAverageRandIndex += averageRandIndexScores[i];
            if (runPython) {
                pythonAverageNMI += NMIPythonResults[i];
                pythonAverageRandIndex += randIndexPythonResults[i];
            }
        }
        overallAverageNMI /= averageNMIScores.length;
        overallAverageRandIndex /= averageRandIndexScores.length;

        System.out.println("Overall average:");
        System.out.println("NMI score: " + overallAverageNMI);
        System.out.println("Rand Index score: " + overallAverageRandIndex);

        if (runPython) {
            pythonAverageNMI /= NMIPythonResults.length;
            pythonAverageRandIndex /= randIndexPythonResults.length;
            System.out.println("Python NMI score: " + pythonAverageNMI);
            System.out.println("Python Rand Index score: " + pythonAverageRandIndex);
        }

    }

    public void runWIthUI(Config[] configs,
                          int nRunsPerDataset,
                          boolean runPython,
                          TestProgressManager progressManager) {

        System.out.println("Running warmup clustering...");
        singleWarmup(configs[0]);
        System.out.println("Warmup completed\n");

        System.out.println("Testing on " + observedPaths.length + " datasets with " + nRunsPerDataset + " runs");

        int nTests = observedPaths.length;
        int nConfigs = configs.length;

        TestLogger testLogger = new TestLogger(nTests, nConfigs + 1, nRunsPerDataset); //+1 for python
        progressManager.initializeProgress(nTests, nConfigs, nRunsPerDataset, runPython);

        int[][] groundTruths = new int[nTests][];
        double[] sparsities = new double[nTests];

        double[][] averageNMIScores = new double[nTests][nConfigs];
        double[][] averageRandIndexScores = new double[nTests][nConfigs];
        double[][] averageTimes = new double[nTests][nConfigs];

        for (int testIndex = 0; testIndex < nTests; testIndex++) {
            if (progressManager.testingStopped()) return;

            // Read and preprocess dataset
            String observedFilePath = observedPaths[testIndex];
            Tuple<float[][], int[]> loaded = model.loadData(dirPath + "/" + observedFilePath);
            float[][] originalData = loaded.x;
            int[] groundTruth = loaded.y;

            double sparsity = model.computeSparsity(originalData);
            float[][] normalizedData = model.logNormalize(originalData);

            groundTruths[testIndex] = groundTruth;
            sparsities[testIndex] = sparsity;

            // Prepare shuffling seeds
            Random r = new Random();
            int[] shuffleSeeds = new int[nRunsPerDataset];
            for (int i = 0; i < nRunsPerDataset; i++) shuffleSeeds[i] = r.nextInt(Integer.MAX_VALUE);
            

            for (int configIndex = 0; configIndex < configs.length; configIndex++) {
                Config config = configs[configIndex];

                for (int run = 0; run < nRunsPerDataset; run++) {
                    if (progressManager.testingStopped()) return;

                    // Shuffle the dataset before each run
                    int[] shuffledGroundTruth = groundTruth.clone();
                    model.shuffleArray(shuffledGroundTruth, shuffleSeeds[run]);
                    model.shuffleArray(normalizedData, shuffleSeeds[run]);

                    long preTime1 = System.currentTimeMillis();
                    double[][] hvgData = model.highlyVariableGenes(normalizedData, Math.min(2000, normalizedData[0].length));
                    int nClusters = getNumberOfClusters(groundTruth);
                    long preTime = System.currentTimeMillis() - preTime1;

                    // Clustering
                    long time1 = System.currentTimeMillis();
                    ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData, model.getMonitor());

                    int a;
                    if (config.isUseSplitPruning()) {
                        a = (int) ((hvgData.length / 16.0) * 0.55);
                    } else {
                        a = (int) (((double) dataset.data.length / nClusters) * config.getaFactor());
                    }
                    Config newConfig = new Config(config);
                    newConfig.setA(a);

                    boolean tuneParameters = config.isTuneParameters();
                    int[] hardClustering = tuneParameters ? model.clusterAuto(dataset, newConfig) : model.cluster(dataset, newConfig);
                    double postTime = (preTime + (System.currentTimeMillis() - time1)) / 1000.0;
                    averageTimes[testIndex][configIndex] += postTime;

                    // Evaluate clustering
                    double NMI = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
                    double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);
                    averageNMIScores[testIndex][configIndex] += NMI;
                    averageRandIndexScores[testIndex][configIndex] += randIndex;

                    // Save results
                    testLogger.setResult(observedFilePath, progressManager.getTitle(configIndex), testIndex, configIndex, run, sparsity, postTime, NMI, randIndex, getNumberOfClusters(hardClustering), (double) model.getMonitor().getDimReductionTime() / 1000, (double) model.getMonitor().getSilhouetteTime() / 1000);
                    progressManager.markSingleRunFinished();
                    if (SAVE_CLUSTERINGS) {
                        File folder = new File("results");
                        if (!folder.exists()) folder.mkdirs();

                        String name = "Test" + (testIndex + 1) + "_" + progressManager.getTitle(configIndex) + "_Run" + (run + 1);
                        //File hardFile = new File(folder, name + "_Hard.csv");
                        File softFile = new File(folder, name + "_Soft.csv");

                        try {
                            //ClusteringIO.saveHard(model.computeUnShuffledArray(hardClustering, shuffleSeeds[run]), hardFile);
                            ClusteringIO.saveSoft(model.computeUnShuffledArray(model.getSoftClustering(), shuffleSeeds[run]), softFile);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Unshuffle dataset after each run
                    normalizedData = model.computeUnShuffledArray(normalizedData, shuffleSeeds[run]);
                }

                averageNMIScores[testIndex][configIndex] /= nRunsPerDataset;
                averageRandIndexScores[testIndex][configIndex] /= nRunsPerDataset;
                averageTimes[testIndex][configIndex] /= nRunsPerDataset;
                progressManager.markTangleFinished(configIndex, testIndex, averageTimes[testIndex][configIndex], averageNMIScores[testIndex][configIndex], averageRandIndexScores[testIndex][configIndex]);
                if (!runPython) progressManager.markPythonFinished(testIndex, 0.0, 0.0, 0.0);
                System.out.println("Average results for dataset " + observedPaths[testIndex].replace("observed_counts_", "") + " for config file " + (configIndex + 1));
                System.out.println("NMI score: " + averageNMIScores[testIndex][configIndex]);
                System.out.println("Rand Index score: " + averageRandIndexScores[testIndex][configIndex]);
                System.out.println("Average time (s): " + averageTimes[testIndex][configIndex]);
                System.out.println();
            }
        }

        double overallAverageNMI = 0.0;
        double overallAverageRandIndex = 0.0;
        double overallAverageTime = 0.0;

        for (int i = 0; i < averageNMIScores.length; i++) {
            overallAverageNMI += averageNMIScores[i][0];
            overallAverageRandIndex += averageRandIndexScores[i][0];
            overallAverageTime += averageTimes[i][0];
        }

        overallAverageNMI /= averageNMIScores.length;
        overallAverageRandIndex /= averageRandIndexScores.length;
        overallAverageTime /= averageTimes.length;

        System.out.println("Overall average for user defined configs:");
        System.out.println("NMI score: " + overallAverageNMI);
        System.out.println("Rand Index score: " + overallAverageRandIndex);
        System.out.println("Time (s): " + overallAverageTime);

        if (runPython) {
            runScanpy(nTests, nRunsPerDataset, groundTruths, sparsities, progressManager, testLogger);
            System.out.println("Scanpy testing completed.\n");
        }

        progressManager.fireAllFinished();

        testLogger.printResults();
        testLogger.writeResultsCSV("test_results.csv");
    }

    private int getNumberOfClusters(int[] clustering) {
        HashSet<Integer> uniques = new HashSet<>();
        for (int i : clustering) {
            uniques.add(i);
        }
        return uniques.size();
    }

    // Runs a warmup clustering on the first dataset of the test set.
    private void singleWarmup(Config warmupConfig) {
        String observedFilePath = observedPaths[0];
        Tuple<float[][], int[]> loaded = model.loadData(dirPath + "/" + observedFilePath);
        float[][] originalData = loaded.x;
        int[] groundTruth = loaded.y;

        Random r = new Random();
        int seed = r.nextInt();
        model.shuffleArray(originalData, seed);

        float[][] normalizedData = model.logNormalize(originalData);
        double[][] hvgData = model.highlyVariableGenes(normalizedData, Math.min(2000, normalizedData[0].length));
        int nClusters = getNumberOfClusters(groundTruth);

        // Single warmup clustering
        ScRNAseqDataset warmupDataset = new ScRNAseqDataset(hvgData, model.getMonitor());
        int warmupA = (int)(((double)warmupDataset.data.length/nClusters)*0.667);
        warmupConfig.setA(warmupA);
        model.cluster(warmupDataset, warmupConfig);
    }

    private void runScanpy(int nTests, int nRuns, int[][] groundTruths, double[] sparsities, TestProgressManager progressManager, TestLogger testLogger) {
        ScanpyRunner.startScanpy();

        // Warmup clustering
        ScanpyRunner.runClustering(dirPath + "/" + observedPaths[0]);

        for (int testIndex = 0; testIndex < nTests; testIndex++) {
            if (progressManager.testingStopped()) break;

            String observedFilePath = observedPaths[testIndex];
            String filePath = dirPath + "/" + observedFilePath;

            double[] nmiAverages = new double[nTests];
            double[] randIndexAverages = new double[nTests];
            double[] timeAverages = new double[nTests];

            for (int runIndex = 0; runIndex < nRuns; runIndex++) {
                if (progressManager.testingStopped()) break;

                Tuple<int[], Double> pythonResult = ScanpyRunner.runClustering(filePath);
                int[] groundTruth = groundTruths[testIndex];

                double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
                double randIndexPython = AdjustedRandIndex.of(groundTruth, pythonResult.x);

                int pythonTitleIndex = progressManager.getConfigsSize();
                testLogger.setResult(observedFilePath, progressManager.getTitle(pythonTitleIndex), testIndex, pythonTitleIndex, runIndex, sparsities[testIndex], pythonResult.y, NMIPython, randIndexPython, getNumberOfClusters(pythonResult.x), 0, 0);

                nmiAverages[testIndex] += NMIPython;
                randIndexAverages[testIndex] += randIndexPython;
                timeAverages[testIndex] += pythonResult.y;

                progressManager.markSingleRunFinished();

                if (SAVE_CLUSTERINGS) {
                    File folder = new File("results");
                    if (!folder.exists()) folder.mkdirs();

                    String name = "Test" + (testIndex + 1) + "_Scanpy" + "_Run" + (runIndex + 1);
                    File hardFile = new File(folder, name + "_Hard.csv");
                    try {
                        ClusteringIO.saveHard(pythonResult.x, hardFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            nmiAverages[testIndex] /= nRuns;
            randIndexAverages[testIndex] /= nRuns;
            timeAverages[testIndex] /= nRuns;

            progressManager.markPythonFinished(testIndex, timeAverages[testIndex], nmiAverages[testIndex], randIndexAverages[testIndex]);
        }

        ScanpyRunner.stopScanpy();
    }
}

package util;

import clustering.Model;
import datasets.ScRNAseqDataset;
import main.Main;
import smile.validation.metric.AdjustedRandIndex;
import smile.validation.metric.NormalizedMutualInformation;
import visualization.test.TestEditPanel;

import java.io.File;
import java.util.HashSet;
import java.util.Random;

public class TestSet {

    private String dirPath;
    public String[] observedPaths;
    public String[] labelsPaths;
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
        labelsPaths = new String[filePaths.length/2];

        //Find all observed and labels file paths.
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < filePaths.length; i++) {
            if (filePaths[i].contains("observed_counts")) {
                observedPaths[index1] = filePaths[i];
                index1++;
            }
            else {
                labelsPaths[index2] = filePaths[i];
                index2++;
            }
        }

        //Match observed and labels
        String[] newLabelsPaths = new String[labelsPaths.length];
        for (int i = 0; i < observedPaths.length; i++) {
            for (int j = 0; j < labelsPaths.length; j++) {
                if (observedPaths[i].replace("observed_counts", "").equals(labelsPaths[j].replace("labels", ""))) {
                    newLabelsPaths[i] = labelsPaths[j];
                    break;
                }
            }
        }
        labelsPaths = newLabelsPaths;
    }

    public TestSet(Model model, File[] files) {
        this.model = model;
        dirPath = files[0].getParent();

        observedPaths = new String[files.length];
        labelsPaths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            observedPaths[i] = files[i].getAbsolutePath().replace(dirPath + "\\", "");
            labelsPaths[i] = observedPaths[i].replace("observed_counts", "labels");
        }

        //Match observed and labels
        String[] newLabelsPaths = new String[labelsPaths.length];
        for (int i = 0; i < observedPaths.length; i++) {
            for (int j = 0; j < labelsPaths.length; j++) {
                if (observedPaths[i].replace("observed_counts", "").equals(labelsPaths[j].replace("labels", ""))) {
                    newLabelsPaths[i] = labelsPaths[j];
                    break;
                }
            }
        }
        labelsPaths = newLabelsPaths;
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
            String labelFilePath = labelsPaths[i];
            Tuple<double[][], int[]> loaded = model.loadData(dirPath + "/" +  observedFilePath, dirPath + "/" + labelFilePath);
            double[][] originalData = loaded.x;
            int[] groundTruth = loaded.y;

            Random r = new Random();
            int seed = r.nextInt();
            int[] shuffledGroundTruth = groundTruth.clone();
            model.shuffleArray(shuffledGroundTruth, seed);
            model.shuffleArray(originalData, seed);

            double[][] normalizedData = model.logNormalize(originalData);
            double[][] hvgData = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            int nClusters = getNumberOfClusters(groundTruth);
            for (int j = 0; j < nRunsPerDataset; j++) {

                ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData);

                int a = (int)(((double)dataset.data.length/nClusters)*0.667);
                Config config = new Config(a);
                int[] hardClustering = model.clusterAndReturn(dataset, config);
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
                Tuple<int[], Double> pythonResult = Main.runPython(dirPath + "/" + observedFilePath);
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
                          TestEditPanel.TestProgressManager progressManager) {

        System.out.println("Testing on " + observedPaths.length + " datasets with " + nRunsPerDataset + " runs");

        int nTests = observedPaths.length;
        int nConfigs = configs.length;

        double[][] averageNMIScores = new double[nTests][nConfigs];
        double[][] averageRandIndexScores = new double[nTests][nConfigs];
        double[][] averageTimes = new double[nTests][nConfigs];

        double[] NMIPythonResults = new double[nTests];
        double[] randIndexPythonResults = new double[nTests];
        double[] pythonTimes = new double[nTests];

        for (int testIndex = 0; testIndex < nTests; testIndex++) {
            String observedFilePath = observedPaths[testIndex];
            String labelFilePath = labelsPaths[testIndex];
            Tuple<double[][], int[]> loaded = model.loadData(dirPath + "/" + observedFilePath, dirPath + "/" + labelFilePath);
            double[][] originalData = loaded.x;
            int[] groundTruth = loaded.y;

            Random r = new Random();
            int seed = r.nextInt();
            int[] shuffledGroundTruth = groundTruth.clone();
            model.shuffleArray(shuffledGroundTruth, seed);
            model.shuffleArray(originalData, seed);

            long preTime1 = System.currentTimeMillis();
            double[][] normalizedData = model.logNormalize(originalData);
            double[][] hvgData = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            int nClusters = getNumberOfClusters(groundTruth);
            long preTime = System.currentTimeMillis() - preTime1;

            for (int configIndex = 0; configIndex < configs.length; configIndex++) {
                Config config = configs[configIndex];

                for (int run = 0; run < nRunsPerDataset; run++) {
                    long time1 = System.currentTimeMillis();
                    ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData);

                    int a;
                    if (config.isAutoComputeA()) {
                        a = (int) ((hvgData.length / 20.0) * 0.7);
                    } else {
                        a = (int) (((double) dataset.data.length / nClusters) * config.getaFactor());
                    }
                    Config newConfig = new Config(config.isUseAlternateConsistencyCheck(), config.isUseWernerModification(), config.isUseCache(), config.getCutGeneratorName(), config.getHighLevelCostFunctionName(), config.getLowLevelCostFunctionName(), a, config.getaFactor(), config.getPsi());
                    newConfig.setAutoCompute(config.isAutoComputeA(), config.isAutoComputePsi());
                    newConfig.setTuneParameters(config.isTuneParameters());
                    newConfig.setRemoveRedundant(config.isRemoveRedundant());
                    newConfig.setDimensionReductionParameters(config.getSplitSize(), config.getTsneComponents());

                    boolean tuneParameters = config.isTuneParameters();
                    int[] hardClustering = tuneParameters ? model.clusterAuto(dataset, newConfig) : model.clusterAndReturn(dataset, newConfig);
                    averageTimes[testIndex][configIndex] += (preTime + (System.currentTimeMillis() - time1)) / 1000.0;

                    double NMI = NormalizedMutualInformation.joint(hardClustering, shuffledGroundTruth);
                    double randIndex = AdjustedRandIndex.of(shuffledGroundTruth, hardClustering);
                    averageNMIScores[testIndex][configIndex] += NMI;
                    averageRandIndexScores[testIndex][configIndex] += randIndex;
                }

                averageNMIScores[testIndex][configIndex] /= nRunsPerDataset;
                averageRandIndexScores[testIndex][configIndex] /= nRunsPerDataset;
                averageTimes[testIndex][configIndex] /= nRunsPerDataset;
                progressManager.markTangleFinished(configIndex, testIndex, averageTimes[testIndex][configIndex], averageNMIScores[testIndex][configIndex], averageRandIndexScores[testIndex][configIndex]);
                if (!runPython) progressManager.markPythonFinished(testIndex, 0, 0, 0);
                System.out.println("Average results for dataset " + observedPaths[testIndex].replace("observed_counts_", "") + " for config file " + (configIndex + 1));
                System.out.println("NMI score: " + averageNMIScores[testIndex][configIndex]);
                System.out.println("Rand Index score: " + averageRandIndexScores[testIndex][configIndex]);
                System.out.println("Average time (s): " + averageTimes[testIndex][configIndex]);
                System.out.println();
            }

            if (runPython) {
                Tuple<int[], Double> pythonResult = Main.runPython(dirPath + "/" + observedFilePath);
                double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
                double randIndexPython = AdjustedRandIndex.of(groundTruth, pythonResult.x);
                NMIPythonResults[testIndex] = NMIPython;
                randIndexPythonResults[testIndex] = randIndexPython;
                pythonTimes[testIndex] = pythonResult.y;
                progressManager.markPythonFinished(testIndex, pythonTimes[testIndex], NMIPythonResults[testIndex], randIndexPythonResults[testIndex]);
                System.out.println("NMI python: " + NMIPython);
                System.out.println("Rand index python: " + randIndexPython);
                System.out.println("Python time: " + pythonResult.y);
                System.out.println();
            }
        }

        double overallAverageNMI = 0.0;
        double overallAverageRandIndex = 0.0;
        double overallAverageTime = 0.0;

        double pythonAverageNMI = 0.0;
        double pythonAverageRandIndex = 0.0;
        double pythonAverageTime = 0.0;

        for (int i = 0; i < averageNMIScores.length; i++) {
            overallAverageNMI += averageNMIScores[i][0];
            overallAverageRandIndex += averageRandIndexScores[i][0];
            overallAverageTime += averageTimes[i][0];
            if (runPython) {
                pythonAverageNMI += NMIPythonResults[i];
                pythonAverageRandIndex += randIndexPythonResults[i];
                pythonAverageTime += pythonTimes[i];
            }
        }
        overallAverageNMI /= averageNMIScores.length;
        overallAverageRandIndex /= averageRandIndexScores.length;
        overallAverageTime /= averageTimes.length;

        System.out.println("Overall average for user defined configs:");
        System.out.println("NMI score: " + overallAverageNMI);
        System.out.println("Rand Index score: " + overallAverageRandIndex);
        System.out.println("Time (s): " + overallAverageTime);

        if (runPython) {
            pythonAverageNMI /= NMIPythonResults.length;
            pythonAverageRandIndex /= randIndexPythonResults.length;
            pythonAverageTime /= pythonTimes.length;
            System.out.println("Python NMI score: " + pythonAverageNMI);
            System.out.println("Python Rand Index score: " + pythonAverageRandIndex);
            System.out.println("Python Average Time: " + pythonAverageTime);
        }

    }

    private int getNumberOfClusters(int[] clustering) {
        HashSet<Integer> uniques = new HashSet<>();
        for (int i : clustering) {
            uniques.add(i);
        }
        return uniques.size();
    }


}

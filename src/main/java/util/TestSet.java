package util;

import clustering.Model;
import datasets.ScRNAseqDataset;
import main.Main;
import org.nd4j.common.primitives.Atomic;
import smile.validation.metric.AdjustedRandIndex;
import smile.validation.metric.NormalizedMutualInformation;
import visualization.testSet.TestEditPanel;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestSet {

    private String dirPath;
    public String[] observedPaths;
    public String[] labelsPaths;
    private Model model;

    public double[] averageNMIScores;
    public double[] averageRandIndexScores;
    public double[] NMIPythonResults;
    public double[] randIndexPythonResults;

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
            double[][] originalData = model.loadData(dirPath + "/" +  observedFilePath);
            int[] groundTruth = model.loadGroundTruth(dirPath + "/" + labelFilePath);
            double[][] normalizedData = model.logNormalize(originalData);
            double[][] hvgData = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            int nClusters = getNumberOfClusters(groundTruth);
            for (int j = 0; j < nRunsPerDataset; j++) {

                ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData);

                int a = (int)(((double)dataset.data.length/nClusters)*0.667);
                Config config = new Config(false, false, "Default", "Default", a, 0.0, 0);
                int[] hardClustering = model.clusterAndReturn(dataset, config);
                double NMI = NormalizedMutualInformation.joint(hardClustering, groundTruth);
                double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);
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
                Tuple<int[], Integer> pythonResult = Main.runPython(dirPath + "/" + observedFilePath);
                double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
                double randIndexPython = AdjustedRandIndex.of(groundTruth, pythonResult.x);
                NMIPythonResults[i] = NMIPython;
                randIndexPythonResults[i] = randIndexPython;
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

    public void runWIthUI(Config config,
                          int nRunsPerDataset,
                          boolean runPython,
                          TestEditPanel.TestProgressManager progressManager) {

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
            double[][] originalData = model.loadData(dirPath + "/" +  observedFilePath);
            int[] groundTruth = model.loadGroundTruth(dirPath + "/" + labelFilePath);
            double[][] normalizedData = model.logNormalize(originalData);
            double[][] hvgData = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            int nClusters = getNumberOfClusters(groundTruth);
            for (int j = 0; j < nRunsPerDataset; j++) {

                ScRNAseqDataset dataset = new ScRNAseqDataset(hvgData);

                int a = (int)(((double)dataset.data.length/nClusters)*config.getaFactor());
                Config newConfig = new Config(config.isUseAlternateConsistencyCheck(), config.isUseWernerModification(), config.getCutGeneratorName(), config.getCostFunctionName(), a, config.getaFactor(), config.getPsi());
                int[] hardClustering = model.clusterAndReturn(dataset, newConfig);
                double NMI = NormalizedMutualInformation.joint(hardClustering, groundTruth);
                double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);
                averageNMIScores[i] += NMI;
                averageRandIndexScores[i] += randIndex;
            }
            averageNMIScores[i] /= nRunsPerDataset;
            averageRandIndexScores[i] /= nRunsPerDataset;
            progressManager.markFinished(i, true, 0, averageNMIScores[i], averageRandIndexScores[i]);
            if (!runPython) progressManager.markFinished(i, false, 0, 0, 0);
            System.out.println("Average results for dataset " + observedPaths[i].replace("observed_counts_", ""));
            System.out.println("NMI score: " + averageNMIScores[i]);
            System.out.println("Rand Index score: " + averageRandIndexScores[i]);
            System.out.println();

            if (runPython) {
                Tuple<int[], Integer> pythonResult = Main.runPython(dirPath + "/" + observedFilePath);
                double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
                double randIndexPython = AdjustedRandIndex.of(groundTruth, pythonResult.x);
                NMIPythonResults[i] = NMIPython;
                randIndexPythonResults[i] = randIndexPython;
                progressManager.markFinished(i, false, 0, NMIPythonResults[i], randIndexPythonResults[i]);
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

    private int getNumberOfClusters(int[] clustering) {
        HashSet<Integer> uniques = new HashSet<>();
        for (int i : clustering) {
            uniques.add(i);
        }
        return uniques.size();
    }


}

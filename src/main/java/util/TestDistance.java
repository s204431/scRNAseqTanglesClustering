package util;

import clustering.Model;

public class TestDistance {

    public void runTest() {
        runTest(true);
        runTest(false);
    }

    public void runTest(boolean normalize) {
        System.out.println("Running " + (normalize ? "with" : "without") + " normalization");
        Model model = new Model();

        String[] dataPaths = new String[] {"data/symsim_observed_counts_5000genes_500cells_complex5.csv", "data/symsim_observed_counts_5000genes_1000cells_complex.csv", "data/symsim_observed_counts_5000genes_500cells_simple2.csv"};

        double[][][] fullData = new double[dataPaths.length][][];
        int[][] fullGroundTruths = new int[dataPaths.length][];
        for (int i = 0; i < dataPaths.length; i++) {
            String path = dataPaths[i];
            String labelFilePath = path.replace("observed_counts", "labels");
            Tuple<float[][], int[]> loaded = model.loadData(path, labelFilePath);
            float[][] originalData = loaded.x;
            float[][] normalizedData = normalize ? model.logNormalize(originalData) : originalData;
            int[] groundTruth = loaded.y;
            double[][] data = model.highlyVariableGenes(normalizedData, normalizedData[0].length);
            fullData[i] = data;
            fullGroundTruths[i] = groundTruth;
        }
        checkDistanceMeasures(fullData, fullGroundTruths);

    }

    public void checkDistanceMeasures(double[][][] data, int[][] groundTruths) {
        checkDistanceMeasure(data, groundTruths, Distance.euclidean(), true, "Euclidean");
        checkDistanceMeasure(data, groundTruths, Distance.euclidean(), false, "Euclidean");
        checkDistanceMeasure(data, groundTruths, Distance.jaccard(), true, "Jaccard");
        checkDistanceMeasure(data, groundTruths, Distance.jaccard(), false, "Jaccard");
        checkDistanceMeasure(data, groundTruths, Distance.cosine(), true, "Cosine");
        checkDistanceMeasure(data, groundTruths, Distance.cosine(), false, "Cosine");
        checkDistanceMeasure(data, groundTruths, Distance.manhattan(), true, "Manhattan");
        checkDistanceMeasure(data, groundTruths, Distance.manhattan(), false, "Manhattan");
        checkDistanceMeasure(data, groundTruths, Distance.chebyshev(), true, "Chebyshev");
        checkDistanceMeasure(data, groundTruths, Distance.chebyshev(), false, "Chebyshev");
        checkDistanceMeasure(data, groundTruths, Distance.pearson(), true, "Pearson");
        checkDistanceMeasure(data, groundTruths, Distance.pearson(), false, "Pearson");
        checkDistanceMeasure(data, groundTruths, Distance.hamming(), true, "Hamming");
        checkDistanceMeasure(data, groundTruths, Distance.hamming(), false, "Hamming");
    }

    public void checkDistanceMeasure(double[][][] data, int[][] groundTruths, DistanceMeasure distanceMeasure, boolean tsne, String name) {
        System.out.print(name + (tsne ? " TSNE" : "") + (": "));
        for (int i = 0; i < data.length; i++) {
            double[][] currentData = data[i];
            if (tsne) {
                currentData = Model.tsne(currentData, 5);
            }
            double[][] distanceMatrix = Distance.getDistanceMatrix(currentData, distanceMeasure);
            System.out.print(scoreDistanceMeasure(distanceMatrix, groundTruths[i]) + " ");
        }
        System.out.println();
    }

    public double scoreDistanceMeasure(double[][] distanceMatrix, int[] groundTruth) {
        int nCorrect = 0;
        for (int i = 0; i < distanceMatrix.length; i++) {
            int closestIndex = -1;
            double closestDistance = Double.MAX_VALUE;
            for (int j = 0; j < distanceMatrix[i].length; j++) {
                if (i != j && distanceMatrix[i][j] < closestDistance) {
                    closestDistance = distanceMatrix[i][j];
                    closestIndex = j;
                }
            }
            if (groundTruth[i] == groundTruth[closestIndex]) {
                nCorrect++;
            }
        }
        return nCorrect/(double)distanceMatrix.length;
    }
}

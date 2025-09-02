import clustering.TangleClusterer;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.utils.TSneUtils;
import datasets.ScRNAseqDataset;

import smile.feature.extraction.PCA;
import smile.math.matrix.Matrix;
import util.Tuple;

import java.io.*;
import java.util.ArrayList;

import smile.validation.metric.NormalizedMutualInformation;
import smile.math.matrix.Matrix.SVD;
import smile.manifold.UMAP;

import com.google.gson.Gson;

public class Main {
    public static void main(String[] args) {

        // Read data
        String filePath = "data/symsim_observed_counts_5000genes_1000cells_complex.csv";
        int[][] data = readCSV(filePath);

        // Convert to double
        int dimensions = 5000;//data[0].length;
        double[][] doubleData = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            doubleData[i] = new double[dimensions];
            //doubleData[i][0] = (double) data[i][0];
            //doubleData[i][1] = (double) data[i][1];
            for (int j = 0; j < dimensions; j++) {
                doubleData[i][j] = (double) data[i][j];
            }
        }
        System.out.println(doubleData.length + " " + doubleData[0].length);

        // Ground truth
        String filePathLabels = "data/symsim_labels_5000genes_1000cells_complex.csv";
        int[][] temp = readCSV(filePathLabels);
        int[] gt = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            gt[i] = temp[i][0];
        }

        long time = System.currentTimeMillis();
        double[][] projectedData = tsne(doubleData, 50);
        System.out.println(System.currentTimeMillis() - time);

        ScRNAseqDataset dataset = new ScRNAseqDataset(projectedData);
        TangleClusterer tangleClusterer = new TangleClusterer();
        tangleClusterer.generateClusters(dataset, 70, 0, "Default", "Default");
        int[] hardClustering = tangleClusterer.getHardClustering();
        double NMIScore = NormalizedMutualInformation.joint(hardClustering, gt);

        Tuple<int[], Integer> pythonResult = runPython(filePath);

        double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, gt);

        System.out.println("Finished");
        System.out.println(NMIScore);
        System.out.println("NMI python: " + NMIPython);
    }

    public static double[][] pca(double[][] data, int nComponents) {
        PCA pca = PCA.cor(data);
        return pca.getProjection(nComponents).apply(data);
    }

    public static double[][] svd(double[][] data, int nComponents) {
        Matrix X = Matrix.of(data);
        SVD svd = X.svd();

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
        int initialDims = data[0].length;
        double perplexity = 20.0;
        int maxIterations = 500;
        BarnesHutTSne tsne = new BHTSne();
        TSneConfiguration config = TSneUtils.buildConfig(data, nComponents, initialDims, perplexity, maxIterations);
        return tsne.tsne(config);
    }

    public static double[][] umap(double[][] data, int nComponents) {
        return UMAP.fit(data, new UMAP.Options(2, nComponents, 200, 1, 0.1, 1.0, 5, 1.0, 2));
    }



    public static int[][] readCSV(String filePath) {
        ArrayList<int[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            // Skip first row
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] stringValues = line.split(",");

                // Skip first value in the row
                if (stringValues.length <= 1) continue; // skip row if no data after first value

                int[] values = new int[stringValues.length - 1];
                for (int i = 1; i < stringValues.length; i++) { // start from index 1
                    String cleaned = stringValues[i].replaceAll("\"", "").trim();

                    if (cleaned.isEmpty()) {
                        values[i - 1] = 0; // empty cell → 0
                    } else {
                        values[i - 1] = Integer.parseInt(cleaned);
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
        int[][] data = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }

        return data;
    }

    public static Tuple<int[], Integer> runPython(String filePath) {
        try {
            // Python script path
            String pythonScript = "scRNAseq.py";

            // Start Python process
            ProcessBuilder pb = new ProcessBuilder("python", pythonScript);
            Process process = pb.start();

            // Send a string to Python
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            String message = filePath;
            writer.write(message);
            writer.newLine();
            writer.flush();

            // Read response from Python
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String response = reader.readLine(); // JSON string from Python

            // Parse JSON to Java List<Integer>
            Gson gson = new Gson();
            int[] numbers = gson.fromJson(response, int[].class);

            int exitCode = process.waitFor();
            System.out.println("Python exited with code: " + exitCode);

            return new Tuple<int[], Integer>(numbers, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
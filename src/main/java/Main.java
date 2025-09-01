import clustering.TangleClusterer;
import datasets.ScRNAseqDataset;
import util.BitSet;
import util.Tuple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import smile.validation.metric.NormalizedMutualInformation;


public class Main {
    public static void main(String[] args) {

        // Read data
        //String filePath = "C:\\Dev\\Projects\\DTU\\Master Thesis Preparation\\symsim_true_counts_5000genes_1000cells_complex.csv";
        String filePath = "data/symsim_observed_counts_5000genes_1000cells_complex.csv";
        int[][] data = readCSV(filePath);

        // Convert to double
        int dimensions = 20;//data[0].length;
        double[][] doubleData = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            doubleData[i] = new double[dimensions];
            //doubleData[i][0] = (double) data[i][0];
            //doubleData[i][1] = (double) data[i][1];
            for (int j = 0; j < dimensions; j++) {
                doubleData[i][j] = (double) data[i][j];
            }
        }

        // Ground truth
        //filePath = "C:\\Dev\\Projects\\DTU\\Master Thesis Preparation\\symsim_labels_5000genes_1000cells_complex.csv";
        filePath = "data/symsim_labels_5000genes_1000cells_complex.csv";
        int[][] temp = readCSV(filePath);
        int[] gt = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            gt[i] = temp[i][0];
        }

        ScRNAseqDataset dataset = new ScRNAseqDataset(doubleData);
        TangleClusterer tangleClusterer = new TangleClusterer();
        tangleClusterer.generateClusters(dataset, 90, 0, "Default", "Default");
        int[] hardClustering = tangleClusterer.getHardClustering();
        double NMIScore = NormalizedMutualInformation.joint(hardClustering, gt);


        System.out.println("Finished");
        System.out.println(NMIScore);
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
}
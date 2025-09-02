package visualization;

import clustering.TangleClusterer;
import datasets.ScRNAseqDataset;
import smile.validation.metric.NormalizedMutualInformation;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ParameterPanel extends JPanel {
    private TextField a = new TextField(10);
    private TextField psi = new TextField(10);
    private JButton clusterButton = new JButton("Cluster");

    // REMOVE THIS
    private ScatterPlotPanel scatterPlotPanel;

    private double[][] projectedData;

    public ParameterPanel() {
        //setPreferredSize(new Dimension(150, 800));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new Label("Dimension"), gbc);

        gbc.gridy = 1;
        add(new Label("Clustering"), gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new Label("a"), gbc);
        gbc.gridx = 1;
        add(a, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        add(new Label("psi"), gbc);
        gbc.gridx = 1;
        add(psi, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        add(clusterButton, gbc);

        clusterButton.addActionListener(e -> {
            String aValue = a.getText();
            String psiValue = psi.getText();
            System.out.println(aValue + " " + psiValue);

            // Ground truth
            String filePathLabels = "data/symsim_labels_5000genes_1000cells_complex.csv";
            int[][] temp = readCSV(filePathLabels);
            int[] gt = new int[temp.length];
            for (int i = 0; i < temp.length; i++) {
                gt[i] = temp[i][0];
            }

            ScRNAseqDataset dataset = new ScRNAseqDataset(projectedData);
            TangleClusterer tangleClusterer = new TangleClusterer();
            tangleClusterer.generateClusters(dataset, Integer.parseInt(aValue), Double.parseDouble(psiValue), "Default", "Default");
            int[] hardClustering = tangleClusterer.getHardClustering();
            double NMIScore = NormalizedMutualInformation.joint(hardClustering, gt);

            System.out.println(NMIScore);

            scatterPlotPanel.setClusters(hardClustering);
            scatterPlotPanel.drawClusters();

        });
    }

    // TODO: REMOVE THESE
    public void setProjectedData(double[][] projectedData) {
        this.projectedData = projectedData;
    }

    public void setPanel(ScatterPlotPanel scatterPlotPanel) {
        this.scatterPlotPanel = scatterPlotPanel;
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

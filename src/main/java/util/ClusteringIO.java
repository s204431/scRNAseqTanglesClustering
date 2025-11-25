package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ClusteringIO {

    public static void saveHard(int[] labels, File file) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath())) {
            out.write("cell,cluster\n");
            for (int i = 0; i < labels.length; i++) {
                out.write(i + "," + labels[i] + "\n");
            }
        }
    }

    public static int[] loadHard(File file) throws IOException {
        List<Integer> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    list.add(Integer.parseInt(parts[1]));
                }
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    public static void saveSoft(double[][] probs, File file) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath())) {
            out.write("cell");
            for (int j = 0; j < probs[0].length; j++) {
                out.write(",cluster_" + (j+1));
            }
            out.write("\n");

            for (int i = 0; i < probs.length; i++) {
                out.write(Integer.toString(i));
                for (double p : probs[i]) {
                    out.write("," + p);
                }
                out.write("\n");
            }
        }
    }

    public static double[][] loadSoft(File file) throws IOException {
        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String header = br.readLine(); // skip header
            if (header == null) return new double[0][0]; // empty file

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // skip empty lines
                String[] parts = line.split(",", -1); // keep all columns
                if (parts.length < 2) continue; // skip malformed lines
                double[] row = new double[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    row[i - 1] = Double.parseDouble(parts[i].trim());
                }
                rows.add(row);
            }
        }
        return rows.toArray(new double[rows.size()][]);
    }

    public static Object load(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.isEmpty()) throw new IOException("Empty file");

        String header = lines.get(0);
        boolean isSoft = header.toLowerCase().contains("cluster_");

        // Remove header
        lines = lines.subList(1, lines.size());

        if (!isSoft) {
            // HARD CLUSTERING: cell,cluster
            int[] labels = new int[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                labels[i] = Integer.parseInt(parts[1].trim());
            }
            return labels;
        } else {
            // SOFT CLUSTERING: cell,p1,p2,p3,...
            int numCells = lines.size();
            int numClusters = lines.get(0).split(",").length - 1;
            double[][] probs = new double[numCells][numClusters];

            for (int i = 0; i < numCells; i++) {
                String[] parts = lines.get(i).split(",");
                for (int j = 0; j < numClusters; j++) {
                    probs[i][j] = Double.parseDouble(parts[j + 1].trim());
                }
            }
            return probs;
        }
    }
}

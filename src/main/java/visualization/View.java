package visualization;

import clustering.Model;
import monitor.Monitor;
import util.BitSet;

import javax.swing.*;

public class View {
    private Model model;
    private MainWindow window;

    private double[][] points;

    private Monitor monitor;

    public View(Model model) {
        this.model = model;

        points = model.tsne(model.getHvgData(), 2);

        SwingUtilities.invokeLater(() -> {
            window = new MainWindow(this);
            window.drawPoints(points);
        });
    }

    public void performClustering(int a, double psi, String cutGeneratorName, String costFunctionName) {
        model.cluster(model.getDataset(), a, psi, cutGeneratorName, costFunctionName);
        showClustering(model.getHardClustering());
    }

    public void showClustering(int[] clustering) {
        window.drawClusters(points, clustering);
    }

    public void showClustering() {
        window.drawClusters(points, model.getHardClustering());
    }

    public void showGroundTruth() {
        showClustering(model.getGroundTruth());
    }

    public void showCut(BitSet cut) {
        int[] clustering = new int[cut.size()];
        for (int i = 0; i < cut.size(); i++) {
            if (cut.get(i)) {
                clustering[i] = 1;
            }
        }
        showClustering(clustering);
    }

    public BitSet[] getCuts() {
        return model.getCuts();
    }

    public double[] getCutCosts() {
        return model.getCutCosts();
    }

    public void drawTangleSearchTree() {
        window.drawTangleSearchTree();
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
}

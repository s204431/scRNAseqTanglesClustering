package visualization;

import clustering.Model;
import clustering.TangleSearchTree;
import main.Main;
import monitor.Monitor;
import util.BitSet;
import util.TestSet;

import javax.swing.*;

public class View {
    private Model model;
    private MainWindow window;

    private double[][] points;

    private Monitor monitor;

    public View(Model model) {
        this.model = model;

        points = Model.tsne(model.getHvgData(), 2);
        Main.zScoreNorm(points);

        SwingUtilities.invokeLater(() -> {
            window = new MainWindow(this);
            window.drawPoints(points);
        });
    }

    public void performClustering(boolean useAlternateConsistencyCheck,
                                  boolean useWernerModification,
                                  String cutGeneratorName,
                                  String costFunctionName,
                                  int a,
                                  double psi) {
        model.cluster(model.getDataset(), a, psi, cutGeneratorName, costFunctionName, useAlternateConsistencyCheck, useWernerModification);
        showClustering(model.getHardClustering());
    }

    public void showClustering(int[] clustering) {
        window.drawClusters(points, clustering);
    }

    public void showClustering() {
        window.drawClusters(points, model.getHardClustering());
        window.turnOffCuts();
    }

    public void showGroundTruth() {
        showClustering(model.getGroundTruth());
    }

    public void showCut(BitSet cut, int cutIndex) {
        int[] clustering = new int[cut.size()];
        for (int i = 0; i < cut.size(); i++) {
            if (cut.get(i)) {
                clustering[i] = 1;
            }
        }
        window.turnOnCuts(cutIndex);
        showClustering(clustering);
    }

    public BitSet[] getCuts() {
        return model.getCuts();
    }

    public double[] getCutCosts() {
        return model.getCutCosts();
    }

    public void drawTangleSearchTree(boolean condensed) {
        TangleSearchTree tree = monitor.getUncondensedTree();
        if (condensed) {
            tree = monitor.getCondensedTree();
        }
        if (tree != null) {
            window.drawTangleSearchTree(tree);
        }
    }

    public void loadDataset(String filePath) {
        model.loadDataset(filePath);
        points = Model.tsne(model.getHvgData(), 2);
        Main.zScoreNorm(points);
        window.drawPoints(points);
    }

    public void runTestSet(String dirPath) {
        new TestSet(model, dirPath).run(5, true);
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
}

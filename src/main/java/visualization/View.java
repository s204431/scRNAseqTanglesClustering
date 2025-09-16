package visualization;

import clustering.Model;
import clustering.TangleSearchTree;
import main.Main;
import monitor.Monitor;
import util.BitSet;
import util.TestSet;

import javax.swing.*;
import java.io.File;
import java.util.List;

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

    public void runTestSet(boolean useAlternateConsistencyCheck,
                           boolean useWernerModification,
                           String cutGeneratorName,
                           String costFunctionName,
                           int a,
                           double psi,
                           int runs,
                           boolean compareWithStandardPipeline) {
        File[] selectedFiles = window.getSelectedTestFiles();
        model.runTestset(selectedFiles, useAlternateConsistencyCheck, useWernerModification, cutGeneratorName, costFunctionName, a, psi, runs, compareWithStandardPipeline);
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

    public void showTestSet(List<File> selectedDirs) {
        window.showTestSet(selectedDirs);
    }


    public void changeView(String viewName) {
        window.changeView(viewName);
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

}

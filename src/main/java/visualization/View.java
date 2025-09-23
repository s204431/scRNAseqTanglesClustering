package visualization;

import clustering.Model;
import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;
import main.Main;
import util.Monitor;
import util.BitSet;
import util.Config;
import visualization.test.TestEditPanel;

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

        SwingUtilities.invokeLater(() -> {
            window = new MainWindow(this);
            loadDataset("data/symsim_observed_counts_5000genes_1000cells_complex.csv");
        });
    }

    public void performClustering(Config config) {
        ScRNAseqDataset dataSet = model.getDataset();
        model.cluster(dataSet, config);
        showClustering(model.getHardClustering());
    }

    public void runTestSetWithUI(Config config, int runs, boolean compareWithStandardPipeline) {
        if (window.testIsRunning()) {
            return;
        }
        TestEditPanel.TestProgressManager progressManager = window.prepareUIForTesting();
        File[] selectedFiles = window.getSelectedTestFiles();
        if (selectedFiles == null || selectedFiles.length == 0) {
            System.out.println("No test files were selected.");
            return;
        }
        model.runTestset(selectedFiles, config, runs, compareWithStandardPipeline, progressManager);
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
        TangleSearchTree tree = condensed ? monitor.getCondensedTree() : monitor.getUncondensedTree();
        if (tree != null) {
            window.drawTangleSearchTree(tree);
        }
    }

    public void loadDataset(String filePath) {
        SwingUtilities.invokeLater(() -> window.changeView(MainWindow.LOADING_VIEW));

        new Thread(() -> {
            model.loadDataset(filePath);
            loadDataset();
        }).start();
    }

    public void loadDataset() {
        points = Model.tsne(model.getHvgData(), 2);
        points = Main.zScoreNorm(points);

        SwingUtilities.invokeLater(() -> {
            window.drawPoints(points);
            window.showInformation(model.getDataset());
            window.changeView(MainWindow.DATA_VIEW);
        });
    }

    public void showTestSet(List<File> selectedDirs) {
        window.showTestSet(selectedDirs);
    }

    public void visualizeTestResults(int i, int j, boolean isTangle) {
        window.visualizeTestResults(i, j, isTangle);
    }


    public void changeView(String viewName) {
        window.changeView(viewName);
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public List<double[]> getBranchCosts() {
        return monitor.getBranchCosts();
    }
}

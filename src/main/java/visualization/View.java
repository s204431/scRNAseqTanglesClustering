package visualization;

import clustering.Model;
import datasets.ScRNAseqDataset;
import main.Main;
import util.Monitor;
import util.BitSet;
import util.Config;
import visualization.test.TestEditPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

import com.formdev.flatlaf.FlatLightLaf;

public class View {
    private Model model;
    private MainWindow window;

    protected double[][] points;

    private Monitor monitor;

    private Thread loaderThread;
    private Thread testThread;

    public View(Model model) {
        this.model = model;

        FlatLightLaf.setup();
        UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource("Inter", Font.PLAIN, 13));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);

        UIManager.put("TabbedPane.tabsPopupPolicy", "asNeeded");
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(5, 2, 5, 2));

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        //System.setProperty("sun.java2d.uiScale.enabled", "true");

        SwingUtilities.invokeLater(() -> {
            window = new MainWindow(this);
            loadDataset("data/symsim_observed_counts_5000genes_1000cells_complex.csv", 0);
        });
    }

    public void performClustering(Config config) {
        ScRNAseqDataset dataSet = model.getDataset();
        if (config.isTuneParameters()) model.clusterAuto(dataSet, config);
        else model.clusterAndReturn(dataSet, config);
        showClustering(model.getHardClustering());
    }

    public void runTestSetWithUI(Config config, int runs, boolean compareWithStandardPipeline) {
        if (window.testIsRunning() || testThread != null) {
            return;
        }

        TestEditPanel.TestProgressManager progressManager = window.prepareUIForTesting();
        File[] selectedTestFiles = window.getSelectedTestFiles();
        if (selectedTestFiles == null || selectedTestFiles.length == 0) {
            System.out.println("No test files were selected.");
            return;
        }

        File[] selectedConfigFiles = window.getSelectedConfigFiles();
        Config[] configs = new Config[selectedConfigFiles.length + 1];  // Make space for user defined configurations
        configs[0] = config;
        for (int i = 0; i < selectedConfigFiles.length; i++) {
            configs[i+1] = Config.loadConfiguration(selectedConfigFiles[i].getName());
        }

        testThread = new Thread(() -> {
            try {
                model.runTestset(selectedTestFiles, configs, runs, compareWithStandardPipeline, progressManager);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                stopTesting();
                testThread = null;
            }
        });
        testThread.start();
    }

    public void showClustering() {
        showClustering(model.getHardClustering());
    }

    public void showClustering(int[] clustering) {
        showClustering(clustering, true);
    }

    public void showClustering(int[] clustering, boolean tangle) {
        if (!tangle) {
            // Shuffle python's result to match the order of the data points
            model.shuffleArray(clustering, model.getSeed());
        }
        window.drawClusters(points, clustering, tangle);
    }

    public void showGroundTruth() {
        window.drawGroundTruth(points, model.getShuffledGroundTruth());
    }

    public void showCut(BitSet cut, int cutIndex) {
        int[] clustering = new int[cut.size()];
        for (int i = 0; i < cut.size(); i++) {
            if (cut.get(i)) {
                clustering[i] = 1;
            }
        }
        window.turnOnCuts(cutIndex);
        window.showCut(points, clustering);
    }

    public BitSet[] getCuts() {
        return model.getCuts();
    }

    public double[] getCutCosts() {
        return model.getCutCosts();
    }

    public void drawTangleSearchTree() {
        window.drawTangleSearchTree(monitor.getUncondensedTree(), monitor.getSplitPrunedTree(), monitor.getCondensedTree());
    }

    public void loadDataset(String filePath, int hvg) {
        if (loaderThread != null) {
            return;
        }

        SwingUtilities.invokeLater(() -> window.changeView(MainWindow.LOADING_VIEW));

        loaderThread = new Thread(() -> {
            try {
                model.loadDataset(filePath, hvg);
                loadDataset();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                loaderThread = null;
            }
        });
        loaderThread.start();
    }

    public void loadDataset() {
        points = Model.svd(model.getHvgData(), 100);
        points = Model.tsne(points, 2);
        points = Main.zScoreNorm(points);

        SwingUtilities.invokeLater(() -> {
            window.removeScatterTabs();
            window.removeTrees();
            window.initializeScatterPlotPanel(points, model.getShuffledGroundTruth());
            window.showInformation(model.getDataset());
            window.changeView(MainWindow.DATA_VIEW);
        });
    }

    public void showTestSet(List<File> selectedDirs) {
        window.showTestSet(selectedDirs);
    }

    public void visualizeTestResults(int i, int j) {
        window.visualizeTestResults(i, j);
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

    public void stopTesting() {
        if (testThread != null && testThread.isAlive()) {
            testThread.interrupt();
        }

        SwingUtilities.invokeLater(window::stopTesting);
    }

    public String getCurrentFilePath() {
        return monitor.getFilePath();
    }

    public void loadAndDrawTrees(int clusterIndex) {
        window.loadAndDrawTrees(clusterIndex);
    }

    public void removeTree(int clusterIndex) {
        window.removeTree(clusterIndex);
    }

    public void removeTrees() {
        window.removeTrees();
    }
}

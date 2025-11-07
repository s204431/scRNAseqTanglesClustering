package visualization;

import clustering.Model;
import datasets.ScRNAseqDataset;
import main.Main;
import smile.validation.metric.AdjustedRandIndex;
import smile.validation.metric.NormalizedMutualInformation;
import util.Monitor;
import util.BitSet;
import util.Config;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

import com.formdev.flatlaf.FlatLightLaf;
import visualization.test.TestProgressManager;

public class View {
    private Model model;
    private MainWindow window;

    protected double[][] points;

    private Monitor monitor;

    private Thread loaderThread;
    private Thread testThread;

    private TestProgressManager testProgressManager;

    public View(Model model) {
        this.model = model;

        FlatLightLaf.setup();
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);

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
        else model.cluster(dataSet, config);
        showClustering(model.getHardClustering());
    }

    public void runTestSetWithUI(Config config, int runs, boolean compareWithStandardPipeline) {
        if (testThread != null && testThread.isAlive()) {
            return;
        }

        File[] selectedTestFiles = window.getSelectedTestFiles();
        if (selectedTestFiles == null || selectedTestFiles.length == 0) {
            System.out.println("No test files were selected.");
            window.stopTesting();
            return;
        }

        File[] selectedConfigFiles = window.getSelectedConfigFiles();

        String[] titles;
        Config[] configs;
        if (selectedConfigFiles.length > 0) {
            titles = new String[selectedConfigFiles.length + (compareWithStandardPipeline ? 1 : 0)];
            configs = new Config[selectedConfigFiles.length];
            for (int i = 0; i < selectedConfigFiles.length; i++) {
                String fileName = selectedConfigFiles[i].getName();
                titles[i] = fileName.replace(".txt", "");
                configs[i] = Config.loadConfiguration(fileName);
            }
            if (compareWithStandardPipeline) titles[selectedConfigFiles.length] = "Scanpy";

        // In case no configs were chosen, we use user-defined configs from parameter panel
        } else {
            if (compareWithStandardPipeline) {
                titles = new String[]{"Tangle", "Scanpy"};
            } else {
                titles = new String[]{"Tangle"};
            }
            configs = new Config[]{config};
        }

        testProgressManager = window.prepareUIForTesting(titles);
        testThread = new Thread(() -> {
            try {
                model.runTestset(selectedTestFiles, configs, runs, compareWithStandardPipeline, testProgressManager);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(window::stopTesting);
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
        points = model.getHvgData();
        if (points.length > 4000) {
            points = Model.svd(points, 2);
        } else {
            points = Model.svd(points, 100);
            points = Model.tsne(points, 2);
        }
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


    public void changeView(String viewName) {
        window.changeView(viewName);
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public List<double[]> getBranchCosts() {
        return monitor.getBranchCosts();
    }

    public void stopTestingThread() {
        if (testProgressManager != null) {
            testProgressManager.setStopTesting(true);
        }
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

    public Config getCurrentConfigurations() {
        return window.getCurrentConfigurations();
    }

    public void loadConfig(Config config) {
        window.loadConfig(config);
    }

    public void updateStatisticsPanel(int clusterIndex, int[] clustering, long clusterTime) {
        window.showClusteringDetails(clusterIndex, clustering, clusterTime);
    }

    public Tuple<Double, Double> getClusteringQuality(int[] clustering) {
        double nmi = NormalizedMutualInformation.joint(clustering, model.getShuffledGroundTruth());
        double randIndex = AdjustedRandIndex.of(model.getShuffledGroundTruth(), clustering);
        return new Tuple<>(nmi, randIndex);
    }

    public double getSilhouetteScore(int[] clustering) {
        return Model.silhouetteScore(points, clustering);
    }

    public double getDavisBouldin(int[] clustering) {
        return Model.daviesBouldinIndex(points, clustering);
    }

    public int[] unshuffleClustering(int[] clustering) {
        return model.computeUnShuffledArray(clustering, model.getSeed());
    }

    public double[][] unshuffleClustering(double[][] clustering) {
        return model.computeUnShuffledArray(clustering, model.getSeed());
    }

    public double[][] getSoftClustering() {
        return model.getSoftClustering();
    }

    public long getClusteringTime() {
        return monitor.getClusterTime();
    }

    public Tuple<int[], Double> getScanpyResult() {
        Tuple<int[], Double> result = Main.runPython(getCurrentFilePath());

        long currentTime = System.currentTimeMillis();
        monitor.setClusterStartTime((long) (currentTime - (result.y*1000)));
        monitor.setClusterEndTime(currentTime);
        return result;
    }
}

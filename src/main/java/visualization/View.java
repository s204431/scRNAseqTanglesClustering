package visualization;

import clustering.Model;
import datasets.ScRNAseqDataset;
import main.Main;
import smile.validation.metric.AdjustedRandIndex;
import smile.validation.metric.NormalizedMutualInformation;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
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

    public View(Model model, Monitor monitor) {
        this.model = model;
        this.monitor = monitor;

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
            loadDataset("data/symsim_observed_counts_5000genes_1000cells_complex.csv", 0, true);
        });
    }

    public void loadDataset(String filePath, int hvg, boolean normalizeData) {
        if (loaderThread != null) {
            return;
        }

        SwingUtilities.invokeLater(() -> window.changeView(MainWindow.LOADING_VIEW));

        loaderThread = new Thread(() -> {
            try {
                model.loadDataset(filePath, hvg, normalizeData);
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
            points = Model.svd(points, 2).x;
        } else {
            points = Model.svd(points, 100).x;
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

    public void performClustering(Config config) {
        ScRNAseqDataset dataSet = model.getDataset();
        if (config.isTuneParameters()) model.clusterAuto(dataSet, config);
        else model.cluster(dataSet, config);
        showClustering();
    }

    public void removeUncertainPoints(double certainty) {
        double[][] softClustering = window.getCurrentSoftClustering();
        if (softClustering == null) return;

        int[] hardClustering = softToHardClustering(softClustering);
        double lastNmi = NormalizedMutualInformation.joint(hardClustering, model.getShuffledGroundTruth());
        double lastAri = AdjustedRandIndex.of(model.getShuffledGroundTruth(), hardClustering);
        System.out.println("NMI BEFORE: " + lastNmi);
        System.out.println("ARI BEFORE: " + lastAri);

        HashSet<Integer> indicesToRemove = new HashSet<>();
        for (int cellIdx = 0; cellIdx < softClustering.length; cellIdx++) {
            double maxProb = 0;
            for (double prob : softClustering[cellIdx]) {
                if (prob > maxProb) maxProb = prob;
            }
            if (maxProb < certainty) indicesToRemove.add(cellIdx);
        }

        int removeSize = indicesToRemove.size();

        int[] GT = model.getShuffledGroundTruth();
        int[] HARD = model.getHardClustering();
        double[][] SOFT = model.getSoftClustering();

        double[][] newPoints = points.clone();
        int[] newGT = GT.clone();
        int[] newHARD = HARD.clone();
        double[][] newSOFT = SOFT.clone();

        int improvements = 0;
        int nRemove = 0;//removeSize - 1;   // Set to 0 to remove one point at a time
        while (nRemove < removeSize) {
            nRemove++;

            newPoints = new double[points.length - nRemove][points[0].length];
            newGT = new int[GT.length - nRemove];
            newHARD = new int[HARD.length - nRemove];
            newSOFT = new double[SOFT.length - nRemove][SOFT[0].length];

            int idx = 0;
            int removed = 0;
            for (int i = 0; i < points.length; i++) {
                if (indicesToRemove.contains(i) && removed < nRemove) {
                    removed++;
                    continue;
                }
                newPoints[idx] = points[i];
                newGT[idx] = GT[i];
                newHARD[idx] = HARD[i];
                newSOFT[idx] = SOFT[i];
                idx++;
            }

            double nmi = NormalizedMutualInformation.joint(newHARD, newGT);
            double ari = AdjustedRandIndex.of(newGT, newHARD);
            if (ari > lastAri && nmi > lastNmi) improvements++;
            lastNmi = nmi;
            lastAri = ari;
            //System.out.println("NMI: " + nmi + "   ARI: " + ari);
        }

        double nmi = NormalizedMutualInformation.joint(newHARD, newGT);
        double ari = AdjustedRandIndex.of(newGT, newHARD);
        System.out.println("NMI: " + nmi + "   ARI: " + ari);
        System.out.println("Removed points: " + removeSize + " Improvements: " + improvements);

        double[][] temp = points;
        points = newPoints;
        window.updateClustering(newPoints, newSOFT);
        window.updatePerformanceMetrics(nmi, ari);
        points = temp;
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
        double[][] softClustering = model.getSoftClustering();
        if (softClustering != null) showClustering(softClustering);
        else showClustering(model.getHardClustering());

    }

    public void showClustering(int[] clustering) {
        showClustering(points, clustering, true);
    }

    public void showClustering(int[] clustering, boolean tangle) {
        showClustering(points, clustering, tangle);
    }

    public void showClustering(double[][] clustering) {
        showClustering(points, null, clustering, true);
    }

    public void showClustering(double[][] points, int[] clustering, boolean tangle) {
        if (!tangle) {
            // Shuffle python's result to match the order of the data points
            model.shuffleArray(clustering, model.getSeed());
        }
        window.drawClusters(points, clustering, null, tangle);
    }

    public void showClustering(double[][] points, int[] hardClustering, double[][] softClustering, boolean tangle) {
        if (hardClustering == null) hardClustering = softToHardClustering(softClustering);
        window.drawClusters(points, hardClustering, softClustering, tangle);
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

    public void drawTangleSearchTree(boolean removeRedundantCuts) {
        window.drawTangleSearchTree(monitor.getUncondensedTree(), monitor.getSplitPrunedTree(), monitor.getCondensedTree(), removeRedundantCuts, model.getHardClustering());
    }

    public void showTestSet(List<File> selectedDirs) {
        window.showTestSet(selectedDirs);
    }


    public void changeView(String viewName) {
        window.changeView(viewName);
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

    public void loadAndDrawTrees(int clusterIndex, int[] clustering) {
        window.loadAndDrawTrees(clusterIndex, clustering, model.getShuffledGroundTruth());
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
        int[] GT = model.getShuffledGroundTruth();
        if (GT.length != clustering.length) return new Tuple<>(0.0, 0.0);

        double nmi = NormalizedMutualInformation.joint(clustering, GT);
        double randIndex = AdjustedRandIndex.of(GT, clustering);
        return new Tuple<>(nmi, randIndex);
    }

    public double getSilhouetteScore(int[] clustering) {
        if (points.length != clustering.length) return 0.0;
        return Model.silhouetteScore(points, clustering);
    }

    public double getDavisBouldin(int[] clustering) {
        if (points.length != clustering.length) return 0.0;
        return Model.daviesBouldinIndex(points, clustering);
    }

    public void shuffleClustering(int[] clustering) {
        model.shuffleArray(clustering, model.getSeed());
    }

    public void shuffleClustering(double[][] clustering) {
        model.shuffleArray(clustering, model.getSeed());
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
        ScanpyRunner.startScanpy();
        Tuple<int[], Double> result = ScanpyRunner.runClustering(getCurrentFilePath());
        ScanpyRunner.stopScanpy();

        long currentTime = System.currentTimeMillis();
        monitor.setClusterStartTime((long) (currentTime - (result.y*1000)));
        monitor.setClusterEndTime(currentTime);
        return result;
    }

    public int[] softToHardClustering(double[][] softClustering) {
        int[] hardClustering = new int[softClustering.length];
        for (int i = 0; i < softClustering.length; i++) {
            double maxProb = -1;
            int bestCluster = -1;
            for (int j = 0; j < softClustering[i].length; j++) {
                if (softClustering[i][j] > maxProb) {
                    maxProb = softClustering[i][j];
                    bestCluster = j;
                }
            }
            hardClustering[i] = bestCluster;
        }
        return hardClustering;
    }
}

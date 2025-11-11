package visualization;

import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;
import util.Config;
import visualization.data.ScatterPlotPanel;
import visualization.data.StatisticsPanel;
import visualization.data.TangleTreePanel;
import visualization.test.TestEditPanel;
import visualization.test.TestGraphPanel;
import visualization.test.TestProgressManager;
import visualization.test.TestResultPanel;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.io.File;

public class MainWindow extends JFrame {
    public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    public static final String DATA_VIEW = "data";
    public static final String TEST_VIEW = "test";
    public static final String LOADING_VIEW = "loading";

    private View view;

    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel cards = new JPanel(new CardLayout());

    private final TangleTreePanel tangleTreePanel;
    private final ScatterPlotPanel scatterPanel;
    private final StatisticsPanel statsPanel;
    private final ParameterPanel dataParameterPanel;
    private final ParameterPanel testParameterPanel;
    private final TestEditPanel testEditPanel;
    private final TestGraphPanel testGraphPanel;
    private final TestResultPanel testResultPanel;
    private final TopPanel topPanel;

    private String currentView;

    private final JProgressBar testProgressBar = new JProgressBar(0, 100);

    public MainWindow(View view) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.view = view;

        this.topPanel = new TopPanel(view);
        this.scatterPanel = new ScatterPlotPanel(view);
        this.tangleTreePanel = new TangleTreePanel(view);
        this.statsPanel = new StatisticsPanel(view);
        this.dataParameterPanel = new ParameterPanel(view, true);
        this.testParameterPanel = new ParameterPanel(view, false);
        this.testEditPanel = new TestEditPanel(view);
        this.testResultPanel = new TestResultPanel(view, testEditPanel.getTestProgressManager());
        this.testGraphPanel = new TestGraphPanel(view, testEditPanel.getTestProgressManager());

        int width = (int) (SCREEN_SIZE.width * 0.7);
        int height = (int) (SCREEN_SIZE.height * 0.8);
        setSize(width, height);
        setLocationRelativeTo(null);

        cards.add(createDataView(), DATA_VIEW);
        cards.add(createTestView(), TEST_VIEW);
        cards.add(createLoadingView(), LOADING_VIEW);

        JPanel temp = new JPanel();
        temp.setLayout(new BoxLayout(temp, BoxLayout.Y_AXIS));
        temp.add(topPanel);
        temp.add(Box.createRigidArea(new Dimension(0,5)));

        testProgressBar.setStringPainted(true);
        testProgressBar.setVisible(false);

        root.add(temp, BorderLayout.NORTH);
        root.add(cards, BorderLayout.CENTER);
        root.add(testProgressBar, BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    public void changeView(String viewName) {
        currentView = viewName;
        ((CardLayout) cards.getLayout()).show(cards, viewName);
        cards.revalidate();
        cards.repaint();
    }

    private JComponent createDataView() {
        JPanel dataPanel = new JPanel(new BorderLayout());

        JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scatterPanel, statsPanel);
        split1.setResizeWeight(0.7); // % space the scatter panel takes initially

        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tangleTreePanel, split1);
        split2.setResizeWeight(0.2);

        dataPanel.add(split2, BorderLayout.CENTER);
        dataPanel.add(dataParameterPanel, BorderLayout.EAST);

        return dataPanel;
    }

    private JComponent createTestView() {
        JPanel testPanel = new JPanel(new BorderLayout());

        JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, testGraphPanel, testResultPanel);
        split1.setResizeWeight(0.7); // % space the scatter panel takes initially

        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, testEditPanel, split1);
        split2.setResizeWeight(0.2);

        testPanel.add(split2, BorderLayout.CENTER);
        testPanel.add(testParameterPanel, BorderLayout.EAST);

        return testPanel;
    }

    private JComponent createLoadingView() {
        JPanel loadingPanel = new JPanel(new GridBagLayout());
        JLabel textLabel = new JLabel("Loading Data Set…");
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD, 28f));
        loadingPanel.add(textLabel, new GridBagConstraints());
        return loadingPanel;
    }

    public void drawPoints(double[][] points) {
        scatterPanel.drawScatterPlot(points);
    }

    public void drawClusters(double[][] points, int[] clustering, boolean tangle) {
        scatterPanel.drawClusters(points, clustering, view.getSoftClustering(), tangle);
    }

    public void drawGroundTruth(double[][] points, int[] groundTruth) {
        scatterPanel.drawGroundTruth(points, groundTruth);
    }

    public void drawTangleSearchTree(TangleSearchTree originalTree, TangleSearchTree splitPruned, TangleSearchTree condensed, boolean removeRedundantCuts) {
        int clusterIndex = scatterPanel.getClusterIndex();
        tangleTreePanel.drawTrees(originalTree, splitPruned, condensed, clusterIndex, removeRedundantCuts);
        statsPanel.updateTangleStats(new TangleSearchTree[] { originalTree, splitPruned, condensed });
    }

    public void turnOnCuts(int cutIndex) {
        dataParameterPanel.turnOnCuts(cutIndex);
    }

    public void showTestSet(List<File> selectedDirs) {
        testEditPanel.loadTestSet(selectedDirs);
        testEditPanel.loadConfigFiles();
    }

    public File[] getSelectedTestFiles() {
        return testEditPanel.getSelectedTests();
    }

    public File[] getSelectedConfigFiles() {
        return testEditPanel.getSelectedConfigFiles();
    }

    public TestProgressManager prepareUIForTesting(String[] titles) {
        TestProgressManager testProgressManager = testEditPanel.initializeTestProgressManager();
        testProgressManager.setTitles(titles);

        addTestProgressListener(testProgressManager);

        testResultPanel.initializeResultsTable(titles);
        return testProgressManager;
    }

    public void stopTesting() {
        testEditPanel.stopTesting();
        testParameterPanel.stopTesting();

        testProgressBar.setVisible(false);
        testProgressBar.setValue(0);
    }

    public void addTestProgressListener(TestProgressManager testProgressManager) {
        testProgressBar.setVisible(true);

        TestProgressManager.Listener listener = new TestProgressManager.Listener() {
            @Override
            public void onTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex) {
                int progress = testProgressManager.getProgress();
                testProgressBar.setValue(progress);
            }

            @Override
            public void onPythonFinished(int testIndex, double time, double nmi, double randIndex) {
                int progress = testProgressManager.getProgress();
                testProgressBar.setValue(progress);
            }

            @Override
            public void onRunFinished() {
                int progress = testProgressManager.getProgress();
                testProgressBar.setValue(progress);
            }

            @Override
            public void onAllFinished() {
                int progress = testProgressManager.getProgress();
                testProgressBar.setValue(progress);
                testProgressManager.removeListener(this);
            }
        };
        testProgressManager.addListener(listener);
    }

    public void showInformation(ScRNAseqDataset dataSet) {
        statsPanel.showDataSetInformation(dataSet);
    }

    public void removeScatterTabs() {
        scatterPanel.removeAllTabs();
    }

    public void initializeScatterPlotPanel(double[][] points, int[] groundTruth) {
        scatterPanel.initialize(points, groundTruth);
    }

    public void showCut(double[][] points, int[] clustering) {
        scatterPanel.drawCut(points, clustering);
    }

    public void loadAndDrawTrees(int clusterIndex) {
        tangleTreePanel.loadTrees(clusterIndex);
    }

    public void removeTree(int clusterIndex) {
        tangleTreePanel.removeTree(clusterIndex);
    }

    public void removeTrees() {
        tangleTreePanel.removeTrees();
    }

    public Config getCurrentConfigurations() {
        return currentView.equals(DATA_VIEW) ? dataParameterPanel.getConfig(false) : testParameterPanel.getConfig(true);
    }

    public void loadConfig(Config config) {
        if (currentView.equals(DATA_VIEW)) {
            dataParameterPanel.setConfig(config);
        } else {
            testParameterPanel.setConfig(config);
        }
    }

    public void showClusteringDetails(int clusterIndex, int[] clustering, long clusterTime) {
        statsPanel.updateClusteringStats(clustering, clusterTime);
        TangleSearchTree[] trees = tangleTreePanel.getTrees(clusterIndex);
        statsPanel.updateTangleStats(trees);
    }
}

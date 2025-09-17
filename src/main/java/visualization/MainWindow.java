package visualization;

import clustering.TangleSearchTree;
import util.Tuple;
import visualization.data.ScatterPlotPanel;
import visualization.data.StatisticsPanel;
import visualization.data.TangleTreePanel;
import visualization.testSet.TestEditPanel;
import visualization.testSet.TestProgressPanel;
import visualization.testSet.TestResultPanel;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow extends JFrame {
    private View view;

    public static final String DATA_VIEW = "data";
    public static final String TEST_VIEW = "test";

    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel cards = new JPanel(new CardLayout());

    private final TangleTreePanel tangleTreePanel;
    private final ScatterPlotPanel scatterPanel;
    private final StatisticsPanel statsPanel;
    private final ParameterPanel dataParameterPanel;
    private final ParameterPanel testParameterPanel;
    private final TestEditPanel testEditPanel;
    private final TestProgressPanel testProgressPanel;
    private final TestResultPanel testResultPanel;
    private final TopPanel topPanel;

    public MainWindow(View view) {
        this.view = view;

        this.topPanel = new TopPanel(view);
        this.scatterPanel = new ScatterPlotPanel(view);
        this.tangleTreePanel = new TangleTreePanel(view);
        this.statsPanel = new StatisticsPanel(view);
        this.dataParameterPanel = new ParameterPanel(view, true);
        this.testParameterPanel = new ParameterPanel(view, false);
        this.testEditPanel = new TestEditPanel(view);
        this.testResultPanel = new TestResultPanel(view);
        this.testProgressPanel = new TestProgressPanel(view);

        setSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cards.add(createDataView(), DATA_VIEW);
        cards.add(createTestView(), TEST_VIEW);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(cards, BorderLayout.CENTER);
        setContentPane(root);

        setVisible(true);
    }

    public void changeView(String viewName) {
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

        JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, testProgressPanel, testResultPanel);
        split1.setResizeWeight(0.7); // % space the scatter panel takes initially

        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, testEditPanel, split1);
        split2.setResizeWeight(0.2);

        testPanel.add(split2, BorderLayout.CENTER);
        testPanel.add(testParameterPanel, BorderLayout.EAST);

        return testPanel;
    }

    public void drawPoints(double[][] points) {
        scatterPanel.drawScatterPlot(points);
    }

    public void drawClusters(double[][] points, int[] clustering) {
        scatterPanel.drawClusters(points, clustering);
    }

    public void drawTangleSearchTree(TangleSearchTree tree) {
        tangleTreePanel.drawTree(tree);
    }

    public void turnOnCuts(int cutIndex) {
        dataParameterPanel.turnOnCuts(cutIndex);
    }

    public void turnOffCuts() {
        dataParameterPanel.turnOffCuts();
    }

    public void showTestSet(List<File> selectedDirs) {
        testEditPanel.loadTestSet(selectedDirs);
    }

    public File[] getSelectedTestFiles() {
        return testEditPanel.getSelectedTests();
    }

    public TestEditPanel.TestProgressManager prepareUIForTesting() {
        TestEditPanel.TestProgressManager out = testEditPanel.getTestProgressManager();
        testEditPanel.startTimer();
        return out;
    }

    public boolean testIsRunning() {
        return testEditPanel.isRunning();
    }
}

package visualization.test;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import util.GlobalConstants;
import visualization.View;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TestGraphPanel extends JPanel {
    private static final Color[] COLORS = getColors(100);

    private View view;

    private TestProgressManager testProgressManager;
    private TestProgressManager.Listener testProgressListener;

    private final JComboBox<String> plotStyleComboBox = new JComboBox<>(new String[] {"Bar Plot", "Line Plot"});
    private final JCheckBox showTimeCheckBox = new JCheckBox("Show time plot", false);

    private final JPanel componentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
    private final JPanel plotPanel = new JPanel(new GridLayout(1, 1, 5, 5));

    private int lastTestIndex = 0;

    public TestGraphPanel(View view, TestProgressManager testProgressManager) {
        this.view = view;
        this.testProgressManager = testProgressManager;
        testProgressListener = new TestProgressManager.Listener() {
            @Override
            public void onTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex) {
                drawPlots(testIndex);
            }

            @Override
            public void onPythonFinished(int testIndex, double time, double nmi, double randIndex) {
                drawPlots(testIndex);
            }
        };
        testProgressManager.addListener(testProgressListener);

        setLayout(new BorderLayout(5, 5));

        componentPanel.add(plotStyleComboBox);
        componentPanel.add(showTimeCheckBox);
        add(componentPanel, BorderLayout.NORTH);
        add(plotPanel, BorderLayout.CENTER);
        addActions();
        drawEmptyPlots();
    }

    private void addActions() {
        plotStyleComboBox.addActionListener(e -> {
            drawPlots(lastTestIndex);
        });

        showTimeCheckBox.addActionListener(e -> {
            drawPlots(lastTestIndex);
        });
    }

    public void drawPlots(int testIndex) {
        if (testProgressManager.getSize() == 0) {
            drawEmptyPlots();
            return;
        }

        lastTestIndex = testIndex;

        int nGraphs = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(nGraphs, 1, 5, 5));
        plotPanel.removeAll();

        boolean useBarPlot = Objects.equals(plotStyleComboBox.getSelectedItem(), "Bar Plot");
        boolean drawTimePlot = showTimeCheckBox.isSelected();

        ChartPanel nmiPlot = useBarPlot ?
                buildBarPlot("NMI", "Test", "NMI Score", Metric.NMI, testIndex) :
                buildLinePlot("NMI", "Test", "NMI Score", Metric.NMI, testIndex);
        ChartPanel randPlot = useBarPlot ?
                buildBarPlot("Rand Index", "Test", "Rand Index Score", Metric.RAND, testIndex) :
                buildLinePlot("Rand Index", "Test", "Rand Index Score", Metric.RAND, testIndex);
        plotPanel.add(nmiPlot);
        plotPanel.add(randPlot);

        if (drawTimePlot) {
            ChartPanel timePlot = useBarPlot ?
                    buildBarPlot("Time", "Test", "Time (ms)", Metric.TIME, testIndex) :
                    buildLinePlot("Time", "Test", "Time (ms)", Metric.TIME, testIndex);
            plotPanel.add(timePlot);
        }

        plotPanel.revalidate();
        plotPanel.repaint();
    }

    private enum Metric { NMI, RAND, TIME }

    private ChartPanel buildBarPlot(String title, String xLabel, String yLabel, Metric metric, int upToTestIndex) {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        int nConfigs = testProgressManager.getConfigsSize();
        int nTests = upToTestIndex + 1;

        for (int test = 0; test < nTests; test++) {

            // Tangle
            for (int cfg = 0; cfg < nConfigs; cfg++) {
                double v = switch (metric) {
                    case NMI  -> testProgressManager.getTangleNMI(cfg, test);
                    case RAND -> testProgressManager.getTangleRandIndex(cfg, test);
                    case TIME -> testProgressManager.getTangleTime(cfg, test);
                };
                dataSet.addValue(v, testProgressManager.getTitle(cfg), ""+(test + 1));
            }

            // Python
            if (testProgressManager.getTitleCount() != nConfigs) {
                String pyName = testProgressManager.getTitle(nConfigs);
                double py = switch (metric) {
                    case NMI -> testProgressManager.getPythonNMI(test);
                    case RAND -> testProgressManager.getPythonRandIdx(test);
                    case TIME -> testProgressManager.getPythonTime(test);
                };
                dataSet.addValue(py, pyName, "" + (test + 1));
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(title, xLabel, yLabel, dataSet, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_GRAY);
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setLowerMargin(0.02);
        plot.getDomainAxis().setUpperMargin(0.02);
        plot.getDomainAxis().setCategoryMargin(0.10);

        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setItemMargin(0.0);
        r.setMaximumBarWidth(0.20);
        r.setShadowVisible(false);
        r.setBarPainter(new StandardBarPainter());
        r.setDrawBarOutline(true);
        r.setBaseOutlinePaint(Color.BLACK);

        for (int s = 0; s < dataSet.getRowCount(); s++) {
            r.setSeriesPaint(s, COLORS[s]);
        }

        // Set y-range [0,1]
        if (metric != Metric.TIME) {
            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setRange(0.0, 1.0);
        }

        return new ChartPanel(chart);
    }

    private ChartPanel buildLinePlot(String title, String xLabel, String yLabel, Metric metric, int upToTestIndex) {
        XYSeriesCollection dataSet = new XYSeriesCollection();

        int nConfigs = testProgressManager.getConfigsSize();
        int nTests = upToTestIndex + 1;

        for (int s = 0; s < testProgressManager.getTitleCount(); s++) {
            XYSeries series = new XYSeries(testProgressManager.getTitle(s));
            for (int i = 0; i < nTests; i++) {
                double x = i + 1;
                double y = switch (metric) {
                    case NMI  -> (s < nConfigs ? testProgressManager.getTangleNMI(s, i) : testProgressManager.getPythonNMI(i));
                    case RAND -> (s < nConfigs ? testProgressManager.getTangleRandIndex(s, i) : testProgressManager.getPythonRandIdx(i));
                    case TIME -> (s < nConfigs ? testProgressManager.getTangleTime(s, i) : testProgressManager.getPythonTime(i));
                };
                series.add(x, y);
            }
            dataSet.addSeries(series);
        }

        NumberAxis xAxis = new NumberAxis(xLabel);
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setAutoRangeIncludesZero(false);

        NumberAxis yAxis = new NumberAxis(yLabel);
        if (metric != Metric.TIME) {
            yAxis.setRange(0.0, 1.0);
        }

        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, true);;
        for (int i = 0; i < dataSet.getSeriesCount(); i++) {
            r.setSeriesStroke(i, new BasicStroke(2f));
            r.setSeriesShapesVisible(i, true);
            r.setSeriesPaint(i, COLORS[i]);
        }

        XYPlot plot = new XYPlot(dataSet, xAxis, yAxis, r);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_GRAY);
        plot.setDomainGridlinePaint(GlobalConstants.COLOR_GRAY);

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        return new ChartPanel(chart);
    }

    public void drawEmptyPlots() {
        int nGraphs = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(nGraphs, 1, 5, 5));
        plotPanel.removeAll();
        plotPanel.add(buildEmptyPlot("NMI", "Test", "NMI Score"));
        plotPanel.add(buildEmptyPlot("Rand Index", "Test", "Rand Index Score"));
        if (showTimeCheckBox.isSelected()) plotPanel.add(buildEmptyPlot("Time", "Test", "Time (ms)"));
        plotPanel.revalidate();
        plotPanel.repaint();
    }

    private ChartPanel buildEmptyPlot(String title, String xLabel, String yLabel) {
        XYSeriesCollection empty = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, empty, PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_VERY_LIGHT_GRAY);
        plot.setDomainGridlinePaint(GlobalConstants.COLOR_VERY_LIGHT_GRAY);
        return new ChartPanel(chart);
    }

    public static Color[] getColors(int a) {
        return new Color[]{
                new Color(255, 0, 0, a),       // Red (#FF0000)
                new Color(0, 0, 255, a),       // Blue (#0000FF)
                new Color(0, 128, 0, a),       // Green (#008000)
                new Color(255, 192, 203, a),   // Pink (#FFC0CB)
                new Color(255, 165, 0, a),     // Orange (#FFA500)
                new Color(255, 0, 255, a),     // Magenta (#FF00FF)
                new Color(0, 255, 255, a),     // Cyan (#00FFFF)
                new Color(255, 255, 0, a),     // Yellow (#FFFF00)
                new Color(211, 211, 211, a),   // Light gray (#D3D3D3)
                new Color(128, 128, 128, a),   // Gray (#808080)
                new Color(64, 64, 64, a),      // Dark gray (#404040)
                new Color(128, 0, 128, a),     // Purple (#800080)
                new Color(255, 105, 180, a),   // Hot pink (#FF69B4)
                new Color(0, 128, 128, a),     // Teal (#008080)
                new Color(139, 69, 19, a),     // Saddle brown (#8B4513)
                new Color(75, 0, 130, a),      // Indigo (#4B0082)
                new Color(255, 165, 0, a),     // Orange (duplicate)
                new Color(50, 205, 50, a),     // Lime green (#32CD32)
                new Color(0, 191, 255, a),     // Deep sky blue (#00BFFF)
                new Color(220, 20, 60, a),     // Crimson (#DC143C)
                new Color(255, 215, 0, a),     // Gold (#FFD700)
                new Color(0, 100, 0, a),       // Dark green (#006400)
                new Color(123, 104, 238, a),   // Medium slate blue (#7B68EE)
                new Color(255, 69, 0, a),      // Red-orange (#FF4500)
                new Color(47, 79, 79, a)       // Dark slate gray (#2F4F4F)
        };
    }
}

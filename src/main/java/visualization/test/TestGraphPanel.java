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
import smile.plot.swing.*;
import smile.plot.swing.Canvas;
import util.GlobalConstants;
import visualization.View;

import javax.swing.*;
import java.awt.*;
import java.awt.Shape;
import java.util.Objects;

public class TestGraphPanel extends JPanel {
    private static final Color[] COLORS = getColors();

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
            String pyName = testProgressManager.getTitle(nConfigs);
            double py = switch (metric) {
                case NMI  -> testProgressManager.getPythonNMI(test);
                case RAND -> testProgressManager.getPythonRandIdx(test);
                case TIME -> testProgressManager.getPythonTime(test);
            };
            dataSet.addValue(py, pyName, ""+(test + 1));
        }

        JFreeChart chart = ChartFactory.createBarChart(title, xLabel, yLabel, dataSet, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_GRAY);
        plot.setOutlineVisible(false);

        BarRenderer r = (BarRenderer) plot.getRenderer();
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

        for (int s = 0; s < nConfigs + 1; s++) {
            XYSeries series = new XYSeries(testProgressManager.getTitle(s));
            for (int i = 0; i < nTests; i++) {
                double x = i + 1;
                double y = switch (metric) {
                    case NMI  -> (s < nConfigs ? testProgressManager.getTangleNMI(s, i)      : testProgressManager.getPythonNMI(i));
                    case RAND -> (s < nConfigs ? testProgressManager.getTangleRandIndex(s, i) : testProgressManager.getPythonRandIdx(i));
                    case TIME -> (s < nConfigs ? testProgressManager.getTangleTime(s, i)      : testProgressManager.getPythonTime(i));
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

    public void drawPlotsOld(int testIndex) {
        lastTestIndex = testIndex;

        int nGraphs = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(nGraphs, 1, 5, 5));

        int nTests = testIndex + 1;
        int nTangleResults = testProgressManager.getConfigsSize();
        int pythonIndex = nTangleResults;

        double[][] barPlotNmiScores = new double[nTangleResults + 1][nTests];
        double[][] barPlotRandIdxScores = new double[nTangleResults + 1][nTests];
        double[][] barPlotTimes = new double[nTangleResults + 1][nTests];
        double[][][] linePlotNmiScores = new double[nTangleResults + 1][nTests][2];
        double[][][] linePlotRandScores = new double[nTangleResults + 1][nTests][2];
        double[][][] linePlotTimeScores = new double[nTangleResults + 1][nTests][2];
        for (int i = 0; i < nTests; i++) {
            for (int j = 0; j < nTangleResults + 1; j++) {
                boolean isTangle = j < pythonIndex;

                double nmi = isTangle ? testProgressManager.getTangleNMI(j, i) : testProgressManager.getPythonNMI(i);
                double randIndex = isTangle ? testProgressManager.getTangleRandIndex(j, i) : testProgressManager.getPythonRandIdx(i);
                double time = isTangle ? testProgressManager.getTangleTime(j, i) : testProgressManager.getPythonTime(i);

                barPlotNmiScores[j][i] = nmi;
                barPlotRandIdxScores[j][i] = randIndex;
                barPlotTimes[j][i] = time;

                linePlotNmiScores[j][i][0] = i + 1;
                linePlotRandScores[j][i][0] = i + 1;
                linePlotTimeScores[j][i][0] = i + 1;
                linePlotNmiScores[j][i][1] = nmi;
                linePlotRandScores[j][i][1] = randIndex;
                linePlotTimeScores[j][i][1] = time;
            }
        }

        Line[] nmiLines = new Line[nTangleResults + 1];
        Line[] randLines = new Line[nTangleResults + 1];
        Line[] timeLines = new Line[nTangleResults + 1];
        for (int i = 0; i < nTangleResults + 1; i++) {
            Line.Style style = Line.Style.SOLID;
            char mark = linePlotTimeScores[i][0][1] == 0 ? ' ' : '@';
            Color color = COLORS[i];

            nmiLines[i] = new Line(linePlotNmiScores[i], style, mark, color);
            randLines[i] = new Line(linePlotRandScores[i], style, mark, color);
            timeLines[i] = new Line(linePlotTimeScores[i], style, mark, color);
        }

        String plotStyle = (String) plotStyleComboBox.getSelectedItem();
        boolean barPlotStyle = plotStyle != null && plotStyle.equals("Bar Plot");

        String[] l = new String[nTangleResults + 1];
        Legend[] legends = new Legend[nTangleResults + 1];
        for (int i = 0; i < nTangleResults + 1; i++) {
            l[i] = testProgressManager.getTitle(i);
            legends[i] = new Legend(l[i], COLORS[i]);
        }

        Plot nmiPlot = barPlotStyle ?
                BarPlot.of(barPlotNmiScores, l) :
                new LinePlot(nmiLines, legends);
        Plot randIdxPlot = barPlotStyle ?
                BarPlot.of(barPlotRandIdxScores, l) :
                new LinePlot(randLines, legends);
        Plot timePlot = barPlotStyle ?
                BarPlot.of(barPlotTimes, l) :
                new LinePlot(timeLines, legends);

        Figure nmiFig = nmiPlot.figure();
        Figure randFig = randIdxPlot.figure();
        Figure timeFig = timePlot.figure();

        // X-axis should center bars around 1, 2, 3,... and Y_axis always between [0,1]
        double xMin = barPlotStyle ? -0.5 : 0.9;
        double xMax = nTests == 1 ? 1.5 : nTests + (barPlotStyle ? 0.5: 0.1);   // Weird edge case where everything blows up if there is only one tick
        double yMin = 0.0;
        double yMax = 1.0;
        double[] minBounds = new double[] { xMin, yMin };
        double[] maxBounds = new double[] { xMax, yMax };

        double maxTime = 0;
        for (int i = 0; i < barPlotTimes.length; i++) {
            for (int j = 0; j < barPlotTimes[i].length; j++) {
                maxTime = Math.max(maxTime, barPlotTimes[i][j]);
            }
        }

        nmiFig.setBound(minBounds, maxBounds);
        randFig.setBound(minBounds, maxBounds);
        timeFig.setBound(minBounds, new double[] { xMax, maxTime + 0.5 });

        nmiFig.setAxisLabels("Test", "NMI Score");
        randFig.setAxisLabels("Test", "Rand Index Score");
        timeFig.setAxisLabels("Test", "Time (seconds)");

        nmiFig.getAxis(0).setGridVisible(false);
        randFig.getAxis(0).setGridVisible(false);
        timeFig.getAxis(0).setGridVisible(false);

        String[] labels = new String[nTests];
        double[] locations = new double[nTests];
        if (barPlotStyle) {
            double barPlotShift = 0.375;
            if (nTests <= 1) {
                labels = new String[]{"1", ""};
                locations = new double[]{barPlotShift, 1 + barPlotShift};

            } else {
                for (int i = 0; i < nTests; i++) {
                    labels[i] = "" + (i + 1);
                    locations[i] = (double) i + barPlotShift;  // Center between the two bars
                }
            }
        } else {
            int m = nTests == 1 ? 2 : nTests;
            labels = new String[m];
            locations = new double[m];
            for (int i = 0; i < m; i++) {
                labels[i] = "" + (i + 1);
                locations[i] = i + 1;
            }
            if (nTests == 1) labels[nTests] = "";
        }
        nmiFig.getAxis(0).setTicks(labels, locations);
        randFig.getAxis(0).setTicks(labels, locations);
        timeFig.getAxis(0).setTicks(labels, locations);

        Canvas nmiCanvas = new Canvas(nmiFig);
        Canvas randCanvas = new Canvas(randFig);
        Canvas timeCanvas = new Canvas(timeFig);

        plotPanel.removeAll();

        plotPanel.add(nmiCanvas);
        plotPanel.add(randCanvas);
        if (showTimeCheckBox.isSelected()) plotPanel.add(timeCanvas);

        plotPanel.revalidate();
        plotPanel.repaint();
    }

    public void drawEmptyPlotsOld() {
        int rows = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(rows, 1, 5, 5));

        BarPlot nmiPlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"", ""});
        BarPlot randPlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"", ""});
        BarPlot timePlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"", ""});

        Figure nmiFig = nmiPlot.figure();
        Figure randFig = randPlot.figure();
        Figure timeFig = timePlot.figure();

        // Make room for second tick or everything blows up
        nmiFig.setBound(new double[]{-0.5, 0.5}, new double[]{1.5, 1.0});
        nmiFig.setAxisLabels("Test", "NMI Score");
        nmiFig.getAxis(0).setTicks(new String[]{"", ""}, new double[]{0.375, 1.375});

        randFig.setBound(new double[]{-0.5, 0.5}, new double[]{1.5, 1.0});
        randFig.setAxisLabels("Test", "Rand Index Score");
        randFig.getAxis(0).setTicks(new String[]{"", ""}, new double[]{0.375, 1.375});

        timeFig.setBound(new double[]{-0.5, 0.5}, new double[]{1.5, 1.0});
        timeFig.setAxisLabels("Test", "Time (seconds)");
        timeFig.getAxis(0).setTicks(new String[]{"", ""}, new double[]{0.375, 1.375});

        Canvas nmiCanvas = new Canvas(nmiFig);
        Canvas randCanvas = new Canvas(randFig);
        Canvas timeCanvas = new Canvas(timeFig);

        plotPanel.removeAll();

        plotPanel.add(nmiCanvas);
        plotPanel.add(randCanvas);
        if (showTimeCheckBox.isSelected()) plotPanel.add(timeCanvas);

        plotPanel.revalidate();
        plotPanel.repaint();
    }

    private static Color[] getColors() {
        int a = 100;

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

package visualization.test;

import smile.plot.swing.*;
import smile.plot.swing.Canvas;
import visualization.View;

import javax.swing.*;
import java.awt.*;

public class TestGraphPanel extends JPanel {
    private View view;

    private TestEditPanel.TestProgressManager testProgressManager;

    private final JComboBox<String> plotStyleComboBox = new JComboBox<>(new String[] {"Bar Plot", "Line Plot"});
    private final JCheckBox showTimeCheckBox = new JCheckBox("Show time plot", false);

    private final JPanel componentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
    private final JPanel plotPanel = new JPanel(new GridLayout(2, 1, 5, 5));

    private int lastLow = 0;
    private int lastHigh = 0;

    public TestGraphPanel(View view, TestEditPanel.TestProgressManager testProgressManager) {
        this.view = view;
        this.testProgressManager = testProgressManager;

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
            drawPlots(lastLow, lastHigh);
        });

        showTimeCheckBox.addActionListener(e -> {
            drawPlots(lastLow, lastHigh);
        });
    }

    public void drawPlots(int low, int high) {
        lastLow = low;
        lastHigh = high;

        int nGraphs = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(nGraphs, 1, 5, 5));

        int nTests = high - low + 1;
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

        Color[] colors = generateColors();

        Line[] nmiLines = new Line[nTangleResults + 1];
        Line[] randLines = new Line[nTangleResults + 1];
        Line[] timeLines = new Line[nTangleResults + 1];
        for (int i = 0; i < nTangleResults + 1; i++) {
            Line.Style style = Line.Style.SOLID;
            char mark = linePlotTimeScores[i][0][1] == 0 ? ' ' : '@';
            Color color = colors[i];

            nmiLines[i] = new Line(linePlotNmiScores[i], style, mark, color);
            randLines[i] = new Line(linePlotRandScores[i], style, mark, color);
            timeLines[i] = new Line(linePlotTimeScores[i], style, mark, color);
        }

        String plotStyle = (String) plotStyleComboBox.getSelectedItem();
        boolean barPlotStyle = plotStyle != null && plotStyle.equals("Bar Plot");

        String[] l = new String[nTangleResults + 1];
        Legend[] legends = new Legend[nTangleResults + 1];
        for (int i = 0; i < nTangleResults + 1; i++) {
            boolean isTangle = i < nTangleResults;

            l[i] = testProgressManager.getTitle(i);
            legends[i] = new Legend(l[i], colors[i]);
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
        for (int i = 0; i < 2; i++) {
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

    public void drawEmptyPlots() {
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

    private Color[] generateColors() {
        return new Color[] {
                Color.RED,
                Color.BLUE,
                Color.GREEN,
                Color.PINK,
                Color.ORANGE,
                Color.MAGENTA,
                Color.CYAN,
                Color.YELLOW,
                Color.LIGHT_GRAY,
                Color.GRAY,
                Color.DARK_GRAY,
                new Color(128, 0, 128),     // Purple
                new Color(255, 105, 180),   // Hot pink
                new Color(0, 128, 128),     // Teal
                new Color(139, 69, 19),     // Saddle brown
                new Color(75, 0, 130),      // Indigo
                new Color(255, 165, 0),     // Orange
                new Color(50, 205, 50),     // Lime green
                new Color(0, 191, 255),     // Deep sky blue
                new Color(220, 20, 60),     // Crimson
                new Color(255, 215, 0),     // Gold
                new Color(0, 100, 0),       // Dark green
                new Color(123, 104, 238),   // Medium slate blue
                new Color(255, 69, 0),      // Red-orange
                new Color(47, 79, 79)       // Dark slate gray
        };
    }
}

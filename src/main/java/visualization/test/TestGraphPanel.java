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

        int rows = showTimeCheckBox.isSelected() ? 3 : 2;
        plotPanel.setLayout(new GridLayout(rows, 1, 5, 5));

        int n = high - low + 1;

        double[][] barPlotNmiScores = new double[2][n];
        double[][] barPlotRandIdxScores = new double[2][n];
        double[][] barPlotTimes = new double[2][n];
        double[][][] linePlotNmiScores = new double[2][n][2];
        double[][][] linePlotRandScores = new double[2][n][2];
        double[][][] linePlotTimeScores = new double[2][n][2];
        for (int i = 0; i < n; i++) {
            int idx = low + i;

            for (int j = 0; j < 2; j++) {
                boolean tangle = j == 0;
                barPlotNmiScores[j][i] = testProgressManager.getNMI(idx, tangle);
                barPlotRandIdxScores[j][i] = testProgressManager.getRandIdx(idx, tangle);
                barPlotTimes[j][i] = testProgressManager.getTime(idx, tangle);

                linePlotNmiScores[j][i][0] = i + 1;
                linePlotRandScores[j][i][0] = i + 1;
                linePlotTimeScores[j][i][0] = i + 1;
                linePlotNmiScores[j][i][1] = testProgressManager.getNMI(idx, tangle);
                linePlotRandScores[j][i][1] = testProgressManager.getRandIdx(idx, tangle);
                linePlotTimeScores[j][i][1] = testProgressManager.getTime(idx, tangle);
            }
        }

        Line[] nmiLines = new Line[2];
        Line[] randLines = new Line[2];
        Line[] timeLines = new Line[2];
        for (int i = 0; i < 2; i++) {
            boolean tangles = i == 0;
            Line.Style style = Line.Style.SOLID;
            char mark = linePlotTimeScores[i][0][1] == 0 ? ' ' : '@';
            Color color = i == 0 ? Color.RED : Color.BLUE;

            nmiLines[i] = new Line(linePlotNmiScores[i], style, mark, color);
            randLines[i] = new Line(linePlotRandScores[i], style, mark, color);
            timeLines[i] = new Line(linePlotTimeScores[i], style, mark, color);
        }

        String plotStyle = (String) plotStyleComboBox.getSelectedItem();
        boolean barPlotStyle = plotStyle != null && plotStyle.equals("Bar Plot");

        String[] l = new String[]{"Tangle", "Python"};
        Legend[] legends = new Legend[] { new Legend(l[0], Color.RED), new Legend(l[1], Color.BLUE) };
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
        double xMin = -0.5;
        double xMax = n == 1 ? 1.5 : n + 0.5;   // Weird edge case where everything blows up if there is only one tick
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

        if (barPlotStyle) {
            if (n <= 1) {
                String[] labels = new String[]{"1", ""};
                double[] locations = new double[]{0.375, 1.375};
                nmiFig.getAxis(0).setTicks(labels, locations);
                randFig.getAxis(0).setTicks(labels, locations);
                timeFig.getAxis(0).setTicks(labels, locations);

            } else {
                String[] labels = new String[n];
                double[] locations = new double[n];
                for (int i = 0; i < n; i++) {
                    labels[i] = "" + (i + 1);
                    locations[i] = (double) i + 0.375;  // Center between the two bars
                }
                nmiFig.getAxis(0).setTicks(labels, locations);
                randFig.getAxis(0).setTicks(labels, locations);
                timeFig.getAxis(0).setTicks(labels, locations);
            }
        }

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

        BarPlot nmiPlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"Tangle", "Python"});
        BarPlot randPlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"Tangle", "Python"});
        BarPlot timePlot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"Tangle", "Python"});

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
}

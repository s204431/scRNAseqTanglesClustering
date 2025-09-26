package visualization.test;

import smile.plot.swing.*;
import smile.plot.swing.Canvas;
import visualization.View;

import javax.swing.*;
import java.awt.*;

public class TestGraphPanel extends JPanel {
    private View view;

    private TestEditPanel.TestProgressManager testProgressManager;

    public TestGraphPanel(View view, TestEditPanel.TestProgressManager testProgressManager) {
        this.view = view;
        this.testProgressManager = testProgressManager;

        setLayout(new GridLayout(3, 1, 5, 5));
        drawEmptyHistogram();
    }

    public void drawHistogram(int low, int high) {
        int n = high - low + 1;

        double[][] nmiScores = new double[2][n];
        double[][] randIdxScores = new double[2][n];
        double[][] times = new double[2][n];
        for (int i = 0; i < n; i++) {
            int idx = low + i;
            nmiScores[0][i] = testProgressManager.getNMI(idx, true);
            nmiScores[1][i] = testProgressManager.getNMI(idx, false);
            randIdxScores[0][i] = testProgressManager.getRandIdx(idx, true);
            randIdxScores[1][i] = testProgressManager.getRandIdx(idx, false);
            times[0][i] = testProgressManager.getTime(idx, true);
            times[1][i] = testProgressManager.getTime(idx, false);
        }

        removeAll();

        String[] l = new String[]{"Tangle", "Python"};
        BarPlot nmiPlot = BarPlot.of(nmiScores, l);
        BarPlot randIdxPlot = BarPlot.of(randIdxScores, l);
        BarPlot timePlot = BarPlot.of(times, l);

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
            for (int j = 0; j < times[i].length; j++) {
                maxTime = Math.max(maxTime, times[i][j]);
            }
        }

        nmiFig.setBound(minBounds, maxBounds);
        randFig.setBound(minBounds, maxBounds);
        timeFig.setBound(minBounds, new double[] { xMax, maxTime + 0.5 });

        nmiFig.setAxisLabels("Test", "NMI Score");
        randFig.setAxisLabels("Test", "Rand Index Score");
        timeFig.setAxisLabels("Test", "Time (seconds)");

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
                labels[i] = "" + (i+1);
                locations[i] = (double) i + 0.375;  // Center between the two bars
            }
            nmiFig.getAxis(0).setTicks(labels, locations);
            randFig.getAxis(0).setTicks(labels, locations);
            timeFig.getAxis(0).setTicks(labels, locations);
        }

        Canvas nmiCanvas = new Canvas(nmiFig);
        Canvas randCanvas = new Canvas(randFig);
        Canvas timeCanvas = new Canvas(timeFig);

        add(nmiCanvas);
        add(randCanvas);
        add(timeCanvas);

        revalidate();
        repaint();
    }

    public void drawEmptyHistogram() {
        removeAll();

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
        add(nmiCanvas);

        Canvas randCanvas = new Canvas(randFig);
        add(randCanvas);

        Canvas timeCanvas = new Canvas(timeFig);
        add(timeCanvas);

        revalidate();
        repaint();
    }
}

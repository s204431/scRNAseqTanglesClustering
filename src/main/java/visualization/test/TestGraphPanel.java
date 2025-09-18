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

        setLayout(new GridLayout(2, 2, 5, 5));
        drawEmptyHistogram();
    }

    public void drawHistogram(int low, int high) {
        int n = high - low + 1;

        double[][] nmiScores = new double[2][n];
        double[][] randIdxScores = new double[2][n];
        for (int i = 0; i < n; i++) {
            int idx = low + i;
            nmiScores[0][i] = testProgressManager.getNMI(idx, true);
            nmiScores[1][i] = testProgressManager.getNMI(idx, false);
            randIdxScores[0][i] = testProgressManager.getRandIdx(idx, true);
            randIdxScores[1][i] = testProgressManager.getRandIdx(idx, false);
        }

        removeAll();

        String[] l = new String[]{"Tangle", "Python"};
        BarPlot nmiPlot = BarPlot.of(nmiScores, l);
        BarPlot randIdxPlot = BarPlot.of(randIdxScores, l);

        Figure nmiFig = nmiPlot.figure();
        Figure randFig = randIdxPlot.figure();

        // X-axis should center bars around 1, 2, 3,... and Y_axis always between [0,1]
        double xMin = -0.5;
        double xMax = n == 1 ? 1.5 : n + 0.5;   // Weird edge case where everything blows up if there is only one tick
        double yMin = 0.0;
        double yMax = 1.0;
        double[] minBounds = new double[] { xMin, yMin };
        double[] maxBounds = new double[] { xMax, yMax };

        nmiFig.setBound(minBounds, maxBounds);
        randFig.setBound(minBounds, maxBounds);
        nmiFig.setAxisLabels("Test", "NMI Score");
        randFig.setAxisLabels("Test", "Rand Index Score");

        if (n <= 1) {
            String[] labels = new String[]{"1", ""};
            double[] locations = new double[]{0.375, 1.375};
            nmiFig.getAxis(0).setTicks(labels, locations);
            randFig.getAxis(0).setTicks(labels, locations);

        } else {
            String[] labels = new String[n];
            double[] locations = new double[n];
            for (int i = 0; i < n; i++) {
                labels[i] = "" + (i+1);
                locations[i] = (double) i + 0.375;  // Center between the two bars
            }
            nmiFig.getAxis(0).setTicks(labels, locations);
            randFig.getAxis(0).setTicks(labels, locations);
        }

        Canvas nmiCanvas = new Canvas(nmiFig);
        Canvas randCanvas = new Canvas(randFig);

        add(nmiCanvas);
        add(randCanvas);

        revalidate();
        repaint();
    }

    public void drawEmptyHistogram() {
        removeAll();

        BarPlot plot = BarPlot.of(new double[][]{{0}, {0}}, new String[]{"Tangle", "Python"});
        Figure fig = plot.figure();

        // Make room for second tick or everything blows up
        fig.setBound(new double[]{-0.5, 0.5}, new double[]{1.5, 1.0});
        fig.setAxisLabels("Test", "NMI Score");
        fig.getAxis(0).setTicks(new String[]{"", ""}, new double[]{0.375, 1.375});

        Canvas canvas = new Canvas(fig);
        add(canvas, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

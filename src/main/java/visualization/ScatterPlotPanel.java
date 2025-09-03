package visualization;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;


import javax.swing.*;
import java.awt.*;

public class ScatterPlotPanel extends JPanel {
    private View view;

    private static Color[] COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};

    public ScatterPlotPanel(View view) {
        this.view = view;

        setLayout(new BorderLayout());
        setBackground(Color.LIGHT_GRAY);
    }

    public void drawScatterPlot(double[][] points) {
        removeAll();
        Canvas scatterPlot = new Canvas(ScatterPlot.of(points, 'o').figure());
        add(scatterPlot, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void drawClusters(double[][] points, int[] clusters) {
        if (clusters.length != points.length) {
            System.out.println("Cluster length does not match points length in ScatterPlotPanel. Maybe use setPoints()");
            return;
        }

        double[] Xs = new double[points.length];
        double[] Ys = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            Xs[i] = points[i][0];
            Ys[i] = points[i][1];
        }

        DataFrame data = new DataFrame(
                new DoubleVector("X", Xs),
                new DoubleVector("Y", Ys),
                new IntVector("cluster", clusters)
        );

        removeAll();
        Canvas scatterPlot = new Canvas(ScatterPlot.of(data, "X", "Y", "cluster", 'o').figure());
        add(scatterPlot, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

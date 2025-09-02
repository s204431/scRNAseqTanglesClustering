package visualization;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;


import javax.swing.*;
import java.awt.*;

public class ScatterPlotPanel extends JPanel {
    private static Color[] COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};

    private double[][] points = new double[][] {{0,0}};
    private int[] clusters;

    public ScatterPlotPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.LIGHT_GRAY);

        drawScatterPlot();
    }

    public void drawScatterPlot() {
        removeAll();
        Canvas scatterPlot = new Canvas(ScatterPlot.of(points).figure());
        add(scatterPlot, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void drawClusters() {
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
        Canvas scatterPlot = new Canvas(ScatterPlot.of(data, "X", "Y", "cluster", 'O').figure());
        add(scatterPlot, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void setPoints(double[][] points) {
        this.points = points;
    }

    public void setClusters(int[] clusters) {
        this.clusters = clusters;
    }

}

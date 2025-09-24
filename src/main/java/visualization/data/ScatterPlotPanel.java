package visualization.data;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.plot.swing.Canvas;
import smile.plot.swing.Figure;
import smile.plot.swing.ScatterPlot;
import visualization.View;


import javax.swing.*;
import java.awt.*;

public class ScatterPlotPanel extends JPanel {
    private View view;

    private static Color[] COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};

    private static final boolean SHOW_GRID = true;

    public ScatterPlotPanel(View view) {
        this.view = view;

        setLayout(new BorderLayout());
        setBackground(Color.LIGHT_GRAY);
    }

    public void drawScatterPlot(double[][] points) {
        removeAll();
        ScatterPlot plot = ScatterPlot.of(points, 'o');
        Figure figure = plot.figure();

        figure.setAxisLabels("", "");
        figure.getAxis(0).setGridVisible(SHOW_GRID);
        figure.getAxis(1).setGridVisible(SHOW_GRID);

        Canvas canvas = new Canvas(figure);
        add(canvas, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void drawClusters(double[][] points, int[] clusters) {
        if (clusters == null || clusters.length != points.length) {
            drawScatterPlot(points);
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

        ScatterPlot plot = ScatterPlot.of(data, "X", "Y", "cluster", 'o');
        Figure fig = plot.figure();

        fig.setAxisLabels("", "");
        fig.getAxis(0).setGridVisible(SHOW_GRID);
        fig.getAxis(1).setGridVisible(SHOW_GRID);

        Canvas canvas = new Canvas(fig);
        add(canvas, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

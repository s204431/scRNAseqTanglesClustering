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

public class ScatterPlotPanel extends JTabbedPane {
    private View view;

    private static final int POINTS_IDX = 0;
    private static final int GROUND_TRUTH_IDX = 1;
    private static final int CUT_IDX = 2;

    private static final boolean SHOW_GRID = true;
    private static final char MARK = 'o';

    private int tangleCounter = 0;

    public ScatterPlotPanel(View view) {
        this.view = view;
        setBackground(new Color(230, 230, 230));
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public void initialize(double[][] points, int[] groundTruth) {
        drawScatterPlot(points);
        if (groundTruth != null) drawGroundTruth(points, groundTruth);
        setSelectedIndex(0);
    }

    public void drawScatterPlot(double[][] points) {
        ScatterPlot plot = ScatterPlot.of(points, MARK);
        Figure figure = plot.figure();

        figure.setAxisLabels("t-SNE1", "t-SNE2");
        figure.getAxis(0).setGridVisible(SHOW_GRID);
        figure.getAxis(1).setGridVisible(SHOW_GRID);

        Canvas canvas = new Canvas(figure);
        add("Points", canvas);
        setSelectedIndex(getTabCount() - 1);
    }

    public void drawCut(double[][] points, int[] clustering) {
        drawClusters(points, clustering, "Cut");
    }

    public void drawGroundTruth(double[][] points, int[] groundTruth) {
        drawClusters(points, groundTruth, "Ground Truth");
    }

    public void drawClusters(double[][] points, int[] clusters, boolean tangle) {
        String title = "Scanpy";
        if (tangle) {
            title = "Tangle " + (++tangleCounter);
        }
        drawClusters(points, clusters, title);
    }

    public void drawClusters(double[][] points, int[] clusters, String title) {
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

        ScatterPlot plot = ScatterPlot.of(data, "X", "Y", "cluster", MARK);
        Figure fig = plot.figure();

        fig.setAxisLabels("t-SNE1", "t-SNE2");
        fig.getAxis(0).setGridVisible(SHOW_GRID);
        fig.getAxis(1).setGridVisible(SHOW_GRID);

        Canvas canvas = new Canvas(fig);

        if (title.equals("Ground Truth")) {
            int i = indexOfTab(title);
            if (i >= 0) {
                setComponentAt(i, canvas);
                setSelectedIndex(i);
            } else {
                insertTab(title, null, canvas, null, GROUND_TRUTH_IDX);
                setSelectedIndex(GROUND_TRUTH_IDX);
            }
        }

        else if (title.equals("Cut")) {
            int i = indexOfTab(title);
            if (i >= 0) {
                setComponentAt(i, canvas);
                setSelectedIndex(i);
            } else {
                int newI = getComponentAt(GROUND_TRUTH_IDX).toString().equals("Ground Truth") ? GROUND_TRUTH_IDX : CUT_IDX;
                insertTab(title, null, canvas, null, newI);
                setTabComponentAt(newI, makeTabHeader(title));
                setSelectedIndex(newI);
            }

        } else {
            addClosableTab(title, canvas);
        }
    }

    public void removeAllTabs() {
        while (getTabCount() > 0) {
            remove(0);
        }
        tangleCounter = 0;
    }

    public void addClosableTab(String title, Component comp) {
        super.addTab(title, comp);
        int idx = getTabCount() - 1;
        setTabComponentAt(idx, makeTabHeader(title));
        setSelectedIndex(idx);
    }

    private Component makeTabHeader(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);

        JLabel lbl = new JLabel(title);

        JButton close = new JButton(new CrossIcon(10, 2f));
        close.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.setRolloverEnabled(true);
        close.setToolTipText("Close");
        close.addActionListener(e -> {
            int i = indexOfTabComponent(p);
            if (i != -1) removeTabAt(i);
        });

        p.add(lbl);
        p.add(close);
        return p;
    }

    private class CrossIcon implements Icon {
        private final int size;
        private final float stroke;
        CrossIcon(int size, float stroke) { this.size = size; this.stroke = stroke; }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean rollover = c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover();

            // Change color when hovering
            Color col = rollover ? new Color(200, 50, 50) : new Color(100, 115, 130);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int pad = 2;
            int w = size - pad * 2;
            int x1 = x + pad, y1 = y + pad, x2 = x + pad + w, y2 = y + pad + w;
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x1, y2, x2, y1);
            g2.dispose();
        }
    }
}

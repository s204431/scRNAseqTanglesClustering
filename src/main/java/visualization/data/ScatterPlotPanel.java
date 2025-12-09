package visualization.data;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import util.ClusteringIO;
import util.Config;
import util.GlobalConstants;
import visualization.View;

import java.awt.geom.Ellipse2D;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ScatterPlotPanel extends JTabbedPane {
    private View view;

    // Tab titles
    private static final String POINTS_TITLE = "Points";
    private static final String GROUND_TRUTH_TITLE = "Ground Truth";
    private static final String CUT_TITLE = "Cut";
    private static final String SCANPY_TITLE = "Scanpy";

    // Property titles
    private static final String PROPERTY_TITLE = "title";
    private static final String INDEX_TITLE = "index";
    private static final String HARD_CLUSTER_TITLE = "clusters";
    private static final String SOFT_CLUSTER_TITLE = "soft_clustering";
    private static final String CLUSTER_TIME_TITLE = "cluster_time";
    private static final String CONFIG_TITLE = "config";

    // Tab indices for non-user generated tabs
    private static final int POINTS_IDX = 0;
    private static final int GROUND_TRUTH_IDX = 1;
    private static final int CUT_IDX = 2;

    private int attachmentIndex = 0;
    private int tangleCounter = 0;

    private JComponent tempComponentHolder = null;
    private int lastSelectedIndex = -1;

    public ScatterPlotPanel(View view) {
        this.view = view;
        setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);    // Should differ only a little from white
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        addSaveActions();
        addSelectionChangeListener();
    }

    public void initialize(double[][] points, int[] groundTruth) {
        drawScatterPlot(points);
        if (groundTruth != null) drawGroundTruth(points, groundTruth);
        setSelectedIndex(0);
    }

    public void drawScatterPlot(double[][] points) {
        XYSeries series = new XYSeries("Points", false, true);
        for (double[] p : points) series.add(p[0], p[1], false);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        NumberAxis xAxis = new NumberAxis("t-SNE1");
        NumberAxis yAxis = new NumberAxis("t-SNE2");
        xAxis.setAutoRangeIncludesZero(false);
        yAxis.setAutoRangeIncludesZero(false);

        double radiusPx = 3;
        Shape circle = new Ellipse2D.Double(-radiusPx, -radiusPx, 2*radiusPx, 2*radiusPx);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, circle);

        renderer.setSeriesPaint(0, GlobalConstants.COLOR_DARK_GRAY);
        renderer.setUseOutlinePaint(true);
        renderer.setSeriesOutlinePaint(0, Color.BLACK);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(0.75f));

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(GlobalConstants.COLOR_ALMOST_WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        chart.setRenderingHints(hints);

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setOpaque(true);
        panel.setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);

        insertTab(panel, POINTS_TITLE, null, null);
        setSelectedIndex(getTabCount() - 1);
    }

    public void drawCut(double[][] points, int[] clustering) {
        ChartPanel panel = drawClusters(points, clustering);
        insertTab(panel, CUT_TITLE, clustering, null);
    }

    public void drawGroundTruth(double[][] points, int[] groundTruth) {
        ChartPanel panel = drawClusters(points, groundTruth);
        insertTab(panel, GROUND_TRUTH_TITLE, groundTruth, null);
    }

    public void drawClusters(double[][] points, int[] hardClusters, double[][] softClusters, boolean tangle) {
        String title = tangle ? "Tangle" + (++tangleCounter) : SCANPY_TITLE;

        ChartPanel panel;
        if (tangle && softClusters != null) panel = drawSoftClustering(points, softClusters);
        else panel = drawClusters(points, hardClusters);
        insertTab(panel, title, hardClusters, softClusters);
    }

    public ChartPanel drawClusters(double[][] points, int[] clusters) {
        if (clusters == null || clusters.length != points.length) {
            drawScatterPlot(points);
            return null;
        }

        // Find clusters
        HashSet<Integer> clusterSet = new HashSet<>();
        for (int c : clusters) clusterSet.add(c);

        // One series per cluster
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int c : clusterSet.stream().toList()) {
            XYSeries s = new XYSeries("Cluster " + c, false, true);
            for (int i = 0; i < points.length; i++) {
                if (clusters[i] == c) s.add(points[i][0], points[i][1], false);
            }
            dataset.addSeries(s);
        }

        NumberAxis xAxis = new NumberAxis("t-SNE1");
        NumberAxis yAxis = new NumberAxis("t-SNE2");
        xAxis.setAutoRangeIncludesZero(false);
        yAxis.setAutoRangeIncludesZero(false);

        double radiusPx = 3;
        Shape circle = new Ellipse2D.Double(-radiusPx, -radiusPx, 2*radiusPx, 2*radiusPx);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        Paint[] palette = GlobalConstants.CLUSTER_COLORS;

        for (int c = 0; c < clusterSet.size(); c++) {
            renderer.setSeriesLinesVisible(c, false);
            renderer.setSeriesShapesVisible(c, true);
            renderer.setSeriesShape(c, circle);

            Paint fillPaint = palette[c % palette.length];
            renderer.setSeriesPaint(c, fillPaint);

            // Outline
            renderer.setUseOutlinePaint(true);
            Color fill = (Color) fillPaint;
            Color outline = new Color(
                    (int)(fill.getRed() * 0.8),
                    (int)(fill.getGreen() * 0.8),
                    (int)(fill.getBlue() * 0.8)
            );
            renderer.setSeriesOutlinePaint(c, outline);
            renderer.setSeriesOutlineStroke(c, new BasicStroke(1f));
        }

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(GlobalConstants.COLOR_ALMOST_WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        chart.setRenderingHints(hints);

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setOpaque(true);
        panel.setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);

        return panel;
    }

    // Renders per-item alpha using provided probabilities for each series
    private static final class SoftClusterRenderer extends XYLineAndShapeRenderer {
        private final List<double[]> perSeriesProbs;

        SoftClusterRenderer(List<double[]> perSeriesProbs) {
            super(false, true);
            this.perSeriesProbs = perSeriesProbs;
        }

        @Override
        public Paint getItemPaint(int series, int item) {
            Paint base = getSeriesPaint(series);
            Color c = (base instanceof Color) ? (Color) base : Color.GRAY;

            double p = 0.0;
            if (series >= 0 && series < perSeriesProbs.size()) {
                double[] arr = perSeriesProbs.get(series);
                if (item >= 0 && item < arr.length) {
                    p = arr[item];
                }
            }

            double threshold = 0.5;
            double minAlpha = 50;
            double maxAlpha = 255;
            int alpha = (int) minAlpha;

            // Map the range [threshold,1] to [minAlpha,maxAlpha]
            if (p > threshold) {
                double scaledP = (p - threshold) / (1 - threshold);
                alpha = (int) Math.max(minAlpha, Math.round(maxAlpha * scaledP));
            }

            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
    }

    // Draws a soft clustering where each cluster has a color
    // and the alpha and gamma values are adjusted based on the probabilities.
    public ChartPanel drawSoftClustering(double[][] points, double[][] softClustering) {
        int n = points.length;
        int k = softClustering[0].length;

        // Recreate hard clustering from the soft clustering
        int[] clustering = new int[softClustering.length];
        for (int i = 0; i < softClustering.length; i++) {
            int maxIdx = 0;
            double maxVal = softClustering[i][0];
            for (int j = 1; j < softClustering[i].length; j++) {
                if (softClustering[i][j] > maxVal) {
                    maxVal = softClustering[i][j];
                    maxIdx = j;
                }
            }
            clustering[i] = maxIdx;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        List<double[]> perSeriesProbs = new ArrayList<>(k);

        HashSet<Integer> clusterSet = new HashSet<>();
        for (int c : clustering) clusterSet.add(c);

        for (int c : clusterSet) {
            XYSeries series = new XYSeries("Cluster " + c, false, true);

            // Collect points for this cluster, and the matching probabilities
            List<Double> probsList = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (clustering[i] == c) {
                    series.add(points[i][0], points[i][1], false);
                    probsList.add(softClustering[i][c]);
                }
            }

            dataset.addSeries(series);

            double[] probsForC = new double[probsList.size()];
            for (int j = 0; j < probsList.size(); j++) probsForC[j] = probsList.get(j);
            perSeriesProbs.add(probsForC);
        }

        NumberAxis xAxis = new NumberAxis("t-SNE1");
        NumberAxis yAxis = new NumberAxis("t-SNE2");
        xAxis.setAutoRangeIncludesZero(false);
        yAxis.setAutoRangeIncludesZero(false);

        SoftClusterRenderer renderer = new SoftClusterRenderer(perSeriesProbs);
        renderer.setUseOutlinePaint(true);

        Paint[] paints = GlobalConstants.CLUSTER_COLORS;
        double radiusPx = 3;
        Shape circle = new Ellipse2D.Double(-radiusPx, -radiusPx, 2*radiusPx, 2*radiusPx);
        for (int c = 0; c < clusterSet.size(); c++) {
            renderer.setSeriesPaint(c, paints[c % paints.length]);
            renderer.setSeriesLinesVisible(c, false);
            renderer.setSeriesShapesVisible(c, true);
            renderer.setSeriesShape(c, circle);

            // Draw darkened outline for each point
            Color fill = (Color) renderer.getSeriesPaint(c);
            double factor = 0.8;
            Color outline = new Color(
                    (int)(fill.getRed() * factor),
                    (int)(fill.getGreen() * factor),
                    (int)(fill.getBlue() * factor)
            );
            renderer.setSeriesOutlinePaint(c, outline);
            renderer.setSeriesOutlineStroke(c, new BasicStroke(1f));
        }

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(GlobalConstants.COLOR_ALMOST_WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setRangeGridlinePaint(GlobalConstants.COLOR_LIGHT_GRAY);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        chart.setRenderingHints(hints);

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setOpaque(true);
        panel.setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);

        return panel;
    }

    public void updateClustering(double[][] points, double[][] softClustering) {
        ChartPanel panel = drawSoftClustering(points, softClustering);
        updateTab(panel);
    }

    public void insertTab(JComponent component, String title, int[] hardClustering, double[][] softClustering) {
        attachTabData(component, title, hardClustering, softClustering);

        if (title.equals(GROUND_TRUTH_TITLE) || title.equals(CUT_TITLE)) {
            int i = indexOfTab(title);
            if (i >= 0) {
                setComponentAt(i, component);
                setSelectedIndex(i);
            } else {
                int insertAt = title.equals(GROUND_TRUTH_TITLE) ? GROUND_TRUTH_IDX
                        : (getTabCount() > GROUND_TRUTH_IDX && getTitleAt(GROUND_TRUTH_IDX).equals(GROUND_TRUTH_TITLE)
                        ? GROUND_TRUTH_IDX : CUT_IDX);
                insertTab(title, null, component, null, insertAt);
                setTabComponentAt(insertAt, makeTabHeader(title));
                setSelectedIndex(insertAt);
            }
        } else {
            addClosableTab(title, component);
        }
    }

    public void updateTab(JComponent component) {
        int idx = getSelectedIndex();
        if (idx < 0) return;
        JComponent oldComponent = (JComponent) getComponentAt(idx);
        copyTabData(oldComponent, component);
        setComponentAt(idx, component);

        if (tempComponentHolder == null) {
            tempComponentHolder = oldComponent;
            lastSelectedIndex = idx;
        }
    }

    public void removeAllTabs() {
        removeAll();
        tangleCounter = 0;
        tempComponentHolder = null;
        lastSelectedIndex = -1;
    }

    private void attachTabData(JComponent component, String title, int[] hardCusters, double[][] softClusters) {
        component.putClientProperty(PROPERTY_TITLE, title);
        component.putClientProperty(INDEX_TITLE, attachmentIndex++);
        component.putClientProperty(SOFT_CLUSTER_TITLE, softClusters);
        component.putClientProperty(HARD_CLUSTER_TITLE, hardCusters);
        long clusteringTime = (title.equals(POINTS_TITLE) || title.equals(CUT_TITLE) || title.equals(GROUND_TRUTH_TITLE)) ? 0 : view.getClusteringTime();
        component.putClientProperty(CLUSTER_TIME_TITLE, clusteringTime);
        component.putClientProperty(CONFIG_TITLE, view.getCurrentConfigurations());
    }

    private void copyTabData(JComponent source, JComponent target) {
        Object title = source.getClientProperty(PROPERTY_TITLE);
        Object index = source.getClientProperty(INDEX_TITLE);
        Object softClusters = source.getClientProperty(SOFT_CLUSTER_TITLE);
        Object hardClusters = source.getClientProperty(HARD_CLUSTER_TITLE);
        Object clusterTime = source.getClientProperty(CLUSTER_TIME_TITLE);
        Object config = source.getClientProperty(CONFIG_TITLE);

        target.putClientProperty(PROPERTY_TITLE, title);
        target.putClientProperty(INDEX_TITLE, index);
        target.putClientProperty(SOFT_CLUSTER_TITLE, softClusters);
        target.putClientProperty(HARD_CLUSTER_TITLE, hardClusters);
        target.putClientProperty(CLUSTER_TIME_TITLE, clusterTime);
        target.putClientProperty(CONFIG_TITLE, config);
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
            if (i != -1) {
                JComponent c = (JComponent) getComponentAt(i);
                int clusterIndex = (int) c.getClientProperty(INDEX_TITLE);
                String t = (String) c.getClientProperty(PROPERTY_TITLE);
                if (!t.equals(CUT_TITLE)) view.removeTree(clusterIndex);
                removeTabAt(i);
            }
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

    public int getClusterIndex() {
        return attachmentIndex - 1;
    }

    private void addSelectionChangeListener() {
        addChangeListener(e -> {
            int idx = getSelectedIndex();
            if (idx < 0) return;

            JComponent c = (JComponent) getComponentAt(idx);

            // Show clustering information in statistics panel
            int clusterIndex = (int) c.getClientProperty(INDEX_TITLE);
            int[] clustering = (int[]) c.getClientProperty(HARD_CLUSTER_TITLE);
            long clusterTime = (long) c.getClientProperty(CLUSTER_TIME_TITLE);
            view.updateStatisticsPanel(clusterIndex, clustering, clusterTime);

            if (idx < 2) return;

            String title = (String) c.getClientProperty(PROPERTY_TITLE);
            if (title.equals(CUT_TITLE)) return;
            else if (title.equals(SCANPY_TITLE)) {
                view.removeTrees();
                return;
            }

            // Draw tangle search trees and load the tangle configuration
            view.loadAndDrawTrees(clusterIndex);
            view.loadConfig((Config) c.getClientProperty(CONFIG_TITLE));

            if (tempComponentHolder != null) {
                setComponentAt(lastSelectedIndex, tempComponentHolder);
                tempComponentHolder = null;
                lastSelectedIndex = -1;
            }
        });
    }

    private void addSaveActions() {
        addMouseListener(new MouseAdapter() {
            private void maybeShowMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = indexAtLocation(e.getX(), e.getY());
                if (idx < 0) return;
                setSelectedIndex(idx);

                JComponent c = (JComponent) getComponentAt(idx);
                String title = (String) c.getClientProperty(PROPERTY_TITLE);
                double[][] softClusters = (double[][]) c.getClientProperty(SOFT_CLUSTER_TITLE);
                int[] clusters = (int[]) c.getClientProperty(HARD_CLUSTER_TITLE);

                JPopupMenu menu = new JPopupMenu();

                JMenuItem savePlotPng = new JMenuItem("Export to PNG...");
                savePlotPng.addActionListener(ee -> exportTabAsPNG((ChartPanel) c, title));
                menu.add(savePlotPng);

                if (!title.equals(POINTS_TITLE)) {
                    JMenuItem saveHardClustering = new JMenuItem("Export hard clustering to CSV...");
                    saveHardClustering.addActionListener(ee -> saveHardClusteringAsCsv(clusters));
                    menu.add(saveHardClustering);
                }

                if (!title.equals(POINTS_TITLE) && !title.equals(GROUND_TRUTH_TITLE)) {
                    JMenuItem saveSoftClustering = new JMenuItem("Export soft clustering to CSV...");
                    saveSoftClustering.addActionListener(ee -> saveSoftClusteringAsCsv(softClusters));
                    menu.add(saveSoftClustering);
                }

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e)  { maybeShowMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowMenu(e); }
        });
    }

    private void exportTabAsPNG(ChartPanel panel, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export plot to PNG");
        chooser.setSelectedFile(new java.io.File(title + ".png"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG files", "png"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new java.io.File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            JFreeChart chart = panel.getChart();
            int width = panel.getWidth();
            int height = panel.getHeight();
            if (width <= 0 || height <= 0) {
                width = 800;
                height = 600;
            }
            // This is the in-built JFree save function
            ChartUtilities.saveChartAsPNG(file, chart, width, height);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save PNG:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveHardClusteringAsCsv(int[] clustering) {
        if (clustering == null || clustering.length == 0) {
            System.out.println("Error when saving hard clustering in ScatterPlotPanel: Clustering is null");
            return;
        }

        int[] labels = view.unshuffleClustering(clustering);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save hard clustering as CSV");
        fileChooser.setSelectedFile(new File("labels.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        // Add extension if user did not
        File file = fileChooser.getSelectedFile();
        if (!file.getName().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");

        // Have user confirm overwrite if file exists
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Do you want to override the existing file?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        try {
            ClusteringIO.saveHard(labels, file);
            System.out.println("Saved hard clustering:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error when writing hard clustering to CSV");
            e.printStackTrace();
        }
    }

    private void saveSoftClusteringAsCsv(double[][] softClustering) {
        if (softClustering == null || softClustering.length == 0) {
            System.out.println("Error when saving soft clustering in ScatterPlotPanel: Clustering is null");
            return;
        }

        double[][] probabilities = view.unshuffleClustering(softClustering);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save soft clustering as CSV");
        fileChooser.setSelectedFile(new File("soft_clustering.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        // Add extension if user did not
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");

        // Have user confirm overwrite if file exists
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Do you want to override the existing file?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        try {
            ClusteringIO.saveSoft(probabilities, file);
            System.out.println("Saved soft clustering:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error when writing soft clustering to CSV");
            e.printStackTrace();
        }
    }

    public double[][] getCurrentSoftClustering() {
        int idx = getSelectedIndex();
        if (idx < 0) return null;
        JComponent c = (JComponent) getComponentAt(idx);
        return (double[][]) c.getClientProperty(SOFT_CLUSTER_TITLE);
    }
}

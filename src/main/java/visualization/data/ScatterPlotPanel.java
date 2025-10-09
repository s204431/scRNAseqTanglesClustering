package visualization.data;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.plot.swing.Canvas;
import smile.plot.swing.Figure;
import smile.plot.swing.ScatterPlot;
import util.Config;
import util.GlobalConstants;
import visualization.View;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;

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
    private static final String CONFIG_TITLE = "config";


    private static final int POINTS_IDX = 0;
    private static final int GROUND_TRUTH_IDX = 1;
    private static final int CUT_IDX = 2;

    private static final boolean SHOW_GRID = true;
    private static final char MARK = 'o';

    private int attachmentIndex = 0;
    private int tangleCounter = 0;

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
        ScatterPlot plot = ScatterPlot.of(points, MARK);
        Figure figure = plot.figure();

        figure.setAxisLabels("t-SNE1", "t-SNE2");
        figure.getAxis(0).setGridVisible(SHOW_GRID);
        figure.getAxis(1).setGridVisible(SHOW_GRID);

        Canvas canvas = new Canvas(figure);
        attachTabData(canvas, POINTS_TITLE, null);

        add(POINTS_TITLE, canvas);
        setSelectedIndex(getTabCount() - 1);
    }

    public void drawCut(double[][] points, int[] clustering) {
        drawClusters(points, clustering, CUT_TITLE);
    }

    public void drawGroundTruth(double[][] points, int[] groundTruth) {
        drawClusters(points, groundTruth, GROUND_TRUTH_TITLE);
    }

    public void drawClusters(double[][] points, int[] clusters, boolean tangle) {
        String title = SCANPY_TITLE;
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
        attachTabData(canvas, title, clusters);

        if (title.equals(GROUND_TRUTH_TITLE)) {
            int i = indexOfTab(title);
            if (i >= 0) {
                setComponentAt(i, canvas);
                setSelectedIndex(i);
            } else {
                insertTab(title, null, canvas, null, GROUND_TRUTH_IDX);
                setSelectedIndex(GROUND_TRUTH_IDX);
            }
        }

        else if (title.equals(CUT_TITLE)) {
            int i = indexOfTab(title);
            if (i >= 0) {
                setComponentAt(i, canvas);
                setSelectedIndex(i);
            } else {
                int newI = getComponentAt(GROUND_TRUTH_IDX).toString().equals(GROUND_TRUTH_TITLE) ? GROUND_TRUTH_IDX : CUT_IDX;
                insertTab(title, null, canvas, null, newI);
                setTabComponentAt(newI, makeTabHeader(title));
                setSelectedIndex(newI);
            }

        } else {
            addClosableTab(title, canvas);
        }
    }

    public void removeAllTabs() {
        removeAll();
        tangleCounter = 0;
    }

    private void attachTabData(Canvas canvas, String title, int[] clusters) {
        canvas.putClientProperty(PROPERTY_TITLE, title);
        canvas.putClientProperty(INDEX_TITLE, attachmentIndex++);
        canvas.putClientProperty(HARD_CLUSTER_TITLE, clusters);
        canvas.putClientProperty(CONFIG_TITLE, view.getCurrentConfigurations());
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
                Canvas c = (Canvas) getComponentAt(i);
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

            Canvas c = (Canvas) getComponentAt(idx);

            // Show clustering information in statistics panel
            int[] clustering = (int[]) c.getClientProperty(HARD_CLUSTER_TITLE);
            int clusterIndex = (int) c.getClientProperty(INDEX_TITLE);
            view.updateStatisticsPanel(clusterIndex, clustering);

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
        });
    }

    private void addSaveActions() {
        addMouseListener(new MouseAdapter() {
            private void maybeShowMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = indexAtLocation(e.getX(), e.getY());
                if (idx < 0) return;
                setSelectedIndex(idx);

                Canvas c = (Canvas) getComponentAt(idx);
                String title = (String) c.getClientProperty(PROPERTY_TITLE);
                int[] clusters = (int[]) c.getClientProperty(HARD_CLUSTER_TITLE);

                JPopupMenu menu = new JPopupMenu();

                JMenuItem savePlotPng = new JMenuItem("Export to PNG...");
                savePlotPng.addActionListener(ee -> exportTabAsPNG(c, title));
                menu.add(savePlotPng);

                if (!title.equals(POINTS_TITLE)) {
                    JMenuItem saveHardClustering = new JMenuItem("Export hard clustering to CSV...");
                    saveHardClustering.addActionListener(ee -> saveHardClusteringAsCsv(clusters));
                    menu.add(saveHardClustering);
                }

                if (!title.equals(POINTS_TITLE) && !title.equals(GROUND_TRUTH_TITLE)) {
                    JMenuItem saveSoftClustering = new JMenuItem("Export soft clustering to CSV...");
                    saveSoftClustering.addActionListener(ee -> saveSoftClusteringAsCsv(null));
                    menu.add(saveSoftClustering);
                }

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e)  { maybeShowMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowMenu(e); }
        });
    }

    private void exportTabAsPNG(Canvas canvas, String title) {
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
            // This is the in-built smile save function
            canvas.save(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save PNG:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveHardClusteringAsCsv(int[] clustering) {
        int[] labels = view.unshuffleClustering(clustering);
        if (labels == null || labels.length == 0) {
            System.out.println("Error when saving hard clustering in ScatterPlotPanel: Clustering is null");
            return;
        }

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

        // Write file
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath())) {
            out.write("cell,cluster");
            out.newLine();
            for (int i = 0; i < labels.length; i++) {
                out.write(i + "," + labels[i]);
                out.newLine();
            }

        } catch (Exception e) {
            System.out.println("Error when writing hard clustering to CSV in ScatterPlotPanel");
            e.printStackTrace();
            return;
        }

        System.out.println("Saved hard clustering:\n" + file.getAbsolutePath());
    }

    private void saveSoftClusteringAsCsv(double[][] softClustering) {
        double[][] probabilities = view.unshuffleClustering(softClustering);
        if (probabilities == null || probabilities.length == 0) {
            System.out.println("Error when saving soft clustering in ScatterPlotPanel: Clustering is null");
            return;
        }

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

        // Write file
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath())) {

            // Header
            out.write("cell");
            for (int i = 1; i <= probabilities[0].length; i++) {
                out.write(",cluster_" + i);
            }
            out.newLine();

            for (int i = 0; i < probabilities.length; i++) {
                out.write(i+"");
                for (int j = 0; j < probabilities[0].length; j++) {
                    out.write(",");
                    out.write(Double.toString(probabilities[i][j]));
                }
                out.newLine();
            }

        } catch (Exception e) {
            System.out.println("Error when writing soft clustering to CSV in ScatterPlotPanel");
            e.printStackTrace();
            return;
        }

        System.out.println("Saved soft clustering:\n" + file.getAbsolutePath());
    }
}

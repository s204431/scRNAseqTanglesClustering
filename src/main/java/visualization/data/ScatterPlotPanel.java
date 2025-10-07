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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class ScatterPlotPanel extends JTabbedPane {
    private View view;

    private static final String POINTS_TITLE = "Points";
    private static final String GROUND_TRUTH_TITLE = "Ground Truth";
    private static final String CUT_TITLE = "Cut";
    private static final String SCANPY_TITLE = "Scanpy";

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
        while (getTabCount() > 0) {
            remove(0);
        }
        tangleCounter = 0;
    }

    private void attachTabData(Canvas canvas, String title, int[] clusters) {
        canvas.putClientProperty("title", title);
        canvas.putClientProperty("index", attachmentIndex++);
        canvas.putClientProperty("clusters", clusters);
        canvas.putClientProperty("config", view.getCurrentConfigurations());
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
                int clusterIndex = (int) c.getClientProperty("index");
                String t = (String) c.getClientProperty("title");
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
            if (idx < 2) return;

            Canvas c = (Canvas) getComponentAt(idx);
            String title = (String) c.getClientProperty("title");
            if (title.equals(CUT_TITLE)) return;
            else if (title.equals(SCANPY_TITLE)) {
                view.removeTrees();
                return;
            }

            int index = (int) c.getClientProperty("index");
            view.loadAndDrawTrees(index);
            view.loadConfig((Config) c.getClientProperty("config"));
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
                String title = (String) c.getClientProperty("title");
                int[] clusters = (int[]) c.getClientProperty("clusters");

                JPopupMenu menu = new JPopupMenu();

                JMenuItem savePlotPng = new JMenuItem("Export to PNG...");
                savePlotPng.addActionListener(ee -> exportTabAsPNG(c, title));
                menu.add(savePlotPng);

                if (!title.equals(POINTS_TITLE)) {
                    JMenuItem saveHardClustering = new JMenuItem("Save hard clustering...");
                    saveHardClustering.addActionListener(ee -> saveHardClusteringAsCsv(clusters));
                    menu.add(saveHardClustering);
                }

                if (!title.equals(POINTS_TITLE) && !title.equals(GROUND_TRUTH_TITLE)) {
                    JMenuItem saveSoftClustering = new JMenuItem("Save soft clustering...");
                    saveSoftClustering.addActionListener(ee -> saveSoftClusteringAsCsv());
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
        chooser.setDialogTitle("Export plot as PNG");
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

    private void saveHardClusteringAsCsv(int[] clusters) {

    }

    private void saveSoftClusteringAsCsv() {

    }
}

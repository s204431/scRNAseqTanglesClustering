package visualization;

import clustering.TangleSearchTree;
import com.sun.tools.javac.Main;
import util.BitSet;

import javax.swing.*;
import javax.swing.text.StyledEditorKit;
import java.awt.*;

public class MainWindow extends JFrame {
    private View view;

    private final TangleTreePanel tangleTreePanel;
    private final ScatterPlotPanel scatterPanel;
    private final StatisticsPanel statsPanel;
    private final ParameterPanel parameterPanel;
    private final TopPanel topPanel;

    public MainWindow(View view) {
        this.view = view;
        this.tangleTreePanel = new TangleTreePanel(view);
        this.scatterPanel = new ScatterPlotPanel(view);
        this.statsPanel = new StatisticsPanel(view);
        this.parameterPanel = new ParameterPanel(view);
        this.topPanel = new TopPanel(view);

        setSize(new Dimension(1200, 800));

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scatterPanel, statsPanel);
        verticalSplit.setResizeWeight(0.7); // % space the scatter panel takes initially

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tangleTreePanel, verticalSplit);
        horizontalSplit.setResizeWeight(0.4); // % space the tangle tree panel takes initially

        getContentPane().add(horizontalSplit, BorderLayout.CENTER);
        getContentPane().add(parameterPanel, BorderLayout.EAST);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void drawPoints(double[][] points) {
        scatterPanel.drawScatterPlot(points);
    }

    public void drawClusters(double[][] points, int[] clustering) {
        scatterPanel.drawClusters(points, clustering);
    }

    public void drawTangleSearchTree(TangleSearchTree tree) {
        tangleTreePanel.drawTree(tree);
    }

    public void turnOnCuts(int cutIndex) {
        parameterPanel.turnOnCuts(cutIndex);
    }

    public void turnOffCuts() {
        parameterPanel.turnOffCuts();
    }
}

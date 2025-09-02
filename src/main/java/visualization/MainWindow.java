package visualization;

import com.sun.tools.javac.Main;

import javax.swing.*;
import javax.swing.text.StyledEditorKit;
import java.awt.*;

public class MainWindow extends JFrame {
    private TangleTreePanel tangleTreePanel = new TangleTreePanel();
    private ScatterPlotPanel scatterPanel = new ScatterPlotPanel();
    private StatisticsPanel statsPanel = new StatisticsPanel();
    private ParameterPanel parameterPanel = new ParameterPanel();
    private TopPanel topPanel = new TopPanel();

    public MainWindow() {
        setSize(new Dimension(1200, 800));

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scatterPanel, statsPanel);
        verticalSplit.setResizeWeight(0.7); // scatter panel takes 70% of the space initially

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tangleTreePanel, verticalSplit);
        horizontalSplit.setResizeWeight(0.4); // tangle tree panel takes 20% space initially

        add(horizontalSplit, BorderLayout.CENTER);
        add(parameterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        // TODO: REMOVE THIS
        parameterPanel.setPanel(scatterPanel);
    }

    public void drawPoints() {
        scatterPanel.drawScatterPlot();
    }

    public void drawClusters() {
        scatterPanel.drawClusters();
    }

    public void setData(double[][] data) {
        scatterPanel.setPoints(data);
    }

    public void setClusters(int[] clusters) {
        scatterPanel.setClusters(clusters);
    }

    // TODO: REMOVE THIS
    public void setProjectedData(double[][] projectedData) {
        parameterPanel.setProjectedData(projectedData);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}

package visualization;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class TangleTreePanel extends JPanel {
    private View view;

    public TangleTreePanel(View view) {
        this.view = view;

        setLayout(new BorderLayout());
    }

    public void drawTree() {
        // Create a simple tree
        DelegateTree<String, String> tree = new DelegateTree<>();
        tree.setRoot("Root");
        tree.addChild("Edge-1", "Root", "Child-1");
        tree.addChild("Edge-2", "Root", "Child-2");
        tree.addChild("Edge-3", "Child-1", "Grandchild-1");
        tree.addChild("Edge-4", "Child-1", "Grandchild-2");

        // Layout
        TreeLayout<String, String> layout = new TreeLayout<>(tree);

        // Visualization
        VisualizationViewer<String, String> vv = new VisualizationViewer<>(layout, new Dimension(400, 400));
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());

        // Interactive mouse
        DefaultModalGraphMouse<String, String> graphMouse = new DefaultModalGraphMouse<>();
        graphMouse.setMode(DefaultModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(graphMouse);

        // Click listener
        vv.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object vertex = vv.getPickSupport().getVertex(vv.getGraphLayout(), e.getPoint().getX(), e.getPoint().getY());
                if (vertex != null) {
                    JOptionPane.showMessageDialog(vv, "You clicked: " + vertex);
                }
            }
        });

        removeAll();
        add(vv, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

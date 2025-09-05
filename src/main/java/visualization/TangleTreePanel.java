package visualization;

import clustering.TangleClusterer;
import clustering.TangleSearchTree;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import smile.neighbor.lsh.Hash;
import util.BitSet;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;

public class TangleTreePanel extends JPanel {
    private View view;

    private HashMap<String, String> idToNodeName = new HashMap<>();
    private HashMap<String, String> idToEdgeName = new HashMap<>();
    private HashMap<String, BitSet> idToCut = new HashMap<>();
    private HashMap<String, Integer> idToCutIndex = new HashMap<>();
    private HashMap<Integer, Integer> originalCutIndexToSortedCutIndex = new HashMap<>();

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    public TangleTreePanel(View view) {
        this.view = view;

        setLayout(new BorderLayout());
    }

    public void drawTree(TangleSearchTree tst) {
        getSortedCutsAndCosts();

        resetHistoryVariables();

        TangleSearchTree.Node root = tst.getRoot();
        DelegateTree<String, String> tree = new DelegateTree<>();

        // Add nodes to tree
        addNodes(tree, root, "None", 0, false);

        // Layout
        TreeLayout<String, String> layout = new TreeLayout<>(tree);

        // Visualization
        VisualizationViewer<String, String> vv = new VisualizationViewer<>(layout);
        vv.getRenderContext().setVertexLabelTransformer(idToNodeName::get);
        vv.getRenderContext().setVertexFillPaintTransformer(v -> Color.WHITE);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        vv.getRenderContext().setVertexShapeTransformer(v -> new Ellipse2D.Double(-20, -20, 30, 30));

        vv.getRenderContext().setEdgeLabelTransformer(idToEdgeName::get);
        vv.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(tree));

        // Interactive mouse
        DefaultModalGraphMouse<String, String> graphMouse = new DefaultModalGraphMouse<>();
        graphMouse.setMode(DefaultModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(graphMouse);

        // Click listener
        vv.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object vertex = vv.getPickSupport().getVertex(vv.getGraphLayout(), e.getPoint().getX(), e.getPoint().getY());
                if (vertex == null) {
                    return;
                }

                String uniqueId = vertex.toString();
                if (idToNodeName.get(uniqueId).equals("Root")) {
                    view.showClustering();
                    return;
                }

                BitSet cut = idToCut.get(uniqueId);
                int cutIndex = idToCutIndex.get(uniqueId);
                view.showCut(cut, cutIndex);
            }
        });

        removeAll();
        add(vv, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void addNodes(DelegateTree<String, String> tree, TangleSearchTree.Node node, String parent, int parentCutIndex, boolean condensed) {
        int cutIndex = parent.equals("None") ? -1 : originalCutIndexToSortedCutIndex.get(node.originalOrientation);

        // Temporary unique ID for each node as identical node names are currently not allowed
        String uniqueId = "" + UUID.randomUUID();
        String nodeName = originalCutIndexToSortedCutIndex.get(node.originalOrientation) + (node.side ? "L" : "R");

        if (parent.equals("None")) {
            tree.setRoot(uniqueId);
            idToNodeName.put(uniqueId, "Root");
        } else {

            if (condensed) {
                tree.addChild(parent + "-" + uniqueId, parent, uniqueId);
                idToNodeName.put(uniqueId, nodeName);
                idToEdgeName.put(uniqueId, "");
                idToCut.put(uniqueId, sortedCuts[cutIndex]);
                idToCutIndex.put(uniqueId, cutIndex);
            } else {
                for (int i = parentCutIndex + 1; i <= cutIndex; i++) {
                    boolean left = node.condensedOrientations.get(i);
                    uniqueId = "" + UUID.randomUUID();
                    nodeName = i + (left ? "L" : "R");

                    tree.addChild(parent + "-" + uniqueId, parent, uniqueId);

                    idToNodeName.put(uniqueId, nodeName);
                    idToEdgeName.put(uniqueId, "");
                    BitSet cut = new BitSet(sortedCuts[i].toString());
                    if (left) {
                        for (int j = 0; j < cut.size(); j++) {
                            cut.flip(j);
                        }
                    }
                    idToCut.put(uniqueId, cut);
                    idToCutIndex.put(uniqueId, i);

                    parent = uniqueId;
                }
            }

        }

        parent = uniqueId;

        if (node.leftChild != null) {
            addNodes(tree, node.leftChild, parent, cutIndex, condensed);
        }

        if (node.rightChild != null) {
            addNodes(tree, node.rightChild, parent, cutIndex, condensed);
        }
    }

    private void getSortedCutsAndCosts() {
        BitSet[] cuts = view.getCuts();
        double[] cutCosts = view.getCutCosts();
        Tuple<BitSet[], double[]> result = TangleClusterer.removeRedundantCuts(cuts, cutCosts, 0.9);
        cuts = result.x;
        cutCosts = result.y;

        int n = cutCosts.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        final double[] finalCutCosts = cutCosts;
        Arrays.sort(indices, Comparator.comparingDouble(i -> finalCutCosts[i]));
        BitSet[] cutsSorted = new BitSet[n];
        double[] costsSorted = new double[n];

        for (int i = 0; i < n; i++) {
            cutsSorted[i] = cuts[indices[i]];
            costsSorted[i] = cutCosts[indices[i]];
            originalCutIndexToSortedCutIndex.put(indices[i], i);
        }

        sortedCuts = cutsSorted;
        sortedCutCosts = costsSorted;
    }

    private void resetHistoryVariables() {
        idToNodeName.clear();
        idToEdgeName.clear();
        idToCut.clear();
        idToCutIndex.clear();
    }
}

package visualization;

import clustering.TangleClusterer;
import clustering.TangleSearchTree;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer;
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

    private JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private JPanel treePanel = new JPanel(new BorderLayout());

    private HashMap<String, String> idToNodeName = new HashMap<>();
    private HashMap<String, String> idToEdgeName = new HashMap<>();
    private HashMap<String, BitSet> idToCut = new HashMap<>();
    private HashMap<String, Integer> idToCutIndex = new HashMap<>();
    private HashMap<Integer, Integer> originalCutIndexToSortedCutIndex = new HashMap<>();
    private HashMap<Integer, Integer> sortedCutIndexToOriginalCutIndex = new HashMap<>();

    private BitSet[] cuts;
    private double[] cutCosts;

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    private boolean showCondensedTree = false;
    private boolean useIntersections = false;

    private int nodeCounter = 0;

    public TangleTreePanel(View view) {
        this.view = view;
        setLayout(new BorderLayout());

        // Checkbox to toggle condensed / uncondensed
        JCheckBox condensedCheckBox = new JCheckBox("Condense tree", showCondensedTree);
        condensedCheckBox.addActionListener(e -> {
            showCondensedTree = condensedCheckBox.isSelected();
            view.drawTangleSearchTree(showCondensedTree);
        });
        topPanel.add(condensedCheckBox);

        // Checkbox to toggle the use of intersections when clicking on cuts in the tree
        JCheckBox intersectionCheckBox = new JCheckBox("Use intersections", useIntersections);
        intersectionCheckBox.addActionListener(e -> {
            useIntersections = intersectionCheckBox.isSelected();
            view.drawTangleSearchTree(showCondensedTree);
        });
        topPanel.add(intersectionCheckBox);

        add(topPanel, BorderLayout.NORTH);
        add(treePanel, BorderLayout.CENTER);
    }

    public void drawTree(TangleSearchTree tst) {
        getSortedCutsAndCosts();

        resetHistoryVariables();

        TangleSearchTree.Node root = tst.getRoot();
        DelegateTree<String, String> tree = new DelegateTree<>();

        // Add nodes to tree
        int n = sortedCuts[0].size();
        BitSet cutIncludingAllPoints = new BitSet(n);
        for (int i = 0; i < n; i++) {
            cutIncludingAllPoints.flip(i);
        }
        //addNodes(tree, root, "None", 0, cutIncludingAllPoints, showCondensedTree);
        String parent = "None";
        addNode(tree, root, parent);

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

                System.out.println(sortedCutCosts[cutIndex]);
            }
        });

        treePanel.removeAll();
        treePanel.add(vv, BorderLayout.CENTER);
        treePanel.revalidate();
        treePanel.repaint();
    }

    public void addNode(DelegateTree<String, String> tree, TangleSearchTree.Node node, String parent) {
        if (node == null) {
            return;
        }

        int cutIndex = originalCutIndexToSortedCutIndex.get(node.originalOrientation);

        // Temporary unique ID for each node as identical node names are currently not allowed
        String uniqueId = "" + nodeCounter++;//UUID.randomUUID();
        String nodeName = originalCutIndexToSortedCutIndex.get(node.originalOrientation) + (node.side ? "L" : "R");

        if (parent.equals("None")) {    // Root
            tree.setRoot(uniqueId);
            idToNodeName.put(uniqueId, "Root");
            addNode(tree, node.leftChild, uniqueId);
            addNode(tree, node.rightChild, uniqueId);
            return;
        }

        BitSet cut = cuts[node.originalOrientation].clone();
        if (node.side) {
            cut.flipALl();
        }

        tree.addChild(parent + "-" + uniqueId, parent, uniqueId);
        idToNodeName.put(uniqueId, nodeName);
        idToEdgeName.put(uniqueId, "");
        idToCutIndex.put(uniqueId, cutIndex);
        if (useIntersections) {
            BitSet intersection = node.intersection.clone();
            idToCut.put(uniqueId, intersection);
        } else {
            idToCut.put(uniqueId, cut);
        }

        addNode(tree, node.leftChild, uniqueId);
        addNode(tree, node.rightChild, uniqueId);
    }

    private void getSortedCutsAndCosts() {
        cuts = view.getCuts();
        cutCosts = view.getCutCosts();
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
            sortedCutIndexToOriginalCutIndex.put(i, indices[i]);
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

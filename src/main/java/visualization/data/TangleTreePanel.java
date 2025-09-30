package visualization.data;

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
import visualization.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;

public class TangleTreePanel extends JPanel {
    private View view;

    private final JTabbedPane treeTabs = new JTabbedPane(JTabbedPane.TOP);

    private final JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private final JCheckBox intersectionCheckBox = new JCheckBox("Toggle intersections");

    private final JPanel treePanelOriginal = new JPanel(new BorderLayout());
    private final JPanel treePanelSplitPruned = new JPanel(new BorderLayout());
    private final JPanel treePanelCondensed = new JPanel(new BorderLayout());

    private final HashMap<String, TangleSearchTree.Node> idToNode = new HashMap<>();
    private final HashMap<String, String> idToNodeName = new HashMap<>();
    private final HashMap<String, String> idToEdgeName = new HashMap<>();
    private final HashMap<String, BitSet> idToCut = new HashMap<>();
    private final HashMap<String, BitSet> idToIntersection = new HashMap<>();
    private final HashMap<String, Integer> idToCutIndex = new HashMap<>();
    private final HashMap<Integer, Integer> originalCutIndexToSortedCutIndex = new HashMap<>();
    private final HashMap<Integer, Integer> sortedCutIndexToOriginalCutIndex = new HashMap<>();

    private BitSet[] cuts;
    private double[] cutCosts;

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    private List<double[]> branchCosts;

    public TangleTreePanel(View view) {
        this.view = view;
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(treeTabs, BorderLayout.CENTER);

        topPanel.add(intersectionCheckBox);
    }

    public void drawTrees(TangleSearchTree originalTree, TangleSearchTree splitPruned, TangleSearchTree condensed) {
        getSortedCutsAndCosts();
        resetHistoryVariables();

        while (treeTabs.getTabCount() > 0)
            treeTabs.remove(0);

        if (originalTree != null) {
            drawTree(originalTree, treePanelOriginal);
            treeTabs.add("Original Tree", treePanelOriginal);
        }

        if (splitPruned != null) {
            drawTree(splitPruned, treePanelSplitPruned);
            treeTabs.add("Split Pruned Tree", treePanelSplitPruned);
        }
        if (condensed != null) {
            drawTree(condensed, treePanelCondensed);
            treeTabs.add("Condensed Tree", treePanelCondensed);
        }

        revalidate();
        repaint();
    }

    public void drawTree(TangleSearchTree tst, JPanel treePanel) {
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

                BitSet cut = intersectionCheckBox.isSelected() ? idToIntersection.get(uniqueId) : idToCut.get(uniqueId);
                int cutIndex = idToCutIndex.get(uniqueId);
                view.showCut(cut, cutIndex);

                TangleSearchTree.Node node = idToNode.get(uniqueId);

                int originalOrientation = node.originalOrientation;
                TangleSearchTree.Node lastParentBranchNode = node.parent;
                while (lastParentBranchNode.leftChild == null || lastParentBranchNode.rightChild == null) {
                    lastParentBranchNode = lastParentBranchNode.parent;
                }

                String originalCost = "Original Cost: " + sortedCutCosts[cutIndex];
                String branchCost = (branchCosts == null ? "" : "Branch cost: " + branchCosts.get(node.parent.branchId)[originalOrientation]);
                String parentBranchCost = (branchCosts == null) ? "" : "Parent branch cost: " + branchCosts.get(lastParentBranchNode.branchId)[originalOrientation];

                System.out.println("Cut Index: " + cutIndex);
                System.out.println("Cost: " + node.cost);
                System.out.println("Original Orientation: " + originalOrientation);
                System.out.println(originalCost);
                if (branchCosts != null) {
                    System.out.println(branchCost);
                    System.out.println(parentBranchCost);
                }
                System.out.println();

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
        String uniqueId = "" + UUID.randomUUID();
        String nodeName = originalCutIndexToSortedCutIndex.get(node.originalOrientation) + (node.side ? "L" : "R");

        if (parent.equals("None")) {    // Root
            tree.setRoot(uniqueId);
            idToNode.put(uniqueId, node);
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
        idToNode.put(uniqueId, node);
        idToNodeName.put(uniqueId, nodeName);
        idToEdgeName.put(uniqueId, "");
        idToCutIndex.put(uniqueId, cutIndex);
        BitSet intersection = node.intersection.clone();
        idToIntersection.put(uniqueId, intersection);
        idToCut.put(uniqueId, cut);

        addNode(tree, node.leftChild, uniqueId);
        addNode(tree, node.rightChild, uniqueId);
    }

    private void getSortedCutsAndCosts() {
        branchCosts = view.getBranchCosts();

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
        idToIntersection.clear();
        idToCutIndex.clear();
    }
}

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
import util.GlobalConstants;
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
    private final JCheckBox clusteringCheckBox = new JCheckBox("Toggle clusters below");

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

    private final HashMap<Integer, Integer[]> clusterIndexToHardClustering = new HashMap<>();
    private final HashMap<Integer, TangleSearchTree[]> clusterIndexToTrees = new HashMap<>();
    private final HashMap<Integer, BitSet[]> clusterIndexToCuts = new HashMap<>();
    private final HashMap<Integer, double[]> clusterIndexToCutCosts = new HashMap<>();
    private final HashMap<Integer, List<double[]>> clusterIndexToBranchCosts = new HashMap<>();

    private int[] hardClustering;
    private int[] groundTruth;
    HashMap<Integer, Integer> map;

    private BitSet[] cuts;
    private double[] cutCosts;

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    private List<double[]> branchCosts;

    public TangleTreePanel(View view) {
        this.view = view;
        //setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);
        //setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(treeTabs, BorderLayout.CENTER);

        treeTabs.setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);

        //topPanel.setBackground(Color.WHITE);
        topPanel.add(intersectionCheckBox);
        topPanel.add(clusteringCheckBox);

        intersectionCheckBox.addActionListener(e -> {
            if (intersectionCheckBox.isSelected()) clusteringCheckBox.setSelected(false);
        });

        clusteringCheckBox.addActionListener(e -> {
            if (clusteringCheckBox.isSelected()) intersectionCheckBox.setSelected(false);
        });
    }

    public void removeTree(int clusterIndex) {
        clusterIndexToTrees.remove(clusterIndex);
        clusterIndexToCuts.remove(clusterIndex);
        clusterIndexToCutCosts.remove(clusterIndex);
        clusterIndexToBranchCosts.remove(clusterIndex);
    }

    public void removeTrees() {
        clusterIndexToTrees.clear();
        clusterIndexToCuts.clear();
        clusterIndexToCutCosts.clear();
        clusterIndexToBranchCosts.clear();

        treePanelOriginal.removeAll();
        treePanelSplitPruned.removeAll();
        treePanelCondensed.removeAll();

        removeTabs();
    }

    public void removeTabs() {
        treeTabs.removeAll();
    }

    public void loadTrees(int clusterIndex, int[] clustering, int[] GT) {
        if (!clusterIndexToTrees.containsKey(clusterIndex)) return;

        /*
        hardClustering = clustering;
        groundTruth = GT;
        map = new HashMap<>();

        int[][] similarity = new int[8][8];
        for (int i = 0; i < hardClustering.length; i++) {
            similarity[hardClustering[i]][GT[i]-1]++;
        }

        for (int i = 0; i < similarity.length; i++) {
            int maxIndex = -1;
            int maxValue = -1;
            for (int j = 0; j < similarity.length; j++) {
                if (similarity[i][j] > maxValue) {
                    maxValue = similarity[i][j];
                    maxIndex = j;
                }
            }
            map.put(maxIndex, i);
        }

        for (int key : map.keySet()) {
            int value = map.get(key);
            System.out.println(key + " -> " + value);
        }
         */

        cuts = clusterIndexToCuts.get(clusterIndex);
        cutCosts = clusterIndexToCutCosts.get(clusterIndex);
        branchCosts = clusterIndexToBranchCosts.get(clusterIndex);
        sortCutsAndCosts();

        TangleSearchTree[] trees = clusterIndexToTrees.get(clusterIndex);
        drawTrees(trees[0], trees[1], trees[2]);
    }

    public void drawTrees(TangleSearchTree originalTree, TangleSearchTree splitPruned, TangleSearchTree condensed, int clusterIndex, boolean removeRedundantCuts, double redundancyFactor, int[] clustering) {
        hardClustering = clustering;
        getCutsAndCosts(removeRedundantCuts, redundancyFactor);
        sortCutsAndCosts();

        clusterIndexToTrees.put(clusterIndex, new TangleSearchTree[] { originalTree, splitPruned, condensed });
        clusterIndexToCuts.put(clusterIndex, cuts.clone());
        clusterIndexToCutCosts.put(clusterIndex, cutCosts.clone());
        clusterIndexToBranchCosts.put(clusterIndex, branchCosts == null ? null : new ArrayList<>(branchCosts));

        drawTrees(originalTree, splitPruned, condensed);
    }

    private void drawTrees(TangleSearchTree originalTree, TangleSearchTree splitPruned, TangleSearchTree condensed) {
        resetHistoryVariables();

        removeTabs();

        if (originalTree != null) {
            drawTree(originalTree, treePanelOriginal);
            treeTabs.add("Original", treePanelOriginal);
        }

        if (splitPruned != null) {
            drawTree(splitPruned, treePanelSplitPruned);
            treeTabs.add("Split Pruned", treePanelSplitPruned);
        }

        if (condensed != null) {
            drawTree(condensed, treePanelCondensed);
            treeTabs.add("Condensed", treePanelCondensed);
        }
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
        //vv.setBackground(GlobalConstants.COLOR_VERY_LIGHT_GRAY);
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
                    // Do Nothing
                    return;
                }

                BitSet cut = null;
                if (intersectionCheckBox.isSelected()) {
                    cut = idToIntersection.get(uniqueId);
                } else if (clusteringCheckBox.isSelected()) {
                    cut = findClusteringCut(tst, idToNode.get(uniqueId), hardClustering, false);
                } else {
                    //cut = findClusteringCut(tst, idToNode.get(uniqueId), groundTruth, true);
                    cut = idToCut.get(uniqueId);
                }
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

    private void getCutsAndCosts(boolean removeRedundantCuts, double redundancyFactor) {
        branchCosts = view.getBranchCosts();

        cuts = view.getCuts().clone();
        cutCosts = view.getCutCosts().clone();

        double factor = removeRedundantCuts ? redundancyFactor : 1.0;
        Tuple<BitSet[], double[]> result = TangleClusterer.removeRedundantCuts(cuts, cutCosts, factor);
        cuts = result.x;
        cutCosts = result.y;
    }

    private void sortCutsAndCosts() {
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

    private BitSet findClusteringCut(TangleSearchTree tst, TangleSearchTree.Node node, int[] hardClustering, boolean gt) {
        TangleSearchTree.Node root = tst.getRoot();
        computeLeafIndices(root, 0);

        BitSet out = new BitSet(hardClustering.length);
        for (int i = 0; i < hardClustering.length; i++) {
            int cluster = hardClustering[i];
            //if (gt) cluster = map.get(cluster - 1);
            if (node.leafIndices.contains(cluster)) {
                out.setValue(i, true);
            }
        }

        return out;
    }

    private int computeLeafIndices(TangleSearchTree.Node node, int index) {
        if (node.leftChild == null && node.rightChild == null) {
            node.leafIndices = new ArrayList<>();
            node.leafIndices.add(index);
            return index + 1;
        }

        node.leafIndices = new ArrayList<>();
        if (node.leftChild != null) {
            index = computeLeafIndices(node.leftChild, index);
            for (int i : node.leftChild.leafIndices) {
                node.leafIndices.add(i);
            }
        }

        if (node.rightChild != null) {
            index = computeLeafIndices(node.rightChild, index);
            for (int i : node.rightChild.leafIndices) {
                node.leafIndices.add(i);
            }
        }

        return index;
    }

    public TangleSearchTree[] getTrees(int clusterIndex) {
        return clusterIndexToTrees.get(clusterIndex);
    }

    private void resetHistoryVariables() {
        idToNodeName.clear();
        idToEdgeName.clear();
        idToCut.clear();
        idToIntersection.clear();
        idToCutIndex.clear();
    }
}

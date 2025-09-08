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

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    private boolean showCondensedTree = false;
    private boolean useIntersections = false;

    public TangleTreePanel(View view) {
        this.view = view;
        setLayout(new BorderLayout());

        // Checkbox to toggle condensed / uncondensed
        JCheckBox condensedCheckBox = new JCheckBox("Condense tree", showCondensedTree);
        condensedCheckBox.addActionListener(e -> {
            showCondensedTree = condensedCheckBox.isSelected();
            view.drawTangleSearchTree();
        });
        topPanel.add(condensedCheckBox);

        // Checkbox to toggle the use of intersections when clicking on cuts in the tree
        JCheckBox intersectionCheckBox = new JCheckBox("Use intersections", useIntersections);
        intersectionCheckBox.addActionListener(e -> {
            useIntersections = intersectionCheckBox.isSelected();
            view.drawTangleSearchTree();
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
        addNodes(tree, root, "None", 0, cutIncludingAllPoints, showCondensedTree);

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

    public void addNodes(DelegateTree<String, String> tree, TangleSearchTree.Node node, String parent, int parentCutIndex, BitSet parentCut, boolean condensed) {
        int cutIndex = parent.equals("None") ? -1 : originalCutIndexToSortedCutIndex.get(node.originalOrientation);

        // Temporary unique ID for each node as identical node names are currently not allowed
        String uniqueId = "" + UUID.randomUUID();
        String nodeName = originalCutIndexToSortedCutIndex.get(node.originalOrientation) + (node.side ? "L" : "R");
        BitSet intersection = new BitSet(parentCut.toString());

        if (parent.equals("None")) {
            tree.setRoot(uniqueId);
            idToNodeName.put(uniqueId, "Root");

        } else {
            if (condensed) {
                // Loop over every condensed node and compute the intersection between their respective cuts
                for (int i = parentCutIndex + 1; i <= cutIndex; i++) {
                    boolean left = node.condensedOrientations.get(sortedCutIndexToOriginalCutIndex.get(i));
                    boolean right = node.condensedOrientations.get(sortedCutIndexToOriginalCutIndex.get(i) + node.condensedOrientations.size()/2);
                    if (!left && !right) {
                        continue;
                    }

                    BitSet cut = new BitSet(sortedCuts[i].toString());
                    if (left) {
                        for (int j = 0; j < cut.size(); j++) {
                            cut.flip(j);
                        }
                    }

                    if (useIntersections) {
                        intersection.intersectWith(cut);
                    }
                }

                tree.addChild(parent + "-" + uniqueId, parent, uniqueId);
                idToNodeName.put(uniqueId, nodeName);
                idToEdgeName.put(uniqueId, "");
                idToCut.put(uniqueId, intersection);
                idToCutIndex.put(uniqueId, cutIndex);

            // Not condensed
            } else {
                // Loop over every condensed node and add them to the tree
                for (int i = parentCutIndex + 1; i <= cutIndex; i++) {
                    boolean left = node.condensedOrientations.get(sortedCutIndexToOriginalCutIndex.get(i));
                    boolean right = node.condensedOrientations.get(sortedCutIndexToOriginalCutIndex.get(i) + node.condensedOrientations.size()/2);
                    if (!left && !right) {
                        continue;
                    }

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

                    if (useIntersections) {
                        intersection.intersectWith(cut);
                    } else {
                        intersection = cut;
                    }

                    idToCut.put(uniqueId, intersection);
                    idToCutIndex.put(uniqueId, i);

                    parent = uniqueId;
                    intersection = new BitSet(intersection.toString());
                }
            }
        }

        parent = uniqueId;
        intersection = new BitSet(intersection.toString());

        if (node.leftChild != null) {
            addNodes(tree, node.leftChild, parent, cutIndex, intersection, condensed);
        }

        if (node.rightChild != null) {
            addNodes(tree, node.rightChild, parent, cutIndex, intersection, condensed);
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

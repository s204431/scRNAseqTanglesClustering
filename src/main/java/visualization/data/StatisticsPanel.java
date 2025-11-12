package visualization.data;

import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;
import util.Tuple;
import visualization.View;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class StatisticsPanel extends JScrollPane {
    private static final int HEIGHT_INDEX = -1;
    private static final int SPLIT_COUNT_INDEX = -2;

    private final View view;

    private final JPanel content;

    private final Section dataSection;
    private final Section clusteringSection;
    private final Section tangleSection;
    private final Section performanceSection;

    public StatisticsPanel(View view) {
        this.view = view;

        content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(content, BorderLayout.NORTH);
        setViewportView(topWrapper);

        dataSection = new Section("Data Set");
        performanceSection = new Section("Performance");
        clusteringSection = new Section("Clustering");
        tangleSection = new Section("Tangles");

        addSection(dataSection, 0, 0);
        addSection(performanceSection, 0, 1);
        addSection(clusteringSection, 1, 0);
        addSection(tangleSection, 1, 1);

        getVerticalScrollBar().setUnitIncrement(16);
    }

    private void addSection(Section s, int row, int col) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = (col == 0) ? new Insets(0, 0, 5, 3) : new Insets(0, 3, 5, 0);
        content.add(s, gbc);
    }

    public void showDataSetInformation(ScRNAseqDataset dataSet) {
        if (dataSet == null || dataSet.data == null || dataSet.data.length == 0) {
            dataSection.clear().put("Cells","-").put("Highly Variable Genes","-").render();
            return;
        }
        int cells = dataSet.data.length;
        int hvg   = dataSet.data[0].length;
        double sparsity = dataSet.getSparsity();
        dataSection.clear()
                .put("Cells", Integer.toString(cells))
                .put("Highly Variable Genes", Integer.toString(hvg))
                .put("Sparsity", format(sparsity))
                .render();
    }

    public void updateClusteringStats(int[] clustering, long clusterTime) {
        if (clustering == null) {
            updatePerformance(0, 0, 0);
            clusteringSection.clear().render();
            return;
        }

        clusteringSection.clear();

        // Performance cell
        Tuple<Double, Double> result = view.getClusteringQuality(clustering);
        updatePerformance(result.x, result.y, clusterTime);

        // Clustering cell
        HashMap<Integer, Integer> clusterMap = computeClusterMapping(clustering);
        int clusters = clusterMap.size();
        double silhouetteScore = view.getSilhouetteScore(clustering);
        double daviesBouldinIndex = view.getDavisBouldin(clustering);
        clusteringSection
                .put("Number of clusters", (clusters == 0) ? "-" : Integer.toString(clusters))
                .put("Silhouette Score", format(silhouetteScore))
                .put("Davies-Bouldin Index", format(daviesBouldinIndex))
                .put(" ", " ")
                .put("Cluster and cell count:", "");

        for (int c : clusterMap.keySet()) {
            clusteringSection.put("Cluster " + c, clusterMap.get(c)+"");
        }

        clusteringSection.render();
    }

    public void updateTangleStats(TangleSearchTree[] trees) {
        if (trees == null){
            tangleSection.clear().render();
            return;
        }

        tangleSection.clear();

        TangleSearchTree original = trees[0];
        TangleSearchTree splitPruned = trees[1];
        TangleSearchTree condensed = trees[2];

        addTreeInfo(original, "Original", "");
        addTreeInfo(splitPruned, "Split pruned", " ");
        addTreeInfo(condensed, "Condensed", "  ");

        tangleSection.render();
    }

    public void addTreeInfo(TangleSearchTree tree, String treeName, String id) {
        if (tree == null || (tree.getRoot().rightChild == null && tree.getRoot().leftChild == null)) return;

        HashMap<Integer, Integer> cutCountMap = computeCutCountMap(tree);

        int nodesSum = 0;
        int distinctCuts = 0;
        for (var entry : cutCountMap.entrySet()){
            if (entry.getKey() == HEIGHT_INDEX || entry.getKey() == SPLIT_COUNT_INDEX) continue;
            nodesSum += entry.getValue();
            distinctCuts++;
        }
        int treeHeight = cutCountMap.get(HEIGHT_INDEX);
        int splitCount = cutCountMap.get(SPLIT_COUNT_INDEX);

        tangleSection.put(treeName + " tangle search tree:", "")
                .put("Nodes" + id, nodesSum+"")
                .put("Tree height" + id, treeHeight+"")
                .put("Distinct cuts" + id, distinctCuts+"")
                .put("Split cuts" + id, splitCount+"")
                .put(" " + id, " ");
    }

    public void updatePerformance(double nmi, double randIndex, long clusterTime) {
        performanceSection.clear()
                .put("Cluster Time (s)", format((double) clusterTime / 1000))
                .put("NMI Score", format(nmi))
                .put("Rand Index Score", format(randIndex))
                .render();
    }

    private String format(double value) {
        if (value == 0) return "-";
        int decimals = 3;
        double factor = Math.pow(10, decimals);
        return Double.toString((double)Math.round(value * (factor)) / factor);
    }

    private HashMap<Integer, Integer> computeClusterMapping(int[] clusters) {
        HashMap<Integer, Integer> countsMap = new HashMap<>();
        for (int c : clusters) {
            int count = countsMap.getOrDefault(c, 0);
            countsMap.put(c, count + 1);
        }

        return countsMap;
    }

    private HashMap<Integer, Integer> computeCutCountMap(TangleSearchTree tree) {
        HashMap<Integer, Integer> cutCountMap = new HashMap<>();
        computeDistinctCutsRecursive(tree.getRoot(), cutCountMap, 0);
        return cutCountMap;
    }

    private void computeDistinctCutsRecursive(TangleSearchTree.Node node, HashMap<Integer, Integer> map, int height) {
        if (node == null) return;

        int idx = node.originalOrientation;
        map.put(idx, 1 + map.getOrDefault(idx, 0));

        int prevHeight = map.getOrDefault(HEIGHT_INDEX, 0);
        if (height > prevHeight) map.put(HEIGHT_INDEX, height);

        // Split node
        if (node.leftChild != null && node.rightChild != null) map.put(SPLIT_COUNT_INDEX, 1 + map.getOrDefault(SPLIT_COUNT_INDEX, 0));

        computeDistinctCutsRecursive(node.leftChild, map, height + 1);
        computeDistinctCutsRecursive(node.rightChild, map, height + 1);
    }

    private void appendSection(StringBuilder sb, String title, Section s) {
        sb.append("[").append(title).append("]\n");
        for (Map.Entry<String, String> e : s.data.entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\n");
    }

    private static final class Section extends JPanel {
        private final Map<String, String> data = new LinkedHashMap<>();
        private final JPanel grid = new JPanel(new GridBagLayout());
        private final GridBagConstraints gbc = new GridBagConstraints();

        private Section(String title) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(210, 210, 210)),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ),
                    title,
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP,
                    UIManager.getFont("Label.font").deriveFont(Font.BOLD, 13f)
            ));
            add(grid, BorderLayout.CENTER);

            gbc.insets = new Insets(1, 2, 1, 2);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
        }

        private Section clear() {
            data.clear(); return this;
        }

        private Section put(String key, String value) {
            data.put(key, value); return this;
        }

        private void render() {
            grid.removeAll();
            int row = 0;

            final Insets FIRST = new Insets(0, 2, 1, 2);
            final Insets REST  = new Insets(1, 2, 1, 2);

            for (Map.Entry<String, String> e : data.entrySet()) {
                JLabel k = new JLabel(e.getKey());
                k.setFont(k.getFont().deriveFont(Font.PLAIN, 12f));
                k.setForeground(new Color(70, 70, 70));

                JLabel v = new JLabel(e.getValue());
                v.setFont(v.getFont().deriveFont(Font.BOLD, 12f));
                v.setHorizontalAlignment(SwingConstants.RIGHT);

                // label (col 0, left)
                gbc.gridx = 0; gbc.gridy = row;
                gbc.weightx = 0.0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = (row == 0) ? FIRST : REST;
                gbc.fill = GridBagConstraints.NONE;
                grid.add(k, gbc);

                // spacer (col 1, expands)
                gbc.gridx = 1; gbc.gridy = row;
                gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.insets = (row == 0) ? FIRST : REST;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                grid.add(Box.createHorizontalGlue(), gbc);

                // value (col 2, right)
                gbc.gridx = 2; gbc.gridy = row;
                gbc.weightx = 0.0;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = (row == 0) ? FIRST : REST;
                gbc.fill = GridBagConstraints.NONE;
                grid.add(v, gbc);

                row++;
            }

            grid.revalidate();
            grid.repaint();
        }
    }
}


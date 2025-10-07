package visualization.data;

import datasets.ScRNAseqDataset;
import visualization.View;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;

public class StatisticsPanel extends JScrollPane {
    private final View view;

    // ==== outer grid now two columns ====
    private final JPanel content; // GridBagLayout

    private final Section dataSection;
    private final Section clusteringSection;
    private final Section tangleSection;
    private final Section performanceSection;

    private final JButton copyButton;

    public StatisticsPanel(View view) {
        this.view = view;

        content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setViewportView(content);

        // Tools
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        copyButton = new JButton("Copy All");
        copyButton.setFocusable(false);
        copyButton.addActionListener(e -> copyAllToClipboard());

        // Sections
        dataSection = new Section("Data Set");
        clusteringSection = new Section("Clustering");
        tangleSection = new Section("Tangles");
        performanceSection = new Section("Performance");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 2, 0);
        content.add(tools, gbc);
        tools.add(copyButton);

        // Left column
        addSection(dataSection,   0, 1);
        addSection(clusteringSection, 0, 2);

        // Right column
        addSection(tangleSection,     1, 1);
        addSection(performanceSection,1, 2);

        // Filler to push content to top
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 999;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        content.add(Box.createVerticalGlue(), gbc);

        getVerticalScrollBar().setUnitIncrement(16);
    }

    private void addSection(Section s, int col, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        // Add a small horizontal gutter between columns (right padding on left col)
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
        dataSection.clear()
                .put("Cells", Integer.toString(cells))
                .put("Highly Variable Genes", Integer.toString(hvg))
                .render();
    }

    public void updateClusteringStats(int[] clustering) {
        int k = (clustering == null) ? 0 : distinctCount(clustering);
        clusteringSection.clear()
                .put("Clusters", (k == 0) ? "-" : Integer.toString(k))
                .render();
    }

    public void updateTangleStats(int cuts) {
        tangleSection.clear()
                .put("Number of Cuts", Integer.toString(cuts))
                .render();
    }

    public void updatePerformance(double time, double nmi, double randIndex) {
        performanceSection.clear()
                .put("Time (ms)", "" + time)
                .put("NMI Score", "" + nmi)
                .put("Rand Index Score", "" + randIndex)
                .render();
    }

    private int distinctCount(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        java.util.BitSet seen = new java.util.BitSet();
        for (int v : arr) if (v >= 0) seen.set(v);
        return seen.cardinality();
    }

    private void copyAllToClipboard() {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "Data Set", dataSection);
        appendSection(sb, "Clustering", clusteringSection);
        appendSection(sb, "Tangles", tangleSection);
        appendSection(sb, "Performance", performanceSection);
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
    }

    private void appendSection(StringBuilder sb, String title, Section s) {
        sb.append("[").append(title).append("]\n");
        for (Map.Entry<String, String> e : s.data.entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\n");
    }

    private static final class Section extends JPanel {
        private final java.util.Map<String, String> data = new java.util.LinkedHashMap<>();
        private final JPanel grid = new JPanel(new GridBagLayout());
        private final GridBagConstraints gbc = new GridBagConstraints();

        Section(String title) {
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

        Section clear() {
            data.clear(); return this;
        }

        Section put(String key, String value) {
            data.put(key, value); return this;
        }

        void render() {
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


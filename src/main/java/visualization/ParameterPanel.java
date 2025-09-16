package visualization;

import clustering.TangleClusterer;
import util.BitSet;
import util.GlobalConstants;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Comparator;

public class ParameterPanel extends JPanel {
    private View view;

    // Constants
    private static final String FONT_NAME = "Arial";
    private static final int TITLE_TEXT_SIZE = 18;
    private static final int DEFAULT_TEXT_SIZE = 14;
    private static final Insets DEFAULT_INSETS = new Insets(5, 5, 5, 5);
    private static final Insets TITLE_INSETS = new Insets(25, 5, 10, 5);

    // Layout state
    private final GridBagConstraints gbc = new GridBagConstraints();
    private int row = 0;

    // Data
    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    // Algorithm section components
    private JCheckBox consistencyCheckbox;
    private JCheckBox wernerModificationCheckbox;
    private JComboBox<String> cutGeneratorDropdown;
    private JComboBox<String> costFunctionDropdown;

    // Cluster section components
    private JTextField aField;
    private JTextField psiField;
    private JButton clusterButton;
    private JCheckBox groundTruthCheckBox;

    // Cut section components
    private JCheckBox showCutCheckBox;
    private JTextField cutNumberField;
    private JButton minusButton;
    private JButton plusButton;

    public ParameterPanel(View view) {
        this.view = view;

        setLayout(new GridBagLayout());
        gbc.insets = DEFAULT_INSETS;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        buildAlgorithmSection();
        buildClusteringSection();
        buildCutsSection();

        addActions();
    }

    private void buildAlgorithmSection() {
        addTitle("Algorithm Modifications");

        consistencyCheckbox = new JCheckBox("<html>" +
                        "Consistency Check<br>" +
                        "Modification" +
                    "</html>");
        wernerModificationCheckbox = new JCheckBox("<html>" +
                    "Werner<br>" +
                    "Modification" +
                "</html>");
        addRow(consistencyCheckbox, wernerModificationCheckbox);

        cutGeneratorDropdown = new JComboBox<>(GlobalConstants.CUT_GENERATOR_NAMES);
        addRow("Cut Generator ", cutGeneratorDropdown);

        costFunctionDropdown = new JComboBox<>(GlobalConstants.COST_FUNCTION_NAMES);
        addRow("Cost Function ", costFunctionDropdown);
    }

    private void buildClusteringSection() {
        addTitle("Cluster Parameters");

        aField = new JTextField(10);
        addRow("a", aField);

        psiField = new JTextField(10);
        addRow("psi", psiField);

        clusterButton = new JButton("Cluster");
        groundTruthCheckBox = new JCheckBox("<html>Show Ground<br> Truth</html>");
        groundTruthCheckBox.setSelected(false);

        addRow(clusterButton, groundTruthCheckBox);
    }

    private void buildCutsSection() {
        addTitle("Cut Visualization");

        // A counter for cuts ( - [number] + )
        cutNumberField = new JTextField("0", 3);
        minusButton = new JButton("-");
        plusButton = new JButton("+");

        // Small panel to hold the three components
        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        counterPanel.add(minusButton);
        counterPanel.add(cutNumberField);
        counterPanel.add(plusButton);

        showCutCheckBox = new JCheckBox("Show cuts");
        showCutCheckBox.setSelected(false);

        addRow(counterPanel, showCutCheckBox);
    }

    private void addActions() {
        // ==================== Button Logic ==================== //
        clusterButton.addActionListener(this::clusterAction);
        plusButton.addActionListener(e -> stepCutCounter(1));
        minusButton.addActionListener(e -> stepCutCounter(-1));

        // ==================== Check Box Logic ==================== //
        /*consistencyCheckbox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (!isChecked) {
                wernerModificationCheckbox.setSelected(false);
            }
        });

        wernerModificationCheckbox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                consistencyCheckbox.setSelected(true);
            }
        });*/

        groundTruthCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                view.showGroundTruth();
                turnOffCuts();
            } else {
                view.showClustering();
            }
        });

        showCutCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                groundTruthCheckBox.setSelected(false);
                if (sortedCuts == null || sortedCuts.length == 0) {
                    view.showClustering();
                    return;
                }
                showCut(restrictCutIndex(parseCutNumberField()));
            } else {
                view.showClustering();
            }
        });



        // ==================== Text Field Logic ==================== //
        cutNumberField.addActionListener(e -> {
            if (showCutCheckBox.isSelected()) {
                try {
                    int value = Integer.parseInt(cutNumberField.getText());
                    if (value >= sortedCuts.length) {
                        value = sortedCuts.length - 1;
                        cutNumberField.setText(String.valueOf(value));
                    }
                    int cutIndex = restrictCutIndex(parseCutNumberField());
                    showCut(cutIndex);
                    System.out.println("Cut: " + cutIndex + " Cost: " + sortedCutCosts[cutIndex]);
                } catch (NumberFormatException ex) {
                    cutNumberField.setText("0");
                }
            }
        });
    }

    private void clusterAction(ActionEvent e) {
        int a;
        double psi;
        try {
            a = Integer.parseInt(aField.getText());
            psi = Double.parseDouble(psiField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Parameter \"a\" must be an integer and \"psi\" must be a double",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        view.performClustering(
                consistencyCheckbox.isSelected(),
                wernerModificationCheckbox.isSelected(),
                (String) cutGeneratorDropdown.getSelectedItem(),
                (String) costFunctionDropdown.getSelectedItem(),
                a,
                psi
        );
        view.drawTangleSearchTree(false);

        getAndSortCutsAndCosts();
        groundTruthCheckBox.setSelected(false);
        cutNumberField.setText("0");
        turnOffCuts();
    }

    private void stepCutCounter(int step) {
        if (!showCutCheckBox.isSelected() || sortedCuts == null || sortedCuts.length == 0) {
            return;
        }
        int cutIndex = parseCutNumberField() + step;
        cutIndex = restrictCutIndex(cutIndex);
        cutNumberField.setText(Integer.toString(cutIndex));
        showCut(cutIndex);
    }

    private int parseCutNumberField() {
        try {
            return Integer.parseInt(cutNumberField.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int restrictCutIndex(int cutIndex) {
        return Math.max(0, Math.min(cutIndex, sortedCuts.length));
    }

    private void showCut(int cutIndex) {
        if (sortedCuts == null || cutIndex < 0 || cutIndex >= sortedCuts.length) return;
        view.showCut(sortedCuts[cutIndex], cutIndex);
        turnOnCuts(cutIndex);
        System.out.println("Cut: " + cutIndex + " Cost: " + sortedCutCosts[cutIndex]);
    }

    private void getAndSortCutsAndCosts() {
        BitSet[] cuts = view.getCuts();
        double[] cutCosts = view.getCutCosts();
        Tuple<BitSet[], double[]> result = TangleClusterer.removeRedundantCuts(cuts, cutCosts, 0.9);
        cuts = result.x;
        cutCosts = result.y;

        int n = cutCosts.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        final double[] finalCutCosts = cutCosts;
        Arrays.sort(indices, Comparator.comparingDouble(i -> finalCutCosts[i]));
        BitSet[] cutsSorted = new BitSet[n];
        double[] costsSorted = new double[n];

        for (int i = 0; i < n; i++) {
            cutsSorted[i] = cuts[indices[i]];
            costsSorted[i] = cutCosts[indices[i]];
        }

        sortedCuts = cutsSorted;
        sortedCutCosts = costsSorted;
    }

    // ================= TOGGLES =================

    public void turnOnCuts(int cutIndex) {
        cutIndex = restrictCutIndex(cutIndex);
        cutNumberField.setText(Integer.toString(Math.min(cutIndex, sortedCuts.length - 1)));
        showCutCheckBox.setSelected(true);
    }

    public void turnOffCuts() {
        showCutCheckBox.setSelected(false);
    }

    // ================= LAYOUT HELPERS =================

    private void addTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font(FONT_NAME, Font.BOLD, TITLE_TEXT_SIZE));
        Insets previousInsets = gbc.insets;
        gbc.insets = TITLE_INSETS;
        addAt(title, 0, 2);
        gbc.insets = previousInsets;
        row++;
    }

    // Adds a label and a component on the same row
    private void addRow(String text, JComponent component) {
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font(FONT_NAME, Font.PLAIN, DEFAULT_TEXT_SIZE));
        addRow(textLabel, component);
    }

    // Adds two components side by side in a column
    private void addRow(JComponent left, JComponent right) {
        addAt(left, 0, 1);
        addAt(right, 1, 1);
        row++;
    }

    // Adds component spanning two columns
    private void addFullWidth(JComponent component) {
        addAt(component, 0, 2);
        row++;
    }

    // Adds component at given col index spanning spanColumns
    private void addAt(JComponent comp, int col, int spanColumns) {
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.gridwidth = spanColumns;
        gbc.weightx = (col == 1 || spanColumns == 2) ? 1.0 : 0.0;
        gbc.anchor = (col == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;

        add(comp, gbc);

        // Reset
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
    }
}

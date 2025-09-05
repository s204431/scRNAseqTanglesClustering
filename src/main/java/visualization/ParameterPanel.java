package visualization;

import clustering.TangleClusterer;
import util.BitSet;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Comparator;

public class ParameterPanel extends JPanel {
    private View view;

    // Font and text size
    private static final String FONT_NAME = "Arial";
    private static final int TITLE_TEXT_SIZE = 18;
    private static final int DEFAULT_TEXT_SIZE = 14;

    // Insets used for spacing between components
    private static final Insets DEFAULT_INSETS = new Insets(5, 5, 5, 5);
    private static final Insets TITLE_INSETS = new Insets(25, 20, 10, 20);

    private final GridBagConstraints gbc = new GridBagConstraints();

    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    private JTextField cutNumberField;
    private JCheckBox showCutCheckBox;

    public ParameterPanel(View view) {
        this.view = view;

        setLayout(new GridBagLayout());
        gbc.insets = DEFAULT_INSETS;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int x = 0;

        gbc.insets = TITLE_INSETS;
        addToPanel(x, 0, createTitleLabel("Dimension"));
        gbc.insets = DEFAULT_INSETS;
        x++;

        gbc.insets = TITLE_INSETS;
        addToPanel(x, 0, createTitleLabel("Clustering"));
        gbc.insets = DEFAULT_INSETS;
        x++;

        JTextField aField = new JTextField(10);
        addToPanel(x, 0, createTextLabel("a"));
        addToPanel(x, 1, aField);
        x++;

        JTextField psiField = new JTextField(10);
        addToPanel(x, 0, createTextLabel("psi"));
        addToPanel(x, 1, psiField);
        x++;

        String[] cutGeneratorNames = new String[] {"Simple", "Range", "Local Means"};
        JComboBox<String> cutGeneratorDropdown = new JComboBox<>(cutGeneratorNames);
        addToPanel(x, 0, createTextLabel("Cut Generator: "));
        addToPanel(x, 1, cutGeneratorDropdown);
        x++;

        String[] costFunctionNames = new String[] {"Distance To Mean", "Pairwise Distance"};
        JComboBox<String> costFunctionDropdown = new JComboBox<>(costFunctionNames);
        addToPanel(x, 0, createTextLabel("Cost Function: "));
        addToPanel(x, 1, costFunctionDropdown);
        x++;

        JButton clusterButton = new JButton("Cluster");
        JCheckBox groundTruthCheckBox = new JCheckBox("Show Ground Truth");
        groundTruthCheckBox.setSelected(false);
        addToPanel(x, 0, clusterButton);
        addToPanel(x, 1, groundTruthCheckBox);
        x++;
        x++;

        gbc.insets = TITLE_INSETS;
        addToPanel(x, 0, createTitleLabel("Cuts"));
        gbc.insets = DEFAULT_INSETS;
        x++;

        showCutCheckBox = new JCheckBox("Show cuts");
        showCutCheckBox.setSelected(false);
        addToPanel(x, 0, showCutCheckBox);

        // A text field with buttons on each side to increment or decrement the cut number
        cutNumberField = new JTextField("0", 3);
        JButton minusButton = new JButton("-");
        JButton plusButton = new JButton("+");

        // Small panel to hold the three components
        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        counterPanel.add(minusButton);
        counterPanel.add(cutNumberField);
        counterPanel.add(plusButton);
        counterPanel.add(minusButton);
        counterPanel.add(cutNumberField);
        counterPanel.add(plusButton);
        addToPanel(x, 1, counterPanel);
        x++;



        // ==================== Button Logic ==================== //
        clusterButton.addActionListener(e -> {
            try {
                Integer.parseInt(aField.getText());
                Double.parseDouble(psiField.getText());
            } catch (NumberFormatException ignore) {
                System.out.println("a-value has to be an integer. psi-value has to be a double.");
                return;
            }

            view.performClustering(
                    Integer.parseInt(aField.getText()),
                    Double.parseDouble(psiField.getText()),
                    (String) cutGeneratorDropdown.getSelectedItem(),
                    (String) costFunctionDropdown.getSelectedItem()
            );
            view.drawTangleSearchTree();
            getAndSortCutsAndCosts();
            groundTruthCheckBox.setSelected(false);
            turnOffCuts();
        });

        plusButton.addActionListener(e -> {
            try {
                int value = Integer.parseInt(cutNumberField.getText()) + 1;
                cutNumberField.setText(String.valueOf(value));
                if (sortedCuts != null && showCutCheckBox.isSelected()) {
                    if (value >= sortedCuts.length) {
                        value = sortedCuts.length - 1;
                        cutNumberField.setText(String.valueOf(value));
                    }
                    int currentCut = Integer.parseInt(cutNumberField.getText());
                    view.showCut(sortedCuts[currentCut], currentCut);
                    System.out.println(sortedCutCosts[currentCut]);
                }
            } catch (NumberFormatException ex) {
                cutNumberField.setText("0");
            }
        });

        minusButton.addActionListener(e -> {
            try {
                int value = Integer.parseInt(cutNumberField.getText()) - 1;
                if (value < 0) {
                    value = 0;
                }
                cutNumberField.setText(String.valueOf(value));
                if (sortedCuts != null && showCutCheckBox.isSelected()) {
                    int currentCut = Integer.parseInt(cutNumberField.getText());
                    view.showCut(sortedCuts[currentCut], currentCut);
                    System.out.println(sortedCutCosts[currentCut]);
                }
            } catch (NumberFormatException ex) {
                cutNumberField.setText("0");
            }
        });



        // ==================== Check Box Logic ==================== //
        groundTruthCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                showCutCheckBox.setSelected(false);
                view.showGroundTruth();
            } else {
                view.showClustering();
            }
        });

        showCutCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                groundTruthCheckBox.setSelected(false);
                if (sortedCuts == null) {
                    view.showClustering();
                    return;
                }
                int currentCut = Integer.parseInt(cutNumberField.getText());
                view.showCut(sortedCuts[currentCut], currentCut);
            } else {
                view.showClustering();
            }
        });



        // ==================== Text Field Logic ==================== //
        cutNumberField.addActionListener(e -> {
            if (showCutCheckBox.isSelected()) {
                int value = Integer.parseInt(cutNumberField.getText());
                if (value >= sortedCuts.length) {
                    value = sortedCuts.length - 1;
                    cutNumberField.setText(String.valueOf(value));
                }
                int currentCut = Integer.parseInt(cutNumberField.getText());
                view.showCut(sortedCuts[currentCut], currentCut);
                System.out.println(sortedCutCosts[currentCut]);
            }
        });
    }

    private void addToPanel(int x, int y, JComponent component) {
        gbc.gridy = x;
        gbc.gridx = y;
        add(component, gbc);
    }

    private JLabel createTitleLabel(String text) {
        JLabel titleLabel = new JLabel(text);
        titleLabel.setFont(new Font(FONT_NAME, Font.BOLD, TITLE_TEXT_SIZE));
        return titleLabel;
    }

    private JLabel createTextLabel(String text) {
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font(FONT_NAME, Font.PLAIN, DEFAULT_TEXT_SIZE));
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return textLabel;
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

    public void turnOnCuts(int cutIndex) {
        cutNumberField.setText("" + cutIndex);
        showCutCheckBox.setSelected(true);
    }

    public void turnOffCuts() {
        showCutCheckBox.setSelected(false);
    }
}

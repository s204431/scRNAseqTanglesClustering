package visualization;

import clustering.TangleClusterer;
import smile.plot.swing.Grid;
import util.BitSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Comparator;

public class ParameterPanel extends JPanel {
    private View view;

    private GridBagConstraints gbc = new GridBagConstraints();

    private String fontName = "Arial";
    private int titleSize = 18;
    private int textSize = 14;

    private BitSet[] cuts;
    private double[] cutCosts;

    public ParameterPanel(View view) {
        this.view = view;

        setLayout(new GridBagLayout());
        gbc.insets = new Insets(5, 5, 5, 5); // Spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int x = 0;

        addToPanel(x, 0, createTitleLabel("Dimension"));
        x++;
        x++;

        addToPanel(x, 0, createTitleLabel("Clustering"));
        x++;

        addToPanel(x, 0, createTextLabel("a"));

        JTextField aField = new JTextField(10);
        addToPanel(x, 1, aField);
        x++;

        addToPanel(x, 0, createTextLabel("psi"));

        JTextField psiField = new JTextField(10);
        addToPanel(x, 1, psiField);
        x++;

        JButton clusterButton = new JButton("Cluster");
        addToPanel(x, 0, clusterButton);

        JCheckBox groundTruthCheckBox = new JCheckBox("Show Ground Truth");
        groundTruthCheckBox.setSelected(false);
        addToPanel(x, 1, groundTruthCheckBox);
        x++;
        x++;

        addToPanel(x, 0, createTitleLabel("Cuts"));
        x++;

        addToPanel(x, 0, createTextLabel("Cut Generator: "));

        String[] cutGeneratorNames = new String[] {"Simple", "Range", "Local Means"};
        JComboBox<String> cutDropdown = new JComboBox<>(cutGeneratorNames);
        addToPanel(x, 1, cutDropdown);
        x++;

        JCheckBox showCutCheckBox = new JCheckBox("Show cuts");
        showCutCheckBox.setSelected(false);
        addToPanel(x, 0, showCutCheckBox);

        // A text field with buttons on each side to increment or decrement the cut number
        JTextField cutNumberField = new JTextField("0", 3);
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
                    "Range",
                    "Distance To Mean"
            );
        });

        plusButton.addActionListener(e -> {
            try {
                int value = Integer.parseInt(cutNumberField.getText()) + 1;
                cutNumberField.setText(String.valueOf(value));
                if (showCutCheckBox.isSelected()) {
                    String cutGenerator = (String) cutDropdown.getSelectedItem();
                    generateAndSortCutsAndCosts(cutGenerator);
                    if (value >= cuts.length) {
                        value = cuts.length - 1;
                        cutNumberField.setText(String.valueOf(value));
                    }
                    int currentCut = Integer.parseInt(cutNumberField.getText());
                    view.showCut(cuts[currentCut]);
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
                if (showCutCheckBox.isSelected()) {
                    String cutGenerator = (String) cutDropdown.getSelectedItem();
                    generateAndSortCutsAndCosts(cutGenerator);
                    int currentCut = Integer.parseInt(cutNumberField.getText());
                    view.showCut(cuts[currentCut]);
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
                String cutGenerator = (String) cutDropdown.getSelectedItem();
                generateAndSortCutsAndCosts(cutGenerator);
                int currentCut = Integer.parseInt(cutNumberField.getText());
                view.showCut(cuts[currentCut]);
            } else {
                view.showClustering();
            }
        });



        // ==================== Combo Box Logic ==================== //
        cutDropdown.addActionListener(e -> {
            if (showCutCheckBox.isSelected()) {
                String cutGenerator = (String) cutDropdown.getSelectedItem();
                generateAndSortCutsAndCosts(cutGenerator);
                int currentCut = Integer.parseInt(cutNumberField.getText());
                view.showCut(cuts[currentCut]);
            }
        });



        // ==================== Text Field Logic ==================== //
        cutNumberField.addActionListener(e -> {
            if (showCutCheckBox.isSelected()) {
                int value = Integer.parseInt(cutNumberField.getText());
                String cutGenerator = (String) cutDropdown.getSelectedItem();
                generateAndSortCutsAndCosts(cutGenerator);

                if (value >= cuts.length) {
                    value = cuts.length - 1;
                    cutNumberField.setText(String.valueOf(value));
                }
                int currentCut = Integer.parseInt(cutNumberField.getText());
                view.showCut(cuts[currentCut]);
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
        titleLabel.setFont(new Font(fontName, Font.BOLD, titleSize));
        return titleLabel;
    }

    private JLabel createTextLabel(String text) {
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font(fontName, Font.PLAIN, textSize));
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return textLabel;
    }

    private void generateAndSortCutsAndCosts(String cutGenerator) {
        cuts = view.getCuts(cutGenerator);
        cutCosts = view.getCutCosts("Distance To Mean");

        int n = cutCosts.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        Arrays.sort(indices, Comparator.comparingDouble(i -> cutCosts[i]));
        BitSet[] cutsSorted = new BitSet[n];
        double[] costsSorted = new double[n];

        for (int i = 0; i < n; i++) {
            cutsSorted[i] = cuts[indices[i]];
            costsSorted[i] = cutCosts[indices[i]];
        }

        cuts = cutsSorted;
        cutCosts = costsSorted;
    }
}

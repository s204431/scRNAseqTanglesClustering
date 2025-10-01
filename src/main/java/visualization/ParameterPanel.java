package visualization;

import clustering.TangleClusterer;
import main.Main;
import util.BitSet;
import util.Config;
import util.GlobalConstants;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Comparator;

public class ParameterPanel extends JPanel {
    private final View view;

    // Constants
    private static final String FONT_NAME = null;
    private static final int TITLE_TEXT_SIZE = 18;
    private static final int DEFAULT_TEXT_SIZE = 14;
    private static final Insets DEFAULT_INSETS = new Insets(0, 5, 5, 5);
    private static final Insets TITLE_INSETS = new Insets(0, 5, 5, 5);

    // Layout state
    private final GridBagConstraints gbc = new GridBagConstraints();
    private int row = 0;

    // Data
    private BitSet[] sortedCuts;
    private double[] sortedCutCosts;

    // Dimension reduction components
    private JTextField splitSizeField;
    private JTextField tsneComponentsField;

    // Algorithm section components
    private JCheckBox consistencyCheckbox;
    private JCheckBox wernerModificationCheckbox;
    private JCheckBox useCacheCheckBox;
    private JCheckBox removeRedundantCheckBox;
    private JComboBox<String> cutGeneratorDropdown;
    private JComboBox<String> highLevelCostFunctionDropdown;
    private JComboBox<String> lowLevelCostFunctionDropdown;

    // Cluster section components
    private JCheckBox autoComputeACheckBox;
    private JCheckBox autoComputePsiCheckBox;
    private JTextField aField;
    private JTextField psiField;
    private JButton clusterButton;
    private JButton pythonClusterButton;

    // Cut section components
    private JTextField cutNumberField;
    private JButton minusButton;
    private JButton plusButton;
    private JButton cutButton;

    // Test section components
    private JTextField runNumberField;
    private JCheckBox pythonCheckBox;
    private JButton testButton;

    private final boolean dataPanel;

    public ParameterPanel(View view, boolean dataPanel) {
        this.view = view;
        this.dataPanel = dataPanel;
        //setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = DEFAULT_INSETS;
        gbc.weightx = 0.0;

        buildDimensionSection();
        buildAlgorithmSection();
        buildClusteringSection();
        if (dataPanel) {
            buildCutsSection();
        } else {
            buildTestSection();
        }
        addActions();

        // Glue components to the top of the panel
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createGlue(), gbc);
    }

    private void buildDimensionSection() {
        addTitle("Dimension Reduction");

        splitSizeField = new JTextField(5);
        splitSizeField.setText("1000");
        addRow("Split size", splitSizeField);

        tsneComponentsField = new JTextField(5);
        tsneComponentsField.setText("5");
        addRow("TSNE components", tsneComponentsField);
    }

    private void buildAlgorithmSection() {
        addFullWidth(new JLabel(" "));

        addTitle("Algorithm Modifications");

        consistencyCheckbox = new JCheckBox("<html>" +
                "Consistency<br>" +
                " Check<br>" +
                "Modification" +
            "</html>");
        wernerModificationCheckbox = new JCheckBox("<html>" +
                "Werner<br>" +
                "Modification" +
            "</html>");
        consistencyCheckbox.setSelected(true);
        wernerModificationCheckbox.setSelected(true);
        addRow(consistencyCheckbox, wernerModificationCheckbox);

        useCacheCheckBox = new JCheckBox("Use Cache");
        useCacheCheckBox.setSelected(true);

        removeRedundantCheckBox = new JCheckBox("<html>Remove Cuts<br>Iteratively</html>");
        removeRedundantCheckBox.setSelected(false);
        addRow(useCacheCheckBox, removeRedundantCheckBox);

        cutGeneratorDropdown = new JComboBox<>(GlobalConstants.CUT_GENERATOR_NAMES);
        addRow("Cut Generator ", cutGeneratorDropdown);

        highLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.HIGH_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>High Level<br> Cost Function</html>", highLevelCostFunctionDropdown);

        lowLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.LOW_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>Low Level<br> Cost Function</html>", lowLevelCostFunctionDropdown);
    }

    private void buildClusteringSection() {
        addFullWidth(new JLabel(" "));

        addTitle("Cluster Parameters");

        aField = new JTextField(6);
        String aFieldText = " a ";
        if (!dataPanel) {
            aField.setText("0.667");
            //aFieldText += "(0-1) ";
        }
        autoComputeACheckBox = new JCheckBox("<html>Automatically<br> Compute a</html>");
        autoComputeACheckBox.setSelected(true);
        JPanel aComponent = new JPanel(new BorderLayout());
        aComponent.add(new JLabel(aFieldText), BorderLayout.WEST);
        aComponent.add(aField, BorderLayout.EAST);
        addRow(aComponent, autoComputeACheckBox);

        psiField = new JTextField(6);
        psiField.setText("0");
        autoComputePsiCheckBox = new JCheckBox("<html>Automatically<br> Compute ψ</html>");
        autoComputePsiCheckBox.setSelected(true);
        JPanel psiComponent = new JPanel(new BorderLayout());
        psiComponent.add(new JLabel(" ψ "), BorderLayout.WEST);
        psiComponent.add(psiField, BorderLayout.EAST);
        addRow(psiComponent, autoComputePsiCheckBox);

        if (dataPanel) {
            clusterButton = new JButton("Cluster Tangles");
            pythonClusterButton = new JButton("Cluster Scanpy");
            addRow(clusterButton, pythonClusterButton);
        }
    }

    private void buildCutsSection() {
        addFullWidth(new JLabel(" "));

        addTitle("Cut Visualization");

        // A counter for cuts ( - [number] + )
        cutNumberField = new JTextField("0", 3);
        minusButton = createStepButton("-");
        plusButton = createStepButton("+");

        // Small panel to hold the three components
        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        counterPanel.add(minusButton);
        counterPanel.add(cutNumberField);
        counterPanel.add(plusButton);

        cutButton = new JButton("Show Cut");
        addRow(counterPanel, cutButton);
    }

    private void buildTestSection() {
        addTitle("Test Parameters");

        runNumberField = new JTextField(3);
        runNumberField.setText("1");
        addRow("<html>Number of runs<br>on each test</html>", runNumberField);

        pythonCheckBox = new JCheckBox("<html>Compare With<br>Standard<br>Pipeline</html>");
        testButton = new JButton("Run Tests");
        addRow(testButton, pythonCheckBox);
    }

    private void addActions() {
        // ==================== Button Logic ==================== //
        if (dataPanel) {
            clusterButton.addActionListener(this::clusterAction);
            plusButton.addActionListener(e -> stepCutCounter(1));
            minusButton.addActionListener(e -> stepCutCounter(-1));
            cutButton.addActionListener(e -> readAndShowCut());
            pythonClusterButton.addActionListener(this::pythonClusterAction);
        } else {
            testButton.addActionListener(this::testAction);
        }


        // ==================== Check Box Logic ==================== //
        autoComputeACheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            aField.setEditable(!isChecked);
            aField.setEnabled(!isChecked);
        });

        autoComputePsiCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            psiField.setEditable(!isChecked);
            psiField.setEnabled(!isChecked);
        });



        // ==================== Text Field Logic ==================== //
        if (dataPanel) {
            cutNumberField.addActionListener(e -> {
                readAndShowCut();
            });
        }
    }

    private void readAndShowCut() {
        if (sortedCuts == null || sortedCuts.length == 0) return;

        try {
            int value = Integer.parseInt(cutNumberField.getText());
            if (value >= sortedCuts.length) {
                value = sortedCuts.length - 1;
                cutNumberField.setText(String.valueOf(value));
            }
            int cutIndex = restrictCutIndex(parseCutNumberField());
            showCut(cutIndex);
            //System.out.println("Cut: " + cutIndex + " Cost: " + sortedCutCosts[cutIndex]);
        } catch (NumberFormatException ex) {
            cutNumberField.setText("0");
        }
    }

    private void pythonClusterAction(ActionEvent e) {
        Tuple<int[], Double> result = Main.runPython(view.getCurrentFilePath());
        if (result == null) return;

        view.showClustering(result.x, false);
        cutNumberField.setText("0");
    }

    private void clusterAction(ActionEvent e) {
        int a;
        double psi;
        try {
            if (autoComputeACheckBox.isSelected()) {
                a = (int)((view.points.length/20.0)*0.7);
            }
            else {
                a = Integer.parseInt(aField.getText());
            }
            psi = Double.parseDouble(psiField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Parameter a must be an integer and ψ must be a double",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int hvg;
        int splitSize;
        int tsneComponents;
        try {
            splitSize = Integer.parseInt(splitSizeField.getText());
            tsneComponents = Integer.parseInt(tsneComponentsField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Highly variable genes, split size and TSNE components must be integers.",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Config config = getConfig(a, 0.0, psi, splitSize, tsneComponents);

        view.performClustering(config);
        view.drawTangleSearchTree();

        getAndSortCutsAndCosts();
        cutNumberField.setText("0");
    }

    private Config getConfig(int a, double aFactor, double psi, int splitSize, int tsneComponents) {
        Config config = new Config(consistencyCheckbox.isSelected(),
                wernerModificationCheckbox.isSelected(),
                useCacheCheckBox.isSelected(),
                (String) cutGeneratorDropdown.getSelectedItem(),
                (String) highLevelCostFunctionDropdown.getSelectedItem(),
                (String) lowLevelCostFunctionDropdown.getSelectedItem(),
                a,
                aFactor,
                psi);
        config.setAutoCompute(autoComputeACheckBox.isSelected(), autoComputePsiCheckBox.isSelected());
        config.setDimensionReductionParameters(splitSize, tsneComponents);
        config.setRemoveRedundant(removeRedundantCheckBox.isSelected());
        return config;
    }

    private void testAction(ActionEvent e) {
        double aFactor;
        double psi;
        try {
            aFactor = Double.parseDouble(aField.getText());
            if (aFactor < 0 || aFactor > 1) throw new NumberFormatException("a should be a factor between 0 and 1");
            psi = Double.parseDouble(psiField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Parameter a must be a double between 0 and 1, and ψ must be a double",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int hvg;
        int splitSize;
        int tsneComponents;
        try {
            splitSize = Integer.parseInt(splitSizeField.getText());
            tsneComponents = Integer.parseInt(tsneComponentsField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Highly variable genes, split size and TSNE components must be integers.",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int runs;
        try {
            runs = Integer.parseInt(runNumberField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Number of runs must be an integer.",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Config config = getConfig(0, aFactor, psi, splitSize, tsneComponents);
        view.runTestSetWithUI(config, runs, pythonCheckBox.isSelected());
    }

    private void stepCutCounter(int step) {
        if (sortedCuts == null || sortedCuts.length == 0) {
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
        return Math.max(0, Math.min(cutIndex, sortedCuts.length - 1));
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

    private JButton createStepButton(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusable(false);

        //FontMetrics fm = b.getFontMetrics(b.getFont());
        Dimension d = new Dimension(20, 20);
        b.setPreferredSize(d);
        //b.setMaximumSize(d);
        //b.setMinimumSize(d);
        return b;
    }

    // ================= TOGGLES =================
    public void turnOnCuts(int cutIndex) {
        cutIndex = restrictCutIndex(cutIndex);
        cutNumberField.setText(Integer.toString(Math.min(cutIndex, sortedCuts.length - 1)));
    }

    // ================= LAYOUT HELPERS =================
    private void addTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font(FONT_NAME, Font.BOLD, TITLE_TEXT_SIZE));
        Insets previousInsets = gbc.insets;
        gbc.insets = TITLE_INSETS;
        addFullWidth(title);
        gbc.insets = previousInsets;
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

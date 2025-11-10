package visualization;

import clustering.TangleClusterer;
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
    private static final int TITLE_TEXT_SIZE = 16;
    private static final int DEFAULT_TEXT_SIZE = 13;
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
    private JCheckBox useSplitFirstCheckbox;
    private JCheckBox disableEarlyStopCheckbox;
    private JCheckBox useCacheCheckBox;
    private JCheckBox removeRedundantCheckBox;
    private JComboBox<String> highLevelCutGeneratorDropdown;
    private JComboBox<String> lowLevelCutGeneratorDropdown;
    private JComboBox<String> highLevelCostFunctionDropdown;
    private JComboBox<String> lowLevelCostFunctionDropdown;
    private JCheckBox fastVersionCheckBox;
    private JCheckBox parameterTuningCheckBox;

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

    private boolean runningTests = false;

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

        // Placed at the end to trigger change actions of check boxes
        autoComputeACheckBox.setSelected(true);
        autoComputePsiCheckBox.setSelected(true);
    }

    private void buildDimensionSection() {
        addFiller(10);

        addTitle("Dimension Reduction");

        splitSizeField = new JTextField(5);
        splitSizeField.setText("1000");
        addRow("Split size", splitSizeField);

        tsneComponentsField = new JTextField(5);
        tsneComponentsField.setText("5");
        addRow("t-SNE Components", tsneComponentsField);
    }

    private void buildAlgorithmSection() {
        addFiller(5);

        addTitle("Algorithm Modifications");

        consistencyCheckbox = new JCheckBox("<html>" +
                "Consistency<br>" +
                "Modification" +
            "</html>");
        wernerModificationCheckbox = new JCheckBox("<html>" +
                "Werner<br>" +
                "Modification" +
            "</html>");
        consistencyCheckbox.setSelected(true);
        wernerModificationCheckbox.setSelected(true);
        addRow(consistencyCheckbox, wernerModificationCheckbox);

        useSplitFirstCheckbox = new JCheckBox("Split First");
        disableEarlyStopCheckbox = new JCheckBox("<html>Disable<br>Early Stop</html>");
        useSplitFirstCheckbox.setSelected(true);
        disableEarlyStopCheckbox.setSelected(true);
        addRow(useSplitFirstCheckbox, disableEarlyStopCheckbox);

        useCacheCheckBox = new JCheckBox("Use Cache");
        removeRedundantCheckBox = new JCheckBox("<html>Remove Cuts<br>Iteratively</html>");
        useCacheCheckBox.setSelected(true);
        removeRedundantCheckBox.setSelected(false);
        addRow(useCacheCheckBox, removeRedundantCheckBox);

        highLevelCutGeneratorDropdown = new JComboBox<>(GlobalConstants.HIGH_LEVEL_CUT_GENERATOR_NAMES);
        addRow("<html>High Level<br> Cut Generator</html>", highLevelCutGeneratorDropdown);

        lowLevelCutGeneratorDropdown = new JComboBox<>(GlobalConstants.LOW_LEVEL_CUT_GENERATOR_NAMES);
        addRow("<html>Low Level<br> Cut Generator</html>", lowLevelCutGeneratorDropdown);

        highLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.HIGH_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>High Level<br> Cost Function</html>", highLevelCostFunctionDropdown);

        lowLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.LOW_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>Low Level<br> Cost Function</html>", lowLevelCostFunctionDropdown);
    }

    private void buildClusteringSection() {
        addFiller(5);

        addTitle("Cluster Parameters");

        aField = new JTextField(6);
        String aFieldText = " a ";
        if (!dataPanel) {
            aField.setText("0.667");
            //aFieldText += "(0-1) ";
        }
        autoComputeACheckBox = new JCheckBox("<html>Automatically<br> Compute a</html>");
        autoComputeACheckBox.setSelected(false);
        JPanel aComponent = new JPanel(new BorderLayout());
        aComponent.add(new JLabel(aFieldText), BorderLayout.WEST);
        aComponent.add(aField, BorderLayout.EAST);
        addRow(aComponent, autoComputeACheckBox);

        psiField = new JTextField(6);
        psiField.setText("0");
        autoComputePsiCheckBox = new JCheckBox("<html>Automatically<br> Compute ψ</html>");
        autoComputePsiCheckBox.setSelected(false);
        JPanel psiComponent = new JPanel(new BorderLayout());
        psiComponent.add(new JLabel(" ψ "), BorderLayout.WEST);
        psiComponent.add(psiField, BorderLayout.EAST);
        addRow(psiComponent, autoComputePsiCheckBox);

        parameterTuningCheckBox = new JCheckBox("Tune Parameters");
        parameterTuningCheckBox.setSelected(false);

        fastVersionCheckBox = new JCheckBox("Fast Version");
        fastVersionCheckBox.setSelected(false);
        addRow(fastVersionCheckBox, parameterTuningCheckBox);

        if (dataPanel) {
            clusterButton = new JButton("Cluster Tangles");
            pythonClusterButton = new JButton("Cluster Scanpy");
            addRow(clusterButton, pythonClusterButton);
        }
    }

    private void buildCutsSection() {
        addFiller(5);

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
        addFiller(5);

        addTitle("Test Parameters");

        runNumberField = new JTextField(3);
        runNumberField.setText("1");
        addRow("<html>Number of runs<br>on each test</html>", runNumberField);

        pythonCheckBox = new JCheckBox("<html>Compare With<br>Scanpy</html>");
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
            aField.setEditable(!parameterTuningCheckBox.isSelected() && !isChecked);
            aField.setEnabled(!parameterTuningCheckBox.isSelected() && !isChecked);
        });

        autoComputePsiCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            psiField.setEditable(!parameterTuningCheckBox.isSelected() && !isChecked);
            psiField.setEnabled(!parameterTuningCheckBox.isSelected() && !isChecked);
        });

        parameterTuningCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            //autoComputeACheckBox.setSelected(false);
            //autoComputeACheckBox.setEnabled(!isChecked);
            //autoComputePsiCheckBox.setSelected(false);
            //autoComputePsiCheckBox.setEnabled(!isChecked);
            aField.setEditable(!autoComputeACheckBox.isSelected() && !isChecked);
            aField.setEnabled(!autoComputeACheckBox.isSelected() && !isChecked);
            psiField.setEditable(!autoComputePsiCheckBox.isSelected() && !isChecked);
            psiField.setEnabled(!autoComputePsiCheckBox.isSelected() && !isChecked);
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
        Tuple<int[], Double> result = view.getScanpyResult();
        if (result == null) return;

        view.showClustering(result.x, false);
        cutNumberField.setText("0");
    }

    private void clusterAction(ActionEvent e) {
        Config config = getConfig(false);
        if (config == null) {
            System.out.println("Error in Parameter Panel (cluster): Config is null");
            return;
        }

        view.performClustering(config);
        view.drawTangleSearchTree();

        getAndSortCutsAndCosts();
        cutNumberField.setText("0");
    }

    private void testAction(ActionEvent e) {
        if (runningTests) {
            view.stopTestingThread();
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

        Config config = getConfig(true);
        if (config == null) {
            System.out.println("Error in Parameter Panel (testing): Config is null");
            return;
        }

        testButton.setText("Stop");
        runningTests = true;
        view.runTestSetWithUI(config, runs, pythonCheckBox.isSelected());
    }

    public Config getConfig(boolean testing) {
        boolean useParameterTuning = parameterTuningCheckBox.isSelected();

        int a = 0;
        double aFactor = 0.0;
        double psi = 0;
        if (!testing && !useParameterTuning) {
            try {
                if (autoComputeACheckBox.isSelected()) {
                    a = (int) ((view.points.length / 20.0) * 0.7);
                } else {
                    a = Integer.parseInt(aField.getText());
                    if (a <= 0) throw new NumberFormatException("Parameter a is 0");
                }
                psi = Double.parseDouble(psiField.getText());
            } catch (NumberFormatException ignore) {
                JOptionPane.showMessageDialog(
                        this,
                        "Parameter a must be an integer greater than 0 and ψ must be a double",
                        "Invalid parameters",
                        JOptionPane.WARNING_MESSAGE
                );
                return null;
            }

        } else if (testing && !useParameterTuning) {
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
                return null;
            }
        }

        int splitSize;
        int tsneComponents;
        try {
            splitSize = Integer.parseInt(splitSizeField.getText());
            tsneComponents = Integer.parseInt(tsneComponentsField.getText());
        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Split size and TSNE components must be integers.",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        return new Config(
                consistencyCheckbox.isSelected(),
                wernerModificationCheckbox.isSelected(),
                useSplitFirstCheckbox.isSelected(),
                !disableEarlyStopCheckbox.isSelected(),
                useCacheCheckBox.isSelected(),
                (String) highLevelCutGeneratorDropdown.getSelectedItem(),
                (String) lowLevelCutGeneratorDropdown.getSelectedItem(),
                (String) highLevelCostFunctionDropdown.getSelectedItem(),
                (String) lowLevelCostFunctionDropdown.getSelectedItem(),
                a,
                aFactor,
                psi,
                autoComputeACheckBox.isSelected(),
                autoComputePsiCheckBox.isSelected(),
                useParameterTuning,
                fastVersionCheckBox.isSelected(),
                removeRedundantCheckBox.isSelected(),
                splitSize,
                tsneComponents
        );
    }

    public void setConfig(Config config) {
        consistencyCheckbox.setSelected(config.isUseAlternateConsistencyCheck());
        wernerModificationCheckbox.setSelected(config.isUseWernerModification());
        useSplitFirstCheckbox.setSelected(config.isUseSplitFirst());
        disableEarlyStopCheckbox.setSelected(!config.isUseEarlyStop());
        useCacheCheckBox.setSelected(config.isUseCache());
        removeRedundantCheckBox.setSelected(config.isRemoveRedundant());

        selectDropdown(highLevelCutGeneratorDropdown, config.getHighLevelCutGeneratorName());
        selectDropdown(lowLevelCutGeneratorDropdown, config.getLowLevelCutGeneratorName());
        selectDropdown(highLevelCostFunctionDropdown, config.getHighLevelCostFunctionName());
        selectDropdown(lowLevelCostFunctionDropdown, config.getLowLevelCostFunctionName());

        if (dataPanel) {
            aField.setText(Integer.toString(config.getA()));
        } else {
            aField.setText(Double.toString(config.getaFactor()));
        }
        psiField.setText(Double.toString(config.getPsi()));
        autoComputeACheckBox.setSelected(config.isAutoComputeA());
        autoComputePsiCheckBox.setSelected(config.isAutoComputePsi());
        parameterTuningCheckBox.setSelected(config.isTuneParameters());
        fastVersionCheckBox.setSelected(config.isUseFastVersion());
        splitSizeField.setText(Integer.toString(config.getSplitSize()));
        tsneComponentsField.setText(Integer.toString(config.getTsneComponents()));

        revalidate();
        repaint();
    }

    public void stopTesting() {
        testButton.setText("Run Tests");
        runningTests = false;
    }

    private void selectDropdown(JComboBox<String> combo, String value) {
        ComboBoxModel<String> model = combo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(value)) {
                combo.setSelectedItem(value);
                return;
            }
        }
        combo.setSelectedIndex(0);
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
        title.setFont(title.getFont().deriveFont(Font.BOLD, TITLE_TEXT_SIZE));
        Insets previousInsets = gbc.insets;
        gbc.insets = TITLE_INSETS;
        addFullWidth(title);
        gbc.insets = previousInsets;
    }

    // Adds a label and a component on the same row
    private void addRow(String text, JComponent component) {
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN, DEFAULT_TEXT_SIZE));
        addRow(textLabel, component);
    }

    // Adds two components side by side in a column
    private void addRow(JComponent left, JComponent right) {
        addAt(left, 0, 1);
        addAt(right, 1, 1);
        row++;
    }

    // Add vertical filler
    private void addFiller(int height) {
        addFullWidth(Box.createRigidArea(new Dimension(0, height)));
    }

    // Adds component spanning two columns
    private void addFullWidth(Component component) {
        addAt(component, 0, 2);
        row++;
    }

    // Adds component at given col index spanning spanColumns
    private void addAt(Component comp, int col, int spanColumns) {
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.gridwidth = spanColumns;
        gbc.weightx = (col == 1 || spanColumns == 2) ? 1.0 : 0.0;
        gbc.anchor = (col == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
        //gbc.anchor = GridBagConstraints.BASELINE_LEADING;

        add(comp, gbc);

        // Reset
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
    }
}

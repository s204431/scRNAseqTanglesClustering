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

public class ParameterPanel extends JScrollPane {
    private final View view;
    private final JPanel contentPanel = new JPanel();

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

    // Preprocessing section components
    private JTextField splitSizeCutGenerationField;
    private JCheckBox usePcaCutGenerationCheckbox;
    private JTextField pcaComponentsCutGenerationField;
    private JCheckBox useTsneCutGenerationCheckbox;
    private JTextField tsneComponentsCutGenerationField;
    private JTextField splitSizeCostFunctionField;
    private JCheckBox usePcaCostFunctionCheckbox;
    private JTextField pcaComponentsCostFunctionField;
    private JCheckBox useTsneCostFunctionCheckbox;
    private JTextField tsneComponentsCostFunctionField;

    // Algorithm section components
    private JCheckBox consistencyCheckbox;
    private JCheckBox wernerModificationCheckbox;
    private JCheckBox useSplitFirstCheckbox;
    private JCheckBox disableEarlyStopCheckbox;
    private JCheckBox useCacheCheckBox;
    private JCheckBox removeRedundantCutsCheckbox;
    private JCheckBox removeRedundantCutsIterativelyCheckBox;
    private JComboBox<String> highLevelCutGeneratorDropdown;
    private JComboBox<String> lowLevelCutGeneratorDropdown;
    private JComboBox<String> highLevelCostFunctionDropdown;
    private JComboBox<String> lowLevelCostFunctionDropdown;
    private JCheckBox parameterTuningCheckBox;

    // Cluster section components
    private JCheckBox splitPruningCheckBox;
    private JTextField aField;
    private JTextField psiField;
    private JButton clusterButton;
    private JButton pythonClusterButton;

    // Visualization section components
    private JTextField cutNumberField;
    private JButton minusButton;
    private JButton plusButton;
    private JButton cutButton;
    private JTextField uncertaintyTextField;
    private JButton removeUncertaintyButton;

    // Test section components
    private JTextField runNumberField;
    private JCheckBox pythonCheckBox;
    private JButton testButton;

    private final boolean dataPanel;

    private boolean runningTests = false;

    // Where the layout helpers should add components
    private JPanel targetPanel = contentPanel;
    private int sectionRow = 0;

    public ParameterPanel(View view, boolean dataPanel) {
        this.view = view;
        this.dataPanel = dataPanel;

        contentPanel.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = DEFAULT_INSETS;
        gbc.weightx = 0.0;

        buildPreprocessingSection();
        buildAlgorithmSection();
        buildClusteringSection();
        if (dataPanel) {
            buildVisualizationSection();
        } else {
            buildTestSection();
        }
        addActions();

        // Glue components to the top of the panel
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(Box.createGlue(), gbc);

        // Make parameter panel scrollable
        setViewportView(contentPanel);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        //setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setOpaque(true);
    }

    private void buildPreprocessingSection() {
        beginSection("Preprocessing Parameters");

        splitSizeCutGenerationField = new JTextField(5);
        splitSizeCutGenerationField.setText("1000");
        addRow("<html>Split Size<br> Cut Generation", splitSizeCutGenerationField);

        usePcaCutGenerationCheckbox = new JCheckBox("<html>Use PCA<br> Cut Generation</html>");
        usePcaCutGenerationCheckbox.setSelected(true);
        pcaComponentsCutGenerationField = new JTextField(5);
        pcaComponentsCutGenerationField.setText("10");
        addRow(usePcaCutGenerationCheckbox, pcaComponentsCutGenerationField);

        useTsneCutGenerationCheckbox = new JCheckBox("<html>Use t-SNE<br> Cut Generation</html>");
        useTsneCutGenerationCheckbox.setSelected(true);
        tsneComponentsCutGenerationField = new JTextField(5);
        tsneComponentsCutGenerationField.setText("3");
        addRow(useTsneCutGenerationCheckbox, tsneComponentsCutGenerationField);

        addFiller(5);

        splitSizeCostFunctionField = new JTextField(5);
        splitSizeCostFunctionField.setText("1000");
        addRow("<html>Split Size<br> Cost Function", splitSizeCostFunctionField);

        usePcaCostFunctionCheckbox = new JCheckBox("<html>Use PCA<br> Cost Function</html>");
        usePcaCostFunctionCheckbox.setSelected(false);
        pcaComponentsCostFunctionField = new JTextField(5);
        pcaComponentsCostFunctionField.setText("10");
        pcaComponentsCostFunctionField.setEnabled(false);
        pcaComponentsCostFunctionField.setEditable(false);
        addRow(usePcaCostFunctionCheckbox, pcaComponentsCostFunctionField);

        useTsneCostFunctionCheckbox = new JCheckBox("<html>Use t-SNE<br> Cost Function</html>");
        useTsneCostFunctionCheckbox.setSelected(true);
        tsneComponentsCostFunctionField = new JTextField(5);
        tsneComponentsCostFunctionField.setText("5");
        addRow(useTsneCostFunctionCheckbox, tsneComponentsCostFunctionField);

        endSection();
    }

    private void buildAlgorithmSection() {
        beginSection("Algorithm Modifications");

        consistencyCheckbox = new JCheckBox("<html>" +
                "Local<br>" +
                "Consistency" +
            "</html>");
        wernerModificationCheckbox = new JCheckBox("Local Costs");
        consistencyCheckbox.setSelected(true);
        wernerModificationCheckbox.setSelected(true);
        addRow(consistencyCheckbox, wernerModificationCheckbox);

        useSplitFirstCheckbox = new JCheckBox("Split First");
        disableEarlyStopCheckbox = new JCheckBox("<html>Skip<br>Inconsistent Cuts</html>");
        useSplitFirstCheckbox.setSelected(true);
        disableEarlyStopCheckbox.setSelected(true);
        addRow(useSplitFirstCheckbox, disableEarlyStopCheckbox);

        //useCacheCheckBox = new JCheckBox("Use Cache");
        //useCacheCheckBox.setSelected(true);

        removeRedundantCutsCheckbox = new JCheckBox("<html>Remove<br>Redundant Cuts</html>");
        removeRedundantCutsCheckbox.setSelected(true);
        removeRedundantCutsIterativelyCheckBox = new JCheckBox("<html>Remove Cuts<br>Iteratively</html>");
        removeRedundantCutsIterativelyCheckBox.setSelected(false);
        addRow(removeRedundantCutsCheckbox, removeRedundantCutsIterativelyCheckBox);

        highLevelCutGeneratorDropdown = new JComboBox<>(GlobalConstants.HIGH_LEVEL_CUT_GENERATOR_NAMES);
        addRow("<html>Cut Generator<br>Batching</html>", highLevelCutGeneratorDropdown);

        lowLevelCutGeneratorDropdown = new JComboBox<>(GlobalConstants.LOW_LEVEL_CUT_GENERATOR_NAMES);
        addRow("<html>Cut Generator</html>", lowLevelCutGeneratorDropdown);

        highLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.HIGH_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>Cost Function<br>Batching</html>", highLevelCostFunctionDropdown);

        lowLevelCostFunctionDropdown = new JComboBox<>(GlobalConstants.LOW_LEVEL_COST_FUNCTION_NAMES);
        addRow("<html>Cost Function</html>", lowLevelCostFunctionDropdown);

        endSection();
    }

    private void buildClusteringSection() {
        beginSection("Clustering Parameters");

        aField = new JTextField(6);
        String aFieldText = " a ";
        if (!dataPanel) {
            aField.setText("0.667");
            //aFieldText += "(0-1) ";
        }

        JPanel aComponent = new JPanel(new BorderLayout());
        aComponent.add(new JLabel(aFieldText), BorderLayout.WEST);
        aComponent.add(aField, BorderLayout.CENTER);
        addFullWidth(aComponent);

        psiField = new JTextField(6);
        psiField.setText("0");
        JPanel psiComponent = new JPanel(new BorderLayout());
        psiComponent.add(new JLabel(" ψ "), BorderLayout.WEST);
        psiComponent.add(psiField, BorderLayout.CENTER);
        addFullWidth(psiComponent);

        splitPruningCheckBox = new JCheckBox("Split Pruning");
        splitPruningCheckBox.setSelected(false);
        parameterTuningCheckBox = new JCheckBox("Tune Parameters");
        parameterTuningCheckBox.setSelected(false);
        addRow(splitPruningCheckBox, parameterTuningCheckBox);

        if (dataPanel) {
            clusterButton = new JButton("Cluster Tangles");
            pythonClusterButton = new JButton("Cluster Scanpy");
            addRow(clusterButton, pythonClusterButton);
        }

        endSection();
    }

    private void buildVisualizationSection() {
        beginSection("Visualization");

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

        addFullWidth(Box.createVerticalStrut(10));

        uncertaintyTextField = new JTextField("1.0", 3);
        addRow(new JLabel("Uncertainty Factor"), uncertaintyTextField);

        removeUncertaintyButton = new JButton("<html>Remove Uncertainty</html>");
        addFullWidth(removeUncertaintyButton);

        endSection();
    }

    private void buildTestSection() {
        beginSection("Testing Parameters");

        runNumberField = new JTextField(3);
        runNumberField.setText("1");
        addRow("<html>Number of runs<br>on each test</html>", runNumberField);

        pythonCheckBox = new JCheckBox("<html>Compare With<br>Scanpy</html>");
        testButton = new JButton("Run Tests");
        addRow(testButton, pythonCheckBox);

        endSection();
    }

    private void addActions() {
        // ==================== Button Logic ==================== //
        if (dataPanel) {
            clusterButton.addActionListener(this::clusterAction);
            plusButton.addActionListener(e -> stepCutCounter(1));
            minusButton.addActionListener(e -> stepCutCounter(-1));
            cutButton.addActionListener(e -> readAndShowCut());
            pythonClusterButton.addActionListener(this::pythonClusterAction);
            removeUncertaintyButton.addActionListener(this::removeUncertaintyAction);
        } else {
            testButton.addActionListener(this::testAction);
        }


        // ==================== Check Box Logic ==================== //
        splitPruningCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) parameterTuningCheckBox.setSelected(false);
            aField.setEditable(!isChecked);
            aField.setEnabled(!isChecked);
            psiField.setEditable(!isChecked);
            psiField.setEnabled(!isChecked);
        });

        parameterTuningCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) splitPruningCheckBox.setSelected(false);
            aField.setEditable(!isChecked);
            aField.setEnabled(!isChecked);
            psiField.setEditable(!isChecked);
            psiField.setEnabled(!isChecked);
        });

        usePcaCostFunctionCheckbox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            pcaComponentsCostFunctionField.setEditable(isChecked);
            pcaComponentsCostFunctionField.setEnabled(isChecked);

            useTsneCostFunctionCheckbox.setSelected(!isChecked);
            tsneComponentsCostFunctionField.setEditable(!isChecked);
            tsneComponentsCostFunctionField.setEnabled(!isChecked);
        });

        useTsneCostFunctionCheckbox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            tsneComponentsCostFunctionField.setEditable(isChecked);
            tsneComponentsCostFunctionField.setEnabled(isChecked);

            usePcaCostFunctionCheckbox.setSelected(!isChecked);
            pcaComponentsCostFunctionField.setEditable(!isChecked);
            pcaComponentsCostFunctionField.setEnabled(!isChecked);
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

    private void removeUncertaintyAction(ActionEvent e) {
        double certaintyThreshold;
        try {
            certaintyThreshold = Double.parseDouble(uncertaintyTextField.getText());
            System.out.println("Certainty threshold: " + certaintyThreshold);
            if (certaintyThreshold < 0.0 || certaintyThreshold > 1.0)
                throw new NumberFormatException("Certainty threshold out of bounds");

        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Certainty Threshold must be a double between 0.0 and 1.0.",
                    "Invalid parameters",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        view.removeUncertainPoints(certaintyThreshold);
    }

    private void clusterAction(ActionEvent e) {
        Config config = getConfig(false);
        if (config == null) {
            System.out.println("Error in Parameter Panel (cluster): Config is null");
            return;
        }

        view.performClustering(config);
        view.drawTangleSearchTree(config.isRemoveRedundantCuts());
        getAndSortCutsAndCosts(config.isRemoveRedundantCuts());
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
                if (splitPruningCheckBox.isSelected()) {
                    a = (int) ((view.points.length / 16.0) * 0.55);
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

        int splitSizeCutGeneration;
        boolean usePcaCutGeneration = usePcaCutGenerationCheckbox.isSelected();
        int pcaComponentsCutGeneration;
        boolean useTsneCutGeneration = useTsneCutGenerationCheckbox.isSelected();
        int tsneComponentsCutGeneration;

        int splitSizeCostFunction;
        boolean usePcaCostFunction = usePcaCostFunctionCheckbox.isSelected();
        int pcaComponentsCostFunction;
        boolean useTsneCostFunction = useTsneCostFunctionCheckbox.isSelected();
        int tsneComponentsCostFunction;
        try {
            splitSizeCutGeneration = Integer.parseInt(splitSizeCutGenerationField.getText());
            pcaComponentsCutGeneration = Integer.parseInt(pcaComponentsCutGenerationField.getText());
            tsneComponentsCutGeneration = Integer.parseInt(tsneComponentsCutGenerationField.getText());

            splitSizeCostFunction = Integer.parseInt(splitSizeCostFunctionField.getText());
            pcaComponentsCostFunction = Integer.parseInt(pcaComponentsCostFunctionField.getText());
            tsneComponentsCostFunction = Integer.parseInt(tsneComponentsCostFunctionField.getText());

        } catch (NumberFormatException ignore) {
            JOptionPane.showMessageDialog(
                    this,
                    "Split sizes and component values must be integers.",
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
                true,//useCacheCheckBox.isSelected(),
                (String) highLevelCutGeneratorDropdown.getSelectedItem(),
                (String) lowLevelCutGeneratorDropdown.getSelectedItem(),
                (String) highLevelCostFunctionDropdown.getSelectedItem(),
                (String) lowLevelCostFunctionDropdown.getSelectedItem(),
                a,
                aFactor,
                psi,
                splitPruningCheckBox.isSelected(),
                useParameterTuning,
                removeRedundantCutsCheckbox.isSelected(),
                removeRedundantCutsIterativelyCheckBox.isSelected(),
                splitSizeCutGeneration,
                usePcaCutGeneration,
                pcaComponentsCutGeneration,
                useTsneCutGeneration,
                tsneComponentsCutGeneration,
                splitSizeCostFunction,
                usePcaCostFunction,
                pcaComponentsCostFunction,
                useTsneCostFunction,
                tsneComponentsCostFunction
        );
    }

    public void setConfig(Config config) {
        consistencyCheckbox.setSelected(config.isUseAlternateConsistencyCheck());
        wernerModificationCheckbox.setSelected(config.isUseWernerModification());
        useSplitFirstCheckbox.setSelected(config.isUseSplitFirst());
        disableEarlyStopCheckbox.setSelected(!config.isUseEarlyStop());
        //useCacheCheckBox.setSelected(config.isUseCache());
        removeRedundantCutsCheckbox.setSelected(config.isRemoveRedundantCuts());
        removeRedundantCutsIterativelyCheckBox.setSelected(config.isRemoveRedundantCutsIteratively());

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
        splitPruningCheckBox.setSelected(config.isUseSplitPruning());
        parameterTuningCheckBox.setSelected(config.isTuneParameters());
        splitSizeCutGenerationField.setText(Integer.toString(config.getSplitSizeCutGeneration()));
        usePcaCutGenerationCheckbox.setSelected(config.isUsePcaCutGeneration());
        pcaComponentsCutGenerationField.setText(Integer.toString(config.getPcaComponentsCutGeneration()));
        useTsneCutGenerationCheckbox.setSelected(config.isUseTSNECutGeneration());
        tsneComponentsCutGenerationField.setText(Integer.toString(config.getTsneComponentsCutGeneration()));
        splitSizeCostFunctionField.setText(Integer.toString(config.getSplitSizeCostFunction()));
        usePcaCostFunctionCheckbox.setSelected(config.isUsePcaCostFunction());
        pcaComponentsCostFunctionField.setText(Integer.toString(config.getPcaComponentsCostFunction()));
        useTsneCostFunctionCheckbox.setSelected(config.isUseTSNECostFunction());
        tsneComponentsCostFunctionField.setText(Integer.toString(config.getTsneComponentsCostFunction()));

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

    private void getAndSortCutsAndCosts(boolean removeRedundantCuts) {
        BitSet[] cuts = view.getCuts();
        double[] cutCosts = view.getCutCosts();

        if (removeRedundantCuts) {
            Tuple<BitSet[], double[]> result = TangleClusterer.removeRedundantCuts(cuts, cutCosts, 0.9);
            cuts = result.x;
            cutCosts = result.y;
        }

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
    private void beginSection(String title) {
        CollapsibleSection section = new CollapsibleSection(title);
        section.getHeaderButton().setBackground(contentPanel.getBackground());

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(section, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        targetPanel = section.getBodyPanel();
        sectionRow = 0;
    }

    private void endSection() {
        targetPanel = contentPanel;
        sectionRow = 0;
    }

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
        if (targetPanel == contentPanel) {
            row++;
        } else {
            sectionRow++;
        }
    }

    // Add vertical filler
    private void addFiller(int height) {
        addFullWidth(Box.createRigidArea(new Dimension(0, height)));
    }

    // Adds component spanning two columns
    private void addFullWidth(Component component) {
        addAt(component, 0, 2);
        if (targetPanel == contentPanel) {
            row++;
        } else {
            sectionRow++;
        }
    }

    // Adds component at given col index spanning spanColumns
    private void addAt(Component comp, int col, int spanColumns) {
        GridBagConstraints newConstraints = new GridBagConstraints();
        newConstraints.gridx = col;
        newConstraints.gridy = (targetPanel == contentPanel) ? row : sectionRow;
        newConstraints.gridwidth = spanColumns;
        newConstraints.fill = GridBagConstraints.HORIZONTAL;
        newConstraints.insets = DEFAULT_INSETS;
        newConstraints.weightx = (col == 1 || spanColumns == 2) ? 1.0 : 0.0;
        newConstraints.anchor = (col == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
        targetPanel.add(comp, newConstraints);
    }

    // Collapsible section panel (reduces space used by parameter panel)
    private static final class CollapsibleSection extends JPanel {
        private final JButton headerButton;
        private final JPanel bodyPanel;

        private String openSymbol = "+  ";
        private String closeSymbol = "–  ";

        public CollapsibleSection(String title) {
            super(new BorderLayout());
            headerButton = new JButton(closeSymbol + title);
            headerButton.setFocusPainted(false);
            headerButton.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            headerButton.setFont(headerButton.getFont().deriveFont(Font.BOLD, TITLE_TEXT_SIZE));
            headerButton.setHorizontalAlignment(SwingConstants.LEFT);

            // Make it possible to change background color
            headerButton.setContentAreaFilled(false);
            headerButton.setOpaque(true);

            bodyPanel = new JPanel(new GridBagLayout());
            headerButton.addActionListener(e -> toggle());

            add(headerButton, BorderLayout.NORTH);
            add(bodyPanel, BorderLayout.CENTER);

            setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        }

        public JPanel getBodyPanel() {
            return bodyPanel;
        }

        public JButton getHeaderButton() {
            return headerButton;
        }

        public void toggle() {
            boolean visible = bodyPanel.isVisible();
            bodyPanel.setVisible(!visible);
            headerButton.setText((visible ? openSymbol : closeSymbol) + headerButton.getText().substring(3));
            revalidate(); repaint();

            Container p = getParent();
            while (p != null) {
                p.revalidate();
                p.repaint();
                p = p.getParent();
            }
        }
    }
}

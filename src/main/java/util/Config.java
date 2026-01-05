package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {
    private boolean useAlternateConsistencyCheck;
    private boolean useWernerModification;
    private boolean useSplitFirst;
    private boolean useEarlyStop;
    private boolean useCache;
    private String highLevelCutGeneratorName;
    private String lowLevelCutGeneratorName;
    private String highLevelCostFunctionName;
    private String lowLevelCostFunctionName;
    private int a;
    private double aFactor;
    private double psi;
    private boolean useSplitPruning;
    private boolean tuneParameters;
    private boolean removeRedundantCuts;
    private boolean removeRedundantCutsIteratively;
    private int splitSizeCutGeneration;
    private boolean usePcaCutGeneration;
    private int pcaComponentsCutGeneration;
    private boolean useTSNECutGeneration;
    private int tsneComponentsCutGeneration;
    private int splitSizeCostFunction;
    private boolean usePcaCostFunction;
    private int pcaComponentsCostFunction;
    private boolean useTSNECostFunction;
    private int tsneComponentsCostFunction;

    public Config() {
        this.useAlternateConsistencyCheck = true;
        this.useWernerModification = true;
        this.useSplitFirst = true;
        this.useEarlyStop = false;
        this.useCache = true;
        this.highLevelCutGeneratorName = "Default";
        this.lowLevelCutGeneratorName = "Default";
        this.highLevelCostFunctionName = "Default";
        this.lowLevelCostFunctionName = "Default";
        this.a = 0;
        this.aFactor = 0.667;
        this.psi = 0;
        this.useSplitPruning = false;
        setTuneParameters(false);
        setRemoveRedundantCuts(true);
        setRemoveRedundantCutsIteratively(false);
        setPreprocessingCutGeneration(1000, true, 10, true, 5);
        setPreprocessingCostFunction(1000, false, 10, true, 5);
    }

    public Config(boolean useAlternateConsistencyCheck,
                  boolean useWernerModification,
                  boolean useSplitFirst,
                  boolean useEarlyStop,
                  boolean useCache,
                  String highLevelCutGeneratorName,
                  String lowLevelCutGeneratorName,
                  String highLevelCostFunctionName,
                  String lowLevelCostFunctionName,
                  int a,
                  double aFactor,
                  double psi,
                  boolean useSplitPruning,
                  boolean tuneParameters,
                  boolean removeRedundantCuts,
                  boolean removeRedundantCutsIteratively,
                  int splitSizeCutGeneration,
                  boolean usePcaCutGeneration,
                  int pcaComponentsCutGeneration,
                  boolean useTSNECutGeneration,
                  int tsneComponentsCutGeneration,
                  int splitSizeCostFunction,
                  boolean usePcaCostFunction,
                  int pcaComponentsCostFunction,
                  boolean useTSNECostFunction,
                  int tsneComponentsCostFunction) {
        this.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
        this.useWernerModification = useWernerModification;
        this.useSplitFirst = useSplitFirst;
        this.useEarlyStop = useEarlyStop;
        this.useCache = useCache;
        this.highLevelCutGeneratorName = highLevelCutGeneratorName;
        this.lowLevelCutGeneratorName = lowLevelCutGeneratorName;
        this.highLevelCostFunctionName = highLevelCostFunctionName;
        this.lowLevelCostFunctionName = lowLevelCostFunctionName;
        this.a = a;
        this.aFactor = aFactor;
        this.psi = psi;
        this.useSplitPruning = useSplitPruning;
        this.tuneParameters = tuneParameters;
        this.removeRedundantCuts = removeRedundantCuts;
        this.removeRedundantCutsIteratively = removeRedundantCutsIteratively;
        this.splitSizeCutGeneration = splitSizeCutGeneration;
        this.usePcaCutGeneration = usePcaCutGeneration;
        this.pcaComponentsCutGeneration = pcaComponentsCutGeneration;
        this.useTSNECutGeneration = useTSNECutGeneration;
        this.tsneComponentsCutGeneration = tsneComponentsCutGeneration;
        this.splitSizeCostFunction = splitSizeCostFunction;
        this.usePcaCostFunction = usePcaCostFunction;
        this.pcaComponentsCostFunction = pcaComponentsCostFunction;
        this.useTSNECostFunction = useTSNECostFunction;
        this.tsneComponentsCostFunction = tsneComponentsCostFunction;
    }

    public Config(Config config) {
        this.useAlternateConsistencyCheck = config.useAlternateConsistencyCheck;
        this.useWernerModification = config.useWernerModification;
        this.useSplitFirst = config.useSplitFirst;
        this.useEarlyStop = config.useEarlyStop;
        this.useCache = config.useCache;
        this.highLevelCutGeneratorName = config.highLevelCutGeneratorName;
        this.lowLevelCutGeneratorName = config.lowLevelCutGeneratorName;
        this.highLevelCostFunctionName = config.highLevelCostFunctionName;
        this.lowLevelCostFunctionName = config.lowLevelCostFunctionName;
        this.a = config.a;
        this.aFactor = config.aFactor;
        this.psi = config.psi;
        this.useSplitPruning = config.useSplitPruning;
        this.tuneParameters = config.tuneParameters;
        this.removeRedundantCuts = config.removeRedundantCuts;
        this.removeRedundantCutsIteratively = config.removeRedundantCutsIteratively;
        this.splitSizeCutGeneration = config.splitSizeCutGeneration;
        this.usePcaCutGeneration = config.usePcaCutGeneration;
        this.pcaComponentsCutGeneration = config.pcaComponentsCutGeneration;
        this.useTSNECutGeneration = config.useTSNECutGeneration;
        this.tsneComponentsCutGeneration = config.tsneComponentsCutGeneration;
        this.splitSizeCostFunction = config.splitSizeCostFunction;
        this.usePcaCostFunction = config.usePcaCostFunction;
        this.pcaComponentsCostFunction = config.pcaComponentsCostFunction;
        this.useTSNECostFunction = config.useTSNECostFunction;
        this.tsneComponentsCostFunction = config.tsneComponentsCostFunction;
    }

    public void setTuneParameters(boolean tuneParameters) {
        this.tuneParameters = tuneParameters;
    }

    public void setPreprocessingCutGeneration(int splitSizeCutGeneration,
                                              boolean usePcaCutGeneration,
                                              int pcaComponentsCutGeneration,
                                              boolean useTSNECutGeneration,
                                              int tsneComponentsCutGeneration) {
        this.splitSizeCutGeneration = splitSizeCutGeneration;
        this.usePcaCutGeneration = usePcaCutGeneration;
        this.useTSNECutGeneration = useTSNECutGeneration;
        this.pcaComponentsCutGeneration = pcaComponentsCutGeneration;
        this.tsneComponentsCutGeneration = tsneComponentsCutGeneration;
    }

    public void setPreprocessingCostFunction(int splitSizeCostFunction,
                                             boolean usePcaCostFunction,
                                             int pcaComponentsCostFunction,
                                             boolean useTSNECostFunction,
                                             int tsneComponentsCostFunction) {
        this.splitSizeCostFunction = splitSizeCostFunction;
        this.usePcaCostFunction = usePcaCostFunction;
        this.useTSNECostFunction = useTSNECostFunction;
        this.pcaComponentsCostFunction = pcaComponentsCostFunction;
        this.tsneComponentsCostFunction = tsneComponentsCostFunction;
    }

    // =========== Helpers for saving and loading config files ===========
    private enum ConfigIndices {
        ALTERNATE_CONSISTENCY_CHECK_INDEX,
        WERNER_MODIFICATION_INDEX,
        SPLIT_FIRST_INDEX,
        EARLY_STOP_INDEX,
        CACHE_INDEX,
        HIGH_LEVEL_CUT_GENERATOR_INDEX,
        LOW_LEVEL_CUT_GENERATOR_INDEX,
        HIGH_LEVEL_COST_FUNCTION_INDEX,
        LOW_LEVEL_COST_FUNCTION_INDEX,
        A_INDEX,
        PSI_INDEX,
        A_FACTOR_INDEX,
        SPLIT_PRUNING_INDEX,
        TUNE_PARAMETERS,
        REMOVE_REDUNDANT_CUTS_INDEX,
        REMOVE_REDUNDANT_CUTS_ITERATIVELY_INDEX,
        SPLIT_SIZE_CUT_GENERATION_INDEX,
        USE_PCA_CUT_GENERATOR_INDEX,
        PCA_COMPONENTS_CUT_GENERATOR_INDEX,
        USE_TSNE_CUT_GENERATOR_INDEX,
        TSNE_COMPONENTS_CUT_GENERATOR_INDEX,
        SPLIT_SIZE_COST_FUNCTION_INDEX,
        USE_PCA_COST_FUNCTION_INDEX,
        PCA_COMPONENTS_COST_FUNCTION_INDEX,
        USE_TSNE_COST_FUNCTION_INDEX,
        TSNE_COMPONENTS_COST_FUNCTION_INDEX
    }

    private static final String[] CONFIG_DESCRIPTIONS = new String[ConfigIndices.values().length];
    static {
        CONFIG_DESCRIPTIONS[ConfigIndices.ALTERNATE_CONSISTENCY_CHECK_INDEX.ordinal()] = "use_alternate_consistency_check";
        CONFIG_DESCRIPTIONS[ConfigIndices.WERNER_MODIFICATION_INDEX.ordinal()] = "use_werner_modification";
        CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_FIRST_INDEX.ordinal()] = "use_split_first";
        CONFIG_DESCRIPTIONS[ConfigIndices.EARLY_STOP_INDEX.ordinal()] = "use_early_stop";
        CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()] = "use_cache";
        CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_CUT_GENERATOR_INDEX.ordinal()] = "high_level_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_CUT_GENERATOR_INDEX.ordinal()] = "low_level_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()] = "high_level_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()] = "low_level_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()] = "a";
        CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()] = "psi";
        CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()] = "a_factor";
        CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_PRUNING_INDEX.ordinal()] = "use_split_pruning";
        CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()] = "tune_parameters";
        CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()] = "remove_redundant_cuts";
        CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_ITERATIVELY_INDEX.ordinal()] = "remove_redundant_cuts_iteratively";
        CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_CUT_GENERATION_INDEX.ordinal()] = "split_size_cut_generation";
        CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_CUT_GENERATOR_INDEX.ordinal()] = "use_pca_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()] = "pca_components_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_CUT_GENERATOR_INDEX.ordinal()] = "use_tsne_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()] = "tsne_components_cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_COST_FUNCTION_INDEX.ordinal()] = "split_size_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_COST_FUNCTION_INDEX.ordinal()] = "use_pca_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_COST_FUNCTION_INDEX.ordinal()] = "pca_components_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_COST_FUNCTION_INDEX.ordinal()] = "use_tsne_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_COST_FUNCTION_INDEX.ordinal()] = "tsne_components_cost_function";
    }

    private String formatValue(boolean b) {
        return b ? "true" : "false";
    }

    private String formatValue(String s) {
        return s;
    }

    private String formatValue(int i) {
        return Integer.toString(i);
    }

    private String formatValue(double d) {
        return Double.toString(d);
    }

    public void saveConfiguration(String fileName) {
        if (!fileName.endsWith(".txt")) fileName += ".txt";
        Path configDir = Paths.get("config");
        try {
            Files.createDirectories(configDir);
            Path configPath = configDir.resolve(fileName);

            try (PrintWriter writer = new PrintWriter(configPath.toFile(), StandardCharsets.UTF_8)) {
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.ALTERNATE_CONSISTENCY_CHECK_INDEX.ordinal()] + ":" + formatValue(useAlternateConsistencyCheck));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.WERNER_MODIFICATION_INDEX.ordinal()] + ":" + formatValue(useWernerModification));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_FIRST_INDEX.ordinal()] + ":" + formatValue(useSplitFirst));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.EARLY_STOP_INDEX.ordinal()] + ":" + formatValue(useEarlyStop));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()] + ":" + formatValue(useCache));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(highLevelCutGeneratorName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(lowLevelCutGeneratorName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(highLevelCostFunctionName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(lowLevelCostFunctionName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()] + ":" + formatValue(a));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()] + ":" + formatValue(psi));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()] + ":" + formatValue(aFactor));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_PRUNING_INDEX.ordinal()] + ":" + formatValue(useSplitPruning));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()] + ":" + formatValue(tuneParameters));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()] + ":" + formatValue(removeRedundantCuts));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_ITERATIVELY_INDEX.ordinal()] + ":" + formatValue(removeRedundantCutsIteratively));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_CUT_GENERATION_INDEX.ordinal()] + ":" + formatValue(splitSizeCutGeneration));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(usePcaCutGeneration));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(pcaComponentsCutGeneration));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(useTSNECutGeneration));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(tsneComponentsCutGeneration));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(splitSizeCostFunction));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(usePcaCostFunction));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(pcaComponentsCostFunction));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(useTSNECostFunction));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(tsneComponentsCostFunction));
            }
            System.out.println("Config saved at " + configPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Config loadConfiguration(String fileName) {
        boolean useAlternateConsistencyCheck = false;
        boolean useWernerModification = false;
        boolean useSplitFirst = false;
        boolean useEarlyStop = false;
        boolean useCache = false;
        String highLevelCutGeneratorName = "Default";
        String lowLevelCutGeneratorName = "Default";
        String highLevelCostFunctionName = "Default";
        String lowLevelCostFunctionName = "Default";
        int a = 0;
        double aFactor = 0;
        double psi = 0;
        boolean useSplitPruning = false;
        boolean tuneParameters = false;
        boolean removeRedundantCuts = false;
        boolean removeRedundantCutsIteratively = false;
        int splitSizeCutGeneration = 1000;
        boolean usePcaCutGeneration = false;
        int pcaComponentsCutGeneration = 0;
        boolean useTSNECutGeneration = false;
        int tsneComponentsCutGeneration = 0;
        int splitSizeCostFunction = 1000;
        boolean usePcaCostFunction = false;
        int pcaComponentsCostFunction = 0;
        boolean useTSNECostFunction = false;
        int tsneComponentsCostFunction = 0;

        if (!fileName.contains(".txt")) fileName += ".txt";
        Path configPath = Paths.get("config", fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath.toFile()))) {
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(":");
                String description = parts[0];
                String value = parts[1];

                if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.ALTERNATE_CONSISTENCY_CHECK_INDEX.ordinal()])) useAlternateConsistencyCheck = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.WERNER_MODIFICATION_INDEX.ordinal()])) useWernerModification = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_FIRST_INDEX.ordinal()])) useSplitFirst = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.EARLY_STOP_INDEX.ordinal()])) useEarlyStop = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()])) useCache = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_CUT_GENERATOR_INDEX.ordinal()])) highLevelCutGeneratorName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_CUT_GENERATOR_INDEX.ordinal()])) lowLevelCutGeneratorName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()])) highLevelCostFunctionName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()])) lowLevelCostFunctionName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()])) a = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()])) psi = Double.parseDouble(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()])) aFactor = Double.parseDouble(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_PRUNING_INDEX.ordinal()])) useSplitPruning = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()])) tuneParameters = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()])) removeRedundantCuts = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_ITERATIVELY_INDEX.ordinal()])) removeRedundantCutsIteratively = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_CUT_GENERATION_INDEX.ordinal()])) splitSizeCutGeneration = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_CUT_GENERATOR_INDEX.ordinal()])) usePcaCutGeneration = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()])) pcaComponentsCutGeneration = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_CUT_GENERATOR_INDEX.ordinal()])) useTSNECutGeneration = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_CUT_GENERATOR_INDEX.ordinal()])) tsneComponentsCutGeneration = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_COST_FUNCTION_INDEX.ordinal()])) splitSizeCostFunction = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.USE_PCA_COST_FUNCTION_INDEX.ordinal()])) usePcaCostFunction = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.PCA_COMPONENTS_COST_FUNCTION_INDEX.ordinal()])) pcaComponentsCostFunction = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.USE_TSNE_COST_FUNCTION_INDEX.ordinal()])) useTSNECostFunction = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_COST_FUNCTION_INDEX.ordinal()])) tsneComponentsCostFunction = Integer.parseInt(value);
                else {
                    System.out.println("Error when loading config file:\nConfig file not formatted correctly");
                    return null;
                }

                line = reader.readLine();
            }

        } catch (IOException e) {
            System.out.println("Error when loading config file");
            e.printStackTrace();
        }

        Config newConfig = new Config();
        newConfig.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
        newConfig.useWernerModification = useWernerModification;
        newConfig.useSplitFirst = useSplitFirst;
        newConfig.useEarlyStop = useEarlyStop;
        newConfig.useCache = useCache;
        newConfig.highLevelCutGeneratorName = highLevelCutGeneratorName;
        newConfig.lowLevelCutGeneratorName = lowLevelCutGeneratorName;
        newConfig.highLevelCostFunctionName = highLevelCostFunctionName;
        newConfig.lowLevelCostFunctionName = lowLevelCostFunctionName;
        newConfig.a = a;
        newConfig.aFactor = aFactor;
        newConfig.psi = psi;
        newConfig.useSplitPruning = useSplitPruning;
        newConfig.tuneParameters = tuneParameters;
        newConfig.removeRedundantCuts = removeRedundantCuts;
        newConfig.removeRedundantCutsIteratively = removeRedundantCutsIteratively;
        newConfig.splitSizeCutGeneration = splitSizeCutGeneration;
        newConfig.usePcaCutGeneration = usePcaCutGeneration;
        newConfig.pcaComponentsCutGeneration = pcaComponentsCutGeneration;
        newConfig.useTSNECutGeneration = useTSNECutGeneration;
        newConfig.tsneComponentsCutGeneration = tsneComponentsCutGeneration;
        newConfig.splitSizeCostFunction = splitSizeCostFunction;
        newConfig.usePcaCostFunction = usePcaCostFunction;
        newConfig.pcaComponentsCostFunction = pcaComponentsCostFunction;
        newConfig.useTSNECostFunction = useTSNECostFunction;
        newConfig.tsneComponentsCostFunction = tsneComponentsCostFunction;
        return newConfig;
    }

    @Override
    public String toString() {
        return "Use Alternate Consistency Check: " + useAlternateConsistencyCheck + "\n" +
                "Use Werner Modification: " + useWernerModification + "\n" +
                "Use Split First: " + useSplitFirst + "\n" +
                "Use Early Stop: " + useEarlyStop + "\n" +
                "Use Cache: " + useCache + "\n" +
                "High Level Cut Generator: " + highLevelCutGeneratorName + "\n" +
                "Low Level Cut Generator: " + lowLevelCutGeneratorName + "\n" +
                "High Level Cost Function: " + highLevelCostFunctionName + "\n" +
                "Low Level Cost Function: " + lowLevelCostFunctionName + "\n" +
                "Parameter a: " + a + "\n" +
                "Parameter psi: " + psi + "\n" +
                "Factor a: " + aFactor + "\n" +
                "Split Pruning: " + useSplitPruning + "\n" +
                "Tune Parameters: " + tuneParameters + "\n" +
                "Remove Redundant Cuts: " + removeRedundantCuts + "\n" +
                "Remove Redundant Cuts Iteratively: " + removeRedundantCutsIteratively + "\n" +
                "Split Size Cut Generation: " + splitSizeCutGeneration + "\n" +
                "Use PCA Cut Generation: " + usePcaCutGeneration + "\n" +
                "PCA Components Cut Generation: " + pcaComponentsCutGeneration + "\n" +
                "Use TSNE Cut Generation: " + useTSNECutGeneration + "\n" +
                "TSNE Components Cut Generation: " + tsneComponentsCutGeneration + "\n" +
                "Split Size Cost Function: " + splitSizeCostFunction + "\n" +
                "Use PCA Cost Function: " + usePcaCostFunction + "\n" +
                "PCA Components Cost Function: " + pcaComponentsCostFunction + "\n" +
                "Use TSNE Cost Function: " + useTSNECostFunction + "\n" +
                "TSNE Components Cost Function: " + tsneComponentsCostFunction + "\n";
    }

    public boolean isUseAlternateConsistencyCheck() {
        return useAlternateConsistencyCheck;
    }

    public void setUseAlternateConsistencyCheck(boolean useAlternateConsistencyCheck) {
        this.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
    }

    public boolean isUseWernerModification() {
        return useWernerModification;
    }

    public void setUseWernerModification(boolean useWernerModification) {
        this.useWernerModification = useWernerModification;
    }

    public boolean isUseSplitFirst() {
        return useSplitFirst;
    }

    public void setUseSplitFirst(boolean useSplitFirst) {
        this.useSplitFirst = useSplitFirst;
    }

    public boolean isUseEarlyStop() {
        return useEarlyStop;
    }

    public void setUseEarlyStop(boolean useEarlyStop) {
        this.useEarlyStop = useEarlyStop;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public String getHighLevelCutGeneratorName() {
        return highLevelCutGeneratorName;
    }

    public void setHighLevelCutGeneratorName(String cutGeneratorName) {
        this.highLevelCutGeneratorName = cutGeneratorName;
    }

    public String getLowLevelCutGeneratorName() {
        return lowLevelCutGeneratorName;
    }

    public void setLowLevelCutGeneratorName(String cutGeneratorName) {
        this.lowLevelCutGeneratorName = cutGeneratorName;
    }

    public String getHighLevelCostFunctionName() {
        return highLevelCostFunctionName;
    }

    public void setHighLevelCostFunctionName(String highLevelCostFunctionName) {
        this.highLevelCostFunctionName = highLevelCostFunctionName;
    }

    public String getLowLevelCostFunctionName() {
        return lowLevelCostFunctionName;
    }

    public void setLowLevelCostFunctionName(String lowLevelCostFunctionName) {
        this.lowLevelCostFunctionName = lowLevelCostFunctionName;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public double getaFactor() {
        return aFactor;
    }

    public void setaFactor(double aFactor) {
        this.aFactor = aFactor;
    }

    public double getPsi() {
        return psi;
    }

    public void setPsi(double psi) {
        this.psi = psi;
    }

    public boolean isUseSplitPruning() {
        return useSplitPruning;
    }

    public void setUseSplitPruning(boolean useSplitPruning) {
        this.useSplitPruning = useSplitPruning;
    }

    public boolean isTuneParameters() {
        return tuneParameters;
    }

    public boolean isRemoveRedundantCuts() {
        return removeRedundantCuts;
    }

    public void setRemoveRedundantCuts(boolean removeRedundantCuts) {
        this.removeRedundantCuts = removeRedundantCuts;
    }

    public boolean isRemoveRedundantCutsIteratively() {
        return removeRedundantCutsIteratively;
    }

    public void setRemoveRedundantCutsIteratively(boolean removeRedundantCutsIteratively) {
        this.removeRedundantCutsIteratively = removeRedundantCutsIteratively;
    }

    public boolean isUsePcaCutGeneration() {
        return usePcaCutGeneration;
    }

    public void setUsePcaCutGeneration(boolean usePcaCutGeneration) {
        this.usePcaCutGeneration = usePcaCutGeneration;
    }

    public int getPcaComponentsCutGeneration() {
        return pcaComponentsCutGeneration;
    }

    public void setPcaComponentsCutGeneration(int pcaComponentsCutGeneration) {
        this.pcaComponentsCutGeneration = pcaComponentsCutGeneration;
    }

    public boolean isUseTSNECutGeneration() {
        return useTSNECutGeneration;
    }

    public void setUseTSNECutGeneration(boolean useTSNECutGeneration) {
        this.useTSNECutGeneration = useTSNECutGeneration;
    }

    public int getTsneComponentsCutGeneration() {
        return tsneComponentsCutGeneration;
    }

    public void setTsneComponentsCutGeneration(int tsneComponentsCutGeneration) {
        this.tsneComponentsCutGeneration = tsneComponentsCutGeneration;
    }

    public boolean isUsePcaCostFunction() {
        return usePcaCostFunction;
    }

    public void setUsePcaCostFunction(boolean usePcaCostFunction) {
        this.usePcaCostFunction = usePcaCostFunction;
    }

    public int getPcaComponentsCostFunction() {
        return pcaComponentsCostFunction;
    }

    public void setPcaComponentsCostFunction(int pcaComponentsCostFunction) {
        this.pcaComponentsCostFunction = pcaComponentsCostFunction;
    }

    public boolean isUseTSNECostFunction() {
        return useTSNECostFunction;
    }

    public void setUseTSNECostFunction(boolean useTSNECostFunction) {
        this.useTSNECostFunction = useTSNECostFunction;
    }

    public int getTsneComponentsCostFunction() {
        return tsneComponentsCostFunction;
    }

    public void setTsneComponentsCostFunction(int tsneComponentsCostFunction) {
        this.tsneComponentsCostFunction = tsneComponentsCostFunction;
    }

    public int getSplitSizeCutGeneration() {
        return splitSizeCutGeneration;
    }

    public void setSplitSizeCutGeneration(int splitSizeCutGeneration) {
        this.splitSizeCutGeneration = splitSizeCutGeneration;
    }

    public int getSplitSizeCostFunction() {
        return splitSizeCostFunction;
    }

    public void setSplitSizeCostFunction(int splitSizeCostFunction) {
        this.splitSizeCostFunction = splitSizeCostFunction;
    }
}

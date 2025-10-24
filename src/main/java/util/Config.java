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
    private boolean autoComputeA;
    private boolean autoComputePsi;
    private boolean tuneParameters;
    private boolean useFastVersion;
    private boolean removeRedundant;
    private int splitSize;
    private int tsneComponents;

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
        setAutoCompute(true, true);
        setTuneParameters(false);
        setUseFastVersion(false);
        setRemoveRedundant(false);
        setDimensionReductionParameters(1000, 5);
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
                  boolean autoComputeA,
                  boolean autoComputePsi,
                  boolean tuneParameters,
                  boolean useFastVersion,
                  boolean removeRedundant,
                  int splitSize,
                  int tsneComponents) {
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
        this.autoComputeA = autoComputeA;
        this.autoComputePsi = autoComputePsi;
        this.tuneParameters = tuneParameters;
        this.useFastVersion = useFastVersion;
        this.removeRedundant = removeRedundant;
        this.splitSize = splitSize;
        this.tsneComponents = tsneComponents;
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
        this.autoComputeA = config.autoComputeA;
        this.autoComputePsi = config.autoComputePsi;
        this.tuneParameters = config.tuneParameters;
        this.useFastVersion = config.useFastVersion;
        this.removeRedundant = config.removeRedundant;
        this.splitSize = config.splitSize;
        this.tsneComponents = config.tsneComponents;
    }

    public void setAutoCompute(boolean a, boolean psi) {
        this.autoComputeA = a;
        this.autoComputePsi = psi;
    }

    public void setTuneParameters(boolean tuneParameters) {
        this.tuneParameters = tuneParameters;
    }

    public void setUseFastVersion(boolean useFastVersion) {
        this.useFastVersion = useFastVersion;
    }

    public void setDimensionReductionParameters(int splitSize, int tsneComponents) {
        this.splitSize = splitSize;
        this.tsneComponents = tsneComponents;
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
        AUTO_COMPUTE_A_INDEX,
        AUTO_COMPUTE_PSI_INDEX,
        TUNE_PARAMETERS,
        FAST_VERSION,
        REMOVE_REDUNDANT_CUTS_INDEX,
        SPLIT_SIZE_INDEX,
        TSNE_COMPONENTS_INDEX
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
        CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()] = "auto_compute_a";
        CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()] = "auto_compute_psi";
        CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()] = "tune_parameters";
        CONFIG_DESCRIPTIONS[ConfigIndices.FAST_VERSION.ordinal()] = "use_fast_version";
        CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()] = "remove_redundant_cuts";
        CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_INDEX.ordinal()] = "split_size";
        CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_INDEX.ordinal()] = "tsne_components";
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
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()] + ":" + formatValue(autoComputeA));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()] + ":" + formatValue(autoComputePsi));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()] + ":" + formatValue(tuneParameters));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.FAST_VERSION.ordinal()] + ":" + formatValue(useFastVersion));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()] + ":" + formatValue(removeRedundant));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_INDEX.ordinal()] + ":" + formatValue(splitSize));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_INDEX.ordinal()] + ":" + formatValue(tsneComponents));
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
        boolean autoComputeA = false;
        boolean autoComputePsi = false;
        boolean tuneParameters = false;
        boolean useFastVersion = false;
        boolean removeRedundant = false;
        int splitSize = 0;
        int tsneComponents = 0;

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
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()])) autoComputeA = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()])) autoComputePsi = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.TUNE_PARAMETERS.ordinal()])) tuneParameters = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.FAST_VERSION.ordinal()])) useFastVersion = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.REMOVE_REDUNDANT_CUTS_INDEX.ordinal()])) removeRedundant = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.SPLIT_SIZE_INDEX.ordinal()])) splitSize = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.TSNE_COMPONENTS_INDEX.ordinal()])) tsneComponents = Integer.parseInt(value);
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
        newConfig.autoComputeA = autoComputeA;
        newConfig.autoComputePsi = autoComputePsi;
        newConfig.tuneParameters = tuneParameters;
        newConfig.useFastVersion = useFastVersion;
        newConfig.removeRedundant = removeRedundant;
        newConfig.splitSize = splitSize;
        newConfig.tsneComponents = tsneComponents;
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
                "Auto Compute a: " + autoComputeA + "\n" +
                "Auto Compute psi: " + autoComputePsi + "\n" +
                "Tune Parameters: " + tuneParameters + "\n" +
                "Use Fast Version" + useFastVersion + "\n" +
                "Remove Redundant Cuts: " + removeRedundant + "\n" +
                "Split Size: " + splitSize + "\n" +
                "t-SNE Components: " + tsneComponents;
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

    public boolean isAutoComputeA() {
        return autoComputeA;
    }

    public void setAutoComputeA(boolean autoComputeA) {
        this.autoComputeA = autoComputeA;
    }

    public boolean isAutoComputePsi() {
        return autoComputePsi;
    }

    public void setAutoComputePsi(boolean autoComputePsi) {
        this.autoComputePsi = autoComputePsi;
    }

    public boolean isTuneParameters() {
        return tuneParameters;
    }

    public boolean isUseFastVersion() {
        return useFastVersion;
    }

    public boolean isRemoveRedundant() {
        return removeRedundant;
    }

    public void setRemoveRedundant(boolean removeRedundant) {
        this.removeRedundant = removeRedundant;
    }

    public int getSplitSize() {
        return splitSize;
    }

    public void setSplitSize(int splitSize) {
        this.splitSize = splitSize;
    }

    public int getTsneComponents() {
        return tsneComponents;
    }

    public void setTsneComponents(int tsneComponents) {
        this.tsneComponents = tsneComponents;
    }
}

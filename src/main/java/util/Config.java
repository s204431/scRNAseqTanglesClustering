package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {
    private final boolean useAlternateConsistencyCheck;
    private final boolean useWernerModification;
    private final boolean useCache;
    private final String cutGeneratorName;
    private final String highLevelCostFunctionName;
    private final String lowLevelCostFunctionName;
    private final int a;
    private final double aFactor;
    private final double psi;
    private boolean autoComputeA;
    private boolean autoComputePsi;
    private boolean removeRedundant;
    private int splitSize;
    private int tsneComponents;

    public Config() {
        this.useAlternateConsistencyCheck = true;
        this.useWernerModification = true;
        this.useCache = true;
        this.cutGeneratorName = "Default";
        this.highLevelCostFunctionName = "Default";
        this.lowLevelCostFunctionName = "Default";
        this.a = 0;
        this.aFactor = 0.667;
        this.psi = 0;
        setAutoCompute(true, true);
        setRemoveRedundant(false);
        setDimensionReductionParameters(1000, 5);
    }

    public Config(int a) {
        this.useAlternateConsistencyCheck = true;
        this.useWernerModification = true;
        this.useCache = true;
        this.cutGeneratorName = "Default";
        this.highLevelCostFunctionName = "Default";
        this.lowLevelCostFunctionName = "Default";
        this.a = a;
        this.aFactor = 0.667;
        this.psi = 0;
        setAutoCompute(false, false);
        setRemoveRedundant(false);
        setDimensionReductionParameters(1000, 5);
    }

    public Config(boolean useAlternateConsistencyCheck,
                  boolean useWernerModification,
                  boolean useCache,
                  String cutGeneratorName,
                  String highLevelCostFunctionName,
                  String lowLevelCostFunctionName,
                  int a,
                  double aFactor,
                  double psi) {
        this.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
        this.useWernerModification = useWernerModification;
        this.useCache = useCache;
        this.cutGeneratorName = cutGeneratorName;
        this.highLevelCostFunctionName = highLevelCostFunctionName;
        this.lowLevelCostFunctionName = lowLevelCostFunctionName;
        this.a = a;
        this.aFactor = aFactor;
        this.psi = psi;
        setAutoCompute(false, false);
        setRemoveRedundant(false);
        setDimensionReductionParameters(1000, 5);
    }

    public void setAutoCompute(boolean a, boolean psi) {
        this.autoComputeA = a;
        this.autoComputePsi = psi;
    }

    public void setDimensionReductionParameters(int splitSize, int tsneComponents) {
        this.splitSize = splitSize;
        this.tsneComponents = tsneComponents;
    }

    public void setRemoveRedundant(boolean remove) {
        this.removeRedundant = remove;
    }

    public boolean isUseAlternateConsistencyCheck() {
        return useAlternateConsistencyCheck;
    }

    public boolean isUseWernerModification() {
        return useWernerModification;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public boolean isRemoveRedundant() {
        return removeRedundant;
    }

    public String getCutGeneratorName() {
        return cutGeneratorName;
    }

    public String getHighLevelCostFunctionName() {
        return highLevelCostFunctionName;
    }

    public String getLowLevelCostFunctionName() {
        return lowLevelCostFunctionName;
    }

    public int getA() {
        return a;
    }

    public double getaFactor() {
        return aFactor;
    }

    public double getPsi() {
        return psi;
    }

    public boolean isAutoComputeA() {
        return autoComputeA;
    }

    public boolean isAutoComputePsi() {
        return autoComputePsi;
    }

    public int getSplitSize() {
        return splitSize;
    }

    public int getTsneComponents() {
        return tsneComponents;
    }


    // =========== Helpers for saving and loading config files ===========
    private enum ConfigIndices {
        ALTERNATE_CONSISTENCY_CHECK_INDEX,
        WERNER_MODIFICATION_INDEX,
        CACHE_INDEX,
        CUT_GENERATOR_INDEX,
        HIGH_LEVEL_COST_FUNCTION_INDEX,
        LOW_LEVEL_COST_FUNCTION_INDEX,
        A_INDEX,
        PSI_INDEX,
        A_FACTOR_INDEX,
        AUTO_COMPUTE_A_INDEX,
        AUTO_COMPUTE_PSI_INDEX,
        REMOVE_REDUNDANT_CUTS_INDEX,
        SPLIT_SIZE_INDEX,
        TSNE_COMPONENTS_INDEX
    }

    private static final String[] CONFIG_DESCRIPTIONS = new String[ConfigIndices.values().length];
    static {
        CONFIG_DESCRIPTIONS[ConfigIndices.ALTERNATE_CONSISTENCY_CHECK_INDEX.ordinal()] = "use_alternate_consistency_check";
        CONFIG_DESCRIPTIONS[ConfigIndices.WERNER_MODIFICATION_INDEX.ordinal()] = "cse_werner_modification";
        CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()] = "use_cache";
        CONFIG_DESCRIPTIONS[ConfigIndices.CUT_GENERATOR_INDEX.ordinal()] = "cut_generator";
        CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()] = "high_level_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()] = "low_level_cost_function";
        CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()] = "a";
        CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()] = "psi";
        CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()] = "a_factor";
        CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()] = "auto_compute_a";
        CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()] = "auto_compute_psi";
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
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()] + ":" + formatValue(useCache));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.CUT_GENERATOR_INDEX.ordinal()] + ":" + formatValue(cutGeneratorName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(highLevelCostFunctionName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()] + ":" + formatValue(lowLevelCostFunctionName));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()] + ":" + formatValue(a));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()] + ":" + formatValue(psi));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()] + ":" + formatValue(aFactor));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()] + ":" + formatValue(autoComputeA));
                writer.println(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()] + ":" + formatValue(autoComputePsi));
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
        boolean useCache = false;
        String cutGeneratorName = "Default";
        String highLevelCostFunctionName = "Default";
        String lowLevelCostFunctionName = "Default";
        int a = 0;
        double aFactor = 0;
        double psi = 0;
        boolean autoComputeA = false;
        boolean autoComputePsi = false;
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
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.CACHE_INDEX.ordinal()])) useCache = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.CUT_GENERATOR_INDEX.ordinal()])) cutGeneratorName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.HIGH_LEVEL_COST_FUNCTION_INDEX.ordinal()])) highLevelCostFunctionName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.LOW_LEVEL_COST_FUNCTION_INDEX.ordinal()])) lowLevelCostFunctionName = value;
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.A_INDEX.ordinal()])) a = Integer.parseInt(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.PSI_INDEX.ordinal()])) psi = Double.parseDouble(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.A_FACTOR_INDEX.ordinal()])) aFactor = Double.parseDouble(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_A_INDEX.ordinal()])) autoComputeA = Boolean.parseBoolean(value);
                else if (description.equals(CONFIG_DESCRIPTIONS[ConfigIndices.AUTO_COMPUTE_PSI_INDEX.ordinal()])) autoComputePsi = Boolean.parseBoolean(value);
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

        Config newConfig = new Config(useAlternateConsistencyCheck,
                useWernerModification,
                useCache,
                cutGeneratorName,
                highLevelCostFunctionName,
                lowLevelCostFunctionName,
                a,
                aFactor,
                psi);
        newConfig.setAutoCompute(autoComputeA, autoComputePsi);
        newConfig.setRemoveRedundant(removeRedundant);
        newConfig.setDimensionReductionParameters(splitSize, tsneComponents);
        return newConfig;
    }

    @Override
    public String toString() {
        return "Use Alternate Consistency Check: " + useAlternateConsistencyCheck + "\n" +
                "Use Werner Modification: " + useWernerModification + "\n" +
                "Use Cache: " + useCache + "\n" +
                "Cut Generator: " + cutGeneratorName + "\n" +
                "High Level Cost Function: " + highLevelCostFunctionName + "\n" +
                "Low Level Cost Function: " + lowLevelCostFunctionName + "\n" +
                "Parameter a: " + a + "\n" +
                "Parameter psi: " + psi + "\n" +
                "Factor a: " + aFactor + "\n" +
                "Auto Compute a: " + autoComputeA + "\n" +
                "Auto Compute psi: " + autoComputePsi + "\n" +
                "Remove Redundant Cuts: " + removeRedundant + "\n" +
                "Split Size: " + splitSize + "\n" +
                "t-SNE Components: " + tsneComponents;
    }
}

package util;

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

    private boolean autoComputeA = false;
    private boolean autoComputePsi = false;

    private boolean removeRedundant = false;

    private int splitSize = 1000;
    private int tsneComponents = 5;

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
}

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
    private boolean autoComputeA;
    private boolean autoComputePsi;

    public Config(boolean useAlternateConsistencyCheck,
                  boolean useWernerModification,
                  boolean useCache,
                  String cutGeneratorName,
                  String highLevelCostFunctionName,
                  String lowLevelCostFunctionName,
                  int a,
                  double aFactor,
                  double psi,
                  boolean autoComputeA,
                  boolean autoComputePsi) {
        this.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
        this.useWernerModification = useWernerModification;
        this.useCache = useCache;
        this.cutGeneratorName = cutGeneratorName;
        this.highLevelCostFunctionName = highLevelCostFunctionName;
        this.lowLevelCostFunctionName = lowLevelCostFunctionName;
        this.a = a;
        this.aFactor = aFactor;
        this.psi = psi;
        this.autoComputeA = autoComputeA;
        this.autoComputePsi = autoComputePsi;
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
}

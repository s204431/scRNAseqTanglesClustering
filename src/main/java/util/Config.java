package util;

public final class Config {
    private final boolean useAlternateConsistencyCheck;
    private final boolean useWernerModification;
    private final String cutGeneratorName;
    private final String costFunctionName;
    private final int a;
    private final double aFactor;
    private final double psi;

    public Config(boolean useAlternateConsistencyCheck, boolean useWernerModification, String cutGeneratorName, String costFunctionName, int a, double aFactor, double psi) {
        this.useAlternateConsistencyCheck = useAlternateConsistencyCheck;
        this.useWernerModification = useWernerModification;
        this.cutGeneratorName = cutGeneratorName;
        this.costFunctionName = costFunctionName;
        this.a = a;
        this.aFactor = aFactor;
        this.psi = psi;
    }

    public boolean isUseAlternateConsistencyCheck() {
        return useAlternateConsistencyCheck;
    }

    public boolean isUseWernerModification() {
        return useWernerModification;
    }

    public String getCutGeneratorName() {
        return cutGeneratorName;
    }

    public String getCostFunctionName() {
        return costFunctionName;
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

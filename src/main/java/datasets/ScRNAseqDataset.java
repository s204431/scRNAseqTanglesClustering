package datasets;

import clustering.Model;
import smile.base.mlp.Cost;
import util.BitSet;
import util.GlobalConstants;

import java.util.Arrays;

public class ScRNAseqDataset {
    public double[][] data;
    private int a;
    private double sparsity;

    private BitSet[] initialCuts;
    private double[] cutCosts;

    private CutGenerators cutGenerators;
    private CostFunctions costFunctions;

    //This class should be a dataset object containing the data and other representations.

    public ScRNAseqDataset(double[][] data) {
        this.data = data;
        this.cutGenerators = new CutGenerators();
        this.costFunctions = new CostFunctions();
    }

    public void setCostFunctions(CostFunctions costFunctions) {
        this.costFunctions = costFunctions;
    }

    public BitSet[] getInitialCuts(String highLevelCutGenerator, String lowLevelCutGenerator, boolean useFastVersion) {
        if (a == 0) {
            System.out.println("Variable for a is not chosen yet or is 0.");
            return null;
        }

        if (highLevelCutGenerator.equals(GlobalConstants.HIGH_LEVEL_CUT_GENERATOR_NORMAL)) {
            initialCuts = cutGenerators.singleCutGenerator(data, lowLevelCutGenerator, a, useFastVersion);
        } else {
            initialCuts = cutGenerators.splitCutGenerator(data, lowLevelCutGenerator, a, useFastVersion);
        }

        return initialCuts;
    }

    public double[] getCutCosts(String highLevelCostFunction, String lowLevelCostFunction, boolean useCache, int splitSize, int tsneComponents, boolean useFastVersion) {
        switch (highLevelCostFunction) {
            case GlobalConstants.HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT:
                cutCosts = costFunctions.bestFirstCostFunction(data, initialCuts, lowLevelCostFunction, useCache, splitSize, tsneComponents, useFastVersion);
                break;

            case GlobalConstants.HIGH_LEVEL_COST_FUNCTION_AVERAGE:
                cutCosts = costFunctions.averageCostFunction(data, initialCuts, lowLevelCostFunction, useCache, splitSize, tsneComponents, useFastVersion);
                break;

            default:
                cutCosts = costFunctions.singleCostFunction(data, initialCuts, lowLevelCostFunction, useCache, tsneComponents, useFastVersion);
                break;
        }

        return cutCosts;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setSparsity(double sparsity) {
        this.sparsity = sparsity;
    }

    public void setInitialCuts(BitSet[] initialCuts) {
        this.initialCuts = initialCuts;
    }

    public BitSet[] getLastCuts() {
        return initialCuts;
    }

    public double[] getLastCosts() {
        return cutCosts;
    }

    public double getSparsity() {
        return sparsity;
    }
}

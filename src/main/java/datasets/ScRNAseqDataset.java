package datasets;

import smile.base.mlp.Cost;
import util.BitSet;
import util.GlobalConstants;

import java.util.Arrays;

public class ScRNAseqDataset {
    public double[][] data;
    private int a;

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

    public BitSet[] getInitialCuts(String initialCutGenerator) {
        if (a == 0) {
            System.out.println("Variable for a is not chosen yet or is 0.");
            return null;
        }

        switch (initialCutGenerator) {
            case GlobalConstants.CUT_GENERATOR_RANGE:
                initialCuts = cutGenerators.getInitialCutsRange(data, a);
                break;

            case GlobalConstants.CUT_GENERATOR_LOCAL_MEANS:
                initialCuts = cutGenerators.getInitialCutsLocalMeans(data, a);
                break;

            case GlobalConstants.CUT_GENERATOR_SIMPLE:
                initialCuts = cutGenerators.getInitialCutsSimple(data, a);
                break;

            default:
                initialCuts = cutGenerators.splitCutGenerator(data, a);
                break;
        }

        return initialCuts;
    }

    public double[] getCutCosts(String highLevelCostFunction, String lowLevelCostFunction, boolean useCache) {
        switch (highLevelCostFunction) {
            default:
                cutCosts = costFunctions.averageCostFunction(data, initialCuts, lowLevelCostFunction, useCache);
                break;
        }

        return cutCosts;
    }

    public void setA(int a) {
        this.a = a;
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

}

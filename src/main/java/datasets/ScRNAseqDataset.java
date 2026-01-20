package datasets;

import util.BitSet;
import util.GlobalConstants;
import util.Monitor;

public class ScRNAseqDataset {
    public double[][] data;
    private int a;
    private double sparsity;

    private BitSet[] initialCuts;
    private double[] cutCosts;

    private CutGenerators cutGenerators;
    private CostFunctions costFunctions;

    //This class should be a dataset object containing the data and other representations.

    public ScRNAseqDataset(double[][] data, Monitor monitor) {
        this.data = data;
        this.cutGenerators = new CutGenerators(monitor);
        this.costFunctions = new CostFunctions(monitor);
    }

    public void setCostFunctions(CostFunctions costFunctions) {
        this.costFunctions = costFunctions;
    }

    public BitSet[] getInitialCuts(String highLevelCutGenerator, String lowLevelCutGenerator, int splitSize, boolean usePca, int pcaComponents, boolean useTsne, int tsneComponents) {
        if (a == 0) {
            System.out.println("Variable for a is not chosen yet or is 0.");
            return null;
        }

        if (highLevelCutGenerator.equals(GlobalConstants.HIGH_LEVEL_CUT_GENERATOR_NORMAL)) {
            initialCuts = cutGenerators.singleCutGenerator(data, lowLevelCutGenerator, a, usePca, pcaComponents, useTsne, tsneComponents);
        } else {
            initialCuts = cutGenerators.splitCutGenerator(data, lowLevelCutGenerator, a, splitSize, usePca, pcaComponents, useTsne, tsneComponents);
        }

        return initialCuts;
    }

    public double[] getCutCosts(String highLevelCostFunction,
                                String lowLevelCostFunction,
                                boolean useCache,
                                int splitSize,
                                boolean usePca,
                                int pcaComponents,
                                boolean useTsne,
                                int tsneComponents) {

        switch (highLevelCostFunction) {
            case GlobalConstants.HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT:
                cutCosts = costFunctions.bestFirstCostFunction(
                        data,
                        initialCuts,
                        lowLevelCostFunction,
                        useCache,
                        splitSize,
                        usePca,
                        pcaComponents,
                        useTsne,
                        tsneComponents);
                break;

            case GlobalConstants.HIGH_LEVEL_COST_FUNCTION_AVERAGE:
                cutCosts = costFunctions.averageCostFunction(
                        data,
                        initialCuts,
                        lowLevelCostFunction,
                        useCache,
                        splitSize,
                        usePca,
                        pcaComponents,
                        useTsne,
                        tsneComponents);
                break;

            default:
                cutCosts = costFunctions.singleCostFunction(
                        data,
                        initialCuts,
                        lowLevelCostFunction,
                        useCache,
                        usePca,
                        pcaComponents,
                        useTsne,
                        tsneComponents);
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

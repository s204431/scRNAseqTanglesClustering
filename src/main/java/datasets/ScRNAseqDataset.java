package datasets;

import util.BitSet;

import java.util.Arrays;

public class ScRNAseqDataset {
    private double[][] data;
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

    public BitSet[] getInitialCuts(String initialCutGenerator) {
        if (a == 0) {
            System.out.println("Variable for a is not chosen yet or is 0.");
            return null;
        }

        switch (initialCutGenerator) {
            case "Range":
                initialCuts = cutGenerators.getInitialCutsRange(data, a);
                break;

            case "Local Means":
                initialCuts = cutGenerators.getInitialCutsLocalMeans(data, a);
                break;

            default:
                initialCuts = cutGenerators.getInitialCutsSimple(data, a);
                break;
        }

        return initialCuts;
    }

    public double[] getCutCosts(String costFunctionName) {
        switch (costFunctionName) {
            case "Distance To Mean":
                cutCosts = costFunctions.distanceToMeanCostFunction(data, initialCuts);
                break;

            default:
                cutCosts = costFunctions.pairwiseDistanceCostFunction(data, initialCuts);
                break;
        }

        return cutCosts;
    }

    public void setA(int a) {
        this.a = a;
    }

    public BitSet[] getLastCuts() {
        return initialCuts;
    }

    public double[] getLastCosts() {
        return cutCosts;
    }

}

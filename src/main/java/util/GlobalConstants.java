package util;

import datasets.CutGenerators;

public class GlobalConstants {
    public enum CUT_GENERATOR {
        SPLIT,
        SIMPLE,
        RANGE,
        LOCAL_MEANS
    }
    public static final String CUT_GENERATOR_SPLIT = "Split";
    public static final String CUT_GENERATOR_SIMPLE = "Simple";
    public static final String CUT_GENERATOR_RANGE = "Range";
    public static final String CUT_GENERATOR_LOCAL_MEANS = "Local Means";
    public static final String[] CUT_GENERATOR_NAMES = new String[CUT_GENERATOR.values().length];
    static {
        CUT_GENERATOR_NAMES[CUT_GENERATOR.SPLIT.ordinal()] = CUT_GENERATOR_SPLIT;
        CUT_GENERATOR_NAMES[CUT_GENERATOR.SIMPLE.ordinal()] = CUT_GENERATOR_SIMPLE;
        CUT_GENERATOR_NAMES[CUT_GENERATOR.RANGE.ordinal()] = CUT_GENERATOR_RANGE;
        CUT_GENERATOR_NAMES[CUT_GENERATOR.LOCAL_MEANS.ordinal()] = CUT_GENERATOR_LOCAL_MEANS;
    }


    public enum COST_FUNCTION {
        AVERAGE,
        DISTANCE_TO_MEAN,
        PAIRWISE,
        SHORTEST
    }
    public static final String COST_FUNCTION_AVERAGE = "Average";
    public static final String COST_FUNCTION_DISTANCE_TO_MEAN = "Distance To Mean";
    public static final String COST_FUNCTION_PAIRWISE = "Pairwise";
    public static final String COST_FUNCTION_SHORTEST = "Shortest";
    public static final String[] COST_FUNCTION_NAMES = new String[COST_FUNCTION.values().length];
    static {
        COST_FUNCTION_NAMES[COST_FUNCTION.AVERAGE.ordinal()] = COST_FUNCTION_AVERAGE;
        COST_FUNCTION_NAMES[COST_FUNCTION.DISTANCE_TO_MEAN.ordinal()] = COST_FUNCTION_DISTANCE_TO_MEAN;
        COST_FUNCTION_NAMES[COST_FUNCTION.PAIRWISE.ordinal()] = COST_FUNCTION_PAIRWISE;
        COST_FUNCTION_NAMES[COST_FUNCTION.SHORTEST.ordinal()] = COST_FUNCTION_SHORTEST;
    }
}

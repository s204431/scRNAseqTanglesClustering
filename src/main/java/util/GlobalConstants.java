package util;

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


    public enum HIGH_LEVEL_COST_FUNCTION {
        AVERAGE,
        NORMAL,
        BEST_SPLIT
    }
    public static final String HIGH_LEVEL_COST_FUNCTION_AVERAGE = "Average";
    public static final String HIGH_LEVEL_COST_FUNCTION_NORMAL = "Normal";
    public static final String HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT = "Best Split";
    public static final String[] HIGH_LEVEL_COST_FUNCTION_NAMES = new String[HIGH_LEVEL_COST_FUNCTION.values().length];
    static{
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.AVERAGE.ordinal()] = HIGH_LEVEL_COST_FUNCTION_AVERAGE;
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.NORMAL.ordinal()] = HIGH_LEVEL_COST_FUNCTION_NORMAL;
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.BEST_SPLIT.ordinal()] = HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT;
    }

    public enum LOW_LEVEL_COST_FUNCTION {
        DISTANCE_TO_MEAN,
        PAIRWISE,
        SHORTEST,
        PAIRWISE_CLOSEST,
        KNN
    }
    public static final String LOW_LEVEL_COST_FUNCTION_DISTANCE_TO_MEAN = "Distance To Mean";
    public static final String LOW_LEVEL_COST_FUNCTION_PAIRWISE = "Pairwise";
    public static final String LOW_LEVEL_COST_FUNCTION_SHORTEST = "Shortest";
    public static final String LOW_LEVEL_COST_FUNCTION_PAIRWISE_CLOSEST = "Pairwise Closest";
    public static final String LOW_LEVEL_COST_FUNCTION_KNN = "KNN";
    public static final String[] LOW_LEVEL_COST_FUNCTION_NAMES = new String[LOW_LEVEL_COST_FUNCTION.values().length];
    static {
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.DISTANCE_TO_MEAN.ordinal()] = LOW_LEVEL_COST_FUNCTION_DISTANCE_TO_MEAN;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.PAIRWISE.ordinal()] = LOW_LEVEL_COST_FUNCTION_PAIRWISE;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.SHORTEST.ordinal()] = LOW_LEVEL_COST_FUNCTION_SHORTEST;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.PAIRWISE_CLOSEST.ordinal()] = LOW_LEVEL_COST_FUNCTION_PAIRWISE_CLOSEST;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.KNN.ordinal()] = LOW_LEVEL_COST_FUNCTION_KNN;
    }
}

package util;

import java.awt.*;

public class GlobalConstants {
    public static final int MAX_PCS_COMPONENTS = 100;

    public enum HIGH_LEVEL_CUT_GENERATOR {
        SPLIT,
        NORMAL
    }
    public static final String HIGH_LEVEL_CUT_GENERATOR_SPLIT = "Batching";
    public static final String HIGH_LEVEL_CUT_GENERATOR_NORMAL = "No batching";
    public static final String[] HIGH_LEVEL_CUT_GENERATOR_NAMES = new String[HIGH_LEVEL_CUT_GENERATOR.values().length];
    static {
        HIGH_LEVEL_CUT_GENERATOR_NAMES[HIGH_LEVEL_CUT_GENERATOR.SPLIT.ordinal()] = HIGH_LEVEL_CUT_GENERATOR_SPLIT;
        HIGH_LEVEL_CUT_GENERATOR_NAMES[HIGH_LEVEL_CUT_GENERATOR.NORMAL.ordinal()] = HIGH_LEVEL_CUT_GENERATOR_NORMAL;
    }

    public enum LOW_LEVEL_CUT_GENERATOR {
        KNN,
        SIMPLE,
        RANGE,
        LOCAL_MEANS
        //DISTANCE_BETWEEN_MEANS
    }
    public static final String LOW_LEVEL_CUT_GENERATOR_KNN = "KNN";
    public static final String LOW_LEVEL_CUT_GENERATOR_SIMPLE = "Simple";
    public static final String LOW_LEVEL_CUT_GENERATOR_RANGE = "Range";
    public static final String LOW_LEVEL_CUT_GENERATOR_LOCAL_MEANS = "Local means";
    public static final String LOW_LEVEL_CUT_GENERATOR_DISTANCE_BETWEEN_MEANS = "Dist btw Means";
    public static final String[] LOW_LEVEL_CUT_GENERATOR_NAMES = new String[LOW_LEVEL_CUT_GENERATOR.values().length];
    static {
        LOW_LEVEL_CUT_GENERATOR_NAMES[LOW_LEVEL_CUT_GENERATOR.KNN.ordinal()] = LOW_LEVEL_CUT_GENERATOR_KNN;
        LOW_LEVEL_CUT_GENERATOR_NAMES[LOW_LEVEL_CUT_GENERATOR.SIMPLE.ordinal()] = LOW_LEVEL_CUT_GENERATOR_SIMPLE;
        LOW_LEVEL_CUT_GENERATOR_NAMES[LOW_LEVEL_CUT_GENERATOR.RANGE.ordinal()] = LOW_LEVEL_CUT_GENERATOR_RANGE;
        LOW_LEVEL_CUT_GENERATOR_NAMES[LOW_LEVEL_CUT_GENERATOR.LOCAL_MEANS.ordinal()] = LOW_LEVEL_CUT_GENERATOR_LOCAL_MEANS;
        //LOW_LEVEL_CUT_GENERATOR_NAMES[LOW_LEVEL_CUT_GENERATOR.DISTANCE_BETWEEN_MEANS.ordinal()] = LOW_LEVEL_CUT_GENERATOR_DISTANCE_BETWEEN_MEANS;
    }

    public enum HIGH_LEVEL_COST_FUNCTION {
        AVERAGE,
        BEST_SPLIT,
        NORMAL
    }
    public static final String HIGH_LEVEL_COST_FUNCTION_AVERAGE = "Average batch";
    public static final String HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT = "Best Batch";
    public static final String HIGH_LEVEL_COST_FUNCTION_NORMAL = "No batching";
    public static final String[] HIGH_LEVEL_COST_FUNCTION_NAMES = new String[HIGH_LEVEL_COST_FUNCTION.values().length];
    static{
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.AVERAGE.ordinal()] = HIGH_LEVEL_COST_FUNCTION_AVERAGE;
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.BEST_SPLIT.ordinal()] = HIGH_LEVEL_COST_FUNCTION_BEST_SPLIT;
        HIGH_LEVEL_COST_FUNCTION_NAMES[HIGH_LEVEL_COST_FUNCTION.NORMAL.ordinal()] = HIGH_LEVEL_COST_FUNCTION_NORMAL;
    }

    public enum LOW_LEVEL_COST_FUNCTION {
        KNN,
        DISTANCE_TO_MEAN,
        PAIRWISE,
        SHORTEST,
        PAIRWISE_CLOSEST
    }
    public static final String LOW_LEVEL_COST_FUNCTION_DISTANCE_TO_MEAN = "Dist To Mean";
    public static final String LOW_LEVEL_COST_FUNCTION_PAIRWISE = "Pairwise";
    public static final String LOW_LEVEL_COST_FUNCTION_SHORTEST = "Shortest";
    public static final String LOW_LEVEL_COST_FUNCTION_PAIRWISE_CLOSEST = "Pairwise Closest";
    public static final String LOW_LEVEL_COST_FUNCTION_KNN = "KNN";
    public static final String[] LOW_LEVEL_COST_FUNCTION_NAMES = new String[LOW_LEVEL_COST_FUNCTION.values().length];
    static {
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.KNN.ordinal()] = LOW_LEVEL_COST_FUNCTION_KNN;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.DISTANCE_TO_MEAN.ordinal()] = LOW_LEVEL_COST_FUNCTION_DISTANCE_TO_MEAN;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.PAIRWISE.ordinal()] = LOW_LEVEL_COST_FUNCTION_PAIRWISE;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.SHORTEST.ordinal()] = LOW_LEVEL_COST_FUNCTION_SHORTEST;
        LOW_LEVEL_COST_FUNCTION_NAMES[LOW_LEVEL_COST_FUNCTION.PAIRWISE_CLOSEST.ordinal()] = LOW_LEVEL_COST_FUNCTION_PAIRWISE_CLOSEST;
    }

    public enum SPLIT_PRUNING_METHODS {
        MEAN_IN_WINDOW,
        PERFORMANCE_METRIC
    }
    public static final String SPLIT_PRUNE_MEAN_IN_WINDOW = "Window Mean";
    public static final String SPLIT_PRUNE_PERFORMANCE_METRIC = "Performance Metric";
    public static final String[] SPLIT_PRUNE_METHOD_NAMES = new String[SPLIT_PRUNING_METHODS.values().length];
    static {
        SPLIT_PRUNE_METHOD_NAMES[SPLIT_PRUNING_METHODS.MEAN_IN_WINDOW.ordinal()] = SPLIT_PRUNE_MEAN_IN_WINDOW;
        SPLIT_PRUNE_METHOD_NAMES[SPLIT_PRUNING_METHODS.PERFORMANCE_METRIC.ordinal()] = SPLIT_PRUNE_PERFORMANCE_METRIC;
    }

    public enum PERFORMANCE_METRICS {
        SILHOUETTE_SCORE,
        DAVIES_BOLDIN_INDEX
    }
    public static final String PERFORMANCE_METRIC_SIL = "Silhouette";
    public static final String PERFORMANCE_METRIC_DAV = "Davies-Boldin";
    public static final String[] PERFORMANCE_METRIC_NAMES = new String[PERFORMANCE_METRICS.values().length];
    static {
        PERFORMANCE_METRIC_NAMES[PERFORMANCE_METRICS.SILHOUETTE_SCORE.ordinal()] = PERFORMANCE_METRIC_SIL;
        PERFORMANCE_METRIC_NAMES[PERFORMANCE_METRICS.DAVIES_BOLDIN_INDEX.ordinal()] = PERFORMANCE_METRIC_DAV;
    }

    public static final Color COLOR_DARK_GRAY = new Color(150, 150, 150);
    public static final Color COLOR_GRAY = new Color(200, 200, 200);
    public static final Color COLOR_LIGHT_GRAY = new Color(220, 220, 220);
    public static final Color COLOR_VERY_LIGHT_GRAY = new Color(230, 230, 230);
    public static final Color COLOR_ALMOST_WHITE = new Color(245, 245, 245);

    public static Color[] CLUSTER_COLORS = new Color[] {
            new Color(228, 26, 28),    // Red
            new Color(55, 126, 184),   // Blue
            new Color(77, 175, 74),    // Green
            new Color(152, 78, 163),   // Purple
            new Color(255, 127, 0),    // Orange
            new Color(255, 255, 51),   // Yellow
            new Color(166, 206, 227),  // Light Blue
            new Color(178, 223, 138),  // Light Green
            new Color(202, 178, 214),  // Lavender
            new Color(227, 119, 194),  // Magenta
            new Color(153, 153, 153),  // Gray
            new Color(188, 189, 34),   // Olive
            new Color(23, 190, 207),   // Cyan-Teal
            new Color(0, 128, 128),    // Teal
            new Color(0, 0, 128),       // Navy
            new Color(121, 85, 72),    // Dark Brown
            new Color(197, 176, 43),   // Mustard
            new Color(0, 170, 170),    // Blue-Cyan
            new Color(63, 63, 191),    // Indigo
            new Color(140, 205, 0)     // Yellow-Green
    };
}

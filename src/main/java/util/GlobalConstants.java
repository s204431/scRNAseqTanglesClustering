package util;

import java.awt.*;

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
            new Color(166, 86, 40),    // Brown
            new Color(247, 129, 191),  // Pink
            new Color(153, 153, 153),  // Gray
            new Color(31, 120, 180),   // Deep Blue
            new Color(51, 160, 44),    // Deep Green
            new Color(251, 154, 153),  // Light Red
            new Color(227, 26, 28),    // Crimson
            new Color(253, 191, 111),  // Light Orange
            new Color(255, 255, 51),   // Yellow
            new Color(166, 206, 227),  // Light Blue
            new Color(178, 223, 138),  // Light Green
            new Color(202, 178, 214),  // Lavender
            new Color(255, 255, 153),  // Light Yellow
            new Color(31, 119, 180),   // Steel Blue
            new Color(255, 140, 0),    // Dark Orange
            new Color(44, 160, 44),    // Forest Green
            new Color(214, 39, 40),    // Strong Red
            new Color(148, 103, 189),  // Violet
            new Color(140, 86, 75),    // Clay Brown
            new Color(227, 119, 194),  // Magenta
            new Color(127, 127, 127),  // Mid Gray
            new Color(188, 189, 34),   // Olive
            new Color(23, 190, 207),   // Cyan-Teal
            new Color(0, 128, 128),    // Teal
            new Color(0, 0, 128)       // Navy
    };
}

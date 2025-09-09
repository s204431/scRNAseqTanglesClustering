package util;

import com.sun.jna.platform.win32.WinDef;
import org.opencv.android.LoaderCallbackInterface;

public class GlobalConstants {
    public static final String CUT_GENERATOR_SPLIT = "Split";
    public static final String CUT_GENERATOR_SIMPLE = "Simple";
    public static final String CUT_GENERATOR_RANGE = "Range";
    public static final String CUT_GENERATOR_LOCAL_MEANS = "Local Means";
    public static final String[] CUT_GENERATOR_NAMES = {CUT_GENERATOR_SPLIT, CUT_GENERATOR_SIMPLE, CUT_GENERATOR_RANGE, CUT_GENERATOR_LOCAL_MEANS};


    public static final String COST_FUNCTION_DISTANCE_TO_MEAN = "Distance To Mean";
    public static final String COST_FUNCTION_PAIRWISE = "Pairwise";
    public static final String COST_FUNCTION_SHORTEST = "Shortest";
    public static final String COST_FUNCTION_AVERAGE = "Average";
    public static final String[] COST_FUNCTION_NAMES = {COST_FUNCTION_AVERAGE, COST_FUNCTION_DISTANCE_TO_MEAN, COST_FUNCTION_PAIRWISE, COST_FUNCTION_SHORTEST};
}

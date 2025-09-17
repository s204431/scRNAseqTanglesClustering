package util;

public class Distance {

    public static double[][] getDistanceMatrix(double[][] points, DistanceMeasure distanceMeasure) {
        double[][] distance = new double[points.length][points.length];
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                distance[i][j] = distanceMeasure.distance(points[i], points[j]);
            }
        }
        return distance;
    }

    public static DistanceMeasure euclidean() {
        return (p1, p2) -> {
            double length = 0;
            for (int i = 0; i < p1.length; i++) {
                length += (p1[i] - p2[i]) * (p1[i] - p2[i]);
            }
            return Math.sqrt(length);
        };
    }

    public static DistanceMeasure jaccard() {
        return (p1, p2) -> {
            double minSum = 0.0;
            double maxSum = 0.0;
            for (int i = 0; i < p1.length; i++) {
                minSum += Math.min(p1[i], p2[i]);
                maxSum += Math.max(p1[i], p2[i]);
            }
            return 1.0 - (minSum / maxSum);
        };
    }

    public static DistanceMeasure cosine() {
        return (p1, p2) -> {
            double dotProduct = 0.0;
            double length1 = 0.0;
            double length2 = 0.0;
            for (int i = 0; i < p1.length; i++) {
                dotProduct += p1[i]*p2[i];
                length1 += p1[i]*p1[i];
                length2 += p2[i]*p2[i];
            }

            return 1.0-(dotProduct/(Math.sqrt(length1)*Math.sqrt(length2)));
        };
    }

    public static DistanceMeasure manhattan() {
        return (p1, p2) -> {
            double sum = 0.0;
            for (int i = 0; i < p1.length; i++) {
                sum += Math.abs(p1[i] - p2[i]);
            }
            return sum;
        };
    }

    public static DistanceMeasure chebyshev() {
        return (p1, p2) -> {
            double max = -1.0;
            for (int i = 0; i < p1.length; i++) {
                max = Math.max(max, Math.abs(p1[i] - p2[i]));
            }
            return max;
        };
    }

    public static DistanceMeasure pearson() {
        return (p1, p2) -> {
            double mean1 = 0.0;
            double mean2 = 0.0;
            for (int i = 0; i < p1.length; i++) {
                mean1 += p1[i];
                mean2 += p2[i];
            }
            mean1 /= p1.length;
            mean2 /= p2.length;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            for (int i = 0; i < p1.length; i++) {
                double p1m = p1[i] - mean1;
                double p2m = p2[i] - mean2;
                sum1 += p1m*p2m;
                sum2 += p1m*p1m;
                sum3 += p2m*p2m;
            }
            return 1.0 - Math.abs(sum1/(Math.sqrt(sum2)*Math.sqrt(sum3)));
        };
    }

    public static DistanceMeasure hamming() {
        return (p1, p2) -> {
            int nDifferent = 0;
            for (int i = 0; i < p1.length; i++) {
                if (p1[i] != p2[i]) {
                    nDifferent++;
                }
            }
            return (double)nDifferent;
        };
    }
}

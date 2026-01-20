package main;

import clustering.Model;
import util.Monitor;
import visualization.View;

public class Main {
    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        Model model = new Model(monitor);
        View view = new View(model, monitor);
    }

    public static double[][] zScoreNorm(double[][] doubleData) {
        double[][] newData = new double[doubleData.length][doubleData[0].length];
        //Normalization
        double[] mean = new double[doubleData[0].length];
        double[] std = new double[doubleData[0].length];
        for (int i = 0; i < doubleData.length; i++) {
            for (int j = 0; j < doubleData[i].length; j++) {
                mean[j] += doubleData[i][j];
            }
        }

        for (int j = 0; j < mean.length; j++) {
            mean[j] /= doubleData.length;
        }

        for (int i = 0; i < doubleData.length; i++) {
            for (int j = 0; j < doubleData[i].length; j++) {
                std[j] += (doubleData[i][j] - mean[j])*(doubleData[i][j] - mean[j]);
            }
        }

        for (int j = 0; j < std.length; j++) {
            std[j] = Math.sqrt(std[j]/(doubleData.length-1));
        }

        for (int i = 0; i < doubleData.length; i++) {
            for (int j = 0; j < doubleData[i].length; j++) {
                newData[i][j] = (doubleData[i][j] - mean[j])/std[j];
            }
        }
        return newData;
    }

}
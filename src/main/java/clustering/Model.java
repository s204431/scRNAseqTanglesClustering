package clustering;

import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.utils.TSneUtils;
import datasets.CostFunctions;
import datasets.CutGenerators;
import datasets.ScRNAseqDataset;
import elki.data.DoubleVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.StaticArrayDatabase;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.datasource.DatabaseConnection;
import elki.projection.AffinityMatrixBuilder;
import elki.projection.BarnesHutTSNE;
import elki.projection.PerplexityAffinityMatrixBuilder;
import elki.utilities.random.RandomFactory;
import elki.distance.minkowski.EuclideanDistance;
import monitor.Monitor;
import smile.feature.extraction.PCA;
import smile.manifold.UMAP;
import smile.math.matrix.Matrix;
import smile.validation.metric.NormalizedMutualInformation;
import smile.validation.metric.AdjustedRandIndex;
import util.BitSet;
import util.Tuple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static main.Main.runPython;

public class Model {
    private int[][] originalData;
    private double[][] doubleData;
    private double[][] hvgData;
    private double[][] projectedData;
    private ScRNAseqDataset dataset;
    private int[] groundTruth;
    private int[] hardClustering;

    private Monitor monitor;

    private TangleClusterer tangleClusterer = new TangleClusterer();

    public Model() {
        originalData = loadData("data/symsim_observed_counts_5000genes_1000cells_complex.csv");
        groundTruth = loadGroundTruth("data/symsim_labels_5000genes_1000cells_complex.csv");
        doubleData = convertToDouble(originalData);
        hvgData = highlyVariableGenes(doubleData, 100);

        long time = System.currentTimeMillis();
        //projectedData = tsne(hvgData, 2);
        projectedData = hvgData;
        System.out.println(System.currentTimeMillis() - time);

        dataset = new ScRNAseqDataset(projectedData);
        //cluster(dataset, 70, 0, "Range", "Distance To Mean");

        /*Tuple<int[], Integer> pythonResult = runPython("data/symsim_observed_counts_5000genes_1000cells_complex.csv");
        double NMIPython = NormalizedMutualInformation.joint(pythonResult.x, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, pythonResult.x);
        System.out.println("NMI python: " + NMIPython);
        System.out.println("Rand index python: " + randIndex);*/
    }

    public void cluster(ScRNAseqDataset dataset, int a, double psi, String initialCutGenerator, String costFunctionName) {
        monitor.setDataset(dataset);
        tangleClusterer.generateClusters(dataset, a, psi, initialCutGenerator, costFunctionName);
        hardClustering = tangleClusterer.getHardClustering();
        double NMIScore = NormalizedMutualInformation.joint(hardClustering, groundTruth);
        double randIndex = AdjustedRandIndex.of(groundTruth, hardClustering);
        System.out.println(NMIScore);
        System.out.println(randIndex);
    }

    public static double[][] pca(double[][] data, int nComponents) {
        PCA pca = PCA.cor(data);
        return pca.getProjection(nComponents).apply(data);
    }

    public static double[][] svd(double[][] data, int nComponents) {
        Matrix X = Matrix.of(data);
        Matrix.SVD svd = X.svd();

        Matrix V = svd.V;
        double[][] V_k_array = new double[X.ncol()][nComponents];
        for (int i = 0; i < X.ncol(); i++) {
            for (int j = 0; j < nComponents; j++) {
                V_k_array[i][j] = V.get(i, j);
            }
        }
        Matrix V_k = Matrix.of(V_k_array);

        Matrix projectedData = X.mm(V_k);
        return projectedData.toArray();
    }

    public static double[][] tsne(double[][] data, int nComponents) {
        long time = System.currentTimeMillis();

        int initialDims = data[0].length;
        double perplexity = 20.0;
        int maxIterations = 100;
        /*BarnesHutTSne tsne = new BHTSne();
        TSneConfiguration config = TSneUtils.buildConfig(data, nComponents, initialDims, perplexity, maxIterations);

        double[][] output = tsne.tsne(config);
        System.out.println("TSNE time: " + (System.currentTimeMillis() - time));
        return output;*/


        // Wrap raw data into ELKI database
        Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data), null);
        db.initialize();
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

        // Affinity matrix builder (perplexity 30, Euclidean distance)
        AffinityMatrixBuilder<DoubleVector> affinity =
                new PerplexityAffinityMatrixBuilder<>(EuclideanDistance.STATIC, perplexity);

        // Construct Barnes-Hut t-SNE
        BarnesHutTSNE<DoubleVector> tsne = new BarnesHutTSNE<>(
                affinity,
                nComponents,
                0.8,              // finalMomentum
                200.0,            // learningRate
                maxIterations,             // maxIterations
                RandomFactory.DEFAULT,
                false,            // keep original data
                0.5               // theta (Barnes-Hut approximation)
        );

        // Run algorithm
        Relation<DoubleVector> projected = tsne.run(db, rel);

        // Collect results using DBIDIter
        List<double[]> resultList = new ArrayList<>();
        for (DBIDIter iter = projected.getDBIDs().iter(); iter.valid(); iter.advance()) {
            DoubleVector vec = projected.get(iter);
            double[] coords = new double[nComponents];
            for (int j = 0; j < nComponents; j++) {
                coords[j] = vec.doubleValue(j);
            }
            resultList.add(coords);
        }

        // Convert list to array
        double[][] output = new double[resultList.size()][nComponents];
        for (int i = 0; i < output.length; i++) {
            output[i] = resultList.get(i);
        }

        System.out.println("TSNE time: " + (System.currentTimeMillis() - time));

        return output;
    }

    public static double[][] umap(double[][] data, int nComponents) {
        return UMAP.fit(data, new UMAP.Options(2, nComponents, 200, 1, 0.1, 1.0, 5, 1.0, 2));
    }

    public int[][] loadData(String filePath) {
        return readCSV(filePath);
    }

    public int[] loadGroundTruth(String filePath) {
        int[][] temp = loadData(filePath);
        int[] gt = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            gt[i] = temp[i][0];
        }
        return gt;
    }

    public double[][] highlyVariableGenes(double[][] data, int nTopGenes) {
        int nGenes = data[0].length;
        int nCells = data.length;

        double[] dispersions = new double[nGenes];

        for (int g = 0; g < nGenes; g++) {
            double sum = 0.0;
            for (int c = 0; c < nCells; c++) {
                sum += data[c][g];
            }
            double mean = sum / nCells;

            double sqDiff = 0.0;
            for (int c = 0; c < nCells; c++) {
                sqDiff += Math.pow(data[c][g] - mean, 2);
            }
            double variance = sqDiff / (nCells - 1);

            dispersions[g] = mean > 0 ? variance / mean : 0.0;
        }

        // Get indices sorted by dispersion (descending)
        Integer[] indices = new Integer[nGenes];
        for (int i = 0; i < nGenes; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(a -> dispersions[a]));

        // Take top nTopGenes
        int[] indc =  Arrays.stream(indices)
                .limit(nTopGenes)
                .mapToInt(Integer::intValue)
                .toArray();

        double[][] newData = new double[nCells][nTopGenes];
        for (int i = 0; i < nTopGenes; i++) {
            for (int j = 0; j < nCells; j++) {
                newData[j][i] = data[j][indc[i]];
            }
        }
        System.out.println("Dimension after HVG: " + newData.length + " " + newData[0].length);
        return newData;
    }

    private double[][] convertToDouble(int[][] intData) {
        int dimensions = intData[0].length;//data[0].length;
        double[][] doubleData = new double[intData.length][];
        int nZeros = 0;
        for (int i = 0; i < intData.length; i++) {
            doubleData[i] = new double[dimensions];
            //doubleData[i][0] = (double) intData[i][0];
            //doubleData[i][1] = (double) intData[i][1];
            for (int j = 0; j < dimensions; j++) {
                doubleData[i][j] = Math.log(1 + (double) intData[i][j]);
                if (intData[i][j] == 0) {
                    nZeros++;
                }
            }
        }
        System.out.println("Sparsity: " + ((double)nZeros)/(doubleData.length*doubleData[0].length));
        System.out.println("Dimension: " + doubleData.length + " " + doubleData[0].length);
        return doubleData;
    }

    public int[][] readCSV(String filePath) {
        ArrayList<int[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            // Skip first row
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] stringValues = line.split(",");

                // Skip first value in the row
                if (stringValues.length <= 1) continue; // skip row if no data after first value

                int[] values = new int[stringValues.length - 1];
                for (int i = 1; i < stringValues.length; i++) { // start from index 1
                    String cleaned = stringValues[i].replaceAll("\"", "").trim();

                    if (cleaned.isEmpty()) {
                        values[i - 1] = 0; // empty cell → 0
                    } else {
                        values[i - 1] = Integer.parseInt(cleaned);
                    }
                }
                rows.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid number in CSV: " + e.getMessage());
        }

        // Convert ArrayList<int[]> to int[][]
        int[][] data = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }

        return data;
    }

    public BitSet[] getCuts() {
        return dataset.getLastCuts();
    }

    public double[] getCutCosts() {
        return dataset.getLastCosts();
    }

    public int[] getGroundTruth() {
        return groundTruth;
    }

    public double[][] getProjectedData() {
        return projectedData;
    }

    public double[][] getHvgData() {
        return hvgData;
    }

    public double[][] getDoubleData() {
        return doubleData;
    }

    public int[][] getOriginalData() {
        return originalData;
    }

    public int[] getHardClustering() {
        return hardClustering;
    }

    public ScRNAseqDataset getDataset() {
        return dataset;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
        tangleClusterer.setMonitor(monitor);
    }

}

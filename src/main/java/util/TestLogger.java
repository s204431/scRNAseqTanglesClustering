package util;

public class TestLogger {
    public final int tests;
    public final int configs;
    public final int runs;
    public final TestResult[][] results;

    public TestLogger(int tests, int configs, int runs) {
        this.tests = tests;
        this.configs = configs;
        this.runs = runs;
        this.results = new TestResult[tests][configs];
    }

    public void setResult(String testName, String configName, int testIndex, int configIndex, int runIndex, double sparsity, double time, double nmi, double randIndex, int nClusters, double dimReducTime) {
        if (results[testIndex][configIndex] == null) {
            results[testIndex][configIndex] = new TestResult(testName, configName, runs, sparsity);
        }
        results[testIndex][configIndex].setResult(runIndex, time, nmi, randIndex, nClusters, dimReducTime);
    }

    public void printResults() {
        for (int i = 0; i < tests; i++) {
            for (int j = 0; j < configs; j++) {
                if (results[i][j] != null) {
                    System.out.println(results[i][j].toString());
                }
            }
        }
    }

    public void writeResultsCSV(String filePath) {
        int maxRuns = results[0][0].times.length;

        // Header
        StringBuilder sb = new StringBuilder();
        sb.append("Test Name,Config Name,Genes,Cells,Depth_Mean,Balanced,Complexity,Sparsity,Runs");
        for (int k = 1; k <= maxRuns; k++) sb.append(",Time_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",NMI_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",RandIndex_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",Clusters_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",DimReducTime_").append(k);
        sb.append("\n");

        // Rows
        for (int i = 0; i < tests; i++) {
            for (int j = 0; j < configs; j++) {
                TestResult result = results[i][j];
                if (result == null) continue;

                String[] parts = result.testName.split("_");
                int genes = -1;
                int cells = -1;
                int depthMean = -1;
                boolean balanced = true;
                String complexity = "unknown";
                double sparsity = result.sparsity;

                for (String part : parts) {
                    if (part.contains("genes")) {
                        genes = Integer.parseInt(part.replace("genes", ""));
                    } else if (part.contains("cells")) {
                        cells = Integer.parseInt(part.replace("cells", ""));
                    } else if (part.contains("depth")) {
                        if (part.contains("e+")) {
                            String[] depthParts = part.replace("depth", "").split("e+");
                            depthMean = (int) (Double.parseDouble(depthParts[0]) * Math.pow(10, Integer.parseInt(depthParts[1])));
                        } else depthMean = Integer.parseInt(part.replace("depth", ""));
                    } else if (part.contains("balanced")) {
                        balanced = Boolean.parseBoolean(part.replace("balanced", ""));
                    } else if (part.contains("complex")) {
                        complexity = "complex";
                    } else if (part.contains("simple")) {
                        complexity = "simple";
                    }
                }

                sb.append(result.testName)
                        .append(",").append(result.configName)
                        .append(",").append(genes)
                        .append(",").append(cells)
                        .append(",").append(depthMean)
                        .append(",").append(balanced)
                        .append(",").append(complexity)
                        .append(",").append(sparsity)
                        .append(",").append(runs);

                // Times
                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.times.length) sb.append(result.times[k]);
                }

                // NMIs
                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.nmis.length) sb.append(result.nmis[k]);
                }

                // Rand Indices
                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.randIndices.length) sb.append(result.randIndices[k]);
                }

                // Clusters
                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.times.length) sb.append(result.nClusters[k]);
                }

                // Dimensionality reduction times
                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.dimReductionTimes.length) sb.append(result.dimReductionTimes[k]);
                }

                sb.append("\n");
            }
        }

        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private class TestResult {
        public final String testName;
        public final String configName;
        public final double sparsity;
        public final double[] times;
        public final double[] nmis;
        public final double[] randIndices;
        public final int[] nClusters;
        public final double[] dimReductionTimes;

        public TestResult(String testName, String configName, int runs, double sparsity) {
            this.testName = testName;
            this.configName = configName;
            this.sparsity = sparsity;
            this.times = new double[runs];
            this.nmis = new double[runs];
            this.randIndices = new double[runs];
            this.nClusters = new int[runs];
            this.dimReductionTimes = new double[runs];
        }

        public void setResult(int runIndex, double time, double nmi, double randIndex, int nClusters, double dimReducTime) {
            this.times[runIndex] = time;
            this.nmis[runIndex] = nmi;
            this.randIndices[runIndex] = randIndex;
            this.nClusters[runIndex] = nClusters;
            this.dimReductionTimes[runIndex] = dimReducTime;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Test: ").append(testName).append(", Config: ").append(configName).append(", Sparsity").append(sparsity).append("\n");

            for (int i = 0; i < times.length; i++) {
                sb.append(" Run ").append(i + 1).append(": Time = ").append(times[i])
                  .append(", NMI = ").append(nmis[i])
                  .append(", Rand Index = ").append(randIndices[i])
                  .append("  Clusters = ").append(nClusters[i])
                  .append(" Dimention Reduction Time = ").append("\n");
            }
            return sb.toString();
        }
    }
}


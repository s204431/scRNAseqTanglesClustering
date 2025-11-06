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

    public void setResult(String testName, String configName, int testIndex, int configIndex, int runIndex, double time, double nmi, double randIndex) {
        if (results[testIndex][configIndex] == null) {
            results[testIndex][configIndex] = new TestResult(testName, configName, runs);
        }
        results[testIndex][configIndex].setResult(runIndex, time, nmi, randIndex);
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
        sb.append("Test Name,Config Name");
        for (int k = 1; k <= maxRuns; k++) sb.append(",Time_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",NMI_").append(k);
        for (int k = 1; k <= maxRuns; k++) sb.append(",RandIndex_").append(k);
        sb.append("\n");

        // Rows
        for (int i = 0; i < tests; i++) {
            for (int j = 0; j < configs; j++) {
                TestResult result = results[i][j];
                sb.append(result.testName).append(",").append(result.configName);

                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.times.length) sb.append(result.times[k]);
                }

                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.nmis.length) sb.append(result.nmis[k]);
                }

                for (int k = 0; k < maxRuns; k++) {
                    sb.append(",");
                    if (k < result.randIndices.length) sb.append(result.randIndices[k]);
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
        public final double[] times;
        public final double[] nmis;
        public final double[] randIndices;

        public TestResult(String testName, String configName, int runs) {
            this.testName = testName;
            this.configName = configName;
            this.times = new double[runs];
            this.nmis = new double[runs];
            this.randIndices = new double[runs];
        }

        public void setResult(int runIndex, double time, double nmi, double randIndex) {
            this.times[runIndex] = time;
            this.nmis[runIndex] = nmi;
            this.randIndices[runIndex] = randIndex;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Test: ").append(testName).append(", Config: ").append(configName).append("\n");

            for (int i = 0; i < times.length; i++) {
                sb.append(" Run ").append(i + 1).append(": Time = ").append(times[i])
                  .append(", NMI = ").append(nmis[i])
                  .append(", Rand Index = ").append(randIndices[i]).append("\n");
            }
            return sb.toString();
        }
    }
}


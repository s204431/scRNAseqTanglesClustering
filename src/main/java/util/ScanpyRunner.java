package util;

import com.google.gson.Gson;

import java.io.*;

public class ScanpyRunner {
    private static final boolean DEBUG = false;
    private static final boolean PRINT_ERRORS = false;

    private static Process process;
    private static BufferedWriter writer;
    private static BufferedReader reader;

    public static void startScanpy() {
        try {
            System.out.println("Starting Scanpy...");

            // Python script path
            String pythonScript = "scRNAseq.py";

            // Start Python process
            ProcessBuilder pb = new ProcessBuilder("python", pythonScript);
            //pb.redirectErrorStream(true);
            process = pb.start();

            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StderrGobbler errorGobbler = new StderrGobbler(process.getErrorStream());
            errorGobbler.setDaemon(true);
            errorGobbler.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopScanpy() {
        try {
            if (DEBUG) System.out.println("Stopping Scanpy...");

            // Send termination signal to Python
            writer.write("0"); //Indicates termination
            writer.newLine();
            writer.flush();

            int exitCode = process.waitFor();
            System.out.println("Python process exited with code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Tuple<int[], Double> runClustering(String filePath) {
        try {
            if (DEBUG) System.out.println("Running clustering in Scanpy...");

            // Send a string to Python
            String message = filePath;

            writer.write("1"); //Indicates a clustering request
            writer.newLine();

            writer.write(message); //File path of dataset to cluster
            writer.newLine();

            writer.write('1'); //Whether to use tuning (0 is off, 1 is on)
            writer.newLine();

            writer.flush();

            if (DEBUG) System.out.println("Flushed data to Python");

            // Read response from Python
            String response = reader.readLine(); // JSON string from Python

            if (DEBUG) System.out.println("Received response from Python: " + response.length());

            if (response == null) {
                throw new RuntimeException("Python process ended unexpectedly (no JSON).");
            }

            // Parse JSON to Java List<Integer>
            Gson gson = new Gson();
            int[] numbers = gson.fromJson(response, int[].class);

            // Read time
            String responseTime = reader.readLine();
            double pythonTime = Double.parseDouble(responseTime);

            return new Tuple<int[], Double>(numbers, pythonTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static class StderrGobbler extends Thread {
        private final InputStream is;

        StderrGobbler(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (PRINT_ERRORS) System.err.println("[PY STDERR] " + line);
                }
            } catch (Exception e) {}
        }
    }
}

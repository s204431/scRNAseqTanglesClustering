package visualization.test;

import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestResultPanel extends JPanel {
    private View view;
    private TestProgressManager testProgressManager;
    private TestProgressManager.Listener testProgressListener;

    private ResultsTable resultsTable = new ResultsTable(0, 3);
    private JTable table = new JTable(resultsTable);

    public TestResultPanel(View view, TestProgressManager testProgressManager) {
        setLayout(new BorderLayout());

        this.view = view;
        this.testProgressManager = testProgressManager;
        testProgressListener = new TestProgressManager.Listener() {
            @Override
            public void onTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex) {
                drawResultsTable(testIndex);
            }

            @Override
            public void onPythonFinished(int testIndex, double time, double nmi, double randIndex) {
                drawResultsTable(testIndex);
            }
        };
        testProgressManager.addListener(testProgressListener);

        table.setRowSelectionAllowed(false);
        addTableCellRenderer();

        add(new JScrollPane(table), BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Test Results"));
        resizeViewportToRows(4);
    }

    private void addTableCellRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);


                ((JComponent) c).setBorder(null);

                String desc = (String) table.getModel().getValueAt(row, 0);
                if (desc != null && (desc.startsWith("Test") || desc.startsWith("Averages"))) {

                    // Seperate each test block
                    ((JComponent) c).setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }

                return c;
            }
        });
    }

    public void drawResultsTable(int testIndex) {
        int nTests = testIndex + 1;
        int nConfigs = testProgressManager.getConfigsSize();

        boolean[] missingValues = new boolean[nConfigs + 1];
        double[] avgTimes = new double[nConfigs + 1];
        double[] avgNmiScores = new double[nConfigs + 1];
        double[] avgRandIndexScores = new double[nConfigs + 1];

        for (int i = 0; i < nTests; i++) {
            int baseRow = i * ResultsTable.ROW_OFFSET + ResultsTable.HEADER_ROWS;

            double[] timeValues = new double[nConfigs + 1];
            double[] nmiValues = new double[nConfigs + 1];
            double[] randIndexValues = new double[nConfigs + 1];

            for (int j = 0; j < nConfigs + 1; j++) {
                boolean isTangle = j < nConfigs;

                double time = isTangle ? testProgressManager.getTangleTime(j, i) : testProgressManager.getPythonTime(i);
                double nmi = isTangle ? testProgressManager.getTangleNMI(j, i) : testProgressManager.getPythonNMI(i);
                double randIndex = isTangle ? testProgressManager.getTangleRandIndex(j, i) : testProgressManager.getPythonRandIdx(i);

                timeValues[j] = time;
                nmiValues[j] = nmi;
                randIndexValues[j] = randIndex;

                if (time == 0 && nmi == 0 && randIndex == 0) {
                    missingValues[j] = true;
                } else {
                    missingValues[j] = false;
                }

                avgTimes[j] += time;
                avgNmiScores[j] += nmi;
                avgRandIndexScores[j] += randIndex;
            }

            resultsTable.setRowValues(baseRow + ResultsTable.ROW_TIME, timeValues);
            resultsTable.setRowValues(baseRow + ResultsTable.ROW_NMI, nmiValues);
            resultsTable.setRowValues(baseRow + ResultsTable.ROW_RAND_IDX, randIndexValues);
        }

        for (int i = 0; i < nConfigs + 1; i++) {
            int n = nTests;
            if (missingValues[i]) n--;

            avgTimes[i] /= n;
            avgNmiScores[i] /= n;
            avgRandIndexScores[i] /= n;
        }

        resultsTable.setRowValues(1, avgTimes);
        resultsTable.setRowValues(2, avgNmiScores);
        resultsTable.setRowValues(3, avgRandIndexScores);
    }

    public void initializeResultsTable() {
        int size = testProgressManager.getSize();
        int rowSize = testProgressManager.getConfigsSize() + 1;
        resultsTable = new ResultsTable(size, rowSize);
        table.setModel(resultsTable);
        resizeViewportToRows(size*4 + 4);
    }

    private void resizeViewportToRows(int maxRows) {
        int rows = Math.min(maxRows, table.getRowCount());
        int h = rows * table.getRowHeight();
        int w = getWidth();
        table.setPreferredScrollableViewportSize(new Dimension(w, h));
        table.revalidate();
    }

    private class ResultsTable extends AbstractTableModel {
        private static String[] COL_NAMES = new String[] { "", "" };

        public static final int HEADER_ROWS = 4;
        public static final int ROW_OFFSET = 4;
        private static final int ROW_TEST = 0;
        private static final int ROW_TIME = 1;
        private static final int ROW_NMI = 2;
        private static final int ROW_RAND_IDX = 3;

        private class Row {
            String description;
            double[] values;

            public Row(String description, double[] values) {
                this.description = description;
                this.values = values;
            }
        }

        private List<Row> rows = new ArrayList<>();
        private DecimalFormat df = new DecimalFormat("0.###");

        private int rowSize;

        public ResultsTable(int size, int rowSize) {
            this.rowSize = rowSize + 1;

            COL_NAMES = new String[rowSize + 1];
            for (int i = 1; i < rowSize + 1; i++) {
                if (testProgressManager == null) COL_NAMES[i] = "";
                else COL_NAMES[i] = testProgressManager.getTitle(i - 1);
            }
            COL_NAMES[0] = "Description";

            createEmptyRow("Averages");
            createEmptyRow("Avg Time");
            createEmptyRow("Avg NMI");
            createEmptyRow("Avg Rand Idx");

            for (int i = 0; i < size; i++) {
                createEmptyRow("Test " + (i+1));
                createEmptyRow("Time");
                createEmptyRow("NMI");
                createEmptyRow("Rand Idx");
            }

            fireTableDataChanged();
        }

        public void createEmptyRow(String dscr) {
            rows.add(new Row(dscr, new double[rowSize]));
        }

        public void setRowValues(int r, double[] values) {
            Row row = rows.get(r);
            row.values = values;
            fireTableRowsUpdated(r, r);
        }

        public void setRowDescription(int r, String dscr) {
            rows.get(r).description = dscr;
            fireTableRowsUpdated(r, 0);
        }

        public void reset() {
            rows.clear();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COL_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COL_NAMES[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rows.get(rowIndex);
            if (columnIndex == 0) return row.description;
            if (isFillerRow(rowIndex)) return "";
            return format(row.values[columnIndex - 1]);
        }

        private String format(Double val) {
            return (val == null || val == 0) ? "-" : df.format(val);
        }

        private boolean isFillerRow(int rowIndex) {
            Row r = rows.get(rowIndex);
            return r.description.contains("Test") || r.description.contains("Averages");
        }
    }
}

package visualization.test;

import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestResultPanel extends JPanel {
    private View view;
    private TestEditPanel.TestProgressManager testProgressManager;
    private ResultsTable resultsTable = new ResultsTable(0);
    private JTable table = new JTable(resultsTable);

    public TestResultPanel(View view, TestEditPanel.TestProgressManager testProgressManager) {
        this.view = view;
        this.testProgressManager = testProgressManager;
        setLayout(new BorderLayout());

        //table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Test Results"));
        resizeViewportToRows(3);
    }

    public void drawResultsTable(int low, int high, boolean isTangle) {
        // Fill rows
        int n = high - low + 1;

        double tangleAvgTime = 0;
        double pythonAvgTime = 0;
        double tangleAvgNmi = 0;
        double pythonAvgNmi = 0;
        double tangleAvgRandIdx = 0;
        double pythonAvgRandIdx = 0;

        for (int i = 0; i < n; i++) {
            int baseRow = i * ResultsTable.ROW_OFFSET + ResultsTable.HEADER_ROWS;

            double tangleTime = testProgressManager.getTime(i, true);
            double pythonTime = testProgressManager.getTime(i, false);
            double tangleNmi = testProgressManager.getNMI(i, true);
            double pythonNmi = testProgressManager.getNMI(i, false);
            double tangleRandIdx = testProgressManager.getRandIdx(i, true);
            double pythonRandIdx = testProgressManager.getRandIdx(i, false);

            resultsTable.setRowValues(baseRow + ResultsTable.ROW_TIME, tangleTime, pythonTime);
            resultsTable.setRowValues(baseRow + ResultsTable.ROW_NMI, tangleNmi, pythonNmi);
            resultsTable.setRowValues(baseRow + ResultsTable.ROW_RAND_IDX, tangleRandIdx, pythonRandIdx);

            tangleAvgTime += tangleTime;
            pythonAvgTime += pythonTime;
            tangleAvgNmi += tangleNmi;
            pythonAvgNmi += pythonNmi;
            tangleAvgRandIdx += tangleRandIdx;
            pythonAvgRandIdx += pythonRandIdx;
        }

        tangleAvgTime /= n;
        pythonAvgTime /= isTangle ? n-1 : n;
        tangleAvgNmi /= n;
        pythonAvgNmi /= isTangle ? n-1 : n;
        tangleAvgRandIdx /= n;
        pythonAvgRandIdx /= isTangle ? n-1 : n;
        resultsTable.setRowValues(0, tangleAvgTime, pythonAvgTime);
        resultsTable.setRowValues(1, tangleAvgNmi, pythonAvgNmi);
        resultsTable.setRowValues(2, tangleAvgRandIdx, pythonAvgRandIdx);
    }

    public void initializeResultsTable() {
        int size = testProgressManager.getSize();
        resultsTable = new ResultsTable(size);
        table.setModel(resultsTable);
        resizeViewportToRows(size*4 + 3);
    }

    private void resizeViewportToRows(int maxRows) {
        int rows = Math.min(maxRows, table.getRowCount());
        int h = rows * table.getRowHeight();
        int w = getWidth();
        table.setPreferredScrollableViewportSize(new Dimension(w, h));
        table.revalidate();
    }

    private class ResultsTable extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = { "", "Tangle", "Python" };

        public static final int HEADER_ROWS = 3;
        public static final int ROW_OFFSET = 4;
        private static final int ROW_TEST = 0;
        private static final int ROW_TIME = 1;
        private static final int ROW_NMI = 2;
        private static final int ROW_RAND_IDX = 3;

        private class Row {
            String description;
            double tangleVal;
            double pythonVal;

            public Row(String description, double tangleVal, double pythonVal) {
                this.description = description;
                this.tangleVal = tangleVal;
                this.pythonVal = pythonVal;
            }
        }

        private List<Row> rows = new ArrayList<>();
        private DecimalFormat df = new DecimalFormat("0.###");

        public ResultsTable(int size) {
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
            rows.add(new Row(dscr, 0, 0));
        }

        public void setRowValues(int r, double tangleVal, double pythonVal) {
            Row row = rows.get(r);
            row.tangleVal = tangleVal;
            row.pythonVal = pythonVal;
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
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rows.get(rowIndex);

            switch (columnIndex) {
                case 0: return row.description;
                case 1: if (isTestRow(rowIndex)) return ""; else return format(row.tangleVal);
                case 2: if (isTestRow(rowIndex)) return ""; else return format(row.pythonVal);
                default: return "";
            }
        }

        private String format(Double val) {
            return (val == null || val == 0) ? "-" : df.format(val);
        }

        private boolean isTestRow(int rowIndex) {
            return rows.get(rowIndex).description.contains("Test");
        }
    }
}

package visualization.testSet;

import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TestEditPanel extends JPanel {
    private View view;

    private javax.swing.Timer timer;
    private TestProgressManager testProgressManager;
    private int[] rowsPending;

    private final JButton selectButton = new JButton("Select");
    private final JButton unselectButton = new JButton("Unselect");
    private final JButton selectAllButton = new JButton("Select All");
    private final JButton selectNoneButton = new JButton("Select None");
    private final JButton invertButton = new JButton("Invert All");

    private final TestSetTable testSetTable = new TestSetTable();
    private final JTable table = new JTable(testSetTable) {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);

            // Convert view row to model row (sorting is enabled)
            int modelRow = convertRowIndexToModel(row);
            TestStatus status = testSetTable.getStatus(modelRow);

            // Don't override selection highlight
            if (!isRowSelected(row)) {
                switch (status) {
                    case FINISHED:
                        c.setBackground(new Color(198, 239, 206)); // light green
                        c.setForeground(Color.BLACK);
                        break;
                    case PENDING:
                        c.setBackground(new Color(255, 242, 204)); // light yellow
                        c.setForeground(Color.BLACK);
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                        c.setForeground(getForeground());
                }
            } else {
                c.setBackground(getSelectionBackground());
                c.setForeground(getSelectionForeground());
            }

            if (c instanceof JComponent) {
                ((JComponent) c).setOpaque(true);
            }
            return c;
        }
    };
    private final JScrollPane scrollPane = new JScrollPane(table);

    public TestEditPanel(View view) {
        this.view = view;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        table.setFillsViewportHeight(false);
        //table.setAutoCreateRowSorter(true);
        table.setRowSelectionAllowed(true);
        table.getColumnModel().getColumn(0).setMaxWidth(400);


        scrollPane.setBorder(BorderFactory.createTitledBorder("Tests in test set"));
        add(scrollPane);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(selectButton);
        toolbar.add(unselectButton);
        toolbar.add(selectAllButton);
        toolbar.add(selectNoneButton);
        add(toolbar);

        selectButton.addActionListener(e -> testSetTable.setMarked(table.getSelectedRows(), true));
        unselectButton.addActionListener(e -> testSetTable.setMarked(table.getSelectedRows(), false));
        selectAllButton.addActionListener(e -> testSetTable.setAll(true));
        selectNoneButton.addActionListener(e -> testSetTable.setAll(false));
    }

    public void loadTestSet(List<File> selectedDirs) {
        List<TestRow> rows = new ArrayList<>();
        for (File f : selectedDirs) {
            rows.add(new TestRow(false, f));
        }
        testSetTable.setRows(rows);
        resizeTableViewportToRows();
    }

    private void resizeTableViewportToRows() {
        int rows = Math.max(1, Math.min(table.getRowCount(), 40));
        int rowH = table.getRowHeight();
        int headerH = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height : 0;

        int height = rows * rowH + headerH + 25;
        int width  = Math.max(table.getPreferredSize().width, 200);

        Dimension d = new Dimension(width, height);
        table.setPreferredScrollableViewportSize(d);

        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        scrollPane.revalidate();
    }

    public boolean isRunning() {
        return testProgressManager != null;
    }

    public File[] getSelectedTests() {
        return testSetTable.getSelectedFiles();
    }

    public void refreshTable() {
        boolean testingFinished = true;

        for (int i = 0; i < testProgressManager.getSize(); i++) {
            int row = rowsPending[i];
            TestStatus status = testSetTable.getStatus(row);
            if (!testProgressManager.getStatus(i, true) || !testProgressManager.getStatus(i, false)) {
                testingFinished = false;
                if (status != TestStatus.PENDING) {
                    testSetTable.setStatus(row, TestStatus.PENDING);
                }
            } else {
                if (status != TestStatus.FINISHED) {
                    testSetTable.setStatus(row, TestStatus.FINISHED);
                }
            }
        }

        if (testingFinished) {
            stopTimer();
            testProgressManager = null;
            testSetTable.resetRowStatus();
        }
    }

    public void startTimer() {
        timer = new Timer(500, e -> refreshTable());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        timer.start();
    }

    public void stopTimer() {
        timer.stop();
        timer = null;
    }

    public TestEditPanel.TestProgressManager getTestProgressManager() {
        rowsPending = testSetTable.getSelectedRows();
        testProgressManager = new TestProgressManager(rowsPending.length);
        return testProgressManager;
    }

    public static class TestProgressManager {
        private final int size;

        private final AtomicBoolean[] tangleFinished;
        private final AtomicBoolean[] pythonFinished;

        public TestProgressManager(int size) {
            this.size = size;

            tangleFinished = new AtomicBoolean[size];
            pythonFinished = new AtomicBoolean[size];
            for (int i = 0; i < size; i++) {
                tangleFinished[i] = new AtomicBoolean(false);
                pythonFinished[i] = new AtomicBoolean(false);
            }
        }

        public void markFinished(int i, boolean tangle) {
            AtomicBoolean[] array = tangle ? tangleFinished : pythonFinished;
            array[i].set(true);
        }

        public boolean getStatus(int i, boolean tangle) {
            AtomicBoolean[] array = tangle ? tangleFinished : pythonFinished;
            return array[i].get();
        }

        public int getSize() {
            return size;
        }
    }

    private enum TestStatus { DEFAULT, PENDING, FINISHED };

    private static final class TestRow {
        boolean run;
        final File file;
        TestStatus status = TestStatus.DEFAULT;

        TestRow(boolean run, File file) {
            this.run = run;
            this.file = file;
        }

        String name() {
            return file.getName();
        }
    }

    private static final class TestSetTable extends AbstractTableModel {
        private final String[] cols = {"Run", "Test name"};
        private final Class<?>[] types = {Boolean.class, String.class};
        private final List<TestRow> rows = new ArrayList<>();

        void setRows(List<TestRow> data) {
            rows.clear();
            rows.addAll(data);
            fireTableDataChanged();
        }

        void setMarked(int[] rows, boolean val) {
            for (int row : rows) {
                setValueAt(val, row, 0);
            }
        }

        void setAll(boolean val) {
            rows.forEach(r -> r.run = val);
            fireTableRowsUpdated(0, getRowCount()-1);
        }

        File[] getSelectedFiles() {
            List<File> files = new ArrayList<>();
            for (TestRow r : rows) {
                if (r.run) {
                    files.add(r.file);
                }
            }
            return files.toArray(new File[0]);
        }

        int[] getSelectedRows() {
            List<Integer> rowList = new ArrayList<>();
            int i = 0;
            for (TestRow r : rows) {
                if (r.run) {
                    rowList.add(i);
                }
                i++;
            }

            int[] out = new int[rowList.size()];
            for (int k = 0; k < rowList.size(); k++) {
                out[k] = rowList.get(k);
            }
            return out;
        }

        void setStatus(int row, TestStatus newStatus) {
            rows.get(row).status = newStatus;
            fireTableRowsUpdated(row, row);
        }

        TestStatus getStatus(int row) {
            return rows.get(row).status;
        }

        void resetRowStatus() {
            int length = 0;
            for (TestRow r : rows) {
                r.status = TestStatus.DEFAULT;
                length++;
            }
            fireTableRowsUpdated(0, length - 1);
        }

        @Override public int getRowCount() {
            return rows.size();
        }

        @Override public int getColumnCount() {
            return cols.length;
        }

        @Override public String getColumnName(int c) {
            return cols[c];
        }

        @Override public Class<?> getColumnClass(int c) {
            return types[c];
        }

        @Override public boolean isCellEditable(int r, int c) {
            return c == 0;
        }

        @Override public Object getValueAt(int r, int c) {
            TestRow tr = rows.get(r);
            return (c == 0) ? tr.run : tr.name();
        }

        @Override public void setValueAt(Object v, int r, int c) {
            if (c == 0) {
                rows.get(r).run = (Boolean) v;
                fireTableCellUpdated(r, c);
            }
        }
    }
}

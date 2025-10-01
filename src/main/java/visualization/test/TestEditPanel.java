package visualization.test;

import org.nd4j.common.primitives.AtomicDouble;
import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestEditPanel extends JPanel {
    private View view;

    private javax.swing.Timer timer;
    private final TestProgressManager testProgressManager = new TestProgressManager();
    private int[] rowsPending;

    private final JButton selectButton = new JButton("Select");
    private final JButton unselectButton = new JButton("Unselect");
    private final JButton selectAllButton = new JButton("Select All");
    private final JButton selectNoneButton = new JButton("Unselect all");

    private final TestSetTable testSetTable = new TestSetTable();
    private final JTable table = new JTable(testSetTable) {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);

            int modelRow = convertRowIndexToModel(row);
            TestStatus status = testSetTable.getStatus(modelRow);

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
            rows.add(new TestRow(true, f));
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
        return timer != null;
    }

    public File[] getSelectedTests() {
        return testSetTable.getSelectedFiles();
    }

    public void updateResults() {
        boolean testingFinished = true;

        for (int j = 0; j < 2; j++) {
            boolean isTangle = j == 0;

            for (int i = 0; i < testProgressManager.getSize(); i++) {
                int row = rowsPending[i];
                TestStatus status = testSetTable.getStatus(row);

                if (!testProgressManager.getStatus(i, isTangle)) {
                    testingFinished = false;
                    if (status != TestStatus.PENDING) testSetTable.setStatus(row, TestStatus.PENDING);
                } else {
                    if (status != TestStatus.FINISHED) {
                        // New test has finished
                        if (!isTangle) testSetTable.setStatus(row, TestStatus.FINISHED);
                        view.visualizeTestResults(0, i, isTangle);
                    }
                }
            }
        }

        if (testingFinished) {
            testSetTable.resetRowStatus();
        }
    }

    public void startTimer() {
        timer = new Timer(500, e -> updateResults());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        timer.start();
    }

    public void stopTimer() {
        updateResults();

        if (timer == null) {
            return;
        }

        timer.stop();
        timer = null;
    }

    public TestEditPanel.TestProgressManager initializeTestProgressManager() {
        rowsPending = testSetTable.getSelectedRows();
        testProgressManager.reset(rowsPending.length);
        return testProgressManager;
    }

    public TestProgressManager getTestProgressManager() {
        return testProgressManager;
    }

    public static class TestProgressManager {
        private int size;

        private AtomicBoolean[] tangleFinished;
        private AtomicBoolean[] pythonFinished;

        private AtomicDouble[] tangleTimes;
        private AtomicDouble[] pythonTimes;

        private AtomicDouble[] tangleNMI;
        private AtomicDouble[] pythonNMI;

        private AtomicDouble[] tangleRandIndex;
        private AtomicDouble[] pythonRandIndex;

        public TestProgressManager() {
            reset(0);
        }

        public void markFinished(int i, boolean tangle, double time, double NMI, double randIndex) {
            if (tangle) {
                tangleFinished[i].set(true);
                tangleTimes[i].set(time);
                tangleNMI[i].set(NMI);
                tangleRandIndex[i].set(randIndex);
            } else {
                pythonFinished[i].set(true);
                pythonTimes[i].set(time);
                pythonNMI[i].set(NMI);
                pythonRandIndex[i].set(randIndex);
            }
        }

        public boolean getStatus(int i, boolean tangle) {
            AtomicBoolean[] array = tangle ? tangleFinished : pythonFinished;
            return array[i].get();
        }

        public double getTime(int i, boolean tangle) {
            AtomicDouble[] array = tangle ? tangleTimes : pythonTimes;
            return array[i].get();
        }

        public double getNMI(int i, boolean tangle) {
            AtomicDouble[] array = tangle ? tangleNMI : pythonNMI;
            return array[i].get();
        }

        public double getRandIdx(int i, boolean tangle) {
            AtomicDouble[] array = tangle ? tangleRandIndex : pythonRandIndex;
            return array[i].get();
        }

        public int getSize() {
            return size;
        }

        public void reset(int size) {
            this.size = size;

            tangleFinished = new AtomicBoolean[size];
            pythonFinished = new AtomicBoolean[size];
            tangleTimes = new AtomicDouble[size];
            pythonTimes = new AtomicDouble[size];
            tangleNMI = new AtomicDouble[size];
            pythonNMI = new AtomicDouble[size];
            tangleRandIndex = new AtomicDouble[size];
            pythonRandIndex = new AtomicDouble[size];

            for (int i = 0; i < size; i++) {
                tangleFinished[i] = new AtomicBoolean(false);
                pythonFinished[i] = new AtomicBoolean(false);
                tangleTimes[i] = new AtomicDouble();
                pythonTimes[i] = new AtomicDouble();
                tangleNMI[i] = new AtomicDouble();
                pythonNMI[i] = new AtomicDouble();
                tangleRandIndex[i] = new AtomicDouble();
                pythonRandIndex[i] = new AtomicDouble();
            }
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

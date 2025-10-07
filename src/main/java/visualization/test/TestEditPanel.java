package visualization.test;

import org.nd4j.common.primitives.AtomicDouble;
import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TestEditPanel extends JPanel {
    private View view;

    private javax.swing.Timer timer;
    private final TestProgressManager testProgressManager = new TestProgressManager();
    private int[] testRowsPending;
    private int[] configRowsPending;

    private final EditableTable editableTestTable = new EditableTable(new String[] {"Run", "Test Name"});
    private final JTable testEditTable = createModifiedJTable(editableTestTable);
    private final JScrollPane testEditScrollPane = new JScrollPane(testEditTable);

    private final EditableTable editableConfigTable = new EditableTable(new String[] {"Run", "Config Name"});
    private final JTable configEditTable = createModifiedJTable(editableConfigTable);
    private final JScrollPane configEditScrollPane = new JScrollPane(configEditTable);

    public TestEditPanel(View view) {
        this.view = view;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addTableWithToolbar(editableTestTable, testEditTable, testEditScrollPane, "Choose tests to run");
        add(Box.createRigidArea(new Dimension(0, 50)));
        addTableWithToolbar(editableConfigTable, configEditTable, configEditScrollPane, "Choose additional configurations to run");
    }

    private void addTableWithToolbar(EditableTable editableTable, JTable table, JScrollPane scrollPane, String titledBorder) {
        table.setFillsViewportHeight(false);
        //editTable.setAutoCreateRowSorter(true);
        table.setRowSelectionAllowed(true);
        table.getColumnModel().getColumn(0).setMaxWidth(100);

        scrollPane.setBorder(BorderFactory.createTitledBorder(titledBorder));
        add(scrollPane);

        JButton selectButton = new JButton("Select");
        JButton unselectButton = new JButton("Unselect");
        JButton selectAllButton = new JButton("Select All");
        JButton selectNoneButton = new JButton("Unselect All");

        selectButton.addActionListener(e -> editableTable.setMarked(table.getSelectedRows(), true));
        unselectButton.addActionListener(e -> editableTable.setMarked(table.getSelectedRows(), false));
        selectAllButton.addActionListener(e -> editableTable.setAll(true));
        selectNoneButton.addActionListener(e -> editableTable.setAll(false));

        JToolBar testToolBar = new JToolBar();
        testToolBar.setFloatable(false);
        testToolBar.add(selectButton);
        testToolBar.add(unselectButton);
        testToolBar.add(selectAllButton);
        testToolBar.add(selectNoneButton);

        add(testToolBar);
    }

    public void loadTestSet(List<File> selectedDirs) {
        List<TestRow> rows = new ArrayList<>();
        for (File f : selectedDirs) {
            rows.add(new TestRow(true, f));
        }
        editableTestTable.setRows(rows);
        resizeTableViewportToRows(testEditTable, testEditScrollPane);
    }

    public void loadConfigFiles() {
        Path dir = Paths.get("config");
        File folder = dir.toFile();

        List<TestRow> rows = new ArrayList<>();
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Error in TestEditPanel: Config folder not found");
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isFile()) continue;
            rows.add(new TestRow(false, f));
        }

        editableConfigTable.setRows(rows);
        resizeTableViewportToRows(configEditTable, configEditScrollPane);
    }

    private void resizeTableViewportToRows(JTable table, JScrollPane scrollPane) {
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

    public File[] getSelectedConfigFiles() {
        return editableConfigTable.getSelectedFiles();
    }

    public File[] getSelectedTests() {
        return editableTestTable.getSelectedFiles();
    }

    public void updateResults() {
        boolean testingFinished = true;

        for (int testIndex = 0; testIndex < testProgressManager.getSize(); testIndex++) {
            int testRow = testRowsPending[testIndex];
            TestStatus testStatus = editableTestTable.getStatus(testRow);
            boolean currentTestFinished = true;

            // Tangle configurations
            for (int configIndex = 0; configIndex < testProgressManager.getConfigsSize(); configIndex++) {
                if (!testProgressManager.getTangleStatus(configIndex, testIndex)) {
                    testingFinished = false;
                    currentTestFinished = false;
                } else {
                    if (testStatus != TestStatus.FINISHED) {
                        // New test has finished
                        view.visualizeTestResults(0, testIndex);
                    }
                }
            }

            // Python
            if (!testProgressManager.getPythonStatus(testIndex)) {
                testingFinished = false;
                currentTestFinished = false;
            } else {
                if (testStatus != TestStatus.FINISHED) {
                    // New test has finished
                    view.visualizeTestResults(0, testIndex);
                }
            }

            // Color test table
            if (currentTestFinished) {
                editableTestTable.setStatus(testRow, TestStatus.FINISHED);
            } else {
                editableTestTable.setStatus(testRow, TestStatus.PENDING);
            }
        }

        if (testingFinished) {
            editableTestTable.resetRowStatus();
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

    private JTable createModifiedJTable(EditableTable editableTable) {
        JTable newJTable = new JTable(editableTable) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                int modelRow = convertRowIndexToModel(row);
                TestStatus status = editableTable.getStatus(modelRow);

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
        return newJTable;
    }

    public TestEditPanel.TestProgressManager initializeTestProgressManager() {
        testRowsPending = editableTestTable.getSelectedRows();
        configRowsPending = editableConfigTable.getSelectedRows();
        testProgressManager.reset(testRowsPending.length, configRowsPending.length + 1);
        return testProgressManager;
    }

    public TestProgressManager getTestProgressManager() {
        return testProgressManager;
    }

    public static class TestProgressManager {
        private int size;
        private int configs;

        private String[] titles;

        private AtomicReferenceArray<AtomicBoolean[]> tangleFinished;
        private AtomicBoolean[] pythonFinished;

        private AtomicReferenceArray<AtomicDouble[]> tangleTimes;
        private AtomicDouble[] pythonTimes;

        private AtomicReferenceArray<AtomicDouble[]> tangleNMI;
        private AtomicDouble[] pythonNMI;

        private AtomicReferenceArray<AtomicDouble[]> tangleRandIndex;
        private AtomicDouble[] pythonRandIndex;

        public TestProgressManager() {
            reset(0, 0);
        }

        public void markTangleFinished(int configIndex, int i, double time, double nmi, double randIndex) {
            System.out.println("Size: " + this.size + " Configs: " + this.configs);
            tangleFinished.get(configIndex)[i].set(true);
            tangleTimes.get(configIndex)[i].set(time);
            tangleNMI.get(configIndex)[i].set(nmi);
            tangleRandIndex.get(configIndex)[i].set(randIndex);
        }

        public void markPythonFinished(int i, double time, double nmi, double randIndex) {
            pythonFinished[i].set(true);
            pythonTimes[i].set(time);
            pythonNMI[i].set(nmi);
            pythonRandIndex[i].set(randIndex);
        }

        public void setTitles(String[] titles) {
            this.titles = titles;
        }

        public boolean getTangleStatus(int configIndex, int i) {
            return tangleFinished.get(configIndex)[i].get();
        }

        public double getTangleTime(int configIndex, int i) {
            return tangleTimes.get(configIndex)[i].get();
        }

        public double getTangleNMI(int configIndex, int i) {
            return tangleNMI.get(configIndex)[i].get();
        }

        public double getTangleRandIndex(int configIndex, int i) {
            return tangleRandIndex.get(configIndex)[i].get();
        }

        public boolean getPythonStatus(int i) {
            return pythonFinished[i].get();
        }

        public double getPythonTime(int i) {
            return pythonTimes[i].get();
        }

        public double getPythonNMI(int i) {
            return pythonNMI[i].get();
        }

        public double getPythonRandIdx(int i) {
            return pythonRandIndex[i].get();
        }

        public int getSize() {
            return size;
        }

        public int getConfigsSize() {
            return configs;
        }

        public String getTitle(int i) {
            if (titles == null) return "";
            return titles[i];
        }

        public void reset(int size, int configurations) {
            this.size = size;
            this.configs = configurations;

            tangleFinished = new AtomicReferenceArray<>(configurations);
            tangleTimes = new AtomicReferenceArray<>(configurations);
            tangleNMI = new AtomicReferenceArray<>(configurations);
            tangleRandIndex = new AtomicReferenceArray<>(configurations);
            for (int i = 0; i < configurations; i++) {
                tangleFinished.set(i, new AtomicBoolean[size]);
                tangleTimes.set(i, new AtomicDouble[size]);
                tangleNMI.set(i, new AtomicDouble[size]);
                tangleRandIndex.set(i, new AtomicDouble[size]);
                for (int j = 0; j < size; j++) {
                    tangleFinished.get(i)[j] = new AtomicBoolean(false);
                    tangleTimes.get(i)[j] = new AtomicDouble();
                    tangleNMI.get(i)[j] = new AtomicDouble();
                    tangleRandIndex.get(i)[j] = new AtomicDouble();
                }
            }

            pythonFinished = new AtomicBoolean[size];
            pythonTimes = new AtomicDouble[size];
            pythonNMI = new AtomicDouble[size];
            pythonRandIndex = new AtomicDouble[size];
            for (int i = 0; i < size; i++) {
                pythonFinished[i] = new AtomicBoolean(false);
                pythonTimes[i] = new AtomicDouble();
                pythonNMI[i] = new AtomicDouble();
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

    private static final class EditableTable extends AbstractTableModel {
        private String[] cols;
        private Class<?>[] types = new Class<?>[] {Boolean.class, String.class};
        private List<TestRow> rows = new ArrayList<>();

        public EditableTable(String[] columnNames) {
            this.cols = columnNames;
        }

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

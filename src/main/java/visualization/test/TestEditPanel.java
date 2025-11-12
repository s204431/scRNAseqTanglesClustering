package visualization.test;

import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.File;

public class TestEditPanel extends JPanel {
    public static final Color LIGHT_GREEN = new Color(100, 240, 100);
    public static final Color LIGHT_YELLOW = new Color(240, 240, 150);

    private View view;

    private final TestProgressManager testProgressManager = new TestProgressManager();
    TestProgressManager.Listener testProgressListener = new TestProgressManager.Listener() {
        @Override
        public void onTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex) {
            SwingUtilities.invokeLater(() -> updateResults());
        }
        @Override
        public void onPythonFinished(int testIndex, double time, double nmi, double randIndex) {
            SwingUtilities.invokeLater(() -> updateResults());
        }
        @Override
        public void onAllFinished() {
            SwingUtilities.invokeLater(() -> {
                updateResults();
                editableTestTable.resetRowStatus();
            });
        }
    };

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
        testProgressManager.addListener(testProgressListener);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addTableWithToolbar(editableTestTable, testEditTable, testEditScrollPane, "Choose tests to run");
        add(Box.createRigidArea(new Dimension(0, 50)));
        addTableWithToolbar(editableConfigTable, configEditTable, configEditScrollPane, "Choose additional configurations to run");

        addDoubleClickFunctionality(editableTestTable, testEditTable);
        addDoubleClickFunctionality(editableConfigTable, configEditTable);
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

    public void addDoubleClickFunctionality(EditableTable editableTable, JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = table.rowAtPoint(e.getPoint());
                    int viewCol = table.columnAtPoint(e.getPoint());
                    if (viewRow < 0 || viewCol == 0) return;

                    int modelRow = table.convertRowIndexToModel(viewRow);
                    File f = editableTable.getFileAtModelRow(modelRow);
                    openFile(f);
                }
            }
        });
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

    public File[] getSelectedConfigFiles() {
        return editableConfigTable.getSelectedFiles();
    }

    public File[] getSelectedTests() {
        return editableTestTable.getSelectedFiles();
    }

    // NOTE THAT THIS METHOD DOES MUCH UNNECESSARY WORK AFTER ADDING LISTENERS TO TestProgressManager CLASS...
    // TODO: MAKE METHOD MORE EFFICIENT
    public void updateResults() {
          for (int testIndex = 0; testIndex < testProgressManager.getSize(); testIndex++) {
            int testRow = testRowsPending[testIndex];
            TestStatus testStatus = editableTestTable.getStatus(testRow);
            boolean currentTestFinished = true;

            // Tangle configurations
            for (int configIndex = 0; configIndex < testProgressManager.getConfigsSize(); configIndex++) {
                if (!testProgressManager.getTangleStatus(configIndex, testIndex)) {
                    currentTestFinished = false;
                }
            }

            // Python
            if (!testProgressManager.getPythonStatus(testIndex)) {
                currentTestFinished = false;
            }

            // Color test table
            if (currentTestFinished) {
                editableTestTable.setStatus(testRow, TestStatus.FINISHED);
            } else {
                editableTestTable.setStatus(testRow, TestStatus.PENDING);
            }
        }
    }

    public void stopTesting() {
        updateResults();
        editableTestTable.resetRowStatus();
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
                            c.setBackground(LIGHT_GREEN); // Bright green
                            c.setForeground(Color.BLACK);
                            break;
                        case PENDING:
                            c.setBackground(LIGHT_YELLOW); // Bright yellow
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

    public TestProgressManager initializeTestProgressManager() {
        testRowsPending = editableTestTable.getSelectedRows();
        configRowsPending = editableConfigTable.getSelectedRows();
        // In case no configs were chosen, we use user-defined configs
        testProgressManager.reset(testRowsPending.length, configRowsPending.length == 0 ? 1 : configRowsPending.length);
        updateResults();
        return testProgressManager;
    }

    public TestProgressManager getTestProgressManager() {
        return testProgressManager;
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

        File getFileAtModelRow(int modelRow) {
            return rows.get(modelRow).file;
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

    private void openFile(File file) {
        if (file == null || !file.isFile()) return;

        try {
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this,
                        "Desktop integration is not supported on this platform.",
                        "Cannot open file", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                JOptionPane.showMessageDialog(this,
                        "OPEN action is not supported on this platform.",
                        "Cannot open file", JOptionPane.WARNING_MESSAGE);
                return;
            }

            desktop.open(file);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to open file:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package visualization.testSet;

import visualization.View;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.File;

public class TestEditPanel extends JPanel {
    private View view;

    private final TestSetTable testSetTable = new TestSetTable();
    private final JTable table = new JTable(testSetTable);
    private final JScrollPane scrollPane = new JScrollPane(table);

    private final JButton selectButton = new JButton("Select");
    private final JButton unselectButton = new JButton("Unselect");
    private final JButton selectAllButton = new JButton("Select All");
    private final JButton selectNoneButton = new JButton("Select None");
    private final JButton invertButton = new JButton("Invert All");

    public TestEditPanel(View view) {
        this.view = view;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        table.setFillsViewportHeight(false);
        table.setAutoCreateRowSorter(true);
        table.setRowSelectionAllowed(true);
        table.getColumnModel().getColumn(0).setMaxWidth(400);


        scrollPane.setBorder(BorderFactory.createTitledBorder("Tests in test set"));
        add(scrollPane);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(true);
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
        resizeTableViewportToRows(40);
    }

    private void resizeTableViewportToRows(int maxRows) {
        int rows = Math.max(1, Math.min(table.getRowCount(), maxRows));
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

    public File[] getSelectedTests() {
        return testSetTable.getSelectedFiles();
    }

    private static final class TestRow {
        boolean run;
        final File file;

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

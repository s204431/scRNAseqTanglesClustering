package visualization;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TopPanel extends JPanel {
    private final View view;

    public TopPanel(View view) {
        this.view = view;

        setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("Open");
        openButton.addActionListener(this::openAction);

        JButton testSetButton = new JButton("Run test set");
        testSetButton.addActionListener(this::testSetAction);

        add(openButton);
        add(testSetButton);
    }

    private void openAction(ActionEvent e) {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser fileChooser = new JFileChooser();

        // Start in data project folder if possible
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path dataDir = projectRoot.resolve("data");
        Path dir = Files.isDirectory(dataDir) ? dataDir : projectRoot;
        fileChooser.setCurrentDirectory(dir.toFile());

        // Only show csv files for observed counts
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.contains("observed_counts") && name.endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV files with 'observed counts' in the name";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            view.loadDataset(selected.getAbsolutePath());
        }
    }

    private void testSetAction(ActionEvent e) {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser dirChooser = new JFileChooser();

        // Start in project folder
        dirChooser.setCurrentDirectory(Paths.get(System.getProperty("user.dir")).toFile());

        // Directories only
        dirChooser.setDialogTitle("Select test set folder(s) to run");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setMultiSelectionEnabled(true);
        dirChooser.setAcceptAllFileFilterUsed(false);

        int result = dirChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedDirs = dirChooser.getSelectedFiles();
            if (selectedDirs == null || selectedDirs.length == 0) {
                // Only one dir was chosen
                File singleDir = dirChooser.getSelectedFile();
                if (singleDir != null) {
                    selectedDirs = new File[] { singleDir };
                }
            }

            for (File dir : selectedDirs) {
                if (dir != null && dir.isDirectory()) {
                    view.runTestSet(dir.getAbsolutePath());
                }
            }
        }
    }
}

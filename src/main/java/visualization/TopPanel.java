package visualization;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TopPanel extends JPanel {
    private final View view;

    public TopPanel(View view) {
        this.view = view;

        setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("Open Data Set");
        openButton.addActionListener(this::openAction);

        JButton testSetButton = new JButton("Open Test Set");
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
            view.changeView(MainWindow.DATA_VIEW);
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
        dirChooser.setMultiSelectionEnabled(false);
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

            List<File> allFiles = new ArrayList<>();
            for (File f : selectedDirs) {
                if (f == null) continue;
                if (f.isFile()) {
                    allFiles.add(f);
                    continue;
                }
                if (f.isDirectory()) {
                    try (Stream<Path> s = Files.walk(f.toPath())) {
                        s.filter(p -> Files.isRegularFile(p))
                                .filter(p -> p.toString().contains("observed_counts"))
                                .filter(p -> p.toString().endsWith(".csv"))
                                .forEach(p -> allFiles.add(p.toFile()));
                    } catch (IOException ex) {
                        System.out.println("Failed to read " + f + ": " + ex.getMessage());
                    }
                }
            }

            view.showTestSet(allFiles);
            view.changeView(MainWindow.TEST_VIEW);
        }
    }
}

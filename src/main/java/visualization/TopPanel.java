package visualization;

import util.Config;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.NumberFormatter;
import java.io.IOException;
import java.text.NumberFormat;
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
        setBackground(new Color(220, 230, 245));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JButton openButton = new JButton("Open Data Set");
        openButton.addActionListener(this::openAction);

        JButton testSetButton = new JButton("Open Test Set");
        testSetButton.addActionListener(this::testSetAction);

        JButton saveConfigButton = new JButton("Save Config");
        saveConfigButton.addActionListener(this::saveConfigAction);

        JButton loadConfigButton = new JButton("Load Config");
        loadConfigButton.addActionListener(this::loadConfigAction);

        addButton(openButton);
        addButton(testSetButton);

        add(Box.createHorizontalGlue());

        addButton(saveConfigButton);
        addButton(loadConfigButton);

        add(Box.createHorizontalStrut(30));
    }

    private void addButton(JButton button) {
        add(Box.createHorizontalStrut(10));
        add(button);
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
                return (name.contains("obs") || name.contains("observed_counts")) && (name.endsWith(".csv") || name.endsWith(".h5ad"));
            }

            @Override
            public String getDescription() {
                return "CSV files with 'observed counts' in the name";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            // Ask user for number of highly variable genes
            JFormattedTextField input = new JFormattedTextField(new NumberFormatter(NumberFormat.getIntegerInstance()));
            int option = JOptionPane.showConfirmDialog(
                    this,
                    input,
                    "Enter number of highly variable genes:",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option == JOptionPane.OK_OPTION) {
                Number v = (Number) input.getValue();
                int hvg = v == null ? 0 : v.intValue();
                view.loadDataset(selected.getAbsolutePath(), hvg);
                view.changeView(MainWindow.DATA_VIEW);
            }
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
                                .filter(p -> (p.toString().contains("observed_counts") || p.toString().contains("obs")))
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

    private void saveConfigAction(ActionEvent e) {
        Config config = view.getCurrentConfigurations();
        if (config == null) {
            System.out.println("Error in top panel: Could not load current configurations");
            return;
        }

        JFormattedTextField input = new JFormattedTextField();
        int option = JOptionPane.showConfirmDialog(
                this,
                input,
                "Enter name of the config file:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String name = input.getText();
            if (!name.endsWith(".txt")) name += ".txt";
            config.saveConfiguration(name);
        }
    }

    private void loadConfigAction(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();

        Path configDir = Paths.get("config");
        chooser.setCurrentDirectory(configDir.toFile());

        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }
            @Override public String getDescription() {
                return "Config files (*.txt)";
            }
        });

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        if (selected == null || !selected.isFile()) return;

        String fileName = selected.getName();
        Config config = Config.loadConfiguration(fileName);
        if (config == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load the selected configuration.",
                    "Load Config",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        view.loadConfig(config);
    }
}

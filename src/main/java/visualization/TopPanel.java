package visualization;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

public class TopPanel extends JPanel {
    private final View view;

    public TopPanel(View view) {
        this.view = view;

        setPreferredSize(new Dimension(600, 40));
        setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("Open");
        openButton.setToolTipText("Open a data set or test set");
        openButton.addActionListener(this::onOpen);

        add(openButton);
    }

    private void onOpen(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select dataset file(s) or a test set folder");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // Common dataset extensions; tweak as needed
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Datasets (*.csv, *.tsv, *.json, *.xlsx, *.parquet)",
                "csv", "tsv", "json", "xlsx", "parquet"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.isMultiSelectionEnabled()
                    ? chooser.getSelectedFiles()
                    : new File[]{ chooser.getSelectedFile() };

            // TODO: Adapt these calls to whatever your View expects.
            // Examples:
            //   view.loadDataset(selected[i]);
            //   view.runTestsOn(selected[i]);
            // or a single unified entry point:
            //   view.openPath(selected[i].toPath());

            if (view != null) {
                Arrays.stream(selected).forEach(file -> {
                    // Replace this with your real handler:
                    // view.openPath(file.toPath());

                    // Temporary example: show what was chosen (remove in production)
                    System.out.println("Selected: " + file.getAbsolutePath());
                });
            }
        }
    }
}

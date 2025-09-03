package visualization;

import clustering.TangleClusterer;
import datasets.ScRNAseqDataset;
import smile.validation.metric.NormalizedMutualInformation;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ParameterPanel extends JPanel {
    private View view;

    private TextField aField = new TextField(10);
    private TextField psiField = new TextField(10);
    private JButton clusterButton = new JButton("Cluster");

    public ParameterPanel(View view) {
        this.view = view;

        //setPreferredSize(new Dimension(150, 800));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new Label("Dimension"), gbc);

        gbc.gridy = 1;
        add(new Label("Clustering"), gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new Label("a"), gbc);
        gbc.gridx = 1;
        add(aField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        add(new Label("psi"), gbc);
        gbc.gridx = 1;
        add(psiField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        add(clusterButton, gbc);

        clusterButton.addActionListener(e -> {
            String aValue = aField.getText();
            String psiValue = psiField.getText();
            view.performClustering(
                    Integer.parseInt(aField.getText()),
                    Double.parseDouble(psiField.getText()),
                    "Range",
                    "Distance To Mean"
            );
        });
    }
}

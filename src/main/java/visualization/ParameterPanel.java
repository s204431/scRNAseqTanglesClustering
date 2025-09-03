package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class ParameterPanel extends JPanel {
    private View view;

    private TextField aField = new TextField(10);
    private TextField psiField = new TextField(10);
    private JButton clusterButton = new JButton("Cluster");
    private JButton groundTruthButton = new JButton("Show Ground Truth");

    public ParameterPanel(View view) {
        this.view = view;

        String fontName = "Arial";
        int titleSize = 14;
        int textSize = 12;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel dimensionLabel = new JLabel("Dimension");
        dimensionLabel.setFont(new Font(fontName, Font.BOLD, titleSize));
        add(dimensionLabel, gbc);

        gbc.gridy = 1;
        JLabel clusteringLabel = new JLabel("Clustering");
        clusteringLabel.setFont(new Font(fontName, Font.BOLD, titleSize));
        add(clusteringLabel, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel aLabel = new JLabel("a");
        aLabel.setFont(new Font(fontName, Font.PLAIN, textSize));
        add(aLabel, gbc);
        gbc.gridx = 1;
        add(aField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel psiLabel = new JLabel("psi");
        aLabel.setFont(new Font(fontName, Font.PLAIN, textSize));
        add(psiLabel, gbc);
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

        gbc.gridy = 4;
        gbc.gridx = 1;
        JCheckBox groundTruthCheckBox = new JCheckBox("Show Ground Truth");
        groundTruthCheckBox.setSelected(false);
        add(groundTruthCheckBox, gbc);
        groundTruthCheckBox.addItemListener(e -> {
            boolean isChecked = (e.getStateChange() == ItemEvent.SELECTED);
            if (isChecked) {
                view.showGroundTruth();
            } else {
                view.showClustering();
            }
        });


    }
}

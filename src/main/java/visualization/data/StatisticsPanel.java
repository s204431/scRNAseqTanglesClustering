package visualization.data;

import datasets.ScRNAseqDataset;
import visualization.View;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class StatisticsPanel extends JScrollPane {
    private View view;

    private JTextArea textArea;

    public StatisticsPanel(View view) {
        this.view = view;
        setPreferredSize(new Dimension(getWidth(), getHeight()));
        textArea = new JTextArea();
        setViewportView(textArea);
    }

    public void showInformation(ScRNAseqDataset dataSet) {
        int cells = dataSet.data.length;
        int dimensions = dataSet.data[0].length;

        StringBuilder fillerText = new StringBuilder();
        int x = 1;
        for (int i = 0; i < 20; i++) {
            fillerText.append(" More stuff... ").append(x++).append("\n");
        }

        textArea.setText(
                " Cells: " + cells + "\n" +
                        " Dimensions: " + dimensions + "\n" +
                        fillerText
        );

        textArea.setCaretPosition(0);
    }
}

package visualization;

import clustering.Model;

import javax.swing.*;

public class View {
    private Model model;
    private MainWindow window;

    private double[][] points;

    public View(Model model) {
        this.model = model;

        points = model.tsne(model.getDoubleData(), 2);

        SwingUtilities.invokeLater(() -> {
            window = new MainWindow(this);
            window.drawPoints(points);
        });
    }

    public void performClustering(int a, double psi, String cutGeneratorName, String costFunctionName) {
        model.cluster(model.getDataset(), a, psi, cutGeneratorName, costFunctionName);
        window.drawClusters(points, model.getHardClustering());
    }
}

package monitor;

import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;

public class Monitor {
    private ScRNAseqDataset dataset;
    private TangleSearchTree tree;

    public void setDataset(ScRNAseqDataset dataset) {
        this.dataset = dataset;
    }

    public void setTree(TangleSearchTree tree) {
        this.tree = tree;
    }

    public ScRNAseqDataset getDataset() {
        return dataset;
    }

    public TangleSearchTree getTree() {
        return tree;
    }
}

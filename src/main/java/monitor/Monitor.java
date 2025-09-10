package monitor;

import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;

public class Monitor {
    private ScRNAseqDataset dataset;
    private TangleSearchTree uncondensedTree;
    private TangleSearchTree condensedTree;

    public void setDataset(ScRNAseqDataset dataset) {
        this.dataset = dataset;
    }

    public void setUncondensedTree(TangleSearchTree tree) {
        this.uncondensedTree = tree;
    }

    public void setCondensedTree(TangleSearchTree condensedTree) {
        this.condensedTree = condensedTree;
    }

    public ScRNAseqDataset getDataset() {
        return dataset;
    }

    public TangleSearchTree getUncondensedTree() {
        return uncondensedTree;
    }

    public TangleSearchTree getCondensedTree() {
        return condensedTree;
    }
}

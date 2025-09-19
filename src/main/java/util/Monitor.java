package util;

import clustering.TangleSearchTree;
import datasets.ScRNAseqDataset;

import java.util.List;

public class Monitor {
    private ScRNAseqDataset dataset;
    private TangleSearchTree uncondensedTree;
    private TangleSearchTree condensedTree;
    private List<double[]> branchCosts;

    public void setDataset(ScRNAseqDataset dataset) {
        this.dataset = dataset;
    }

    public void setUncondensedTree(TangleSearchTree tree) {
        this.uncondensedTree = tree;
    }

    public void setCondensedTree(TangleSearchTree condensedTree) {
        this.condensedTree = condensedTree;
    }

    public void setBranchCosts(List<double[]> branchCosts) {
        this.branchCosts = branchCosts;
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

    public List<double[]> getBranchCosts() {
        return branchCosts;
    }
}

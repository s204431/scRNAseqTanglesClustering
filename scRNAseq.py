import scanpy as sc
import anndata as ad
import pandas as pd
import matplotlib.pyplot as plt
import sys
import json

input_str = sys.stdin.readline().strip()

#df = pd.read_csv("data/symsim_observed_counts_5000genes_1000cells_complex.csv", index_col=0)
df = pd.read_csv(input_str, index_col=0)
adata = ad.AnnData(df)

#sc.pp.filter_cells(adata, min_genes=100)
#sc.pp.filter_genes(adata, min_cells=3)

# Saving count data
adata.layers["counts"] = adata.X.copy()
# Normalizing to median total counts
sc.pp.normalize_total(adata)
# Logarithmize the data
sc.pp.log1p(adata)

sc.pp.highly_variable_genes(adata, n_top_genes=2000)

sc.tl.pca(adata)

sc.pp.neighbors(adata)

#sc.tl.umap(adata)

# Using the igraph implementation and a fixed number of iterations can be significantly faster, especially for larger datasets
sc.tl.leiden(adata, flavor="igraph", n_iterations=2)

#sc.pl.umap(adata, color=["leiden"])

clusters = adata.obs["leiden"]

print(json.dumps(clusters.tolist()))
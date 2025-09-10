import scanpy as sc
import anndata as ad
import pandas as pd
import matplotlib.pyplot as plt
import sys
import json

input_str = sys.stdin.readline().strip()

#adata = sc.read("data/Ear_TSP1_30_version2d_10X_smartseq_scvi_Nov122024.h5ad", index_col=0)

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

sc.tl.pca(adata, n_comps=50)

sc.pp.neighbors(adata)

#sc.tl.umap(adata)

# Using the igraph implementation and a fixed number of iterations can be significantly faster, especially for larger datasets
sc.tl.leiden(adata, flavor="igraph", n_iterations=2)

#sc.pl.umap(adata, color=["leiden"])

clusters = adata.obs["leiden"]

print(json.dumps(clusters.tolist()))




"""
datapath = 'Ear_TSP1_30_version2d_10X_smartseq_scvi_Nov122024.h5ad'

adata  = sc.read(datapath)

labels = adata.obs['cell_ontology_class']

labels = labels.reset_index(drop=True)

labels_table = labels.value_counts()

labels, uniques = pd.factorize(labels)
labels = pd.Series(labels)
labels.to_csv("Ear_labels.csv")


data = adata.to_df("log_normalized")
data = data.reset_index(drop=True)
data.to_csv("Ear_data.csv")
"""
import scanpy as sc
import anndata as ad
import pandas as pd
import matplotlib.pyplot as plt
import sys
import json
import time

input_str = sys.stdin.readline().strip()

#adata = sc.read("data/Ear_TSP1_30_version2d_10X_smartseq_scvi_Nov122024.h5ad", index_col=0)

if input_str.endswith(".h5ad"):
    adata = sc.read(input_str, index_col=0)
else:
    df = pd.read_csv(input_str, index_col=0)
    adata = ad.AnnData(df)

start_time = time.time()

#sc.pp.filter_cells(adata, min_genes=100)
sc.pp.filter_genes(adata, min_cells=3)

#sc.pp.scrublet(adata)

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

end_time = time.time()
clusters = adata.obs["leiden"]

print(json.dumps(clusters.tolist()))
print(end_time - start_time)




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



"""
import numpy as np
x1 = np.array([0.8896370991641621, 0.9659436091851916, 0.9670394502415844, 0.9686015653398797, 0.9725962806052968, 0.977682491695656, 0.9777470905555343, 0.9785456759404099, 0.9786707312910273, 0.9815611036830836, 0.9819060821735162])
x2 = np.array([0.8589212247941465, 0.8956795003081647, 0.9650138392739418, 0.9799213011054277, 0.9809314020374679, 0.9816259857752607, 0.9826110762540996, 0.9835151469706538, 0.9877700673001243])
x3 = np.array([0.6013449196896277, 0.7164517042497263, 0.7537096451585213, 0.7674473096432579, 0.7700403714571369, 0.7927173413227905, 0.8845029127944166, 0.9523030548478836, 0.9886184220931385, 0.9896683042566508, 0.9910671470391069])
x4 = np.array([0.9035384014632643, 0.9203966531385948, 0.9319204916472543, 0.953560487838827, 0.9690792753293984, 0.9756596336869654, 0.9790308274695747, 0.9834137019917286])
x5 = np.array([0.9145458128567416, 0.9534817921003222, 0.9620217681989827, 0.9705205909883012, 0.9827985469300925, 0.9831523361916845, 0.9858948907624799, 0.9862438941613382, 0.9870864577054774, 0.9871637369184214, 0.9886766262984812, 0.9890787180697934, 0.9895358741267719])
x6 = np.array([0.46630155427175374, 0.5577830636677893, 0.6112889451822134, 0.6363629716645194, 0.640123609954623, 0.8361285190994071, 0.929602682997415, 0.9473893122620383, 0.963848848816442, 0.9720405053051464, 0.9737377738616058, 0.9766965559441156, 0.9805317915089513])
x7 = np.array([0.8446392128740667, 0.8737302951245757, 0.9670232638234889, 0.9703949440762751, 0.9713455317519686, 0.9749649874457482, 0.9754283878801872, 0.9761511458223423, 0.9808341745971182, 0.9877855432316589])
x8 = np.array([0.6955838461972494, 0.7693580005290641, 0.9748135191043893, 0.978674514871075, 0.9789339624951923, 0.9791114918433591, 0.9815804046210372, 0.9833321843180647, 0.9840238646914307, 0.9883933136530592, 0.9886852114603881, 0.989307618548548, 0.9908461520147105])
x9 = np.array([0.6351238711747479, 0.6831991954275749, 0.7159224954572581, 0.7198064205137795, 0.7577267639735827, 0.7746200278616737, 0.8219164062310571, 0.830844406584739, 0.8331135400321272, 0.8831407997942835, 0.894958896615376, 0.8964434502910308, 0.9099290141070198])

for x in [x1, x2, x3, x4, x5, x6, x7, x8, x9]:
    #x = (x - np.min(x))/(np.max(x) - np.min(x))
    #y = x[1:] - x[:-1]
    y = [np.mean(x[max(0,(i-3)):(i+1)]) for i in range(len(x))]
    z = x[1:] - y[:-1]
    #m = np.max(y)
    #for i in range(len(x)):
    #    y[len(y)-i-1] = np.sum(y[:(len(y)-i)][y[:(len(y)-i)]>(m/3)])
        
    #y = 1-y
    plt.plot(x)
    plt.show()
    plt.plot(y)
    plt.show()
    plt.plot(z)
    plt.show()

"""

import scanpy as sc
import anndata as ad
import pandas as pd
import matplotlib.pyplot as plt
import sys
import json
import time
import numpy as np
from sklearn.metrics import silhouette_score, davies_bouldin_score
from kneed import KneeLocator


RUN_FROM_JAVA = False

while not RUN_FROM_JAVA or int(sys.stdin.readline().strip()) == 1:
    
    if RUN_FROM_JAVA: input_str = sys.stdin.readline().strip()
    else: input_str = "testSet/symsim_observed_counts_5000genes_500cells_complex2.csv"
    
    if RUN_FROM_JAVA: use_tuning = int(sys.stdin.readline().strip()) == 1
    else: use_tuning = True
    
    print(f"[PY] Clustering dataset: {input_str}", file=sys.stderr, flush=True)
    
    use_pca = True
    
    #adata = sc.read("data/Kidney_observed_counts.h5ad", index_col=0)
    
    
    if input_str.endswith(".h5ad"):
        adata = sc.read(input_str, index_col=0)
    else:
        df = pd.read_csv(input_str, index_col=0)
        adata = ad.AnnData(df)
        
    
    # Shuffle
    perm = np.random.permutation(adata.n_obs)
    adata.uns["_perm"] = perm
    adata._inplace_subset_obs(perm)

    
        
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
    
    SEED = np.random.randint(0, 100000)
    
    n_pcs = 100 if use_pca else 3
    if use_pca: sc.tl.pca(adata, n_comps=n_pcs)
    else: sc.tl.tsne(adata, n_pcs=n_pcs, random_state=SEED)
    
    
    def choose_pcs_by_elbow(vr):
        """
        Elbow: max-distance-to-line between (1, vr[0]) and (N, vr[-1]) on the 
        per-PC variance curve.
        """
        y = vr
        x = np.arange(1, len(y)+1)
    
        # line from first to last
        p1 = np.array([x[0], y[0]])
        p2 = np.array([x[-1], y[-1]])
        v = p2 - p1
        v_norm = v / np.linalg.norm(v)
    
        # distance from each point to the line
        distances = []
        for i in range(len(x)):
            p = np.array([x[i], y[i]])
            # projection length on v
            proj_len = np.dot(p - p1, v_norm)
            proj = p1 + proj_len * v_norm
            distances.append(np.linalg.norm(p - proj))
    
        k = int(np.argmax(distances)) + 1  # +1 because PCs are 1-based conceptually
        return k
    
    def choose_pcs_by_kneedle(vr):
        x = np.arange(1, len(vr) + 1)
        kneedle = KneeLocator(x, vr, curve='convex', direction='decreasing')
        k = kneedle.knee
        if k is None:
            k = len(vr)
        return k
    
    if use_pca:
        # Per-PC explained variance
        vr = adata.uns['pca']['variance_ratio']
        #n_elbow = choose_pcs_by_elbow(vr)
        n_elbow = choose_pcs_by_kneedle(vr)
    
    
    """
    # Quick visualization of "elbow"
    plt.figure(figsize=(5,3))
    plt.plot(np.arange(1, len(vr)+1), vr, marker='o')
    plt.axvline(n_elbow, ls='--', color='gray')
    plt.xlabel("PC")
    plt.ylabel("Explained variance ratio")
    plt.title("PCA elbow")
    plt.tight_layout()
    plt.show()
    """
    
    
    SEED = np.random.randint(0, 100000)
    
    #sc.tl.umap(adata)
    
    silTime = 0
    best_score = -1
    best_neighbors = -1
    best_resolution = -1
    clusters = -1
    
    use_rep = "X_pca" if use_pca else "X_tsne"
    X_k = adata.obsm[use_rep][:, :n_pcs]
    if use_tuning: #Tune parameters
        for n_neighbors in np.arange(5, 55, 5):
            
            if use_pca:
                sc.pp.neighbors(adata, n_neighbors=n_neighbors, n_pcs=n_elbow, use_rep=use_rep, random_state=SEED)
            else:
                sc.pp.neighbors(adata, n_neighbors=n_neighbors, use_rep=use_rep, random_state=SEED)
            
            for resolution in np.arange(0.1, 3.1, 0.1):
                # Using the igraph implementation and a fixed number of iterations can be significantly faster, especially for larger datasets
                sc.tl.leiden(adata, flavor="igraph", n_iterations=-1, resolution=resolution, random_state=SEED)
                if len(np.unique(adata.obs["leiden"])) >= 2:
                    silStart = time.time()
                    silhouette = silhouette_score(X_k, adata.obs["leiden"])
                    silTime += time.time() - silStart
                    
                    #davies_bouldin = davies_bouldin_score(X_k, adata.obs["leiden"])
                    #print(silhouette)
                    if silhouette > best_score:
                        best_score = silhouette
                        best_neighbors = n_neighbors
                        best_resolution = resolution
                        clusters = adata.obs["leiden"].copy()
    
    else: #Use default parameters
    
        if use_pca:
            sc.pp.neighbors(adata, n_pcs=n_elbow, use_rep=use_rep, random_state=SEED)
        else:
            sc.pp.neighbors(adata, use_rep=use_rep, random_state=SEED)
            
        sc.tl.leiden(adata, flavor="igraph", n_iterations=-1, resolution=1, random_state=SEED)
        clusters = adata.obs["leiden"].copy()
    
        
    #sc.pl.umap(adata, color=["leiden"])
    
    # Unshuffle
    perm = adata.uns["_perm"]
    inv_perm = np.argsort(perm)
    clusters = clusters.iloc[inv_perm].reset_index(drop=True)



    
    end_time = time.time()
    #clusters = adata.obs["leiden"]
    print(json.dumps(clusters.tolist()))
    print(end_time - start_time)
    print("Silhouette time: ", silTime)
    #print(best_neighbors)
    #print(best_resolution)
    #print(best_score)
    sys.stdout.flush()



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
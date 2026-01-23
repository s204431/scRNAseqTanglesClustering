import scanpy as sc
import anndata as ad
import pandas as pd
import sys
import json
import time
import numpy as np
from sklearn.metrics import silhouette_score, davies_bouldin_score
from kneed import KneeLocator


RUN_FROM_JAVA = True

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
    else: sc.tl.tsne(adata, n_pcs=n_pcs, use_rep='X', random_state=SEED)
    
    
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
    #print("Silhouette time: ", silTime)
    #print(best_neighbors)
    #print(best_resolution)
    #print(best_score)
    sys.stdout.flush()
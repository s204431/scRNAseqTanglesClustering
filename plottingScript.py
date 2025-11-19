# %% Imports & data
import re
import io
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

SAVE_FIGURES = True

plt.rcParams.update({
    "font.size": 10,
    "axes.titlesize": 10,
    "axes.labelsize": 10,
    "xtick.labelsize": 9,
    "ytick.labelsize": 9,
    "legend.fontsize": 9
})

# size helpers: ~3.5" wide per panel so two subfigures fit side-by-side
PANEL_WIDTH = 4   # inches per panel (horizontally)
FIG_HEIGHT  = 4   # inches tall (compact but readable)
SAVE_DPI    = 400   # higher DPI for sharpness in LaTeX



df = pd.read_csv("experiments/test_results_cuts_pca.csv")

df = df.rename(columns=lambda c: re.sub(r'^RandIndex', 'ARI', c))
df = df.replace("TSNE", "t-SNE", regex=True)

for i in range(len(df)):
    df.loc[i, "Config Name"] = df.loc[i, "Config Name"][2:]
    
for i in range(len(df)):
    df.loc[i, "Config Name"] = df.loc[i, "Config Name"] + " only" if df.loc[i, "Config Name"] == "t-SNE" else df.loc[i, "Config Name"]
    df.loc[i, "Config Name"] = df.loc[i, "Config Name"] + " only" if df.loc[i, "Config Name"] == "PCA" else df.loc[i, "Config Name"]
    

"""
# Manually set some values
df.loc[np.arange(48,54), "Genes"] = 5000
df.loc[np.arange(48,54), "Cells"] = 902
df.loc[np.arange(48,54), "Depth_Mean"] = 1000
df.loc[np.arange(48,54), "Complexity"] = "simple"

df.loc[np.arange(54,60), "Genes"] = 5000
df.loc[np.arange(54,60), "Cells"] = 297
df.loc[np.arange(54,60), "Depth_Mean"] = 1000
df.loc[np.arange(54,60), "Complexity"] = "simple"
"""

# Number of runs (assumed constant)
runs = int(df["Runs"].iloc[0])

# Identifier columns to keep when melting
id_cols = [
    "Test Name","Config Name","Genes","Cells","Depth_Mean",
    "Balanced","Complexity","Sparsity","Runs"
]

# All repeated metric columns (Time_k, NMI_k, ARI_k)
value_cols = [c for c in df.columns if re.match(r'^(Time|NMI|ARI|Clusters)_\d+$', c)]

# Some handy lists
test_names = sorted(df["Test Name"].unique().tolist())
metrics = ["NMI", "ARI", "Time", "Clusters"]
metrics_to_plot = ["ARI"]

# Filter by test names
#names_to_keep = [test_names[-1]]
#print(test_names[-1])
#df = df[df["Test Name"].isin(names_to_keep)].copy()

# Long/tidy format
long = df.melt(
    id_vars=id_cols,
    value_vars=value_cols,
    var_name="MetricRun",
    value_name="Value"
)

# Split Metric/Run
long[["Metric","Run"]] = long["MetricRun"].str.extract(r'^(Time|NMI|ARI|Clusters)_(\d+)$')
long["Run"] = long["Run"].astype(int)
long = long.drop(columns="MetricRun")

# Normalize Balanced to boolean for robust filtering
long = long.copy()
long["Balanced_bool"] = (
    long["Balanced"].astype(str).str.strip().str.lower().map({"true": True, "false": False})
)

# Consistent config order across *all* panels
#configs_order = sorted(long["Config Name"].unique().tolist())
configs_order = long["Config Name"].unique().tolist()

# Color map per metric (defined once and reused)
colors = plt.cm.Set2(np.linspace(0, 1, len(metrics_to_plot)))
metric_color = {m: colors[i] for i, m in enumerate(metrics_to_plot)}


# %% Helper
def grouped_boxplot(ax, df_sub, title,
                    metrics_to_plot=metrics_to_plot,
                    metric_color=metric_color,
                    configs_order=configs_order,
                    fix_01=True,
                    show_means=True):
    """
    Draw grouped boxplots: x = config groups; side-by-side boxes = metrics.
    Returns (ymin, ymax) of the plotted data for later harmonization.
    """
    if df_sub.empty:
        ax.set_title(title + " (no data)")
        ax.axis("off")
        return (np.inf, -np.inf)

    # Keep only configs that actually appear here, but preserve the global order
    present_configs = [c for c in configs_order if c in df_sub["Config Name"].unique()]
    if not present_configs:
        ax.set_title(title + " (no data)")
        ax.axis("off")
        return (np.inf, -np.inf)

    group_width = 0.8
    box_width   = group_width / max(len(metrics_to_plot), 1)

    positions, data, pos_groups = [], [], []  # (cfg, metric, xpos)
    for i, cfg in enumerate(present_configs):
        
        # Filter by config name
        #if not cfg[0:2] == "3_": continue
    
        center = i + 1
        start  = center - group_width/2 + box_width/2
        for j, metric in enumerate(metrics_to_plot):
            vals = df_sub.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna().tolist()
            if len(vals):
                positions.append(start + j*box_width)
                data.append(vals)
                pos_groups.append((cfg, metric, positions[-1]))

    if not data:
        ax.set_title(title + " (no data)")
        ax.axis("off")
        return (np.inf, -np.inf)

    bplot = ax.boxplot(
        data,
        positions=positions,
        widths=box_width*0.9,
        patch_artist=True
    )

    # Color boxes by metric
    for idx, (_, metric, _) in enumerate(pos_groups):
        bplot["boxes"][idx].set(facecolor=metric_color[metric])

    # X axis
    ax.set_xticks(np.arange(len(present_configs)) + 1)
    ax.set_xticklabels(present_configs, rotation=30, ha="right")
    ax.set_xlabel("Config")
    ax.set_title(title)
    ax.grid(True, linestyle="--", alpha=0.5)

    # Optional mean markers
    if show_means:
        for (cfg, metric, xpos), vals in zip(pos_groups, data):
            ax.plot(xpos, np.mean(vals), marker="o", markersize=4, linestyle="None", alpha=0.9)

    # Clamp y-axis
    if metric == "Time":
        ax.set_ylim(bottom=0)
    elif metric != "Clusters":
        ax.set_ylim(0.5, 1)

    ymin = min(min(v) for v in data)
    ymax = max(max(v) for v in data)
    return (ymin, ymax)



def barplot_per_dataset(df_long, metrics_to_plot=["NMI", "ARI"], configs_order=None, colors=None):
    """
    Create one grouped bar plot per dataset (Test Name), showing all configs as bars
    for each metric in metrics_to_plot, and print mean scores per config.
    """
    if configs_order is None:
        configs_order = df_long["Config Name"].unique().tolist()
    if colors is None:
        colors = ["#1f77b4", "#ff7f0e"]  # default colors for metrics

    datasets = df_long["Test Name"].unique()

    # Compute overall best config for each metric
    for metric in metrics_to_plot:
        overall_means = []
        for cfg in configs_order:
            vals = df_long.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna()
            overall_means.append(vals.mean() if len(vals) > 0 else np.nan)
        max_idx = np.nanargmax(overall_means)
        best_config = configs_order[max_idx]
        print(f"Overall highest average {metric} = {overall_means[max_idx]:.4f} → {best_config}")

    counter = 0
    for dataset in datasets:
        counter += 1
        
        df_sub = df_long.query("`Test Name` == @dataset and Metric in @metrics_to_plot")
        if df_sub.empty:
            print(f"{dataset} (no data)")
            continue

        present_configs = [c for c in configs_order if c in df_sub["Config Name"].unique()]
        x = np.arange(len(present_configs))  # positions for configs
        width = 0.35  # width of each bar

        # Print mean score per config for this dataset
        print(f"\nDataset: {dataset}")
        for cfg in present_configs:
            scores = []
            for metric in metrics_to_plot:
                vals = df_sub.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna()
                scores.append(vals.mean() if len(vals) > 0 else np.nan)
            score_str = ", ".join([f"{metric}={score:.4f}" for metric, score in zip(metrics_to_plot, scores)])
            print(f"  {cfg}: {score_str}")

        # Plot grouped bars
        fig, ax = plt.subplots(figsize=(len(present_configs)*0.8 + 2, 4))
        for i, metric in enumerate(metrics_to_plot):
            means = []
            for cfg in present_configs:
                vals = df_sub.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna()
                means.append(vals.mean() if len(vals) > 0 else np.nan)
            ax.bar(x + i*width, means, width, color=colors[i], label=metric, zorder=3)

        ax.set_axisbelow(True)
        ax.grid(True, linestyle="--", alpha=0.5)
        ax.set_ylabel("Score")
        ax.set_xticks(x + width*(len(metrics_to_plot)-1)/2)  # center ticks
        ax.set_xticklabels(present_configs, rotation=30, ha="right")
        ax.set_xlabel("Config")
        ax.set_ylim(0,1)
        ax.set_title(f"Quality - Test {counter}")
        ax.legend(title="Metric")
        plt.tight_layout()
        #plt.savefig(f"plots/Test{counter}.png", dpi=300, bbox_inches="tight")
        plt.show()


def boxplot_per_dataset(df_long, metrics_to_plot=["NMI", "ARI"], configs_order=None, metric_color=None):
    """
    Create one grouped box plot per dataset (Test Name), showing all configs as boxes
    for each metric in metrics_to_plot, print mean scores per config, and display legend.
    """
    if configs_order is None:
        configs_order = df_long["Config Name"].unique().tolist()
    if metric_color is None:
        metric_color = {"NMI": "#1f77b4", "ARI": "#ff7f0e"}  # default colors

    datasets = df_long["Test Name"].unique()

    # Compute overall best config for each metric
    for metric in metrics_to_plot:
        overall_means = []
        for cfg in configs_order:
            vals = df_long.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna()
            overall_means.append(vals.mean() if len(vals) > 0 else np.nan)
        max_idx = np.nanargmax(overall_means)
        best_config = configs_order[max_idx]
        print(f"Overall highest average {metric} = {overall_means[max_idx]:.4f} → {best_config}")

    counter = 0
    for dataset in datasets:
        counter += 1
        df_sub = df_long.query("`Test Name` == @dataset and Metric in @metrics_to_plot")
        if df_sub.empty:
            print(f"{dataset} (no data)")
            continue

        present_configs = [c for c in configs_order if c in df_sub["Config Name"].unique()]
        group_width = 0.8
        box_width = group_width / max(len(metrics_to_plot), 1)

        # Print mean scores
        print(f"\nDataset: {dataset}")
        for cfg in present_configs:
            scores = []
            for metric in metrics_to_plot:
                vals = df_sub.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna()
                scores.append(vals.mean() if len(vals) > 0 else np.nan)
            score_str = ", ".join([f"{metric}={score:.4f}" for metric, score in zip(metrics_to_plot, scores)])
            print(f"  {cfg}: {score_str}")

        # Prepare data for grouped boxplot
        positions, data, labels = [], [], []
        for i, cfg in enumerate(present_configs):
            center = i + 1
            start = center - group_width/2 + box_width/2
            for j, metric in enumerate(metrics_to_plot):
                vals = df_sub.query("`Config Name` == @cfg and Metric == @metric")["Value"].dropna().tolist()
                if vals:
                    positions.append(start + j*box_width)
                    data.append(vals)
                    labels.append((cfg, metric))

        # Plot
        fig, ax = plt.subplots(figsize=(len(present_configs)*0.8 + 2, 4))
        bplot = ax.boxplot(data, positions=positions, widths=box_width*0.9, patch_artist=True)

        # Color boxes by metric
        for idx, (_, metric) in enumerate(labels):
            bplot["boxes"][idx].set(facecolor=metric_color[metric])

        # Optional: mark means
        for (cfg, metric), vals, pos in zip(labels, data, positions):
            ax.plot(pos, np.mean(vals), marker="o", markersize=4, linestyle="None", alpha=0.9)

        # X-axis
        ax.set_xticks(np.arange(len(present_configs)) + 1)
        ax.set_xticklabels(present_configs, rotation=30, ha="right")
        ax.set_xlabel("Config")
        ax.set_ylabel("Score")
        ax.set_title(f"Quality - Test {counter}")
        ax.set_ylim(0, 1)
        ax.grid(True, linestyle="--", alpha=0.5)

        # Legend for metrics
        handles = [mpatches.Patch(color=metric_color[m], label=m) for m in metrics_to_plot]
        ax.legend(handles=handles, title="Metric")

        plt.tight_layout()
        plt.savefig(f"plots/Test{counter}.png", dpi=300, bbox_inches="tight")
        plt.show()


def per_run_means(df_sub):
    # Average across tests for each (Config, Metric, Run)
    return (df_sub
            .groupby(["Config Name", "Metric", "Run"], as_index=False)
            .agg(Value=("Value", "mean")))

# %% Sparsity panels (LOW vs HIGH) — per-run means
LOW_SPARSITY_MAX = 0.5

subset_low  = long.query("Metric in @metrics_to_plot and Sparsity <= @LOW_SPARSITY_MAX")
subset_high = long.query("Metric in @metrics_to_plot and Sparsity >  @LOW_SPARSITY_MAX")

subset_low_agg  = per_run_means(subset_low)
subset_high_agg = per_run_means(subset_high)

fig, axes = plt.subplots(1, 2, figsize=(PANEL_WIDTH*2, FIG_HEIGHT), sharey=True)

grouped_boxplot(axes[0], subset_low_agg,  f"LOW sparsity ≤ {LOW_SPARSITY_MAX}",
                metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)
grouped_boxplot(axes[1], subset_high_agg, f"HIGH sparsity > {LOW_SPARSITY_MAX}",
                metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

y_string = metrics_to_plot[0] if metrics_to_plot[0] != "Time" else "Seconds"
axes[0].set_ylabel(y_string)
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
#axes[0].legend(title="Metric", loc="best")

fig.suptitle("Quality - Sparsity (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Sparsity.png", dpi=300, bbox_inches="tight")
plt.show()


# %% Cell count panels — per-run means
#cells_to_plot = sorted(df['Cells'].unique().tolist())
cells_to_plot = [2000]

subsets_cells = [(c, per_run_means(long.query("Metric in @metrics_to_plot and Cells == @c")))
                 for c in cells_to_plot]

n = len(subsets_cells)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (cells, subdf) in zip(axes, subsets_cells):
    grouped_boxplot(ax, subdf, f"{metrics_to_plot[0]} - Cells = {cells}",
                    metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

y_string = metrics_to_plot[0] 
if metrics_to_plot[0] == "Time":
    y_string = "Seconds"
    
axes[0].set_ylabel(y_string)
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
#axes[0].legend(title="Metric", loc="best")

#fig.suptitle("Quality - Cell Counts (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Cell_Count.png", dpi=300, bbox_inches="tight")
plt.show()


# %% Complexity panels — per-run means
complexities_to_plot = ["complex"]#["simple", "complex", "unknown"]

subsets_comp = [(comp, per_run_means(long.query("Metric in @metrics_to_plot and Complexity == @comp")))
                for comp in complexities_to_plot]

n = len(subsets_comp)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (comp, subdf) in zip(axes, subsets_comp):
    grouped_boxplot(ax, subdf, f"{metrics_to_plot[0]} - Structure = {comp}",
                    metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

y_string = metrics_to_plot[0] if metrics_to_plot[0] != "Time" else "Seconds"
axes[0].set_ylabel(y_string)
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
#axes[0].legend(title="Metric", loc="best")

#fig.suptitle("Quality - Structure (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Complexity.png", dpi=300, bbox_inches="tight")
plt.show()


# %% Balanced panels — per-run means
panels_balanced = [(True,  "Balanced = True"),
                   (False, "Balanced = False")]

subsets_bal = [(label, per_run_means(long.query("Metric in @metrics_to_plot and Balanced_bool == @val")))
               for val, label in panels_balanced]

n = len(subsets_bal)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (label, subdf) in zip(axes, subsets_bal):
    grouped_boxplot(ax, subdf, label,
                    metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

y_string = metrics_to_plot[0] if metrics_to_plot[0] != "Time" else "Seconds"
axes[0].set_ylabel(y_string)
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
#axes[0].legend(title="Metric", loc="best")

#fig.suptitle("Quality - Balanced vs Unbalanced (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Balanced.png", dpi=300, bbox_inches="tight")
plt.show()

# %% All tests: box = variation across runs (mean over tests)

# 1) Keep only the metrics we want
subset_all = long.query("Metric in @metrics_to_plot")

# 2) For each (Config, Metric, Run), average across *all tests*
agg_runs = (
    subset_all
    .groupby(["Config Name", "Metric", "Run"], as_index=False)
    .agg(Value=("Value", "mean"))
)

# 3) Single plot: boxes per Config, side-by-side for NMI & RandIndex
fig, ax = plt.subplots(figsize=(PANEL_WIDTH, FIG_HEIGHT))

# Reuse your color map & ordering (defined earlier)
_ = grouped_boxplot(
    ax,
    agg_runs,
    title=metrics_to_plot[0] + " - all tests (per-run means)",
    metrics_to_plot=metrics_to_plot,
    metric_color=metric_color,
    configs_order=configs_order,
    fix_01=True,        # keep [0,1] for NMI/RandIndex
    show_means=True     # mean dot of per-run means
)

y_string = metrics_to_plot[0] if metrics_to_plot[0] != "Time" else "Seconds"
ax.set_ylabel(y_string)
# Legend
for m in metrics_to_plot:
    ax.plot([], [], color=metric_color[m], label=m)
#ax.legend(title="Metric", loc="best")

plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_AllTests.png", dpi=300, bbox_inches="tight")
plt.show()


#%% Histograms
#barplot_per_dataset(long, configs_order=configs_order)
#boxplot_per_dataset(long, configs_order=configs_order)

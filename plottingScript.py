# %% Imports & data
import re
import io
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

SAVE_FIGURES = False

plt.rcParams.update({
    "font.size": 10,
    "axes.titlesize": 10,
    "axes.labelsize": 10,
    "xtick.labelsize": 9,
    "ytick.labelsize": 9,
    "legend.fontsize": 9
})

# size helpers: ~3.5" wide per panel so two subfigures fit side-by-side
PANEL_WIDTH = 3.5   # inches per panel (horizontally)
FIG_HEIGHT  = 3.2   # inches tall (compact but readable)
SAVE_DPI    = 400   # higher DPI for sharpness in LaTeX



df = pd.read_csv("test_results.csv")

# Number of runs (assumed constant)
runs = int(df["Runs"].iloc[0])

# Identifier columns to keep when melting
id_cols = [
    "Test Name","Config Name","Genes","Cells","Depth_Mean",
    "Balanced","Complexity","Sparsity","Runs"
]

# All repeated metric columns (Time_k, NMI_k, RandIndex_k)
value_cols = [c for c in df.columns if re.match(r'^(Time|NMI|RandIndex)_\d+$', c)]

# Long/tidy format
long = df.melt(
    id_vars=id_cols,
    value_vars=value_cols,
    var_name="MetricRun",
    value_name="Value"
)

# Split Metric/Run
long[["Metric","Run"]] = long["MetricRun"].str.extract(r'^(Time|NMI|RandIndex)_(\d+)$')
long["Run"] = long["Run"].astype(int)
long = long.drop(columns="MetricRun")

# Normalize Balanced to boolean for robust filtering
long = long.copy()
long["Balanced_bool"] = (
    long["Balanced"].astype(str).str.strip().str.lower().map({"true": True, "false": False})
)

# Some handy lists
test_names = sorted(df["Test Name"].unique().tolist())
metrics = ["NMI", "RandIndex", "Time"]
metrics_to_plot = ["NMI", "RandIndex"]

# Consistent config order across *all* panels
configs_order = sorted(long["Config Name"].unique().tolist())

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
    ax.set_xticklabels(present_configs)
    ax.set_xlabel("Config")
    ax.set_title(title)

    # Optional mean markers
    if show_means:
        for (cfg, metric, xpos), vals in zip(pos_groups, data):
            ax.plot(xpos, np.mean(vals), marker="o", markersize=4, linestyle="None", alpha=0.9)

    # Optional: clamp to [0,1] for NMI/RandIndex
    #if fix_01 and set(metrics_to_plot).issubset({"NMI", "RandIndex"}):
    #    ax.set_ylim(0, 1)

    ymin = min(min(v) for v in data)
    ymax = max(max(v) for v in data)
    return (ymin, ymax)

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

axes[0].set_ylabel("Score")
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Quality - Sparsity (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Sparsity.png", dpi=300, bbox_inches="tight")
plt.show()


# %% Cell count panels — per-run means
cells_to_plot = sorted(df['Cells'].unique().tolist())

subsets_cells = [(c, per_run_means(long.query("Metric in @metrics_to_plot and Cells == @c")))
                 for c in cells_to_plot]

n = len(subsets_cells)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (cells, subdf) in zip(axes, subsets_cells):
    grouped_boxplot(ax, subdf, f"Cells = {cells}",
                    metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

axes[0].set_ylabel("Score")
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Quality - Cell Counts (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Cell_Count.png", dpi=300, bbox_inches="tight")
plt.show()


# %% Complexity panels — per-run means
complexities_to_plot = ["simple", "complex"]

subsets_comp = [(comp, per_run_means(long.query("Metric in @metrics_to_plot and Complexity == @comp")))
                for comp in complexities_to_plot]

n = len(subsets_comp)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (comp, subdf) in zip(axes, subsets_comp):
    grouped_boxplot(ax, subdf, f"Complexity = {comp}",
                    metrics_to_plot, metric_color, configs_order, fix_01=True, show_means=True)

axes[0].set_ylabel("Score")
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Quality - Complexity (per-run means)", fontsize=12)
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

axes[0].set_ylabel("Score")
for m in metrics_to_plot:
    axes[0].plot([], [], color=metric_color[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Quality - Balanced vs Unbalanced (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_Balanced.png", dpi=300, bbox_inches="tight")
plt.show()

# %% All tests: box = variation across runs (mean over tests)

metrics_to_plot = ["NMI", "RandIndex"]

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
    title="Quality - all tests (per-run means)",
    metrics_to_plot=metrics_to_plot,
    metric_color=metric_color,
    configs_order=configs_order,
    fix_01=True,        # keep [0,1] for NMI/RandIndex
    show_means=True     # mean dot of per-run means
)

ax.set_ylabel("Score")
# Legend
for m in metrics_to_plot:
    ax.plot([], [], color=metric_color[m], label=m)
ax.legend(title="Metric", loc="best")

plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Quality_AllTests.png", dpi=300, bbox_inches="tight")
plt.show()




#%% TIME PLOTS
# ==== Running time figures (per-run means across tests) ====

# One metric for these panels
metrics_to_plot_time = ["Time"]

# Color map for time (single metric still needs a color)
colors_time = plt.cm.Set2(np.linspace(0, 1, len(metrics_to_plot_time)))
metric_color_time = {m: colors_time[i] for i, m in enumerate(metrics_to_plot_time)}

# Optional: log-scale for time if there is a wide range
USE_LOG_TIME = False

# Ensure plots/ exists when saving
if SAVE_FIGURES:
    import os
    os.makedirs("plots", exist_ok=True)

# ---------- Sparsity (LOW vs HIGH) ----------
subset_low_t  = long.query("Metric in @metrics_to_plot_time and Sparsity <= @LOW_SPARSITY_MAX")
subset_high_t = long.query("Metric in @metrics_to_plot_time and Sparsity >  @LOW_SPARSITY_MAX")

subset_low_t_agg  = per_run_means(subset_low_t)
subset_high_t_agg = per_run_means(subset_high_t)

fig, axes = plt.subplots(1, 2, figsize=(PANEL_WIDTH*2, FIG_HEIGHT), sharey=True)

grouped_boxplot(axes[0], subset_low_t_agg,  f"LOW sparsity ≤ {LOW_SPARSITY_MAX}",
                metrics_to_plot_time, metric_color_time, configs_order,
                fix_01=False, show_means=True)
grouped_boxplot(axes[1], subset_high_t_agg, f"HIGH sparsity > {LOW_SPARSITY_MAX}",
                metrics_to_plot_time, metric_color_time, configs_order,
                fix_01=False, show_means=True)

if USE_LOG_TIME:
    axes[0].set_yscale("log")
    axes[1].set_yscale("log")

axes[0].set_ylabel("Time (s)")
axes[0].set_ylim(bottom=0)
# Legend (single metric)
for m in metrics_to_plot_time:
    axes[0].plot([], [], color=metric_color_time[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Runtime - Sparsity (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Time_Sparsity.png", dpi=300, bbox_inches="tight")
plt.show()


# ---------- Cells ----------
cells_to_plot = sorted(df['Cells'].unique().tolist())
subsets_cells_t = [(c, per_run_means(long.query("Metric in @metrics_to_plot_time and Cells == @c")))
                   for c in cells_to_plot]

n = len(subsets_cells_t)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (cells, subdf) in zip(axes, subsets_cells_t):
    grouped_boxplot(ax, subdf, f"Cells = {cells}",
                    metrics_to_plot_time, metric_color_time, configs_order,
                    fix_01=False, show_means=True)

if USE_LOG_TIME:
    for ax in axes:
        ax.set_yscale("log")

axes[0].set_ylabel("Time (s)")
axes[0].set_ylim(bottom=0)
for m in metrics_to_plot_time:
    axes[0].plot([], [], color=metric_color_time[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Runtime - Cell Counts (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Time_Cells.png", dpi=300, bbox_inches="tight")
plt.show()


# ---------- Complexity ----------
complexities_to_plot = ["simple", "complex"]
subsets_comp_t = [(comp, per_run_means(long.query("Metric in @metrics_to_plot_time and Complexity == @comp")))
                  for comp in complexities_to_plot]

n = len(subsets_comp_t)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (comp, subdf) in zip(axes, subsets_comp_t):
    grouped_boxplot(ax, subdf, f"Complexity = {comp}",
                    metrics_to_plot_time, metric_color_time, configs_order,
                    fix_01=False, show_means=True)

if USE_LOG_TIME:
    for ax in axes:
        ax.set_yscale("log")

axes[0].set_ylabel("Time (s)")
axes[0].set_ylim(bottom=0)
for m in metrics_to_plot_time:
    axes[0].plot([], [], color=metric_color_time[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Runtime - Complexity (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Time_Complexity.png", dpi=300, bbox_inches="tight")
plt.show()


# ---------- Balanced ----------
panels_balanced = [(True,  "Balanced = True"),
                   (False, "Balanced = False")]
subsets_bal_t = [(label, per_run_means(long.query("Metric in @metrics_to_plot_time and Balanced_bool == @val")))
                 for val, label in panels_balanced]

n = len(subsets_bal_t)
fig, axes = plt.subplots(1, n, figsize=(PANEL_WIDTH*n, FIG_HEIGHT), sharey=True)
if n == 1:
    axes = [axes]

for ax, (label, subdf) in zip(axes, subsets_bal_t):
    grouped_boxplot(ax, subdf, label,
                    metrics_to_plot_time, metric_color_time, configs_order,
                    fix_01=False, show_means=True)

if USE_LOG_TIME:
    for ax in axes:
        ax.set_yscale("log")

axes[0].set_ylabel("Time (s)")
axes[0].set_ylim(bottom=0)
for m in metrics_to_plot_time:
    axes[0].plot([], [], color=metric_color_time[m], label=m)
axes[0].legend(title="Metric", loc="best")

fig.suptitle("Runtime - Balanced vs Unbalanced (per-run means)", fontsize=12)
plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Time_Balanced.png", dpi=300, bbox_inches="tight")
plt.show()


# ---------- All tests summary: per-run means across tests ----------
subset_all_t = long.query("Metric in @metrics_to_plot_time")
agg_runs_t = (subset_all_t
              .groupby(["Config Name", "Metric", "Run"], as_index=False)
              .agg(Value=("Value", "mean")))

fig, ax = plt.subplots(figsize=(PANEL_WIDTH, FIG_HEIGHT))
grouped_boxplot(ax, agg_runs_t,
                title="Runtime - all tests (per-run means)",
                metrics_to_plot=metrics_to_plot_time,
                metric_color=metric_color_time,
                configs_order=configs_order,
                fix_01=False,
                show_means=True)

if USE_LOG_TIME:
    ax.set_yscale("log")

ax.set_ylabel("Time (s)")
ax.set_ylim(bottom=0)
for m in metrics_to_plot_time:
    ax.plot([], [], color=metric_color_time[m], label=m)
ax.legend(title="Metric", loc="best")

plt.tight_layout()
if SAVE_FIGURES: plt.savefig("plots/Time_AllTests.png", dpi=300, bbox_inches="tight")
plt.show()

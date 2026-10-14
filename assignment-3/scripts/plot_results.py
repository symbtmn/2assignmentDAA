import csv
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
PLOTS = ROOT / "results" / "plots"
PLOTS.mkdir(parents=True, exist_ok=True)
with (ROOT / "results" / "tables" / "summary.csv").open() as stream:
    rows = list(csv.DictReader(stream))

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10, "svg.hashsalt": "smart-city-daa"})
palette = ["#2563eb", "#ea580c", "#059669", "#9333ea", "#0891b2", "#be123c", "#475569"]
groups = {}
for row in rows:
    label = row["algorithm"]
    if row["graph_type"] != "Undirected":
        label += " (" + row["graph_type"] + ")"
    groups.setdefault(label, []).append(row)

def save(fig, name):
    fig.savefig(PLOTS / (name + ".png"), dpi=180, bbox_inches="tight")
    fig.savefig(PLOTS / (name + ".svg"), bbox_inches="tight", metadata={"Date": None})
    plt.close(fig)

for field, name, axis_label in [("V", "time_vs_vertices", "Number of vertices (V)"),
                                 ("E", "time_vs_edges", "Number of logical edges (E)")]:
    fig, ax = plt.subplots(figsize=(10, 6))
    for (label, values), color in zip(groups.items(), palette):
        values.sort(key=lambda item: int(item[field]))
        x = [int(item[field]) for item in values]
        y = [float(item["mean_ns"]) / 1000 for item in values]
        ax.plot(x, y, "o-", label=label, color=color, linewidth=2, markersize=5)
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xticks(sorted({int(row[field]) for row in rows}))
    ax.set_xticklabels([str(value) for value in sorted({int(row[field]) for row in rows})])
    ax.set_xlabel(axis_label)
    ax.set_ylabel("Mean execution time (microseconds, log scale)")
    ax.set_title("Smart city graph algorithms: five measured runs", loc="left", fontweight="bold")
    ax.grid(True, which="both", alpha=0.2)
    ax.legend(loc="upper left", fontsize=9, framealpha=0.95)
    fig.text(0.12, 0.01, "Fixed seed 20261014; 200 warmups per case. V and E grow together; points are observed means.", fontsize=9)
    save(fig, name)

fig, axes = plt.subplots(1, 2, figsize=(11, 5))
for algorithm, color in [("Kruskal", palette[0]), ("Prim", palette[1])]:
    values = [row for row in rows if row["algorithm"] == algorithm]
    x = [int(row["V"]) for row in values]
    axes[0].plot(x, [float(row["comparisons"]) for row in values], "o-", label=algorithm, color=color, linewidth=2)
    axes[1].plot(x, [float(row["edge_examinations"]) for row in values], "o-", label=algorithm, color=color, linewidth=2)
for ax, title, ylabel in zip(axes, ["Sort / heap comparator calls", "Algorithm edge examinations"],
                            ["Comparator invocations", "Edges / adjacency arcs examined"]):
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xticks([10, 100, 1000], labels=["10", "100", "1000"])
    ax.set_xlabel("Vertices (V)")
    ax.set_ylabel(ylabel)
    ax.set_title(title, loc="left", fontweight="bold")
    ax.grid(True, which="both", alpha=0.2)
    ax.legend()
fig.suptitle("MST operation counts", fontsize=14, fontweight="bold")
fig.tight_layout()
save(fig, "mst_operations")

with (ROOT / "results" / "tables" / "dsu_summary.csv").open() as stream:
    dsu = list(csv.DictReader(stream))
fig, ax = plt.subplots(figsize=(9, 5))
x = np.arange(3)
for index, variant in enumerate(["Neither", "Compression", "Rank", "Both"]):
    values = [float(row["parent_hops"]) for row in dsu if row["variant"] == variant]
    ax.bar(x + (index - 1.5) * 0.18, values, 0.18, label=variant, color=palette[index])
ax.set_xticks(x, ["10", "100", "1,000"])
ax.set_yscale("log")
ax.set_xlabel("DSU elements (V)")
ax.set_ylabel("Parent links traversed (log scale)")
ax.set_title("DSU: adversarial chain unions followed by V finds of vertex 0", loc="left", fontweight="bold", fontsize=11)
ax.legend(title="Enabled optimizations")
ax.grid(True, axis="y", which="both", alpha=0.2)
save(fig, "dsu_parent_hops")
print("Created four PNG plots and four SVG plots in results/plots")

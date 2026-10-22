import csv
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / "results" / "tables"

def read_csv(name):
    with (TABLES / name).open() as stream:
        return list(csv.DictReader(stream))

def table(headers, values):
    return "\n".join(["| " + " | ".join(headers) + " |", "| " + " | ".join(["---"] * len(headers)) + " |"]
                     + ["| " + " | ".join(str(value) for value in row) + " |" for row in values])

def label(row):
    return row["algorithm"] + ("" if row["graph_type"] == "Undirected" else " (" + row["graph_type"] + ")")

summary = read_csv("summary.csv")
raw = read_csv("raw_runs.csv")
dsu = read_csv("dsu_summary.csv")
lookup = {(row["algorithm"], row["graph_type"], int(row["V"])): row for row in summary}
timing_groups = {}
for row in raw:
    key = (row["algorithm"], row["graph_type"], int(row["V"]))
    timing_groups.setdefault(key, []).append(int(row["time_ns"]))

report = """# Assignment 3: Smart City Infrastructure and Route Planning

## Overview

This project uses graphs to solve three city planning problems: connecting districts at the lowest cost, checking directed connections, and finding shortest routes. A vertex represents a district. An edge represents a road or an infrastructure connection.

The graph is stored as an adjacency list, with a separate list of edges for Kruskal. District IDs start at 0. An undirected road is stored in both districts' adjacency lists, but it is counted as one edge in E. Directed roads are stored in one direction. The graph takes O(V + E) space.

Road costs and travel distances are kept in separate graphs because the cheapest network is not necessarily the network with the shortest routes. The demo has six districts: Central, University, Hospital, Airport, Industrial, and Suburb.

## Implementation

| Folder | Contents |
| --- | --- |
| `src/graph/` | Graph, edges, file input, graph generation, and counters |
| `src/dsu/` | Disjoint Set Union |
| `src/mst/` | Kruskal and Prim |
| `src/connectivity/` | Topological ordering and Kosaraju SCC |
| `src/shortestpath/` | Dijkstra |
| `src/app/` | Demo and commands for custom graphs |
| `src/benchmark/` | Experiments and CSV output |
| `tests/` | Correctness tests |
| `results/` | Tables and plots |

### DSU, Kruskal, and Prim

DSU stores a parent and a rank for each vertex. `find` follows parent links to the root, then uses path compression to point the visited vertices directly to that root. `union` joins two roots using rank. The root with the smaller rank goes under the larger one. If the ranks are equal, the remaining root's rank increases by one.

Kruskal sorts edges by weight. For each edge, DSU checks whether its endpoints are already connected. If they are not, the edge is added to the result. The algorithm stops after V - 1 accepted edges or when there are no more edges. This avoids cycles while choosing the cheapest connections.

Prim uses Java's `PriorityQueue`. It starts at an unvisited vertex, adds its outgoing edges to the queue, and chooses the cheapest edge leading to an unvisited district. Some queued edges become unnecessary after their destination has been visited, so they are skipped. This is the lazy version of Prim, and its queue can hold O(E) edges.

Both return the selected edges, total weight, and number of components. If the graph is disconnected, they return a minimum spanning forest and report that a complete network is impossible. A single vertex is connected and has MST weight 0. Equal edge weights may produce different MST edge sets, so the important comparison is their total weight. Negative weights are allowed for MSTs. Both algorithms use the cut property: the cheapest edge across a cut can be included in an MST.

### Topological ordering and SCC

Topological ordering uses Kahn's algorithm. It counts incoming edges and puts vertices with indegree 0 into a queue. Removing a vertex reduces the indegrees of its neighbours. If all V vertices are removed, the result is a valid order. Otherwise, the graph contains a cycle. The program returns an empty order for a cyclic graph.

Kosaraju finds strongly connected components in two DFS passes. The first records finishing order. The second uses reversed edges and processes vertices in reverse finishing order. Each traversal in the second pass gives one SCC. A vertex is marked when it enters the stack, so it is not added twice. DFS uses an explicit stack to handle long chains without recursive stack overflow.

### Dijkstra

Dijkstra starts with distance 0 for the chosen source and infinity for other vertices. It takes the smallest distance from a priority queue and tries to improve the distances of neighbouring districts. Each improvement also stores a predecessor, which is used to reconstruct the path. Old queue entries are skipped when their distance no longer matches the current result.

Unreachable districts are printed as `unreachable` with an empty path. Zero weights are allowed. Distances and MST totals use `long`, so they can be larger than the range of `int`.

Dijkstra rejects negative weights. Its greedy rule depends on the fact that extending a path cannot make it shorter. For example, suppose `S -> A = 2`, `S -> B = 5`, and `B -> A = -10`. Standard Dijkstra would settle A at 2 too early, even though the route through B costs -5. Negative edges break this guarantee. Bellman-Ford can be used instead when negative edges are needed. A reachable negative cycle means some shortest distances are not finite.

### Running the project

The Java code needs Java 17 or newer and no external Java libraries. Run these commands from the repository root on macOS, Linux, or Git Bash:

```bash
cd assignment-3
bash build.sh
java -cp out tests.CorrectnessTests
java -cp out app.Main demo
```

Commands for the example files:

```bash
java -cp out app.Main mst examples/roads.txt
java -cp out app.Main connectivity examples/dependencies.txt
java -cp out app.Main connectivity examples/one-way-roads.txt
java -cp out app.Main routes examples/roads.txt 0
```

The last argument in `routes` selects the source district. Custom files start with `V E directed` or `V E undirected`, followed by E lines of `from to weight`. For example:

```text
4 4 directed
0 1 2
0 2 5
1 3 3
2 3 1
```

Python is only needed to recreate the plots and refresh the report. To repeat the full experiment:

```bash
python3 -m pip install -r requirements.txt
bash reproduce.sh
```

On Windows PowerShell, after entering `assignment-3`, use:

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
java -cp out tests.CorrectnessTests
java -cp out app.Main demo
python -m pip install -r requirements.txt
powershell -ExecutionPolicy Bypass -File reproduce.ps1
```

The reproduction scripts compile the code, run tests, save the demo, run benchmarks, and update the plots and both README copies. To run only the benchmarks, use `java -cp out benchmark.Benchmark`.

## Complexity Analysis

Here V is the number of vertices and E is the number of logical edges. These bounds assume adjacency lists, comparison sorting, and a binary heap. Extra space includes the returned result but excludes the input graph.

| Algorithm | Time | Extra space |
| --- | --- | --- |
| DSU initialization | O(V) | O(V) |
| DSU find | O(alpha(V)) amortized with both optimizations | O(1) per call, besides DSU arrays |
| DSU union | Two finds and O(1) extra work; O(alpha(V)) amortized | O(1) per call, besides DSU arrays |
| Kruskal | O(V + E log(2 + E) + E alpha(V)) | O(V + E) |
| Prim, lazy heap | O(V + E log(2 + E)) | O(V + E) |
| Kosaraju | O(V + E) | O(V + E), including reversed graph |
| Topological ordering | O(V + E) | O(V) |
| Dijkstra, lazy heap | O(V + E log(2 + E)) | O(V + E) |

`alpha(V)` is the inverse Ackermann function, which grows very slowly. Without optimizations, DSU can form a chain and one find can take O(V). Rank keeps tree height O(log V). Compression makes later finds shorter. With both, m operations take O(V + m alpha(V)) including initialization. The amortized cost describes a sequence of operations; an individual find may still take O(log V).

Kruskal sorts all E edges, even if it later stops early. Prim scans adjacency lists and may push and pop O(E) candidates. Increasing E therefore increases sorting or heap work. Increasing V also increases the size of arrays and the number of districts to visit.

Kosaraju and topological ordering do linear work in V + E. More vertices mean more visits, and more edges mean more connections to check. A cycle can stop Kahn's second pass early, but its worst-case bound stays O(V + E).

Dijkstra checks all E weights first, then scans edges from reachable vertices. Each successful relaxation inserts a new state, so this implementation can have O(E) heap entries. On simple graphs, log E is O(log V), giving the usual heap-based bounds: O(E log V) for connected Prim and O((V + E) log V) for Dijkstra. `log(2 + E)` in the table also covers edgeless graphs.

Reconstructing a path takes time proportional to its length. Printing paths to every vertex can take O(V squared) in a long chain. This output work is not included in the benchmarks.

## Experimental Setup

| Graph | V | E |
| --- | --- | --- |
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1,000 | 5,000 |

The base seed is **20261014**, using `java.util.Random`. For each size, the undirected seed is `base + V`, the DAG seed is `base + V + 1`, and the cyclic seed is `base + V + 2`. Weights are random integers from 1 to 100. Generators avoid self-loops and duplicate endpoint pairs, but different edges can have equal weights.

The undirected generator first connects each vertex i to a random earlier vertex. This creates a spanning tree. Random extra edges are then added until there are exactly E edges. Kruskal, Prim, and Dijkstra use the same connected graph. Dijkstra starts at vertex 0.

The DAG starts with the chain `0 -> 1 -> ... -> V - 1`. Extra edges always go from a lower ID to a higher ID, so no cycle is possible.

The cyclic graph uses blocks of five vertices. Each block contains a directed ring. Extra edges stay inside a block or go to a later block. This gives exactly V / 5 SCCs of size five. Every vertex has an incoming ring edge, so Kahn's queue starts empty. This setup tests guaranteed cycles, rather than assuming that random edges will create them.

Each graph is created before timing. Each algorithm and graph size has 200 warmup calls, followed by **five measured runs**. A run is timed using `System.nanoTime()` immediately before and after the algorithm call. The measured loop rotates its starting case. Algorithm arrays, queues, and results are created again for each run; the graph is reused without changes.

The timer includes operation counting, but excludes graph generation, CSV writing, path printing, and plotting. Results are consumed through a volatile field after timing. The average is the sum of the five times divided by five. Sample standard deviation uses a divisor of four. Raw results are kept, and no outliers are removed. The same seeds reproduce graphs and operation counts, but execution times will change between runs and computers.

The environment used for the saved results was:

```text
__ENVIRONMENT__
```

## Results

### Average execution time

Times below are in microseconds: 1 microsecond = 1,000 nanoseconds. Each value is the average of five runs.

__MEAN_TABLE__

Some runs took much longer than others. The next table shows the sample standard deviation, median, minimum, and maximum from the same five runs.

<details>
<summary>Timing variation for all cases</summary>

__VARIATION_TABLE__

</details>

### Kruskal and Prim

__MST_TABLE__

For Kruskal, an edge examination means checking a sorted candidate, and comparisons are sorting comparator calls. For Prim, examinations count adjacency entries, including both directions of an undirected road; comparisons are heap comparator calls. They measure different parts of the algorithms, so they should be compared separately.

### SCC and topological ordering

__CONNECTIVITY_TABLE__

Kosaraju checks 3E edges: one pass in the original graph, one while reversing edges, and one in the reversed graph. Its two traversals give 2V vertex visits. Kahn checks 2E edges on these DAGs. On the cyclic inputs it only does the E indegree checks, because no vertex can enter the queue. DAGs have V SCCs, while the cyclic graphs have V / 5.

### Dijkstra

__DIJKSTRA_TABLE__

All vertices are reachable in these benchmark graphs. Edge examinations include E weight checks and 2E adjacency entries, giving 3E. A relaxation is counted only when a distance improves. There is one initial queue entry plus one per successful relaxation. All entries are eventually removed, so pushes and pops are equal.

### DSU optimizations

The DSU experiment performs `union(i, i - 1)` for i from 1 to V - 1, then calls `find(0)` V times. Without rank, this creates a chain. Each version has V - 1 union calls and 3V - 2 find calls, including finds inside unions. Each version is measured five times after 200 warmups.

__DSU_TABLE__

Parent hops count traversed parent links, including the second pass used for compression. Parent writes count actual changes, excluding initialization. Rank alone can therefore show fewer hops than both optimizations when the tree is already shallow.

### Plots

![Execution time versus vertices](__PREFIX__results/plots/time_vs_vertices.svg)

![Execution time versus edges](__PREFIX__results/plots/time_vs_edges.svg)

Both axes use logarithmic scales. V and E grow together in the required configurations, so these plots do not isolate the effect of changing only one of them. The lines join measured averages; they are not fitted complexity curves.

![MST operation counts](__PREFIX__results/plots/mst_operations.svg)

![DSU parent hops](__PREFIX__results/plots/dsu_parent_hops.svg)

The data is available in [raw runs](__PREFIX__results/tables/raw_runs.csv), [summary](__PREFIX__results/tables/summary.csv), [DSU raw runs](__PREFIX__results/tables/dsu_raw_runs.csv), and [DSU summary](__PREFIX__results/tables/dsu_summary.csv). PNG copies of the plots are also included.

## Comparison and Discussion

__DISCUSSION__

The timing results are not perfectly smooth. For example, medium Kruskal has a slower mean than large Kruskal because one of its five runs was unusually slow. The large DAG SCC result also has a wide range. JVM compilation, memory allocation, garbage collection, and other processes can affect short measurements. The timings alone do not identify which of these caused a delay.

Operation counts are more consistent with the expected growth. Kosaraju scans edges a fixed number of times, while Kruskal, Prim, and Dijkstra also do sorting or heap work. Five runs are enough for this assignment, but more runs and several seeds would give a stronger timing comparison.

Kruskal and Prim solve the same task, but their work is different. Kruskal sorts once; lazy Prim keeps processing queued edges, including ones it later skips. This helps explain their comparison counts on these sparse graphs. It does not mean Kruskal is always faster. A different graph density or heap implementation could change the result.

The cyclic topological tests finish quickly because every vertex is blocked by a ring. A graph with an acyclic section and a small cycle would let Kahn process some vertices first. Both cases still have O(V + E) worst-case complexity.

Rank prevents deep DSU trees, and compression shortens paths that have already been searched. Rank alone works especially well for this union sequence because it builds a shallow tree. Both optimizations are used in Kruskal because their combined amortized bound applies to general sequences of unions and finds. Tiny DSU timings are sensitive to warmup and measurement order, so parent hops give a clearer comparison here.

## Testing

The test runner uses checks that throw `AssertionError`; Java's `-ea` option is not needed. The saved result is:

```text
__TEST_RESULT__
```

| Test | What is checked |
| --- | --- |
| Small graphs | Known MST weight 6, known shortest distances, and a known path |
| Disconnected graphs | Correct forest weight and component count; isolated vertices |
| Cycles | Cyclic graphs and self-loops rejected by topological ordering; SCC membership |
| Multiple MSTs | Equal-weight graph has optimal weight 3, regardless of selected edge set |
| Unreachable vertices | Infinite distance, empty path, and a disconnected source |
| Duplicate weights and parallel edges | MST and shortest-path results remain correct |
| Single vertex | MST weight 0, order [0], one SCC, distance 0, path [0] |
| DSU | Repeated unions, component counts, root agreement, and actual path compression |
| Input and numeric limits | Invalid IDs and graph types, negative route weights, file parsing, and totals above `int` range |
| Required graph sizes | Exact V/E, reproducible edges, connectivity, valid DAG order, known cyclic SCCs |
| 100 random undirected graphs | MST weight checked by enumerating spanning trees; distances checked with Floyd-Warshall |
| 100 random directed graphs | SCCs and cycles checked with transitive closure; shortest paths checked from every source |
| 20,000-vertex chain | Iterative SCC, topological ordering, and end-to-end distance |

Every selected forest is checked for valid edges, total weight, no cycles, component count, and V - components edges. Kruskal and Prim must agree on the weight of connected graphs. A topological order must contain every vertex once and place every edge's source before its destination.

SCC checks verify that each vertex occurs exactly once. They also check that two vertices share a component exactly when they can reach each other. Dijkstra distances are compared with Floyd-Warshall, and each reported path is checked by adding its edge weights. Negative route edges are rejected even if they are unreachable from the source.

The demo gives construction cost **130** with both MST algorithms. Travel distances from Central are 0 to Central, 3 to University, 2 to Hospital, 8 to Airport, and 10 to Industrial. Suburb is unreachable. The service DAG gives `[0, 5, 1, 2, 3, 4]`. Adding `4 -> 2` creates the SCC `{2, 3, 4}` and prevents a topological order.

Full outputs are in [tests.txt](__PREFIX__results/tables/tests.txt) and [demo.txt](__PREFIX__results/tables/demo.txt).

## Conclusion

Kruskal and Prim are suitable for choosing low-cost infrastructure connections. Kahn's algorithm is useful for ordering services with dependencies, and Kosaraju shows which districts are mutually reachable through directed roads. Dijkstra gives shortest routes when edge weights are non-negative.

The tests confirm the expected results and cover disconnected graphs, cycles, equal weights, unreachable vertices, and a single vertex. The operation counts follow the expected complexity trends. The timing results are more variable, so they should be used together with the counts rather than as a reason to choose one algorithm for every situation.
"""

algorithms = [("Kruskal", "Undirected"), ("Prim", "Undirected"), ("Kosaraju", "DAG"),
              ("Kosaraju", "Cyclic"), ("Topological", "DAG"), ("Topological", "Cyclic"), ("Dijkstra", "Undirected")]
mean_rows = []
for algorithm, kind in algorithms:
    name = algorithm + ("" if kind == "Undirected" else " (" + kind + ")")
    mean_rows.append([name] + [f"{float(lookup[(algorithm, kind, v)]['mean_ns']) / 1000:.3f}" for v in (10, 100, 1000)])

variation_rows = []
for row in summary:
    key = (row["algorithm"], row["graph_type"], int(row["V"]))
    variation_rows.append([label(row), row["V"], f"{float(row['stddev_ns']) / 1000:.3f}",
                           f"{statistics.median(timing_groups[key]) / 1000:.3f}",
                           f"{int(row['min_ns']) / 1000:.3f}", f"{int(row['max_ns']) / 1000:.3f}"])

mst_rows = [[row["algorithm"], row["V"], row["E"], row["edge_examinations"], row["comparisons"],
             row["find_calls"], row["union_calls"], row["heap_pushes"], row["heap_pops"]]
            for row in summary if row["algorithm"] in ("Kruskal", "Prim")]
connectivity_rows = [[label(row), row["V"], row["E"], row["edge_examinations"], row["vertex_visits"],
                     (row["V"] if row["graph_type"] == "DAG" else str(int(row["V"]) // 5))
                     if row["algorithm"] == "Kosaraju" else ("Valid DAG order" if row["graph_type"] == "DAG" else "Cycle detected")]
                    for row in summary if row["algorithm"] in ("Kosaraju", "Topological")]
dijkstra_rows = [[row["V"], row["E"], row["edge_examinations"], row["comparisons"], row["relaxations"],
                  row["heap_pushes"], row["heap_pops"], row["vertex_visits"]]
                 for row in summary if row["algorithm"] == "Dijkstra"]
dsu_rows = [[row["variant"], row["V"], f"{float(row['mean_ns']) / 1000:.3f}",
             f"{float(row['stddev_ns']) / 1000:.3f}", row["parent_hops"], row["parent_writes"]] for row in dsu]

large_k = lookup[("Kruskal", "Undirected", 1000)]
large_p = lookup[("Prim", "Undirected", 1000)]
cyclic_scc = [lookup[("Kosaraju", "Cyclic", v)] for v in (10, 100, 1000)]
dijkstra_values = [lookup[("Dijkstra", "Undirected", v)] for v in (10, 100, 1000)]
discussion = f"""On the large graph, Kruskal took {float(large_k['mean_ns']) / 1000:.3f} microseconds on average and Prim took {float(large_p['mean_ns']) / 1000:.3f}. Kruskal made {large_k['comparisons']} sort comparisons and checked {large_k['edge_examinations']} candidates. Prim made {large_p['comparisons']} heap comparisons and scanned {large_p['edge_examinations']} adjacency entries. Both gave the same MST weight in the connected test cases.

For cyclic SCC graphs, the average times were {', '.join(f"{float(row['mean_ns']) / 1000:.3f}" for row in cyclic_scc)} microseconds from small to large. Edge checks increased from 90 to 1,500 to 15,000. This agrees with the O(V + E) analysis. DAG and cyclic SCC runs do the same number of edge checks, but DAGs return more separate component lists and their measured times had more variation.

Dijkstra's averages were {', '.join(f"{float(row['mean_ns']) / 1000:.3f}" for row in dijkstra_values)} microseconds. Successful relaxations increased from {dijkstra_values[0]['relaxations']} to {dijkstra_values[1]['relaxations']} to {dijkstra_values[2]['relaxations']}. Heap comparisons increased from {dijkstra_values[0]['comparisons']} to {dijkstra_values[1]['comparisons']} to {dijkstra_values[2]['comparisons']}. More roads mean more possible routes to check, although only improved distances create new queue entries.

At V = 1,000, DSU without optimizations made 999,000 parent hops. Compression only made 3,996, rank only made 1,997, and both made 3,994. Using both reduced hops by about {999000 / 3994:.1f} times compared with neither on this test. That is a reduction in counted work, not a claim that the program will always run that many times faster."""

replacements = {
    "__ENVIRONMENT__": (TABLES / "environment.txt").read_text().strip(),
    "__MEAN_TABLE__": table(["Algorithm / graph", "Small: V=10, E=30", "Medium: V=100, E=500", "Large: V=1,000, E=5,000"], mean_rows),
    "__VARIATION_TABLE__": table(["Algorithm / graph", "V", "SD (µs)", "Median (µs)", "Min (µs)", "Max (µs)"], variation_rows),
    "__MST_TABLE__": table(["Algorithm", "V", "E", "Edges examined", "Comparator calls", "Finds", "Unions", "Heap pushes", "Heap pops"], mst_rows),
    "__CONNECTIVITY_TABLE__": table(["Algorithm / graph", "V", "E", "Edges examined", "Vertex visits", "Outcome / SCC count"], connectivity_rows),
    "__DIJKSTRA_TABLE__": table(["V", "E", "Edges examined", "Heap comparisons", "Relaxations", "Heap pushes", "Heap pops", "Vertex visits"], dijkstra_rows),
    "__DSU_TABLE__": table(["Optimizations", "V", "Mean (µs)", "SD (µs)", "Parent hops", "Parent writes"], dsu_rows),
    "__TEST_RESULT__": (TABLES / "tests.txt").read_text().strip().splitlines()[-1],
    "__DISCUSSION__": discussion,
}
for marker, value in replacements.items():
    report = report.replace(marker, value)
(ROOT / "README.md").write_text(report.replace("__PREFIX__", ""))
(ROOT.parent / "README.md").write_text(report.replace("__PREFIX__", "assignment-3/"))
print("Generated root README.md and assignment-3/README.md from recorded measurements")

package tests;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import graph.GraphIO;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class CorrectnessTests {
    private static int checks;
    private static int suites;

    public static void main(String[] args) throws Exception {
        dsu();
        knownMst();
        disconnected();
        multipleMst();
        singleVertex();
        topology();
        scc();
        shortestPaths();
        rejection();
        inputFiles();
        generators();
        randomMst();
        randomDirected();
        deepGraph();
        System.out.println("PASS: " + suites + " suites, " + checks + " correctness checks");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void pass(String label) {
        suites++;
        System.out.println("PASS: " + label);
    }

    private static void dsu() {
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(8, metrics);
        check(dsu.union(0, 1), "First union must join sets");
        check(!dsu.union(1, 0), "Repeated union must return false");
        check(dsu.components() == 7, "DSU component count");
        dsu.union(2, 3);
        dsu.union(0, 2);
        long writes = metrics.parentWrites;
        check(dsu.find(3) == dsu.find(0), "Transitive connectivity");
        check(metrics.parentWrites > writes, "Path compression must flatten a depth-two path");
        for (int vertex = 4; vertex < 8; vertex++) {
            dsu.union(0, vertex);
        }
        check(dsu.components() == 1, "All DSU vertices joined");
        check(metrics.unionCalls == 8, "Union operation counter");
        pass("DSU union, rank, compression and counters");
    }

    private static Graph graph(int vertices, boolean directed, int[][] edges) {
        Graph graph = new Graph(vertices, directed);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1], edge[2]);
        }
        return graph;
    }

    private static void verifyMst(Graph graph, long expectedWeight, int components) {
        for (MstResult result : List.of(Kruskal.compute(graph, new Metrics()), Prim.compute(graph, new Metrics()))) {
            check(result.totalWeight() == expectedWeight, "Expected MST or forest weight " + expectedWeight);
            check(result.components() == components, "Expected forest components");
            check(result.connected() == (components == 1), "Connected status");
            check(result.edges().size() == graph.vertices() - components, "Forest edge count");
            DisjointSetUnion selected = new DisjointSetUnion(graph.vertices(), new Metrics());
            long sum = 0;
            for (Edge edge : result.edges()) {
                check(selected.union(edge.from(), edge.to()), "Selected forest contains no cycle");
                check(graph.edges().stream().anyMatch(original -> original.weight() == edge.weight()
                        && ((original.from() == edge.from() && original.to() == edge.to())
                        || (original.from() == edge.to() && original.to() == edge.from()))), "Selected edge exists");
                sum += edge.weight();
            }
            check(sum == expectedWeight, "Sum of selected edges");
            check(selected.components() == components, "Forest spans the expected components");
        }
    }

    private static void knownMst() {
        Graph graph = graph(4, false, new int[][] {{0, 1, 1}, {1, 2, 2}, {2, 3, 3}, {0, 3, 10}, {0, 2, 8}});
        verifyMst(graph, 6, 1);
        verifyMst(graph(3, false, new int[][] {{0, 1, -3}, {1, 2, 0}, {0, 2, 5}, {0, 0, -10}}), -3, 1);
        pass("Small MST with known weights; negative MST edges and self-loop");
    }

    private static void disconnected() {
        verifyMst(graph(5, false, new int[][] {{0, 1, 3}, {2, 3, 4}}), 7, 3);
        verifyMst(new Graph(4, false), 0, 4);
        pass("Disconnected graphs and isolated districts");
    }

    private static void multipleMst() {
        verifyMst(graph(4, false, new int[][] {{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {3, 0, 1}, {0, 2, 1}, {0, 1, 1}}), 3, 1);
        pass("Multiple valid MSTs, equal weights and parallel edges");
    }

    private static void singleVertex() {
        verifyMst(new Graph(1, false), 0, 1);
        Graph single = new Graph(1, true);
        verifyTopological(single);
        verifyScc(single);
        Dijkstra.Result result = Dijkstra.compute(single, 0, new Metrics());
        check(result.distanceTo(0) == 0 && result.pathTo(0).equals(List.of(0)), "Singleton path");
        single.addEdge(0, 0, 0);
        check(!TopologicalSort.compute(single, new Metrics()).dag(), "Singleton self-loop is a cycle");
        verifyScc(single);
        pass("Single vertex with and without a self-loop");
    }

    private static void verifyTopological(Graph graph) {
        TopologicalSort.Result result = TopologicalSort.compute(graph, new Metrics());
        check(result.dag(), "DAG must have a topological ordering");
        check(result.order().size() == graph.vertices(), "Ordering contains all vertices");
        int[] positions = new int[graph.vertices()];
        Arrays.fill(positions, -1);
        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);
            check(positions[vertex] == -1, "Ordering has no duplicate vertices");
            positions[vertex] = index;
        }
        for (Edge edge : graph.edges()) {
            check(positions[edge.from()] < positions[edge.to()], "Every edge points forward in the ordering");
        }
    }

    private static void topology() {
        Graph dag = graph(6, true, new int[][] {{0, 2, 1}, {1, 2, 1}, {1, 3, 1}, {2, 4, 1}, {3, 4, 1}});
        verifyTopological(dag);
        verifyTopological(new Graph(4, true));
        dag.addEdge(4, 1, 1);
        TopologicalSort.Result cyclic = TopologicalSort.compute(dag, new Metrics());
        check(!cyclic.dag() && cyclic.order().isEmpty(), "Cycle cannot return a full or misleading partial ordering");
        pass("DAG ordering, multiple choices, isolated vertices and cycle detection");
    }

    private static boolean[][] reachability(Graph graph) {
        boolean[][] reach = new boolean[graph.vertices()][graph.vertices()];
        for (Edge edge : graph.edges()) {
            reach[edge.from()][edge.to()] = true;
        }
        for (int middle = 0; middle < graph.vertices(); middle++) {
            for (int from = 0; from < graph.vertices(); from++) {
                for (int to = 0; to < graph.vertices(); to++) {
                    reach[from][to] |= reach[from][middle] && reach[middle][to];
                }
            }
        }
        return reach;
    }

    private static void verifyScc(Graph graph) {
        List<List<Integer>> components = Kosaraju.compute(graph, new Metrics());
        int[] membership = new int[graph.vertices()];
        Arrays.fill(membership, -1);
        for (int index = 0; index < components.size(); index++) {
            check(!components.get(index).isEmpty(), "SCC is nonempty");
            for (int vertex : components.get(index)) {
                check(membership[vertex] == -1, "Vertex belongs to only one SCC");
                membership[vertex] = index;
            }
        }
        boolean[][] reach = reachability(graph);
        for (int first = 0; first < graph.vertices(); first++) {
            check(membership[first] >= 0, "Every vertex belongs to an SCC");
            for (int second = 0; second < graph.vertices(); second++) {
                boolean mutual = first == second || (reach[first][second] && reach[second][first]);
                check((membership[first] == membership[second]) == mutual, "SCCs equal mutual-reachability classes");
            }
        }
    }

    private static void scc() {
        Graph graph = graph(8, true, new int[][] {{0, 1, 1}, {1, 2, 1}, {2, 0, 1}, {2, 3, 1},
                {3, 4, 1}, {4, 3, 1}, {4, 5, 1}, {6, 6, 1}});
        verifyScc(graph);
        check(Kosaraju.compute(graph, new Metrics()).size() == 5, "Five known SCCs");
        pass("Known SCC partition, cycles, self-loop and disconnected vertices");
    }

    private static void verifyDistances(Graph graph, int source) {
        int vertices = graph.vertices();
        long infinity = Long.MAX_VALUE / 4;
        long[][] distances = new long[vertices][vertices];
        for (int first = 0; first < vertices; first++) {
            Arrays.fill(distances[first], infinity);
            distances[first][first] = 0;
            for (Edge edge : graph.neighbors(first)) {
                distances[first][edge.to()] = Math.min(distances[first][edge.to()], edge.weight());
            }
        }
        for (int middle = 0; middle < vertices; middle++) {
            for (int from = 0; from < vertices; from++) {
                for (int to = 0; to < vertices; to++) {
                    distances[from][to] = Math.min(distances[from][to], distances[from][middle] + distances[middle][to]);
                }
            }
        }
        Dijkstra.Result result = Dijkstra.compute(graph, source, new Metrics());
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (distances[source][vertex] == infinity) {
                check(!result.reachable(vertex) && result.pathTo(vertex).isEmpty(), "Unreachable vertex");
                continue;
            }
            check(result.distanceTo(vertex) == distances[source][vertex], "Dijkstra matches Floyd-Warshall");
            List<Integer> path = result.pathTo(vertex);
            check(path.get(0) == source && path.get(path.size() - 1) == vertex, "Path endpoints");
            long total = 0;
            for (int index = 1; index < path.size(); index++) {
                int to = path.get(index);
                int weight = graph.neighbors(path.get(index - 1)).stream().filter(edge -> edge.to() == to)
                        .mapToInt(Edge::weight).min().orElseThrow();
                total += weight;
            }
            check(total == result.distanceTo(vertex), "Reported path weight equals shortest distance");
        }
    }

    private static void shortestPaths() {
        Graph graph = graph(6, true, new int[][] {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1},
                {2, 3, 8}, {3, 4, 0}, {0, 1, 4}, {4, 2, 0}});
        Dijkstra.Result result = Dijkstra.compute(graph, 0, new Metrics());
        check(result.distanceTo(1) == 3 && result.distanceTo(3) == 4 && result.distanceTo(4) == 4, "Known shortest distances");
        check(result.pathTo(4).equals(List.of(0, 2, 1, 3, 4)), "Known shortest path");
        verifyDistances(graph, 0);
        verifyDistances(graph, 5);
        Graph large = graph(3, true, new int[][] {{0, 1, Integer.MAX_VALUE}, {1, 2, Integer.MAX_VALUE}});
        check(Dijkstra.compute(large, 0, new Metrics()).distanceTo(2) == 4294967294L, "Distances use long arithmetic");
        verifyMst(graph(3, false, new int[][] {{0, 1, Integer.MAX_VALUE}, {1, 2, Integer.MAX_VALUE}}), 4294967294L, 1);
        pass("Shortest distances, paths, zero weights, unreachable vertices and long totals");
    }

    private static void expectRejected(Runnable action) {
        boolean rejected = false;
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        check(rejected, "Invalid input must be rejected");
    }

    private static void rejection() {
        expectRejected(() -> new Graph(0, false));
        expectRejected(() -> new Graph(2, false).addEdge(0, 2, 1));
        expectRejected(() -> Kruskal.compute(new Graph(2, true), new Metrics()));
        expectRejected(() -> Prim.compute(new Graph(2, true), new Metrics()));
        expectRejected(() -> Kosaraju.compute(new Graph(2, false), new Metrics()));
        expectRejected(() -> TopologicalSort.compute(new Graph(2, false), new Metrics()));
        expectRejected(() -> Dijkstra.compute(new Graph(2, true), 2, new Metrics()));
        Graph negative = graph(4, true, new int[][] {{0, 1, 1}, {2, 3, -1}});
        expectRejected(() -> Dijkstra.compute(negative, 0, new Metrics()));
        pass("Invalid vertices, graph types and negative edges, including unreachable negative edges");
    }

    private static void inputFiles() throws Exception {
        Path path = Files.createTempFile("smart-city-test", ".txt");
        try {
            Files.writeString(path, "3 2 undirected\n0 1 2\n1 2 3\n");
            verifyMst(GraphIO.read(path), 5, 1);
            Files.writeString(path, "2 1 directed\n0 1 0\n");
            verifyTopological(GraphIO.read(path));
        } finally {
            Files.deleteIfExists(path);
        }
        pass("Custom graph file parsing");
    }

    private static void generators() {
        expectRejected(() -> GraphGenerator.connectedUndirected(4, 2, 1));
        expectRejected(() -> GraphGenerator.dag(4, 7, 1));
        expectRejected(() -> GraphGenerator.cyclic(10, 66, 1));
        for (int[] size : new int[][] {{10, 30}, {100, 500}, {1000, 5000}}) {
            Graph first = GraphGenerator.connectedUndirected(size[0], size[1], 20261014L);
            Graph same = GraphGenerator.connectedUndirected(size[0], size[1], 20261014L);
            check(first.edges().equals(same.edges()), "Fixed seed reproduces edges and weights");
            check(first.edges().size() == size[1] && first.vertices() == size[0], "Exact required graph size");
            MstResult kruskal = Kruskal.compute(first, new Metrics());
            MstResult prim = Prim.compute(first, new Metrics());
            check(kruskal.connected() && prim.connected() && kruskal.totalWeight() == prim.totalWeight(), "Generated connected MST agreement");
            Graph dag = GraphGenerator.dag(size[0], size[1], 20261015L);
            verifyTopological(dag);
            check(Kosaraju.compute(dag, new Metrics()).size() == size[0], "DAG singleton SCCs");
            Graph cyclic = GraphGenerator.cyclic(size[0], size[1], 20261016L);
            check(cyclic.edges().size() == size[1], "Exact cyclic edge count");
            check(!TopologicalSort.compute(cyclic, new Metrics()).dag(), "Generated cyclic graph detected");
            List<List<Integer>> components = Kosaraju.compute(cyclic, new Metrics());
            check(components.size() == size[0] / 5, "Generated SCC count");
            check(components.stream().allMatch(component -> component.size() == 5), "Each generated SCC has five vertices");
        }
        pass("Reproducible generators at all three required sizes");
    }

    private static long exhaustiveMst(Graph graph) {
        long best = Long.MAX_VALUE;
        int edges = graph.edges().size();
        for (int mask = 0; mask < (1 << edges); mask++) {
            if (Integer.bitCount(mask) != graph.vertices() - 1) {
                continue;
            }
            DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices(), new Metrics());
            long total = 0;
            boolean tree = true;
            for (int index = 0; index < edges; index++) {
                if ((mask & (1 << index)) != 0) {
                    Edge edge = graph.edges().get(index);
                    tree &= dsu.union(edge.from(), edge.to());
                    total += edge.weight();
                }
            }
            if (tree && dsu.components() == 1) {
                best = Math.min(best, total);
            }
        }
        return best;
    }

    private static void randomMst() {
        Random random = new Random(73001);
        for (int round = 0; round < 100; round++) {
            int vertices = 2 + random.nextInt(5);
            Graph graph = new Graph(vertices, false);
            for (int vertex = 1; vertex < vertices; vertex++) {
                graph.addEdge(vertex, random.nextInt(vertex), random.nextInt(7));
            }
            for (int edge = 0; edge < 5; edge++) {
                graph.addEdge(random.nextInt(vertices), random.nextInt(vertices), random.nextInt(7));
            }
            verifyMst(graph, exhaustiveMst(graph), 1);
            verifyDistances(graph, random.nextInt(vertices));
        }
        pass("100 seeded MST cases checked by exhaustive spanning-tree enumeration");
    }

    private static void randomDirected() {
        Random random = new Random(73002);
        for (int round = 0; round < 100; round++) {
            int vertices = 1 + random.nextInt(7);
            Graph graph = new Graph(vertices, true);
            for (int edge = 0; edge < 12; edge++) {
                graph.addEdge(random.nextInt(vertices), random.nextInt(vertices), random.nextInt(8));
            }
            verifyScc(graph);
            boolean[][] reach = reachability(graph);
            boolean cycle = false;
            for (int vertex = 0; vertex < vertices; vertex++) {
                cycle |= reach[vertex][vertex];
            }
            TopologicalSort.Result result = TopologicalSort.compute(graph, new Metrics());
            check(result.dag() == !cycle, "Cycle result matches transitive-closure oracle");
            if (!cycle) {
                verifyTopological(graph);
            }
            for (int source = 0; source < vertices; source++) {
                verifyDistances(graph, source);
            }
        }
        pass("100 seeded directed cases checked by reachability and Floyd-Warshall");
    }

    private static void deepGraph() {
        Graph chain = new Graph(20000, true);
        for (int vertex = 1; vertex < chain.vertices(); vertex++) {
            chain.addEdge(vertex - 1, vertex, 1);
        }
        check(Kosaraju.compute(chain, new Metrics()).size() == chain.vertices(), "Iterative SCC handles a deep chain");
        check(TopologicalSort.compute(chain, new Metrics()).dag(), "Deep chain is a DAG");
        check(Dijkstra.compute(chain, 0, new Metrics()).distanceTo(chain.vertices() - 1) == 19999, "Deep-chain distance");
        pass("20,000-vertex chain without recursive stack overflow");
    }
}

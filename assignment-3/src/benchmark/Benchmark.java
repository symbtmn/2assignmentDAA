package benchmark;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.Graph;
import graph.GraphGenerator;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Benchmark {
    private static final long SEED = 20261014L;
    private static final int WARMUPS = 200;
    private static final int REPETITIONS = 5;
    private static volatile Object sink;

    private record Experiment(String algorithm, String kind, Graph graph, long seed) {
        Object run(Metrics metrics) {
            return switch (algorithm) {
                case "Kruskal" -> Kruskal.compute(graph, metrics);
                case "Prim" -> Prim.compute(graph, metrics);
                case "Kosaraju" -> Kosaraju.compute(graph, metrics);
                case "Topological" -> TopologicalSort.compute(graph, metrics);
                case "Dijkstra" -> Dijkstra.compute(graph, 0, metrics);
                default -> throw new IllegalArgumentException(algorithm);
            };
        }

        String label() {
            return algorithm + (kind.equals("Undirected") ? "" : "-" + kind);
        }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        Path tables = Path.of("results", "tables");
        Files.createDirectories(tables);
        List<Experiment> experiments = new ArrayList<>();
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            int vertices = size[0];
            int edges = size[1];
            long seed = SEED + vertices;
            Graph roads = GraphGenerator.connectedUndirected(vertices, edges, seed);
            Graph dag = GraphGenerator.dag(vertices, edges, seed + 1);
            Graph cyclic = GraphGenerator.cyclic(vertices, edges, seed + 2);
            validateGraphs(roads, dag, cyclic);
            experiments.add(new Experiment("Kruskal", "Undirected", roads, seed));
            experiments.add(new Experiment("Prim", "Undirected", roads, seed));
            experiments.add(new Experiment("Kosaraju", "DAG", dag, seed + 1));
            experiments.add(new Experiment("Kosaraju", "Cyclic", cyclic, seed + 2));
            experiments.add(new Experiment("Topological", "DAG", dag, seed + 1));
            experiments.add(new Experiment("Topological", "Cyclic", cyclic, seed + 2));
            experiments.add(new Experiment("Dijkstra", "Undirected", roads, seed));
        }
        for (Experiment experiment : experiments) {
            for (int warmup = 0; warmup < WARMUPS; warmup++) {
                sink = experiment.run(new Metrics());
            }
        }
        long[][] times = new long[experiments.size()][REPETITIONS];
        Metrics[] counts = new Metrics[experiments.size()];
        try (PrintWriter raw = new PrintWriter(Files.newBufferedWriter(tables.resolve("raw_runs.csv")))) {
            raw.println("algorithm,graph_type,V,E,seed,run,time_ns");
            for (int run = 0; run < REPETITIONS; run++) {
                for (int offset = 0; offset < experiments.size(); offset++) {
                    int index = (offset + run) % experiments.size();
                    Experiment experiment = experiments.get(index);
                    Metrics metrics = new Metrics();
                    long start = System.nanoTime();
                    Object result = experiment.run(metrics);
                    long elapsed = System.nanoTime() - start;
                    sink = result;
                    times[index][run] = elapsed;
                    if (counts[index] != null && !signature(counts[index]).equals(signature(metrics))) {
                        throw new IllegalStateException("Operation counts changed between identical runs");
                    }
                    counts[index] = metrics;
                    raw.printf("%s,%s,%d,%d,%d,%d,%d%n", experiment.algorithm(), experiment.kind(),
                            experiment.graph().vertices(), experiment.graph().edges().size(), experiment.seed(), run + 1, elapsed);
                }
            }
        }
        try (PrintWriter summary = new PrintWriter(Files.newBufferedWriter(tables.resolve("summary.csv")))) {
            summary.println("algorithm,graph_type,V,E,seed,mean_ns,stddev_ns,min_ns,max_ns,edge_examinations,comparisons,find_calls,union_calls,parent_hops,parent_writes,heap_pushes,heap_pops,relaxations,vertex_visits");
            for (int index = 0; index < experiments.size(); index++) {
                Experiment experiment = experiments.get(index);
                Metrics metrics = counts[index];
                long[] measured = times[index];
                double mean = mean(measured);
                summary.printf("%s,%s,%d,%d,%d,%.2f,%.2f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                        experiment.algorithm(), experiment.kind(), experiment.graph().vertices(),
                        experiment.graph().edges().size(), experiment.seed(), mean, deviation(measured),
                        minimum(measured), maximum(measured), metrics.edgeExaminations, metrics.comparisons,
                        metrics.findCalls, metrics.unionCalls, metrics.parentHops, metrics.parentWrites,
                        metrics.heapPushes, metrics.heapPops, metrics.relaxations, metrics.vertexVisits);
                System.out.printf("%-20s V=%4d E=%4d mean=%10.2f ns%n", experiment.label(),
                        experiment.graph().vertices(), experiment.graph().edges().size(), mean);
            }
        }
        benchmarkDsu(tables, sizes);
        try (PrintWriter environment = new PrintWriter(Files.newBufferedWriter(tables.resolve("environment.txt")))) {
            environment.println("Java: " + System.getProperty("java.runtime.version"));
            environment.println("VM: " + System.getProperty("java.vm.name"));
            environment.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            environment.println("Available processors: " + Runtime.getRuntime().availableProcessors());
            environment.println("Base seed: " + SEED);
            environment.println("Warmup invocations per case: " + WARMUPS);
            environment.println("Measured invocations per case: " + REPETITIONS);
            environment.println("Timer: System.nanoTime()");
        }
    }

    private static void validateGraphs(Graph roads, Graph dag, Graph cyclic) {
        MstResult first = Kruskal.compute(roads, new Metrics());
        MstResult second = Prim.compute(roads, new Metrics());
        if (!first.connected() || !second.connected() || first.totalWeight() != second.totalWeight()) {
            throw new IllegalStateException("Generated MST graph failed validation");
        }
        if (!TopologicalSort.compute(dag, new Metrics()).dag()
                || TopologicalSort.compute(cyclic, new Metrics()).dag()
                || Kosaraju.compute(cyclic, new Metrics()).size() != cyclic.vertices() / 5) {
            throw new IllegalStateException("Generated directed graph failed validation");
        }
    }

    private static void benchmarkDsu(Path tables, int[][] sizes) throws Exception {
        String[] variants = {"Neither", "Compression", "Rank", "Both"};
        try (PrintWriter raw = new PrintWriter(Files.newBufferedWriter(tables.resolve("dsu_raw_runs.csv")));
             PrintWriter summary = new PrintWriter(Files.newBufferedWriter(tables.resolve("dsu_summary.csv")))) {
            raw.println("variant,V,run,time_ns,parent_hops");
            summary.println("variant,V,mean_ns,stddev_ns,find_calls,union_calls,parent_hops,parent_writes");
            for (int[] size : sizes) {
                for (int variant = 0; variant < variants.length; variant++) {
                    boolean compression = (variant & 1) != 0;
                    boolean ranked = (variant & 2) != 0;
                    for (int warmup = 0; warmup < WARMUPS; warmup++) {
                        sink = dsuWorkload(size[0], compression, ranked, new Metrics());
                    }
                    long[] times = new long[REPETITIONS];
                    Metrics metrics = null;
                    for (int run = 0; run < REPETITIONS; run++) {
                        metrics = new Metrics();
                        long start = System.nanoTime();
                        int result = dsuWorkload(size[0], compression, ranked, metrics);
                        times[run] = System.nanoTime() - start;
                        sink = result;
                        raw.printf("%s,%d,%d,%d,%d%n", variants[variant], size[0], run + 1,
                                times[run], metrics.parentHops);
                    }
                    summary.printf("%s,%d,%.2f,%.2f,%d,%d,%d,%d%n", variants[variant], size[0],
                            mean(times), deviation(times), metrics.findCalls, metrics.unionCalls,
                            metrics.parentHops, metrics.parentWrites);
                }
            }
        }
    }

    private static int dsuWorkload(int vertices, boolean compression, boolean ranked, Metrics metrics) {
        DisjointSetUnion dsu = new DisjointSetUnion(vertices, metrics, compression, ranked);
        for (int vertex = 1; vertex < vertices; vertex++) {
            dsu.union(vertex, vertex - 1);
        }
        int checksum = 0;
        for (int query = 0; query < vertices; query++) {
            checksum ^= dsu.find(0);
        }
        return checksum + dsu.components();
    }

    private static double mean(long[] values) {
        double sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static List<Long> signature(Metrics metrics) {
        return List.of(metrics.edgeExaminations, metrics.comparisons, metrics.findCalls,
                metrics.unionCalls, metrics.parentHops, metrics.parentWrites, metrics.heapPushes,
                metrics.heapPops, metrics.relaxations, metrics.vertexVisits);
    }

    private static double deviation(long[] values) {
        double average = mean(values);
        double sum = 0;
        for (long value : values) {
            sum += (value - average) * (value - average);
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    private static long minimum(long[] values) {
        long result = Long.MAX_VALUE;
        for (long value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static long maximum(long[] values) {
        long result = 0;
        for (long value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}

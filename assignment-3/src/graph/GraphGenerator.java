package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    public static Graph connectedUndirected(int vertices, int edges, long seed) {
        validate(vertices, edges, false);
        if (edges < vertices - 1) {
            throw new IllegalArgumentException("A connected graph needs at least V - 1 edges");
        }
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 1; vertex < vertices; vertex++) {
            add(graph, used, random.nextInt(vertex), vertex, random);
        }
        while (graph.edges().size() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second) {
                add(graph, used, Math.min(first, second), Math.max(first, second), random);
            }
        }
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        validate(vertices, edges, false);
        if (edges < vertices - 1) {
            throw new IllegalArgumentException("This DAG generator requires E >= V - 1");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 1; vertex < vertices; vertex++) {
            add(graph, used, vertex - 1, vertex, random);
        }
        while (graph.edges().size() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second) {
                add(graph, used, Math.min(first, second), Math.max(first, second), random);
            }
        }
        return graph;
    }

    public static Graph cyclic(int vertices, int edges, long seed) {
        validate(vertices, edges, true);
        if (vertices < 5 || vertices % 5 != 0 || edges < vertices) {
            throw new IllegalArgumentException("Cyclic generator needs V divisible by 5 and E >= V");
        }
        long allowedEdges = ((long) vertices * vertices + 3L * vertices) / 2;
        if (edges > allowedEdges) {
            throw new IllegalArgumentException("Too many edges for the ordered SCC blocks");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int start = 0; start < vertices; start += 5) {
            for (int offset = 0; offset < 5; offset++) {
                add(graph, used, start + offset, start + (offset + 1) % 5, random);
            }
        }
        while (graph.edges().size() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second && first / 5 <= second / 5) {
                add(graph, used, first, second, random);
            }
        }
        return graph;
    }

    private static void add(Graph graph, Set<Long> used, int first, int second, Random random) {
        long key = ((long) first << 32) | (second & 0xffffffffL);
        if (used.add(key)) {
            graph.addEdge(first, second, 1 + random.nextInt(100));
        }
    }

    private static void validate(int vertices, int edges, boolean directed) {
        long maximum = (long) vertices * (vertices - 1) / (directed ? 1 : 2);
        if (vertices < 1 || edges < 0 || edges > maximum) {
            throw new IllegalArgumentException("Invalid graph dimensions");
        }
    }
}

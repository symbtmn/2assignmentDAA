package dsu;

import graph.Metrics;

public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;
    private final boolean compression;
    private final boolean ranked;
    private final Metrics metrics;
    private int components;

    public DisjointSetUnion(int size, Metrics metrics) {
        this(size, metrics, true, true);
    }

    public DisjointSetUnion(int size, Metrics metrics, boolean compression, boolean ranked) {
        if (size < 1) {
            throw new IllegalArgumentException("DSU size must be positive");
        }
        this.metrics = metrics;
        this.compression = compression;
        this.ranked = ranked;
        parent = new int[size];
        rank = new int[size];
        components = size;
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    public int find(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("Invalid DSU vertex: " + vertex);
        }
        metrics.findCalls++;
        int root = vertex;
        while (root != parent[root]) {
            metrics.parentHops++;
            root = parent[root];
        }
        if (compression) {
            while (vertex != root) {
                int next = parent[vertex];
                metrics.parentHops++;
                if (parent[vertex] != root) {
                    parent[vertex] = root;
                    metrics.parentWrites++;
                }
                vertex = next;
            }
        }
        return root;
    }

    public boolean union(int first, int second) {
        metrics.unionCalls++;
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (ranked && rank[firstRoot] < rank[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }
        parent[secondRoot] = firstRoot;
        metrics.parentWrites++;
        if (ranked && rank[firstRoot] == rank[secondRoot]) {
            rank[firstRoot]++;
        }
        components--;
        return true;
    }

    public int components() {
        return components;
    }
}

package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Graph {
    private final boolean directed;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Edge>> adjacency = new ArrayList<>();

    public Graph(int vertices, boolean directed) {
        if (vertices < 1) {
            throw new IllegalArgumentException("At least one vertex is required");
        }
        this.directed = directed;
        for (int i = 0; i < vertices; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertices() {
        return adjacency.size();
    }

    public boolean directed() {
        return directed;
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> neighbors(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed) {
            adjacency.get(to).add(new Edge(to, from, weight));
        }
    }

    public void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertices()) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
    }

    public void requireDirected() {
        if (!directed) {
            throw new IllegalArgumentException("A directed graph is required");
        }
    }

    public void requireUndirected() {
        if (directed) {
            throw new IllegalArgumentException("An undirected graph is required");
        }
    }
}

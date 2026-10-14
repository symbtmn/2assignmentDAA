package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    public static MstResult compute(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        List<Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((first, second) -> {
            metrics.comparisons++;
            int order = Integer.compare(first.weight(), second.weight());
            if (order == 0) {
                order = Integer.compare(first.from(), second.from());
            }
            return order == 0 ? Integer.compare(first.to(), second.to()) : order;
        });
        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices(), metrics);
        List<Edge> selected = new ArrayList<>();
        long weight = 0;
        for (Edge edge : sorted) {
            if (selected.size() == graph.vertices() - 1) {
                break;
            }
            metrics.edgeExaminations++;
            if (dsu.union(edge.from(), edge.to())) {
                selected.add(edge);
                weight += edge.weight();
            }
        }
        return new MstResult(selected, weight, dsu.components());
    }
}

package mst;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    public static MstResult compute(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        boolean[] visited = new boolean[graph.vertices()];
        PriorityQueue<Edge> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            int order = Integer.compare(first.weight(), second.weight());
            if (order == 0) {
                order = Integer.compare(first.from(), second.from());
            }
            return order == 0 ? Integer.compare(first.to(), second.to()) : order;
        });
        List<Edge> selected = new ArrayList<>();
        long weight = 0;
        int components = 0;
        for (int start = 0; start < graph.vertices(); start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visit(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                metrics.heapPops++;
                if (visited[edge.to()]) {
                    continue;
                }
                selected.add(edge);
                weight += edge.weight();
                visit(graph, edge.to(), visited, queue, metrics);
            }
        }
        return new MstResult(selected, weight, components);
    }

    private static void visit(Graph graph, int vertex, boolean[] visited,
                              PriorityQueue<Edge> queue, Metrics metrics) {
        visited[vertex] = true;
        metrics.vertexVisits++;
        for (Edge edge : graph.neighbors(vertex)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to()]) {
                queue.add(edge);
                metrics.heapPushes++;
            }
        }
    }
}

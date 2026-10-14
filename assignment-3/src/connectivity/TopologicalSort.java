package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class TopologicalSort {
    public record Result(boolean dag, List<Integer> order) {
        public Result {
            order = List.copyOf(order);
        }
    }

    public static Result compute(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int[] indegree = new int[graph.vertices()];
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            indegree[edge.to()]++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertices(); vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            metrics.vertexVisits++;
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                metrics.edgeExaminations++;
                if (--indegree[edge.to()] == 0) {
                    queue.add(edge.to());
                }
            }
        }
        boolean dag = order.size() == graph.vertices();
        return new Result(dag, dag ? order : List.of());
    }
}

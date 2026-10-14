package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Kosaraju {
    public static List<List<Integer>> compute(Graph graph, Metrics metrics) {
        graph.requireDirected();
        boolean[] visited = new boolean[graph.vertices()];
        int[] nextNeighbor = new int[graph.vertices()];
        List<Integer> finishOrder = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int start = 0; start < graph.vertices(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            metrics.vertexVisits++;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Edge> neighbors = graph.neighbors(vertex);
                if (nextNeighbor[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(nextNeighbor[vertex]++);
                    metrics.edgeExaminations++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        metrics.vertexVisits++;
                        stack.push(edge.to());
                    }
                } else {
                    finishOrder.add(stack.pop());
                }
            }
        }
        Graph reversed = new Graph(graph.vertices(), true);
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            reversed.addEdge(edge.to(), edge.from(), edge.weight());
        }
        visited = new boolean[graph.vertices()];
        List<List<Integer>> components = new ArrayList<>();
        for (int index = finishOrder.size() - 1; index >= 0; index--) {
            int start = finishOrder.get(index);
            if (visited[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                metrics.vertexVisits++;
                component.add(vertex);
                for (Edge edge : reversed.neighbors(vertex)) {
                    metrics.edgeExaminations++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        stack.push(edge.to());
                    }
                }
            }
            components.add(List.copyOf(component));
        }
        return List.copyOf(components);
    }
}

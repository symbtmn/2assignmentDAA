package shortestpath;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class Dijkstra {
    private record State(int vertex, long distance) {
    }

    public static final class Result {
        private final long[] distances;
        private final int[] predecessors;

        private Result(long[] distances, int[] predecessors) {
            this.distances = distances;
            this.predecessors = predecessors;
        }

        public long distanceTo(int vertex) {
            return distances[vertex];
        }

        public boolean reachable(int vertex) {
            return distanceTo(vertex) != Long.MAX_VALUE;
        }

        public List<Integer> pathTo(int vertex) {
            if (!reachable(vertex)) {
                return List.of();
            }
            List<Integer> path = new ArrayList<>();
            for (int current = vertex; current != -1; current = predecessors[current]) {
                path.add(current);
            }
            Collections.reverse(path);
            return List.copyOf(path);
        }
    }

    public static Result compute(Graph graph, int source, Metrics metrics) {
        graph.checkVertex(source);
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            if (edge.weight() < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative weights");
            }
        }
        long[] distances = new long[graph.vertices()];
        int[] predecessors = new int[graph.vertices()];
        Arrays.fill(distances, Long.MAX_VALUE);
        Arrays.fill(predecessors, -1);
        distances[source] = 0;
        PriorityQueue<State> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            int order = Long.compare(first.distance(), second.distance());
            return order == 0 ? Integer.compare(first.vertex(), second.vertex()) : order;
        });
        queue.add(new State(source, 0));
        metrics.heapPushes++;
        while (!queue.isEmpty()) {
            State current = queue.remove();
            metrics.heapPops++;
            if (current.distance() != distances[current.vertex()]) {
                continue;
            }
            metrics.vertexVisits++;
            for (Edge edge : graph.neighbors(current.vertex())) {
                metrics.edgeExaminations++;
                long candidate = current.distance() + edge.weight();
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    predecessors[edge.to()] = current.vertex();
                    metrics.relaxations++;
                    queue.add(new State(edge.to(), candidate));
                    metrics.heapPushes++;
                }
            }
        }
        return new Result(distances, predecessors);
    }
}

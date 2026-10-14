package app;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import graph.GraphIO;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("demo")) {
            demo();
        } else if (args[0].equals("mst") && args.length == 2) {
            printMst(GraphIO.read(Path.of(args[1])));
        } else if (args[0].equals("connectivity") && args.length == 2) {
            printConnectivity(GraphIO.read(Path.of(args[1])));
        } else if (args[0].equals("routes") && args.length == 3) {
            printRoutes(GraphIO.read(Path.of(args[1])), Integer.parseInt(args[2]), null);
        } else {
            throw new IllegalArgumentException("Use demo | mst FILE | connectivity FILE | routes FILE SOURCE");
        }
    }

    private static void demo() {
        String[] districts = {"Central", "University", "Hospital", "Airport", "Industrial", "Suburb"};
        Graph roads = new Graph(6, false);
        roads.addEdge(0, 1, 4);
        roads.addEdge(0, 2, 2);
        roads.addEdge(1, 2, 1);
        roads.addEdge(1, 3, 5);
        roads.addEdge(2, 3, 8);
        roads.addEdge(2, 4, 10);
        roads.addEdge(3, 4, 2);
        System.out.println("SMART CITY INFRASTRUCTURE AND ROUTE PLANNING");
        System.out.println("Districts: " + List.of(districts));
        System.out.println("Construction costs are independent of travel distances.");
        Graph infrastructure = new Graph(6, false);
        for (Edge edge : roads.edges()) {
            infrastructure.addEdge(edge.from(), edge.to(), edge.weight() * 10);
        }
        infrastructure.addEdge(4, 5, 30);
        printMst(infrastructure);
        System.out.println("\nTravel distances in km from Central; Suburb has no road yet:");
        printRoutes(roads, 0, districts);
        Graph services = new Graph(6, true);
        services.addEdge(0, 1, 0);
        services.addEdge(0, 2, 0);
        services.addEdge(1, 3, 0);
        services.addEdge(2, 3, 0);
        services.addEdge(3, 4, 0);
        System.out.println("\nInfrastructure service dependencies:");
        printConnectivity(services);
        services.addEdge(4, 2, 0);
        System.out.println("\nOne-way network after adding a cycle:");
        printConnectivity(services);
    }

    private static void printMst(Graph graph) {
        MstResult kruskal = Kruskal.compute(graph, new Metrics());
        MstResult prim = Prim.compute(graph, new Metrics());
        System.out.println("Kruskal total construction cost: " + kruskal.totalWeight());
        System.out.println("Prim total construction cost: " + prim.totalWeight());
        System.out.println("Same minimum weight: " + (kruskal.totalWeight() == prim.totalWeight()));
        System.out.println("Connected: " + kruskal.connected() + "; components: " + kruskal.components());
        System.out.println(kruskal.connected() ? "Selected MST roads:" : "Selected minimum spanning forest roads:");
        for (Edge edge : kruskal.edges()) {
            System.out.printf("%d -- %d : %d%n", edge.from(), edge.to(), edge.weight());
        }
    }

    private static void printConnectivity(Graph graph) {
        TopologicalSort.Result result = TopologicalSort.compute(graph, new Metrics());
        System.out.println(result.dag() ? "Topological ordering: " + result.order()
                : "Topological ordering is impossible: the graph contains a cycle.");
        System.out.println("Strongly connected components: " + Kosaraju.compute(graph, new Metrics()));
    }

    private static void printRoutes(Graph graph, int source, String[] names) {
        Dijkstra.Result result = Dijkstra.compute(graph, source, new Metrics());
        for (int vertex = 0; vertex < graph.vertices(); vertex++) {
            String label = names == null ? Integer.toString(vertex) : names[vertex];
            if (!result.reachable(vertex)) {
                System.out.println(label + ": unreachable, path: []");
            } else {
                List<Integer> path = result.pathTo(vertex);
                String displayedPath = names == null ? path.toString()
                        : path.stream().map(index -> names[index]).toList().toString();
                System.out.println(label + ": distance " + result.distanceTo(vertex) + ", path: " + displayedPath);
            }
        }
    }
}

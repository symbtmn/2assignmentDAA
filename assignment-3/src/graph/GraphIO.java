package graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public final class GraphIO {
    public static Graph read(Path path) throws IOException {
        try (Scanner scanner = new Scanner(Files.newBufferedReader(path))) {
            int vertices = scanner.nextInt();
            int edges = scanner.nextInt();
            String mode = scanner.next();
            if (edges < 0 || (!mode.equals("directed") && !mode.equals("undirected"))) {
                throw new IllegalArgumentException("Expected V E directed|undirected");
            }
            Graph graph = new Graph(vertices, mode.equals("directed"));
            for (int index = 0; index < edges; index++) {
                graph.addEdge(scanner.nextInt(), scanner.nextInt(), scanner.nextInt());
            }
            if (scanner.hasNext()) {
                throw new IllegalArgumentException("Unexpected content after the declared edges");
            }
            return graph;
        }
    }
}

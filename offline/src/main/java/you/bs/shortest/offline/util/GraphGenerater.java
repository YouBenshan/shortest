package you.bs.shortest.offline.util;


import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author You Benshan
 */
public class GraphGenerater {
    private GraphGenerater(){
    }
    public static List<Triple<Integer, Integer, DoubleDoublePair>> generate(Graph<Integer, DefaultWeightedEdge> graph) {
        List<Triple<Integer, Integer, DoubleDoublePair>> list = new ArrayList<>();
        for (DefaultWeightedEdge e : graph.edgeSet()) {
            double w = graph.getEdgeWeight(e);
            list.add(Triple.of(graph.getEdgeSource(e), graph.getEdgeTarget(e), DoubleDoublePair.of(w, -w)));
        }
        return list;
    }

    public static List<Triple<Integer, Integer, DoubleDoublePair>> generateIntWeight(Graph<Integer, DefaultWeightedEdge> graph) {
        List<Triple<Integer, Integer, DoubleDoublePair>> list = new ArrayList<>();
        for (DefaultWeightedEdge e : graph.edgeSet()) {
            int w = 1 + (int) graph.getEdgeWeight(e) * 100;
            list.add(Triple.of(graph.getEdgeSource(e), graph.getEdgeTarget(e), DoubleDoublePair.of(w, -w)));
        }
        return list;
    }


    public static Graph<Integer, DefaultWeightedEdge> generateGraph(int numOfVertices) {
        DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
        GnmRandomGraphGenerator<Integer, DefaultWeightedEdge> generator = new GnmRandomGraphGenerator<>(numOfVertices, numOfVertices * 3, 19L);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeight(graph, new Random(19L));
        return graph;
    }

    private static void makeConnected(DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; i++) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            graph.addEdge((Integer) vertices[i + 1], (Integer) vertices[i]);
        }
    }

    private static void addEdgeWeight(Graph<Integer, DefaultWeightedEdge> graph, Random random) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, random.nextDouble());
        }
    }

}

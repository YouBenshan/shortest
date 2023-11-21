package you.bs.shortest.offline;

import you.bs.shortest.online.ChRouter;
import you.bs.shortest.online.Path;
import you.bs.shortest.offline.util.GraphGenerater;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.ContractionHierarchyBidirectionalDijkstra;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ChConverterTest {

    @Test
    public void testRandom() {
        int vm = 100;
        Graph<Integer, DefaultWeightedEdge> graph = GraphGenerater.generateGraph(vm);
        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra = new ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge>(graph, (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));


        List<Triple<Integer, Integer, DoubleDoublePair>> list = GraphGenerater.generate(graph);
        ChConverter<Integer> converter = new ChConverter<>(list);
        converter.convert();
        Map<Integer, Integer> vToLevel = converter.getVToLevels();
        ChRouter chRouter = new ChRouter(converter.getRouteCh());

        for (int i = 0; i < graph.vertexSet().size(); i++) {
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> singleSourcePaths = dijkstra.getPaths(i);
            int level = vToLevel.get(i);
            for (int j = 0; j < graph.vertexSet().size(); j++) {
                GraphPath<Integer, DefaultWeightedEdge> expect = singleSourcePaths.getPath(j);
                Path actual = chRouter.path(level, vToLevel.get(j));
                Assert.assertEquals(expect.getWeight(), actual.getWeight(), 0.0000001d);
                Assert.assertArrayEquals(expect.getVertexList().stream().mapToInt(vToLevel::get).toArray(), actual.getLevels());
                Assert.assertEquals(actual.getDistance(), -actual.getWeight(), 0.0000001d);

            }
        }
    }

}

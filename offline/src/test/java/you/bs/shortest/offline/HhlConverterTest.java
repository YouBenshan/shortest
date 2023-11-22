package you.bs.shortest.offline;

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Assert;
import org.junit.Test;
import you.bs.shortest.common.Hhl;
import you.bs.shortest.common.RouteCh;
import you.bs.shortest.offline.util.GraphGenerater;
import you.bs.shortest.online.ChRouter;
import you.bs.shortest.online.HhlDistancer;
import you.bs.shortest.online.Path;

import java.util.List;

/**
 * @author You Benshan
 */

public class HhlConverterTest {
    @Test
    public void testRandom() {
        int vm = 100;
        Graph<Integer, DefaultWeightedEdge> graph = GraphGenerater.generateGraph(vm);
        List<Triple<Integer, Integer, DoubleDoublePair>> list = GraphGenerater.generate(graph);
        ChConverter<Integer> converter = new ChConverter<>(list);
        converter.convert();
        RouteCh rch = converter.getRouteCh();
        ChRouter chRouter = new ChRouter(rch);

        Hhl hhl = new HhlConverter(rch).convert();
        HhlDistancer hhlDistancer = new HhlDistancer(hhl);

        for (int i = 0; i < vm; i++) {
            for (int j = 0; j < vm; j++) {
                Path expect = chRouter.path(i, j);
                DoubleDoublePair actual = hhlDistancer.weight(i, j);
                Assert.assertEquals(expect.getWeight(), actual.firstDouble(), 0.0000001d);
                Assert.assertEquals(expect.getDistance(), actual.secondDouble(), 0.0000001d);
            }
        }
    }

}

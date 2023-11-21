package you.bs.shortest.offline;

import you.bs.shortest.common.RouteCh;
import you.bs.shortest.common.Tnr;
import you.bs.shortest.offline.util.GraphGenerater;
import you.bs.shortest.online.ChRouter;
import you.bs.shortest.online.Path;
import you.bs.shortest.online.TnrRouter;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TnrConverterTest {
    @Test
    public void testRandom() {
        int vm = 500;
        Graph<Integer, DefaultWeightedEdge> graph = GraphGenerater.generateGraph(vm);
        List<Triple<Integer, Integer, DoubleDoublePair>> list = GraphGenerater.generate(graph);
        ChConverter<Integer> converter = new ChConverter<>(list);
        converter.convert();
        RouteCh rch = converter.getRouteCh();
        ChRouter chRouter = new ChRouter(rch);

        TnrConverter tnrConverter = new TnrConverter(rch);
        Tnr tnr = tnrConverter.convert();
        TnrRouter tnrRouter = new TnrRouter(tnr);

        for (int i = 0; i < vm; i++) {
            for (int j = 0; j < vm; j++) {
                Path expect = chRouter.path(i, j);
                Path actual = tnrRouter.path(i, j);
                Assert.assertEquals(expect.getWeight(), actual.getWeight(), 0.0000001d);
                Assert.assertEquals(expect.getDistance(), actual.getDistance(), 0.0000001d);
                Assert.assertArrayEquals(expect.getLevels(), actual.getLevels());
            }
        }
    }

}

package you.bs.shortest.offline;

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.alg.util.Triple;
import org.junit.Assert;
import org.junit.Test;
import you.bs.shortest.common.Hhl;
import you.bs.shortest.common.RouteCh;
import you.bs.shortest.common.Tnr;
import you.bs.shortest.offline.util.GrReader;
import you.bs.shortest.online.ChRouter;
import you.bs.shortest.online.HhlDistancer;
import you.bs.shortest.online.Path;
import you.bs.shortest.online.TnrRouter;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GrTest {
    @Test
    public void testSmallCitys() throws URISyntaxException {
        testFile("rome99");
        testFile("small");
    }
//    @Test
    public void testBigCity() throws URISyntaxException {
        testFile("USA-road-d.COL");
    }

    private void testFile(String name) throws URISyntaxException {
        List<Triple<Integer, Integer, DoubleDoublePair>> list = new GrReader().read(name);
        ChConverter<Integer> converter = new ChConverter<>(list);
        converter.convert();
        RouteCh rch = converter.getRouteCh();
        ChRouter chRouter = new ChRouter(rch);

        TnrConverter tnrConverter = new TnrConverter(rch);
        Tnr tnr = tnrConverter.convert();
        TnrRouter tnrRouter = new TnrRouter(tnr);

        Hhl hhl = new HhlConverter(rch).convert();
        HhlDistancer hhlDistancer = new HhlDistancer(hhl);

        Map<Integer, Integer> vToLevels = converter.getVToLevels();

        int testPair = 1000;
        Random random = new Random();
        for (int i = 0; i < testPair; i++) {
            int s = random.nextInt(vToLevels.size());
            int t = random.nextInt(vToLevels.size());

            Path chResult = chRouter.path(s, t);
            Path tnrResult = tnrRouter.path(s, t);
            DoubleDoublePair hhlResult = hhlDistancer.weight(s, t);
            Assert.assertEquals(chResult.getWeight(), hhlResult.firstDouble(), 0.0000001d);
            Assert.assertEquals(chResult.getDistance(), hhlResult.secondDouble(), 0.0000001d);
            Assert.assertEquals(tnrResult.getWeight(), hhlResult.firstDouble(), 0.0000001d);
            Assert.assertEquals(tnrResult.getDistance(), hhlResult.secondDouble(), 0.0000001d);
        }
    }
}

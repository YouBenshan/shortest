package you.bs.shortest.offline.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphUtil {
    private static final Logger logger = Logger.getLogger(GraphUtil.class.getName());

    private GraphUtil() {
    }

    public static <V> void noRedundant(Table<V, V, DoubleDoublePair> weightAndDistances) {
        List<Pair<V, V>> redundants = new ArrayList<>();
        for (Table.Cell<V, V, DoubleDoublePair> cell : weightAndDistances.cellSet()) {
            for (Map.Entry<V, DoubleDoublePair> e : weightAndDistances.row(cell.getRowKey()).entrySet()) {
                DoubleDoublePair second = weightAndDistances.get(e.getKey(), cell.getColumnKey());
                if (second != null && cell.getValue().firstDouble() > second.firstDouble() + e.getValue().firstDouble()) {
                    redundants.add(Pair.of(cell.getRowKey(), cell.getColumnKey()));
                }
            }
        }
        for (Pair<V, V> p : redundants) {
            weightAndDistances.remove(p.first(), p.second());
        }
    }

    public static <V> Table<V, V, DoubleDoublePair> noMultiEdge(List<Triple<V, V, DoubleDoublePair>> weightAndDistances) {
        logger.log(Level.INFO, "size of input edges: {0}", weightAndDistances.size());
        Table<V, V, DoubleDoublePair> results = HashBasedTable.create();
        for (Triple<V, V, DoubleDoublePair> triple : weightAndDistances) {
            DoubleDoublePair p = results.get(triple.getFirst(), triple.getSecond());
            if (p == null || triple.getThird().firstDouble() < p.firstDouble()) {
                results.put(triple.getFirst(), triple.getSecond(), triple.getThird());
            }
        }
        logger.log(Level.INFO, "size without multi-edges: {0}", results.size());
        return results;
    }
}

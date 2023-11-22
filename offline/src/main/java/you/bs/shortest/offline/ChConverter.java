package you.bs.shortest.offline;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.Getter;
import org.jgrapht.alg.util.Triple;
import org.jheaps.AddressableHeap;
import org.jheaps.array.DaryArrayAddressableHeap;
import you.bs.shortest.common.Level;
import you.bs.shortest.common.RouteCh;
import you.bs.shortest.offline.util.CommonUtil;
import you.bs.shortest.offline.util.GraphUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author You Benshan
 */
public class ChConverter<V extends Comparable<V>> {
    private static final Random RANDOM = new Random();
    private final int maxHop;
    private final Table<V, V, DoubleDoublePair> weightAndDistances;
    private final List<V> levelToVs = new ArrayList<>();
    @Getter
    private final Map<V, Integer> vToLevels = new HashMap<>();
    private final Set<Remaining> remainings = new HashSet<>();
    private final Table<V, V, IntObjectPair<V>> shortcuts = HashBasedTable.create();
    Logger logger = Logger.getLogger(getClass().getName());
    private int size = 0;
    @Getter
    private RouteCh routeCh;

    public ChConverter(List<Triple<V, V, DoubleDoublePair>> list) {
        this(list, 7);
    }

    public ChConverter(List<Triple<V, V, DoubleDoublePair>> list, int maxHop) {
        this.maxHop = maxHop;
        this.weightAndDistances = GraphUtil.noMultiEdge(list);
        GraphUtil.noRedundant(this.weightAndDistances);
    }

    public void convert() {
        long start = System.currentTimeMillis();
        init();
        statisticAll();
        long current = 0L;
        while (!remainings.isEmpty()) {
            if (current == 0 || System.currentTimeMillis() - current > 10_000) {
                current = System.currentTimeMillis();
                logger.log(java.util.logging.Level.INFO, "remainings.size= {0}", remainings.size());
            }
            Set<Remaining> independents = independents();
            remainings.removeAll(independents);
            levelToVs.addAll(independents.stream().map(r -> r.v).collect(Collectors.toList()));
            Set<Remaining> neighbours = independents.stream().map(Remaining::contract).flatMap(Collection::stream).collect(Collectors.toSet());
            neighbours.parallelStream().forEach(Remaining::statistics);
        }
        size = levelToVs.size();
        for (int i = 0; i < size; i++) {
            vToLevels.put(levelToVs.get(i), i);
        }
        Int2IntArrayMap[] stc = new Int2IntArrayMap[size];
        List<IntIntPair>[] cst = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            stc[i] = new Int2IntArrayMap();
            cst[i] = new ArrayList<>();
        }
        for (Table.Cell<V, V, IntObjectPair<V>> cell : shortcuts.cellSet()) {
            int s = vToLevels.get(cell.getRowKey());
            int t = vToLevels.get(cell.getColumnKey());
            int c = vToLevels.get(cell.getValue().value());
            stc[s].put(t, c);
            cst[c].add(IntIntPair.of(s, t));
        }

        Int2DoubleRBTreeMap[] upOuts = new Int2DoubleRBTreeMap[size];
        Int2DoubleRBTreeMap[] upIns = new Int2DoubleRBTreeMap[size];
        List<Level>[] distances = new ArrayList[size];

        for (int i = 0; i < size; i++) {
            upOuts[i] = new Int2DoubleRBTreeMap();
            upIns[i] = new Int2DoubleRBTreeMap();
            distances[i] = new ArrayList<>();
        }

        for (Table.Cell<V, V, DoubleDoublePair> cell : weightAndDistances.cellSet()) {
            int s = vToLevels.get(cell.getRowKey());
            int t = vToLevels.get(cell.getColumnKey());
            double w = cell.getValue().firstDouble();
            double d = cell.getValue().secondDouble();
            distances[s].add(new Level(t, d));
            if (s < t) {
                upOuts[s].put(t, w);
            } else {
                upIns[t].put(s, w);
            }
        }
        for (int c = 0; c < size; c++) {
            for (IntIntPair p : cst[c]) {
                int s = p.firstInt();
                int t = p.secondInt();
                double w = upIns[c].get(s) + upOuts[c].get(t);
                if (s < t) {
                    upOuts[s].put(t, w);
                } else {
                    upIns[t].put(s, w);
                }
            }
        }

        Level[][] dd = Arrays.stream(distances).map(l -> l.toArray(new Level[0])).toArray(Level[][]::new);
        routeCh = new RouteCh(toUps(upIns), toUps(upOuts), dd, stcs(stc));

        logger.log(java.util.logging.Level.INFO, "CH processing time: {0} ms", System.currentTimeMillis() - start);
        logger.log(java.util.logging.Level.INFO, "upIns: {0}", CommonUtil.objectStatistics(routeCh.getUpIns()));
        logger.log(java.util.logging.Level.INFO, "upOuts: {0}", CommonUtil.objectStatistics(routeCh.getUpOuts()));
        logger.log(java.util.logging.Level.INFO, "shortcuts: {0}", CommonUtil.objectStatistics(routeCh.getShortCuts()));
    }

    private Level[] toLevels(Int2DoubleRBTreeMap map) {
        return map.int2DoubleEntrySet().stream().map(e -> new Level(e.getIntKey(), e.getDoubleValue())).toArray(Level[]::new);
    }

    private Level[][] toUps(Int2DoubleRBTreeMap[] maps) {
        return Arrays.stream(maps).map(this::toLevels).toArray(Level[][]::new);
    }

    private int[][] stc(Int2IntArrayMap stc) {
        return stc.int2IntEntrySet().stream().map(e -> new int[]{e.getIntKey(), e.getIntValue()}).toArray(int[][]::new);
    }

    private int[][][] stcs(Int2IntArrayMap[] stcs) {
        return Arrays.stream(stcs).map(this::stc).toArray(int[][][]::new);
    }

    private void statisticAll() {
        remainings.parallelStream().forEach(Remaining::statistics);
    }

    private void init() {
        Set<V> vs = new HashSet<>();
        vs.addAll(weightAndDistances.rowKeySet());
        vs.addAll(weightAndDistances.columnKeySet());
        Map<V, Remaining> map = vs.stream().collect(Collectors.toMap(v -> v, v -> new Remaining(v)));
        for (Table.Cell<V, V, DoubleDoublePair> cell : weightAndDistances.cellSet()) {
            Remaining source = map.get(cell.getRowKey());
            Remaining target = map.get(cell.getColumnKey());
            source.outWeights.put(target, cell.getValue().firstDouble());
            target.ins.add(source);
        }
        remainings.addAll(map.values());
    }

    private Set<Remaining> independents() {
        Set<Remaining> results = new HashSet<>();
        Set<Remaining> checked = new HashSet<>();
        for (Remaining r : remainings) {
            if (!checked.contains(r)) {
                Map<Boolean, List<Remaining>> greatNeighbours = r.greatNeighbours();
                if (greatNeighbours.get(Boolean.FALSE) == null) {
                    results.add(r);
                }
                checked.addAll(greatNeighbours.getOrDefault(Boolean.TRUE, Collections.emptyList()));
                checked.add(r);
            }
        }
        return results;
    }

    private int orignal(V s, V t) {
        IntObjectPair<V> r = shortcuts.get(s, t);
        if (r == null) {
            return 1;
        } else {
            return r.firstInt();
        }
    }


    class Remaining implements Comparable<Remaining> {
        private final V v;
        private final Set<Remaining> ins = new HashSet<>();
        private final Object2DoubleAVLTreeMap<Remaining> outWeights = new Object2DoubleAVLTreeMap<>();
        private final int random = RANDOM.nextInt();
        private int depth;
        private double priority;

        public Remaining(V v) {
            this.v = v;
        }

        void statistics() {
            if (ins.size() == 0 || outWeights.size() == 0) {
                priority = depth;
            } else {
                priority();
            }
        }

        private void priority() {
            int rc = ins.size() + outWeights.size();
            int ro = 0;
            ro += ins.stream().mapToInt(i -> orignal(i.v, v)).sum();
            ro += outWeights.keySet().stream().mapToInt(o -> orignal(v, o.v)).sum();
            List<Pair<Remaining, Remaining>> cache = ins.parallelStream().flatMap(this::visit).collect(Collectors.toList());
            int ac = cache.size();
            int ao = cache.stream().mapToInt(p -> orignal(p.left().v, v) + orignal(v, p.right().v)).sum();
            this.priority = 4.0 * ac / rc + 2.0 * ao / ro + 1.0 * depth;
        }

        private Stream<Pair<Remaining, Remaining>> visit(Remaining in) {
            Object2DoubleAVLTreeMap<Remaining> outs = new Object2DoubleAVLTreeMap<>(outWeights);
            outs.removeDouble(in);
            if (outs.size() == 0) {
                return Stream.empty();
            }
            double inWeight = in.outWeights.getDouble(this);
            double maxOutWeight = outs.values().doubleStream().max().getAsDouble();
            double maxDistance = inWeight + maxOutWeight;
            AddressableHeap<Double, IntObjectPair<Remaining>> heap = new DaryArrayAddressableHeap<>(4);
            Map<Remaining, AddressableHeap.Handle<Double, IntObjectPair<Remaining>>> seen = new IdentityHashMap<>();
            AddressableHeap.Handle<Double, IntObjectPair<Remaining>> node = heap.insert(0d, IntObjectPair.of(maxHop, in));
            seen.put(in, node);
            while (heap.size() > 0 && outs.size() > 0) {
                AddressableHeap.Handle<Double, IntObjectPair<Remaining>> min = heap.deleteMin();
                Remaining minRemaining = min.getValue().value();
                if (min.getKey() <= inWeight + outs.getOrDefault(minRemaining, Double.NEGATIVE_INFINITY)) {
                    outs.removeDouble(minRemaining);
                }
                int leftHop = min.getValue().leftInt() - 1;
                if (leftHop > 0) {
                    for (Object2DoubleMap.Entry<Remaining> next : minRemaining.outWeights.object2DoubleEntrySet()) {
                        if (next.getKey() != this) {
                            double ww = min.getKey() + next.getDoubleValue();
                            if (ww < maxDistance) {
                                AddressableHeap.Handle<Double, IntObjectPair<Remaining>> nn = seen.get(next.getKey());
                                if (nn == null) {
                                    nn = heap.insert(ww, IntObjectPair.of(leftHop, next.getKey()));
                                    seen.put(next.getKey(), nn);
                                } else if (ww < nn.getKey()) {
                                    nn.decreaseKey(ww);
                                }
                            }
                        }
                    }
                }
            }
            return outs.keySet().stream().map(o -> Pair.of(in, o));
        }

        Set<Remaining> contract() {
            List<Pair<Remaining, Remaining>> cache = ins.parallelStream().flatMap(this::visit).collect(Collectors.toList());
            for (Pair<Remaining, Remaining> p : cache) {
                int orignal = orignal(p.first().v, v) + orignal(v, p.second().v);
                shortcuts.put(p.first().v, p.second().v, IntObjectPair.of(orignal, v));
                p.left().outWeights.put(p.right(), p.left().outWeights.getDouble(this) + this.outWeights.getDouble(p.right()));
                p.right().ins.add(p.left());
            }

            ins.stream().forEach(r -> r.outWeights.removeDouble(this));
            outWeights.keySet().stream().forEach(r -> r.ins.remove(this));

            Set<Remaining> neighbours = new HashSet<>();
            neighbours.addAll(ins);
            neighbours.addAll(outWeights.keySet());
            int d = this.depth + 1;
            neighbours.stream().forEach(r -> r.depth = Math.max(r.depth, d));
            return neighbours;
        }

        private Map<Boolean, List<Remaining>> greatNeighbours() {
            return twoHop().stream().collect(Collectors.groupingBy(this::neighbourGreat));
        }

        private boolean neighbourGreat(Remaining other) {
            if (priority == other.priority) {
                return this.random < other.random;
            }
            return this.priority < other.priority;
        }


        private Set<Remaining> twoHop() {
            Set<Remaining> one = oneHop();
            Set<Remaining> set = new HashSet<>(one);
            for (Remaining r : one) {
                set.addAll(r.oneHop());
            }
            set.remove(this);
            return set;
        }

        private Set<Remaining> oneHop() {
            return Stream.concat(this.outWeights.keySet().stream(), this.ins.stream()).collect(Collectors.toSet());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Remaining remaining = (Remaining) o;
            return Objects.equals(v, remaining.v);
        }

        @Override
        public int hashCode() {
            return Objects.hash(v);
        }

        @Override
        public int compareTo(Remaining o) {
            return v.compareTo(o.v);
        }
    }

}
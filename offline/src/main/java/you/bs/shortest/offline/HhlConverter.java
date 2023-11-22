package you.bs.shortest.offline;


import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import you.bs.shortest.common.Hhl;
import you.bs.shortest.common.Level;
import you.bs.shortest.common.LevelWithDistance;
import you.bs.shortest.common.RouteCh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author You Benshan
 */
public class HhlConverter {

    private final LevelWithDistance[][] upOutsFromCh;
    private final LevelWithDistance[][] upInsFromCh;
    Logger logger = Logger.getLogger(getClass().getName());

    public HhlConverter(RouteCh ch) {
        Int2DoubleRBTreeMap[] distances = fullDistance(ch.getDistances(), ch.getShortCuts());
        this.upOutsFromCh = createUps((i, j) -> distances[i].applyAsDouble(j), ch.getUpOuts());
        this.upInsFromCh = createUps((i, j) -> distances[j].applyAsDouble(i), ch.getUpIns());
    }

    public Hhl convert() {
        long start = System.currentTimeMillis();
        int size = upInsFromCh.length;
        Hhl.Label[] upIns = new Hhl.Label[size];
        Hhl.Label[] upOuts = new Hhl.Label[size];

        IntStream.range(0, size).forEach(i -> {
            int j = size - 1 - i;
            CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> min(upOuts, upIns, j, upOutsFromCh));
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> min(upIns, upOuts, j, upInsFromCh));
            CompletableFuture.allOf(f1, f2).join();
        });

        Arrays.stream(upIns).parallel().forEach(this::reverse);
        Arrays.stream(upOuts).parallel().forEach(this::reverse);
        logger.log(java.util.logging.Level.INFO, "HHL processing time: {0} ms", System.currentTimeMillis() - start);
        logger.log(java.util.logging.Level.INFO, "upIns: {0}", Arrays.stream(upIns).mapToInt(l -> l.getLevel().length).summaryStatistics());
        logger.log(java.util.logging.Level.INFO, "upOuts: {0}", Arrays.stream(upOuts).mapToInt(l -> l.getLevel().length).summaryStatistics());
        return new Hhl(upIns, upOuts);
    }

    private void reverse(Hhl.Label label) {
        int length = label.getLevel().length;
        for (int i = 0, j = length - 1; i < j; i++, j--) {
            swap(label.getLevel(), i, j);
            swap(label.getWeight(), i, j);
            swap(label.getDistance(), i, j);
        }
    }

    private void swap(double[] a, int i, int j) {
        double temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    private void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    private void min(Hhl.Label[] ups, Hhl.Label[] otherUps, int i, LevelWithDistance[][] chUps) {
        List<LevelWithDistance> revertMay = getRevertMay(chUps[i], ups);
        List<LevelWithDistance> up = new ArrayList<>();
        for (LevelWithDistance lwd : revertMay) {
            if (isMin(up, otherUps[lwd.getIndex()], lwd)) {
                up.add(lwd);
            }
        }
        up.add(new LevelWithDistance(i, 0d, 0d));
        ups[i] = toLabel(up);
    }

    private Hhl.Label toLabel(List<LevelWithDistance> list) {
        int[] levels = new int[list.size()];
        double[] weights = new double[list.size()];
        double[] distances = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            LevelWithDistance v = list.get(i);
            levels[i] = v.getIndex();
            weights[i] = v.getWeight();
            distances[i] = v.getDistance();
        }
        return new Hhl.Label(levels, weights, distances);
    }

    private boolean isMin(List<LevelWithDistance> up, Hhl.Label otherUps, LevelWithDistance may) {
        for (int i = 0, j = 0; i < up.size() && j < otherUps.getLevel().length; ) {
            LevelWithDistance upi = up.get(i);
            int otherLevel = otherUps.getLevel()[j];
            if (upi.getIndex() < otherLevel) {
                j++;
            } else if (upi.getIndex() > otherLevel) {
                i++;
            } else {
                if (upi.getWeight() + otherUps.getWeight()[j] <= may.getWeight()) {
                    return false;
                }
                j++;
                i++;
            }
        }
        return true;
    }

    private List<LevelWithDistance> getRevertMay(LevelWithDistance[] chUps, Hhl.Label[] ups) {
        Int2ObjectOpenHashMap<LevelWithDistance> may = new Int2ObjectOpenHashMap<>();
        for (LevelWithDistance directUp : chUps) {
            double directUpWeight = directUp.getWeight();
            double directUpDistance = directUp.getDistance();
            Hhl.Label label = ups[directUp.getIndex()];
            for (int i = 0; i < label.getLevel().length; i++) {
                double w = label.getWeight()[i] + directUpWeight;
                int index = label.getLevel()[i];
                LevelWithDistance lwd = may.get(index);
                if (lwd == null) {
                    double d = label.getDistance()[i] + directUpDistance;
                    may.put(index, new LevelWithDistance(index, w, d));
                } else if (w < lwd.getWeight()) {
                    double d = label.getDistance()[i] + directUpDistance;
                    lwd.setWeight(w);
                    lwd.setDistance(d);
                }
            }
        }
        return may.values().stream().sorted(Comparator.comparingInt(LevelWithDistance::getIndex).reversed()).collect(Collectors.toList());
    }

    private LevelWithDistance[][] createUps(BiFunction<Integer, Integer, Double> distanceFunction, Level[][] chUps) {
        int size = chUps.length;
        LevelWithDistance[][] ups = new LevelWithDistance[size][];
        for (int i = 0; i < size; i++) {
            Level[] chUp = chUps[i];
            LevelWithDistance[] up = new LevelWithDistance[chUp.length];
            ups[i] = up;
            for (int j = 0; j < chUp.length; j++) {
                Level level = chUp[j];
                double distance = distanceFunction.apply(i, level.getIndex());
                up[j] = new LevelWithDistance(level.getIndex(), level.getWeight(), distance);
            }
        }
        return ups;
    }

    private Int2DoubleRBTreeMap[] fullDistance(Level[][] chDistances, int[][][] stc) {
        int size = chDistances.length;
        Int2DoubleRBTreeMap[] distances = new Int2DoubleRBTreeMap[chDistances.length];

        List<IntIntPair>[] cst = new ArrayList[size];
        for (int c = 0; c < size; c++) {
            cst[c] = new ArrayList<>();
            distances[c] = new Int2DoubleRBTreeMap();
            for (Level l : chDistances[c]) {
                distances[c].put(l.getIndex(), l.getWeight());
            }
        }

        for (int s = 0; s < size; s++) {
            for (int[] tc : stc[s]) {
                cst[tc[1]].add(IntIntPair.of(s, tc[0]));
            }
        }
        for (int c = 0; c < size; c++) {
            for (IntIntPair st : cst[c]) {
                int s = st.firstInt();
                int t = st.secondInt();
                distances[s].put(t, distances[s].get(c) + distances[c].get(t));
            }
        }
        return distances;
    }
}

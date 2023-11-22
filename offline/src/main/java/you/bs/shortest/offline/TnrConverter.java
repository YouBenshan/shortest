package you.bs.shortest.offline;


import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jheaps.tree.PairingHeap;
import you.bs.shortest.common.Hhl;
import you.bs.shortest.common.Level;
import you.bs.shortest.common.RouteCh;
import you.bs.shortest.common.Tnr;
import you.bs.shortest.offline.util.CommonUtil;
import you.bs.shortest.online.HhlDistancer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author You Benshan
 */
public class TnrConverter {
    private final RouteCh ch;
    private final Hhl hhl;
    private final HhlDistancer hhlDistancer;
    Logger logger = Logger.getLogger(getClass().getName());
    int size;
    int tnSize;
    int localSize;
    double[][] tnTable;
    int[] voronoi;

    public TnrConverter(RouteCh ch) {
        this.ch = ch;
        this.hhl = new HhlConverter(ch).convert();
        this.hhlDistancer = new HhlDistancer(hhl);
        size = ch.getUpIns().length;
        tnSize = (int) Math.sqrt(size);
        localSize = size - tnSize;
    }

    public Tnr convert() {
        long start = System.currentTimeMillis();
        tnTable();
        this.voronoi();
        Pair<int[][], Level[][]> vorTnIns = vorTns(ch.getUpIns(), hhl.getUpIns(), false);
        Pair<int[][], Level[][]> vorTnOuts = vorTns(ch.getUpOuts(), hhl.getUpOuts(), true);

        Level[][] tnIns = vorTnIns.second();
        Level[][] tnOuts = vorTnOuts.second();
        int[][] vorIns = vorTnIns.first();
        int[][] vorOuts = vorTnOuts.first();

        logger.log(java.util.logging.Level.INFO, "CH processing time: {0} ms", System.currentTimeMillis() - start);
        logger.log(java.util.logging.Level.INFO, "tnIns: {0}", CommonUtil.objectStatistics(tnIns));
        logger.log(java.util.logging.Level.INFO, "tnOuts: {0}", CommonUtil.objectStatistics(tnOuts));
        logger.log(java.util.logging.Level.INFO, "vorIns: {0}", CommonUtil.intStatistics(vorIns));
        logger.log(java.util.logging.Level.INFO, "vorOuts: {0}", CommonUtil.intStatistics(vorOuts));
        return new Tnr(ch, tnTable, tnIns, tnOuts, vorIns, vorOuts);
    }

    private Pair<int[][], Level[][]> vorTns(Level[][] levels, Hhl.Label[] labels, boolean out) {
        IntSet[] vors = createIntSet(size);
        IntList[] tns = createIntList(size);
        Level[][] tnLevels = new Level[localSize][];

        for (int i = localSize - 1; i >= 0; i--) {
            vors[i].add(voronoi[i]);
            IntSet tnSet = new IntOpenHashSet();
            for (Level level : levels[i]) {
                int upLevel = level.getIndex();
                if (upLevel < localSize) {
                    vors[i].add(voronoi[upLevel]);
                } else {
                    tnSet.add(upLevel);
                }
                vors[i].addAll(vors[upLevel]);
                tnSet.addAll(tns[upLevel]);
            }
            tnLevels[i] = prune(labels[i], tnSet, out);
            tns[i] = new IntArrayList(Arrays.stream(tnLevels[i]).mapToInt(Level::getIndex).toArray());
        }
        return Pair.of(toArray(vors), tnLevels);
    }

    private int[][] toArray(IntSet[] sets) {
        int[][] r = new int[localSize][];
        for (int i = 0; i < localSize; i++) {
            r[i] = sets[i].toIntArray();
            Arrays.sort(r[i]);
        }
        return r;
    }

    private Level[] prune(Hhl.Label label, IntSet tns, boolean out) {
        List<Level> levels = contains(label, tns);
        List<Level> result = contains(label, tns);
        for (Level candidate : levels) {
            if (small(candidate, result, out)) {
                result.add(candidate);
            }
        }
        return result.toArray(Level[]::new);
    }

    private boolean small(Level candidate, List<Level> befores, boolean out) {
        for (Level before : befores) {
            double gap = out ? tnTable[size - 1 - before.getIndex()][size - 1 - candidate.getIndex()] : tnTable[size - 1 - candidate.getIndex()][size - 1 - before.getIndex()];
            if (candidate.getWeight() + 0.0000001 >= before.getWeight() + gap) {
                return false;
            }
        }
        return true;
    }

    private List<Level> contains(Hhl.Label label, IntSet tns) {
        List<Level> levels = new ArrayList<>();
        int[] tnsArray = tns.toIntArray();
        Arrays.sort(tnsArray);
        for (int i = 0, j = 0; i < label.getLevel().length && j < tnsArray.length; ) {
            if (label.getLevel()[i] == tnsArray[j]) {
                levels.add(new Level(label.getLevel()[i], label.getWeight()[i]));
                i++;
                j++;
            } else if (label.getLevel()[i] < tnsArray[j]) {
                i++;
            } else {
                j++;
            }
        }
        return levels;
    }

    private IntList[] createIntList(int n) {
        IntList[] sets = new IntList[size];
        for (int i = 0; i < n; i++) {
            sets[i] = new IntArrayList();
        }
        return sets;
    }

    private IntSet[] createIntSet(int n) {
        IntSet[] sets = new IntSet[size];
        for (int i = 0; i < n; i++) {
            sets[i] = new IntOpenHashSet();
        }
        return sets;
    }

    private void voronoi() {
        Level[] voronoiLevel = new Level[size];
        Level[][] upIns = ch.getUpIns();
        Level[][] downIns = downIns(ch.getUpOuts());
        PairingHeap<Double, Integer> heap = new PairingHeap<>();
        for (int i = size - 1; i >= localSize; i--) {
            voronoiLevel[i] = new Level(i, 0d);
            heap.insert(0d, i);
        }
        while (!heap.isEmpty()) {
            int min = heap.deleteMin().getValue();
            travelMin(voronoiLevel, upIns, heap, min);
            travelMin(voronoiLevel, downIns, heap, min);
        }
        this.voronoi = Arrays.stream(voronoiLevel).mapToInt(Level::getIndex).toArray();
    }

    private void travelMin(Level[] voronoiLevel, Level[][] ins, PairingHeap<Double, Integer> heap, int min) {
        for (Level in : ins[min]) {
            double ww = voronoiLevel[min].getWeight() + in.getWeight();
            if (voronoiLevel[in.getIndex()] == null || ww < voronoiLevel[in.getIndex()].getWeight()) {
                voronoiLevel[in.getIndex()] = new Level(voronoiLevel[min].getIndex(), ww);
                heap.insert(ww, in.getIndex());
            }
        }
    }

    private Level[][] downIns(Level[][] upOuts) {
        ArrayList<Level>[] result = new ArrayList[upOuts.length];
        for (int i = 0; i < upOuts.length; i++) {
            result[i] = new ArrayList<>();
        }
        for (int s = 0; s < upOuts.length; s++) {
            for (Level l : upOuts[s]) {
                result[l.getIndex()].add(new Level(s, l.getWeight()));
            }
        }
        return Arrays.stream(result).map(l -> l.toArray(Level[]::new)).toArray(Level[][]::new);
    }

    private void tnTable() {
        tnTable = new double[tnSize][tnSize];
        for (int i = 0; i < tnSize; i++) {
            for (int j = 0; j < tnSize; j++) {
                tnTable[i][j] = hhlDistancer.weight(size - 1 - i, size - 1 - j).leftDouble();
            }
        }
    }
}

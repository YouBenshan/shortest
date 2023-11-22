package you.bs.shortest.online;


import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.ints.*;
import org.jheaps.AddressableHeap;
import you.bs.shortest.common.Level;
import you.bs.shortest.common.RouteCh;

/**
 * @author You Benshan
 */
public class ChRouter {
    private final Level[][] upIns;
    private final Level[][] upOuts;
    private final Int2IntAVLTreeMap[] shortcuts;
    private final Int2DoubleAVLTreeMap[] distances;

    public ChRouter(RouteCh routeCh) {
        this.upIns = routeCh.getUpIns();
        this.upOuts = routeCh.getUpOuts();
        this.shortcuts = new Int2IntAVLTreeMap[upIns.length];
        int[][][] scs = routeCh.getShortCuts();
        for (int i = 0; i < scs.length; i++) {
            shortcuts[i] = new Int2IntAVLTreeMap();
            for (int[] a : scs[i]) {
                shortcuts[i].put(a[0], a[1]);
            }
        }
        this.distances = new Int2DoubleAVLTreeMap[upIns.length];
        Level[][] ds = routeCh.getDistances();
        for (int i = 0; i < ds.length; i++) {
            distances[i] = new Int2DoubleAVLTreeMap();
            for (Level ls : ds[i]) {
                distances[i].put(ls.getIndex(), ls.getWeight());
            }
        }
    }

    private int meet(IndexFrontier frontier, IndexFrontier otherFrontier) {
        int meetLevel = -1;
        double totalWeight = Double.POSITIVE_INFINITY;
        while (!frontier.heap.isEmpty()) {
            if (frontier.heap.findMin().getKey() >= totalWeight) {
                frontier.heap.clear();
            } else {
                AddressableHeap.Handle<Double, IndexFrontier.Record> min = frontier.deleteMin();
                double tw = min.getKey() + otherFrontier.getWeight(min.getValue().now);
                if (tw < totalWeight) {
                    totalWeight = tw;
                    meetLevel = min.getValue().now;
                }
            }
            if (!otherFrontier.heap.isEmpty()) {
                IndexFrontier tmpFrontier = frontier;
                frontier = otherFrontier;
                otherFrontier = tmpFrontier;
            }
        }
        return meetLevel;
    }

    public Path path(int sourceLevel, int sinkLevel) {
        check(sourceLevel, sinkLevel);
        if (sourceLevel == sinkLevel) {
            return new Path(0d, 0d, new int[]{sourceLevel});
        }

        IndexFrontier forwardFrontier = new IndexFrontier(upOuts, sourceLevel);
        IndexFrontier backwardFrontier = new IndexFrontier(upIns, sinkLevel);

        int meetLevel = meet(forwardFrontier, backwardFrontier);
        if (meetLevel == -1) {
            return Path.NOT_CONNECT;
        } else {
            double weight = forwardFrontier.seen.get(meetLevel).getKey() + backwardFrontier.seen.get(meetLevel).getKey();
            IntList upAndDown = path(forwardFrontier, backwardFrontier, meetLevel);
            IntList noShortCut = noShortCut(upAndDown);
            double distance = distances(noShortCut);
            return new Path(weight, distance, noShortCut.toIntArray());
        }
    }

    private double distances(IntList all) {
        double r = 0;
        for (int i = 0; i < all.size() - 1; i++) {
            r += distances[all.getInt(i)].get(all.getInt(i + 1));
        }
        return r;
    }

    private IntList noShortCut(IntList ps) {
        IntList r = new IntArrayList();
        int s = ps.getInt(0);
        r.add(s);
        for (int i = 1; i < ps.size(); i++) {
            int t = ps.getInt(i);
            addT(r, s, t);
            s = t;
        }
        return r;
    }

    private void addT(IntList result, int s, int t) {
        int c = shortcuts[s].getOrDefault(t, -1);
        if (c == -1) {
            result.add(t);
        } else {
            addT(result, s, c);
            addT(result, c, t);
        }
    }

    private IntList path(IndexFrontier forwardFrontier, IndexFrontier backwardFrontier, int meetLevel) {
        IntList temp = new IntArrayList();
        path(forwardFrontier.seen, meetLevel, temp);
        int[] t = temp.toIntArray();
        Ints.reverse(t);
        IntList all = new IntArrayList(t);
        all.add(meetLevel);
        path(backwardFrontier.seen, meetLevel, all);
        return all;
    }

    private void path(Int2ObjectOpenHashMap<AddressableHeap.Handle<Double, IndexFrontier.Record>> seen, int meetLevel, IntList all) {
        int down = seen.get(meetLevel).getValue().down;
        while (down != -1) {
            all.add(down);
            down = seen.get(down).getValue().down;
        }
    }

    protected void check(int sourceLevel, int sinkLevel) {
        if (sourceLevel < 0 || sourceLevel >= upOuts.length) {
            throw new IllegalArgumentException("No Source");
        }
        if (sinkLevel < 0 || sinkLevel >= upIns.length) {
            throw new IllegalArgumentException("No Sink");
        }
    }
}
package you.bs.shortest.online;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import you.bs.shortest.common.Level;
import you.bs.shortest.common.Tnr;

/**
 * @author You Benshan
 */

public class TnrRouter {
    private final Tnr tnr;
    private final ChRouter chRouter;
    private final int size;
    private final int localSize;

    public TnrRouter(Tnr tnr) {
        this.tnr = tnr;
        this.chRouter = new ChRouter(tnr.getRouteCh());
        this.size = tnr.getRouteCh().getUpOuts().length;
        this.localSize = tnr.getVoronoiOuts().length;
    }

    public Path path(int sourceLevel, int sinkLevel) {
        chRouter.check(sourceLevel, sinkLevel);
        if (sourceLevel == sinkLevel) {
            return new Path(0d, 0d, new int[]{sourceLevel});
        }
        if (sourceLevel < localSize && sinkLevel < localSize && local(tnr.getVoronoiOuts()[sourceLevel], tnr.getVoronoiIns()[sinkLevel])) {
            return chRouter.path(sourceLevel, sinkLevel);
        }
        Level[] accessOut = new Level[]{new Level(sourceLevel, 0d)};
        if (sourceLevel < localSize) {
            accessOut = tnr.getAccessOuts()[sourceLevel];
        }
        Level[] accessIn = new Level[]{new Level(sinkLevel, 0d)};
        if (sinkLevel < localSize) {
            accessIn = tnr.getAccessIns()[sinkLevel];
        }

        double total = Double.POSITIVE_INFINITY;
        Level minTnOut = null;
        Level minTnIn = null;
        for (Level tnOut : accessOut) {
            for (Level tnIn : accessIn) {
                double r = tnOut.getWeight() + tnIn.getWeight() + tnr.getTnTable()[size - 1 - tnOut.getIndex()][size - 1 - tnIn.getIndex()];
                if (r < total) {
                    total = r;
                    minTnOut = tnOut;
                    minTnIn = tnIn;
                }
            }
        }
        return merge(chRouter.path(sourceLevel, minTnOut.getIndex()), chRouter.path(minTnOut.getIndex(), minTnIn.getIndex()), chRouter.path(minTnIn.getIndex(), sinkLevel));
    }

    private Path merge(Path path0, Path path1, Path path2) {
        double weight = path0.getWeight() + path1.getWeight() + path2.getWeight();
        double distance = path0.getDistance() + path1.getDistance() + path2.getDistance();
        IntList levels = new IntArrayList(path0.getLevels());
        levels.removeInt(levels.size() - 1);
        levels.addElements(levels.size(), path1.getLevels());
        levels.removeInt(levels.size() - 1);
        levels.addElements(levels.size(), path2.getLevels());
        return new Path(weight, distance, levels.toIntArray());
    }

    private boolean local(int[] out, int[] in) {
        for (int i = 0, j = 0; i < out.length && j < in.length; ) {
            if (out[i] == in[j]) {
                return true;
            } else if (out[i] < in[j]) {
                i++;
            } else {
                j++;
            }
        }
        return false;
    }
}

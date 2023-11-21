package you.bs.shortest.online;

import you.bs.shortest.common.Hhl;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;

public class HhlDistancer {
    private final Hhl.Label[] upIns;
    private final Hhl.Label[] upOuts;

    public HhlDistancer(Hhl hhl) {
        this.upIns = hhl.getUpIns();
        this.upOuts = hhl.getUpOuts();
    }

    public DoubleDoublePair weight(int sourceLevel, int sinkLevel) {
        check(sourceLevel, sinkLevel);
        if (sourceLevel == sinkLevel) {
            return DoubleDoublePair.of(0d, 0d);
        }
        double w = Double.POSITIVE_INFINITY;
        double d = Double.POSITIVE_INFINITY;

        Hhl.Label upOut = upOuts[sourceLevel];
        Hhl.Label upIn = upIns[sinkLevel];

        for (int i = 0, j = 0; i < upOut.getLevel().length && j < upIn.getLevel().length; ) {
            if (upOut.getLevel()[i] == upIn.getLevel()[j]) {
                double ww = upOut.getWeight()[i] + upIn.getWeight()[j];
                if (ww < w) {
                    w = ww;
                    d = upOut.getDistance()[i] + upIn.getDistance()[j];
                }
                i++;
                j++;
            } else if (upOut.getLevel()[i] < upIn.getLevel()[j]) {
                i++;
            } else {
                j++;
            }
        }
        return DoubleDoublePair.of(w, d);
    }

    private void check(int sourceLevel, int sinkLevel) {
        if (sourceLevel < 0 || sourceLevel >= upOuts.length) {
            throw new IllegalArgumentException("No Source");
        }
        if (sinkLevel < 0 || sinkLevel >= upOuts.length) {
            throw new IllegalArgumentException("No Source");
        }
    }
}

package you.bs.shortest.common;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Tnr implements Serializable {
    private final RouteCh routeCh;

    private final double[][] tnTable;
    private final Level[][] accessIns;
    private final Level[][] accessOuts;
    private final int[][] voronoiIns;
    private final int[][] voronoiOuts;

    public Tnr(RouteCh routeCh, double[][] tnTable, Level[][] accessIns, Level[][] accessOuts, int[][] voronoiIns, int[][] voronoiOuts) {
        this.routeCh = routeCh;
        this.tnTable = tnTable;
        this.accessIns = accessIns;
        this.accessOuts = accessOuts;
        this.voronoiIns = voronoiIns;
        this.voronoiOuts = voronoiOuts;
    }
}

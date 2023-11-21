package you.bs.shortest.online;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Path {
    public static final Path NOT_CONNECT = new Path(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, new int[0]);

    private final double weight;
    private final double distance;
    private final int[] levels;

}

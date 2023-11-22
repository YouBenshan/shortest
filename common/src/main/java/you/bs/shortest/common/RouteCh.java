package you.bs.shortest.common;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author You Benshan
 */
@AllArgsConstructor
@Getter
public class RouteCh implements Serializable {
    private Level[][] upIns;
    private Level[][] upOuts;
    private Level[][] distances;
    private int[][][] shortCuts;
}
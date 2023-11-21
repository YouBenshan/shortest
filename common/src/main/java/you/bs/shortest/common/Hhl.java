package you.bs.shortest.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class Hhl implements Serializable {
    private Label[] upIns;
    private Label[] upOuts;


    @AllArgsConstructor
    @Getter
    public static class Label implements Serializable {
        private int[] level;
        private double[] weight;
        private double[] distance;
    }
}
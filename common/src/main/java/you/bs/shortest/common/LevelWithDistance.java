package you.bs.shortest.common;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author You Benshan
 */
@Getter
@Setter
public class LevelWithDistance extends Level implements Serializable {
    private double distance;

    public LevelWithDistance(int index, double weight, double distance) {
        super(index, weight);
        this.distance = distance;
    }
}

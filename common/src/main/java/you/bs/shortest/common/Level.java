package you.bs.shortest.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@Setter
public class Level implements Serializable {
    private int index;
    private double weight;
}

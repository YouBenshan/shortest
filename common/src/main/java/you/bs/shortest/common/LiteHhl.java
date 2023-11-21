package you.bs.shortest.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public class LiteHhl {
    private Label[] upOuts;
    private Label[] uplns;


    public LiteHhl(Hhl hhl) {
        this.uplns = labels(hhl.getUpIns());
        this.upOuts = labels(hhl.getUpOuts());

    }

    private LiteHhl.Label label(Hhl.Label hhlLabel) {
        return new Label(hhlLabel.getLevel(), Arrays.stream(hhlLabel.getWeight()).mapToInt(d -> (int) Math.round(d)).toArray());
    }

    private LiteHhl.Label[] labels(Hhl.Label[] hhlLabels) {
        return Arrays.stream(hhlLabels).map(this::label).toArray(LiteHhl.Label[]::new);
    }

    @AllArgsConstructor
    public static class Label {
        int[] level;
        int[] weight;
    }
}
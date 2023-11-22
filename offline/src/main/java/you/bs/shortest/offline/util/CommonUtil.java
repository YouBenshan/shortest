package you.bs.shortest.offline.util;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

/**
 * @author You Benshan
 */

public class CommonUtil {
    private CommonUtil() {
    }

    public static <T> IntSummaryStatistics objectStatistics(T[][] d2) {
        return Arrays.stream(d2).mapToInt(l -> l.length).summaryStatistics();
    }

    public static IntSummaryStatistics intStatistics(int[][] d2) {
        return Arrays.stream(d2).mapToInt(l -> l.length).summaryStatistics();
    }
}

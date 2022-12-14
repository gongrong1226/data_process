package division.cell;

import java.util.Collections;
import java.util.List;

/**
 * * @author ZT 2022-12-14 11:13
 */
public class MedianRTTCalculator implements RTTCalculator {
    @Override
    public int calculate(List<Integer> rtts) {
        Collections.sort(rtts);
        int size = rtts.size();
        int result;
        if (size % 2 != 1) {
            result = (rtts.get(size / 2 - 1) + rtts.get(size / 2)) / 2;
        } else {
            result = rtts.get((size - 1) / 2);
        }
        return result;
    }
}

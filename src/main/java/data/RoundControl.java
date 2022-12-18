package data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 每一个测量任务一个轮次控制
 * TODO 定期或定量清除已有轮次
 *
 * @author ZT 2022-12-18 0:57
 */
public class RoundControl {

    private static final int DEFAULT_REMAIN_NUM = 100;

    /**
     * measurement_prefix, RoundControl
     */
    private static final ConcurrentMap<String, RoundControl> roundControlMap = new ConcurrentHashMap<>();

    public static RoundControl getRoundControl(String measurementPrefix) {
        RoundControl roundControl = roundControlMap.get(measurementPrefix);
        if (roundControl == null) {
            RoundControl newRoundControl = new RoundControl();
            roundControl = roundControlMap.putIfAbsent(measurementPrefix, newRoundControl);
            if (roundControl == null) {
                roundControl = newRoundControl;
            }
        }
        return roundControl;
    }

    /**
     * 记录已有的轮次
     */
    private final ConcurrentMap<Integer, Object> rounds;

    public RoundControl() {
        rounds = new ConcurrentHashMap<>();
    }

    public boolean isNewRound(int round) {
        boolean b = rounds.containsKey(round);
        if (b) {
            return false;
        }
        Object o = rounds.putIfAbsent(round, new Object());
        return o == null;
    }
}

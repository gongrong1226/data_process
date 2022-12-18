package data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 每一个测量任务一个轮次控制
 * @author ZT 2022-12-18 0:57
 */
public class RoundControl {

    private static final int DEFAULT_REMAIN_NUM = 100;

    /**
     * measurement_prefix, RoundControl
     */
    private static final ConcurrentMap<String, RoundControl> roundControlMap = new ConcurrentHashMap<>();




}

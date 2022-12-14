package division.cell;

import java.util.List;

/**
 * TODO 将统计值计算和Cell中的RTT插入结合到一起，优化统计值的计算速度
 *
 * @author ZT 2022-12-14 11:08
 */
public interface RTTCalculator {

    /**
     * 计算rtt统计值
     *
     * @param rtts 单元格rtt，单位微秒
     * @return 统计值
     */
    int calculate(List<Integer> rtts);

    /**
     * TODO 可能会根据不同的IP集合采用不同的RTT计算方法
     *
     * @param IPs  ip
     * @param rtts 对应IP的RTT （单位微秒）
     * @return 统计值
     */
    default int calculate(List<Integer> IPs, List<Integer> rtts) {
        return calculate(rtts);
    }
}

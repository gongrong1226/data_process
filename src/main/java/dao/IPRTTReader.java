package dao;

import java.util.List;

/**
 * 获取某个IP的RTT
 *
 * @author ZT 2022-12-17 15:44
 */
public interface IPRTTReader {

    /**
     * 获取IP的RTT
     *
     * @param ip IP
     * @return 微秒RTT
     */
    List<Integer> queryRTT(String ip);

    /**
     * 获取IP的RTT
     *
     * @param ip          IP
     * @param lastMinutes 过去的时间窗口大小
     * @return rtt
     */
    List<Integer> queryRTT(String ip, int lastMinutes);

}

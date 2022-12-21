package dao;

import java.util.List;

/**
 * 获取Path
 *
 * @author ZT 2022-12-06 21:11
 */
public interface TracerouteReader {

    /**
     * 返回从源点到IP的traceroute路径
     *
     * @param IP 目标IP
     * @return 路径
     */
    List<Object> readTraceroute(String IP);

    /**
     *
     * 返回从源点到IP的traceroute路径
     * @param IP 目标IP
     * @param lastMinutes 过去多少分钟
     * @return traceroute
     */
    List<Object> readTraceroute(String IP, int lastMinutes);

    static String getTracerouteTable(String measurementPrefix) {
        return TracerouteWriter.getTracerouteTable(measurementPrefix);
    }
}

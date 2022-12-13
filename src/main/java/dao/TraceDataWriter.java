package dao;

import pojo.TraceData;

/**
 *
 * @author ZT 2022-12-09 21:02
 */
public interface TraceDataWriter {

    /**
     * 写入零散的Trace数据
     * NOTE 注意线程安全
     * @param traceData data
     */
    void write(TraceData traceData);

    void flush();

    /**
     * 1. 把零散数据构建为Traceroute
     * 2. 把构建好的写入数据库
     * 3. 清除已经使用过的
     */
    void buildAndClear();
}

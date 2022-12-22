package dao.tracefiledb;

import dao.TraceDataWriter;
import dao.TracerouteWriter;
import pojo.TraceData;
import division.path.Traceroute;

import java.time.Instant;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * 1. 先把数据顺序存储
 * 2. 再把
 *
 * @author ZT 2022-12-10 13:53
 */
public abstract class AbstractTraceDataWriter implements TraceDataWriter {

    /**
     * 用作写入Traceroute数据
     */
    protected TracerouteWriter tracerouteWriter;

    public AbstractTraceDataWriter(TracerouteWriter tracerouteWriter) {
        this.tracerouteWriter = tracerouteWriter;
    }


    /**
     * 已保证线程安全
     *
     * @param simpleTraceData data
     */
    protected abstract void write(Traceroute.SimpleTraceData simpleTraceData);

    @Override
    public final void write(TraceData traceData) {
        Instant time = traceData.getTime();
        long epochSecond = time.getEpochSecond();
        epochSecond = epochSecond * 1_000_000_000L + time.getNano();
        write(new Traceroute.SimpleTraceData(traceData.getDest(), traceData.getResponseIp(), traceData.getHop(),
                traceData.getMicrosecondRTT(), epochSecond));
    }

    /**
     * 把零散的Trace数据构建成一条条Traceroute
     */
    protected abstract void buildTraceroute();

    /**
     * 清理已经使用过的Trace数据
     */
    protected abstract void clearOldTraceData();

    protected static abstract class TracerouteIterator implements Iterator<Traceroute> {
        @Override
        public final void remove() {
            Iterator.super.remove();
        }

        @Override
        public final void forEachRemaining(Consumer<? super Traceroute> action) {
            Iterator.super.forEachRemaining(action);
        }
    }

    protected abstract TracerouteIterator getTracerouteIterator();

    @Override
    public void buildAndClear() {
        buildTraceroute();
        TracerouteIterator tracerouteIterator = getTracerouteIterator();
        while (tracerouteIterator.hasNext()) {
            tracerouteWriter.write(tracerouteIterator.next());
        }
        clearOldTraceData();
    }
}

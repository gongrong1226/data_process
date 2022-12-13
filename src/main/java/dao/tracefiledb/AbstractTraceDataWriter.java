package dao.tracefiledb;

import com.zfoo.protocol.IPacket;
import com.zfoo.protocol.registration.anno.Protocol;
import dao.TraceDataWriter;
import dao.TracerouteWriter;
import pojo.TraceData;
import pojo.division.Traceroute;

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

    @Protocol
    protected static class SimpleTraceData implements IPacket {

        public static final transient short PROTOCOL_ID = 101;
        private String dest;
        private String response;
        private Integer hop;
        // 微秒
        private Integer rtt;

        private Long recvTimestamp;

        public SimpleTraceData() {
        }

        public SimpleTraceData(String dest, String response, Integer hop, Integer rtt, Long recvTimestamp) {
            this.dest = dest;
            this.response = response;
            this.hop = hop;
            this.rtt = rtt;
            this.recvTimestamp = recvTimestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleTraceData that = (SimpleTraceData) o;

            if (dest != null ? !dest.equals(that.dest) : that.dest != null) return false;
            if (response != null ? !response.equals(that.response) : that.response != null) return false;
            if (hop != null ? !hop.equals(that.hop) : that.hop != null) return false;
            if (rtt != null ? !rtt.equals(that.rtt) : that.rtt != null) return false;
            return recvTimestamp != null ? recvTimestamp.equals(that.recvTimestamp) : that.recvTimestamp == null;
        }

        public static short getProtocolId() {
            return PROTOCOL_ID;
        }

        public String getDest() {
            return dest;
        }

        public void setDest(String dest) {
            this.dest = dest;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public Integer getHop() {
            return hop;
        }

        public void setHop(Integer hop) {
            this.hop = hop;
        }

        public Integer getRtt() {
            return rtt;
        }

        public void setRtt(Integer rtt) {
            this.rtt = rtt;
        }

        public Long getRecvTimestamp() {
            return recvTimestamp;
        }

        public void setRecvTimestamp(Long recvTimestamp) {
            this.recvTimestamp = recvTimestamp;
        }
    }


    /**
     * 保证线程安全
     *
     * @param simpleTraceData data
     */
    protected abstract void write(SimpleTraceData simpleTraceData);

    @Override
    public final void write(TraceData traceData) {
        Instant time = traceData.getTime();
        long epochSecond = time.getEpochSecond();
        epochSecond = epochSecond * 1_000_000L + time.getNano();
        write(new SimpleTraceData(traceData.getDest(), traceData.getResponseIp(), traceData.getHop(),
                traceData.getMicrosecondsRTT(), epochSecond));
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

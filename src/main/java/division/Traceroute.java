package division;

import com.zfoo.protocol.IPacket;
import com.zfoo.protocol.registration.anno.Protocol;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author ZT 2022-12-09 21:37
 */
@Protocol
@ToString
public class Traceroute implements IPacket {

    public static final transient short PROTOCOL_ID = 100;
    /**
     * 实际
     */
    public static final transient short ZFOO_SERIALIZE_LENGTH = 40;

    private String ip;
    private String traceroute;
    private Boolean arrived;
    // 纳秒时间戳
    private Long timestamp;

    public Traceroute() {
    }

    public Traceroute(String ip, String traceroute, Boolean arrived, Long timestamp) {
        this.ip = ip;
        this.traceroute = traceroute;
        this.arrived = arrived;
        this.timestamp = timestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTraceroute() {
        return traceroute;
    }

    public void setTraceroute(String traceroute) {
        this.traceroute = traceroute;
    }

    public Boolean getArrived() {
        return arrived;
    }

    public void setArrived(Boolean arrived) {
        this.arrived = arrived;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Traceroute that = (Traceroute) o;

        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (traceroute != null ? !traceroute.equals(that.traceroute) : that.traceroute != null) return false;
        if (arrived != null ? !arrived.equals(that.arrived) : that.arrived != null) return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }


    @Protocol
    public static class SimpleTraceData implements IPacket {

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
     * 便于将构建和拆分都放到一起
     */
    public static class TracerouteBuilder {
        StringBuilder sb;
        public TracerouteBuilder() {
            sb = new StringBuilder(128);
        }

        public Traceroute build(String dest, List<SimpleTraceData> traces, StringBuilder sb) {
            sb.delete(0, sb.length());
            // 按照跳数从近到远排序
            traces.sort(Comparator.comparingInt(SimpleTraceData::getHop));
            // 表示当前要处理第idx跳的数据
            int idx = 1;
            long maxTime = 0L;
            boolean arrived = false;
            for (SimpleTraceData trace : traces) {
                maxTime = Math.max(trace.getRecvTimestamp(), maxTime);
                int hop = trace.getHop();

                while (idx < hop) {
                    sb.append("*|");
                    idx++;
                }
                String response = trace.getResponse();
                sb.append(response);
                sb.append('|');
                if (response.equals(dest)) {
                    arrived = true;
                    break;
                }
                idx++;
            }
            sb.deleteCharAt(sb.length() - 1);
            String tracerouteString = sb.toString();
            return new Traceroute(dest, tracerouteString, arrived, maxTime);
        }

        public Traceroute build(String dest, List<SimpleTraceData> traces) {
            return build(dest, traces, sb);
        }

        /**
         * 将构建出来的Traceroute路径字符串转换成可见跳IP
         * @param traceroute traceroute
         * @return 可见跳IP
         */
        public static List<String> getNonStarHops(Traceroute traceroute) {
            String tracerouteString = traceroute.getTraceroute();
            String[] split = tracerouteString.split("\\|");
            List<String> hops = new ArrayList<>();
            for (String s : split) {
                if (s.equals("*")) {
                    continue;
                }
                hops.add(s);
            }
            return hops;
        }
    }
}

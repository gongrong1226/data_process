package pojo.division;

import com.zfoo.protocol.IPacket;
import com.zfoo.protocol.registration.anno.Protocol;
import lombok.ToString;

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



}

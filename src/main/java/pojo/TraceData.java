package pojo;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

/**
 * @author atractylodis
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Measurement(name = "trace")
public class TraceData extends MeasurementData {
    @Column(name = "host", tag = true)
    private String host;
    @Column(name = "dest_A", tag = true)
    private String destA;
    @Column(name = "dest_B", tag = true)
    private String destB;
    @Column(name = "dest_C", tag = true)
    private String destC;
    @Column(name = "dest_D", tag = true)
    private String destD;
    @Column(name = "protocol", tag = true)
    private String protocol;
    @Column(name = "round", tag = true)
    private Integer round;
    @Column(name = "measurement_prefix", tag = true)
    private String measurementPrefix;
    @Column(name = "RTT")
    private Float rtt;
    @Column(name = "hop", tag = true)
    private Integer hop;
    @Column(name = "response_ip", tag = true)
    private String responseIp;
    @Column(timestamp = true)
    private Instant time;
    @Column(name = "country", tag = true)
    private String country;
    @Column(name = "region", tag = true)
    private String region;
    @Column(name = "city", tag = true)
    private String city;

    @Override
    public String getType() {
        return "trace";
    }

    @Override
    public void setByData(MeasurementData data) {
        logger.info("trace data");
        TraceData traceData = (TraceData) data;
        this.host = traceData.host;
        this.destA = traceData.destA;
        this.destB = traceData.destB;
        this.destC = traceData.destC;
        this.destD = traceData.destD;
        this.protocol = traceData.protocol;
        this.round = traceData.round;
        this.measurementPrefix = traceData.measurementPrefix;
        this.hop = traceData.hop;
        this.responseIp = traceData.responseIp;
        this.rtt = traceData.rtt;
        this.time = traceData.time;
    }

    public String getDest() {
        return destA + "." + destB + "." + destC + "." + destD;
    }

    public int getMicrosecondsRTT() {
        float rtt_ = rtt;
        return (int)(rtt_*1000);
    }
}

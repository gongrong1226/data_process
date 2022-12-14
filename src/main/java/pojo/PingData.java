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
@Measurement(name = "ping")
public class PingData extends MeasurementData {
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
    // 毫秒
    @Column(name = "RTT")
    private Float rtt;
    @Column(timestamp = true)
    private Instant time;
    @Column(name = "country", tag = true)
    private String country;
    @Column(name = "region", tag = true)
    private String region;
    @Column(name = "city", tag = true)
    private String city;

    public String getDest() {
        return destA + "." + destB + "." + destC + "." + destD;
    }

    @Override
    public String getType() {
        return "ping";
    }

    public int getMicrosecondRTT() {
        float rtt_ = rtt;
        return (int) (rtt_ * 1000);
    }

    @Override
    public void setByData(MeasurementData data) {
        logger.info("ping data");
        PingData pingData = (PingData) data;
        this.host = pingData.host;
        this.destA = pingData.destA;
        this.destB = pingData.destB;
        this.destC = pingData.destC;
        this.destD = pingData.destD;
        this.protocol = pingData.protocol;
        this.round = pingData.round;
        this.measurementPrefix = pingData.measurementPrefix;
        this.rtt = pingData.rtt;
        this.time = pingData.time;
    }

}

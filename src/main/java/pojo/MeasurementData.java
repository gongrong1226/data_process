package pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class MeasurementData {
    protected static final Logger logger = LoggerFactory.getLogger(MeasurementData.class);

    public String getType() {
        return "unknown";
    }

    public void setByData(MeasurementData data) {
        if (data.getType().equals("unknown")) {
            logger.warn("unknown data type");
        }
    }

    public String getHost() {
        throw new UnsupportedOperationException();
    }

    public String getDestA() {
        throw new UnsupportedOperationException();
    }

    public String getDestB() {
        throw new UnsupportedOperationException();
    }

    public String getDestC() {
        throw new UnsupportedOperationException();
    }

    public String getDestD() {
        throw new UnsupportedOperationException();
    }

    public String getProtocol() {
        throw new UnsupportedOperationException();
    }

    public String getMeasurementPrefix() {
        throw new UnsupportedOperationException();
    }

    public Instant getTime() {
        throw new UnsupportedOperationException();
    }

    public int getMicrosecondRTT() {
        throw new UnsupportedOperationException();
    }

    public static long timeToNanos(Instant time) {
        long epochSecond = time.getEpochSecond();
        epochSecond = epochSecond * 1_000_000_000L + time.getNano();
        return epochSecond;
    }
}

package data.impl;

import base.Constants;
import data.IPLocation;
import data.ValueResolver;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.MeasurementData;
import pojo.PingData;
import pojo.TraceData;
import transfer.TransferOuterClass;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Map;

public class GRPCValueResolver implements ValueResolver {
    private IPLocation locator;
    private Logger logger = LoggerFactory.getLogger(GRPCValueResolver.class);

    public GRPCValueResolver() {
//        this.locator = new DefaultIPLocation();
        locator = new EmptyIPLocation();
    }

    @Override
    public MeasurementData resolveValues(Object obj) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TransferOuterClass.MeasurementBaseInfo base = null;
        MeasurementData data = null;
        if (obj instanceof TransferOuterClass.TransferPingRequest pingRequest) {
            base = pingRequest.getMeasurementBaseResult();
            data = new PingData();
            data.getClass().getMethod("setRtt", Float.class).invoke(data, pingRequest.getRttMillisecond());
        } else if (obj instanceof TransferOuterClass.TransferTraceRequest traceRequest) {
            base = traceRequest.getMeasurementBaseResult();
            data = new TraceData();
            data.getClass().getMethod("setRtt", Float.class).invoke(data, traceRequest.getRttMillisecond());
            data.getClass().getMethod("setHop", Integer.class).invoke(data, traceRequest.getHop());
            data.getClass().getMethod("setResponseIp", String.class).invoke(data, traceRequest.getResponseIP());
        } else {
            logger.error("");
        }
        if (base != null) {
            String dest = base.getDestination();
            String[] netSeg = dest.split("\\.");
            Map<String, Object> ipInfo = this.locator.LocateBySingleIP(netSeg[0] + "." + netSeg[1] + "." + netSeg[2] + ".0");
            Class<? extends MeasurementData> dataClass = data.getClass();
            dataClass.getMethod("setHost", String.class).invoke(data, base.getHost());
            dataClass.getMethod("setDestA", String.class).invoke(data, netSeg[0]);
            dataClass.getMethod("setDestB", String.class).invoke(data, netSeg[1]);
            dataClass.getMethod("setDestC", String.class).invoke(data, netSeg[2]);
            dataClass.getMethod("setDestD", String.class).invoke(data, netSeg[3]);
            dataClass.getMethod("setProtocol", String.class).invoke(data,
                    Constants.PROTOCOL_MAP[base.getProtocolValue()]);
            dataClass.getMethod("setRound", Integer.class).invoke(data, base.getRound());
            dataClass.getMethod("setMeasurementPrefix", String.class).invoke(data, base.getMeasurementPrefix());
            dataClass.getMethod("setTime", Instant.class).invoke(data, Instant.ofEpochMilli(base.getRecvTimestampMillisecond()));
            dataClass.getMethod("setCountry", String.class).invoke(data, String.valueOf(
                    ipInfo.getOrDefault("country_name", "unknown")));
            dataClass.getMethod("setRegion", String.class).invoke(data, String.valueOf(
                    ipInfo.getOrDefault("region_name", "unknown")));
            dataClass.getMethod("setCity", String.class).invoke(data, String.valueOf(
                    ipInfo.getOrDefault("city_name", "unknown")));
        }
        return data;
    }
}

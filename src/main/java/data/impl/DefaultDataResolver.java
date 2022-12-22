package data.impl;

import base.Constants;
import com.google.protobuf.InvalidProtocolBufferException;
import data.MeasurementDataResolver;
import data.ValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfer.TransferOuterClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Base64;

public class DefaultDataResolver implements MeasurementDataResolver {
    private ValueResolver valueResolver;
    private Logger logger = LoggerFactory.getLogger(DefaultDataResolver.class);

    public DefaultDataResolver() {
        this.valueResolver = new GRPCValueResolver();
    }

    /**
     * TODO move to utility class
     * @param str s
     * @param c c
     * @return s
     */
    public static String customTrim(String str, char c) {
        char[] chars = str.toCharArray();
        int len = chars.length;
        int st = 0;
        while ((st < len) && (chars[st] == c)) {
            st++;
        }

        while ((st < len) && (chars[len - 1] == c)) {
            len--;
        }

        return (st > 0) && (len < chars.length) ? str.substring(st, len) : str;
    }

    @Override
    public Object resolveLineData(String line) {
        Object result = null;
        String[] tags = line.split(",");
        String dataType = tags[0];
        String[] fields = tags[2].split(" ");
        String data = fields[1].split("=")[1];
        // remove " "
        data = customTrim(data, '"');
        byte[] base64Data = Base64.getDecoder().decode(data);
        switch (dataType) {
            case Constants.PING_DATA -> {
                try {
                    TransferOuterClass.TransferPingRequest pingRequest = TransferOuterClass.TransferPingRequest.parseFrom(base64Data);
                    result = valueResolver.resolveValues(pingRequest);
                } catch (InvalidProtocolBufferException e) {
                    logger.error("parse ping data error: " + e.getMessage());
                } catch (InvocationTargetException e) {
                    logger.error("invoke ping's method error: " + e.getMessage());
                } catch (NoSuchMethodException e) {
                    logger.error("it is not ping data method: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    logger.error("permission denied: " + e.getMessage());
                }
            }
            case Constants.TRACE_DATA -> {
                try {
                    TransferOuterClass.TransferTraceRequest traceRequest = TransferOuterClass.TransferTraceRequest.parseFrom(base64Data);
                    result = this.valueResolver.resolveValues(traceRequest);
                } catch (InvalidProtocolBufferException e) {
                    logger.error("parse trace data error: " + e.getMessage());
                } catch (InvocationTargetException e) {
                    logger.error("invoke trace's method error: " + e.getMessage());
                } catch (NoSuchMethodException e) {
                    logger.error("it is not trace data method: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    logger.error("permission denied: " + e.getMessage());
                }
            }
            default -> {
            }
        }
        return result;
    }
}

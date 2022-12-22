package data;

import pojo.MeasurementData;

import java.util.Map;

@FunctionalInterface
public interface MeasurementDataResolver {
    /**
     *  解析失败返回null
     * @param line line
     * @return null if resolve failed
     */
    Object resolveLineData(String line);
}

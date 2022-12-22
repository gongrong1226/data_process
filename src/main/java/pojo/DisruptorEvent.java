package pojo;

import lombok.Data;

@Data
public class DisruptorEvent {
    private MeasurementData data;
    private byte[] originalByte;
}

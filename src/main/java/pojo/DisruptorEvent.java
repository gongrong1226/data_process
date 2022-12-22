package pojo;

import lombok.Data;

@Data
public class DisruptorEvent {
    // TODO non volatile?
    private MeasurementData data;
    private byte[] originalByte;
}

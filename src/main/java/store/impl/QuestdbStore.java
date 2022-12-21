package store.impl;

import com.influxdb.client.write.Point;
import pojo.MeasurementData;
import store.Writeable;

import java.util.List;

/**
 * @author ZT 2022-12-20 21:24
 */
public class QuestdbStore implements Writeable {
    @Override
    public void writeDatasetByPojo(List<MeasurementData> dataset) {

    }

    @Override
    public void writeDatasetByPoint(List<Point> dataset) {

    }

    @Override
    public void writeDataByPojo(MeasurementData data) {

    }

    @Override
    public void writeDataByPoint(Point data) {

    }
}

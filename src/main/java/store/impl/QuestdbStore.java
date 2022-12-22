package store.impl;

import com.influxdb.client.write.Point;
import dao.questdb.QuestASPathCountryCSegCellWriter;
import dao.questdb.QuestIPRTTWriter;
import pojo.MeasurementData;
import store.Writeable;

import java.util.List;

/**
 * @author ZT 2022-12-20 21:24
 */
public class QuestdbStore implements Writeable {

    private QuestIPRTTWriter questIPRTTWriter;

    public QuestdbStore() {
        questIPRTTWriter = new QuestIPRTTWriter(QuestASPathCountryCSegCellWriter.DEFAULT_LOCAL_ADDRESS, "default_ip_rtt_table");
    }

    @Override
    public void writeDatasetByPojo(List<MeasurementData> dataset) {
        questIPRTTWriter.writeDatasetByPojo(dataset);
    }

    @Override
    public void writeDatasetByPoint(List<Point> dataset) {
        questIPRTTWriter.writeDatasetByPoint(dataset);
    }

    @Override
    public void writeDataByPojo(MeasurementData data) {
        questIPRTTWriter.writeDataByPojo(data);
    }

    @Override
    public void writeDataByPoint(Point data) {
        questIPRTTWriter.writeDataByPoint(data);
    }
}

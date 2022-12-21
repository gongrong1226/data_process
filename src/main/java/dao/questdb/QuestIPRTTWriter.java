package dao.questdb;


import com.influxdb.client.write.Point;
import io.questdb.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.MeasurementData;
import pojo.TraceData;
import store.Writeable;

import java.util.List;

/**
 * 非线程安全
 * 一个线程一个实例
 *
 * @author ZT 2022-12-20 22:28
 */
public class QuestIPRTTWriter implements Writeable {

    private final String address;
    private final String table;

    private final Logger logger = LoggerFactory.getLogger(QuestIPRTTWriter.class);

    public static final String DEFAULT_TABLE = "tunis_routers";

    public static QuestIPRTTWriter newDefaultInstance() {
        return new QuestIPRTTWriter(QuestASPathCountryCSegCellWriter.DEFAULT_LOCAL_ADDRESS, DEFAULT_TABLE);
    }

    // 防止线程die了过后，socket连接还没释放
    //private final ThreadLocal<Sender> senderThreadLocal;

    private Sender sender;

    public QuestIPRTTWriter(String address, String table) {
        this.address = address;
        this.table = table;
        sender = Sender.builder().address(address).build();
    }


    @Override
    public void writeDatasetByPojo(List<MeasurementData> dataset) {

    }

    @Override
    public void writeDatasetByPoint(List<Point> dataset) {

    }

    public static String getTableName(MeasurementData data) {
        return data.getMeasurementPrefix() + "_" + data.getType();
    }

    /**
     * 非线程安全 每个线程一个实例去执行
     * TODO 根据注解去设定columns name，以及是否symbol等等
     *
     * @param data data
     */
    @Override
    public void writeDataByPojo(MeasurementData data) {
        int retry = 1;
        while (retry >= 0) {
            retry--;
            String tableName = getTableName(data);
            Sender table = sender.table(tableName);
            table.symbol("destA", data.getDestA())
                    .symbol("destB", data.getDestB())
                    .symbol("destC", data.getDestC())
                    .symbol("destD", data.getDestD());
            if (data instanceof TraceData traceData) {
                table.symbol("hop", String.valueOf(traceData.getHop()));
                table.symbol("response_ip", traceData.getResponseIp());
            }
            // 放后面
            table.symbol("protocol", data.getProtocol())
                    .symbol("host", data.getHost())
                    .longColumn("rtt", data.getMicrosecondRTT());
            long nanos = MeasurementData.timeToNanos(data.getTime());
            try {
                table.at(nanos);
                return;
            } catch (Throwable t) {
                logger.info("data=" + data + "; throwable=" + t);
            } finally {
                // 有可能是连接异常
                sender = Sender.builder().address(address).build();
                logger.info("retry with new connection");
            }
        }
    }

    @Override
    public void writeDataByPoint(Point data) {

    }
}

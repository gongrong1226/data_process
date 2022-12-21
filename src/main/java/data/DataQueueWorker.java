package data;

import base.Constants;
import base.JsonUtil;
import com.lmax.disruptor.WorkHandler;
import dao.TraceDataWriter;
import dao.tracefiledb.TraceDataFileWriter;
import data.impl.DefaultDataComputer;
import data.impl.ThreeLevelDataCollector;
import division.Assessment;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.DisruptorEvent;
import pojo.MeasurementData;
import pojo.PingData;
import pojo.TraceData;
import pojo.compute.City;
import pojo.compute.Country;
import pojo.compute.NetSegment;
import pojo.compute.Region;
import store.Writeable;
import store.impl.InfluxdbStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1. 关于测量任务的数据存储，每隔测量任务的名称就是measurement_prefix, 存放原始数据的table分别为measurement_prefix + "_" + type
 * <p>
 * 2. 一个测量任务如果有trace的话，会每一轮为trace到的routers进行单独的ping, 任务名称为当前measurement_prefix + "_routers"
 * <p>
 * 3. 第2步生成的routers ping任务测量结果也会存放到数据库中，表名为measurement_prefix + "routers_ping"。
 * 在对measurement_prefix这个任务做质量评估时，获取路径历史rtt的时候也是使用的这个table。
 * 另外，针对这些路由器的测量任务，不会触发针对这个任务的集合划分和网络质量评估，所以只有数据存储操作,使用字符串"_routers"做判断。
 * <p>
 * 综上，一个测量任务会生成三个表，分别是：1)mp + "_ping" , 2)mp + "_trace", 3) mp + "routers_ping"
 * <p>
 * 不同measurementPrefix代表不同的測量任务，需要不同的控制
 *
 * @see dao.questdb.QuestIPRTTWriter#getTableName(MeasurementData) for table name of step 1&3
 * @see Assessment#getQuestIPRTTReaderTable(String) for table name of path rtt reading  at judge
 * @see DataQueueWorker#onlyWriteIntoDB(MeasurementData) for ingroring assessment (step 3)
 * @see DataQueueWorker#ROUTERS_MEASUREMENT_SUFFIX for step 2 & 3
 */
public class DataQueueWorker implements WorkHandler<DisruptorEvent> {
    private volatile Map<String, Object> rawData;
    private DataCollector dataCollector;
    private MeasurementDataComputer computer;
    private Lock lock;
    private Writeable writer;
    private Logger logger = LoggerFactory.getLogger(DataQueueWorker.class);

//    private static final TraceDataWriter traceDataWriter = TraceDataFileWriter.defaultTraceDataFileWriter();

    public static final String ROUTERS_MEASUREMENT_SUFFIX = "_routers";

    public DataQueueWorker() {
        this.rawData = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.writer = Writeable.getDefaultWriteable();
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
    }

    public DataQueueWorker(AtomicInteger pingTurn, AtomicInteger traceTurn,
                           Lock lock, Map<String, Object> rawData) {
        this.rawData = rawData;
        this.lock = lock;
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
        this.writer = Writeable.getDefaultWriteable();
    }

    /**
     * 针对这些路由器的测量任务，不会触发针对这个任务的集合划分和网络质量评估，所以只有数据存储操作,使用字符串"_routers"做判断。
     * @param measurementData m
     * @return b
     */
    private boolean onlyWriteIntoDB(MeasurementData measurementData) {
        return measurementData.getMeasurementPrefix().endsWith(ROUTERS_MEASUREMENT_SUFFIX);
    }

    @Override
    public void onEvent(DisruptorEvent event) {
        logger.info("get data: "+event);
        MeasurementData data = event.getData();
        writer.writeDataByPojo(data);
        if (onlyWriteIntoDB(data)) {
            return;
        }
        Assessment assessment = Assessment.getAssessment(data.getMeasurementPrefix());
        if (data.getType().equals(Constants.PING_DATA)) {
            PingData pingData = (PingData) data;
            // 集合划分和评估
            assessment.put(data);
            // 多线程之间，确认是否已经处理过，幂等性，加map。确保一轮数据只有一个线程能进到下面
            RoundControl roundControl = RoundControl.getRoundControl(pingData.getMeasurementPrefix());
            if (roundControl.isNewRound(pingData.getRound())) {
                assessment.assess();
                Map<String, Object> dataMap;
                lock.lock();
                try {
                    dataMap = JsonUtil.mapDeepcopy(this.rawData);
                    this.rawData.clear();
                } finally {
                    lock.unlock();
                }
                this.computer.compute(dataMap);
            }
            lock.lock();
            try {
            //数据分类，分类标准是国家，省份，城市，网段
                String country = pingData.getCountry();
                String region = pingData.getRegion();
                String city = pingData.getCity();
                String ip = pingData.getHost();
                Float rtt = pingData.getRtt();
                String[] split = ip.split("\\.");
                String seg = split[0] + "." + split[1] + "." + split[2] + ".0";
                Object countryCollector = this.rawData.getOrDefault(country, new Country(country, new SynchronizedDescriptiveStatistics()));
                Object regionCollector = this.rawData.getOrDefault(region, new Region(country, region, new SynchronizedDescriptiveStatistics()));
                Object cityCollector = this.rawData.getOrDefault(city, new City(country, region, city, new SynchronizedDescriptiveStatistics()));
                Object segCollector = this.rawData.getOrDefault(seg, new NetSegment(city, region, city, seg, new SynchronizedDescriptiveStatistics()));
                this.dataCollector.collect(rtt, countryCollector, regionCollector, cityCollector, segCollector);
            } finally {
                lock.unlock();
            }
        }
        if (data.getType().equals(Constants.TRACE_DATA)) {
            TraceData traceData = (TraceData) data;
            RoundControl roundControl = RoundControl.getRoundControl(traceData.getMeasurementPrefix());
            // 用作拓扑构建
            assessment.write(traceData);
            if (roundControl.isNewRound(traceData.getRound())) {
                assessment.buildAndClear();
            }
        }
    }
}

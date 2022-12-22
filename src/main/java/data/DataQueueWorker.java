package data;

import base.Constants;
import com.lmax.disruptor.WorkHandler;
import data.impl.DefaultDataComputer;
import data.impl.DefaultDataResolver;
import data.impl.ThreeLevelDataCollector;
import division.Assessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.DisruptorEvent;
import pojo.MeasurementData;
import pojo.PingData;
import pojo.TraceData;
import store.Writeable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1. 关于测量任务的数据存储，每隔测量任务的名称就是measurement_prefix, 存放原始数据的table分别为measurement_prefix + "_" + type
 * <p>
 * 2. 一个测量任务如果有trace的话，会每一轮为trace到的routers进行单独的ping, 任务名称为当前measurement_prefix + "_routers"。
 * 并且这个任务的trace数据也会写到表measurement_prefix + "_routers_ping"当中。
 * 此外还会根据trace数据构建traceroute，写入表m_p + "_traceroute"中
 * <p>
 * 3. 第2步生成的routers ping任务测量结果也会存放到数据库中，表名为measurement_prefix + "routers_ping"。
 * 在对measurement_prefix这个任务做质量评估时，获取路径历史rtt的时候也是使用的这个table。
 * 另外，针对这些路由器的测量任务，不会触发针对这个任务的集合划分和网络质量评估，所以只有数据存储操作,使用字符串"_routers"做判断。
 * <p>
 * 综上，一个测量任务会生成四个表，分别是：1)mp + "_ping" , 2)mp + "_trace", 3) mp + "routers_ping"
 * <p>
 * 不同measurementPrefix代表不同的測量任务，需要不同的控制
 *
 * @see dao.questdb.QuestIPRTTWriter#getTableName(MeasurementData) for table name of step 1&3
 * @see Assessment#getQuestIPRTTReaderTable(String) for table name of path rtt reading  at judge
 * @see DataQueueWorker#onlyWriteIntoDB(MeasurementData) for ingroring assessment (step 3)
 * @see Assessment#ROUTERS_MEASUREMENT_SUFFIX for step 2 & 3
 * @see dao.questdb.QuestTracerouteWriter#getTracerouteTable(String)  for table of step 2
 */
public class DataQueueWorker implements WorkHandler<DisruptorEvent> {
    private volatile Map<String, Object> rawData;
    private DataCollector dataCollector;
    private MeasurementDataComputer computer;
    private Lock lock;
    private Writeable writer;
    private Logger logger = LoggerFactory.getLogger(DataQueueWorker.class);

    /**
     * 线程安全，单实例
     */
    private static final MeasurementDataResolver resolver = new DefaultDataResolver();

//    private static final TraceDataWriter traceDataWriter = TraceDataFileWriter.defaultTraceDataFileWriter();

    public DataQueueWorker() {
        this.rawData = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.writer = Writeable.newDefaultWriteable();
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
    }

    public DataQueueWorker(AtomicInteger pingTurn, AtomicInteger traceTurn,
                           Lock lock, Map<String, Object> rawData) {
        this.rawData = rawData;
        this.lock = lock;
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
        this.writer = Writeable.newDefaultWriteable();
    }

    /**
     * 针对这些路由器的测量任务，不会触发针对这个任务的集合划分和网络质量评估，所以只有数据存储操作,使用字符串"_routers"做判断。
     *
     * @param measurementData m
     * @return b
     */
    private boolean onlyWriteIntoDB(MeasurementData measurementData) {
        return measurementData.getMeasurementPrefix().endsWith(Assessment.ROUTERS_MEASUREMENT_SUFFIX);
    }

    private MeasurementData parseData(byte[] arr) {
        String line = new String(arr);
        Object data = null;
        try {
            data = resolver.resolveLineData(line);
        } catch (Exception e) {
            logger.error("resolver error=" + e + ", byte[] arr=" + new String(arr));
        }
        return (MeasurementData) data;
    }

    private void cleanEvent(DisruptorEvent event) {
        event.setOriginalByte(null);
        event.setData(null);
    }

    @Override
    public void onEvent(DisruptorEvent event) {
        if (event.getOriginalByte() == null) {
            logger.error("event.getOriginalByte() == null");
            return;
        }
        MeasurementData data = parseData(event.getOriginalByte());
        logger.debug("" + data);
        cleanEvent(event);
        if (data == null) {
            logger.error("data == null");
            return;
        }
        writer.writeDataByPojo(data);
        if (onlyWriteIntoDB(data)) {
            return;
        }
        Assessment assessment = Assessment.getAssessment(data.getMeasurementPrefix());
        if (data.getType().equals(Constants.PING_DATA)) {
            PingData pingData = (PingData) data;
            // 多线程之间，确认是否已经处理过，幂等性，加map。确保一轮数据只有一个线程能进到下面
            RoundControl roundControl = RoundControl.getRoundControl(pingData.getMeasurementPrefix());
            if (roundControl.isNewRound(pingData.getRound())) {
                assessment.assess();
//                Map<String, Object> dataMap;
//                lock.lock();
//                try {
//                    dataMap = JsonUtil.mapDeepcopy(this.rawData);
//                    this.rawData.clear();
//                } finally {
//                    lock.unlock();
//                }
//                this.computer.compute(dataMap);
            }
            // 集合划分和评估
            assessment.put(data);
            lock.lock();
            try {
                //数据分类，分类标准是国家，省份，城市，网段
//                String country = pingData.getCountry();
//                String region = pingData.getRegion();
//                String city = pingData.getCity();
//                String ip = pingData.getHost();
//                Float rtt = pingData.getRtt();
//                String[] split = ip.split("\\.");
//                String seg = split[0] + "." + split[1] + "." + split[2] + ".0";
//                Object countryCollector = this.rawData.getOrDefault(country, new Country(country, new SynchronizedDescriptiveStatistics()));
//                Object regionCollector = this.rawData.getOrDefault(region, new Region(country, region, new SynchronizedDescriptiveStatistics()));
//                Object cityCollector = this.rawData.getOrDefault(city, new City(country, region, city, new SynchronizedDescriptiveStatistics()));
//                Object segCollector = this.rawData.getOrDefault(seg, new NetSegment(city, region, city, seg, new SynchronizedDescriptiveStatistics()));
//                this.dataCollector.collect(rtt, countryCollector, regionCollector, cityCollector, segCollector);
            } finally {
                lock.unlock();
            }
        }
        if (data.getType().equals(Constants.TRACE_DATA)) {
            // TODO 生成routers的测量任务
            // TODO 将trace数据写到routers_ping的表中
            TraceData traceData = (TraceData) data;
            RoundControl roundControl = RoundControl.getRoundControl(traceData.getMeasurementPrefix());
            // 新round先清理库存，在此之前一定是所有的该轮次数据已写完。
            // 因为测量代理数据源与数据库之间只有一条连接，数据串行，服务端也是串行处理。一般来说轮次之间都会有间隔，这个间隔大于线程切换的开销。
            if (roundControl.isNewRound(traceData.getRound())) {
                assessment.buildAndClear();
            }
            // 用作拓扑构建
            assessment.write(traceData);
        }
    }
}

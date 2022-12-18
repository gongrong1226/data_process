package data;

import base.Constants;
import base.JsonUtil;
import com.lmax.disruptor.WorkHandler;
import dao.TraceDataWriter;
import dao.tracefiledb.TraceDataFileWriter;
import data.impl.DefaultDataComputer;
import data.impl.ThreeLevelDataCollector;
import division.DefaultDivision;
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
 * 不同measurementPrefix代表不同的測量任务，需要不同的控制
 * TODO 当一个worker可能处理多个数据源时，需要分别控制dispatch
 */
public class DataQueueWorker implements WorkHandler<DisruptorEvent> {
    private volatile AtomicInteger pingTurn;
    private volatile AtomicInteger traceTurn;
    private volatile Map<String, Object> rawData;
    private DataCollector dataCollector;
    private MeasurementDataComputer computer;
    private Lock lock;
    private Writeable writer;
    private Logger logger = LoggerFactory.getLogger(DataQueueWorker.class);

    private static final TraceDataWriter traceDataWriter = TraceDataFileWriter.defaultTraceDataFileWriter();


    public DataQueueWorker() {
        this.rawData = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.writer = new InfluxdbStore();
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
    }

    public DataQueueWorker(AtomicInteger pingTurn, AtomicInteger traceTurn,
                           Lock lock, Map<String, Object> rawData) {
        this.rawData = rawData;
        this.lock = lock;
        this.dataCollector = new ThreeLevelDataCollector();
        this.computer = new DefaultDataComputer();
        this.writer = new InfluxdbStore();
    }

    @Override
    public void onEvent(DisruptorEvent event) {
        logger.info("get data: "+event);
        MeasurementData data = event.getData();
        writer.writeDataByPojo(data);
        if (data.getType().equals(Constants.PING_DATA)) {
            PingData pingData = (PingData) data;
            // IP集合划分
            DefaultDivision.getDefaultDivision().divide(pingData);
            // 多线程之间，确认是否已经处理过，幂等性，加map
            RoundControl roundControl = RoundControl.getRoundControl(pingData.getMeasurementPrefix());
            if (roundControl.isNewRound(pingData.getRound())) {
                Map<String, Object> dataMap;
                lock.lock();
                try {
                    dataMap = JsonUtil.mapDeepcopy(this.rawData);
                    this.rawData.clear();
                } finally {
                    lock.unlock();
                }
                //计算
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
                this.dataCollector.collect(rtt,countryCollector, regionCollector, cityCollector, segCollector);
            } finally {
                lock.unlock();
            }
        }
        if (data.getType().equals(Constants.TRACE_DATA)) {
            TraceData traceData = (TraceData) data;
            RoundControl roundControl = RoundControl.getRoundControl(traceData.getMeasurementPrefix());
            // 用作拓扑构建
            traceDataWriter.write(traceData);
            if (roundControl.isNewRound(traceData.getRound())) {
                traceDataWriter.buildAndClear();
            }
        }
    }
}

package data.impl;

import base.StatisticUtil;
import com.influxdb.client.write.Point;
import data.MeasurementDataComputer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.compute.City;
import pojo.compute.Country;
import pojo.compute.NetSegment;
import pojo.compute.Region;
import store.Writeable;
import store.impl.InfluxdbStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultDataComputer implements MeasurementDataComputer {
    private Lock lock;
    private Writeable store;
    private Logger logger = LoggerFactory.getLogger(DefaultDataComputer.class);

    public DefaultDataComputer() {
        this.lock = new ReentrantLock();
        this.store = Writeable.getDefaultWriteable();
    }

    @Override
    public void compute(Map<String, Object> data) {
        data.forEach((key, value) -> {
            SynchronizedDescriptiveStatistics collector = null;
            Map<String, String> tags = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            if (value instanceof NetSegment segment) {
                collector = segment.getCollector();
                tags.put("country", segment.getCountry());
                tags.put("region", segment.getRegion());
                tags.put("city", segment.getCity());
                tags.put("net_id", segment.getNetId());
            } else if (value instanceof City city) {
                collector = city.getCollector();
                tags.put("country", city.getCountry());
                tags.put("region", city.getRegion());
                tags.put("city", city.getCity());
            } else if (value instanceof Region region) {
                collector = region.getCollector();
                tags.put("country", region.getCountry());
                tags.put("region", region.getRegion());
            } else if (value instanceof Country country) {
                collector = country.getCollector();
                tags.put("country", country.getCountry());
            } else {
                logger.error("");
            }
            double[][] stat = this.getStat(collector, 100);
            if (stat == null) {
                return;
            }
            double[] base = stat[0];
            fields.put("mean", base[0]);
            fields.put("std", base[1]);
            fields.put("skew", base[2]);
            fields.put("kurt", base[3]);
            double[] percentiles = stat[1];
            for (int i = 0; i < percentiles.length; i++) {
                fields.put("percentile_"+i, percentiles[i]);
            }
            Point point = new Point("ping_stat");
            point.addTags(tags);
            point.addFields(fields);
            //数据写数据库
            this.store.writeDataByPoint(point);
        });
    }

    private double[][] getStat(DescriptiveStatistics collector, int n) {
        if (collector == null) {
            logger.error("");
            return null;
        }
        //平均值
        double mean = collector.getMean();
        //标准差
        double std = collector.getStandardDeviation();
        //偏度
        double skew = collector.getSkewness();
        //峰度
        double kurt = collector.getKurtosis();
        //分位数
        double[] percentiles = StatisticUtil.computeMultiPercentile(collector, n);
        return new double[][]{{mean, std, skew, kurt}, percentiles};
    }
}

package division;

import dao.*;
import dao.asndb.ASNFileDB;
import dao.questdb.QuestASPathCountryCSegCellWriter;
import dao.questdb.QuestIPRTTReader;
import dao.questdb.QuestTracerouteReader;
import dao.questdb.QuestTracerouteWriter;
import dao.tracefiledb.TraceDataFileWriter;
import division.cell.Cell;
import division.cell.MedianRTTCalculator;
import division.judge.BlameIt;
import division.judge.CellThresholdGetter;
import division.path.ASPathRTTGetterUseLastIP;
import division.path.PathRTTGetter;
import division.path.PathRTTGetterBuilder;
import division.strategy.ASPathCountryCSegCellLocator;
import division.strategy.CellLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.MeasurementData;
import pojo.TraceData;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ZT 2022-12-19 11:01
 */
public class Assessment implements TraceDataWriter {

    private final Division division;

    private final Judge judge;

    private final CellWriter cellWriter;

    private final TraceDataWriter traceDataWriter;

    private static final String TRACE_DATA_TMP_DIR = "/tmp/trace_data";

    private static final Logger logger = LoggerFactory.getLogger(Assessment.class);
    /**
     * measurement_prefix, Assessment
     */
    private static final ConcurrentMap<String, Assessment> assessmentConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * 这里加的"_ping"和 写入数据时的prefix + "_" + type有关，也与transfer侧设定的type字符串ping/trace 相关
     *
     * @param measurementPrefix measurementPrefix
     * @return table name
     */
    public static String getQuestIPRTTReaderTable(String measurementPrefix) {
        return measurementPrefix + "_routers_ping";
    }

    public static String getTraceDataPath(String measurementPrefix) {
        Path traceDataPath = Path.of(TRACE_DATA_TMP_DIR, measurementPrefix);
        return traceDataPath.toAbsolutePath().toString();
    }

    /**
     * TODO 根据measurementPrefix去获取URL, 等等以及各种策略配置
     *
     * @param measurementPrefix measurementPrefix
     * @return Assessment
     */
    public static Assessment getAssessment(String measurementPrefix) {
        Assessment assessment = assessmentConcurrentHashMap.get(measurementPrefix);
        if (assessment == null) {
            // 确保对每一个measurementPrefix只初始化一个Assessment，避免socket等资源浪费
            synchronized (Assessment.class) {
                // 有map中的value是volatile，无问题
                assessment = assessmentConcurrentHashMap.get(measurementPrefix);
                if (assessment != null) {
                    return assessment;
                }
                logger.info("Creating a new Assessment for measurementPrefix=" + measurementPrefix);
                // step 1 先要有历史traceroute数据和IP接口级拓扑，才能对IP集合划分
                // step 1.1 这一步用来生成这些数据构建出的实时traceroute的存放
                String tracerouteWriteTable = TracerouteWriter.getTracerouteTable(measurementPrefix);
                String questTracerouteWriteAddress = QuestTracerouteWriter.DEFAULT_LOCAL_ADDRESS;
                TracerouteWriter questTracerouteWriter = new QuestTracerouteWriter(questTracerouteWriteAddress,
                        tracerouteWriteTable);
                // 将Trace数据转换为traceroute的方法，以及存放数据库
                String traceDataPath = getTraceDataPath(measurementPrefix);
                TraceDataFileWriter traceDataFileWriter = new TraceDataFileWriter(questTracerouteWriter, traceDataPath);
                logger.info(String.format("...Created TraceDataFileWriter, questTracerouteWriteTable=%s, questTracerouteWriteAddress=%s, traceDataPath=%s",
                        tracerouteWriteTable, questTracerouteWriteAddress, traceDataPath));

                // step 1.2 这一步用来指明从哪里获取这些数据
                String tracerouteReadTable = TracerouteReader.getTracerouteTable(measurementPrefix);
                String questTracerouteReaderURL = QuestTracerouteReader.DEFAULT_URL;
                TracerouteReader questTracerouteReader = new QuestTracerouteReader(questTracerouteReaderURL, tracerouteReadTable);
                logger.info(String.format("...Created TracerouteReader, questTracerouteReadTable=%s, questTracerouteReaderURL=%s",
                        tracerouteReadTable, questTracerouteReaderURL));

                // step 2 数据源有了，现在可以将实时Ping数据进行集合划分。划分规则CellLocator需要自定义，这里使用了ASPath-Country-C段来划分
                ASNFileDB asnFileDB = ASNFileDB.getDefaultASNFileDB();
                CellLocator asPathCountryCSegCellLocator = new ASPathCountryCSegCellLocator(asnFileDB);
                DefaultDivision defaultDivision = new DefaultDivision(questTracerouteReader, asPathCountryCSegCellLocator);
                logger.info(String.format("...Created DefaultDivision, CellLocator=%s",
                        asPathCountryCSegCellLocator.getClass()));

                // step 3 评估
                // step 3.1 先确定评估结果要写到哪里去
                String questASPathCountryCSegCellWriteAddress = QuestASPathCountryCSegCellWriter.DEFAULT_LOCAL_ADDRESS;
                String questASPathCountryCSegCellWriteTable = QuestASPathCountryCSegCellWriter.getTable(measurementPrefix);
                CellWriter cellWriter = new QuestASPathCountryCSegCellWriter(questASPathCountryCSegCellWriteAddress,
                        questASPathCountryCSegCellWriteTable);
                logger.info(String.format("...Created CellWriter, CellWriter=%s, questASPathCountryCSegCellWriteAddress=%s, questASPathCountryCSegCellWriteTable=%s",
                        cellWriter.getClass(), questASPathCountryCSegCellWriteAddress, questASPathCountryCSegCellWriteTable));

                // step 3.2 评估时，每个Path的rtt获取方法以及rtt数据源
                // rtt数据源使用routers的。对routers ip rtt的获取
                String questIPRTTReaderTable = getQuestIPRTTReaderTable(measurementPrefix);
                String questIPRTTReaderURL = QuestIPRTTReader.DEFAULT_URL;
                IPRTTReader questIPRTTReader = new QuestIPRTTReader(questIPRTTReaderURL, questIPRTTReaderTable);
                // step 3.3 对AS Path的rtt具体获取以及计算方法
                PathRTTGetter asPathRTTGetterUseLastIP = new ASPathRTTGetterUseLastIP(questIPRTTReader, new MedianRTTCalculator());
                PathRTTGetter pathRTTGetter = new PathRTTGetterBuilder().addLast(asPathRTTGetterUseLastIP).build();
                // step 3.4 包含τ和每个单元格的阈值，以及as path的rtt的获取及计算
                double tau = 0.8;
                // 每一个cell的阈值都是200*1000微秒
                CellThresholdGetter cellThresholdGetter = cell -> 200 * 1000;
                BlameIt blameIt = new BlameIt(tau, cellThresholdGetter, pathRTTGetter);
                logger.info("...Created BlameIt, tau=" + tau);
                Assessment newAssessment = new Assessment(defaultDivision, blameIt, cellWriter, traceDataFileWriter);
                assessment = assessmentConcurrentHashMap.putIfAbsent(measurementPrefix, newAssessment);
                logger.info("Created Assessment.");
                if (assessment == null) {
                    assessment = newAssessment;
                }
            }
        }
        return assessment;
    }



    public Assessment(Division division, Judge judge, CellWriter cellWriter, TraceDataWriter traceDataWriter) {
        this.division = division;
        this.judge = judge;
        this.cellWriter = cellWriter;
        this.traceDataWriter = traceDataWriter;
    }

    public void put(MeasurementData data) {
        division.divide(data);
    }

    public void assess() {
        List<Cell> cells = division.resetDivision();
        cells = judge.adjudge(cells);
        cellWriter.write(cells);
    }


    @Override
    public void write(TraceData traceData) {
        traceDataWriter.write(traceData);
    }

    @Override
    public void flush() {
        traceDataWriter.flush();
    }

    @Override
    public void buildAndClear() {
        traceDataWriter.buildAndClear();
    }
}

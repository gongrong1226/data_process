package division;

import dao.TracerouteReader;
import division.strategy.CellLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.MeasurementData;
import pojo.PingData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZT 2022-12-06 16:49
 */
public abstract class AbstractDivision implements Division {

    private final Logger logger = LoggerFactory.getLogger(AbstractDivision.class);

    protected TracerouteReader tracerouteReader;

    protected CellLocator cellLocator;

    private final AtomicReference<ConcurrentMap<String, Cell>> cellsMapHolder;

    public AbstractDivision(TracerouteReader tracerouteReader, CellLocator cellLocator) {
        this.tracerouteReader = tracerouteReader;
        this.cellLocator = cellLocator;
        this.cellsMapHolder = new AtomicReference<>(new ConcurrentHashMap<>());
    }

    /**
     * 根据Traceroute和IP定位进行划分
     *
     * @param data 测量数据
     */
    @Override
    public void divide(MeasurementData data) {
        if (!(data instanceof PingData pingData)) {
            logger.error(data + " is not ping data type");
            return;
        }
        // 获取这个IP的最近n天Traceroute，不止一条
        List<Object> traceroutes = tracerouteReader.readTraceroute(pingData.getDest());

        // 根据traceroute和ping数据、IP信息定位到cell
        List<String> keys = cellLocator.locate(traceroutes, pingData);
        ConcurrentMap<String, Cell> cellMap = cellsMapHolder.get();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Cell cell = cellMap.get(key);
            if (cell == null) {
                cell = cellLocator.newCellForCellKey(traceroutes.get(i), pingData);
                cellMap.putIfAbsent(key, cell);
                cell = cellMap.get(key);
            }
            cell.addRtt(pingData.getDest(), pingData.getMicrosecondRTT());
        }
    }

    @Override
    public List<Cell> resetDivision() {
        ConcurrentMap<String, Cell> stringCellConcurrentMap = cellsMapHolder.getAndSet(new ConcurrentHashMap<>());
        return new ArrayList<>(stringCellConcurrentMap.values());
    }

    @Override
    public List<Cell> getAllCells() {
        ConcurrentMap<String, Cell> stringCellConcurrentMap = cellsMapHolder.get();
        Collection<Cell> values = stringCellConcurrentMap.values();
        return new ArrayList<>(values);
    }
}

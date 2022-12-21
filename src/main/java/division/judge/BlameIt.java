package division.judge;

import division.cell.Cell;
import division.Judge;
import division.path.PathRTTGetter;
import division.path.PrintablePath;

import java.util.*;

/**
 * @author ZT 2022-12-17 13:16
 */
public class BlameIt implements Judge {

    /**
     * 故障归因阈值τ
     */
    private double tau;

    /**
     * 单元格的阈值获取
     */
    private CellThresholdGetter thresholdGetter;

    /**
     * 路径的RTT获取
     */
    private PathRTTGetter pathRTTGetter;

    public BlameIt(double tau, CellThresholdGetter thresholdGetter, PathRTTGetter pathRTTGetter) {
        this.tau = tau;
        this.thresholdGetter = thresholdGetter;
        this.pathRTTGetter = pathRTTGetter;
    }

    private Set<String> extractPaths(List<Cell> cellList) {
        Set<String> pathStrings = new HashSet<>();
        for (Cell cell : cellList) {
            pathStrings.add(cell.getPath().getPathString());
        }
        return pathStrings;
    }

    private boolean RTTFault(Cell cell) {
        int expectedRTT = cell.getStatisticRTT();
        int threshold = thresholdGetter.getThreshold(cell);
        return expectedRTT > threshold;
    }

    /**
     * 按照算法来写
     *
     * @param cellList cells
     * @return cells
     */
    @Override
    public List<Cell> adjudge(List<Cell> cellList) {
        Set<String> pathStrings = extractPaths(cellList);
        Map<String, Integer> pathNum = new HashMap<>();
        Map<String, Integer> pathBadCnt = new HashMap<>();
        Map<String, Double> pathBadRatio = new HashMap<>();
        // 统计经过path的总数以及故障的数量
        for (Cell c : cellList) {
            PrintablePath path = c.getPath();
            String pathString = path.getPathString();
            Integer orDefault = pathNum.getOrDefault(pathString, 0);
            pathNum.put(pathString, orDefault + 1);
            if (c.getStatisticRTT() > pathRTTGetter.getRTT(path)) {
                orDefault = pathBadCnt.getOrDefault(pathString, 0);
                pathBadCnt.put(pathString, orDefault + 1);
            }
        }
        // 计算比例
        for (String pathString : pathStrings) {
            Integer num = pathNum.get(pathString);
            Integer bad = pathBadCnt.getOrDefault(pathString, 0);
            pathBadRatio.put(pathString, bad * 1.0 / num);
        }

        // 归因
        for (Cell c : cellList) {
            if (!RTTFault(c)) {
                continue;
            }
            String pathString = c.getPath().getPathString();
            Integer integer = pathNum.get(pathString);
            if (integer == null || integer < 5) {
                c.setFaultCause(Cell.FaultCause.INSUFFICIENT);
            } else if (pathBadRatio.getOrDefault(pathString, 0.0) > tau) {
                c.setFaultCause(Cell.FaultCause.MIDDLE);
            } else {
                c.setFaultCause(Cell.FaultCause.CLIENT);
            }
        }
        return cellList;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double tau) {
        this.tau = tau;
    }

    public CellThresholdGetter getThresholdGetter() {
        return thresholdGetter;
    }

    public void setThresholdGetter(CellThresholdGetter thresholdGetter) {
        this.thresholdGetter = thresholdGetter;
    }

    public PathRTTGetter getPathRTTGetter() {
        return pathRTTGetter;
    }

    public void setPathRTTGetter(PathRTTGetter pathRTTGetter) {
        this.pathRTTGetter = pathRTTGetter;
    }
}

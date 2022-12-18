package division.judge;

import division.cell.Cell;

/**
 * 获取单元格阈值
 * 可能不同单元格拥有不同阈值
 *
 * @author ZT 2022-12-17 13:27
 */
public interface CellThresholdGetter {

    /**
     * 获取单元格的阈值
     *
     * @param cell 单元格
     * @return RTT阈值，单位微秒
     */
    int getThreshold(Cell cell);

}

package division;

import division.Cell;
import pojo.MeasurementData;

import java.util.List;

/**
 * 将一个IP划分到对应的集合当中
 *
 * @author ZT 2022-12-06 15:27
 */
public interface Division {

    /**
     * 输入实时数据流，并将这个IP和RTT放到对应的单元格当中
     *
     * @param data 测量数据
     */
    void divide(MeasurementData data);

    /**
     * 获取当前所有已经划分出来的单元格
     *
     * @return 所有单元格
     */
    List<Cell> getAllCells();

    /**
     * 清空当前的划分和RTT累积，并且返回所有已经划分出来的单元格
     * @return 所有单元格
     */
    List<Cell> resetDivision();
}

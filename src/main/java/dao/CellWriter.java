package dao;

import division.cell.Cell;

import java.util.List;

/**
 * 存放cell
 * @author ZT 2022-12-17 21:29
 */
public interface CellWriter {


    /**
     * 写入cell
     * @param cell cell
     */
    void write(Cell cell);

    void write(List<Cell> cells);

}

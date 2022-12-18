package division;

import division.cell.Cell;

import java.util.List;

/**
 * 故障评判
 *
 * @author ZT 2022-12-16 17:56
 */
public interface Judge {

    List<Cell> adjudge(List<Cell> cellList);

}

package dao.questdb;

import dao.CellWriter;
import division.cell.ASPathCountryCSegCell;
import division.cell.Cell;
import division.path.Traceroute;
import io.questdb.client.Sender;

import java.util.List;

/**
 * 只写入ASPathCountryCSegCell
 *
 * @author ZT 2022-12-17 21:31
 */
public class QuestASPathCountryCSegCellWriter implements CellWriter {


    public static final String DEFAULT_LOCAL_ADDRESS = QuestTracerouteWriter.DEFAULT_LOCAL_ADDRESS;
    public static final String DEFAULT_TABLE = "tunis_aspath_country_cseg_cell";

    private final String address;
    private final String table;

    public static QuestASPathCountryCSegCellWriter newDefaultInstance() {
        return new QuestASPathCountryCSegCellWriter(DEFAULT_LOCAL_ADDRESS, DEFAULT_TABLE);
    }

    public QuestASPathCountryCSegCellWriter(String address, String table) {
        this.address = address;
        this.table = table;
    }


    @Override
    public void write(List<Cell> cells) {
        try (Sender sender = Sender.builder().address(address).build()) {
            for (Cell cell : cells) {
                if (!(cell instanceof ASPathCountryCSegCell asPathCountryCSegCell)) {
                    continue;
                }
                Cell.FaultCause faultCause = asPathCountryCSegCell.getFaultCause();
                sender.table(table)
                        .symbol("as_path", asPathCountryCSegCell.getPath().getPathString())
                        .symbol("country", asPathCountryCSegCell.getCountry())
                        .symbol("c_segment", asPathCountryCSegCell.getcSegment())
                        .stringColumn("fault_cause", faultCause==null ? "" : faultCause.toString())
                        .at(asPathCountryCSegCell.getTimestampNano());
            }
        }
    }

    @Override
    public void write(Cell cell) {
        try (Sender sender = Sender.builder().address(address).build()) {
            if (!(cell instanceof ASPathCountryCSegCell asPathCountryCSegCell)) {
                return;
            }
            Cell.FaultCause faultCause = asPathCountryCSegCell.getFaultCause();
            sender.table(table)
                    .symbol("as_path", asPathCountryCSegCell.getPath().getPathString())
                    .symbol("country", asPathCountryCSegCell.getCountry())
                    .symbol("c_segment", asPathCountryCSegCell.getcSegment())
                    .stringColumn("fault_cause", faultCause==null ? "" : faultCause.toString())
                    .at(asPathCountryCSegCell.getTimestampNano());
        }
    }
}

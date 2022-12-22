package dao.questdb;

import dao.TracerouteWriter;
import division.path.Traceroute;
import io.questdb.client.Sender;

import java.util.List;

/**
 * CREATE TABLE 'tunis_traceroute' (
 * ip SYMBOL capacity 33554432 index capacity 256,
 * traceroute STRING,
 * ts TIMESTAMP
 * ) timestamp (ts);
 *
 * @author ZT 2022-12-09 22:32
 */
public class QuestTracerouteWriter implements TracerouteWriter {

    public static final String DEFAULT_LOCAL_ADDRESS = "localhost:9009";
    public static final String DEFAULT_TRACEROUTE_TABLE = QuestTracerouteReader.TUNIS_TRACEROUTE_TABLE;

    private String address;
    private String table;


    public QuestTracerouteWriter(String address, String table) {
        this.address = address;
        this.table = table;
    }


    @Override
    public void write(Traceroute traceroute) {
        write(List.of(traceroute));
    }

    /**
     * TODO 后期修改，现在先写死column
     *
     * @param traceroutes traceroute
     */
    @Override
    public void write(List<Traceroute> traceroutes) {
        try (Sender sender = Sender.builder().address(address).build()) {
            for (Traceroute traceroute : traceroutes) {
                String ip = traceroute.getIp();
                String[] split = ip.split("\\.");
                sender.table(table)
                        .symbol(QuestTracerouteReader.DEST_A_COLUMN, split[0])
                        .symbol(QuestTracerouteReader.DEST_B_COLUMN, split[1])
                        .symbol(QuestTracerouteReader.DEST_C_COLUMN, split[2])
                        .symbol(QuestTracerouteReader.DEST_D_COLUMN, split[3])
                        .stringColumn(QuestTracerouteReader.TRACEROUTE_COLUMN, traceroute.getTraceroute())
                        .boolColumn(QuestTracerouteReader.ARRIVED_COLUMN, traceroute.getArrived())
                        .at(traceroute.getTimestamp());
            }
        }
    }
}

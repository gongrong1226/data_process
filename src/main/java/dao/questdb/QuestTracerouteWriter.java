package dao.questdb;

import dao.TracerouteWriter;
import io.questdb.client.Sender;
import pojo.division.Traceroute;

import java.util.List;

/**
 * @author ZT 2022-12-09 22:32
 */
public class QuestTracerouteWriter implements TracerouteWriter {

    private String address;
    private String table;

    public QuestTracerouteWriter(String address, String table) {
        this.address = address;
        this.table = table;
    }


    @Override
    public void write(Traceroute traceroute) {
        try (Sender sender = Sender.builder().address("localhost:9009").build()) {
            sender.table(table)
                    .symbol("ip", traceroute.getIp())
                    .stringColumn("traceroute", traceroute.getTraceroute())
                    .boolColumn("arrived", traceroute.getArrived())
                    .at(traceroute.getTimestamp());
        }
    }

    /**
     * TODO 后期修改，现在先写死column
     *
     * @param traceroutes traceroute
     */
    @Override
    public void write(List<Traceroute> traceroutes) {
        try (Sender sender = Sender.builder().address("localhost:9009").build()) {
            for (Traceroute traceroute : traceroutes) {
                sender.table(table)
                        .symbol("ip", traceroute.getIp())
                        .stringColumn("traceroute", traceroute.getTraceroute())
                        .boolColumn("arrived", traceroute.getArrived())
                        .at(traceroute.getTimestamp());
            }
        }
    }
}

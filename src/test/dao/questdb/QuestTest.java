package dao.questdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import data.impl.DefaultDataResolver;
import data.impl.GRPCValueResolver;
import division.DefaultDivisionTest;
import division.cell.ASPathCountryCSegCell;
import division.cell.Cell;
import division.path.ASPath;
import org.junit.Test;
import pojo.PingData;

import java.util.List;

import static division.DefaultDivisionTest.getASPath1;

/**
 * @author ZT 2022-12-19 23:37
 */
public class QuestTest {
    private String writeAddress = "101.251.218.10:9009";
    private String readURL = "jdbc:postgresql://101.251.218.10:8812/qdb";

    @Test
    public void TestQuestASPathCountryCSegCellWriter() {
        QuestASPathCountryCSegCellWriter test_table = new QuestASPathCountryCSegCellWriter(writeAddress, "test_table");

        DefaultDivisionTest.ASPathAndTraceroute asPath1 = getASPath1();
        ASPath asPath = asPath1.asPath;
        List<Cell> asPathCountryCSegCells = List.of(new ASPathCountryCSegCell(asPath, "1", "1.0.0", 0L),
                new ASPathCountryCSegCell(asPath, "1", "1.0.1",0L));
        asPathCountryCSegCells.get(0).addRtt("1.0.0.1", 1000);
        asPathCountryCSegCells.get(0).addRtt("1.0.0.2", 2000);
        asPathCountryCSegCells.get(1).addRtt("1.0.1.1", 1000);
        test_table.write(asPathCountryCSegCells);
    }

    @Test
    public void TestQuestIPRTTReader() {
        QuestIPRTTReader test_routers = new QuestIPRTTReader(readURL, "tunis_routers");
        List<Integer> integers = test_routers.queryRTT("193.251.240.103");
        System.out.println(integers);
    }


    @Test
    public void TestInfluxdbCreator() {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://101.251.218.10:9009", "7o6XopexyclzY8XhbCI5LTR9dmmNE1L4dmW8Iu5EIS0qfkLDV3uq884WeIw0jvfxPJuoUjlXGp46uZqGGK78Rw==".toCharArray(),
                "club203", "club203");
        WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();
        PingData pingData = new PingData();
        DefaultDataResolver grpcValueResolver = new DefaultDataResolver();
        Object o = grpcValueResolver.resolveLineData("ping,host=cb52f4c932cb,influx_data_bucket=FK-SDZX-EasternEurope-Origin proto_base64=CiwI2dLYiMUwGgs4My4xOS42Ny40OSABKAUyEkVhc3Rlcm5FdXJvcGUtUGluZxU9CkFC 1667807805236177620");

        writeApiBlocking.writeMeasurement(WritePrecision.NS, o);
    }
}

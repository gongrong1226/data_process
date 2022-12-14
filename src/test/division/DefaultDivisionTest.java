package division;

import dao.TracerouteReader;
import division.cell.ASPathCountryCSegCell;
import org.junit.Assert;
import org.junit.Test;
import pojo.PingData;
import pojo.division.AS;
import pojo.division.ASPath;
import pojo.division.Traceroute;

import java.util.*;

public class DefaultDivisionTest {

    DefaultDivision defaultDivision;
    List<Cell> expectedCells;
    List<PingData> pingData;

    private static PingData newPingData(String IP, int rtt, String country) {
        String[] split = IP.split("\\.");
        PingData pingData = new PingData();
        pingData.setDestA(split[0]);
        pingData.setDestB(split[1]);
        pingData.setDestC(split[2]);
        pingData.setDestD(split[3]);
        pingData.setRtt((float) (rtt / 1000));
        pingData.setCountry(country);
        return pingData;
    }

    private static class ASPathAndTraceroute {
        ASPath asPath;
        String traceroutes;

        public ASPathAndTraceroute(ASPath asPath, String traceroutes) {
            this.asPath = asPath;
            this.traceroutes = traceroutes;
        }
    }

    public static ASPathAndTraceroute getASPath1() {
        String ip1 = "189.157.71.1";
        String ip2 = "189.157.71.2";
        String ip3 = "189.157.71.3";
        AS as1 = new AS(8151, ip1, ip3, 0);

        String ip11 = "189.194.29.1";
        String ip12 = "189.194.29.2";
        AS as2 = new AS(13999, ip11, ip12, 0);

        String ip21 = "189.195.39.255";
        AS as3 = new AS(28481, ip21, ip21, 0);
        List<String> strings = List.of("192.168.0.1",
                ip1,
                ip2,
                "192.168.0.2",
                ip3,
                ip11,
                ip12,
                ip21);
        String tracerouteString = String.join("|", strings);

        List<Traceroute> traceroutes = List.of(
                new Traceroute("1.0.0.1", tracerouteString, true, 0L),
                new Traceroute("1.0.0.2", tracerouteString, true, 0L),
                new Traceroute("1.0.1.1", tracerouteString, true, 0L)
        );
        List<AS> as11 = List.of(as1, as2, as3);
        ASPath asPath = new ASPath(as11);
        return new ASPathAndTraceroute(asPath, tracerouteString);
    }

    public static ASPathAndTraceroute getASPath2() {
        String ip1 = "1.20.118.1";
        String ip2 = "1.20.118.2";
        String ip3 = "1.20.118.3";
        AS as1 = new AS(23969, ip1, ip3, 0);

        String ip11 = "1.22.236.25";
        String ip12 = "1.22.236.26";
        String ip13 = "1.22.236.27";
        AS as2 = new AS(45528, ip11, ip13, 0);

        String ip21 = "1.32.218.2";
        String ip22 = "1.32.218.3";
        AS as3 = new AS(64050, ip21, ip22, 0);
        List<String> strings = List.of("192.168.0.1",
                ip1,
                ip2,
                "192.168.0.2",
                ip3,
                ip11,
                ip12,
                ip13,
                ip21,
                ip22,
                "192.168.0.1");
        String tracerouteString = String.join("|", strings);

        List<AS> as11 = List.of(as1, as2, as3);
        ASPath asPath = new ASPath(as11);
        return new ASPathAndTraceroute(asPath, tracerouteString);
    }


    public DefaultDivisionTest() {
        defaultDivision = DefaultDivision.getDefaultDivision();
        expectedCells = new ArrayList<>();
        pingData = new ArrayList<>();
        ASPathAndTraceroute asPath1 = getASPath1();
        ASPath asPath = asPath1.asPath;
        String tracerouteString = asPath1.traceroutes;
        List<Traceroute> traceroutes1 = new ArrayList<>(List.of(
                new Traceroute("1.0.0.1", tracerouteString, true, 0L),
                new Traceroute("1.0.0.2", tracerouteString, true, 0L),
                new Traceroute("1.0.1.1", tracerouteString, true, 0L)
        ));
        List<ASPathCountryCSegCell> asPathCountryCSegCells = List.of(new ASPathCountryCSegCell(asPath, "1", "1.0.0"),
                new ASPathCountryCSegCell(asPath, "1", "1.0.1"));
        asPathCountryCSegCells.get(0).addRtt("1.0.0.1", 1000);
        pingData.add(newPingData("1.0.0.1", 1000, "1"));
        asPathCountryCSegCells.get(0).addRtt("1.0.0.2", 2000);
        pingData.add(newPingData("1.0.0.2", 2000, "1"));
        asPathCountryCSegCells.get(1).addRtt("1.0.1.1", 1000);
        pingData.add(newPingData("1.0.1.1", 1000, "1"));
        expectedCells.addAll(asPathCountryCSegCells);

        //
        ASPathAndTraceroute asPath2 = getASPath2();
        asPath = asPath2.asPath;
        tracerouteString = asPath2.traceroutes;
        traceroutes1.addAll(List.of(
                new Traceroute("2.0.0.1", tracerouteString, true, 0L),
                new Traceroute("2.0.0.2", tracerouteString, true, 0L),
                new Traceroute("2.0.1.1", tracerouteString, true, 0L)
        ));
        List<ASPathCountryCSegCell> asPathCountryCSegCells1 = List.of(new ASPathCountryCSegCell(asPath, "1", "2.0.0"),
                new ASPathCountryCSegCell(asPath, "2", "2.0.1"));
        asPathCountryCSegCells1.get(0).addRtt("2.0.0.1", 1000);
        pingData.add(newPingData("2.0.0.1", 1000, "1"));
        asPathCountryCSegCells1.get(0).addRtt("2.0.0.2", 2000);
        pingData.add(newPingData("2.0.0.2", 2000, "1"));
        asPathCountryCSegCells1.get(1).addRtt("2.0.1.1", 1000);
        pingData.add(newPingData("2.0.1.1", 1000, "2"));
        expectedCells.addAll(asPathCountryCSegCells1);


        Map<String, List<Object>> map = new HashMap<>();
        for (Traceroute traceroute : traceroutes1) {
            List<Object> orDefault = map.getOrDefault(traceroute.getIp(), new ArrayList<>());
            orDefault.add(traceroute);
            map.put(traceroute.getIp(), orDefault);
        }
        defaultDivision.setTracerouteReader(new TracerouteReader() {
            @Override
            public List<Object> readTraceroute(String IP) {
                return map.get(IP);
            }

            @Override
            public List<Object> readTraceroute(String IP, int lastMinutes) {
                return readTraceroute(IP);
            }
        });
    }

    @Test
    public void TestDivision1() {
        for (PingData pingDatum : pingData) {
            defaultDivision.divide(pingDatum);
        }
        List<Cell> actual = defaultDivision.resetDivision();
        Comparator<Cell> comparator = (a, b) -> {
            String s1 = a.getPath().getPathString() + a.getExpectedRTT();
            String s2 = b.getPath().getPathString() + b.getExpectedRTT();
            return s1.compareTo(s2);
        };
        actual.sort(comparator);
        expectedCells.sort(comparator);

        Assert.assertArrayEquals(expectedCells.toArray(), actual.toArray());
    }
}
package division.strategy;

import dao.IP2ASN;
import division.PrintablePath;
import org.junit.Assert;
import org.junit.Test;
import pojo.division.AS;
import pojo.division.ASPath;
import pojo.division.Traceroute;

import java.util.List;

public class ASPathCountryCSegCellLocatorTest {

    @Test
    public void pathTransferTest() {
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
        Traceroute traceroute = new Traceroute("dest", tracerouteString, true, 0L);
        ASPathCountryCSegCellLocator asPathCountryCSegCellLocator = new ASPathCountryCSegCellLocator(new IP2ASN() {
            @Override
            public int queryASN(String IP) {
                if (IP.startsWith("189.157")) {
                    return 8151;
                } else if (IP.startsWith("189.194")) {
                    return 13999;
                } else {
                    return 28481;
                }
            }

            @Override
            public int queryASN(int IP) {
                return 0;
            }
        });
        PrintablePath actual = asPathCountryCSegCellLocator.pathTransfer(traceroute);

        List<AS> as11 = List.of(as1, as2, as3);
        PrintablePath expected = new ASPath(as11);
        Assert.assertEquals(expected, actual);
    }

}
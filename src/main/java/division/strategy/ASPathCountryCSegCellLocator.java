package division.strategy;

import dao.IP2ASN;
import division.cell.Cell;
import division.path.PrintablePath;
import division.cell.ASPathCountryCSegCell;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.PingData;
import division.path.AS;
import division.path.ASPath;
import division.path.Traceroute;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 对应于ASPathCountryCell的划分，以ASPath、C段和Country为依据
 *
 * @author ZT 2022-12-14 12:11
 * @see ASPathCountryCSegCell
 */
public class ASPathCountryCSegCellLocator extends CellLocator {

    private final Logger logger = LoggerFactory.getLogger(ASPathCountryCSegCellLocator.class);

    private final IP2ASN ip2ASN;

    public ASPathCountryCSegCellLocator(IP2ASN ip2ASN) {
        this.ip2ASN = ip2ASN;
    }

    @Override
    protected PrintablePath pathTransfer(Object o) {
        if (!(o instanceof Traceroute traceroute)) {
            String err = String.format("Object o=%s is not instanceof Traceroute.", o);
            logger.error(err);
            throw new RuntimeException(err);
        }
        if (!traceroute.getArrived()) {
            return null;
        }
        List<String> nonStarHops = Traceroute.TracerouteBuilder.getNonStarHops(traceroute);
        int lastAsn = -1, first = -1, last = -1;
        List<AS> path = new ArrayList<>();
        for (int i = 0; i < nonStarHops.size(); i++) {
            String ipString = nonStarHops.get(i);
            IPv4Address iPv4Address = new IPAddressString(ipString).getAddress().toIPv4();
            if (iPv4Address.isPrivate()) {
                continue;
            }
            int asn = ip2ASN.queryASN(ipString);
            if (asn == 0) {
                logger.warn(String.format("ASN of IP %s is 0", ipString));
                continue;
            }
            if (first == -1) {
                lastAsn = asn;
                first = last = i;
                continue;
            }
            // 构建上一个
            if (asn != lastAsn) {
                AS as = new AS(lastAsn, nonStarHops.get(first), nonStarHops.get(last), 0);
                path.add(as);
                first = i;
            }
            lastAsn = asn;
            last = i;
        }
        path.add(new AS(lastAsn, nonStarHops.get(first), nonStarHops.get(last), 0));
        return new ASPath(path);
    }

    public static String extractCSegment(PingData pingData) {
        return pingData.getDestA() + "." + pingData.getDestB() + "." + pingData.getDestC();
    }

    /**
     * 以ASPath和Country为依据进行划分
     * @param printablePath path
     * @param pingData ping数据
     * @return key string
     */
    @Override
    protected String cellKey(PrintablePath printablePath, PingData pingData) {
        return printablePath.getPathString() + "_" + pingData.getCountry() + "_" + extractCSegment(pingData);
    }

    /**
     * 根据traceroute和ping data构建cell
     * 调用不了多少次，突尼斯一个区域也就调用50-100次
     *
     * 最关键的是，即便两个不同单元格中的getPathString()相同，单元格中的ASPath也不会是同一个对象，带来的好处是，
     * 这两个ASPath中的边缘IP可以是不同的。
     *
     * TODO 如果不是同一个ASPath对象是否会有影响？
     *
     * @param o traceroute
     * @param pingData ping data
     * @return ASPathCountryCell
     */
    @Override
    public Cell newCellForCellKey(Object o, PingData pingData) {
        PrintablePath printablePath = pathTransfer(o);
        Instant time = pingData.getTime();
        long epochSecond = time.getEpochSecond();
        epochSecond = epochSecond * 1_000_000_000L + time.getNano();
        return new ASPathCountryCSegCell(printablePath, pingData.getCountry(), extractCSegment(pingData), epochSecond);
    }
}

package division.path;

import dao.IPRTTReader;
import division.cell.RTTCalculator;

import java.util.List;

/**
 * 用这个AS Path的最后一跳IP去寻找
 *
 * @author ZT 2022-12-19 11:23
 */
public class ASPathRTTGetterUseLastIP implements PathRTTGetter {

    private volatile IPRTTReader IPRTTReader;
    private volatile RTTCalculator rttCalculator;

    public ASPathRTTGetterUseLastIP(IPRTTReader IPRTTReader, RTTCalculator rttCalculator) {
        this.IPRTTReader = IPRTTReader;
        this.rttCalculator = rttCalculator;
    }

    @Override
    public int getRTT(PrintablePath path) {
        if (!(path instanceof ASPath asPath)) {
            return -1;
        }
        List<AS> path1 = asPath.getPath();
        AS as = path1.get(path1.size() - 1);
        String lastIP = as.getLastIP();
        List<Integer> rtts = IPRTTReader.queryRTT(lastIP);
        return rttCalculator.calculate(rtts);
    }

    public IPRTTReader getIPRTTReader() {
        return IPRTTReader;
    }

    public void setIPRTTReader(IPRTTReader IPRTTReader) {
        this.IPRTTReader = IPRTTReader;
    }

    public RTTCalculator getRttCalculator() {
        return rttCalculator;
    }

    public void setRttCalculator(RTTCalculator rttCalculator) {
        this.rttCalculator = rttCalculator;
    }
}

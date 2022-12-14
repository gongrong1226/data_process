package division.cell;

import division.Cell;
import division.PrintablePath;

import java.util.List;
import java.util.Map;

/**
 * IP集合划分出来的结果，就是一个单元格
 * TODO 如果Cell占用内存过多，优化RTTs和IPs的保存方式，分别使用int[]和int[]保存，最小化字节浪费
 *
 * @author ZT 2022-12-06 16:05
 */
public abstract class AbstractCell implements Cell {

    /**
     * 单元格属性不定，方便扩展划分标准
     */
    protected Map<String, Object> feature;

    /**
     * 必须包括路径属性
     */
    protected final PrintablePath path;

    /**
     * 这个单元格中的RTT， 单位：微秒
     */
    protected final List<Integer> RTTs;

    /**
     * 这个单元格中的IP
     */
    protected final List<String> IPs;

    public AbstractCell(Map<String, Object> feature, List<Integer> RTTs, List<String> IPs, PrintablePath path) {
        this.feature = feature;
        this.path = path;
        this.RTTs = RTTs;
        this.IPs = IPs;
    }

    public Map<String, Object> getFeature() {
        return feature;
    }

    public void setFeature(Map<String, Object> feature) {
        this.feature = feature;
    }

    public List<Integer> getRTTs() {
        return RTTs;
    }

    public List<String> getIPs() {
        return IPs;
    }

    public synchronized void addRtt(String ip, int RTT) {
        IPs.add(ip);
        RTTs.add(RTT);
    }

    public PrintablePath getPath() {
        return path;
    }

}

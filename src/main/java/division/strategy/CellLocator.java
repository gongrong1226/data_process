package division.strategy;

import division.cell.Cell;
import division.path.PrintablePath;
import pojo.PingData;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过traceroute和ping数据唯一定位到一个单元格
 *
 * @author ZT 2022-12-06 21:33
 */
public abstract class CellLocator {

    private List<PrintablePath> pathTransfer(List<Object> traceroutes) {
        List<PrintablePath> printablePaths = new ArrayList<>(traceroutes.size());
        for (Object traceroute : traceroutes) {
            PrintablePath printablePath = pathTransfer(traceroute);
            if (printablePath == null) {
                continue;
            }
            printablePaths.add(printablePath);
        }
        return printablePaths;
    }

    /**
     * 将Traceroute转换为高级别的路径
     *
     * @param traceroute traceroute结果，每个Object代表一条路径
     * @return 路径
     */
    protected abstract PrintablePath pathTransfer(Object traceroute);

    /**
     * path和Ping数据结合，唯一确定一个cell
     *
     * @param printablePath path
     * @param pingData      ping数据
     * @return 这个cell对应的key
     */
    protected abstract String cellKey(PrintablePath printablePath, PingData pingData);

    /**
     * 构造一个符合当前策略的Cell
     * 当cell key对应的cell不存在时，会调用该方法
     *
     * @param traceroute traceroute
     * @param pingData ping data
     * @return cell
     */
    public abstract Cell newCellForCellKey(Object traceroute, PingData pingData);

    /**
     * 根据traceroute 和 ping data将这个IP和它的RTT定位到对应的cell key
     *
     * @param traceroutes traceroute
     * @param pingData    ping data
     * @return 对应的key
     */
    public final List<String> locate(List<Object> traceroutes, PingData pingData) {
        // 根据Traceroute做转换，转换成其他Path，不止一条
        List<PrintablePath> printablePaths = pathTransfer(traceroutes);
        List<String> keys = new ArrayList<>(traceroutes.size());
        for (PrintablePath printablePath : printablePaths) {
            // 按照Path + Geo或者其他规则分类，定位到cell
            keys.add(cellKey(printablePath, pingData));
        }
        return keys;
    }

}

package division.path;

import division.path.PrintablePath;

/**
 * @author ZT 2022-12-17 13:42
 */
public interface PathRTTGetter {
    /**
     * 获取这条Path的RTT
     *
     * @param path printablePath
     * @return 微秒RTT，小于0表示没有获取到
     */
    int getRTT(PrintablePath path);
}

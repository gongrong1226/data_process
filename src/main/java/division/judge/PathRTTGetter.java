package division.judge;

import division.path.PrintablePath;

/**
 * @author ZT 2022-12-17 13:42
 */
public interface PathRTTGetter {
    /**
     * 获取这条Path的RTT
     *
     * @param path printablePath
     * @return 微秒RTT
     */
    int getRTT(PrintablePath path);
}

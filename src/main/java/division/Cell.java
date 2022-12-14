package division;

/**
 * @author ZT 2022-12-07 15:49
 */
public interface Cell {

    void addRtt(String ip, int RTT);

    PrintablePath getPath();

    /**
     * 微秒RTT
     * @return rtt
     */
    int getExpectedRTT();
}

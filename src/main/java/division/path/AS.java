package division.path;

/**
 * @author ZT 2022-12-06 21:59
 */
public class AS {

    private int ASN;

    // TODO 现在的lastIP是第一个触发newCell方法的路径上的IP
    private String firstIP, lastIP;

    /**
     * 单位微秒
     */
    private int RTT;

    public AS(int ASN, String firstIP, String lastIP, int RTT) {
        this.ASN = ASN;
        this.firstIP = firstIP;
        this.lastIP = lastIP;
        this.RTT = RTT;
    }

    public int getASN() {
        return ASN;
    }

    public void setASN(int ASN) {
        this.ASN = ASN;
    }

    public int getRTT() {
        return RTT;
    }

    public void setRTT(int RTT) {
        this.RTT = RTT;
    }

    @Override
    public String toString() {
        return "AS{" +
                "ASN=" + ASN +
                ", RTT=" + RTT +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AS as = (AS) o;

        if (ASN != as.ASN) return false;
        if (!firstIP.equals(as.firstIP)) return false;
        return lastIP.equals(as.lastIP);
    }

    public String getLastIP() {
        return lastIP;
    }
}

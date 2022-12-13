package pojo.division;

/**
 * @author ZT 2022-12-06 21:59
 */
public class AS {

    private int ASN;

    /**
     * 单位微秒
     */
    private int RTT;

    public AS(int ASN, int RTT) {
        this.ASN = ASN;
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
}

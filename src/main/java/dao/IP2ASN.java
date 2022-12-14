package dao;

/**
 * @author ZT 2022-12-13 16:41
 */
public interface IP2ASN {
    /**
     * IP转换成ASN
     * @param IP ip字符串
     * @return asn
     */
    int queryASN(String IP);

    /**
     * IP转换成ASN
     * @param IP 32位IP
     * @return asn
     */
    int queryASN(int IP);
}

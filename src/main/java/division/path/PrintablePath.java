package division.path;

/**
 * @author ZT 2022-12-06 22:26
 */
public interface PrintablePath {

    /**
     * 将Path转换为字符串。
     * 单元格划分时，这个会是一个维度，也就是说靠这个字符串来判定两个Path是否相等
     * @return string
     */
    String getPathString();
}

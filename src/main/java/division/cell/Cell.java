package division.cell;

import division.path.PrintablePath;

/**
 * @author ZT 2022-12-07 15:49
 */
public interface Cell {

    void addRtt(String ip, int RTT);

    PrintablePath getPath();

    /**
     * 获取单元格的RTT
     * @return 微秒RTT
     */
    int getStatisticRTT();

    void setFaultCause(FaultCause faultCause);

    /**
     * 故障原因
     */
    enum FaultCause {
        // 不能确定
        AMBIGUOUS(1),
        CLOUD(2),
        MIDDLE(3),
        CLIENT(4),
        // 样本不足
        INSUFFICIENT(5);

        private final int cause;

        FaultCause() {
            this(0);
        }

        FaultCause(int cause) {
            this.cause = cause;
        }

        public int getCause() {
            return cause;
        }

        @Override
        public String toString() {
            switch (cause) {
                case 1:
                    return "AMBIGUOUS";
                case 2:
                    return "CLOUD";
                case 3:
                    return "MIDDLE";
                case 4:
                    return "CLIENT";
                case 5:
                    return "INSUFFICIENT";
                default:
                    return "UNKNOWN";
            }
        }
    }
}

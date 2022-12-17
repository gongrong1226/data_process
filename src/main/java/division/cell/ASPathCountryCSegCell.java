package division.cell;

import division.PrintablePath;

import java.util.ArrayList;

/**
 * 如果占用内存过高，可以Map<country, class>的形式存放类，以进行构造实例
 *
 * @author ZT 2022-12-14 11:07
 */
public class ASPathCountryCSegCell extends AbstractCell {

    /**
     * 默认使用中位数
     */
    private static RTTCalculator rttCalculator = new MedianRTTCalculator();

    private final String cSegment;

    private final String country;

    public ASPathCountryCSegCell(PrintablePath path, String country, String cSegment) {
        super(null, new ArrayList<>(), new ArrayList<>(), path);
        this.country = country;
        this.cSegment = cSegment;
    }

    public static void setRttCalculator(RTTCalculator rttCalculator) {
        ASPathCountryCSegCell.rttCalculator = rttCalculator;
    }

    @Override
    public int getStatisticRTT() {
        return rttCalculator.calculate(getRTTs());
    }

    public String getCountry() {
        return country;
    }

    public String getcSegment() {
        return cSegment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ASPathCountryCSegCell that = (ASPathCountryCSegCell) o;

        if (cSegment != null ? !cSegment.equals(that.cSegment) : that.cSegment != null) return false;
        if (country != null ? !country.equals(that.country) : that.country != null) return false;
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "ASPathCountryCSegCell{" +
                "cSegment='" + cSegment + '\'' +
                ", country='" + country + '\'' +
                ", feature=" + feature +
                ", path=" + path +
                ", RTTs=" + RTTs +
                ", IPs=" + IPs +
                '}';
    }
}

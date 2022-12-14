package division.cell;

import division.PrintablePath;

import java.util.ArrayList;

/**
 * 如果占用内存过高，可以Map<country, class>的形式存放类，以进行构造实例
 *
 * @author ZT 2022-12-14 11:07
 */
public class ASPathCountryCell extends AbstractCell {

    /**
     * 默认使用中位数
     */
    private static RTTCalculator rttCalculator = new MedianRTTCalculator();

    private final String country;

    public ASPathCountryCell(PrintablePath path, String Country) {
        super(null, new ArrayList<>(), new ArrayList<>(), path);
        country = Country;
    }

    public static void setRttCalculator(RTTCalculator rttCalculator) {
        ASPathCountryCell.rttCalculator = rttCalculator;
    }

    @Override
    public int getExpectedRTT() {
        return rttCalculator.calculate(getRTTs());
    }

    public String getCountry() {
        return country;
    }
}

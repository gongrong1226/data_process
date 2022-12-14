import division.DefaultDivision;
import net.MeasurementDataServer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class DataProcessor {
    public static void main(String[] args) {
        DefaultDivision defaultDivision = DefaultDivision.getDefaultDivision();
        MeasurementDataServer dataServer = new MeasurementDataServer();
        dataServer.run();
    }
}

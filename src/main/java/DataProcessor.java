import dao.asndb.ASNFileDB;
import division.DefaultDivision;
import net.MeasurementDataServer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class DataProcessor {
    public static void main(String[] args) {
        // init, about 6 seconds
        ASNFileDB.getDefaultASNFileDB();
        MeasurementDataServer dataServer = new MeasurementDataServer();
        dataServer.run();
    }
}

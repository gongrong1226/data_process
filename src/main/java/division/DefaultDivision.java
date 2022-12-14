package division;

import dao.TracerouteReader;
import dao.asndb.ASNFileDB;
import dao.questdb.QuestTracerouteReader;
import division.strategy.ASPathCountryCSegCellLocator;
import division.strategy.CellLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * 线程安全
 * @author ZT 2022-12-14 16:41
 */
public class DefaultDivision extends AbstractDivision {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDivision.class);

    private volatile static DefaultDivision defaultDivision;

    public static DefaultDivision getDefaultDivision() {
        if (defaultDivision == null) {
            synchronized (DefaultDivision.class) {
                if (defaultDivision == null) {
                    URL asnResource = DefaultDivision.class.getClassLoader().getResource("dao/ip2asn-v4.tsv");
                    if (asnResource == null) {
                        String err = "can not get the asn data file";
                        logger.error(err);
                        throw new RuntimeException(err);
                    }
                    String path = asnResource.getPath();
                    File file = new File(path);
                    ASNFileDB asnFileDB = new ASNFileDB(file);
                    asnFileDB.queryASN("1.1.1.1");
                    ASPathCountryCSegCellLocator asPathCountryCSegCellLocator = new ASPathCountryCSegCellLocator(asnFileDB);

                    TracerouteReader questTracerouteReader = new QuestTracerouteReader(QuestTracerouteReader.DEFAULT_URL, QuestTracerouteReader.TUNIS_TRACEROUTE_TABLE);
                    defaultDivision = new DefaultDivision(questTracerouteReader, asPathCountryCSegCellLocator);
                }
            }
        }
        return defaultDivision;
    }

    public DefaultDivision(TracerouteReader tracerouteReader, CellLocator cellLocator) {
        super(tracerouteReader, cellLocator);
    }
}

package dao.questdb;

import dao.IPRTTReader;
import org.postgresql.jdbc2.optional.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ZT 2022-12-17 15:46
 */
public class QuestIPRTTReader implements IPRTTReader {

    private Logger logger = LoggerFactory.getLogger(QuestIPRTTReader.class);

    public final static String TUNIS_ROUTERS_TABLE = "tunis_routers";

    public final static String DEFAULT_URL = "jdbc:postgresql://localhost:8812/qdb";


    private String table;
    private PoolingDataSource dataSource;


    public QuestIPRTTReader(String url, String table, int initialConnections, int maxConnections) {
        this.table = table;
        dataSource = new PoolingDataSource();
        dataSource.setInitialConnections(initialConnections);
        dataSource.setMaxConnections(maxConnections);
        dataSource.setUrl(url);
        dataSource.setUser("admin");
        dataSource.setPassword("quest");
        dataSource.setSsl(false);
    }


    public QuestIPRTTReader(String url, String table) {
        this(url, table, QuestTracerouteReader.INITIAL_CONNECTIONS, QuestTracerouteReader.MAX_CONNECTIONS);
    }


    @Override
    public List<Integer> queryRTT(String ip) {
        return queryRTT(ip, 60*24*366*2);
    }

    @Override
    public List<Integer> queryRTT(String IP, int lastMinutes) {
        String[] split = IP.split("\\.");
        String destA = split[0];
        String destB = split[1];
        String destC = split[2];
        String destD = split[3];
        List<Integer> result = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT RTT FROM ? where destA=? and destB=? and destC=? and destD=? and ts > ?;")) {
                int i = 1;
                preparedStatement.setString(i++, table);
                preparedStatement.setString(i++, destA);
                preparedStatement.setString(i++, destB);
                preparedStatement.setString(i++, destC);
                preparedStatement.setString(i++, destD);
                long currentTimeMillis = System.currentTimeMillis();
                currentTimeMillis = currentTimeMillis - (long) lastMinutes * 60 * 1000;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                String date = df.format(new Date(currentTimeMillis));
                preparedStatement.setString(i, date);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        // 没到达的就不考虑
                        result.add(rs.getInt("RTT"));
                    }
                }
            }
        } catch (SQLException throwables) {
            logger.error(String.format("query IP=%s, table=%s, error=%s", IP, table, throwables));
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (Throwable throwables) {
                logger.error(String.format("close connection error=%s", throwables));
            }
        }
        return result;
    }
}

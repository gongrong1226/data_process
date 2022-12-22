package dao.questdb;

import dao.TracerouteReader;
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
 * @author ZT 2022-12-10 2:17
 */
public class QuestTracerouteReader implements TracerouteReader {

    private Logger logger = LoggerFactory.getLogger(QuestTracerouteReader.class);

    protected final static int INITIAL_CONNECTIONS = 10;
    protected final static int MAX_CONNECTIONS = 32;

    public final static String TUNIS_TRACEROUTE_TABLE = "tunis_traceroute";

    public final static String DEFAULT_URL = "jdbc:postgresql://localhost:8812/qdb";

    /**
     * TODO Refactor， 放到DAO注解当中
     */
    protected final static String DEST_A_COLUMN = "destA";
    protected final static String DEST_B_COLUMN = "destB";
    protected final static String DEST_C_COLUMN = "destC";
    protected final static String DEST_D_COLUMN = "destD";
    protected final static String TRACEROUTE_COLUMN = "traceroute";
    protected final static String ARRIVED_COLUMN = "arrived";

    private String table;
    private PoolingDataSource dataSource;

    public QuestTracerouteReader(String url, String table, int initialConnections, int maxConnections) {
        this.table = table;
        dataSource = new PoolingDataSource();
        dataSource.setInitialConnections(initialConnections);
        dataSource.setMaxConnections(maxConnections);
        dataSource.setUrl(url);
        dataSource.setUser("admin");
        dataSource.setPassword("quest");
        dataSource.setSsl(false);
    }


    public QuestTracerouteReader(String url, String table) {
        this(url, table, 10, MAX_CONNECTIONS);
    }


    @Override
    public List<Object> readTraceroute(String IP) {
        return readTraceroute(IP, 60*24*366*2);
    }

    @Override
    public List<Object> readTraceroute(String IP, int lastMinutes) {
        List<Object> result = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM '$TABLE_NAME' where destA=? and destB=? and destC=? and destD=? and timestamp > ?;";
            sql = sql.replace("$TABLE_NAME", table);
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                String[] split = IP.split("\\.");
                int i = 1;
                preparedStatement.setString(i++, split[0]);
                preparedStatement.setString(i++, split[1]);
                preparedStatement.setString(i++, split[2]);
                preparedStatement.setString(i++, split[3]);
                long currentTimeMillis = System.currentTimeMillis();
                currentTimeMillis = currentTimeMillis - (long) lastMinutes * 60 * 1000;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//设置日期格式
                String date = df.format(new Date(currentTimeMillis));
                preparedStatement.setString(i++, date);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        // 没到达的就不考虑
                        if (!rs.getBoolean(ARRIVED_COLUMN)) {
                            continue;
                        }
                        result.add(rs.getString(TRACEROUTE_COLUMN));
                    }
                }
            }
        } catch (SQLException throwables) {
            // 表不存在先不爆出来
            if (!throwables.getMessage().contains("table does not exist")) {
                logger.error(String.format("query IP=%s, table=%s, error=%s", IP, table, throwables));
            }
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

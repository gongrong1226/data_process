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

    private final static int INITIAL_CONNECTIONS = 10;
    private final static int MAX_CONNECTIONS = 32;

    /**
     * TODO Refactor， 放到DAO注解当中
     */
    private final static String IP_COLUMN = "ip";
    private final static String TRACEROUTE_COLUMN = "traceroute";
    private final static String ARRIVED_COLUMN = "traceroute";

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
        List<Object> result = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM ? where ip=?;")) {
                preparedStatement.setString(1, table);
                preparedStatement.setString(2, IP);
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

    @Override
    public List<Object> readTraceroute(String IP, int lastMinutes) {
        List<Object> result = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM ? where ip=? and ts > ?;")) {
                preparedStatement.setString(1, table);
                preparedStatement.setString(2, IP);
                long currentTimeMillis = System.currentTimeMillis();
                currentTimeMillis = currentTimeMillis - (long) lastMinutes * 60 * 1000;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                String date = df.format(new Date(currentTimeMillis));
                preparedStatement.setString(3, date);
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

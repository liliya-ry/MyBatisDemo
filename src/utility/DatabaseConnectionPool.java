package utility;

import java.sql.*;
import java.util.*;

public class DatabaseConnectionPool {
    private static final DatabaseConnectionPool instance = null;
    private static final long KEEP_AWAKE_TIME = 600000; //10min
    private static final long EXPIRATION_TIME = 1800000; //30min
    private final CircularArrayQueue<Connection> pool;
    private final HashMap<Connection, ConnectionInfo> conInfoMap;
    private final String url;
    private final String user;
    private final String password;
    private final int poolSize;
    private final Timer timer;


    private DatabaseConnectionPool(String url, String user, String password, int poolSize) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
        this.conInfoMap = new HashMap<>();
        this.pool = new CircularArrayQueue<>(poolSize);
        this.timer = new Timer();
        for (int i = 0; i < poolSize; i++) {
            addConnection();
        }
    }

    public static DatabaseConnectionPool init(String url, String user, String password, int poolSize) throws Exception {
        if (instance != null) {
            throw new Exception("Pool is already created");
        }

        return new DatabaseConnectionPool(url, user, password, poolSize);
    }

    public static DatabaseConnectionPool getConnectionPool() throws Exception {
        if (instance == null) {
            throw new Exception("Pool is not created yet");
        }

        return instance;
    }

    public Connection getConnection() throws Exception {
        if (pool.isEmpty() && conInfoMap.size() < poolSize) {
            addConnection();
        }

        Connection con = pool.poll();
        if (con == null) {
            throw new Exception("All connections are used");
        }

        ConnectionInfo info = conInfoMap.get(con);
        info.isAvailable = false;
        info.setNotReturnedTask();
        timer.schedule(info.notReturned, EXPIRATION_TIME);

        return con;
    }

    private void addConnection() throws SQLException {
        Connection con = DriverManager.getConnection(url, user, password);
        TimerTask task = getKeepAwakeTask(con);
        timer.schedule(task, KEEP_AWAKE_TIME);
        ConnectionInfo info = new ConnectionInfo(task);
        pool.add(con);
        conInfoMap.put(con, info);
    }

    public boolean releaseConnection(Connection connection) throws SQLException {
        if (pool.size() == poolSize) {
            return false;
        }

        return pool.add(connection);
    }

    private TimerTask getKeepAwakeTask(Connection con) {
        return new TimerTask() {
            @Override
            public void run() {
                ConnectionInfo info = conInfoMap.get(con);
                try {
                    if (con.isClosed()) {
                        pool.remove(con);
                        conInfoMap.remove(con);
                        return;
                    }
                    try (Statement st = con.createStatement()) {
                        st.executeQuery("SELECT 1");
                    }
                } catch (SQLException ignored) {
                    TimerTask next = getKeepAwakeTask(con);
                    timer.schedule(next, KEEP_AWAKE_TIME);
                    info.keepAwake = next;
                }
            }
        };
    }

    private static class ConnectionInfo {
        boolean isAvailable = true;
        TimerTask keepAwake;
        TimerTask notReturned;

        ConnectionInfo(TimerTask keepAwake) {
            this.keepAwake = keepAwake;
        }

        public void setNotReturnedTask() {
            this.notReturned = new TimerTask() {
                public void run() {
                    throw new IllegalStateException("Connection not returned in 30 minutes");
                }
            };
        }
    }
}

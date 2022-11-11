package session;

import dto.Configuration;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class SqlSession implements Closeable {
    private final DatabaseConnectionPool dcp;
    private final Connection conn;
    private final Configuration configuration;

    SqlSession(Configuration configuration, Connection conn, DatabaseConnectionPool dcp) {
        this.configuration = configuration;
        this.conn = conn;
        this.dcp = dcp;
    }

    public <T> T getMapper(Class<T> type) {
        var handler = new DaoHandler(this, type);
        return (T) Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class[]{type}, handler);    }

    public Connection getConnection() throws Exception {
        return this.conn;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void close() {
        try {
            if (this.dcp != null)
                dcp.releaseConnection(conn);
             else
                conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


//        <T> T selectOne(String statement, Object parameter);
//    <E> List<E> selectList(String statement, Object parameter);
//    <K,V> Map<K,V> selectMap(String statement, Object parameter, String mapKey);
//    <T> Cursor selectCursor(String statement, Object parameter);
//    void select(String statement, Object parameter);
//    int insert(String statement, Object parameter);
//    int update(String statement, Object parameter);
//    int delete(String statement, Object parameter);
}

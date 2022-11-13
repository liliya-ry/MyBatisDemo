package dto.data_source;

import utility.DatabaseConnectionPool;

import java.sql.Connection;
import java.util.Properties;

public class PooledDataSource extends DataSource {
    private static final int DEFAULT_POOL_SIZE = 10;
    private final DatabaseConnectionPool dcp;

    public PooledDataSource(Properties properties) throws Exception {
        super(properties);
        DatabaseConnectionPool.init(this.url, this.user, this.password, DEFAULT_POOL_SIZE);
        this.dcp = DatabaseConnectionPool.getConnectionPool();
    }

    @Override
    public Connection getConnection() throws Exception {
        return dcp.getConnection();
    }

    public DatabaseConnectionPool getDcp() {
        return dcp;
    }
}

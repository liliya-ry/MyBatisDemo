package session;

import dto.Configuration;
import dto.data_source.DataSource;
import dto.data_source.PooledDataSource;
import utility.DatabaseConnectionPool;

public class SqlSessionFactory {
    private final Configuration configuration;
    private final DataSource dataSource;

    SqlSessionFactory(Configuration configuration, DataSource dataSource) {
        this.configuration = configuration;
        this.dataSource = dataSource;
    }

    public SqlSession openSession() throws Exception {
        DatabaseConnectionPool dcp = null;
        if (dataSource instanceof PooledDataSource pooledDataSource) {
            dcp = pooledDataSource.getDcp();
        }
        return new SqlSession(configuration, dataSource.getConnection(), dcp);
    }

    public SqlSession openSession(Configuration configuration) throws Exception {
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory factory = builder.build(configuration);
        return factory.openSession();
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }
}

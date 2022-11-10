package session;

import dto.Configuration;

import java.util.Properties;

public class SqlSessionFactoryPooled implements SqlSessionFactory {
    private static final int DEFAULT_POOL_SIE = 10;
    private final Configuration configuration;
    private DatabaseConnectionPool dcp;

    SqlSessionFactoryPooled(Configuration configuration) throws Exception {
        this.configuration = configuration;
        assignDCP();
    }

    private void assignDCP() throws Exception {
        Properties properties = this.configuration.getProperties();
        String url = (String) properties.get("url");
        String user = (String) properties.get("user");
        String password = (String) properties.get("password");
        DatabaseConnectionPool.init(url, user, password, DEFAULT_POOL_SIE);
        this.dcp = DatabaseConnectionPool.getConnectionPool();
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public SqlSession openSession() throws Exception {
        return new SqlSession(this.configuration, this.dcp.getConnection(), this.dcp);
    }

    public SqlSession openSession(Configuration configuration) {
        return null;
    }
}

package session;

import dto.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class SqlSessionFactoryUnpooled implements SqlSessionFactory {
    private final Configuration configuration;
    private final String url;
    private final String user;
    private final String password;

    SqlSessionFactoryUnpooled(Configuration configuration) {
        this.configuration = configuration;
        Properties properties = this.configuration.properties;
        this.url = properties.getProperty("url");
        this.user = properties.getProperty("username");
        this.password = properties.getProperty("password");
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public SqlSession openSession() throws Exception {
        Connection con = DriverManager.getConnection(url, user, password);
        return new SqlSession(this.configuration, con, null);
    }

    public SqlSession openSession(Configuration configuration) {
        return null;
    }
}

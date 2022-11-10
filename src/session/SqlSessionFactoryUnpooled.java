package session;

import dto.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class SqlSessionFactoryJDBC implements SqlSessionFactory {
    private final Configuration configuration;
    private final String url;
    private final String user;
    private final String password;

    SqlSessionFactoryJDBC(Configuration configuration) {
        this.configuration = configuration;
        Properties properties = this.configuration.properties;
        this.url = properties.getProperty("jdbc.url");
        this.user = properties.getProperty("jdbc.username");
        this.password = properties.getProperty("jdbc.password");
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public SqlSession openSession() throws Exception {
        Connection con = DriverManager.getConnection(url, user, password);
        return new SqlSession(this.configuration, con);
    }

    public SqlSession openSession(Configuration configuration) {
        return null;
    }
}

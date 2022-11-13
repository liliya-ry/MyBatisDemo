package dto.data_source;

import java.sql.*;
import java.util.Properties;

public abstract class DataSource {
    protected Properties properties;
    protected String url;
    protected String user;
    protected String password;

    protected DataSource(Properties properties) throws Exception {
        this.properties = properties;
        this.url = properties.getProperty("url");
        this.user = properties.getProperty("username");
        this.password = properties.getProperty("password");
        registerDriver();
    }

    private void registerDriver() throws Exception {
        String driverName = properties.getProperty("driver");

        if (driverName == null) {
            return;
        }

        Class<?> driverClass = Class.forName(driverName);
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(driver);
    }

    public abstract Connection getConnection() throws Exception;
}

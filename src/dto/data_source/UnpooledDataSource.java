package dto.data_source;

import java.sql.*;
import java.util.Properties;

public class UnpooledDataSource extends DataSource {

    public UnpooledDataSource(Properties properties) throws Exception {
        super(properties);
    }

    @Override
    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(this.url, this.user, this.password);
    }
}

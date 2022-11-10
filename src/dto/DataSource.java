package dto;

import java.util.Properties;

public class DataSource {
    public String type;
    public Properties properties;

    DataSource(String type, Properties properties) {
        this.type = type;
        this.properties = properties;
    }
}

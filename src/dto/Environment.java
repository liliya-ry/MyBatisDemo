package dto;

import dto.data_source.DataSource;

public class Environment {
    String id;
    String transactionManagerType;
    DataSource dataSource;

    public Environment(String id, String transactionManagerType, DataSource dataSource) {
        this.id = id;
        this.transactionManagerType = transactionManagerType;
        this.dataSource = dataSource;
    }

    public String getId() {
        return id;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}

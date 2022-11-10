package dto;

public class Environment {
    public String id;
    public String transactionManagerType;
    public DataSource dataSource;

    Environment(String id, String transactionManagerType, DataSource dataSource) {
        this.id = id;
        this.transactionManagerType = transactionManagerType;
        this.dataSource = dataSource;
    }
}

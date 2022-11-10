package dto;

public class InsertQuery extends Query {
    public boolean useGeneratedKeys;
    public String keyProperty;

    public InsertQuery(QUERY_TYPE queryType,
                       String id,
                       Class<?> parameterType,
                       String sql,
                       boolean useGeneratedKeys,
                       String keyProperty) {
        super(queryType, id, parameterType, sql);
        this.useGeneratedKeys = useGeneratedKeys;
        this.keyProperty = keyProperty;
    }

    @Override
    public String toString() {
        return "InsertQuery{" +
                "useGeneratedKeys=" + useGeneratedKeys +
                ", keyProperty='" + keyProperty + '\'' +
                '}';
    }
}

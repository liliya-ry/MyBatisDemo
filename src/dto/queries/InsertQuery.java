package dto.queries;

public class InsertQuery extends Query {
    boolean useGeneratedKeys;
    String keyProperty;

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

    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    public String getKeyProperty() {
        return keyProperty;
    }
}

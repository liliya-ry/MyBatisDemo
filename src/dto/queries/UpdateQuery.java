package dto.queries;

public class UpdateQuery extends Query {
    boolean useGeneratedKeys;
    String keyProperty;

    public UpdateQuery(QUERY_TYPE queryType,
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
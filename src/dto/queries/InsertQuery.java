package dto.queries;

public class InsertQuery extends Query {
    boolean useGeneratedKeys;
    String keyProperty;
    boolean flushCache;

    public InsertQuery(QUERY_TYPE queryType,
                       String id,
                       Class<?> parameterType,
                       String sql,
                       boolean useGeneratedKeys,
                       String keyProperty,
                       boolean flushCache) {
        super(queryType, id, parameterType, sql);
        this.useGeneratedKeys = useGeneratedKeys;
        this.keyProperty = keyProperty;
        this.flushCache = flushCache;
    }

    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public boolean isFlushCache() {
        return flushCache;
    }
}

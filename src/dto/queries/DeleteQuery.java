package dto.queries;

public class DeleteQuery extends Query {
    boolean flushCache;

    public DeleteQuery(QUERY_TYPE queryType, String id, Class<?> parameterType, String sql, boolean flushCache) {
        super(queryType, id, parameterType, sql);
        this.flushCache = flushCache;
    }

    public boolean isFlushCache() {
        return flushCache;
    }
}

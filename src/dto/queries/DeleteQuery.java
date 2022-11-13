package dto.queries;

public class DeleteQuery extends Query {
    public DeleteQuery(QUERY_TYPE queryType, String id, Class<?> parameterType, String sql) {
        super(queryType, id, parameterType, sql);
    }
}

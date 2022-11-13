package dto.queries;

public class SelectQuery extends Query {
    Class<?> resultType;
    String resultMapId;


    public SelectQuery(QUERY_TYPE queryType,
                       String id,
                       Class<?> parameterType,
                       String sql,
                       Class<?> resultType,
                       String resultMapId) {
        super(queryType, id, parameterType, sql);
        this.resultType = resultType;
        this.resultMapId = resultMapId;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    @Override
    public String toString() {
        return "SelectQuery{" +
                "resultType=" + resultType +
                ", resultMapId='" + resultMapId + '\'' +
                '}';
    }
}

package dto;

public class SelectQuery extends Query {
    public Class<?> resultType;
    public String resultMapId;


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

    @Override
    public String toString() {
        return "SelectQuery{" +
                "resultType=" + resultType +
                ", resultMapId='" + resultMapId + '\'' +
                '}';
    }
}

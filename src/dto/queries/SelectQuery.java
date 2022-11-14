package dto.queries;

import java.lang.reflect.Field;
import java.util.Map;

public class SelectQuery extends Query {
    Class<?> resultType;
    Map<String, Field> resultFieldsMap;
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
        this.resultFieldsMap = getFieldsMap(resultType);
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public Map<String, Field> getFieldsMap() {
        return fieldsMap;
    }

    public Map<String, Field> getResultFieldsMap() {
        return resultFieldsMap;
    }
}

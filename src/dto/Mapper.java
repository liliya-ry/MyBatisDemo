package dto;

import java.util.Map;

public class Mapper {
    public Class<?> namespace;
    public Map<String, Query> queries;
    public Map<String, ResultMap> resultMaps;

    public Mapper(Class<?> namespace, Map<String, Query> queries, Map<String, ResultMap> resultMaps) {
        this.namespace = namespace;
        this.queries = queries;
        this.resultMaps = resultMaps;
    }
}

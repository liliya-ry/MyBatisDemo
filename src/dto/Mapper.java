package dto;

import dto.queries.Query;

import java.util.Map;

public class Mapper {
    String namespace;
    Map<String, Query> queries;
    Map<String, ResultMap> resultMaps;

    public Mapper(String namespace, Map<String, Query> queries, Map<String, ResultMap> resultMaps) {
        this.namespace = namespace;
        this.queries = queries;
        this.resultMaps = resultMaps;
    }

    public String getNamespace() {
        return namespace;
    }

    public Query getQueryById(String queryId) {
        return queries.get(queryId);
    }

    public ResultMap getResultMapById(String resultMapId) {
        return resultMaps.get(resultMapId);
    }
}

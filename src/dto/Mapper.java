package dto;

import dto.queries.Query;
import utility.Cache;

import java.util.Map;

public class Mapper {
    String namespace;
    Map<String, Query> queries;
    Map<String, ResultMap> resultMaps;
    private Map<String, Cache<Object, Object>> caches;

    public Mapper(String namespace,
                  Map<String, Query> queries,
                  Map<String, ResultMap> resultMaps,
                  Map<String, Cache<Object, Object>> caches) {
        this.namespace = namespace;
        this.queries = queries;
        this.resultMaps = resultMaps;
        this.caches = caches;
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

    public Map<String, Cache<Object, Object>> getCaches() {
        return caches;
    }
}

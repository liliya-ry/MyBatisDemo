package dto;

import java.util.Map;

public class ResultMap {
    String id;
    Class<?> type;
    Map<String, Result> results;
    Result resultId;

    public ResultMap(String id, Class<?> type, Map<String, Result> results, Result resultId) {
        this.id = id;
        this.type = type;
        this.results = results;
        this.resultId = resultId;
    }

    public String getId() {
        return id;
    }
}

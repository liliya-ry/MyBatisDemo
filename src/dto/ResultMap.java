package dto;

import java.util.Map;

public class ResultMap {
   public String id;
   public Class<?> type;
   public Map<String, Result> results;
   public Result resultId;

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

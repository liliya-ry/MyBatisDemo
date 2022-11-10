package dto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Query {
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\#\\{([\\w\\d]+)\\}");
    public enum QUERY_TYPE { SELECT, INSERT, UPDATE, DELETE }

    public QUERY_TYPE queryType;
    public String id;
    public Class<?> parameterType;
    public String sql;
    public List<String> paramNames;

    public Query(QUERY_TYPE queryType, String id, Class<?> parameterType, String sql) {
        this.queryType = queryType;
        this.id = id;
        this.parameterType = parameterType;
        this.paramNames = new ArrayList<>();
        this.sql =formatQuery(sql, paramNames);
    }

    private String formatQuery(String sql, List<String> fNames) {
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        return matcher.replaceAll(m -> {
            String fName = m.group(1);
            fName = getNormalizedFieldName(fName);
            fNames.add(fName);
            return "?";
        });
    }

    private String getNormalizedFieldName(String fName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fName.length(); i++) {
            char ch = fName.charAt(i);
            if (ch == '_') {
                continue;
            }
            if (Character.isUpperCase(ch)) {
                ch = Character.toLowerCase(ch);
            }
            sb.append(ch);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Query{" +
                "queryType=" + queryType +
                ", id='" + id + '\'' +
                ", parameterType=" + parameterType +
                ", sql='" + sql + '\'' +
                ", paramNames=" + paramNames.toString() +
                '}';
    }
}

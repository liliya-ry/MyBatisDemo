package session;

import dto.Configuration;
import dto.queries.*;
import handlers.DaoHandler;
import handlers.DaoHandlerAnnotated;
import utility.DatabaseConnectionPool;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SqlSession implements Closeable {
    private final DatabaseConnectionPool dcp;
    private final Connection conn;
    private final Configuration configuration;

    SqlSession(Configuration configuration, Connection conn, DatabaseConnectionPool dcp) {
        this.configuration = configuration;
        this.conn = conn;
        this.dcp = dcp;
    }

    //    <E> List<E> selectList(String statement, Object parameter);
//    <K,V> Map<K,V> selectMap(String statement, Object parameter, String mapKey);

    public <T> T selectOne(String queryId, Object params) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectOne(selectQuery, params);
    }

    public  <T> T selectOne(SelectQuery selectQuery, Object params) throws Exception {
        Class<?> paramType = selectQuery.getParameterType();
        if (!paramType.equals(params.getClass())) {
            throw new Exception("wrong parameter type");
        }

        String sql = selectQuery.getSql();
        ArrayList<String> paramNames = (ArrayList<String>) selectQuery.getParamNames();
        Class<?> resultType = selectQuery.getResultType();
        HashMap<String, Field> fieldsMap = getFieldsMap(resultType);

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            setParameters(st, params, paramNames, fieldsMap);
            ResultSet rs = st.executeQuery();
            rs.next();

            Constructor<?> constructor = resultType.getDeclaredConstructor();
            T res = getObject(rs, constructor, fieldsMap);
            if (rs.next()) {
                throw new IllegalStateException("More than one row in result");
            }
            return res;
        }
    }

    public <T> List<T> selectList(String queryId) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectList(selectQuery);
    }

    private <T> List<T> selectList(SelectQuery selectQuery) throws Exception {
        Class<?> paramType = selectQuery.getResultType();

        String sql = selectQuery.getSql();
        HashMap<String, Field> fieldsMap = getFieldsMap(paramType);

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            ResultSet rs = st.executeQuery();
            Constructor<?> constructor = paramType.getDeclaredConstructor();
            return getObjectList(rs, constructor, fieldsMap);
        }
    }

    private <T> List<T> getObjectList(ResultSet rs, Constructor<?> constructor, HashMap<String, Field> fieldsMap) throws Exception {
        List<T> result = new ArrayList<>();
        while (rs.next()) {
            T object = getObject(rs, constructor, fieldsMap);
            result.add(object);
        }
        rs.close();

        return result;
    }

    public int insert(String queryId, Object params) throws Exception {
        InsertQuery insertQuery;
        try {
            insertQuery = (InsertQuery) configuration.getQueryById(queryId);
        } catch (ClassCastException e) {
            throw new Exception("Invalid query type");
        }

        return insert(insertQuery, params);
    }

    private int insert(InsertQuery insertQuery, Object params) throws Exception {
        return insertQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(insertQuery, params, insertQuery.getKeyProperty()) :
                executeQuery(insertQuery, params);
    }

    public int update(String queryId, Object params) throws Exception {
        UpdateQuery updateQuery;
        try {
            updateQuery = (UpdateQuery) configuration.getQueryById(queryId);
        } catch (ClassCastException e) {
            throw new Exception("Invalid query type");
        }

        return update(updateQuery, params);
    }

    public int update(UpdateQuery updateQuery, Object params) throws Exception {
        return updateQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(updateQuery, params, updateQuery.getKeyProperty()) :
                executeQuery(updateQuery, params);
    }

    public int delete(String queryId, Object params) throws Exception {
        DeleteQuery deleteQuery = (DeleteQuery) configuration.getQueryById(queryId);
        checkQueryType(deleteQuery, Query.QUERY_TYPE.DELETE);
        return delete(deleteQuery, params);
    }

    public int delete(DeleteQuery deleteQuery, Object params) throws Exception {
        return executeQuery(deleteQuery, params);
    }

    private void checkQueryType(Query query, Query.QUERY_TYPE queryType) throws Exception {
        Query.QUERY_TYPE type = query.getQueryType();

        if (!type.equals(queryType)) {
            throw new Exception("Invalid query type");
        }
    }

    private int executeQuery(Query query, Object params) throws Exception {
        Class<?> paramType = query.getParameterType();
        if (!paramType.equals(params.getClass())) {
            throw new Exception("wrong parameter type");
        }

        String sql = query.getSql();
        ArrayList<String> paramNames = (ArrayList<String>) query.getParamNames();
        HashMap<String, Field> fieldsMap = getFieldsMap(paramType);

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            setParameters(st, params, paramNames, fieldsMap);
            return st.executeUpdate();
        }
    }

    private int executeQueryWithGeneratedKeys(Query query, Object params, String keyProperty) throws Exception {
        Class<?> paramType = query.getParameterType();
        if (!paramType.equals(params.getClass())) {
            throw new Exception("wrong parameter type");
        }
        String sql = query.getSql();
        ArrayList<String> paramNames = (ArrayList<String>) query.getParamNames();
        HashMap<String, Field> fieldsMap = getFieldsMap(paramType);

        try (PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(st, params, paramNames, fieldsMap);
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();
            rs.close();
            return rs.findColumn(keyProperty);
        }
    }

    private void setParameters(PreparedStatement st, Object o, ArrayList<String> fNames, HashMap<String, Field> fieldsMap) throws Exception {
        if (fNames.size() == 1) {
            st.setObject(1, o);
            return;
        }

        for (int i = 0; i < fNames.size(); i++) {
            Field f = fieldsMap.get(fNames.get(i));
            f.setAccessible(true);
            Object value = f.get(o);
            st.setObject(i + 1, value);
        }
    }

    private <T> HashMap<String, Field> getFieldsMap(Class<T> c) {
        HashMap<String, Field> fieldsMap = new HashMap<>();
        Field[] fields = c.getDeclaredFields();

        for (Field f : fields) {
            String fName = getNormalizedFieldName(f.getName());
            fieldsMap.put(fName, f);
        }

        return fieldsMap;
    }

    private <T> T getObject(ResultSet rs, Constructor<?> constructor, HashMap<String, Field> fieldsMap) throws Exception {
        T o = (T) constructor.newInstance();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = rsmd.getColumnName(i);
            String normalizedName = getNormalizedFieldName(columnName);

            if (!fieldsMap.containsKey(normalizedName)) {
                throw new IllegalArgumentException("No such field.");
            }

            Object value = rs.getObject(columnName);
            Field f = fieldsMap.get(normalizedName);
            f.setAccessible(true);
            f.set(o, value);
        }

        return o;
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

    public <T> T getMapper(Class<T> type) throws Exception {
        var handler =  configuration.getClassMappers().contains(type) ?
                new DaoHandlerAnnotated(this, type) :
                new DaoHandler(this, configuration);
        return (T) Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class[]{type}, handler);
    }

    public Connection getConnection() {
        return this.conn;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void close() {
        try {
            if (this.dcp != null)
                dcp.releaseConnection(conn);
            else
                conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

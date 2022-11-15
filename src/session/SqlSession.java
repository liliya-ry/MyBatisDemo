package session;

import dto.Configuration;
import dto.queries.*;
import exceptions.IbatisException;
import exceptions.TooManyResultsException;
import handlers.DaoHandler;
import utility.DatabaseConnectionPool;

import java.io.Closeable;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class SqlSession implements Closeable {
    private final DatabaseConnectionPool dcp;
    private final Connection conn;
    private final Configuration configuration;

    SqlSession(Configuration configuration, Connection conn, DatabaseConnectionPool dcp) {
        this.configuration = configuration;
        this.conn = conn;
        this.dcp = dcp;
    }

//    <K,V> Map<K,V> selectMap(String queryId, Object params, String mapId) throws Exception {
//        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
//        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
//        ResultMap resultMap = configuration.getResultMapById(mapId);
//    }

    public <T> T selectOne(String queryId) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectOne(selectQuery);
    }

    <T> T selectOne(SelectQuery selectQuery) throws Exception {
        String sql = selectQuery.getSql();
        Class<?> resultType = selectQuery.getResultType();
        Map<String, Field> fieldsMap = selectQuery.getFieldsMap();

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            Constructor<?> constructor = resultType.getDeclaredConstructor();
            T res = getObject(rs, constructor, fieldsMap);
            manyResultsCheck(rs);

            return res;
        }
    }

    public <T> T selectOne(String queryId, Object params) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectOne(selectQuery, params);
    }

     <T> T selectOne(SelectQuery selectQuery, Object params) throws Exception {
        String sql = selectQuery.getSql();
        List<String> paramNames = selectQuery.getParamNames();
        Class<?> resultType = selectQuery.getResultType();
        Map<String, Field> resultFieldsMap = selectQuery.getResultFieldsMap();
        Map<String, Field> fieldsMap = selectQuery.getFieldsMap();

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            setParameters(st, params, paramNames, fieldsMap);
            ResultSet rs = st.executeQuery();
            rs.next();
            Constructor<?> constructor = resultType.getDeclaredConstructor();
            T res = getObject(rs, constructor, resultFieldsMap);
            manyResultsCheck(rs);

            return res;
        }
    }

    private void manyResultsCheck(ResultSet rs) throws SQLException {
        if (rs.next()) {
            rs.last();
            int count = rs.getRow();
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + count);
        }
    }

    public <T> List<T> selectList(String queryId) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectList(selectQuery);
    }

    <T> List<T> selectList(SelectQuery selectQuery) throws Exception {
        Class<?> resultType = selectQuery.getResultType();
        String sql = selectQuery.getSql();
        Map<String, Field> resultFieldsMap = selectQuery.getResultFieldsMap();

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            Constructor<?> constructor = resultType.getDeclaredConstructor();
            return getObjectList(rs, constructor, resultFieldsMap);
        }
    }

    public <T> List<T> selectList(String queryId, Object params) throws Exception {
        SelectQuery selectQuery = (SelectQuery) configuration.getQueryById(queryId);
        checkQueryType(selectQuery, Query.QUERY_TYPE.SELECT);
        return selectList(selectQuery, params);
    }

    <T> List<T> selectList(SelectQuery selectQuery, Object params) throws Exception {
        String sql = selectQuery.getSql();
        List<String> paramNames = selectQuery.getParamNames();
        Class<?> resultType = selectQuery.getResultType();
        Map<String, Field> resultFieldsMap = selectQuery.getResultFieldsMap();
        Map<String, Field> fieldsMap = selectQuery.getFieldsMap();

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            setParameters(st, params, paramNames, fieldsMap);
            ResultSet rs = st.executeQuery();
            Constructor<?> constructor = resultType.getDeclaredConstructor();
            return getObjectList(rs, constructor, resultFieldsMap);
        }
    }

    private <T> List<T> getObjectList(ResultSet rs, Constructor<?> constructor, Map<String, Field> fieldsMap) throws Exception {
        List<T> result = new ArrayList<>();
        while (rs.next()) {
            T object = getObject(rs, constructor, fieldsMap);
            result.add(object);
        }
        rs.close();

        return result;
    }

    public int insert(String queryId) throws Exception {
        InsertQuery insertQuery = (InsertQuery) configuration.getQueryById(queryId);
        checkQueryType(insertQuery, Query.QUERY_TYPE.INSERT);
        return insert(insertQuery);
    }

    int insert(InsertQuery insertQuery) throws Exception {
        return insertQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(insertQuery, insertQuery.getKeyProperty()) :
                executeQuery(insertQuery);
    }

    public int insert(String queryId, Object params) throws Exception {
        InsertQuery insertQuery = (InsertQuery) configuration.getQueryById(queryId);
        checkQueryType(insertQuery, Query.QUERY_TYPE.INSERT);
        return insert(insertQuery, params);
    }

    int insert(InsertQuery insertQuery, Object params) throws Exception {
        return insertQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(insertQuery, params, insertQuery.getKeyProperty()) :
                executeQuery(insertQuery, params);
    }

    public int update(String queryId) throws Exception {
        UpdateQuery updateQuery = (UpdateQuery) configuration.getQueryById(queryId);
        checkQueryType(updateQuery, Query.QUERY_TYPE.UPDATE);
        return update(updateQuery);
    }

    public int update(String queryId, Object params) throws Exception {
        UpdateQuery updateQuery = (UpdateQuery) configuration.getQueryById(queryId);
        checkQueryType(updateQuery, Query.QUERY_TYPE.UPDATE);
        return update(updateQuery, params);
    }

    int update(UpdateQuery updateQuery) throws Exception {
        return updateQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(updateQuery, updateQuery.getKeyProperty()) :
                executeQuery(updateQuery);
    }

    int update(UpdateQuery updateQuery, Object params) throws Exception {
        return updateQuery.isUseGeneratedKeys() ?
                executeQueryWithGeneratedKeys(updateQuery, params, updateQuery.getKeyProperty()) :
                executeQuery(updateQuery, params);
    }

    public int delete(String queryId) throws Exception {
        Query deleteQuery = configuration.getQueryById(queryId);
        checkQueryType(deleteQuery, Query.QUERY_TYPE.DELETE);
        return delete(deleteQuery);
    }

    public int delete(String queryId, Object params) throws Exception {
        Query deleteQuery = configuration.getQueryById(queryId);
        checkQueryType(deleteQuery, Query.QUERY_TYPE.DELETE);
        return delete(deleteQuery, params);
    }

    int delete(Query deleteQuery) throws Exception {
        return executeQuery(deleteQuery);
    }

    int delete(Query deleteQuery, Object params) throws Exception {
        return executeQuery(deleteQuery, params);
    }

    private void checkQueryType(Query query, Query.QUERY_TYPE queryType) throws Exception {
        Query.QUERY_TYPE type = query.getQueryType();

        if (!type.equals(queryType)) {
            throw new Exception("Invalid query type");
        }
    }

    private int executeQuery(Query query) throws Exception {
        String sql = query.getSql();
        try (Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    private int executeQuery(Query query, Object params) throws Exception {
        String sql = query.getSql();
        List<String> paramNames = query.getParamNames();
        Map<String, Field> fieldsMap = query.getFieldsMap();

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            setParameters(st, params, paramNames, fieldsMap);
            return st.executeUpdate();
        }
    }

    private int executeQueryWithGeneratedKeys(Query query, String keyProperty) throws Exception {
        String sql = query.getSql();
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = st.getGeneratedKeys();
            rs.next();
            rs.close();
            return rs.findColumn(keyProperty);
        }
    }

    private int executeQueryWithGeneratedKeys(Query query, Object params, String keyProperty) throws Exception {
        String sql = query.getSql();
        List<String> paramNames = query.getParamNames();
        Map<String, Field> fieldsMap = query.getFieldsMap();

        try (PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(st, params, paramNames, fieldsMap);
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();
            rs.close();
            return rs.findColumn(keyProperty);
        }
    }

    private void setParameters(PreparedStatement st, Object o, List<String> fNames, Map<String, Field> fieldsMap) throws Exception {
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

    private <T> T getObject(ResultSet rs, Constructor<?> constructor, Map<String, Field> fieldsMap) throws Exception {
        T o = (T) constructor.newInstance();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = rsmd.getColumnName(i);
            String normalizedName = getNormalizedFieldName(columnName);

            if (!fieldsMap.containsKey(normalizedName)) {
                throw new IllegalArgumentException("No such field.");
            }

            Object value;
            try {
                value = rs.getObject(columnName);
            } catch (SQLException e) {
                return null;
            }

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

    public <T> T getMapper(Class<T> type) {
        if (!configuration.getClassMappers().contains(type)) {
            throw new IbatisException("Type interface " + type +" is not known to the MapperRegistry");
        }

        var handler = new DaoHandler(this, configuration, type);
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

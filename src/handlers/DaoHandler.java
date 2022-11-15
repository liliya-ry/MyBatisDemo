package handlers;

import dto.Configuration;
import dto.Mapper;
import dto.queries.*;
import session.SqlSession;
import utility.Cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class DaoHandler implements InvocationHandler {
    private final SqlSession session;
    private final Configuration configuration;
    private final Map<String, Cache<Object, Object>> caches;

    public DaoHandler(SqlSession session, Configuration configuration, Class<?> mapperType) {
        this.session = session;
        this.configuration = configuration;
        String namespace = mapperType.toString();
        Mapper mapper = configuration.getMapperByNamespace(namespace);
        this.caches = mapper.getCaches();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return caches == null ?
                invokeWithoutCache(proxy, method, args) :
                invokeFromCache(proxy, method, args);
    }

    private Object invokeFromCache(Object proxy, Method method, Object[] args) throws Throwable {
        String queryId = method.getName();
        Query query = configuration.getQueryById(queryId);
        Query.QUERY_TYPE queryType = query.getQueryType();

        switch (queryType) {
            case INSERT -> {
                InsertQuery insertQuery = (InsertQuery) query;
                if (insertQuery.isFlushCache()) {
                    flushAllCaches();
                }
                return session.insert(queryId, args[0]);
            }
            case UPDATE -> {
                UpdateQuery updateQuery = (UpdateQuery) query;
                if (updateQuery.isFlushCache()) {
                    flushAllCaches();
                }
                return session.update(queryId, args[0]);
            }
            case DELETE -> {
                DeleteQuery deleteQuery = (DeleteQuery) query;
                if (deleteQuery.isFlushCache()) {
                    flushAllCaches();
                }
                return session.delete(queryId, args[0]);
            }
            case SELECT -> {
                SelectQuery selectQuery = (SelectQuery) query;
                Class<?> resultType = selectQuery.getResultType();
                boolean useCache = selectQuery.isUseCaching();
                return useCache ? invokeSelectFromCache(queryId, method.getName(), args[0], resultType) :
                        invokeSelect(queryId, args[0], resultType);
            }
        }
        return  method.invoke(proxy, args);
    }

    private void flushAllCaches() {
        for (Map.Entry<String, Cache<Object, Object>> cacheEntry : caches.entrySet()) {
            Cache<Object, Object> cache = cacheEntry.getValue();
            cache.flushCache();
        }
    }

    private Object invokeWithoutCache(Object proxy, Method method, Object[] args) throws Throwable {
        String queryId = method.getName();
        Query query = configuration.getQueryById(queryId);
        Query.QUERY_TYPE queryType = query.getQueryType();

        switch (queryType) {
            case INSERT:
                return session.insert(queryId, args[0]);
            case UPDATE:
                return session.update(queryId, args[0]);
            case DELETE:
                return session.delete(queryId, args[0]);
            case SELECT:
                SelectQuery selectQuery = (SelectQuery) query;
                Class<?> resultType = selectQuery.getResultType();
                return invokeSelect(queryId, args[0], resultType);
        }
        return  method.invoke(proxy, args);
    }

    private Object invokeSelect(String queryId, Object param, Class<?> resultType) throws Throwable {
        return resultType.equals(List.class) ?
                session.selectList(queryId, param) :
                session.selectOne(queryId, param);
    }

    private Object invokeSelectFromCache(String queryId, String methodName, Object param, Class<?> resultType) throws Exception {
        Cache<Object, Object> cache = caches.get(methodName);
        Object res = cache.get(param);

        if (res != null) {
            System.out.println("found");
            return res;
        }

        res = resultType.equals(List.class) ?
                session.selectList(queryId, param) :
                session.selectOne(queryId, param);
        cache.set(param, res);

        return res;
    }
}

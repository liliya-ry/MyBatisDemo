package parsers;

import annotations.*;
import dto.Mapper;
import dto.queries.DeleteQuery;
import dto.queries.Query;
import dto.queries.SelectQuery;
import dto.queries.UpdateQuery;
import utility.FifoCache;
import utility.GenerationalCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;


public class AnnotatedMapperParser {
    private final Class<?> mapperType;
    private Map<String, Query> queryMap;
    private Map<String, utility.Cache<Object, Object>> caches; //key - methodName, Cache - key - param , value - result
    private long flushInterval;
    private int size;
    private Constructor<?> cacheConstructor;

    public AnnotatedMapperParser(Class<?> mapperType) throws Exception {
        this.mapperType = mapperType;
        this.queryMap = new HashMap<>();

        Cache cacheAnnotation = mapperType.getAnnotation(Cache.class);
        if (cacheAnnotation != null) {
            caches = new HashMap<>();
            getCacheProperties(cacheAnnotation);
        }

        fillMethodsQueryMap();
    }

    public Mapper parseMapper() throws Exception {
        return new Mapper(mapperType.toString(), queryMap, null, caches);
    }

    private void getCacheProperties(Cache cacheAnnotation) throws Exception {
        this.flushInterval = cacheAnnotation.timeToLiveSeconds();
        this.size = cacheAnnotation.maxEntriesLocalHeap();
        String eviction = cacheAnnotation.memoryStoreEvictionPolicy();

        Class<?> cacheType = switch (eviction) {
            case "FIFO" -> FifoCache.class;
            case "LRU" -> GenerationalCache.class;
            default -> throw new Exception("Illegal cache type");
        };

        this.cacheConstructor = cacheType.getDeclaredConstructor(int.class, long.class);
    }


    private void fillMethodsQueryMap() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        this.queryMap = new HashMap<>();
        Method[] methods = mapperType.getDeclaredMethods();

        for (Method method : methods) {
            Annotation[] annotations = method.getDeclaredAnnotations();
            Query query = getQueryItemByAnnotation(annotations, method);
            queryMap.put(query.getId(), query);
        }
    }

    private Query getQueryItemByAnnotation(Annotation[] annotations, Method method) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> annotationType = annotations[0].annotationType();
        Parameter[] parameters = method.getParameters();
        Class<?> paramType = parameters.length == 0 ? null: parameters[0].getType();
        String id = method.getName();

        if (annotationType.equals(Select.class)) {
            return getSelectQuery(annotations[0], method, paramType, id);
        }

        Class<?> annotationType2 = annotations[1].annotationType();
        boolean useKeys = false;
        String keyProperty = null;
        boolean flushCache = false;


        if (annotationType2.equals(Options.class)) {
            Options options = (Options) annotations[1];
            useKeys = options.useGeneratedKeys();
            keyProperty = options.keyProperty();
            flushCache = options.flushCache();
        }

        if (annotationType.equals(Delete.class)) {
            Delete delete = (Delete) annotations[0];
            String sql = delete.value();
            return new DeleteQuery(Query.QUERY_TYPE.DELETE, id, int.class, sql, flushCache);
        }

        if (annotationType.equals(Insert.class)) {
            Insert insert = (Insert) annotations[0];
            String sql = insert.value();
            return new UpdateQuery(Query.QUERY_TYPE.INSERT, id, paramType, sql, useKeys, keyProperty, flushCache);
        }

        if (annotationType.equals(Update.class)) {
            Update update = (Update) annotations[0];
            String sql = update.value();
            return new UpdateQuery(Query.QUERY_TYPE.UPDATE, id, paramType, sql, useKeys, keyProperty, flushCache);
        }

        return null;
    }

    private SelectQuery getSelectQuery(Annotation annotation, Method method, Class<?> paramType, String id) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Select select = (Select) annotation;
        String sql = select.value();
        boolean useCaching = select.useCaching();
        if (useCaching) {
            utility.Cache<Object, Object> cache = (utility.Cache<Object, Object>) cacheConstructor.newInstance(size, flushInterval);
            caches.put(method.getName(), cache);
        }
        Class<?> resultType = method.getReturnType();
        return new SelectQuery(Query.QUERY_TYPE.SELECT, id, paramType, sql, resultType, null, useCaching);
    }
}

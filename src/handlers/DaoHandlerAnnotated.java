package handlers;

import dto.queries.DeleteQuery;
import dto.queries.Query;
import dto.queries.SelectQuery;
import dto.queries.UpdateQuery;
import handlers.annotations.*;
import session.SqlSession;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DaoHandlerAnnotated implements InvocationHandler {
    private static class AnnotationItem {
        Annotation annotation;
        Class<?> aType;
        String sql;
        AnnotationItem(Annotation annotation, Class<?> aType, String sql) {
            this.annotation = annotation;
            this.aType = aType;
            this.sql = sql;
        }
    }

    private final SqlSession session;
    private final Class<?> mapperInterfaceType;
    private Map<Method, AnnotationItem> methodsMap;

    public DaoHandlerAnnotated(SqlSession session, Class<?> mapperInterfaceType) {
        this.session = session;
        this.mapperInterfaceType = mapperInterfaceType;
        fillMethodsMap();
    }

    private void fillMethodsMap() {
        Method[] methods = mapperInterfaceType.getDeclaredMethods();
        methodsMap = new HashMap<>();

        for (Method method : methods) {
            Annotation[] annotations = method.getDeclaredAnnotations();
            if (annotations.length == 0) {
                continue;
            }

            for (Annotation a : annotations) {
                Class<?> aType = a.annotationType();
                String sql = getSqlString(a, aType);
                if (sql != null) {
                    AnnotationItem item = new AnnotationItem(a, aType, sql);
                    methodsMap.put(method, item);
                    break;
                }
            }
        }
    }

    private String getSqlString(Annotation a, Class<?> aType) {
        if (aType.equals(Select.class)) {
            return  ((Select) a).value();
        }

        if (aType.equals(Insert.class)) {
            return  ((Insert) a).value();
        }

        if (aType.equals(Update.class)) {
            return  ((Update) a).value();
        }

        if (aType.equals(Delete.class)) {
            return  ((Delete) a).value();
        }

        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        AnnotationItem item = methodsMap.get(method);
        String sql = item.sql;

        if (sql == null) {
            return method.invoke(session, args);
        }

        String methodName = method.getName();
        Class<?> paramType = args[0].getClass();

        if (item.aType.equals(Delete.class)) {
            DeleteQuery deleteQuery = new DeleteQuery(Query.QUERY_TYPE.DELETE, methodName, paramType, item.sql);
            return session.delete(deleteQuery, args[0]);
        }

        if (item.aType.equals(Update.class)) {
            boolean useKeys = false;
            String keyProperty = null;
            Options options = method.getAnnotation(Options.class);
            if (options != null) {
                useKeys = options.useGeneratedKeys();
                keyProperty = options.keyProperty();
            }
            UpdateQuery updateQuery = new UpdateQuery(Query.QUERY_TYPE.UPDATE,
                    methodName, paramType, item.sql, useKeys, keyProperty);
            return session.update(updateQuery, args[0]);
        }

        if (item.aType.equals(Insert.class)) {
            return session.insert(sql, args[0]);
        }

        if (item.aType.equals(Select.class)) {
            SelectQuery selectQuery = new SelectQuery(Query.QUERY_TYPE.SELECT, methodName, paramType, item.sql, method.getReturnType(), null);
          return session.selectOne(selectQuery, args[0]);
        }

        return null;
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
}

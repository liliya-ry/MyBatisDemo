package handlers;

import dto.Configuration;
import dto.queries.Query;
import session.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class DaoHandler implements InvocationHandler {
    private final SqlSession session;
    private final Configuration configuration;

    public DaoHandler(SqlSession session, Configuration configuration) {
        this.session = session;
        this.configuration = configuration;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String queryId = method.getName();
        Query query = configuration.getQueryById(queryId);
        Query.QUERY_TYPE queryType = query.getQueryType();

        switch (queryType) {
            case SELECT:
                Class<?> returnType = method.getReturnType();
                return  returnType.equals(List.class) ?
                        session.selectList(queryId) :
                        session.selectOne(queryId, args[0]);
            case INSERT:
                return session.insert(queryId, args[0]);
            case UPDATE:
                return session.update(queryId, args[0]);
            case DELETE:
                return session.delete(queryId, args[0]);
        }

        return method.invoke(session, args);
    }
}

package handlers;

import dto.Configuration;
import session.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DaoHandlerAnnotated implements InvocationHandler {
    private final SqlSession session;
    private final Configuration configuration;

    public DaoHandlerAnnotated(SqlSession session, Configuration configuration) {
        this.session = session;
        this.configuration = configuration;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}

package session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DaoHandler implements InvocationHandler {
    SqlSession sqlSession;
    Class<?> mapperType;

    public <T> DaoHandler(SqlSession sqlSession, Class<T> type) {
        this.sqlSession = sqlSession;
        this.mapperType = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}

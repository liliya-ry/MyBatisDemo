package session;

import dto.Configuration;

public interface SqlSessionFactory {
    public Configuration getConfiguration();
    public SqlSession openSession() throws Exception;
    public SqlSession openSession(Configuration configuration);
}

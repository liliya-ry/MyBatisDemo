package service;

import session.SqlSessionFactory;
import session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;

public class MyBatisUtil {
    private static SqlSessionFactory factory = null;

    private MyBatisUtil() {}

    static {
        String resource = "D:\\IdeaProjects\\MyBatisDemo\\src\\resources\\mybatis-config.xml";
        try {
            FileInputStream fis = new FileInputStream(resource);
            factory = new SqlSessionFactoryBuilder().build(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return factory;
    }
}

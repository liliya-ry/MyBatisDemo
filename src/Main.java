import domain.Employee;
import mappers.EmployeeMapperAnnotated;
import session.SqlSession;
import session.SqlSessionFactory;
import session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String resource = "D:\\IdeaProjects\\MyBatisDemo\\src\\resources\\mybatis-config.xml";
        FileInputStream fis = new FileInputStream(resource);
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(fis);
        SqlSession session = factory.openSession();
        EmployeeMapperAnnotated mapper = session.getMapper(EmployeeMapperAnnotated.class);
        Employee e = mapper.getEmployeeById(544);
        System.out.println(e);
        session.close();
    }
}

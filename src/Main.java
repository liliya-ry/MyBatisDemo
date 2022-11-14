import domain.Employee;
import session.SqlSession;
import session.SqlSessionFactory;
import session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String resource = "D:\\IdeaProjects\\MyBatisDemo\\src\\resources\\mybatis-config.xml";
        FileInputStream fis = new FileInputStream(resource);
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(fis);
        SqlSession session = factory.openSession();
        Employee e = session.selectOne("getEmployeeById",100);
        System.out.println(e);
        List<Employee> employees = session.selectList("getAllEmployees");
        for (Employee employee : employees) {
            System.out.println(employee);
        }
        session.close();
    }
}

import domain.Employee;
import mappers.EmployeeMapper;
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
        EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
       // Employee e = new Employee("maria", "ivanov", "ivan@abv.bg", "1234", Date.valueOf("2022-10-02"), new BigDecimal(2000), 1, 6, 102);

        Employee e = mapper.getEmployeeById(542);
        System.out.println(e);
        session.close();
    }
}

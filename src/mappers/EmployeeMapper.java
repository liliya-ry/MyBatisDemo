package mappers;

import domain.Employee;

import java.util.List;

public interface EmployeeMapper {
    Employee getEmployeeById(Integer employeeId);

    List<Employee> getAllEmployees();

    void insertEmployee(Employee employee);

    void updateEmployee(Employee employee);

    void deleteEmployee(Integer employeeId);
}
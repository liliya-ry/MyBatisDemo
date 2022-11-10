package mappers;

import domain.Employee;

import java.util.List;

public interface EmployeeMapper {
    public Employee getEmployeeById(Integer employeeId);

    public List<Employee> getAllEmployees();

    public void insertEmployee(Employee employee);

    public void updateEmployee(Employee employee);

    public void deleteEmployee(Integer employeeId);
}
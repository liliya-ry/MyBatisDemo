package domain;

import java.math.BigDecimal;
import java.sql.Date;

public class Employee {
    public int employeeId;
    public String firstName;
    public String lastName;
    public String email;
    public String phoneNumber;
    public Date hireDate;
    public BigDecimal salary;
    public int jobId;
    public int departmentId;
    public int managerId;

    public Employee() {
        this.employeeId = 0;
        this.firstName = null;
        this.lastName = null;
        this.email = null;
        this.phoneNumber = null;
        this.hireDate = null;
        this.jobId = 0;
        this.departmentId = 0;
        this.managerId = 0;
    }

    public Employee(int id,
                    String firstName,
                    String lastName,
                    String email,
                    String phone,
                    Date hireDate,
                    BigDecimal salary,
                    int jobId,
                    int departmentId,
                    int managerId) {
        this.employeeId = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phone;
        this.hireDate = hireDate;
        this.salary = salary;
        this.jobId = jobId;
        this.departmentId = departmentId;
        this.managerId = managerId;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + employeeId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phoneNumber + '\'' +
                ", hireDate=" + hireDate +
                ", salary=" + salary +
                ", job=" + jobId +
                ", department=" +departmentId +
                ", manager=" + managerId +
                '}';
    }
}

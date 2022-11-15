package mappers;

import domain.Employee;
import annotations.*;

import java.util.List;

@Cache(memoryStoreEvictionPolicy = "LRU", maxEntriesLocalHeap = 100, timeToLiveSeconds = 3600)
public interface EmployeeMapperAnnotated {
    @Select(value = """
            SELECT
                    employee_id,
                    first_name,
                    last_name,
                    email,
                    phone_number,
                    hire_date,
                    job_id,
                    salary,
                    manager_id,
                    department_id
            FROM EMPLOYEES
            WHERE EMPLOYEE_ID = #{employeeId}
            """, useCaching = true)
    Employee getEmployeeById(int employeeId);

    @Select(value = "SELECT * FROM EMPLOYEES", useCaching = true)
    @Results(value = {
            @Result(id=true, property="employeeId", column="employee_id"),
            @Result(property="firstName", column="first_name"),
            @Result(property = "lastName", column = "lastName"),
            @Result(property="email", column="email"),
            @Result(property="phone_number", column="phone_number"),
            @Result(property="hire_date", column="hire_date"),
            @Result(property="job_id", column="job_id"),
            @Result(property="salary", column="salary"),
            @Result(property="manager_id", column="manager_id"),
            @Result(property="department_id", column="department_id")
    })
    List<Employee> getAllEmployees();

    @Insert("""
            INSERT INTO EMPLOYEES(
                    first_name,
                    last_name,
                    email,
                    phone_number,
                    hire_date,
                    job_id,
                    salary,
                    manager_id,
                    department_id
                    )
                    VALUES(
                    #{firstName},
                    #{lastName},
                    #{email},
                    #{phoneNumber},
                    #{hireDate},
                    #{jobId},
                    #{salary},
                    #{managerId},
                    #{departmentId})
            """)
    @Options(useGeneratedKeys=true, keyProperty="employeeId", flushCache = true)
    void insertEmployee(Employee employee);

    @Update("""
            UPDATE EMPLOYEES
            SET
                FIRST_NAME = #{firstName},
                LAST_NAME = #{lastName},
                EMAIL = #{email},
                PHONE_NUMBER = #{phoneNumber},
                HIRE_DATE = #{hireDate},
                JOB_ID = #{jobId},
                SALARY = #{salary},
                MANAGER_ID = #{managerId},
                DEPARTMENT_ID = #{departmentId}
            WHERE EMPLOYEE_ID = #{employeeId}
            """)
    @Options(useGeneratedKeys=true, keyProperty="employeeId")
    void updateEmployee(Employee employee);

    @Delete("DELETE FROM EMPLOYEES WHERE EMPLOYEE_ID=#{employeeId}")
    @Options(flushCache = true)
    void deleteEmployee(int employeeId);
}

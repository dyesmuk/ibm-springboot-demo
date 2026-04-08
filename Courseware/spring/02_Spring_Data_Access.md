# 02 — Spring Data Access: JDBC, ORM & AOP

> 🏢 **EMS Theme:** Persisting and retrieving `Employee` and `Department` data using Spring JDBC and ORM (Hibernate). Cross-cutting concerns like logging and transaction auditing handled via AOP.

---

## Table of Contents
- [Spring JDBC](#spring-jdbc)
- [Spring ORM — Hibernate Integration](#spring-orm--hibernate-integration)
- [Spring AOP](#spring-aop)

---

## Spring JDBC

### The Problem with Plain JDBC

Plain JDBC requires enormous boilerplate:
```java
// Traditional JDBC — 10+ lines for a simple query
Connection conn = null;
PreparedStatement ps = null;
ResultSet rs = null;
try {
    conn = DriverManager.getConnection(url, user, pass);
    ps = conn.prepareStatement("SELECT * FROM employees WHERE id = ?");
    ps.setInt(1, id);
    rs = ps.executeQuery();
    if (rs.next()) { /* map columns to object */ }
} catch (SQLException e) {
    e.printStackTrace();
} finally {
    // close rs, ps, conn — each in its own try-catch
}
```

Spring JDBC eliminates this with **`JdbcTemplate`** — you only write the SQL and the row mapping.

### Setup — Add Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jdbc</artifactId>
        <version>6.1.0</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <!-- Connection pool -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.1.0</version>
    </dependency>
</dependencies>
```

### Configure DataSource

```java
@Configuration
public class DataConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/emsdb");
        ds.setUsername("root");
        ds.setPassword("password");
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### EMS Schema

```sql
CREATE TABLE departments (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    location VARCHAR(100)
);

CREATE TABLE employees (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) UNIQUE,
    salary        DECIMAL(10,2),
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

### EmployeeRepository using JdbcTemplate

```java
@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── CREATE ──────────────────────────────────────────────────────
    public int save(Employee emp) {
        String sql = "INSERT INTO employees (name, email, salary, department_id) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
            emp.getName(), emp.getEmail(), emp.getSalary(), emp.getDepartmentId());
    }

    // ── READ — single ───────────────────────────────────────────────
    public Employee findById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new EmployeeRowMapper(), id);
    }

    // ── READ — all ──────────────────────────────────────────────────
    public List<Employee> findAll() {
        String sql = "SELECT * FROM employees";
        return jdbcTemplate.query(sql, new EmployeeRowMapper());
    }

    // ── READ — with JOIN ────────────────────────────────────────────
    public List<Employee> findByDepartment(String deptName) {
        String sql = """
            SELECT e.* FROM employees e
            JOIN departments d ON e.department_id = d.id
            WHERE d.name = ?
            """;
        return jdbcTemplate.query(sql, new EmployeeRowMapper(), deptName);
    }

    // ── UPDATE ──────────────────────────────────────────────────────
    public int updateSalary(int employeeId, double newSalary) {
        String sql = "UPDATE employees SET salary = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newSalary, employeeId);
    }

    // ── DELETE ──────────────────────────────────────────────────────
    public int delete(int employeeId) {
        String sql = "DELETE FROM employees WHERE id = ?";
        return jdbcTemplate.update(sql, employeeId);
    }

    // ── COUNT ───────────────────────────────────────────────────────
    public int countByDepartment(int deptId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE department_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, deptId);
    }
}
```

### RowMapper — Map ResultSet to Object

```java
public class EmployeeRowMapper implements RowMapper<Employee> {
    @Override
    public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        Employee emp = new Employee();
        emp.setId(rs.getInt("id"));
        emp.setName(rs.getString("name"));
        emp.setEmail(rs.getString("email"));
        emp.setSalary(rs.getDouble("salary"));
        emp.setDepartmentId(rs.getInt("department_id"));
        return emp;
    }
}
```

> 💡 **Tip:** In Spring Boot, you can use a lambda instead:
> ```java
> jdbcTemplate.query(sql, (rs, rowNum) -> {
>     Employee e = new Employee();
>     e.setId(rs.getInt("id"));
>     return e;
> });
> ```

### NamedParameterJdbcTemplate

More readable than `?` placeholders — uses `:paramName` syntax:

```java
@Bean
public NamedParameterJdbcTemplate namedJdbcTemplate(DataSource ds) {
    return new NamedParameterJdbcTemplate(ds);
}

// Usage
public int saveEmployee(Employee emp) {
    String sql = "INSERT INTO employees (name, email, salary) " +
                 "VALUES (:name, :email, :salary)";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("name",   emp.getName())
        .addValue("email",  emp.getEmail())
        .addValue("salary", emp.getSalary());
    return namedJdbcTemplate.update(sql, params);
}
```

---

## Spring ORM — Hibernate Integration

Spring ORM integrates Hibernate (JPA provider) into the Spring container — giving you dependency injection, declarative transactions, and Spring exception handling with Hibernate.

> 🔎 **Analogy:** Hibernate is a skilled translator between your Java objects and the database. Spring ORM is the project manager that coordinates the translator, manages their schedule (sessions/transactions), and handles any issues they raise.

### Additional Dependencies

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-orm</artifactId>
    <version>6.1.0</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.4.0.Final</version>
</dependency>
```

### Entity Classes — JPA Annotations

```java
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location")
    private String location;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    // constructors, getters, setters
}

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "salary")
    private double salary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // constructors, getters, setters
}
```

### Spring ORM Configuration

```java
@Configuration
@EnableTransactionManagement
public class OrmConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sf = new LocalSessionFactoryBean();
        sf.setDataSource(dataSource);
        sf.setPackagesToScan("com.ems.model");
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.hbm2ddl.auto", "update"); // auto-create/update tables
        sf.setHibernateProperties(props);
        return sf;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sf) {
        return new HibernateTransactionManager(sf);
    }
}
```

### EmployeeRepository using Hibernate Session

```java
@Repository
public class EmployeeOrmRepository {

    @Autowired
    private SessionFactory sessionFactory;

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional
    public void save(Employee emp) {
        currentSession().persist(emp);
    }

    @Transactional(readOnly = true)
    public Employee findById(int id) {
        return currentSession().get(Employee.class, id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return currentSession()
            .createQuery("FROM Employee", Employee.class)
            .list();
    }

    @Transactional(readOnly = true)
    public List<Employee> findByDepartment(String deptName) {
        return currentSession()
            .createQuery(
                "FROM Employee e WHERE e.department.name = :deptName",
                Employee.class)
            .setParameter("deptName", deptName)
            .list();
    }

    @Transactional
    public void updateSalary(int empId, double newSalary) {
        Employee emp = currentSession().get(Employee.class, empId);
        if (emp != null) {
            emp.setSalary(newSalary); // Hibernate detects change — auto UPDATE on commit
        }
    }

    @Transactional
    public void delete(int empId) {
        Employee emp = currentSession().get(Employee.class, empId);
        if (emp != null) {
            currentSession().remove(emp);
        }
    }
}
```

### `@Transactional` explained

```java
@Transactional                      // Begins transaction, commits on success, rolls back on exception
@Transactional(readOnly = true)     // Optimisation hint for SELECT queries
@Transactional(rollbackFor = Exception.class)  // Roll back on checked exceptions too
@Transactional(propagation = Propagation.REQUIRES_NEW)  // Always start a new transaction
```

> 💡 **Tip:** `@Transactional` on a class means all methods in that class are transactional. Put it on the service layer — not the repository, and never on the controller.

---

## Spring AOP

AOP (Aspect-Oriented Programming) lets you separate **cross-cutting concerns** from business logic — logging, security checks, performance monitoring, auditing.

> 🔎 **Analogy:** In a hotel, every room has a "Do Not Disturb" sign protocol. The protocol (cross-cutting concern) applies to all rooms without being part of each room's design. AOP is that protocol — it wraps around your methods without you writing it inside each method.

### AOP Concepts

| Term | Meaning | EMS Example |
|---|---|---|
| **Aspect** | The cross-cutting concern | `LoggingAspect`, `AuditAspect` |
| **Advice** | The action to take | Log before/after method runs |
| **Pointcut** | Which methods to intercept | All methods in `com.ems.service.*` |
| **JoinPoint** | The actual method being intercepted | `EmployeeService.save()` |
| **Weaving** | Applying aspect to target | Spring does this at runtime |

### Types of Advice

| Advice | When It Runs |
|---|---|
| `@Before` | Before the method executes |
| `@After` | After the method (always — like finally) |
| `@AfterReturning` | After method returns successfully |
| `@AfterThrowing` | After method throws an exception |
| `@Around` | Wraps the method — before AND after |

### Setup

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>6.1.0</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.21</version>
</dependency>
```

Enable AOP in config:
```java
@Configuration
@EnableAspectJAutoProxy
public class AppConfig { }
```

### EMS Logging Aspect

```java
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Pointcut: all methods in any class in com.ems.service package
    @Pointcut("execution(* com.ems.service.*.*(..))")
    public void serviceLayer() {}

    // Before advice — runs before every service method
    @Before("serviceLayer()")
    public void logBefore(JoinPoint jp) {
        log.info(">>> Calling: {}.{}",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }

    // AfterReturning — logs what was returned
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) {
        log.info("<<< Returned from: {} — result: {}",
            jp.getSignature().getName(), result);
    }

    // AfterThrowing — logs exceptions
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint jp, Exception ex) {
        log.error("!!! Exception in {}: {}",
            jp.getSignature().getName(), ex.getMessage());
    }
}
```

### EMS Audit Aspect — Track Who Did What

```java
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Around advice on all save/update/delete operations
    @Around("execution(* com.ems.service.*.save*(..)) || " +
            "execution(* com.ems.service.*.update*(..)) || " +
            "execution(* com.ems.service.*.delete*(..))")
    public Object auditOperation(ProceedingJoinPoint pjp) throws Throwable {
        String operation = pjp.getSignature().getName();
        long start = System.currentTimeMillis();

        Object result = pjp.proceed(); // Execute the actual method

        long duration = System.currentTimeMillis() - start;

        // Log the audit entry
        AuditLog log = new AuditLog();
        log.setOperation(operation);
        log.setExecutedAt(LocalDateTime.now());
        log.setDurationMs(duration);
        auditLogRepository.save(log);

        System.out.printf("AUDIT: %s completed in %dms%n", operation, duration);
        return result;
    }
}
```

### Performance Monitoring Aspect

```java
@Aspect
@Component
public class PerformanceAspect {

    @Around("execution(* com.ems.service.*.*(..))")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();

        System.out.printf("[PERF] %s executed in %d ms%n",
            pjp.getSignature().toShortString(), (end - start));

        return result;
    }
}
```

### Pointcut Expressions Quick Reference

```java
// All methods in a package
"execution(* com.ems.service.*.*(..))"

// Specific method
"execution(* com.ems.service.EmployeeService.save(..))"

// Method with specific return type
"execution(Employee com.ems.service.*.*(..))"

// Methods with specific annotation
"@annotation(org.springframework.transaction.annotation.Transactional)"

// All beans with @Service annotation
"@within(org.springframework.stereotype.Service)"

// Named pointcut reuse
@Pointcut("execution(* com.ems.service.*.*(..))")
public void serviceLayer() {}

@Before("serviceLayer()")
public void before() {}
```

---

## Summary

| Feature | Purpose | Key Class/Annotation |
|---|---|---|
| `JdbcTemplate` | Simplified JDBC — no boilerplate | `JdbcTemplate.update()`, `.query()` |
| `RowMapper` | Map ResultSet rows to objects | `RowMapper<T>` interface |
| `NamedParameterJdbcTemplate` | Named `:param` instead of `?` | `MapSqlParameterSource` |
| Spring ORM | Hibernate + Spring integration | `LocalSessionFactoryBean` |
| `@Transactional` | Declarative transaction management | `@EnableTransactionManagement` |
| `@Aspect` | Define a cross-cutting concern | `@Aspect`, `@Component` |
| `@Before/@After` | Advice types | `JoinPoint` |
| `@Around` | Full method wrapping | `ProceedingJoinPoint` |
| Pointcut expression | Define which methods to intercept | `execution(* com.ems.service.*.*(..))` |

---

*Previous: [01 — Spring Core](01_Spring_Core.md) | Next: [03 — Spring MVC](03_Spring_MVC.md)*

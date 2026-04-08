# 05 — Spring Data JPA

> 🏢 **EMS Theme:** Complete JPA-based persistence for all EMS entities — Employee, Department, Role, Project — with relationships, custom queries, and caching.

---

## Table of Contents
- [Introduction to Spring Data JPA](#introduction-to-spring-data-jpa)
- [Entity Mapping — Full EMS Model](#entity-mapping--full-ems-model)
- [Repository Pattern](#repository-pattern)
- [Derived Query Methods](#derived-query-methods)
- [JPQL and Native Queries](#jpql-and-native-queries)
- [Entity Relationships](#entity-relationships)
- [Pagination and Sorting](#pagination-and-sorting)
- [Database Caching](#database-caching)
- [Validations](#validations)

---

## Introduction to Spring Data JPA

Spring Data JPA sits on top of JPA (Java Persistence API) and Hibernate. It eliminates DAO boilerplate — you define an interface, Spring generates the implementation at runtime.

```
Your Code
    └── EmployeeRepository (interface)
            └── Spring Data JPA (generates implementation)
                    └── JPA / Hibernate
                            └── MySQL (or any RDBMS)
```

**What you don't have to write:**
- `save()`, `findById()`, `findAll()`, `delete()` — all free
- Basic queries from method names — auto-generated
- Pagination and sorting — built in

---

## Entity Mapping — Full EMS Model

### `Department.java`

```java
@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String location;

    @OneToMany(mappedBy = "department",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @JsonIgnore  // prevent infinite recursion in JSON
    private List<Employee> employees = new ArrayList<>();
}
```

### `Role.java`

```java
@Entity
@Table(name = "roles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private RoleType name;

    public enum RoleType {
        EMPLOYEE, MANAGER, HR, ADMIN
    }
}
```

### `Employee.java` — Full mapping

```java
@Entity
@Table(name = "employees",
       indexes = {
           @Index(name = "idx_email", columnList = "email"),
           @Index(name = "idx_dept", columnList = "department_id")
       })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // Many employees belong to one department
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Many-to-many with roles
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "employee_roles",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Derived field — not stored in DB
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public enum EmployeeStatus { ACTIVE, ON_LEAVE, TERMINATED }
}
```

### `Project.java`

```java
@Entity
@Table(name = "projects")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_employees",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> assignedEmployees = new HashSet<>();
}
```

---

## Repository Pattern

```java
// JpaRepository<Entity, PrimaryKeyType>
// Gives you: save, findById, findAll, delete, count, exists, flush, etc.
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // custom methods go here
}

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
```

### Built-in Methods from `JpaRepository`

```java
// Save (insert or update)
Employee saved = employeeRepository.save(employee);
List<Employee> savedAll = employeeRepository.saveAll(list);

// Find
Optional<Employee> opt = employeeRepository.findById(1L);
List<Employee> all = employeeRepository.findAll();
boolean exists = employeeRepository.existsById(1L);
long count = employeeRepository.count();

// Delete
employeeRepository.deleteById(1L);
employeeRepository.delete(employee);
employeeRepository.deleteAll();

// Flush — force pending changes to DB immediately
employeeRepository.flush();
```

---

## Derived Query Methods

Spring Data JPA derives SQL from method names — no SQL needed.

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ── Find by field ─────────────────────────────────────────────
    Optional<Employee> findByEmail(String email);
    List<Employee> findByStatus(Employee.EmployeeStatus status);
    List<Employee> findByDepartmentName(String deptName);  // traverses relationship

    // ── Comparison ────────────────────────────────────────────────
    List<Employee> findBySalaryGreaterThan(BigDecimal amount);
    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);
    List<Employee> findByJoinDateAfter(LocalDate date);

    // ── String matching ───────────────────────────────────────────
    List<Employee> findByFirstNameContainingIgnoreCase(String name);
    List<Employee> findByEmailEndingWith(String domain);
    List<Employee> findByLastNameStartingWith(String prefix);

    // ── Multiple conditions ───────────────────────────────────────
    List<Employee> findByDepartmentNameAndStatus(
        String deptName, Employee.EmployeeStatus status);

    List<Employee> findByDepartmentNameOrStatus(
        String deptName, Employee.EmployeeStatus status);

    // ── Ordering ──────────────────────────────────────────────────
    List<Employee> findByDepartmentNameOrderBySalaryDesc(String deptName);
    List<Employee> findAllByOrderByLastNameAsc();

    // ── Count / Exists ────────────────────────────────────────────
    long countByDepartmentId(Long deptId);
    boolean existsByEmail(String email);

    // ── Delete ────────────────────────────────────────────────────
    void deleteByStatus(Employee.EmployeeStatus status);
}
```

### Keyword Reference

| Keyword | Example | Equivalent SQL |
|---|---|---|
| `findBy` | `findByName` | `WHERE name = ?` |
| `And` | `findByNameAndEmail` | `WHERE name=? AND email=?` |
| `Or` | `findByNameOrEmail` | `WHERE name=? OR email=?` |
| `Between` | `findBySalaryBetween` | `WHERE salary BETWEEN ? AND ?` |
| `LessThan` | `findBySalaryLessThan` | `WHERE salary < ?` |
| `GreaterThan` | `findBySalaryGreaterThan` | `WHERE salary > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE %?%` |
| `StartingWith` | `findByNameStartingWith` | `WHERE name LIKE ?%` |
| `OrderBy` | `findAllOrderByNameAsc` | `ORDER BY name ASC` |
| `IsNull` | `findByManagerIsNull` | `WHERE manager IS NULL` |
| `In` | `findByDeptIn` | `WHERE dept IN (...)` |

---

## JPQL and Native Queries

For complex queries beyond what method names can express.

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // JPQL — uses entity/field names, not table/column names
    @Query("SELECT e FROM Employee e WHERE e.salary > :minSalary " +
           "AND e.status = 'ACTIVE' ORDER BY e.salary DESC")
    List<Employee> findHighEarners(@Param("minSalary") BigDecimal minSalary);

    // JPQL with JOIN
    @Query("SELECT e FROM Employee e JOIN e.department d " +
           "WHERE d.location = :location AND e.status = :status")
    List<Employee> findByLocationAndStatus(
        @Param("location") String location,
        @Param("status") Employee.EmployeeStatus status);

    // JPQL — aggregate
    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.department.id = :deptId")
    BigDecimal findAvgSalaryByDept(@Param("deptId") Long deptId);

    // JPQL — constructor expression (DTO projection)
    @Query("SELECT new com.ems.dto.EmployeeSummaryDto(e.id, e.firstName, " +
           "e.lastName, e.department.name, e.salary) FROM Employee e")
    List<EmployeeSummaryDto> findAllSummaries();

    // Native SQL — use actual table/column names
    @Query(value = "SELECT * FROM employees WHERE YEAR(join_date) = :year",
           nativeQuery = true)
    List<Employee> findHiredInYear(@Param("year") int year);

    // Native SQL — complex reporting query
    @Query(value = """
        SELECT d.name as department, COUNT(e.id) as headcount,
               AVG(e.salary) as avg_salary
        FROM employees e
        JOIN departments d ON e.department_id = d.id
        GROUP BY d.name
        ORDER BY headcount DESC
        """, nativeQuery = true)
    List<Object[]> getDepartmentReport();

    // Modifying query (UPDATE/DELETE)
    @Modifying
    @Transactional
    @Query("UPDATE Employee e SET e.salary = e.salary * :factor " +
           "WHERE e.department.id = :deptId")
    int applyRaiseToAllInDept(@Param("deptId") Long deptId,
                               @Param("factor") double factor);
}
```

### DTO Projection

```java
// DTO for query results — no @Entity needed
@Value  // Lombok immutable value object
public class EmployeeSummaryDto {
    Long id;
    String firstName;
    String lastName;
    String departmentName;
    BigDecimal salary;
}
```

---

## Entity Relationships

### One-to-Many / Many-to-One — Employee ↔ Department

```java
// Department side (One) — one dept has many employees
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Employee> employees = new ArrayList<>();

// Employee side (Many) — many employees belong to one dept
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id")
private Department department;
```

### Many-to-Many — Employee ↔ Project

```java
// Project side (owner of the relationship)
@ManyToMany
@JoinTable(
    name = "project_employees",
    joinColumns = @JoinColumn(name = "project_id"),
    inverseJoinColumns = @JoinColumn(name = "employee_id")
)
private Set<Employee> assignedEmployees = new HashSet<>();

// Employee side (non-owner — mappedBy)
@ManyToMany(mappedBy = "assignedEmployees")
private Set<Project> projects = new HashSet<>();
```

### Fetch Types

| Type | Behaviour | Default for |
|---|---|---|
| `LAZY` | Load related data only when accessed | `@OneToMany`, `@ManyToMany` |
| `EAGER` | Always load related data in same query | `@ManyToOne`, `@OneToOne` |

```java
// Best practice: always LAZY, load explicitly when needed
@ManyToOne(fetch = FetchType.LAZY)
private Department department;

// Load with JOIN FETCH in query when you need it
@Query("SELECT e FROM Employee e JOIN FETCH e.department WHERE e.id = :id")
Optional<Employee> findByIdWithDept(@Param("id") Long id);
```

> 💡 **N+1 Problem:** If you load 100 employees and access `emp.getDepartment()` for each, Hibernate fires 100 extra queries. Fix: use `JOIN FETCH` in your query to load everything in one query.

### Cascade Types

```java
@OneToMany(cascade = CascadeType.ALL)     // propagate all operations
@OneToMany(cascade = CascadeType.PERSIST) // save child when parent saved
@OneToMany(cascade = CascadeType.REMOVE)  // delete children when parent deleted
@OneToMany(orphanRemoval = true)          // delete child if removed from parent's list
```

---

## Pagination and Sorting

```java
// Controller
@GetMapping
public Page<Employee> getEmployees(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "lastName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {

    Sort sort = direction.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);
    return employeeRepository.findAll(pageable);
}
```

### Response — `Page<T>` object

```json
GET /api/employees?page=0&size=5&sortBy=salary&direction=desc

{
  "content": [ { "id": 1, "firstName": "Alice", "salary": 90000 }, ... ],
  "pageable": { "pageNumber": 0, "pageSize": 5 },
  "totalElements": 47,
  "totalPages": 10,
  "last": false,
  "first": true
}
```

### Paginated Custom Query

```java
@Query("SELECT e FROM Employee e WHERE e.department.name = :dept")
Page<Employee> findByDepartment(@Param("dept") String dept, Pageable pageable);
```

---

## Database Caching

Reduce database load by caching frequently read data.

### Enable Caching

```java
@SpringBootApplication
@EnableCaching
public class EmsApiApplication { ... }
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<!-- Optional: use Redis for distributed cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Cache Annotations

```java
@Service
public class EmployeeService {

    // @Cacheable: on first call, execute method and cache result
    // On subsequent calls with same id, return cached result
    @Cacheable(value = "employees", key = "#id")
    public Employee findById(Long id) {
        System.out.println("Fetching from DB: " + id); // only prints on first call
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Not found: " + id));
    }

    // @Cacheable with condition
    @Cacheable(value = "employees", key = "#id", condition = "#id > 0")
    public Employee findByIdCached(Long id) { ... }

    // @CachePut: always execute method AND update cache
    @CachePut(value = "employees", key = "#result.id")
    public Employee save(Employee emp) {
        return employeeRepository.save(emp);
    }

    // @CacheEvict: remove from cache when data changes
    @CacheEvict(value = "employees", key = "#id")
    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    // @CacheEvict — clear entire cache
    @CacheEvict(value = "employees", allEntries = true)
    public void clearAllCache() { }

    // @Caching — multiple cache operations on one method
    @Caching(evict = {
        @CacheEvict(value = "employees", key = "#emp.id"),
        @CacheEvict(value = "departments", allEntries = true)
    })
    public Employee update(Employee emp) {
        return employeeRepository.save(emp);
    }

    // Cache list of all departments (they change rarely)
    @Cacheable(value = "departments")
    public List<Department> findAllDepartments() {
        return departmentRepository.findAll();
    }
}
```

### Configure Cache TTL with Redis

```properties
# application.properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=600000  # 10 minutes in ms
```

---

## Validations

Validate input at the API layer using Bean Validation (Jakarta Validation API).

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Annotate Entity / DTO

```java
@Data
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "10000.00", message = "Salary must be at least 10,000")
    @DecimalMax(value = "9999999.99", message = "Salary too large")
    private BigDecimal salary;

    @NotNull(message = "Join date is required")
    @PastOrPresent(message = "Join date cannot be in the future")
    private LocalDate joinDate;

    @NotNull(message = "Department is required")
    @Positive(message = "Department ID must be positive")
    private Long departmentId;
}
```

### Controller — Trigger Validation

```java
@PostMapping
public ResponseEntity<Employee> create(
        @Valid @RequestBody EmployeeRequest request) {
    // @Valid triggers validation; BindingResult catches errors
    Employee emp = employeeService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(emp);
}

@PutMapping("/{id}")
public ResponseEntity<Employee> update(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeRequest request) {
    return ResponseEntity.ok(employeeService.update(id, request));
}
```

### Global Exception Handler — catch validation errors

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle @Valid failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            fieldErrors.put(err.getField(), err.getDefaultMessage())
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Validation Failed");
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    // Handle ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 404);
        response.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

### Validation Error Response — sample

```json
POST /api/employees
{
  "firstName": "",
  "email": "not-an-email",
  "salary": -5000
}

Response 400:
{
  "timestamp": "2026-03-26T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "firstName": "First name is required",
    "email": "Invalid email format",
    "salary": "Salary must be at least 10,000",
    "joinDate": "Join date is required"
  }
}
```

---

## Summary

| Feature | Annotation/Class | Purpose |
|---|---|---|
| Entity | `@Entity`, `@Table` | Map class to DB table |
| Primary Key | `@Id`, `@GeneratedValue` | Auto-generated PK |
| Column | `@Column` | Map field to column |
| One-to-Many | `@OneToMany`, `@ManyToOne` | Parent-child relationship |
| Many-to-Many | `@ManyToMany`, `@JoinTable` | Peer relationship |
| Repository | `JpaRepository<T, ID>` | Free CRUD operations |
| Derived Query | `findByNameAndStatus()` | SQL from method name |
| Custom Query | `@Query` | JPQL or native SQL |
| Bulk update | `@Modifying` + `@Query` | UPDATE/DELETE |
| Pagination | `PageRequest`, `Page<T>` | Paginated results |
| Caching | `@Cacheable`, `@CacheEvict` | Reduce DB load |
| Validation | `@Valid`, `@NotNull`, `@Email` | Input validation |
| Error handling | `@RestControllerAdvice` | Centralised exceptions |

---

*Previous: [04 — Spring Boot Basics](04_Spring_Boot_Basics.md) | Next: [06 — Spring REST API](06_Spring_REST_API.md)*

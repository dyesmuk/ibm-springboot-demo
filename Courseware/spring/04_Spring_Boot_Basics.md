# 04 — Spring Boot Basics

> 🏢 **EMS Theme:** Rebuilding the EMS backend using Spring Boot — same features, dramatically less configuration.

---

## Table of Contents
- [Introduction — Why Spring Boot?](#introduction--why-spring-boot)
- [Software Setup](#software-setup)
- [Spring Boot Basics and Internals](#spring-boot-basics-and-internals)
- [Your First Spring Boot Project — EMS](#your-first-spring-boot-project--ems)
- [Profiles](#profiles)
- [Logging](#logging)
- [Health Checks and Metrics — Actuator](#health-checks-and-metrics--actuator)

---

## Introduction — Why Spring Boot?

Traditional Spring requires significant configuration:
- XML or Java config for DispatcherServlet
- Manual DataSource, TransactionManager, JdbcTemplate beans
- WAR packaging + external Tomcat
- Separate configs for each environment

Spring Boot eliminates almost all of this:

| Spring MVC (Traditional) | Spring Boot |
|---|---|
| Configure DispatcherServlet manually | Auto-configured |
| Configure ViewResolver | Auto-configured |
| Set up DataSource manually | Just add `spring.datasource.url` in properties |
| Deploy WAR to Tomcat | Embedded Tomcat — run as JAR |
| 50+ lines of config to start | One `@SpringBootApplication` annotation |

> 🔎 **Analogy:** Traditional Spring is like building your own PC from components. Spring Boot is buying a laptop — same power, everything pre-assembled and ready to use. You can still open it up and customise if needed.

### Spring Boot Key Features
- **Auto-configuration** — detects libraries on classpath and configures them
- **Embedded server** — Tomcat/Jetty inside the JAR, no deployment needed
- **Opinionated defaults** — sensible defaults you can override
- **Spring Initializr** — generate a project in seconds
- **Actuator** — production-ready monitoring out of the box

---

## Software Setup

### Option 1 — Spring Initializr (Recommended)
1. Go to https://start.spring.io
2. Choose:
   - Project: **Maven**
   - Language: **Java**
   - Spring Boot: **3.2.x**
   - Group: `com.ems`
   - Artifact: `ems-api`
   - Packaging: **Jar**
   - Java: **17**
3. Add Dependencies:
   - Spring Web
   - Spring Data JPA
   - MySQL Driver
   - Lombok
   - Spring Boot DevTools
4. Click **Generate** → extract zip → open in Eclipse (File → Import → Existing Maven Project)

### Option 2 — Eclipse Spring Tools
**File → New → Spring Starter Project** (requires STS plugin)

### Project Structure
```
ems-api/
├── src/
│   ├── main/
│   │   ├── java/com/ems/
│   │   │   ├── EmsApiApplication.java    ← Entry point
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── model/
│   │   └── resources/
│   │       ├── application.properties    ← All configuration
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
│       └── java/com/ems/
├── pom.xml
└── mvnw                                  ← Maven wrapper (no Maven install needed)
```

---

## Spring Boot Basics and Internals

### `@SpringBootApplication`

```java
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class EmsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmsApiApplication.class, args);
    }
}
```

### Auto-Configuration — How It Works

When Spring Boot starts:
1. It scans the classpath for known libraries (Jackson, Hibernate, MySQL Driver, etc.)
2. For each library found, it applies a default configuration
3. Your configuration in `application.properties` overrides these defaults
4. If you define your own `@Bean`, Boot backs off — your bean takes priority

```
spring-boot-autoconfigure.jar
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        ← Lists 100+ auto-configuration classes
        e.g. DataSourceAutoConfiguration
             HibernateJpaAutoConfiguration
             DispatcherServletAutoConfiguration
             JacksonAutoConfiguration
```

To see what was auto-configured:
```properties
# application.properties
logging.level.org.springframework.boot.autoconfigure=DEBUG
```

### `application.properties` — Central Configuration

```properties
# ── Server ────────────────────────────────────────────
server.port=8080
server.servlet.context-path=/ems

# ── Database ──────────────────────────────────────────
spring.datasource.url=jdbc:mysql://localhost:3306/emsdb
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ── JPA / Hibernate ───────────────────────────────────
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# ── Logging ───────────────────────────────────────────
logging.level.com.ems=DEBUG
logging.level.org.springframework.web=INFO

# ── Application custom properties ─────────────────────
ems.app.name=Employee Management System
ems.app.max-employees=500
```

---

## Your First Spring Boot Project — EMS

### `pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

> 💡 **Note:** No version numbers needed for Spring Boot starters — `spring-boot-starter-parent` manages versions for you.

### Entity — `Employee.java`

```java
@Entity
@Table(name = "employees")
@Data                    // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder                 // Lombok: Employee.builder().name("Alice").build()
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private Double salary;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

### Repository — `EmployeeRepository.java`

```java
// Spring Data JPA — NO implementation needed, Spring generates it!
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Spring derives SQL from method name automatically
    List<Employee> findByDepartmentName(String deptName);
    List<Employee> findBySalaryGreaterThan(double salary);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByNameContainingIgnoreCase(String name);

    @Query("SELECT e FROM Employee e WHERE e.salary BETWEEN :min AND :max")
    List<Employee> findBySalaryRange(@Param("min") double min,
                                     @Param("max") double max);
}
```

### Service — `EmployeeService.java`

```java
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee updated) {
        Employee existing = findById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setSalary(updated.getSalary());
        existing.setDepartment(updated.getDepartment());
        return employeeRepository.save(existing);
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    public List<Employee> searchByName(String name) {
        return employeeRepository.findByNameContainingIgnoreCase(name);
    }
}
```

### Custom Exception

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### REST Controller — `EmployeeController.java`

```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<Employee> getAll() {
        return employeeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody Employee employee) {
        Employee saved = employeeService.save(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable Long id,
                                           @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Employee> search(@RequestParam String name) {
        return employeeService.searchByName(name);
    }
}
```

### Run the Application

```bash
# From terminal
mvn spring-boot:run

# Or
java -jar target/ems-api-1.0-SNAPSHOT.jar
```

Test with curl:
```bash
curl http://localhost:8080/api/employees
curl -X POST http://localhost:8080/api/employees \
     -H "Content-Type: application/json" \
     -d '{"name":"Alice","email":"alice@ems.com","salary":75000}'
```

---

## Profiles

Profiles allow different configurations for different environments — dev uses H2, prod uses MySQL.

### Define Profile-specific Properties

**`application.properties`** (common):
```properties
ems.app.name=Employee Management System
spring.profiles.active=dev
```

**`application-dev.properties`**:
```properties
# Dev — H2 in-memory DB, verbose logging
spring.datasource.url=jdbc:h2:mem:emsdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
logging.level.com.ems=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

**`application-prod.properties`**:
```properties
# Prod — real MySQL, minimal logging
spring.datasource.url=jdbc:mysql://prod-server:3306/emsdb
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.ems=WARN
logging.level.root=ERROR
```

**`application-test.properties`**:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

### Activating a Profile

```bash
# Command line argument
java -jar ems-api.jar --spring.profiles.active=prod

# Environment variable
SPRING_PROFILES_ACTIVE=prod java -jar ems-api.jar

# In application.properties
spring.profiles.active=dev
```

### Profile-specific Beans

```java
@Configuration
public class NotificationConfig {

    @Bean
    @Profile("dev")
    public NotificationService mockNotification() {
        return (email, msg) -> System.out.println("[MOCK] Email to " + email + ": " + msg);
    }

    @Bean
    @Profile("prod")
    public NotificationService realEmailNotification() {
        return new SmtpEmailNotificationService(); // real implementation
    }
}
```

---

## Logging

Spring Boot uses **SLF4J** as the logging facade and **Logback** as the default implementation — pre-configured, zero setup.

### Basic Usage

```java
@Service
public class EmployeeService {

    // Use SLF4J logger (or @Slf4j from Lombok)
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    // With Lombok @Slf4j annotation — log is auto-created
    public Employee findById(Long id) {
        log.debug("Finding employee with id: {}", id);

        Employee emp = employeeRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Employee not found — id: {}", id);
                return new ResourceNotFoundException("Employee not found: " + id);
            });

        log.info("Found employee: {} in dept: {}", emp.getName(),
                 emp.getDepartment() != null ? emp.getDepartment().getName() : "N/A");
        return emp;
    }

    public Employee save(Employee emp) {
        log.info("Saving employee: {}", emp.getEmail());
        try {
            Employee saved = employeeRepository.save(emp);
            log.debug("Employee saved with id: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save employee: {} — error: {}", emp.getEmail(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### Log Levels (in order of severity)

```
TRACE → DEBUG → INFO → WARN → ERROR
```
Setting a level shows that level and everything above it.
`logging.level.com.ems=DEBUG` → shows DEBUG, INFO, WARN, ERROR.

### Logging Configuration in `application.properties`

```properties
# Log levels per package
logging.level.root=WARN
logging.level.com.ems=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql=TRACE

# Log file output
logging.file.name=logs/ems-app.log
logging.file.max-size=10MB
logging.file.max-history=30

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Structured Logging with `logback-spring.xml`

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss} [%thread] %highlight(%-5level) %cyan(%logger{20}) - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/ems.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/ems.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO"><appender-ref ref="FILE"/></root>
    </springProfile>
</configuration>
```

---

## Health Checks and Metrics — Actuator

Spring Boot Actuator adds production-ready monitoring endpoints — zero code required.

### Add Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Configure Actuator

```properties
# Expose specific or all endpoints
management.endpoints.web.exposure.include=health,info,metrics,env,beans,mappings
management.endpoint.health.show-details=always

# Custom info
info.app.name=EMS API
info.app.version=1.0.0
info.app.description=Employee Management System REST API

# Change actuator base path
management.endpoints.web.base-path=/actuator
```

### Key Endpoints

| Endpoint | URL | What it shows |
|---|---|---|
| Health | `/actuator/health` | App status, DB connectivity |
| Info | `/actuator/info` | App name, version, custom info |
| Metrics | `/actuator/metrics` | JVM memory, CPU, request counts |
| Beans | `/actuator/beans` | All Spring beans in context |
| Mappings | `/actuator/mappings` | All URL → controller mappings |
| Env | `/actuator/env` | All configuration properties |
| Loggers | `/actuator/loggers` | Log levels (can change at runtime!) |

### Sample Health Response

```json
GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 250790436864,
        "free": 180423245824,
        "threshold": 10485760
      }
    }
  }
}
```

### Custom Health Indicator

```java
@Component
public class EmsHealthIndicator implements HealthIndicator {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Health health() {
        try {
            long count = employeeRepository.count();
            return Health.up()
                .withDetail("employeeCount", count)
                .withDetail("status", "EMS database accessible")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Custom Metrics

```java
@Service
public class EmployeeService {

    private final Counter hireCounter;
    private final Counter terminationCounter;

    public EmployeeService(MeterRegistry meterRegistry) {
        this.hireCounter = Counter.builder("ems.employees.hired")
            .description("Total employees hired")
            .register(meterRegistry);
        this.terminationCounter = Counter.builder("ems.employees.terminated")
            .description("Total employees terminated")
            .register(meterRegistry);
    }

    public Employee hire(Employee emp) {
        Employee saved = employeeRepository.save(emp);
        hireCounter.increment();
        return saved;
    }

    public void terminate(Long id) {
        employeeRepository.deleteById(id);
        terminationCounter.increment();
    }
}
```

Access at: `GET /actuator/metrics/ems.employees.hired`

---

## Summary

| Feature | How | Key Property/Annotation |
|---|---|---|
| Auto-configuration | Classpath detection | `@SpringBootApplication` |
| Run the app | Embedded Tomcat | `SpringApplication.run()` |
| Configuration | Properties files | `application.properties` |
| Dev/Prod environments | Profiles | `spring.profiles.active` |
| Logging | SLF4J + Logback | `logging.level.com.ems=DEBUG` |
| Health monitoring | Actuator | `spring-boot-starter-actuator` |
| Custom health | Implement interface | `HealthIndicator` |
| Custom metrics | Micrometer | `MeterRegistry`, `Counter` |
| DevTools | Hot reload | `spring-boot-devtools` |

---

*Previous: [03 — Spring MVC](03_Spring_MVC.md) | Next: [05 — Spring Data JPA](05_Spring_Data_JPA.md)*

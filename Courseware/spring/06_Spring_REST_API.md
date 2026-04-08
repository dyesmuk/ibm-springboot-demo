# 06 — Spring REST API

> 🏢 **EMS Theme:** Building a complete, production-grade REST API for the Employee Management System — consumed by Angular/React frontends and tested via Postman.

---

## Table of Contents
- [REST Concepts Recap](#rest-concepts-recap)
- [Creating a REST CRUD API](#creating-a-rest-crud-api)
- [Creating a REST Client](#creating-a-rest-client)
- [Micro Services & REST Concepts](#micro-services--rest-concepts)
- [Spring Data REST In Action](#spring-data-rest-in-action)
- [Test REST APIs Using Postman](#test-rest-apis-using-postman)
- [Paging and Sorting](#paging-and-sorting)
- [Customising JSON Serialisation](#customising-json-serialisation)
- [Implementing Custom Finder Methods](#implementing-custom-finder-methods)
- [Create Custom Controller Methods](#create-custom-controller-methods)
- [Swagger REST Documentation](#swagger-rest-documentation)

---

## REST Concepts Recap

**REST** (Representational State Transfer) is an architectural style for building web APIs over HTTP.

### REST Constraints
| Constraint | Meaning |
|---|---|
| **Stateless** | Each request contains all needed info — no session on server |
| **Client-Server** | UI and backend are separate, communicate only via API |
| **Uniform Interface** | Standard HTTP verbs, resource-based URLs |
| **Resource-based** | Everything is a resource accessed via a URL |

### HTTP Methods → CRUD

| HTTP Method | CRUD | URL Example | Description |
|---|---|---|---|
| `GET` | Read | `/api/employees` | Get all |
| `GET` | Read | `/api/employees/42` | Get one by ID |
| `POST` | Create | `/api/employees` | Create new |
| `PUT` | Update (full) | `/api/employees/42` | Replace entire resource |
| `PATCH` | Update (partial) | `/api/employees/42` | Update some fields |
| `DELETE` | Delete | `/api/employees/42` | Delete |

### HTTP Status Codes

| Code | Meaning | When to use |
|---|---|---|
| `200 OK` | Success | GET, PUT returns data |
| `201 Created` | Resource created | POST success |
| `204 No Content` | Success, no body | DELETE success |
| `400 Bad Request` | Invalid input | Validation failure |
| `401 Unauthorized` | Not authenticated | Missing/invalid token |
| `403 Forbidden` | Not authorized | Valid token, no permission |
| `404 Not Found` | Resource missing | Wrong ID |
| `409 Conflict` | Duplicate resource | Email already exists |
| `500 Internal Server Error` | Server crash | Unhandled exception |

---

## Creating a REST CRUD API

### Full EMS REST API

```java
@RestController
@RequestMapping("/api/v1/employees")
@CrossOrigin(origins = "*")  // allow all origins (restrict in production)
public class EmployeeRestController {

    private final EmployeeService employeeService;

    public EmployeeRestController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── GET all ─────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        List<EmployeeResponse> employees = employeeService.findAll()
            .stream()
            .map(EmployeeMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    // ── GET by ID ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        Employee emp = employeeService.findById(id);
        return ResponseEntity.ok(EmployeeMapper.toResponse(emp));
    }

    // ── POST — create ───────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody EmployeeRequest request) {
        Employee saved = employeeService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.getId())
            .toUri();
        return ResponseEntity.created(location)
                             .body(EmployeeMapper.toResponse(saved));
    }

    // ── PUT — full update ────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        Employee updated = employeeService.update(id, request);
        return ResponseEntity.ok(EmployeeMapper.toResponse(updated));
    }

    // ── PATCH — partial update (salary only) ────────────────────────
    @PatchMapping("/{id}/salary")
    public ResponseEntity<EmployeeResponse> updateSalary(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal newSalary = body.get("salary");
        Employee updated = employeeService.updateSalary(id, newSalary);
        return ResponseEntity.ok(EmployeeMapper.toResponse(updated));
    }

    // ── DELETE ───────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET by department ────────────────────────────────────────────
    @GetMapping("/department/{deptName}")
    public ResponseEntity<List<EmployeeResponse>> getByDepartment(
            @PathVariable String deptName) {
        return ResponseEntity.ok(
            employeeService.findByDepartment(deptName)
                           .stream()
                           .map(EmployeeMapper::toResponse)
                           .collect(Collectors.toList())
        );
    }
}
```

### Request / Response DTOs

```java
// Request — what client sends (with validation)
@Data
public class EmployeeRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email    private String email;
    @NotNull  @Positive private BigDecimal salary;
    @NotNull  private Long departmentId;
}

// Response — what server returns (no sensitive data)
@Data
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private String departmentName;
    private String status;
    private LocalDate joinDate;
}

// Mapper
public class EmployeeMapper {
    public static EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
            e.getId(), e.getFirstName(), e.getLastName(),
            e.getEmail(), e.getSalary(),
            e.getDepartment() != null ? e.getDepartment().getName() : null,
            e.getStatus().name(), e.getJoinDate()
        );
    }
}
```

---

## Creating a REST Client

Consume other REST APIs from within your Spring Boot app using `RestTemplate` or `WebClient`.

### RestTemplate (Traditional — Synchronous)

```java
@Service
public class HrIntegrationService {

    private final RestTemplate restTemplate;

    public HrIntegrationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    // GET — fetch single object
    public EmployeeResponse getEmployeeFromHrSystem(Long id) {
        String url = "https://hr-system.company.com/api/employees/{id}";
        return restTemplate.getForObject(url, EmployeeResponse.class, id);
    }

    // GET — fetch list
    public List<EmployeeResponse> getAllFromHrSystem() {
        String url = "https://hr-system.company.com/api/employees";
        ResponseEntity<List<EmployeeResponse>> response =
            restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EmployeeResponse>>() {});
        return response.getBody();
    }

    // POST — send new employee to payroll system
    public PayrollRecord syncToPayroll(Employee emp) {
        String url = "https://payroll.company.com/api/employees";
        EmployeeRequest req = new EmployeeRequest();
        req.setEmail(emp.getEmail());
        req.setSalary(emp.getSalary());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("your-api-token");

        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<PayrollRecord> response =
            restTemplate.postForEntity(url, entity, PayrollRecord.class);
        return response.getBody();
    }

    // Handle errors
    public Employee fetchWithErrorHandling(Long id) {
        try {
            return restTemplate.getForObject(
                "https://hr-system.company.com/api/employees/{id}",
                Employee.class, id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Employee not found in HR system: " + id);
        } catch (HttpServerErrorException e) {
            throw new RuntimeException("HR system error: " + e.getStatusCode());
        }
    }
}
```

### WebClient (Modern — Reactive/Async)

```java
@Service
public class PayrollClient {

    private final WebClient webClient;

    public PayrollClient(WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl("https://payroll.company.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // Sync call (block())
    public PayrollRecord getPayroll(Long empId) {
        return webClient.get()
            .uri("/api/payroll/{id}", empId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                resp -> Mono.error(new ResourceNotFoundException("Not found")))
            .bodyToMono(PayrollRecord.class)
            .block(); // makes it synchronous
    }

    // Async call (non-blocking)
    public Mono<PayrollRecord> getPayrollAsync(Long empId) {
        return webClient.get()
            .uri("/api/payroll/{id}", empId)
            .retrieve()
            .bodyToMono(PayrollRecord.class);
    }
}
```

---

## Micro Services & REST Concepts

### What is a Microservice?

A **microservice** is a small, independently deployable service focused on one business domain.

```
Monolith EMS                         Microservices EMS
┌────────────────────────┐           ┌──────────────────┐
│ EmployeeController     │           │ Employee Service  │ :8081
│ DepartmentController   │    →      ├──────────────────┤
│ ProjectController      │           │ Department Svc    │ :8082
│ PayrollController      │           ├──────────────────┤
│ All in one WAR         │           │ Payroll Service   │ :8083
└────────────────────────┘           └──────────────────┘
```

### Microservice Communication
- **Synchronous:** REST (HTTP), gRPC
- **Asynchronous:** Message queues (RabbitMQ, Kafka) — covered in Spring JMS

### Service Registration (Eureka)
Each service registers itself; others discover it by name:
```properties
# Employee Service
spring.application.name=employee-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

---

## Spring Data REST In Action

Spring Data REST auto-exposes your `JpaRepository` as a REST API — zero controller code.

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

### Auto-exposed Endpoints

Just having a `JpaRepository` exposes these automatically:

```
GET    /employees           → findAll()
GET    /employees/{id}      → findById()
POST   /employees           → save()
PUT    /employees/{id}      → save() (full update)
PATCH  /employees/{id}      → partial update
DELETE /employees/{id}      → deleteById()
GET    /employees/search    → all finder methods
```

### Customise Spring Data REST

```java
@RepositoryRestResource(
    collectionResourceRel = "employees",  // JSON key name
    path = "employees"                    // URL path
)
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Auto-exposed at: GET /employees/search/findByEmail?email=...
    List<Employee> findByEmail(@Param("email") String email);

    // Auto-exposed at: GET /employees/search/findByDept?name=...
    List<Employee> findByDepartmentName(@Param("name") String name);
}
```

```properties
# application.properties
spring.data.rest.base-path=/api
spring.data.rest.default-page-size=10
spring.data.rest.max-page-size=100
```

### HATEOAS Response (Spring Data REST format)

```json
GET /api/employees/1

{
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@ems.com",
  "salary": 75000,
  "_links": {
    "self": { "href": "http://localhost:8080/api/employees/1" },
    "employee": { "href": "http://localhost:8080/api/employees/1" },
    "department": { "href": "http://localhost:8080/api/employees/1/department" }
  }
}
```

---

## Test REST APIs Using Postman

### Setup a Postman Collection for EMS

1. Open Postman → **New Collection** → name it "EMS API"
2. Set **Collection Variable**: `baseUrl = http://localhost:8080/api/v1`

### CRUD Test Requests

**Create Employee (POST)**
```
Method: POST
URL: {{baseUrl}}/employees
Headers: Content-Type: application/json
Body (raw JSON):
{
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@ems.com",
  "salary": 75000,
  "departmentId": 1,
  "joinDate": "2024-01-15"
}
```

**Get All (GET)**
```
Method: GET
URL: {{baseUrl}}/employees
```

**Get by ID (GET)**
```
Method: GET
URL: {{baseUrl}}/employees/1
```

**Update (PUT)**
```
Method: PUT
URL: {{baseUrl}}/employees/1
Body: { "firstName": "Alice", "salary": 80000, ... }
```

**Delete (DELETE)**
```
Method: DELETE
URL: {{baseUrl}}/employees/1
Expected: 204 No Content
```

### Postman Tests (JavaScript)

```javascript
// Add to Tests tab in Postman

// Test status code
pm.test("Status 200", () => pm.response.to.have.status(200));

// Test response body
pm.test("Has employees array", () => {
    const body = pm.response.json();
    pm.expect(body).to.be.an('array');
    pm.expect(body.length).to.be.greaterThan(0);
});

// Save response value for next request
pm.test("Save employee ID", () => {
    const emp = pm.response.json();
    pm.collectionVariables.set("employeeId", emp.id);
});

// Test POST returns 201
pm.test("Created status", () => pm.response.to.have.status(201));

// Test response time
pm.test("Response under 500ms", () =>
    pm.expect(pm.response.responseTime).to.be.below(500));
```

---

## Paging and Sorting

*(Covered in detail in 05_Spring_Data_JPA.md — here we show the REST layer)*

```java
@GetMapping
public ResponseEntity<Page<EmployeeResponse>> getAll(
        @RequestParam(defaultValue = "0")         int page,
        @RequestParam(defaultValue = "10")        int size,
        @RequestParam(defaultValue = "lastName")  String sortBy,
        @RequestParam(defaultValue = "asc")       String direction) {

    Sort sort = direction.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Employee> empPage = employeeRepository.findAll(pageable);
    Page<EmployeeResponse> responsePage = empPage.map(EmployeeMapper::toResponse);
    return ResponseEntity.ok(responsePage);
}
```

```bash
# Postman / curl
GET /api/v1/employees?page=0&size=5&sortBy=salary&direction=desc
GET /api/v1/employees?page=1&size=10
```

---

## Customising JSON Serialisation

Control exactly what your API returns and how it looks.

### Jackson Annotations

```java
@Data
public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;

    // Rename field in JSON output
    @JsonProperty("emailAddress")
    private String email;

    // Format date
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate joinDate;

    // Exclude from JSON
    @JsonIgnore
    private String passwordHash;

    // Include null values (default: omit)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String managerName;

    // Custom serialiser for BigDecimal
    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal salary;
}
```

### Global Jackson Configuration

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .featuresToEnable(
                MapperFeature.DEFAULT_VIEW_INCLUSION)
            .simpleDateFormat("yyyy-MM-dd");
    }
}
```

### JSON Views — Different responses per context

```java
public class EmployeeViews {
    public static class Public {}     // minimal — for unauthenticated
    public static class Manager extends Public {}  // more detail for managers
    public static class Admin extends Manager {}   // full detail for admins
}

@Data
public class EmployeeResponse {
    @JsonView(EmployeeViews.Public.class)
    private Long id;

    @JsonView(EmployeeViews.Public.class)
    private String firstName;

    @JsonView(EmployeeViews.Manager.class)
    private String email;

    @JsonView(EmployeeViews.Admin.class)
    private BigDecimal salary;
}

// Controller — choose view based on role
@GetMapping("/{id}")
@JsonView(EmployeeViews.Manager.class)
public EmployeeResponse getById(@PathVariable Long id) {
    return EmployeeMapper.toResponse(employeeService.findById(id));
}
```

---

## Implementing Custom Finder Methods

Expose custom search capabilities on top of Spring Data REST.

```java
@RepositoryRestResource(path = "employees")
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // All below are auto-exposed under /employees/search/

    @RestResource(path = "byEmail", rel = "byEmail")
    Optional<Employee> findByEmail(@Param("email") String email);

    @RestResource(path = "byDept", rel = "byDepartment")
    List<Employee> findByDepartmentName(@Param("name") String name);

    @RestResource(path = "bySalaryRange")
    @Query("SELECT e FROM Employee e WHERE e.salary BETWEEN :min AND :max")
    List<Employee> findBySalaryRange(@Param("min") BigDecimal min,
                                     @Param("max") BigDecimal max);

    // Hide from Spring Data REST exposure
    @RestResource(exported = false)
    void deleteByEmail(String email);
}
```

```bash
# Auto-exposed URLs
GET /api/employees/search
GET /api/employees/search/byEmail?email=alice@ems.com
GET /api/employees/search/byDept?name=Engineering
GET /api/employees/search/bySalaryRange?min=50000&max=100000
```

---

## Create Custom Controller Methods

When Spring Data REST auto-exposure isn't enough — add custom endpoints.

```java
@RepositoryRestController  // integrates with Spring Data REST context
@RequestMapping("/employees")
public class EmployeeCustomController {

    @Autowired private EmployeeService employeeService;
    @Autowired private EntityLinks entityLinks;

    // Bulk salary raise for an entire department
    @PostMapping("/raise")
    public ResponseEntity<?> applyRaise(
            @RequestParam Long departmentId,
            @RequestParam double percentage) {

        int count = employeeService.applyRaise(departmentId, percentage);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("departmentsId", departmentId);
        response.put("percentage", percentage);
        response.put("employeesUpdated", count);
        response.put("message", "Salary raise applied successfully");

        return ResponseEntity.ok(response);
    }

    // Transfer all employees from one dept to another
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @RequestParam Long fromDeptId,
            @RequestParam Long toDeptId) {

        int count = employeeService.transferDepartment(fromDeptId, toDeptId);
        return ResponseEntity.ok(
            Map.of("transferred", count, "to", toDeptId));
    }

    // Export employee data
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        String csv = employeeService.exportToCsv();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=employees.csv")
            .body(csv);
    }
}
```

---

## Swagger REST Documentation

Auto-generate interactive API documentation from your code.

### Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Configuration

```properties
# application.properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.info.title=EMS REST API
springdoc.info.version=1.0.0
```

### Annotate Your API

```java
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee API", description = "Manage employees in the EMS")
public class EmployeeRestController {

    @Operation(
        summary = "Get all employees",
        description = "Returns a list of all active employees"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employees retrieved"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping
    public List<EmployeeResponse> getAll() { ... }

    @Operation(summary = "Create new employee")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Employee created",
            content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Employee details",
                required = true)
            @Valid @RequestBody EmployeeRequest request) { ... }

    @Operation(summary = "Update employee salary")
    @Parameter(name = "id", description = "Employee ID", required = true)
    @PatchMapping("/{id}/salary")
    public ResponseEntity<EmployeeResponse> updateSalary(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) { ... }
}
```

### Annotate DTO

```java
@Data
@Schema(description = "Employee creation/update request")
public class EmployeeRequest {

    @Schema(description = "First name", example = "Alice", required = true)
    @NotBlank private String firstName;

    @Schema(description = "Work email", example = "alice@ems.com")
    @Email private String email;

    @Schema(description = "Annual salary in INR", example = "75000.00", minimum = "10000")
    @Positive private BigDecimal salary;
}
```

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

---

## Summary

| Feature | Annotation/Class | Purpose |
|---|---|---|
| REST Controller | `@RestController` | JSON API controller |
| Request body | `@RequestBody` | Parse JSON from request |
| Response entity | `ResponseEntity<T>` | Control status + headers |
| Path variable | `@PathVariable` | URL segment extraction |
| Request param | `@RequestParam` | Query string params |
| PATCH update | `@PatchMapping` | Partial resource update |
| REST Client | `RestTemplate`, `WebClient` | Consume external APIs |
| Spring Data REST | `@RepositoryRestResource` | Auto-expose repository as REST |
| JSON control | `@JsonProperty`, `@JsonIgnore` | Serialisation customisation |
| API docs | `@Operation`, `@ApiResponse` | Swagger/OpenAPI annotations |

---

*Previous: [05 — Spring Data JPA](05_Spring_Data_JPA.md) | Next: [07 — Spring Security](07_Spring_Security.md)*

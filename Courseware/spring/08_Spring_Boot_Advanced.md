# 08 — Spring Boot Advanced

> 🏢 **EMS Theme:** Server-side rendered pages with Thymeleaf, batch payroll processing, async email notifications via JMS, and comprehensive API testing with MockMvc.

---

## Table of Contents
- [Thymeleaf](#thymeleaf)
- [Spring Batch](#spring-batch)
- [Messaging and Spring JMS](#messaging-and-spring-jms)
- [Unit Testing using MockMvc](#unit-testing-using-mockmvc)

---

## Thymeleaf

Thymeleaf is a **server-side Java template engine** — an alternative to JSP for rendering HTML pages in Spring Boot. It uses natural templates (valid HTML even without a server).

> 🔎 **Analogy:** Thymeleaf is like a mail-merge template. You design the letter once with placeholders, then Spring fills in the actual employee names and data before sending it to the browser.

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

```properties
# Templates live in src/main/resources/templates/
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false  # disable in dev — auto-reload
```

### Controller — same as Spring MVC

```java
@Controller
@RequestMapping("/ems")
public class EmsWebController {

    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentService departmentService;

    @GetMapping("/employees")
    public String listEmployees(Model model,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page) {

        Page<Employee> empPage = employeeService.search(search,
            PageRequest.of(page, 10, Sort.by("lastName")));

        model.addAttribute("employees", empPage.getContent());
        model.addAttribute("totalPages", empPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);
        model.addAttribute("departments", departmentService.findAll());
        return "employees/list";  // → templates/employees/list.html
    }

    @GetMapping("/employees/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", departmentService.findAll());
        return "employees/form";
    }

    @PostMapping("/employees/save")
    public String save(@Valid @ModelAttribute Employee emp,
                       BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "employees/form";
        }
        employeeService.save(emp);
        ra.addFlashAttribute("successMsg", "Employee saved successfully!");
        return "redirect:/ems/employees";
    }
}
```

### Thymeleaf Templates

**`templates/employees/list.html`**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>EMS — Employee List</title>
    <link th:href="@{/css/style.css}" rel="stylesheet"/>
</head>
<body>

<!-- Flash message -->
<div th:if="${successMsg}" class="alert alert-success">
    <span th:text="${successMsg}"></span>
</div>

<!-- Search form -->
<form th:action="@{/ems/employees}" method="get">
    <input type="text" name="search" th:value="${search}" placeholder="Search employees..."/>
    <button type="submit">Search</button>
</form>

<!-- Employee table -->
<table>
    <thead>
        <tr>
            <th>ID</th><th>Name</th><th>Email</th>
            <th>Department</th><th>Salary</th><th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <!-- th:each = loop -->
        <tr th:each="emp : ${employees}">
            <td th:text="${emp.id}"></td>
            <td th:text="${emp.firstName + ' ' + emp.lastName}"></td>
            <td th:text="${emp.email}"></td>
            <td th:text="${emp.department?.name ?: 'Unassigned'}"></td>

            <!-- Format salary with currency -->
            <td th:text="${#numbers.formatDecimal(emp.salary, 1, 2)}"></td>

            <td>
                <!-- Link with path variable -->
                <a th:href="@{/ems/employees/edit/{id}(id=${emp.id})}">Edit</a>

                <!-- Only admins see Delete button -->
                <form th:action="@{/ems/employees/delete/{id}(id=${emp.id})}"
                      method="post" style="display:inline"
                      sec:authorize="hasRole('ADMIN')">
                    <button type="submit"
                            onclick="return confirm('Delete this employee?')">
                        Delete
                    </button>
                </form>
            </td>
        </tr>
        <!-- Empty state -->
        <tr th:if="${#lists.isEmpty(employees)}">
            <td colspan="6">No employees found.</td>
        </tr>
    </tbody>
</table>

<!-- Pagination -->
<div>
    <a th:if="${currentPage > 0}"
       th:href="@{/ems/employees(page=${currentPage - 1}, search=${search})}">
        &laquo; Previous
    </a>
    <span th:text="${'Page ' + (currentPage + 1) + ' of ' + totalPages}"></span>
    <a th:if="${currentPage < totalPages - 1}"
       th:href="@{/ems/employees(page=${currentPage + 1}, search=${search})}">
        Next &raquo;
    </a>
</div>

<a th:href="@{/ems/employees/new}" sec:authorize="hasAnyRole('HR','ADMIN')">
    + Add Employee
</a>

</body>
</html>
```

**`templates/employees/form.html`**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>

<h2 th:text="${employee.id != null ? 'Edit Employee' : 'New Employee'}"></h2>

<!-- th:object binds to the model attribute; th:field binds individual fields -->
<form th:action="@{/ems/employees/save}" th:object="${employee}" method="post">

    <input type="hidden" th:field="*{id}"/>

    <div>
        <label>First Name:</label>
        <input th:field="*{firstName}" type="text"/>
        <!-- Show validation error -->
        <span th:if="${#fields.hasErrors('firstName')}"
              th:errors="*{firstName}" style="color:red">
        </span>
    </div>

    <div>
        <label>Last Name:</label>
        <input th:field="*{lastName}" type="text"/>
        <span th:errors="*{lastName}" style="color:red"></span>
    </div>

    <div>
        <label>Email:</label>
        <input th:field="*{email}" type="email"/>
        <span th:errors="*{email}" style="color:red"></span>
    </div>

    <div>
        <label>Salary:</label>
        <input th:field="*{salary}" type="number" step="0.01"/>
    </div>

    <div>
        <label>Department:</label>
        <!-- Dropdown bound to departments list -->
        <select th:field="*{department.id}">
            <option value="">-- Select Department --</option>
            <option th:each="dept : ${departments}"
                    th:value="${dept.id}"
                    th:text="${dept.name}">
            </option>
        </select>
    </div>

    <div>
        <label>Join Date:</label>
        <input th:field="*{joinDate}" type="date"/>
    </div>

    <button type="submit">Save</button>
    <a th:href="@{/ems/employees}">Cancel</a>
</form>

</body>
</html>
```

### Thymeleaf Utility Objects

```html
<!-- Dates -->
<td th:text="${#temporals.format(emp.joinDate, 'dd MMM yyyy')}"></td>

<!-- Numbers -->
<td th:text="${#numbers.formatCurrency(emp.salary)}"></td>

<!-- Strings -->
<td th:text="${#strings.toUpperCase(emp.status)}"></td>

<!-- Collections -->
<p th:if="${not #lists.isEmpty(employees)}">Found employees</p>

<!-- Conditional rendering -->
<span th:if="${emp.status == 'ACTIVE'}" class="badge-green">Active</span>
<span th:unless="${emp.status == 'ACTIVE'}" class="badge-red">Inactive</span>

<!-- Switch-case equivalent -->
<span th:switch="${emp.status}">
    <span th:case="'ACTIVE'">🟢 Active</span>
    <span th:case="'ON_LEAVE'">🟡 On Leave</span>
    <span th:case="*">🔴 Inactive</span>
</span>
```

---

## Spring Batch

Spring Batch handles large-scale, scheduled data processing — reading, processing, and writing records in chunks.

> 🔎 **Analogy:** Spring Batch is like a payroll department running end-of-month processing. It reads all employee records (Reader), calculates their net pay (Processor), and writes payslips to the database (Writer) — in batches of 100 at a time.

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

### EMS Use Case — Monthly Payroll Processing

```
employees.csv → Read Employee → Calculate Tax → Write PayrollRecord to DB
```

### Step 1 — Item Reader (read from CSV)

```java
@Bean
public FlatFileItemReader<EmployeeCsvDto> employeeReader() {
    return new FlatFileItemReaderBuilder<EmployeeCsvDto>()
        .name("employeeReader")
        .resource(new ClassPathResource("data/employees.csv"))
        .linesToSkip(1)  // skip header
        .delimited()
        .names("id", "firstName", "lastName", "salary", "departmentId")
        .targetType(EmployeeCsvDto.class)
        .build();
}

// Or read from database
@Bean
public JpaPagingItemReader<Employee> dbReader(EntityManagerFactory emf) {
    return new JpaPagingItemReaderBuilder<Employee>()
        .name("employeeDbReader")
        .entityManagerFactory(emf)
        .queryString("SELECT e FROM Employee e WHERE e.status = 'ACTIVE'")
        .pageSize(100)
        .build();
}
```

### Step 2 — Item Processor (calculate payroll)

```java
@Component
public class PayrollProcessor implements ItemProcessor<Employee, PayrollRecord> {

    private static final double TAX_RATE = 0.20;
    private static final double PF_RATE  = 0.12;

    @Override
    public PayrollRecord process(Employee emp) throws Exception {
        BigDecimal gross = emp.getSalary();
        BigDecimal tax = gross.multiply(BigDecimal.valueOf(TAX_RATE));
        BigDecimal pf  = gross.multiply(BigDecimal.valueOf(PF_RATE));
        BigDecimal net = gross.subtract(tax).subtract(pf);

        return PayrollRecord.builder()
            .employee(emp)
            .grossSalary(gross)
            .taxDeduction(tax)
            .pfDeduction(pf)
            .netSalary(net)
            .processedDate(LocalDate.now())
            .build();
    }
}
```

### Step 3 — Item Writer (save to DB)

```java
@Bean
public JpaItemWriter<PayrollRecord> payrollWriter(EntityManagerFactory emf) {
    JpaItemWriter<PayrollRecord> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    return writer;
}
```

### Step 4 — Job and Step Configuration

```java
@Configuration
@EnableBatchProcessing
public class PayrollBatchConfig {

    @Autowired private JobRepository jobRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PayrollProcessor payrollProcessor;

    @Bean
    public Step payrollStep(JpaPagingItemReader<Employee> reader,
                             JpaItemWriter<PayrollRecord> writer) {
        return new StepBuilder("payrollStep", jobRepository)
            .<Employee, PayrollRecord>chunk(100, transactionManager)
            .reader(reader)
            .processor(payrollProcessor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(10)
            .skip(Exception.class)  // skip bad records, don't fail whole job
            .listener(new StepExecutionListener() {
                @Override
                public void afterStep(StepExecution se) {
                    System.out.printf("Processed %d, skipped %d%n",
                        se.getWriteCount(), se.getSkipCount());
                }
            })
            .build();
    }

    @Bean
    public Job payrollJob(Step payrollStep) {
        return new JobBuilder("monthlyPayrollJob", jobRepository)
            .start(payrollStep)
            .listener(new JobExecutionListener() {
                @Override
                public void afterJob(JobExecution je) {
                    System.out.println("Payroll job status: " + je.getStatus());
                }
            })
            .build();
    }
}
```

### Schedule the Job

```java
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

    private final JobLauncher jobLauncher;
    private final Job payrollJob;

    // Run at midnight on the last day of every month
    @Scheduled(cron = "0 0 0 L * *")
    public void runMonthlyPayroll() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDate("runDate", LocalDate.now())
            .toJobParameters();
        jobLauncher.run(payrollJob, params);
    }
}
```

```properties
spring.batch.job.enabled=false  # don't auto-run on startup
```

---

## Messaging and Spring JMS

Decouple services using asynchronous messaging. When an employee is hired, publish an event — the notification service handles it independently.

> 🔎 **Analogy:** JMS is like a company notice board. HR posts "New Employee Hired" (producer). The IT department and the payroll department check the board at their own pace and act accordingly (consumers) — HR doesn't wait for them.

### Setup — ActiveMQ

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
```

```properties
spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin
spring.jms.pub-sub-domain=false  # false = Queue, true = Topic
```

### Message Model

```java
@Data @NoArgsConstructor @AllArgsConstructor
public class EmployeeEvent implements Serializable {
    private Long employeeId;
    private String employeeName;
    private String email;
    private String eventType;  // HIRED, UPDATED, TERMINATED
    private LocalDateTime timestamp;
}
```

### JMS Configuration

```java
@Configuration
@EnableJms
public class JmsConfig {

    public static final String EMPLOYEE_QUEUE    = "ems.employee.events";
    public static final String NOTIFICATION_QUEUE = "ems.notifications";

    @Bean
    public Queue employeeQueue() {
        return new ActiveMQQueue(EMPLOYEE_QUEUE);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory cf) {
        JmsTemplate template = new JmsTemplate(cf);
        template.setMessageConverter(new MappingJackson2MessageConverter());
        return template;
    }
}
```

### Producer — Publish Event When Employee Is Hired

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JmsTemplate jmsTemplate;

    public Employee hire(EmployeeRequest request) {
        Employee emp = employeeRepository.save(toEntity(request));

        // Publish event to queue — fire and forget
        EmployeeEvent event = new EmployeeEvent(
            emp.getId(), emp.getFullName(), emp.getEmail(),
            "HIRED", LocalDateTime.now()
        );

        jmsTemplate.convertAndSend(JmsConfig.EMPLOYEE_QUEUE, event);
        log.info("Published HIRED event for: {}", emp.getEmail());

        return emp;
    }
}
```

### Consumer — Listen and Send Welcome Email

```java
@Component
@Slf4j
public class EmployeeEventListener {

    @Autowired private EmailService emailService;

    @JmsListener(destination = JmsConfig.EMPLOYEE_QUEUE)
    public void handleEmployeeEvent(EmployeeEvent event) {
        log.info("Received event: {} for {}", event.getEventType(), event.getEmail());

        switch (event.getEventType()) {
            case "HIRED" -> {
                emailService.sendWelcomeEmail(event.getEmail(), event.getEmployeeName());
                log.info("Welcome email sent to: {}", event.getEmail());
            }
            case "TERMINATED" -> {
                emailService.sendExitEmail(event.getEmail(), event.getEmployeeName());
                log.info("Exit email sent to: {}", event.getEmail());
            }
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }
}
```

### Topic — Broadcast to Multiple Consumers

```properties
spring.jms.pub-sub-domain=true  # enable topics
```

```java
// Publisher
jmsTemplate.convertAndSend("ems.announcements", announcementEvent);

// Multiple consumers — each gets a copy
@JmsListener(destination = "ems.announcements")
public void notifyHR(AnnouncementEvent event) { ... }

@JmsListener(destination = "ems.announcements")
public void notifyAllEmployees(AnnouncementEvent event) { ... }
```

---

## Unit Testing using MockMvc

Test your REST controllers without starting a real server.

### Setup

```java
@WebMvcTest(EmployeeRestController.class)  // loads only MVC layer, not full context
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // mock the service — don't use real DB
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    // Test data
    private Employee testEmployee;
    private EmployeeRequest testRequest;

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
            .id(1L).firstName("Alice").lastName("Smith")
            .email("alice@ems.com").salary(new BigDecimal("75000"))
            .build();

        testRequest = new EmployeeRequest();
        testRequest.setFirstName("Alice");
        testRequest.setLastName("Smith");
        testRequest.setEmail("alice@ems.com");
        testRequest.setSalary(new BigDecimal("75000"));
        testRequest.setDepartmentId(1L);
    }
```

### Test GET All

```java
    @Test
    @DisplayName("GET /api/v1/employees returns list of employees")
    void getAll_ReturnsEmployeeList() throws Exception {
        when(employeeService.findAll())
            .thenReturn(List.of(testEmployee));

        mockMvc.perform(get("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].firstName", is("Alice")))
            .andExpect(jsonPath("$[0].email", is("alice@ems.com")));
    }
```

### Test GET by ID

```java
    @Test
    @DisplayName("GET /api/v1/employees/1 returns employee")
    void getById_WhenExists_ReturnsEmployee() throws Exception {
        when(employeeService.findById(1L)).thenReturn(testEmployee);

        mockMvc.perform(get("/api/v1/employees/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.firstName", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/v1/employees/999 returns 404")
    void getById_WhenNotExists_Returns404() throws Exception {
        when(employeeService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Employee not found: 999"));

        mockMvc.perform(get("/api/v1/employees/{id}", 999L))
            .andExpect(status().isNotFound());
    }
```

### Test POST (Create)

```java
    @Test
    @DisplayName("POST /api/v1/employees creates employee and returns 201")
    void create_WithValidData_Returns201() throws Exception {
        when(employeeService.create(any(EmployeeRequest.class)))
            .thenReturn(testEmployee);

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email", is("alice@ems.com")))
            .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("POST with invalid data returns 400 with field errors")
    void create_WithInvalidData_Returns400() throws Exception {
        EmployeeRequest invalid = new EmployeeRequest();
        // all fields empty — should fail validation

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.firstName",
                is("First name is required")));
    }
```

### Test DELETE

```java
    @Test
    @DisplayName("DELETE /api/v1/employees/1 returns 204")
    void delete_WhenExists_Returns204() throws Exception {
        doNothing().when(employeeService).delete(1L);

        mockMvc.perform(delete("/api/v1/employees/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(employeeService, times(1)).delete(1L);
    }
```

### Full Integration Test (with `@SpringBootTest`)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clearDb() { employeeRepository.deleteAll(); }

    @Test
    void createAndRetrieveEmployee() throws Exception {
        // Create
        String response = mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Bob","lastName":"Jones",
                     "email":"bob@ems.com","salary":65000,"departmentId":1}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long id = JsonPath.parse(response).read("$.id", Long.class);

        // Retrieve
        mockMvc.perform(get("/api/v1/employees/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("bob@ems.com")));
    }

    @Test
    void searchEmployeesByName() throws Exception {
        // Seed data
        employeeRepository.save(Employee.builder()
            .firstName("Charlie").lastName("Brown")
            .email("charlie@ems.com").salary(BigDecimal.valueOf(70000))
            .build());

        mockMvc.perform(get("/api/v1/employees/search?name=charlie"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].firstName", is("Charlie")));
    }
}
```

---

## Summary

| Feature | Annotation/Class | Purpose |
|---|---|---|
| Thymeleaf template | `th:each`, `th:text`, `th:field` | Server-side HTML rendering |
| Form binding | `th:object`, `*{field}` | Bind form to model object |
| Security integration | `sec:authorize` | Show/hide based on role |
| Batch job | `@EnableBatchProcessing`, `Job`, `Step` | Large-scale processing |
| Chunk processing | `.chunk(100)` | Process N records at a time |
| JMS producer | `JmsTemplate.convertAndSend()` | Publish async message |
| JMS consumer | `@JmsListener` | React to async messages |
| Controller test | `@WebMvcTest` | Test MVC layer in isolation |
| Service mock | `@MockBean` | Mock dependencies |
| MockMvc request | `.perform(get(...))` | Simulate HTTP request |
| Assert response | `.andExpect(status().isOk())` | Verify HTTP response |
| Integration test | `@SpringBootTest` | Full context test |

---

*Previous: [07 — Spring Security](07_Spring_Security.md)*

---

## Complete File Index

| File | Topics Covered | Source Module |
|---|---|---|
| [01_Spring_Core.md](01_Spring_Core.md) | IoC, DI, Setter/Constructor/Autowiring, Lifecycle, Scopes, Properties, Collections, Stereotypes, Interface Injection, Java Config | Spring Framework |
| [02_Spring_Data_Access.md](02_Spring_Data_Access.md) | Spring JDBC, JdbcTemplate, RowMapper, Spring ORM, Hibernate, Transactions, AOP, Aspects | Spring Framework |
| [03_Spring_MVC.md](03_Spring_MVC.md) | DispatcherServlet, Controllers, Model/View, Form binding, AJAX/jQuery, Java Config for Web | Spring Framework |
| [04_Spring_Boot_Basics.md](04_Spring_Boot_Basics.md) | Auto-configuration, Starter POMs, application.properties, Profiles, Logging, Actuator | Spring Boot |
| [05_Spring_Data_JPA.md](05_Spring_Data_JPA.md) | Entity mapping, Relationships, JpaRepository, Derived queries, JPQL, Pagination, Caching, Validations | Spring Boot |
| [06_Spring_REST_API.md](06_Spring_REST_API.md) | REST CRUD, RestTemplate, WebClient, Spring Data REST, Postman testing, JSON serialisation, Swagger | Spring Boot + REST APIs |
| [07_Spring_Security.md](07_Spring_Security.md) | Basic Auth, JWT, Filter chain, Role-based access, Method security | Spring Boot + REST APIs |
| [08_Spring_Boot_Advanced.md](08_Spring_Boot_Advanced.md) | Thymeleaf, Spring Batch, Spring JMS/ActiveMQ, MockMvc testing | Spring Boot |

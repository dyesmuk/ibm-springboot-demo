# 03 — Spring MVC

> 🏢 **EMS Theme:** Building the web layer of EMS — forms to add employees, pages to list and search them, and AJAX-based updates.

---

## Table of Contents
- [Spring MVC Architecture](#spring-mvc-architecture)
- [Setup and Configuration](#setup-and-configuration)
- [Your First Controller](#your-first-controller)
- [Sending Data from Controller to UI](#sending-data-from-controller-to-ui)
- [Sending Data from UI to Controller](#sending-data-from-ui-to-controller)
- [Using ModelMap and String View](#using-modelmap-and-string-view)
- [Spring MVC and ORM](#spring-mvc-and-orm)
- [Spring MVC and AJAX Using jQuery](#spring-mvc-and-ajax-using-jquery)
- [Java Configuration for Web Applications](#java-configuration-for-web-applications)

---

## Spring MVC Architecture

Spring MVC is a **Model-View-Controller** web framework built on the Servlet API.

```
Browser Request
      │
      ▼
┌─────────────────┐
│  DispatcherServlet │  ← Front Controller — all requests pass through here
└────────┬────────┘
         │  asks HandlerMapping: "which controller handles this URL?"
         ▼
┌─────────────────┐
│  Controller      │  ← Your @Controller class processes the request
└────────┬────────┘
         │  returns ModelAndView (data + view name)
         ▼
┌─────────────────┐
│  ViewResolver    │  ← Resolves view name to actual JSP/Thymeleaf file
└────────┬────────┘
         ▼
┌─────────────────┐
│  View (JSP)      │  ← Renders HTML using Model data
└────────┬────────┘
         ▼
    Browser Response
```

| Component | Role |
|---|---|
| `DispatcherServlet` | Entry point — routes all requests |
| `HandlerMapping` | Maps URL to Controller method |
| `Controller` | Handles request, prepares model |
| `Model` | Data passed to the view |
| `ViewResolver` | Translates view name to file path |
| `View` | Renders the response (JSP, Thymeleaf) |

---

## Setup and Configuration

### `pom.xml` dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>6.1.0</version>
    </dependency>
    <!-- Servlet API -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <version>6.0.0</version>
        <scope>provided</scope>
    </dependency>
    <!-- JSTL for JSP -->
    <dependency>
        <groupId>jakarta.servlet.jsp.jstl</groupId>
        <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
<packaging>war</packaging>
```

### Java Configuration (no `web.xml`)

```java
// Replaces web.xml — registers DispatcherServlet programmatically
public class WebAppInitializer
    extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{AppConfig.class}; // non-web beans
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebConfig.class}; // web/MVC beans
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"}; // DispatcherServlet handles everything
    }
}
```

```java
@Configuration
@EnableWebMvc
@ComponentScan("com.ems.controller")
public class WebConfig implements WebMvcConfigurer {

    // ViewResolver: view name "employees/list" → /WEB-INF/views/employees/list.jsp
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver vr = new InternalResourceViewResolver();
        vr.setPrefix("/WEB-INF/views/");
        vr.setSuffix(".jsp");
        return vr;
    }

    // Serve static files (CSS, JS) from /resources/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }
}
```

---

## Your First Controller

```java
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // GET /employees/list
    @GetMapping("/list")
    public String listEmployees(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        return "employees/list";  // resolves to /WEB-INF/views/employees/list.jsp
    }

    // GET /employees/  or  GET /employees
    @GetMapping({"", "/"})
    public String home() {
        return "redirect:/employees/list";
    }
}
```

---

## Sending Data from Controller to UI

Three ways to pass data from your controller to the view.

### 1. `Model` object

```java
@GetMapping("/list")
public String listEmployees(Model model) {
    model.addAttribute("employees", employeeService.getAllEmployees());
    model.addAttribute("totalCount", employeeService.count());
    model.addAttribute("pageTitle", "All Employees");
    return "employees/list";
}
```

### 2. `ModelAndView`

```java
@GetMapping("/view/{id}")
public ModelAndView viewEmployee(@PathVariable int id) {
    ModelAndView mav = new ModelAndView();
    mav.setViewName("employees/detail");
    mav.addObject("employee", employeeService.findById(id));
    mav.addObject("departments", departmentService.findAll());
    return mav;
}
```

### 3. `@ModelAttribute` — add to every method in controller

```java
// This runs before EVERY handler method in this controller
@ModelAttribute("departments")
public List<Department> populateDepartments() {
    return departmentService.findAll(); // available in all views
}
```

### JSP View — `employees/list.jsp`

```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head><title>${pageTitle}</title></head>
<body>
    <h2>${pageTitle}</h2>
    <p>Total Employees: ${totalCount}</p>

    <table border="1">
        <tr>
            <th>ID</th><th>Name</th><th>Email</th>
            <th>Salary</th><th>Department</th><th>Actions</th>
        </tr>
        <c:forEach var="emp" items="${employees}">
            <tr>
                <td>${emp.id}</td>
                <td>${emp.name}</td>
                <td>${emp.email}</td>
                <td><fmt:formatNumber value="${emp.salary}" type="currency"/></td>
                <td>${emp.department.name}</td>
                <td>
                    <a href="/employees/edit/${emp.id}">Edit</a> |
                    <a href="/employees/delete/${emp.id}">Delete</a>
                </td>
            </tr>
        </c:forEach>
    </table>

    <a href="/employees/new">Add New Employee</a>
</body>
</html>
```

---

## Sending Data from UI to Controller

### GET Request — URL Parameters

```java
// GET /employees/search?name=alice&deptId=3
@GetMapping("/search")
public String searchEmployees(
        @RequestParam("name") String name,
        @RequestParam(value = "deptId", required = false) Integer deptId,
        Model model) {

    List<Employee> results = employeeService.search(name, deptId);
    model.addAttribute("employees", results);
    model.addAttribute("searchName", name);
    return "employees/list";
}
```

### Path Variables

```java
// GET /employees/view/42
@GetMapping("/view/{id}")
public String viewEmployee(@PathVariable("id") int id, Model model) {
    model.addAttribute("employee", employeeService.findById(id));
    return "employees/detail";
}
```

### POST Request — Form Submission

**Controller:**
```java
// GET /employees/new — show empty form
@GetMapping("/new")
public String showNewEmployeeForm(Model model) {
    model.addAttribute("employee", new Employee()); // empty object for form binding
    model.addAttribute("departments", departmentService.findAll());
    return "employees/form";
}

// POST /employees/save — handle form submission
@PostMapping("/save")
public String saveEmployee(@ModelAttribute("employee") Employee employee,
                           BindingResult result,
                           RedirectAttributes redirectAttrs) {
    if (result.hasErrors()) {
        return "employees/form"; // validation failed — back to form
    }
    employeeService.save(employee);
    redirectAttrs.addFlashAttribute("successMsg", "Employee saved successfully!");
    return "redirect:/employees/list"; // PRG pattern — prevent double submit
}
```

**JSP Form — `employees/form.jsp`:**
```jsp
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<html>
<body>
    <h2>Employee Form</h2>

    <c:if test="${not empty successMsg}">
        <p style="color:green">${successMsg}</p>
    </c:if>

    <!-- Spring form tag — binds to the 'employee' model attribute -->
    <form:form modelAttribute="employee" action="/employees/save" method="post">

        <form:hidden path="id"/>

        <label>Name:</label>
        <form:input path="name" placeholder="Full Name"/>
        <form:errors path="name" cssStyle="color:red"/>
        <br/>

        <label>Email:</label>
        <form:input path="email" placeholder="email@company.com"/>
        <form:errors path="email" cssStyle="color:red"/>
        <br/>

        <label>Salary:</label>
        <form:input path="salary" type="number"/>
        <br/>

        <label>Department:</label>
        <form:select path="department.id">
            <form:option value="" label="-- Select Department --"/>
            <form:options items="${departments}"
                          itemValue="id" itemLabel="name"/>
        </form:select>
        <br/>

        <input type="submit" value="Save Employee"/>
    </form:form>

    <a href="/employees/list">Back to List</a>
</body>
</html>
```

---

## Using ModelMap and String View

`ModelMap` is an enhanced `Map` for the model — use it when you prefer explicit map operations.

```java
@GetMapping("/dashboard")
public String dashboard(ModelMap model) {
    // ModelMap has addAttribute() — same as Model
    model.addAttribute("totalEmployees", employeeService.count());
    model.addAttribute("departments",    departmentService.findAll());
    model.addAttribute("recentHires",    employeeService.getRecentHires(5));

    // Chain calls
    model.addAttribute("activeProjects", projectService.getActive())
         .addAttribute("pendingApprovals", approvalService.getPending());

    return "dashboard"; // String view name
}
```

### Return type as `String` vs `ModelAndView`

```java
// String return — simpler, use when model is already populated
@GetMapping("/list")
public String list(Model model) {
    model.addAttribute("employees", employeeService.findAll());
    return "employees/list";
}

// ModelAndView return — when you want to package model + view together
@GetMapping("/report")
public ModelAndView report() {
    ModelAndView mav = new ModelAndView("employees/report");
    mav.addObject("data", employeeService.getReportData());
    return mav;
}

// Redirect — prevents double form submission (PRG pattern)
@PostMapping("/save")
public String save(@ModelAttribute Employee emp) {
    employeeService.save(emp);
    return "redirect:/employees/list";
}

// Forward — server-side forward (URL doesn't change)
@GetMapping("/old-path")
public String legacyPath() {
    return "forward:/employees/list";
}
```

---

## Spring MVC and ORM

Combining Spring MVC + Hibernate in a complete CRUD flow.

### Full CRUD Controller

```java
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentService departmentService;

    @ModelAttribute("departments")
    public List<Department> departments() {
        return departmentService.findAll();
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employees/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable int id, Model model) {
        model.addAttribute("employee", employeeService.findById(id));
        return "employees/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Employee emp,
                       BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) return "employees/form";
        employeeService.save(emp);
        ra.addFlashAttribute("msg", "Employee saved!");
        return "redirect:/employees/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        employeeService.delete(id);
        ra.addFlashAttribute("msg", "Employee deleted.");
        return "redirect:/employees/list";
    }
}
```

### EmployeeService (Transactional)

```java
@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> findAll()           { return employeeRepository.findAll(); }
    public Employee findById(int id)          { return employeeRepository.findById(id); }
    public void save(Employee e)              { employeeRepository.save(e); }
    public void delete(int id)                { employeeRepository.delete(id); }
    public long count()                       { return employeeRepository.count(); }
}
```

---

## Spring MVC and AJAX Using jQuery

Make EMS more responsive — update salary without a full page reload.

### Controller — returns JSON for AJAX

```java
@Controller
@RequestMapping("/employees")
public class EmployeeAjaxController {

    @Autowired
    private EmployeeService employeeService;

    // Returns JSON list — consumed by jQuery AJAX
    @GetMapping(value = "/search/ajax", produces = "application/json")
    @ResponseBody
    public List<Employee> searchAjax(@RequestParam String query) {
        return employeeService.search(query);
    }

    // Update salary via AJAX POST
    @PostMapping(value = "/update-salary", produces = "application/json")
    @ResponseBody
    public Map<String, Object> updateSalary(
            @RequestParam int employeeId,
            @RequestParam double newSalary) {

        employeeService.updateSalary(employeeId, newSalary);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Salary updated successfully");
        response.put("newSalary", newSalary);
        return response;
    }
}
```

### JSP — jQuery AJAX calls

```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
</head>
<body>

<!-- Search Box -->
<input type="text" id="searchBox" placeholder="Search employees..."/>
<div id="searchResults"></div>

<!-- Salary Update -->
<div id="salaryUpdate">
    <input type="number" id="newSalary" placeholder="New Salary"/>
    <button id="updateBtn">Update Salary</button>
    <span id="salaryMsg"></span>
</div>

<script>
    // Live search as user types
    $('#searchBox').on('keyup', function () {
        var query = $(this).val();
        if (query.length < 2) return;

        $.ajax({
            url: '/employees/search/ajax',
            type: 'GET',
            data: { query: query },
            success: function (employees) {
                var html = '<ul>';
                $.each(employees, function (i, emp) {
                    html += '<li>' + emp.name + ' — ' + emp.department.name + '</li>';
                });
                html += '</ul>';
                $('#searchResults').html(html);
            },
            error: function () {
                $('#searchResults').html('<p>Search failed</p>');
            }
        });
    });

    // Update salary without page reload
    $('#updateBtn').on('click', function () {
        var employeeId = <c:out value="${employee.id}"/>;
        var newSalary = $('#newSalary').val();

        $.ajax({
            url: '/employees/update-salary',
            type: 'POST',
            data: { employeeId: employeeId, newSalary: newSalary },
            success: function (response) {
                if (response.success) {
                    $('#salaryMsg').css('color', 'green')
                                  .text('Updated to ₹' + response.newSalary);
                }
            },
            error: function () {
                $('#salaryMsg').css('color', 'red').text('Update failed');
            }
        });
    });
</script>

</body>
</html>
```

> 💡 **Note:** `@ResponseBody` tells Spring to write the return value directly as JSON (via Jackson) — skipping the ViewResolver entirely.

---

## Java Configuration for Web Applications

Complete Java-based web configuration — no `web.xml`, no Spring XML files.

### Project structure (no XML config)
```
src/main/
├── java/
│   └── com/ems/
│       ├── config/
│       │   ├── AppConfig.java          ← Root context (services, repos)
│       │   ├── DataConfig.java         ← DataSource, JdbcTemplate
│       │   └── WebConfig.java          ← MVC config, ViewResolver
│       ├── init/
│       │   └── WebAppInitializer.java  ← Replaces web.xml
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── model/
└── webapp/
    ├── WEB-INF/
    │   └── views/                      ← JSP files
    └── resources/
        ├── css/
        └── js/
```

### Complete `WebConfig.java`

```java
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.ems.controller")
public class WebConfig implements WebMvcConfigurer {

    // JSP ViewResolver
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver r = new InternalResourceViewResolver();
        r.setPrefix("/WEB-INF/views/");
        r.setSuffix(".jsp");
        r.setOrder(2);
        return r;
    }

    // Static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    // Enable serving index.html for /
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/employees/list");
    }

    // Global CORS (useful when frontend is separate)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    // Message source for i18n
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }
}
```

---

## Summary

| Concept | Key Class/Annotation | Purpose |
|---|---|---|
| Front Controller | `DispatcherServlet` | Routes all HTTP requests |
| Controller | `@Controller`, `@RequestMapping` | Handle requests |
| GET mapping | `@GetMapping` | Handle GET requests |
| POST mapping | `@PostMapping` | Handle POST/form submissions |
| Path variable | `@PathVariable` | Extract from URL path |
| Request param | `@RequestParam` | Extract query params |
| Model | `Model`, `ModelMap`, `ModelAndView` | Pass data to view |
| Form binding | `@ModelAttribute` | Bind form fields to object |
| Validation | `@Valid`, `BindingResult` | Validate input |
| AJAX / JSON | `@ResponseBody` | Return JSON directly |
| Redirect | `"redirect:/path"` | PRG pattern |
| ViewResolver | `InternalResourceViewResolver` | Find JSP files |

---

*Previous: [02 — Spring Data Access](02_Spring_Data_Access.md) | Next: [04 — Spring Boot Basics](04_Spring_Boot_Basics.md)*

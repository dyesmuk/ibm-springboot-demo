# 01 — Spring Core: IoC Container & Dependency Injection

> 🏢 **EMS Theme:** We are building an **Employee Management System (EMS)**. Throughout all Spring modules, our domain consists of `Employee`, `Department`, `Role`, and `Project` entities.

---

## Table of Contents
- [Introduction to Spring Framework](#introduction-to-spring-framework)
- [Software Setup](#software-setup)
- [Spring Core Concepts — IoC & DI](#spring-core-concepts--ioc--di)
- [Setter Injection](#setter-injection)
- [Constructor Injection](#constructor-injection)
- [Auto-Wiring](#auto-wiring)
- [Life Cycle Methods](#life-cycle-methods)
- [Dependency Check, Inner Beans and Scopes](#dependency-check-inner-beans-and-scopes)
- [Using Properties](#using-properties)
- [Standalone Collections](#standalone-collections)
- [Stereotype Annotations](#stereotype-annotations)
- [Injecting Interfaces](#injecting-interfaces)
- [Java Configuration](#java-configuration)

---

## Introduction to Spring Framework

Spring is an **open-source, lightweight, enterprise Java framework** that simplifies the development of Java applications. It provides infrastructure support so developers focus on business logic rather than plumbing code.

### Why Spring?

| Problem Without Spring | How Spring Solves It |
|---|---|
| Manual object creation everywhere | IoC container manages object lifecycle |
| Hard-coded dependencies | Dependency Injection wires objects together |
| Boilerplate JDBC code | Spring JDBC / JPA templates |
| Manual transaction management | Declarative `@Transactional` |
| Scattered cross-cutting concerns | AOP (Aspect-Oriented Programming) |

> 🔎 **Analogy:** Spring is like an HR department for your Java objects. HR (Spring container) creates employees (objects), assigns them their tools and teammates (dependencies), and manages their employment lifecycle — you just define the job description (configuration).

### Spring Ecosystem

```
Spring Framework (Core)
    ├── Spring Core        ← IoC, DI
    ├── Spring JDBC        ← Database access
    ├── Spring ORM         ← Hibernate integration
    ├── Spring MVC         ← Web layer
    └── Spring AOP         ← Cross-cutting concerns

Spring Boot              ← Auto-configuration, embedded server
Spring Data JPA          ← Repository abstraction
Spring Security          ← Authentication & Authorization
Spring Batch             ← Batch processing
Spring Cloud             ← Microservices
```

---

## Software Setup

### Prerequisites
- JDK 17+
- Eclipse IDE (with Spring Tools Suite plugin recommended)
- Maven 3.6+

### Install Spring Tools Suite (STS) Plugin in Eclipse
1. **Help → Eclipse Marketplace**
2. Search for **"Spring Tools"**
3. Install **Spring Tools 4 (aka Spring Tool Suite 4)**
4. Restart Eclipse

### Create a Spring Maven Project
1. **File → New → Maven Project**
2. Use archetype: `maven-archetype-quickstart`
3. GroupId: `com.ems`, ArtifactId: `ems-core`

### Add Spring Dependencies to `pom.xml`
```xml
<dependencies>
    <!-- Spring Context (includes Core, Beans, AOP) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>6.1.0</version>
    </dependency>

    <!-- For XML config (optional if using Java config only) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
        <version>6.1.0</version>
    </dependency>
</dependencies>
```

---

## Spring Core Concepts — IoC & DI

### Inversion of Control (IoC)

In traditional Java, **you** create and manage objects:
```java
// Traditional - tight coupling
public class EmployeeService {
    private EmployeeRepository repo = new EmployeeRepository(); // YOU create it
}
```

With IoC, the **Spring Container** creates and manages objects:
```java
// Spring IoC - loose coupling
public class EmployeeService {
    private EmployeeRepository repo; // Spring GIVES it to you
}
```

> 🔎 **Analogy:** Instead of a chef going to the market to buy ingredients (creating dependencies), the ingredients are delivered to the kitchen (injected). The chef focuses on cooking — not logistics.

### The Spring IoC Container

The container reads configuration (XML or annotations), creates objects (**beans**), wires their dependencies, and manages their lifecycle.

Two main container types:
- `BeanFactory` — basic, lazy loading (rarely used directly)
- `ApplicationContext` — full-featured, eager loading ✅ (use this)

### Dependency Injection (DI)

DI is the mechanism Spring uses to implement IoC — it *injects* the dependencies an object needs.

Three ways to inject:
1. **Setter Injection** — via setter methods
2. **Constructor Injection** — via constructor
3. **Field Injection** — via `@Autowired` on fields (less preferred)

---

## Setter Injection

Spring calls the setter method after creating the bean to inject the dependency.

### EMS Example — XML Configuration

**`Department.java`**
```java
package com.ems.model;

public class Department {
    private String name;
    private String location;

    // Setter methods — Spring calls these
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }

    public String getName() { return name; }
    public String getLocation() { return location; }

    @Override
    public String toString() {
        return "Department{name='" + name + "', location='" + location + "'}";
    }
}
```

**`EmployeeService.java`**
```java
package com.ems.service;

import com.ems.model.Department;

public class EmployeeService {
    private String serviceName;
    private Department department; // dependency

    // Setter for primitive
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    // Setter for object reference
    public void setDepartment(Department department) {
        this.department = department;
    }

    public void printInfo() {
        System.out.println("Service: " + serviceName);
        System.out.println("Department: " + department);
    }
}
```

**`beans.xml`** (Spring configuration)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Bean for Department -->
    <bean id="hrDepartment" class="com.ems.model.Department">
        <property name="name" value="Human Resources"/>
        <property name="location" value="Mumbai"/>
    </bean>

    <!-- Bean for EmployeeService with setter injection -->
    <bean id="employeeService" class="com.ems.service.EmployeeService">
        <property name="serviceName" value="EMS Core Service"/>
        <property name="department" ref="hrDepartment"/>  <!-- ref = reference to another bean -->
    </bean>

</beans>
```

**`Main.java`**
```java
package com.ems;

import com.ems.service.EmployeeService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        // Load Spring container from XML
        ApplicationContext context =
            new ClassPathXmlApplicationContext("beans.xml");

        // Get bean from container
        EmployeeService service =
            (EmployeeService) context.getBean("employeeService");

        service.printInfo();
        // Output:
        // Service: EMS Core Service
        // Department: Department{name='Human Resources', location='Mumbai'}
    }
}
```

> 💡 **Tip:** `ref` is used to inject another Spring bean. `value` is used for primitives and Strings.

---

## Constructor Injection

Spring uses the constructor to inject dependencies at the time of object creation. Preferred when dependencies are **mandatory**.

### EMS Example

**`Employee.java`**
```java
package com.ems.model;

public class Employee {
    private int id;
    private String name;
    private Department department;

    // Constructor injection
    public Employee(int id, String name, Department department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name +
               "', dept=" + department.getName() + "}";
    }
}
```

**`beans.xml`**
```xml
<!-- Constructor injection using index -->
<bean id="employee1" class="com.ems.model.Employee">
    <constructor-arg index="0" value="101"/>
    <constructor-arg index="1" value="Alice"/>
    <constructor-arg index="2" ref="hrDepartment"/>
</bean>

<!-- Constructor injection using name (cleaner) -->
<bean id="employee2" class="com.ems.model.Employee">
    <constructor-arg name="id" value="102"/>
    <constructor-arg name="name" value="Bob"/>
    <constructor-arg name="department" ref="hrDepartment"/>
</bean>
```

### Setter vs Constructor Injection

| Aspect | Setter Injection | Constructor Injection |
|---|---|---|
| When injected | After object creation | During object creation |
| Optional dependencies | ✅ Good for optional | ❌ All args required |
| Immutability | ❌ Object can change | ✅ Object stays consistent |
| Circular dependency | ✅ Can resolve | ❌ Causes error |
| Best for | Optional config | Mandatory dependencies |

> 💡 **Best Practice:** Prefer constructor injection for required dependencies. Use setter injection for optional ones. Spring team recommends constructor injection.

---

## Auto-Wiring

Instead of manually specifying `ref` in XML, Spring can **automatically** detect and inject matching beans.

### Auto-wire modes

| Mode | How it works |
|---|---|
| `no` | Default. No auto-wiring. |
| `byName` | Matches bean `id` with property name |
| `byType` | Matches bean type with property type |
| `constructor` | Like `byType` but for constructors |

### XML Auto-wire Example

```xml
<!-- byType: Spring finds a bean of type Department and injects it -->
<bean id="employeeService" class="com.ems.service.EmployeeService"
      autowire="byType">
    <property name="serviceName" value="EMS Service"/>
    <!-- department is auto-wired — no need to write ref -->
</bean>

<bean id="department" class="com.ems.model.Department">
    <property name="name" value="IT"/>
    <property name="location" value="Bangalore"/>
</bean>
```

### Annotation-based Auto-wiring — `@Autowired`

More common in modern Spring. Eliminates XML wiring entirely.

```java
package com.ems.service;

import com.ems.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class EmployeeService {

    @Autowired  // Spring finds EmployeeRepository bean and injects it
    private EmployeeRepository employeeRepository;

    // Or on constructor (preferred):
    private final DepartmentService departmentService;

    @Autowired
    public EmployeeService(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }
}
```

> 💡 **Tip:** If there is exactly one bean of the required type, `@Autowired` works seamlessly. If there are multiple, use `@Qualifier("beanName")` to specify which one.

```java
@Autowired
@Qualifier("hrDepartmentService")
private DepartmentService departmentService;
```

---

## Life Cycle Methods

Spring manages the complete lifecycle of a bean. You can hook into **initialization** and **destruction** phases.

```
Container starts
    → Bean instantiated
    → Dependencies injected
    → init-method called   ← your custom init logic
    → Bean is ready to use
    → ... application runs ...
    → destroy-method called ← your custom cleanup
    → Container shuts down
```

### XML Configuration
```xml
<bean id="employeeService" class="com.ems.service.EmployeeService"
      init-method="onStartup"
      destroy-method="onShutdown">
</bean>
```

### Java Implementation
```java
public class EmployeeService {

    public void onStartup() {
        System.out.println("EmployeeService initializing — loading config...");
        // e.g. load cache, open connections
    }

    public void onShutdown() {
        System.out.println("EmployeeService shutting down — releasing resources...");
        // e.g. close connections, flush cache
    }
}
```

### Using Interfaces (alternative)
```java
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

public class EmployeeService implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Init: after all properties set");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("Destroy: before bean removed from container");
    }
}
```

### Using Annotations (modern, preferred)
```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class EmployeeService {

    @PostConstruct
    public void init() {
        System.out.println("Bean initialized — EMS Service ready");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Bean about to be destroyed — cleaning up");
    }
}
```

---

## Dependency Check, Inner Beans and Scopes

### Bean Scopes

Scope controls **how many instances** of a bean Spring creates.

| Scope | Description | Use Case |
|---|---|---|
| `singleton` | **Default.** One instance per container | Services, Repositories |
| `prototype` | New instance every time `getBean()` is called | Stateful objects |
| `request` | One per HTTP request | Web apps only |
| `session` | One per HTTP session | Web apps only |
| `application` | One per ServletContext | Web apps only |

```xml
<!-- Singleton (default) -->
<bean id="employeeService" class="com.ems.service.EmployeeService"
      scope="singleton"/>

<!-- Prototype — new instance each time -->
<bean id="employeeForm" class="com.ems.model.EmployeeForm"
      scope="prototype"/>
```

```java
// With annotations
@Component
@Scope("prototype")
public class EmployeeForm { ... }
```

### Inner Beans

A bean defined **inside** another bean's `<property>` — it cannot be referenced from outside.

```xml
<bean id="employee" class="com.ems.model.Employee">
    <property name="name" value="Charlie"/>
    <property name="department">
        <!-- Inner bean — anonymous, only for this employee -->
        <bean class="com.ems.model.Department">
            <property name="name" value="Finance"/>
            <property name="location" value="Delhi"/>
        </bean>
    </property>
</bean>
```

---

## Using Properties

Externalise configuration values into `.properties` files — avoids hardcoding.

### `application.properties`
```properties
ems.service.name=Employee Management System
ems.db.url=jdbc:mysql://localhost:3306/emsdb
ems.db.username=root
ems.db.maxConnections=10
```

### XML approach
```xml
<!-- Load the properties file -->
<context:property-placeholder location="classpath:application.properties"/>

<bean id="dataSource" class="com.ems.config.DataSourceConfig">
    <property name="url" value="${ems.db.url}"/>
    <property name="username" value="${ems.db.username}"/>
</bean>
```

### Annotation approach — `@Value`
```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    @Value("${ems.service.name}")
    private String serviceName;

    @Value("${ems.db.maxConnections}")
    private int maxConnections;

    public void printConfig() {
        System.out.println("Running: " + serviceName);
        System.out.println("Max connections: " + maxConnections);
    }
}
```

---

## Standalone Collections

Inject `List`, `Set`, `Map`, and `Properties` directly through Spring configuration.

### EMS Example — Employee with list of skills

**`Employee.java`**
```java
public class Employee {
    private String name;
    private List<String> skills;
    private Map<String, String> certifications; // cert name -> issued date
    private Properties contactInfo;

    // setters and getters...
}
```

**`beans.xml`**
```xml
<bean id="seniorEmployee" class="com.ems.model.Employee">
    <property name="name" value="Diana"/>

    <property name="skills">
        <list>
            <value>Java</value>
            <value>Spring Boot</value>
            <value>Microservices</value>
        </list>
    </property>

    <property name="certifications">
        <map>
            <entry key="AWS Solutions Architect" value="2023-06-15"/>
            <entry key="Spring Professional"     value="2022-11-01"/>
        </map>
    </property>

    <property name="contactInfo">
        <props>
            <prop key="email">diana@ems.com</prop>
            <prop key="phone">+91-9876543210</prop>
        </props>
    </property>
</bean>
```

---

## Stereotype Annotations

Stereotype annotations mark classes as Spring-managed beans, eliminating XML bean declarations.

| Annotation | Used For | Layer |
|---|---|---|
| `@Component` | Generic Spring bean | Any |
| `@Service` | Business logic | Service layer |
| `@Repository` | Data access | DAO/Repository layer |
| `@Controller` | Web request handling | Presentation layer |
| `@RestController` | REST API | Presentation layer |

### Enable Component Scanning

**XML:**
```xml
<context:component-scan base-package="com.ems"/>
```

**Java Config:**
```java
@Configuration
@ComponentScan("com.ems")
public class AppConfig { }
```

### EMS Example

```java
// Repository layer
@Repository
public class EmployeeRepository {
    public Employee findById(int id) {
        // DB logic here
        return new Employee(id, "Alice", null);
    }
}

// Service layer
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee getEmployee(int id) {
        return employeeRepository.findById(id);
    }
}

// Controller layer (for web)
@Controller
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // request mappings go here
}
```

> 💡 **Why separate annotations?**
> - `@Repository` adds exception translation (converts SQL exceptions to Spring exceptions)
> - `@Service` signals business logic — useful for AOP pointcuts
> - `@Controller` enables Spring MVC request mapping

---

## Injecting Interfaces

Always **program to interfaces**, not implementations. This makes your code loosely coupled and easily testable.

### EMS Example

```java
// Define the interface
public interface NotificationService {
    void sendNotification(String employeeEmail, String message);
}

// Email implementation
@Service("emailNotification")
public class EmailNotificationService implements NotificationService {
    @Override
    public void sendNotification(String email, String message) {
        System.out.println("Sending EMAIL to " + email + ": " + message);
    }
}

// SMS implementation
@Service("smsNotification")
public class SmsNotificationService implements NotificationService {
    @Override
    public void sendNotification(String email, String message) {
        System.out.println("Sending SMS to " + email + ": " + message);
    }
}

// Inject the interface — Spring decides which implementation
@Service
public class EmployeeOnboardingService {

    private final NotificationService notificationService;

    @Autowired
    @Qualifier("emailNotification")  // specify which impl to inject
    public EmployeeOnboardingService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void onboardEmployee(Employee emp) {
        // ... save employee ...
        notificationService.sendNotification(
            emp.getEmail(),
            "Welcome to the company, " + emp.getName() + "!"
        );
    }
}
```

> 🔎 **Analogy:** Your office uses a courier service (interface). Today it's BlueDart, tomorrow FedEx (implementations). Your desk (EmployeeService) doesn't care which courier — it just hands over the package.

---

## Java Configuration

Modern Spring prefers **Java-based configuration** over XML — type-safe, refactorable, IDE-friendly.

### `@Configuration` and `@Bean`

```java
package com.ems.config;

import org.springframework.context.annotation.*;

@Configuration          // Marks this as a Spring config class
@ComponentScan("com.ems")          // Enable annotation scanning
@PropertySource("classpath:application.properties")  // Load properties
public class AppConfig {

    @Value("${ems.service.name}")
    private String serviceName;

    // Define beans explicitly when needed
    @Bean
    public Department hrDepartment() {
        Department dept = new Department();
        dept.setName("Human Resources");
        dept.setLocation("Mumbai");
        return dept;
    }

    @Bean
    public EmployeeService employeeService() {
        EmployeeService service = new EmployeeService();
        service.setServiceName(serviceName);
        service.setDepartment(hrDepartment()); // Spring manages this — no duplicate bean
        return service;
    }
}
```

### Loading Java Config

```java
public class Main {
    public static void main(String[] args) {
        // Use AnnotationConfigApplicationContext for Java config
        ApplicationContext context =
            new AnnotationConfigApplicationContext(AppConfig.class);

        EmployeeService service = context.getBean(EmployeeService.class);
        service.printInfo();
    }
}
```

### XML Config vs Java Config vs Annotation-only

| Approach | When to Use |
|---|---|
| XML Config | Legacy projects, when you can't modify source code |
| Java Config (`@Configuration`) | Explicit wiring, third-party bean setup |
| Annotation (`@Component`, `@Autowired`) | Your own classes — cleanest, most common |

> 💡 **Real world:** Most Spring Boot projects use annotations (`@Service`, `@Autowired`) for your own classes, and Java Config (`@Bean` in `@Configuration`) for third-party integrations like DataSource, RestTemplate, etc.

---

## Summary

| Concept | What It Does | Key Annotation/Element |
|---|---|---|
| IoC Container | Creates and manages beans | `ApplicationContext` |
| Setter Injection | Injects via setter methods | `<property>` / `@Autowired` on setter |
| Constructor Injection | Injects via constructor | `<constructor-arg>` / `@Autowired` on constructor |
| Auto-Wiring | Spring auto-detects beans to inject | `@Autowired`, `@Qualifier` |
| Bean Lifecycle | init and destroy hooks | `@PostConstruct`, `@PreDestroy` |
| Bean Scope | Controls how many instances | `@Scope("prototype")` |
| Properties | Externalise config | `@Value`, `@PropertySource` |
| Stereotype | Mark class as Spring bean | `@Component`, `@Service`, `@Repository` |
| Interface Injection | Loose coupling via interfaces | `@Qualifier` |
| Java Config | Type-safe bean definition | `@Configuration`, `@Bean` |

---

*Next: [02 — Spring Data Access: JDBC, ORM & AOP](02_Spring_Data_Access.md)*

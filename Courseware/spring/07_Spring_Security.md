# 07 — Spring Security

> 🏢 **EMS Theme:** Securing the EMS API — only authenticated users can access employee data, and only HR/Admin roles can create or delete records.

---

## Table of Contents
- [Introduction to Spring Security](#introduction-to-spring-security)
- [Setup](#setup)
- [Basic Authentication](#basic-authentication)
- [JWT Authentication](#jwt-authentication)
- [Role-Based Access Control](#role-based-access-control)
- [Securing the Micro Service API](#securing-the-micro-service-api)
- [Method-Level Security](#method-level-security)

---

## Introduction to Spring Security

Spring Security is the standard security framework for Spring applications — handles authentication (who are you?) and authorisation (what can you do?).

> 🔎 **Analogy:** An office building has a security guard at the door (authentication — checking your ID badge) and an access card system (authorisation — your card only opens floors you're allowed on).

### Security Filter Chain

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────────┐
│              Security Filter Chain                │
│  ┌──────────────────────────────────────────┐    │
│  │  1. JwtAuthenticationFilter              │    │
│  │  2. UsernamePasswordAuthenticationFilter │    │
│  │  3. BasicAuthenticationFilter            │    │
│  │  4. ExceptionTranslationFilter           │    │
│  │  5. FilterSecurityInterceptor            │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
    │
    ▼ (if allowed)
 Controller
```

---

## Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

> 💡 **Auto-protection:** Just adding `spring-boot-starter-security` to your project immediately protects all endpoints with HTTP Basic Auth. A random password is printed to the console on startup.

---

## Basic Authentication

For simple use cases or internal APIs. Credentials sent with every request.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // disable for REST APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/employees/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("alice")
            .password("password")
            .roles("EMPLOYEE")
            .build();

        UserDetails admin = User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin123")
            .roles("ADMIN", "HR")
            .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
```

---

## JWT Authentication

Industry standard for stateless REST APIs. Client logs in once, gets a token, sends it with every request.

```
Login                          Subsequent Requests
──────                         ───────────────────
Client ──POST /auth/login──▶  Validate credentials
       ◀─── JWT token ───────  ←─────────────────
                               
Client ──GET /api/employees──▶ Validate JWT
       ◀─── Employee data ───  ←─────────────────
```

### User Entity

```java
@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;  // stored as BCrypt hash

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    public enum UserRole { EMPLOYEE, MANAGER, HR, ADMIN }

    // UserDetails interface methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
            .collect(Collectors.toSet());
    }
    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return true; }
}
```

### JWT Utility

```java
@Component
public class JwtUtil {

    @Value("${ems.jwt.secret}")
    private String secretKey;

    @Value("${ems.jwt.expiration:86400000}") // 24 hours default
    private long expiration;

    // Generate token
    public String generateToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    // Extract username from token
    public String extractUsername(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    // Validate token
    public boolean isTokenValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiry = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();
        return expiry.before(new Date());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
```

```properties
# application.properties
ems.jwt.secret=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
ems.jwt.expiration=86400000
```

### JWT Filter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7); // strip "Bearer "
        String username = jwtUtil.extractUsername(jwt);

        if (username != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails user = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(jwt, user)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());

                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }
}
```

### Security Configuration with JWT

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                // Secured endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/employees/**")
                    .hasAnyRole("EMPLOYEE", "MANAGER", "HR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/employees")
                    .hasAnyRole("HR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**")
                    .hasAnyRole("HR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**")
                    .hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Auth Controller

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword())
        );

        UserDetails user = userDetailsService.loadUserByUsername(
            request.getUsername());
        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token,
            user.getUsername(), user.getAuthorities().toString()));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                .body("Username already taken");
        }
        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(Set.of(User.UserRole.EMPLOYEE))
            .build();
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body("User registered successfully");
    }
}

@Data public class AuthRequest {
    private String username;
    private String password;
}

@Data @AllArgsConstructor public class AuthResponse {
    private String token;
    private String username;
    private String roles;
}
```

### Usage (Postman / curl)

```bash
# Step 1 — Login
POST http://localhost:8080/api/auth/login
Body: { "username": "alice", "password": "password" }
Response: { "token": "eyJhbGciOiJIUzI1NiJ9..." }

# Step 2 — Use token in all subsequent requests
GET http://localhost:8080/api/v1/employees
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Role-Based Access Control

### URL-Level Security (in SecurityConfig)

```java
.requestMatchers(HttpMethod.GET,    "/api/v1/employees/**").hasAnyRole("EMPLOYEE","MANAGER","HR","ADMIN")
.requestMatchers(HttpMethod.POST,   "/api/v1/employees").hasAnyRole("HR","ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**").hasRole("ADMIN")
.requestMatchers("/api/v1/payroll/**").hasAnyRole("HR","ADMIN")
.requestMatchers("/api/v1/reports/**").hasAnyRole("MANAGER","HR","ADMIN")
```

### EMS Role Matrix

| Endpoint | EMPLOYEE | MANAGER | HR | ADMIN |
|---|---|---|---|---|
| GET /employees | ✅ (own) | ✅ (team) | ✅ (all) | ✅ |
| POST /employees | ❌ | ❌ | ✅ | ✅ |
| PUT /employees/{id} | ❌ | ❌ | ✅ | ✅ |
| DELETE /employees/{id} | ❌ | ❌ | ❌ | ✅ |
| GET /payroll | ❌ | ❌ | ✅ | ✅ |
| GET /reports | ❌ | ✅ | ✅ | ✅ |

---

## Securing the Micro Service API

### Method-Level Security

Enable with `@EnableMethodSecurity` in your config, then use on individual methods:

```java
@Service
public class EmployeeService {

    // Only HR and Admin can create
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public Employee create(EmployeeRequest request) { ... }

    // Only Admin can delete
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) { ... }

    // Employees can only view their own record; managers can view all
    @PreAuthorize("hasRole('MANAGER') or #id == authentication.principal.id")
    public Employee findById(Long id) { ... }

    // Verify return value after execution
    @PostAuthorize("returnObject.email == authentication.name or hasRole('ADMIN')")
    public Employee getByEmail(String email) { ... }
}
```

### Get Current User in Controller

```java
@GetMapping("/me")
public ResponseEntity<EmployeeResponse> getCurrentUser(
        @AuthenticationPrincipal UserDetails currentUser) {
    Employee emp = employeeService.findByEmail(currentUser.getUsername());
    return ResponseEntity.ok(EmployeeMapper.toResponse(emp));
}

// Or via SecurityContextHolder
public String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getName();
}
```

---

## Summary

| Feature | Annotation/Class | Purpose |
|---|---|---|
| Security config | `@EnableWebSecurity` | Enable Spring Security |
| HTTP rules | `HttpSecurity.authorizeHttpRequests()` | URL-level access control |
| JWT filter | `OncePerRequestFilter` | Extract and validate JWT |
| Password hash | `BCryptPasswordEncoder` | Secure password storage |
| Method security | `@PreAuthorize` | Fine-grained method access |
| Token generation | `JwtUtil.generateToken()` | Create signed JWT |
| Current user | `@AuthenticationPrincipal` | Inject current user |
| Role check | `hasRole()`, `hasAnyRole()` | Role-based rules |
| Stateless session | `SessionCreationPolicy.STATELESS` | No server-side session |

---

*Previous: [06 — Spring REST API](06_Spring_REST_API.md) | Next: [08 — Spring Boot Advanced](08_Spring_Boot_Advanced.md)*

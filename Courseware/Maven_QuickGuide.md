# Maven — Quick Guide
> Windows 11 + Eclipse | Java Full Stack Training

---

## What is Maven?

Maven is a **build automation and dependency management tool** for Java projects. Instead of manually downloading JAR files and adding them to your project, you tell Maven what you need — it fetches, manages, and wires everything together.

> 🔎 **Analogy:** Maven is like a shopping list app. You write down what you need (`pom.xml`), and Maven goes to the store (Maven Central repository), buys the exact items, and stocks your kitchen (project classpath) — automatically.

**What Maven does for you:**
- Downloads and manages library dependencies (no more manual JAR hunting)
- Compiles your source code
- Runs tests
- Packages your app into a JAR or WAR
- Manages multi-module projects
- Standardises project structure across teams

---

## Installation on Windows 11

### Step 1 — Verify Java is installed
Open Command Prompt and run:
```
java -version
```
You should see something like `java version "17.x.x"`. If not, install JDK first.

### Step 2 — Download Maven
1. Go to https://maven.apache.org/download.cgi
2. Download the **Binary zip archive** — e.g. `apache-maven-3.9.x-bin.zip`
3. Extract it to a simple path like `C:\Maven\apache-maven-3.9.x`

### Step 3 — Set Environment Variables
1. Open **Start → Search → "Edit the system environment variables"**
2. Click **Environment Variables**
3. Under **System Variables**, click **New**:
   - Variable name: `MAVEN_HOME`
   - Variable value: `C:\Maven\apache-maven-3.9.x`
4. Find the `Path` variable → click **Edit → New** → add:
   ```
   %MAVEN_HOME%\bin
   ```
5. Click OK on all dialogs

### Step 4 — Verify Installation
Open a **new** Command Prompt window and run:
```
mvn -version
```
Expected output:
```
Apache Maven 3.9.x
Maven home: C:\Maven\apache-maven-3.9.x
Java version: 17.x.x
```

---

## Maven in Eclipse

Eclipse (with m2e plugin) has Maven support built in. Modern versions of Eclipse include m2e by default.

### Check if m2e is installed
Go to **Help → About Eclipse IDE → Installation Details** and look for `m2e - Maven Integration for Eclipse`.

### Create a Maven Project in Eclipse
1. **File → New → Maven Project**
2. Check **"Create a simple project (skip archetype selection)"** for a blank project
   - OR leave unchecked to pick an archetype (template) — use `maven-archetype-quickstart` for a simple Java app
3. Fill in:
   - **Group Id:** `com.yourcompany.training` *(reverse domain — like a package name for the project)*
   - **Artifact Id:** `my-first-maven-app` *(your project name)*
   - **Version:** `1.0-SNAPSHOT` *(default — means in development)*
4. Click Finish

### Import an Existing Maven Project
1. **File → Import → Maven → Existing Maven Projects**
2. Browse to the folder containing `pom.xml`
3. Click Finish — Eclipse reads `pom.xml` and sets everything up

---

## Project Structure

Maven enforces a **standard directory layout**. Every Maven project looks like this:

```
my-first-maven-app/
│
├── pom.xml                          ← The heart of the project
│
└── src/
    ├── main/
    │   ├── java/                    ← Your application source code
    │   │   └── com/training/App.java
    │   └── resources/               ← Config files, properties, XML
    │
    └── test/
        ├── java/                    ← Your test classes
        │   └── com/training/AppTest.java
        └── resources/               ← Test config files
```

> 💡 **Tip:** Never put source code outside `src/main/java`. Maven only compiles what's in the standard directories.

---

## The POM File (`pom.xml`)

POM = **Project Object Model**. This single XML file is the complete description of your project.

### Minimal `pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <!-- Project identity -->
    <groupId>com.training</groupId>
    <artifactId>my-first-maven-app</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <!-- Java version -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <!-- Dependencies -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.2.0</version>
        </dependency>
    </dependencies>

</project>
```

### Key POM Elements

| Element | Purpose | Example |
|---|---|---|
| `groupId` | Organisation / company identifier | `com.acme.training` |
| `artifactId` | Project / module name | `employee-service` |
| `version` | Project version | `1.0.0`, `2.1-SNAPSHOT` |
| `packaging` | Output type | `jar`, `war`, `pom` |
| `dependencies` | Libraries your project needs | Spring, JUnit, Lombok |
| `properties` | Reusable variables | Java version, dependency versions |
| `build/plugins` | Build customisations | compiler plugin, surefire |

### SNAPSHOT vs RELEASE
- `1.0-SNAPSHOT` — work in progress, can change. Maven re-downloads it periodically.
- `1.0.0` — stable release. Maven caches it permanently.

---

## Dependencies

### Adding a Dependency
Find any library on https://mvnrepository.com, copy its XML snippet, and paste inside `<dependencies>`:

```xml
<dependencies>

    <!-- MySQL JDBC Driver -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- JUnit 5 for testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>

</dependencies>
```

After saving `pom.xml`, Eclipse automatically downloads the JAR and adds it to your classpath.

---

## Dependency Scopes

Scope controls **when** a dependency is available on the classpath.

| Scope | Available At | Packaged In JAR/WAR? | Common Use |
|---|---|---|---|
| `compile` | Compile + Runtime + Test | ✅ Yes | Default. Spring, Hibernate |
| `provided` | Compile + Test only | ❌ No | Servlet API (server provides it) |
| `runtime` | Runtime + Test only | ✅ Yes | JDBC drivers |
| `test` | Test only | ❌ No | JUnit, Mockito |
| `system` | Compile only (local JAR) | ❌ No | Rare — avoid if possible |

```xml
<!-- Example: servlet-api is provided by Tomcat, don't bundle it -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

---

## Maven Build Lifecycle

Maven has three built-in lifecycles. The most used is the **default** lifecycle:

```
validate → compile → test → package → verify → install → deploy
```

| Phase | What It Does |
|---|---|
| `validate` | Checks `pom.xml` is correct |
| `compile` | Compiles `src/main/java` → `.class` files |
| `test` | Runs unit tests in `src/test/java` |
| `package` | Creates JAR or WAR in `target/` folder |
| `install` | Copies the JAR to your local Maven repository (`~/.m2`) |
| `deploy` | Uploads JAR to a remote repository (CI/CD use) |

> 💡 **Key rule:** Running a phase automatically runs **all phases before it**. Running `mvn package` will also run validate, compile, and test.

### Common Maven Commands

```bash
mvn compile              # Compile source code only
mvn test                 # Compile + run all tests
mvn package              # Compile + test + create JAR/WAR
mvn clean                # Delete the target/ folder
mvn clean package        # Clean first, then build fresh
mvn install              # Build + copy to local .m2 repo
mvn dependency:tree      # Show all dependencies and their transitive deps
mvn dependency:resolve   # Download all declared dependencies
```

### Running Maven in Eclipse
Right-click your project → **Run As →**
- `Maven build...` — enter a goal like `clean package`
- `Maven install` — shortcut for `mvn install`
- `Maven test` — shortcut for `mvn test`

---

## Maven Web Application

To create a WAR file (deployable to Tomcat):

### 1. Set packaging to `war`
```xml
<packaging>war</packaging>
```

### 2. Add Servlet dependency with `provided` scope
```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

### 3. Web project structure
```
my-web-app/
├── pom.xml
└── src/
    └── main/
        ├── java/          ← Servlets, Controllers
        ├── resources/
        └── webapp/        ← Web content (JSP, HTML, CSS)
            ├── WEB-INF/
            │   └── web.xml
            └── index.jsp
```

### 4. Build the WAR
```bash
mvn clean package
```
Output: `target/my-web-app-1.0-SNAPSHOT.war` — drop this into Tomcat's `webapps/` folder.

---

## Multi-Module Projects

Large projects are split into **modules** — each a separate Maven project with its own `pom.xml`, all tied together by a **parent POM**.

### Structure
```
parent-project/
├── pom.xml                  ← Parent POM (packaging: pom)
├── common-utils/
│   └── pom.xml              ← Module 1
├── data-access/
│   └── pom.xml              ← Module 2
└── web-app/
    └── pom.xml              ← Module 3 (depends on Module 1 & 2)
```

### Parent `pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.training</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>        <!-- Must be 'pom' for parent -->

    <modules>
        <module>common-utils</module>
        <module>data-access</module>
        <module>web-app</module>
    </modules>

    <!-- Shared dependency versions for all child modules -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Child module `pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.training</groupId>
        <artifactId>parent-project</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>common-utils</artifactId>
    <!-- groupId and version inherited from parent -->
</project>
```

### Build all modules at once
```bash
cd parent-project
mvn clean install       # Builds all modules in the right order
```

> 💡 **Tip:** `dependencyManagement` in the parent defines versions centrally. Child modules declare the dependency **without a version** — they inherit it from the parent.

---

## Profiles

Profiles let you have **different build configurations** for different environments (dev, test, prod) — activated by a flag or automatically.

```xml
<profiles>

    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>   <!-- active unless overridden -->
        </activation>
        <properties>
            <db.url>jdbc:h2:mem:devdb</db.url>
        </properties>
    </profile>

    <profile>
        <id>prod</id>
        <properties>
            <db.url>jdbc:mysql://prod-server/appdb</db.url>
        </properties>
    </profile>

</profiles>
```

### Activating a profile
```bash
mvn package -P prod           # Activate prod profile
mvn package -P dev,reporting  # Activate multiple profiles
```

In Eclipse: Right-click project → **Maven → Select Maven Profiles**

---

## Local Repository

Maven caches all downloaded JARs in your **local repository** at:
```
C:\Users\<YourName>\.m2\repository\
```

- First build: Maven downloads from Maven Central → saves to `.m2`
- Subsequent builds: Maven uses the cached copy — no internet needed
- `mvn install` puts **your own project's JAR** into `.m2` so other local projects can depend on it

---

## Quick Reference Cheatsheet

```bash
# Build
mvn clean package          # Clean build, produces JAR/WAR
mvn clean install          # Build + save to local repo

# Testing
mvn test                   # Run all tests
mvn test -Dtest=MyTest     # Run a specific test class
mvn package -DskipTests    # Build without running tests

# Dependencies
mvn dependency:tree        # Show full dependency tree
mvn dependency:resolve     # Force download all deps

# Multi-module
mvn install -pl data-access           # Build only one module
mvn install -pl web-app -am           # Build module + its dependencies

# Profiles
mvn package -P prod                   # Activate a profile

# Info
mvn help:effective-pom                # Show the full resolved POM
mvn versions:display-dependency-updates  # Check for newer versions
```

---

*Maven Quick Guide | Java Full Stack Training | Windows 11 + Eclipse*

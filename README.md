# Online Exam System

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/Spring%20Security-6.x-green?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security 6">
  <img src="https://img.shields.io/badge/JPA-Hibernate-red?style=flat-square" alt="JPA / Hibernate">
  <img src="https://img.shields.io/badge/Database-PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven">
</p>

<p align="center">
  A secure web-based examination platform built with Spring Boot, Spring Security, Thymeleaf, Bootstrap 5, and PostgreSQL.
</p>

<p align="center">
  <b>Exam Management · Timed Online Tests · Automatic Evaluation · Result Tracking</b>
</p>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Default Administrator Account](#default-administrator-account)
- [Common Commands](#common-commands)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Online Exam System is a web application that gives administrators a central place to create and manage examinations, and gives students a straightforward way to take them online.

The platform covers the full examination lifecycle: exam and question management, timed assessments, automatic scoring, student account administration, and result tracking. It also serves as a practical reference implementation of Spring Boot, Spring Security, Spring Data JPA, Hibernate, PostgreSQL, and Thymeleaf working together.

---

## Features

### Administrator

- Secure authentication with role-based access control
- Dashboard with examination statistics
- Full lifecycle management of examinations (create, view, update, delete)
- Question management for each examination
- Student account management, including password resets and account deletion
- Review of student submissions and examination results
- Monitoring of examination and student performance

### Student

- Self-service registration and secure login
- Personalized dashboard
- Online examinations with a question palette for easy navigation
- Timed exams with automatic submission when time expires
- Score, percentage, and pass/fail status after submission
- Review of submitted answers and correct answers, where enabled
- Profile management, including profile picture and password change
- Access to previous examination results

> Feature availability reflects the current state of the source code.

---

## Screenshots

Screenshots are stored in the `screenshots/` directory.

### Front Page

<p align="center">
  <img src="./screenshots/Front%20Page.png" alt="Front page" width="90%">
</p>

### Admin Dashboard

<p align="center">
  <img src="./screenshots/Admin%20Page.png" alt="Admin dashboard" width="90%">
</p>

### Exam Management

<p align="center">
  <img src="./screenshots/Questions%20Page.png" alt="Exam management page" width="90%">
</p>

### Question Management

<p align="center">
  <img src="./screenshots/ManageQuestion%20Page.png" alt="Question management page" width="90%">
</p>

### Student Dashboard

<p align="center">
  <img src="./screenshots/Student%20Page.png" alt="Student dashboard" width="90%">
</p>

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Backend Framework | Spring Boot 3.x |
| Security | Spring Security 6 |
| Templating | Thymeleaf, HTML5, CSS3 |
| UI Framework | Bootstrap 5 |
| Charts | Chart.js (optional) |
| ORM | Hibernate / Spring Data JPA |
| Database | PostgreSQL 14+ |
| Build Tool | Maven |
| File Storage | Local file system |
| Version Control | Git / GitHub |

---

## Project Structure

The layout below is representative; package names and folders may vary slightly in the repository.

```text
online-exam-system/
├── src/
│   ├── main/
│   │   ├── java/com/example/exam/
│   │   │   ├── OnlineExamApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   ├── config/
│   │   │   └── security/
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── images/
│   │       └── application.properties
│   └── test/
├── screenshots/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## Getting Started

### Prerequisites

| Requirement | Notes |
|---|---|
| JDK 17 or higher | Verify with `java -version` |
| PostgreSQL 14 or higher | Local install or Docker; verify with `psql --version` |
| Maven | Optional; the included Maven Wrapper is recommended |
| Git | Required to clone the repository |
| IDE | VS Code, IntelliJ IDEA, or Eclipse |

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/Abhinaav2003/online-exam-system-master-main.git
cd online-exam-system-master-main
```

> If `pom.xml` is located in a nested directory, change into that directory before running Maven commands.

**2. Set up PostgreSQL**

Create a database for the application. Using `psql`:

```bash
psql -U postgres -c "CREATE DATABASE examdb;"
```

Or start a disposable PostgreSQL instance with Docker:

```bash
docker run --name exam-postgres \
  -e POSTGRES_DB=examdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:16
```

**3. Set database credentials**

The application reads credentials from environment variables (see [Configuration](#configuration)).

Windows (PowerShell):

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_password"
```

macOS / Linux:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

**4. Build the application**

Windows (PowerShell):

```powershell
.\mvnw.cmd clean package -DskipTests
```

macOS / Linux:

```bash
./mvnw clean package -DskipTests
```

Using a locally installed Maven:

```bash
mvn clean package -DskipTests
```

**5. Run the application**

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, run `OnlineExamApplication.java` directly from your IDE.

**6. Open the application**

Navigate to **http://localhost:8080**. The port may differ if `server.port` is changed in `application.properties`.

---

## Configuration

The application uses PostgreSQL for persistence. Tables are created and updated automatically by Hibernate on startup.

### Maven dependency

Remove the H2 dependency from `pom.xml` and add the PostgreSQL JDBC driver:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Example `application.properties`

Use this as a reference and adapt it to your project's dependencies and existing settings.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/examdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

server.port=8080
```

### Environment variables

| Variable | Description | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL user | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | none (required) |

### Removing H2 from an existing setup

If you are migrating from the earlier H2 configuration:

- Delete the `com.h2database:h2` dependency from `pom.xml`.
- Remove all `spring.h2.console.*` properties from `application.properties`.
- Remove any `/h2-console/**` matchers and `frameOptions` overrides from the Spring Security configuration.
- Delete the local `data/` directory containing the old H2 database files, if you no longer need them.

Existing H2 data is not migrated automatically. Export it first if you need to keep it.

### Configuration Notes

- The database named in `spring.datasource.url` must exist before the application starts; Hibernate creates tables, not the database itself.
- `spring.jpa.hibernate.ddl-auto=update` is suited to development. Use a managed migration tool such as Flyway or Liquibase in production.
- Do not commit real credentials to version control. Externalize secrets through environment variables or a secrets manager.
- For production, create a dedicated database user with only the privileges the application needs instead of using the `postgres` superuser.

---

## Default Administrator Account

The application may create an administrator account on startup. Check `OnlineExamApplication.java` and the security configuration for the actual credentials.

| Field | Example Value |
|---|---|
| Username | `admin` |
| Password | `adminpass` |

These values are illustrative and have not been independently verified against the current source.

> **Security notice:** Never deploy an application with default administrator credentials. Set a strong, unique password before any public or production deployment.

---

## Common Commands

| Task | Command (PowerShell) |
|---|---|
| Clean previous build | `.\mvnw.cmd clean` |
| Build without tests | `.\mvnw.cmd clean package -DskipTests` |
| Start the application | `.\mvnw.cmd spring-boot:run` |
| Stop the application | `Ctrl + C` in the running terminal |

**Full clean rebuild**

```powershell
Remove-Item -Recurse -Force target -ErrorAction SilentlyContinue
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd spring-boot:run
```

This removes generated build artifacts in `target/` and rebuilds the project. It does not touch the PostgreSQL database or uploaded profile pictures.

**Useful PostgreSQL commands**

| Task | Command |
|---|---|
| Connect to the database | `psql -U postgres -d examdb` |
| List tables | `\dt` (inside `psql`) |
| Back up the database | `pg_dump -U postgres examdb > examdb_backup.sql` |
| Restore from backup | `psql -U postgres -d examdb < examdb_backup.sql` |
| Stop the Docker container | `docker stop exam-postgres` |
| Start the Docker container | `docker start exam-postgres` |

---

## Roadmap

Planned and potential enhancements:

- [ ] Question banks with randomized question and option ordering
- [ ] Support for additional question types (multi-select, short answer)
- [ ] Exam scheduling and access windows
- [ ] Exportable reports (PDF / CSV)
- [ ] Email notifications and password recovery
- [ ] Schema versioning with Flyway or Liquibase
- [ ] Docker Compose setup (application + PostgreSQL) and CI/CD pipeline

---

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes with clear, descriptive messages
4. Push the branch and open a pull request

---

## License

Add your license information here (for example, MIT) and include a corresponding `LICENSE` file in the repository.

---

## Authors

This project was designed and developed by:

- **Abhinav Tiwari**
- **Anshu Rajput**

---

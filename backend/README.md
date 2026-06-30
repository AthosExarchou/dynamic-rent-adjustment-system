# Dynamic Rent Adjustment System (DRAS) - Backend
[![](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![](https://img.shields.io/badge/status-active-brightgreen?style=for-the-badge)]()
[![](https://img.shields.io/github/license/AthosExarchou/dynamic-rent-adjustment-system.svg?style=for-the-badge)](https://github.com/AthosExarchou/dynamic-rent-adjustment-system/blob/master/LICENSE)

This repository contains the backend service for the **Dynamic Rent Adjustment System (DRAS)**,
developed as part of an undergraduate diploma thesis in Computer Science.

The backend exposes a RESTful API responsible for business logic, persistence, authentication, authorization,
and integration with external services.

## Contents
- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Security & Authentication](#security--authentication)
- [Requirements](#requirements)
- [Installation & Run](#installation--run)
- [Configuration](#configuration)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Author](#author)
- [License](#license)

## Overview

This repository contains the Spring Boot backend for the Dynamic Rent Adjustment System (DRAS).

The backend provides a RESTful API that manages rental listings, user accounts, applications, role-based authorization,
external listing imports, and business workflows while persisting data in a relational database.

## System Architecture

The backend follows a layered architecture that separates presentation, business logic, persistence, and domain models:
- **Controllers (`/controllers`)**: RESTful endpoints (`@RestController`) that process HTTP requests and return standard JSON payloads (`ResponseEntity`).
- **Services (`/services`)**: Business logic layer divided into domain-specific services and cross-domain application workflows.
- **Repositories (`/repositories`)**: Spring Data JPA interfaces for database interaction.
- **Entities (`/entities`)**: JPA mapped objects representing the underlying relational database schema.
- **DTOs (`/dto`)**: Data Transfer Objects define request and response payloads, providing validation and preventing direct exposure of persistence models.

## Security & Authentication

- **Role-Based Access Control (RBAC)**: Method-level security (`@Secured`, `@PreAuthorize`) controls access to endpoints based on role requirements (`USER`, `OWNER`, `TENANT`, `ADMIN`).
- **API Security**: Endpoints under `/api/**` bypass CSRF for stateless frontend integration.
- **CORS Configuration**: Configured to accept requests from the frontend client (default `http://localhost:5173`).
- **Error Handling**: Exception interception mapping errors to JSON HTTP responses (e.g., 400, 401, 403, 404, 500).

## Requirements
Ensure you have the following installed to build and run the backend:
- **Java**: JDK 21+
- **Maven**: 3.8+
- **Database**: PostgreSQL (or any relational DB for development)

## Installation & Run
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Build the project and download dependencies:
   ```bash
   mvn clean install
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
4. The server runs on `http://localhost:8080/`.

## Configuration

Update `src/main/resources/application.properties` with your database and external service credentials:

```properties
spring.application.name=DRAS-Backend
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/dras_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# Email Configuration (for internal notifications)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

## Testing

Unit tests are implemented using JUnit 5 and Mockito to verify service-layer business logic and REST endpoint behavior.

To run the test suite:
```bash
mvn clean test
```

## Project Structure

```text
src/main/java/gr/hua/dit/dras/
│
├── config/                   # Global configuration classes (Security, CORS, WebMvc)
├── controllers/              # REST controllers exposing API endpoints
├── dto/                      # Data Transfer Objects for secure request/response bodies
├── entities/                 # Database schema models (JPA Entities)
├── model/                    # Application Enums and simple POJOs
├── repositories/             # Spring Data JPA repository interfaces
├── services/                 # Domain logic and transactional workflows
│   ├── application/
│   ├── domain/
│   └── integration/
└── DrasApplication.java      # Main Spring Boot Entry Point
```

## Technologies

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- JUnit 5
- Mockito

## Responsibilities

The backend is responsible for:

- Authentication and authorization
- User and role management
- Listing lifecycle management
- Rental application workflows
- External listing import
- Email notifications
- Data persistence
- REST API exposure

## Author

- **Name**: Exarchou Athos
- **Student ID**: it2022134
- **Email**: athosexarhou@gmail.com

## License
This project is licensed under the **MIT License**.

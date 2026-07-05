# Dynamic Rent Adjustment System (DRAS)
[![](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactjs.org/)
[![](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![](https://img.shields.io/badge/status-active-brightgreen?style=for-the-badge)]()
[![](https://img.shields.io/github/license/AthosExarchou/dynamic-rent-adjustment-system.svg?style=for-the-badge)](https://github.com/AthosExarchou/dynamic-rent-adjustment-system/blob/master/LICENSE)

Welcome to the **Dynamic Rent Adjustment System (DRAS)**, a web application for real estate property management, connecting owners with tenants, and processing rental applications.

> **Academic Project:** This project is being developed as an undergraduate thesis in the
[Department of Informatics and Telecommunications at Harokopio University of Athens](https://dit.hua.gr).
It follows a layered backend architecture, feature-based frontend architecture, RESTful APIs, and role-based access control.

## Contents
- [Overview](#overview)
- [Academic Context](#academic-context)
- [System Architecture](#system-architecture)
- [Core Features](#core-features)
- [Role Management](#role-management)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage Guide](#usage-guide)
- [Infrastructure & Deployment](#infrastructure--deployment)
- [Author](#author)
- [License](#license)

## Overview
DRAS is a full-stack rental management platform developed as an undergraduate thesis project.
It manages rental properties, tenant applications, and administrative workflows.

## Academic Context

The Dynamic Rent Adjustment System (DRAS) is the implementation component of an undergraduate diploma thesis.

The objective of the project is to design and develop a modern rental management platform that
demonstrates the application of contemporary software engineering principles, including:

- Layered backend architecture
- RESTful API design
- Role-based access control (RBAC)
- Feature-based React architecture
- Relational database design
- Authentication and authorization
- External data integration

The project is an academic research contribution and a demonstration of building a full-stack web application.

## System Architecture

The project consists of two distinct applications communicating over HTTP via a RESTful API:

1. **[Backend (Spring Boot API)](./backend)**: Java REST API handling data persistence, authentication, role-based authorization, and business logic.
2. **[Frontend (React SPA)](./frontend)**: Single Page Application built with Vite and React.

## Core Features
- **Listing Management**: Owners can create, update, and track real estate properties with metrics (price per m², property type, amenities).
- **Application Workflow**: Tenants can apply for properties, and owners can review these applications.
- **Dynamic Roles**: Automatic role assignment (Owner, Tenant) based on user activity and profile creation.
- **Admin Oversight**: Administrative workflows for user management and manual listing approvals.
- **Frontend UI**: User interface compatible with desktop, tablet, and mobile devices.

## Role Management

| Role    | Description                                                |
|---------|------------------------------------------------------------|
| USER    | Assigned by default upon account registration.             |
| OWNER   | Assigned automatically when a user creates an owner profile to list a property. |
| TENANT  | Assigned automatically when a user creates a tenant profile to apply for a rental. |
| ADMIN   | Full system access; oversees approvals, user roles, and platform integrity. |

## Project Structure

```text
/
├── backend/                  # Spring Boot REST API Service
│   ├── src/main/java/        # Java source code and domain logic
│   └── src/main/resources/   # Application properties and configurations
│
└── frontend/                 # React Single Page Application
    ├── src/features/         # Domain-driven feature modules (Auth, Listings, etc.)
    ├── src/shared/           # Centralized API client, shared hooks, and UI components
    └── src/app/              # Global routing and context providers
```

## Requirements
To run this application, ensure you have the following installed:
- **Java**: JDK 21+
- **Node.js**: v18+
- **Maven**: 3.8+
- **Database**: PostgreSQL (or any relational DB for development)

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/AthosExarchou/dynamic-rent-adjustment-system.git
   ```
2. For the **Backend**:
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```
3. For the **Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
4. Access the web application in your browser at `http://localhost:5173/`

## Configuration

### Backend (`backend/src/main/resources/application.properties`)
Configure your database and email credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dras_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### Frontend (`frontend/.env.local`)
Configure the backend API URL:
```properties
VITE_API_BASE_URL=http://localhost:8080/api
```

## Usage Guide

### For Tenants
1. Create an account and log in.
2. Visit the **Listings** page to view available apartments.
3. Click **Apply** on a listing and complete your Tenant Profile to submit the application.

### For Owners
1. Log in to your account.
2. Navigate to **Listings** and click **Submit Property**.
3. Fill out the property details and your Owner Profile.
4. Once the listing is approved by an Admin, manage incoming tenant applications from your dashboard.

### For Admins
1. Log in with an Admin account.
2. Review and approve pending property listings.
3. Manage users and assign or revoke roles as necessary.

## Infrastructure & Deployment

Detailed instructions for deploying the Dynamic Rent Adjustment System (including Docker, Vagrant, and Jenkins
configurations, as well as troubleshooting tips for VirtualBox Secure Boot) have been moved to their own dedicated documentation.

**[Read the Infrastructure Guide](infrastructure/README.md)**

## Author

- **Name**: Exarchou Athos
- **Student ID**: it2022134
- **Email**: athosexarhou@gmail.com

## License

This project is licensed under the **MIT License**.

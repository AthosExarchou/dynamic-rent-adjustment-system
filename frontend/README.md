# Dynamic Rent Adjustment System (DRAS) - Frontend
[![](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/HTML5)
[![](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
[![](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactjs.org/)
[![](https://img.shields.io/badge/status-active-brightgreen?style=for-the-badge)]()
[![](https://img.shields.io/github/license/AthosExarchou/dynamic-rent-adjustment-system.svg?style=for-the-badge)](https://github.com/AthosExarchou/dynamic-rent-adjustment-system/blob/master/LICENSE)

Frontend application for the **Dynamic Rent Adjustment System (DRAS)**,
developed as part of an undergraduate diploma thesis in Computer Science.

This application provides the user interface for the DRAS platform and communicates with
the Spring Boot backend through a RESTful API.

## Contents
- [Overview](#overview)
- [System Architecture](#system-architecture)
- [UI Features](#ui-features)
- [Technologies](#technologies)
- [Requirements](#requirements)
- [Backend Dependency](#backend-dependency)
- [Installation & Run](#installation--run)
- [Available Scripts](#available-scripts)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Author](#author)
- [License](#license)

## Overview

This repository contains the React Single Page Application (SPA) for the Dynamic Rent Adjustment System (DRAS).

The frontend is responsible for presenting rental listings, user dashboards, application workflows, administrative interfaces, and authentication while delegating all business logic and persistence to the Spring Boot backend through REST APIs.

## System Architecture

The frontend follows a feature-based architecture inspired by Domain-Driven Design (DDD), where source code is organized around business capabilities rather than technical layers. The codebase is organized by business domains:
- **Admin**: System-wide administrative workflows (approvals, users, external imports).
- **Auth**: Authentication context, session management, and login/registration workflows.
- **Listings**: Property listings, filtering, and application logic.
- **Owners**: Owner registration and property management dashboards.
- **Tenants**: Renter profiles and application tracking.
- **Users**: Standard user profiles and account management.
- **Public**: Static landing pages, contact forms, and legal documents.

All cross-cutting concerns (API clients, generic UI components, custom hooks, global constants) are located in the `shared` directory to prevent circular dependencies.

## UI Features

- React-based Single Page Application (SPA)
- Responsive layouts
- Client-side routing
- Modular component architecture
- Domain-driven project organization

## Technologies

- **React**: The core UI library.
- **Vite**: Vite provides the development server and production build tooling.
- **React Router**: For declarative client-side routing.
- **Context API**: Handles global state (e.g., authentication).
- **Fetch API**: Powers the centralized HTTP client.
- **CSS Modules**: Scopes CSS locally to prevent style collisions.

## Requirements
To run the frontend, ensure you have the following installed:
- **Node.js**: v18+
- **NPM**: v9+ (or Yarn equivalent)

## Backend Dependency

The frontend depends on the DRAS Spring Boot backend.

The backend must be running before the frontend can communicate with the REST API.

See the root project README for full backend installation instructions.

## Installation & Run
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install all required dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Access the application in your browser at `http://localhost:5173/`.

## Available Scripts

| Command | Description |
|----------|-------------|
| `npm run dev` | Starts the development server |
| `npm run build` | Builds the production bundle |
| `npm run preview` | Serves the production build locally |
| `npm run lint` | Runs the oxlint static analysis tool |

## Configuration

The application uses Vite environment variables. Copy the `.env.example` file to `.env.local` to safely customize the backend API URL:

```bash
cp .env.example .env.local
```

### `.env.local` Content
```properties
VITE_API_BASE_URL=http://localhost:8080/api
```

## Project Structure

```text
src/
├── app/              # Application bootstrap and routing
├── features/         # Feature modules grouped by business domain
├── shared/           # Shared infrastructure and reusable components
├── assets/           # Static frontend assets
├── config/           # Environment configuration
├── App.jsx           # Main React entry component
├── main.jsx          # React DOM mounting point
└── index.css         # Global CSS variables, resets, and typography
```

## Author

- **Name**: Exarchou Athos
- **Student ID**: it2022134
- **Email**: athosexarhou@gmail.com

## License
This project is licensed under the **MIT License**.

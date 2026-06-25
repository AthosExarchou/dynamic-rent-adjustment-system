# Dynamic Rent Adjustment System - Frontend

This is the frontend application for the Dynamic Rent Adjustment System (DRAS).
It is a Single Page Application (SPA) built with React and Vite.

## Architecture

The frontend follows a **Domain-Driven Design (DDD)** architecture that mirrors the backend domains:
- **Admin**: System-wide administrative workflows (approvals, users, external imports).
- **Auth**: Authentication context, models, and workflows.
- **Listings**: Property listings and dynamic rent adjustments.
- **Owners**: Property owner management.
- **Tenants**: Renter/tenant applications and management.
- **Users**: Standard user profiles and account management.
- **Public**: Static landing pages, contact, and legal documents.

All cross-cutting concerns (API client, UI components, hooks, constants) are located in the `shared` directory.

## Development Setup

1. Copy `.env.example` to `.env.local` and set the backend URL:
   ```bash
   cp .env.example .env.local
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```

## Tech Stack
- **Framework**: React 19
- **Bundler**: Vite
- **HTTP Client**: Native `fetch` with centralized wrapper (`shared/api/client.js`)

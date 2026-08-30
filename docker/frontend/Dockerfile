# Build stage: install dependencies and compile the React application.
FROM node:20-alpine AS builder

WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build

# Artifact stage: the image contains only the compiled frontend assets.
# Docker Compose mounts /app/dist into the shared frontend_dist named volume,
# which nginx then serves directly. This container exits after population.
FROM alpine:3.20

COPY --from=builder /app/dist /app/dist

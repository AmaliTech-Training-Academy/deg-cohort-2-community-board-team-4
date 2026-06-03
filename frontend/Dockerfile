# syntax=docker/dockerfile:1


# ---------- Stage 1: build ----------
FROM node:18-alpine AS build
WORKDIR /app

# Backend API URL, baked in at build time (per environment). Defaults to
# localhost for local/QA builds; CI passes --build-arg API_URL=<env secret>.
ARG API_URL=http://localhost:8080/api

COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
# Inject the API URL into the prod environment file (replaces __API_URL__),
# then build. Angular uses 'ng build' (prod config) via the package.json script.
RUN sed -i "s|__API_URL__|${API_URL}|g" src/environments/environment.prod.ts
RUN npm run build

# ---------- Stage 2: runtime ----------
FROM node:18-alpine
WORKDIR /app

RUN npm install -g serve@14

# FIX FOR ANGULAR: 
# Copy from dist/community-board-ui/browser into the runtime folder
COPY --from=build /app/dist/community-board-ui/browser ./build

USER node
EXPOSE 3000

CMD ["serve", "-s", "build", "-l", "3000"]

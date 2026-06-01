# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM node:18-alpine AS build
WORKDIR /app

# Dependencies first — cached unless package files change.
# Uses npm ci when a lockfile is present (reproducible), else falls back to npm install.
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

# Then the source, then build the static bundle.
COPY . .
RUN npm run build

# ---------- Stage 2: runtime ----------
# Serve the static build with a tiny static-file server (no nginx).
FROM node:18-alpine
WORKDIR /app

# Install the static server globally (as root), then drop to the non-root user to run.
RUN npm install -g serve@14

# Only the built assets are needed at runtime.
COPY --from=build /app/build ./build

# 'node' is a built-in non-root user (uid 1000) in the official Node image.
USER node
EXPOSE 3000

# -s = single-page-app mode (always serve index.html for client-side routes).
CMD ["serve", "-s", "build", "-l", "3000"]

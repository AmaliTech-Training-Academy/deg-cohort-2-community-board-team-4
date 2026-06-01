# syntax=docker/dockerfile:1


# ---------- Stage 1: build ----------
FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
# Angular uses 'ng build' via the build script in package.json
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

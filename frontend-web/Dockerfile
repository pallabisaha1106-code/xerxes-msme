# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
# .env.production is picked up automatically by `vite build` (production
# mode) via COPY . . below, so VITE_API_BASE_URL gets baked into the
# built JS the same way it does for a local `npm run build`.
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime stage -------------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

ENV PORT=8080
EXPOSE 8080

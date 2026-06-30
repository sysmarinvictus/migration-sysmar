# syntax=docker/dockerfile:1
# Receituário frontend — React/TS built with Vite, served as static files by nginx. nginx also reverse-
# proxies /api and /auth to the backend so the SPA is same-origin (no CORS needed for the app itself; the
# backend's CORS still gates any direct cross-origin callers).

# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# OFFLINE build: orval reads the COMMITTED ./openapi.json (not a running backend), so gen:api needs no
# network/backend. Regenerate the client here so the image always matches the committed spec, then build.
RUN npm run gen:api && npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1

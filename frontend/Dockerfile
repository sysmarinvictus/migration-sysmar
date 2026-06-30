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
# PREREQUISITE: the typed API client (src/lib/generated/) is a BUILD INPUT. It is git-ignored and orval
# generates it from a RUNNING backend (npm run gen:api → localhost:8080/v3/api-docs), so it cannot be
# regenerated inside this offline build. Run `npm run gen:api` against the backend BEFORE building this
# image (the generated files are then present in the build context — .dockerignore does not exclude them).
# See DEPLOY.md "Frontend image" + the follow-up to commit a static OpenAPI spec for fully reproducible builds.
RUN test -d src/lib/generated || { echo "ERROR: src/lib/generated missing — run 'npm run gen:api' first (see DEPLOY.md)"; exit 1; }
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1

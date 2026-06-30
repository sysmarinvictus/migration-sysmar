import { defineConfig } from "orval";

/**
 * Generates the typed API client + TanStack Query hooks from the COMMITTED OpenAPI doc
 * (./openapi.json) — so `npm run gen:api` (and CI / the Docker build) run OFFLINE, with no
 * running backend. The generated client is the single source of request/response types.
 *
 * Refresh the spec after an API change by running the backend exporter (writes ./openapi.json):
 *   cd ../backend && mvn -o -Dit.test=OpenApiSpecExportIT -Dopenapi.export=true \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dtest=NoneX -DfailIfNoTests=false verify
 * then `npm run gen:api` and commit ./openapi.json. See DEPLOY.md.
 */
export default defineConfig({
  receituario: {
    input: "./openapi.json",
    output: {
      mode: "tags-split",
      target: "src/lib/generated/endpoints.ts",
      schemas: "src/lib/generated/model",
      client: "react-query",
      override: {
        mutator: { path: "src/lib/apiClient.ts", name: "apiRequest" },
      },
    },
  },
});

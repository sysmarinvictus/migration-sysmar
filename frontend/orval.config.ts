import { defineConfig } from "orval";

/**
 * Generates the typed API client + TanStack Query hooks from the backend OpenAPI doc.
 * Run the backend, then `npm run gen:api`. The generated client is the single source of
 * request/response types — never hand-write fetch logic the client already provides.
 */
export default defineConfig({
  receituario: {
    input: "http://localhost:8080/v3/api-docs",
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

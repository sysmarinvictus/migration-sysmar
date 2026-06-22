import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // dev proxy to the NEW Spring Boot backend on 8090 (legacy GeneXus app owns 8080)
      "/api": "http://localhost:8090",
      "/auth": "http://localhost:8090",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test-setup.ts"],
  },
});

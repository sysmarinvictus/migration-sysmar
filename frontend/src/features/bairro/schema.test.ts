import { describe, it, expect } from "vitest";
import { bairroFormSchema } from "./schema";

describe("bairroFormSchema", () => {
  it("accepts valid nome", () => {
    expect(bairroFormSchema.safeParse({ nome: "Centro" }).success).toBe(true);
  });

  it("rejects empty nome (R2)", () => {
    expect(bairroFormSchema.safeParse({ nome: "" }).success).toBe(false);
  });

  it("rejects whitespace-only nome (R2)", () => {
    expect(bairroFormSchema.safeParse({ nome: "   " }).success).toBe(false);
  });

  it("rejects nome longer than 50 chars", () => {
    expect(bairroFormSchema.safeParse({ nome: "A".repeat(51) }).success).toBe(false);
  });

  it("accepts nome of exactly 50 chars", () => {
    expect(bairroFormSchema.safeParse({ nome: "A".repeat(50) }).success).toBe(true);
  });
});

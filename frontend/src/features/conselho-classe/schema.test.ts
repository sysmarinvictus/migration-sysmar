import { describe, expect, it } from "vitest";
import { conselhoClasseFormSchema } from "./schema";

describe("conselhoClasseFormSchema", () => {
  it("accepts a valid conselho", () => {
    const r = conselhoClasseFormSchema.safeParse({ codigo: 1, sigla: "CRM", nome: "Medicina" });
    expect(r.success).toBe(true);
  });

  it("R5: accepts null/empty sigla and nome", () => {
    const r = conselhoClasseFormSchema.safeParse({ codigo: 1 });
    expect(r.success).toBe(true);
  });

  it("R1: rejects codigo > 999", () => {
    const r = conselhoClasseFormSchema.safeParse({ codigo: 1000, sigla: "X" });
    expect(r.success).toBe(false);
  });

  it("R1: rejects missing codigo", () => {
    const r = conselhoClasseFormSchema.safeParse({ sigla: "CRM" });
    expect(r.success).toBe(false);
  });
});

import { describe, it, expect } from "vitest";
import { distritoFormSchema } from "./schema";

describe("distritoFormSchema", () => {
  it("accepts minimal valid input", () => {
    const result = distritoFormSchema.safeParse({ nome: "DS Norte" });
    expect(result.success).toBe(true);
  });

  it("rejects empty nome", () => {
    const result = distritoFormSchema.safeParse({ nome: "" });
    expect(result.success).toBe(false);
  });

  it("rejects nome exceeding 30 chars", () => {
    const result = distritoFormSchema.safeParse({ nome: "A".repeat(31) });
    expect(result.success).toBe(false);
  });

  it("rejects alpha ddd", () => {
    const result = distritoFormSchema.safeParse({ nome: "DS Norte", ddd: "AB" });
    expect(result.success).toBe(false);
  });

  it("accepts numeric ddd", () => {
    const result = distritoFormSchema.safeParse({ nome: "DS Norte", ddd: "44" });
    expect(result.success).toBe(true);
  });

  it("accepts blank ddd", () => {
    const result = distritoFormSchema.safeParse({ nome: "DS Norte", ddd: "" });
    expect(result.success).toBe(true);
  });

  it("coerces empty string numeric fields to null", () => {
    const result = distritoFormSchema.safeParse({ nome: "DS Norte", cep: "" });
    expect(result.success).toBe(true);
    if (result.success) expect(result.data.cep).toBeNull();
  });

  it("accepts all optional fields", () => {
    const result = distritoFormSchema.safeParse({
      nome: "DS Completo",
      endereco: "Rua das Flores",
      numero: 100,
      complemento: "Sala 1",
      cep: 87900000,
      ddd: "44",
      telefone: 32215000,
      fax: 32215001,
      tipoLogradouroCodigo: 1,
      bairroCodigo: 2,
    });
    expect(result.success).toBe(true);
  });
});

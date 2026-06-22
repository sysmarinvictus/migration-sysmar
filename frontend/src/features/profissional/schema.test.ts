import { describe, expect, it } from "vitest";
import { profissionalFormSchema, SITUACAO_ATIVO } from "./schema";

const base = {
  id: 4321,
  numeroCns: "123456789012345",
  situacao: SITUACAO_ATIVO,
  nome: "Dra. Ana Souza",
  exportaEsus: false,
};

describe("profissionalFormSchema", () => {
  it("accepts a valid professional", () => {
    const r = profissionalFormSchema.safeParse(base);
    expect(r.success).toBe(true);
  });

  it("R1: rejects a missing/non-positive person code", () => {
    expect(profissionalFormSchema.safeParse({ ...base, id: 0 }).success).toBe(false);
    expect(profissionalFormSchema.safeParse({ ...base, id: -5 }).success).toBe(false);
  });

  it("R3: rejects a blank CNS", () => {
    const r = profissionalFormSchema.safeParse({ ...base, numeroCns: "" });
    expect(r.success).toBe(false);
  });

  it("R4: rejects a CNS that is not 15 digits", () => {
    const r = profissionalFormSchema.safeParse({ ...base, numeroCns: "12345" });
    expect(r.success).toBe(false);
  });

  it("R4: accepts a masked 15-digit CNS (digits-only counted)", () => {
    const r = profissionalFormSchema.safeParse({ ...base, numeroCns: "123 4567 8901 2345" });
    expect(r.success).toBe(true);
  });

  it("R8/R9: rejects an invalid phone/mobile but accepts pt-BR formats", () => {
    expect(profissionalFormSchema.safeParse({ ...base, telefone: "12345" }).success).toBe(false);
    expect(profissionalFormSchema.safeParse({ ...base, celular: "99999" }).success).toBe(false);
    expect(
      profissionalFormSchema.safeParse({ ...base, telefone: "(44) 3232-1010", celular: "(44) 99999-1010" })
        .success,
    ).toBe(true);
  });

  it("R6: rejects a CPF/CNPJ that is not 11 or 14 digits when present", () => {
    expect(profissionalFormSchema.safeParse({ ...base, cpfCnpj: "123" }).success).toBe(false);
    expect(profissionalFormSchema.safeParse({ ...base, cpfCnpj: "111.444.777-35" }).success).toBe(true);
  });

  it("R3 person: rejects a blank nome", () => {
    const r = profissionalFormSchema.safeParse({ ...base, nome: "" });
    expect(r.success).toBe(false);
  });

  it("rejects an invalid situacao value", () => {
    const r = profissionalFormSchema.safeParse({ ...base, situacao: 9 });
    expect(r.success).toBe(false);
  });

  it("rejects a UF that is not 2 letters", () => {
    const r = profissionalFormSchema.safeParse({ ...base, ufConselho: "PRR" });
    expect(r.success).toBe(false);
  });
});

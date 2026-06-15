import { describe, expect, it } from "vitest";
import { tipoMedicamentoFormSchema } from "./schema";

describe("tipoMedicamentoFormSchema", () => {
  it("accepts a valid tipo", () => {
    const r = tipoMedicamentoFormSchema.safeParse({ codigo: 1, descricao: "Controlado" });
    expect(r.success).toBe(true);
  });

  it("R2: rejects blank descricao", () => {
    const r = tipoMedicamentoFormSchema.safeParse({ codigo: 1, descricao: "" });
    expect(r.success).toBe(false);
  });

  it("R1: rejects codigo > 999999", () => {
    const r = tipoMedicamentoFormSchema.safeParse({ codigo: 1000000, descricao: "Controlado" });
    expect(r.success).toBe(false);
  });
});

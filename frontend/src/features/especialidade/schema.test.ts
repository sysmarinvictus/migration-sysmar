import { describe, expect, it } from "vitest";
import { especialidadeFormSchema } from "./schema";

describe("especialidadeFormSchema", () => {
  it("R1: rejects blank nome", () => {
    const r = especialidadeFormSchema.safeParse({ codigo: 1, nome: "" });
    expect(r.success).toBe(false);
  });

  it("accepts a valid especialidade", () => {
    const r = especialidadeFormSchema.safeParse({ codigo: 1, nome: "Cardiologia" });
    expect(r.success).toBe(true);
  });

  it("R5: rejects vaga min > max", () => {
    const r = especialidadeFormSchema.safeParse({
      codigo: 1,
      nome: "Cardiologia",
      vagaMuitoUrgenteMin: 10,
      vagaMuitoUrgenteMax: 5,
    });
    expect(r.success).toBe(false);
  });
});

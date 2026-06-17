import { describe, expect, it } from "vitest";
import { posologiaFormSchema } from "./schema";

describe("posologiaFormSchema", () => {
  it("accepts a valid posologia", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "Tomar 1 comprimido de 8/8h" });
    expect(r.success).toBe(true);
  });

  it("R2: rejects blank descricao", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "" });
    expect(r.success).toBe(false);
  });

  it("accepts valid intervaloHoras at boundary (99)", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "Dose", intervaloHoras: 99 });
    expect(r.success).toBe(true);
  });

  it("rejects intervaloHoras > 99", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "Dose", intervaloHoras: 100 });
    expect(r.success).toBe(false);
  });

  it("accepts valid duracaoDias at boundary (999)", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "Dose", duracaoDias: 999 });
    expect(r.success).toBe(true);
  });

  it("rejects duracaoDias > 999", () => {
    const r = posologiaFormSchema.safeParse({ descricao: "Dose", duracaoDias: 1000 });
    expect(r.success).toBe(false);
  });
});

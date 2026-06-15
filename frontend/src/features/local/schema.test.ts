import { describe, expect, it } from "vitest";
import { localFormSchema } from "./schema";

describe("localFormSchema", () => {
  it("accepts a valid local", () => {
    const r = localFormSchema.safeParse({ codigo: 1, nome: "Centro", municipioCodigo: 4114402 });
    expect(r.success).toBe(true);
  });

  it("R2: rejects blank nome", () => {
    const r = localFormSchema.safeParse({ codigo: 1, nome: "", municipioCodigo: 4114402 });
    expect(r.success).toBe(false);
  });

  it("R3: rejects missing/zero municipio", () => {
    const r = localFormSchema.safeParse({ codigo: 1, nome: "Centro", municipioCodigo: 0 });
    expect(r.success).toBe(false);
  });

  it("R1: rejects codigo > 999999", () => {
    const r = localFormSchema.safeParse({ codigo: 1000000, nome: "Centro", municipioCodigo: 4114402 });
    expect(r.success).toBe(false);
  });
});

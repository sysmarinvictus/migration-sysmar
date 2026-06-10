import { z } from "zod";

/** Zod schema mirroring the backend Bean Validation for Especialidade (R1, R5). */
const optionalNonNegInt = z
  .union([z.coerce.number().int().nonnegative(), z.literal("").transform(() => undefined)])
  .optional();

export const especialidadeFormSchema = z
  .object({
    codigo: z.coerce.number().int().positive("Código é obrigatório"),
    nome: z.string().trim().min(1, "O nome da especialidade é obrigatório").max(50),
    situacao: z.string().max(1).optional().or(z.literal("")),
    auxiliar: z.boolean().optional(),
    cborCodigo: z.union([z.coerce.number().int(), z.literal("").transform(() => undefined)]).optional(),
    // agenda (subset shown in the form; all optional non-negative ints)
    vagaMuitoUrgenteMin: optionalNonNegInt,
    vagaMuitoUrgenteMax: optionalNonNegInt,
    vagaNormalMin: optionalNonNegInt,
    vagaNormalMax: optionalNonNegInt,
  })
  // R5: min ≤ max per tier
  .refine((d) => minLeMax(d.vagaMuitoUrgenteMin, d.vagaMuitoUrgenteMax), {
    path: ["vagaMuitoUrgenteMax"],
    message: "Vaga mínima não pode ser maior que a máxima",
  })
  .refine((d) => minLeMax(d.vagaNormalMin, d.vagaNormalMax), {
    path: ["vagaNormalMax"],
    message: "Vaga mínima não pode ser maior que a máxima",
  });

function minLeMax(min?: number, max?: number) {
  return min == null || max == null || min <= max;
}

export type EspecialidadeFormValues = z.infer<typeof especialidadeFormSchema>;

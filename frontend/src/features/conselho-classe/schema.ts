import { z } from "zod";

/** Zod schema mirroring the backend Bean Validation for Conselho de Classe (R1, R5). */
export const conselhoClasseFormSchema = z.object({
  // R1: código required, GeneXus N(3,0) → 0..999
  codigo: z.coerce
    .number({ invalid_type_error: "Código é obrigatório" })
    .int()
    .min(0, "Código deve estar entre 0 e 999")
    .max(999, "Código deve estar entre 0 e 999"),
  // R5: sigla/nome optional
  sigla: z.string().trim().max(10).optional().or(z.literal("")),
  nome: z.string().trim().max(100).optional().or(z.literal("")),
});

export type ConselhoClasseFormValues = z.infer<typeof conselhoClasseFormSchema>;

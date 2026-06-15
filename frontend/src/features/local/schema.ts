import { z } from "zod";

/** Zod schema mirroring the backend Bean Validation for Local (R1, R2, R3). */
export const localFormSchema = z.object({
  // R1: código required, GeneXus N(6,0) → 0..999999
  codigo: z.coerce
    .number({ invalid_type_error: "Código é obrigatório" })
    .int()
    .min(0, "Código deve estar entre 0 e 999999")
    .max(999999, "Código deve estar entre 0 e 999999"),
  // R2: nome required
  nome: z.string().trim().min(1, "Informe o nome do local").max(50),
  // R3: município required (non-zero)
  municipioCodigo: z.coerce
    .number({ invalid_type_error: "Informe o município" })
    .int()
    .positive("Informe o município"),
});

export type LocalFormValues = z.infer<typeof localFormSchema>;

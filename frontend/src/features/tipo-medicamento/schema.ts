import { z } from "zod";

/** Zod schema mirroring the backend Bean Validation for Tipo de Medicamento (R1, R2). */
export const tipoMedicamentoFormSchema = z.object({
  // R1: código required, GeneXus N(6,0) → 0..999999
  codigo: z.coerce
    .number({ invalid_type_error: "Código é obrigatório" })
    .int()
    .min(0, "Código deve estar entre 0 e 999999")
    .max(999999, "Código deve estar entre 0 e 999999"),
  // R2: descrição required
  descricao: z.string().trim().min(1, "Informe a descrição do tipo de medicamento").max(50),
});

export type TipoMedicamentoFormValues = z.infer<typeof tipoMedicamentoFormSchema>;

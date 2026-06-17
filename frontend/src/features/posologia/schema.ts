import { z } from "zod";

export const posologiaFormSchema = z.object({
  descricao: z.string().min(1, "Informe a descrição").max(60, "Máximo 60 caracteres"),
  internamento: z.boolean().optional(),
  quantidadeDose: z.number().positive().nullable().optional(),
  medidaDose: z.number().int().nonnegative().nullable().optional(),
  intervaloHoras: z.number().int().min(0).max(99).nullable().optional(),
  duracaoDias: z.number().int().min(0).max(999).nullable().optional(),
});

export type PosologiaFormValues = z.infer<typeof posologiaFormSchema>;

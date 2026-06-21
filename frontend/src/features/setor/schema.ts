import { z } from "zod";

export const setorSchema = z.object({
  setorCod: z.coerce.number().int().min(0).max(999999),
  nome: z.string().trim().min(1, "Nome é obrigatório").max(50),
  estocador: z.coerce.number().int().min(0).max(9).default(0),
  situacao: z.enum(["ativo", "inativo"]),
  dataInativo: z.string().nullable().optional(),
  horarioInicio: z.string().nullable().optional(),
  horarioFim: z.string().nullable().optional(),
});

export type SetorFormValues = z.infer<typeof setorSchema>;

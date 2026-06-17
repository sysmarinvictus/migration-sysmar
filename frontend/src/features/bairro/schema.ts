import { z } from "zod";

export const bairroFormSchema = z.object({
  nome: z.string().min(1, "Informe o nome do bairro").max(50, "Máximo 50 caracteres"),
});

export type BairroFormValues = z.infer<typeof bairroFormSchema>;

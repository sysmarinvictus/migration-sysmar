import { z } from "zod";

export const formaApresentacaoFormSchema = z.object({
  descricao: z.string().min(1, "Informe a Descrição da Forma de Apresentação!").max(30),
  abreviacao: z.string().min(1, "Informe a Abreviação!").max(5),
});

export type FormaApresentacaoFormValues = z.infer<typeof formaApresentacaoFormSchema>;

import { z } from "zod";

export const impedimentoFormSchema = z
  .object({
    profissionalCodigo: z
      .number({ required_error: "Informe o código do Profissional!" })
      .int()
      .positive("O código do Profissional deve ser um número positivo"),
    especialidadeCodigo: z
      .number({ required_error: "Informe o código da Especialidade!" })
      .int()
      .positive("O código da Especialidade deve ser um número positivo"),
    dataInicio: z
      .string({ required_error: "Informe a data inicial do período de Impedimento!" })
      .min(1, "Informe a data inicial do período de Impedimento!"),
    dataFim: z
      .string({ required_error: "Informe a data final do período de Impedimento!" })
      .min(1, "Informe a data final do período de Impedimento!"),
    dataCadastro: z.string().optional(),
  })
  .refine((v) => !v.dataInicio || !v.dataFim || v.dataFim >= v.dataInicio, {
    message: "A data final não pode ser anterior à data inicial",
    path: ["dataFim"],
  });

export type ImpedimentoFormValues = z.infer<typeof impedimentoFormSchema>;

import { z } from "zod";

const optInt = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().int().nullable().optional()
);

export const distritoFormSchema = z.object({
  nome: z.string().min(1, "Informe o Nome do Distrito").max(30, "Máximo 30 caracteres"),
  endereco: z.string().max(50, "Máximo 50 caracteres").optional().nullable(),
  numero: optInt,
  complemento: z.string().max(15, "Máximo 15 caracteres").optional().nullable(),
  cep: optInt,
  ddd: z
    .string()
    .max(3, "Máximo 3 caracteres")
    .refine((v) => !v || /^\d+$/.test(v), "Informe apenas Números!")
    .optional()
    .nullable(),
  telefone: optInt,
  fax: optInt,
  tipoLogradouroCodigo: optInt,
  bairroCodigo: optInt,
});

export type DistritoFormValues = z.infer<typeof distritoFormSchema>;

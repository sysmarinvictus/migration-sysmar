import { z } from "zod";

const optShort = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().int().nullable().optional()
);

const optNum = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().nullable().optional()
);

// Physical BOOLEAN columns (RemUsarPosologia/RemOmitirSaldo/RemSemRename/RemMPP) — checkbox → boolean.
const optBool = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? false : Boolean(v)),
  z.boolean().nullable().optional()
);

export const medicamentoFormSchema = z
  .object({
    nome: z.string().min(1, "Informe o Nome do Medicamento!").max(250),
    tipoMedicamentoCodigo: optShort,
    concentracao: z.string().max(150).optional().nullable(),
    dcbCodigo: z.string().max(10).optional().nullable(),
    apresentacaoCodigo: optShort,
    tipoProduto: optShort,
    situacao: optShort,
    farmaciaBasica: optShort,
    etico: optShort,
    psicotropico: optShort,
    portariaPsicotropico: z.string().max(20).optional().nullable(),
    controleEspecial: optShort,
    usarPosologia: optBool,
    omitirSaldo: optBool,
    semRename: optBool,
    valorHospitalar: optNum,
    valorUnitario: optNum,
    medicamentoPotencialmentePerigoso: optBool,
    mppEfeitos: z.string().max(1000).optional().nullable(),
    mppCancelamentoMotivo: z.string().max(300).optional().nullable(),
  })
  // R42: psicotrópico requires a portaria (mirrors the server rule for early UX feedback)
  .refine((d) => d.psicotropico !== 1 || !!(d.portariaPsicotropico && d.portariaPsicotropico.trim()), {
    path: ["portariaPsicotropico"],
    message: "Informe a Portaria do medicamento psicotrópico",
  });

export type MedicamentoFormValues = z.infer<typeof medicamentoFormSchema>;

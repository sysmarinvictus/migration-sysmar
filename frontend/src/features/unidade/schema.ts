import { z } from "zod";

const optShort = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().int().nullable().optional()
);

const optInt = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().int().nullable().optional()
);

const optLong = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? null : Number(v)),
  z.number().int().nullable().optional()
);

const PHONE_REGEX = /^\([0-9]{2}\)\s([9]{1})?[0-9]{4}-[0-9]{4}$/;

export const unidadeFormSchema = z.object({
  nome: z.string().min(1, "Informe o Nome da Unidade!").max(50),
  razaoSocial: z.string().max(50).optional().nullable(),
  cnpj: z.string().min(1, "Informe o CNPJ!").max(18),
  cep: z.string().min(1, "Informe o CEP!").max(8),
  endereco: z.string().min(1, "Informe o endereço!").max(70),
  enderecoNumero: z.string().min(1, "Informe o número do endereço!").max(10),
  enderecoComplemento: z.string().max(40).optional().nullable(),
  bairro: z.string().min(1, "Informe o Bairro!").max(70),
  telefone: z
    .string()
    .max(20)
    .refine((v) => !v || PHONE_REGEX.test(v), "Formato: (44) 3221-5000")
    .optional()
    .nullable(),
  fax: z
    .string()
    .max(20)
    .refine((v) => !v || PHONE_REGEX.test(v), "Formato: (44) 3221-5000")
    .optional()
    .nullable(),
  licencaFuncionamento: z.string().max(10).optional().nullable(),
  responsavel: z.string().max(50).optional().nullable(),
  email: z.string().max(70).email("Email inválido").optional().nullable().or(z.literal("")),
  cnes: optInt,
  bpa: optShort,
  sipni: optShort,
  orgaoEmissor: z.string().max(10).optional().nullable(),
  estrategiaFamiliar: optShort,
  psf: optShort,
  sisPreNatal: optShort,
  hiperdia: optShort,
  gestao: optShort,
  sia: z.string().max(7).optional().nullable(),
  sigla: z.string().max(6).optional().nullable(),
  situacao: optShort,
  siaSus: z.string().max(7).optional().nullable(),
  scnesId: z.string().max(20).optional().nullable(),
  exportarEsus: z.boolean().optional().nullable(),
  exportarBnafar: z.boolean().optional().nullable(),
  cadastroCns: z.boolean().optional().nullable(),
  cadastroEndereco: z.boolean().optional().nullable(),
  atendimentoSemCns: z.boolean().optional().nullable(),
  atendimentoSemEndereco: z.boolean().optional().nullable(),
  encaminhamentoFisioterapia: z.boolean().optional().nullable(),
  externo: z.boolean().optional().nullable(),
  forPesCod: optLong,
  tipoUnidadeCodigo: optInt,
  atencaoSecundaria: z.boolean().optional().nullable(),
  bloqueioPacSemCadInd: z.boolean().optional().nullable(),
  avisoVacinaAtrasada: z.boolean().optional().nullable(),
  cadastroCpf: z.boolean().optional().nullable(),
  painel: z.boolean().optional().nullable(),
  recepcaoIntermedMpp: z.boolean().optional().nullable(),
  recepcaoIntermedMppImp: z.boolean().optional().nullable(),
  baixaRemSemCns: z.boolean().optional().nullable(),
  bloqueioLancPcdAut: z.boolean().optional().nullable(),
  bloqueioDispPacExt: z.boolean().optional().nullable(),
  bloqueioAgendSolExaPacExt: z.boolean().optional().nullable(),
  municipioCodigo: optInt,
  respProfissionalCodigo: optLong,
  diretorCodigo: optLong,
  auditorCodigo: optLong,
  autorizadorCodigo: optLong,
  distritoCodigo: optShort,
});

export type UnidadeFormValues = z.infer<typeof unidadeFormSchema>;

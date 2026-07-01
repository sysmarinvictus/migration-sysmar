import { z } from "zod";

/**
 * Client-side form schema for the Pessoa cadastro (SAU_PESF). Mirrors the high-value required-field and
 * cross-field rules for fast UX feedback; the BACKEND is authoritative for the full 55-rule set (its 422
 * responses are surfaced inline). Enum option lists come from the GeneXus combos.
 */

export const SEXO_OPTIONS = [
  { value: "M", label: "Masculino" },
  { value: "F", label: "Feminino" },
];

export const COR_OPTIONS = [
  { value: 1, label: "Branca" },
  { value: 2, label: "Preta" },
  { value: 3, label: "Parda" },
  { value: 4, label: "Amarela" },
  { value: 5, label: "Indígena" },
];

export const ESTADO_CIVIL_OPTIONS = [
  { value: 1, label: "Solteiro(a)" },
  { value: 2, label: "Casado(a)" },
  { value: 3, label: "Divorciado(a)" },
  { value: 4, label: "Viúvo(a)" },
  { value: 5, label: "União estável" },
];

export const NACIONALIDADE_OPTIONS = [
  { value: 1, label: "Brasileiro(a)" },
  { value: 2, label: "Estrangeiro(a)" },
  { value: 3, label: "Naturalizado(a)" },
];

export const COR_INDIGENA = 5;
export const NAC_BRASILEIRO = 1;
export const NAC_ESTRANGEIRO = 2;
export const NAC_NATURALIZADO = 3;

const optionalStr = z.string().trim().optional().or(z.literal(""));
const codeOrNull = z
  .union([z.coerce.number().int().positive(), z.literal(""), z.null()])
  .optional();

export const pessoaFormSchema = z
  .object({
    nome: z.string().trim().min(3, "Informe o nome (com sobrenome)").refine((v) => v.includes(" "), "Informe o sobrenome"),
    nomeSocial: optionalStr,
    usaNomeSocial: z.boolean().default(false),
    nomePai: optionalStr,
    nomeMae: optionalStr,
    cpfCnpj: optionalStr,
    cns: z.string().trim().min(1, "Informe o CNS"),
    rgIe: optionalStr,
    rgUf: optionalStr,
    dataNascimento: z.string().min(1, "Informe a data de nascimento"),
    sexo: z.string().min(1, "Informe o sexo"),
    corCod: z.coerce.number().int().positive("Informe a cor/raça"),
    etniaCod: codeOrNull,
    estadoCivilCod: codeOrNull,
    nacionalidadeTipo: z.coerce.number().int().positive("Informe a nacionalidade"),
    paisCod: codeOrNull,
    municipioNascCod: codeOrNull,
    dataNaturalizacao: optionalStr,
    numeroPortaria: optionalStr,
    dataEntradaPais: optionalStr,
    cep: z.string().trim().min(1, "Informe o CEP"),
    tipoLogradouroCod: z.coerce.number().int().positive("Informe o tipo de logradouro"),
    endereco: z.string().trim().min(1, "Informe o logradouro"),
    enderecoNumero: z.string().trim().min(1, "Informe o número"),
    enderecoComplemento: optionalStr,
    bairroCod: z.coerce.number().int().positive("Informe o bairro"),
    municipioCod: z.coerce.number().int().positive("Informe o município"),
    telefone: optionalStr,
    celular: optionalStr,
    email: optionalStr,
    cboCod: optionalStr,
    observacao: optionalStr,
  })
  .superRefine((v, ctx) => {
    // R24: indígena requires etnia
    if (v.corCod === COR_INDIGENA && !v.etniaCod) {
      ctx.addIssue({ path: ["etniaCod"], code: z.ZodIssueCode.custom, message: "Informe a etnia" });
    }
    // R27/R33: brasileiro requires país + município de nascimento
    if (v.nacionalidadeTipo === NAC_BRASILEIRO) {
      if (!v.paisCod) ctx.addIssue({ path: ["paisCod"], code: z.ZodIssueCode.custom, message: "Informe o país (Brasil)" });
      if (!v.municipioNascCod)
        ctx.addIssue({ path: ["municipioNascCod"], code: z.ZodIssueCode.custom, message: "Informe o município de nascimento" });
    }
    // R30: estrangeiro requires data de entrada
    if (v.nacionalidadeTipo === NAC_ESTRANGEIRO && !v.dataEntradaPais) {
      ctx.addIssue({ path: ["dataEntradaPais"], code: z.ZodIssueCode.custom, message: "Informe a data de entrada no Brasil" });
    }
    // R31/R32: naturalizado requires data + portaria
    if (v.nacionalidadeTipo === NAC_NATURALIZADO) {
      if (!v.dataNaturalizacao)
        ctx.addIssue({ path: ["dataNaturalizacao"], code: z.ZodIssueCode.custom, message: "Informe a data de naturalização" });
      if (!v.numeroPortaria)
        ctx.addIssue({ path: ["numeroPortaria"], code: z.ZodIssueCode.custom, message: "Informe a portaria" });
    }
  });

export type PessoaFormValues = z.input<typeof pessoaFormSchema>;
export type PessoaFormOutput = z.output<typeof pessoaFormSchema>;

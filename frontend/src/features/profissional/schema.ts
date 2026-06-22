import { z } from "zod";
import { onlyDigits } from "../../lib/masks";

/**
 * Zod schema for the Profissional (SAU_PRO) create/edit form.
 *
 * Mirrors the backend Bean Validation / mined rules:
 *  - id (= SYS_PES person code) is user-entered and required; NOT auto-generated (R1).
 *  - numeroCns required (R3) and, when present, must be 15 digits (R4 — full mod-11
 *    check-digit validation is enforced server-side; the form only checks shape/length).
 *  - telefone / celular, when present, match the pt-BR phone regex (R8 / R9).
 *  - cpfCnpj, when present, must be 11 (CPF) or 14 (CNPJ) digits (R6 full check-digit
 *    validation is server-side).
 *  - situacao is the 1=ATIVO / 2=INATIVO enum (R12, default ATIVO on create).
 *
 * NOTE (v1): NO certificate/signature fields. certificadoSenha, certificado and
 * assinaturaImagem are never sent from or shown in the UI.
 */

// pt-BR phone/mobile: (NN) NNNN-NNNN or (NN) 9NNNN-NNNN  (R8/R9)
const PHONE_RE = /^\(\d{2}\)\s?9?\d{4}-\d{4}$/;

export const SITUACAO_ATIVO = 1;
export const SITUACAO_INATIVO = 2;

export const situacaoLabel = (s: number | null | undefined): string =>
  s === SITUACAO_ATIVO ? "Ativo" : s === SITUACAO_INATIVO ? "Inativo" : "—";

const optionalTrimmed = z
  .string()
  .trim()
  .optional()
  .transform((v) => (v ? v : undefined));

export const profissionalFormSchema = z.object({
  // Person code = SAU_PRO PK; required, user-entered (R1).
  id: z
    .number({ required_error: "Informe o código da Pessoa (Profissional)!", invalid_type_error: "Informe o código da Pessoa (Profissional)!" })
    .int("O código da Pessoa deve ser um número inteiro")
    .positive("O código da Pessoa deve ser um número positivo"),

  // CNS required (R3); 15 digits (R4 shape — server enforces check digit).
  numeroCns: z
    .string({ required_error: "Informe o Número do CNS!" })
    .trim()
    .min(1, "Informe o Número do CNS!")
    .refine((v) => onlyDigits(v).length === 15, {
      message: "O CNS deve conter 15 dígitos",
    }),

  numeroCr: optionalTrimmed,
  ufConselho: z
    .string()
    .trim()
    .optional()
    .transform((v) => (v ? v.toUpperCase() : undefined))
    .refine((v) => v == null || /^[A-Z]{2}$/.test(v), {
      message: "UF deve conter 2 letras",
    }),
  conselhoClasseCod: z
    .number()
    .int()
    .nonnegative()
    .optional(),
  dataInicio: optionalTrimmed,
  dataFim: optionalTrimmed,
  cnesId: optionalTrimmed,
  exportaEsus: z.boolean().default(false),
  situacao: z
    .number()
    .int()
    .refine((v) => v === SITUACAO_ATIVO || v === SITUACAO_INATIVO, {
      message: "Situação inválida",
    }),

  // Writable person sub-fields (write back to SYS_PES — R2).
  nome: z
    .string({ required_error: "Informe o Nome!" })
    .trim()
    .min(1, "Informe o Nome!")
    .max(100, "Nome muito longo"),
  cpfCnpj: z
    .string()
    .trim()
    .optional()
    .transform((v) => (v ? v : undefined))
    .refine((v) => v == null || onlyDigits(v).length === 11 || onlyDigits(v).length === 14, {
      message: "CPF deve ter 11 dígitos ou CNPJ 14 dígitos",
    }),
  telefone: z
    .string()
    .trim()
    .optional()
    .transform((v) => (v ? v : undefined))
    .refine((v) => v == null || PHONE_RE.test(v), { message: "Telefone inválido!" }),
  celular: z
    .string()
    .trim()
    .optional()
    .transform((v) => (v ? v : undefined))
    .refine((v) => v == null || PHONE_RE.test(v), { message: "Celular inválido!" }),
});

// Input type (pre-transform) drives react-hook-form; output type drives the request body.
export type ProfissionalFormValues = z.input<typeof profissionalFormSchema>;
export type ProfissionalFormOutput = z.output<typeof profissionalFormSchema>;

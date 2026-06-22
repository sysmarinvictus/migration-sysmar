import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http, type ProblemDetail } from "../../lib/apiClient";

/**
 * Profissional (SAU_PRO) API hooks. Hand-written for this slice following the project
 * convention; replaced by `npm run gen:api` (orval) once the backend OpenAPI is reachable.
 *
 * SECURITY: certificadoSenha, certificado and assinaturaImagem are NOT part of any DTO
 * here — the API never returns them and the UI never sends them (slice v1).
 */

// --- types (mirror the backend DTOs) ---

/** Row returned by the list endpoint (grid columns). */
export interface ProfissionalListItem {
  id: number;
  nome: string | null;
  numeroCns: string | null;
  numeroCr: string | null;
  ufConselho: string | null;
  conselhoClasseSigla: string | null;
  situacao: number | null;
  externo: number | null;
}

/** Full read-only detail (joins SYS_PES + SAU_CONCLA). No certificate/signature fields. */
export interface ProfissionalDetail {
  id: number;
  numeroCns: string | null;
  numeroCr: string | null;
  ufConselho: string | null;
  conselhoClasseCod: number | null;
  conselhoClasseSigla: string | null;
  conselhoClasseNome: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  cnesId: string | null;
  exportaEsus: boolean | null;
  externo: number | null;
  situacao: number | null;
  // SYS_PES person fields
  nome: string | null;
  cpfCnpj: string | null;
  rgIe: string | null;
  sexo: string | null;
  dataNascimento: string | null;
  endereco: string | null;
  telefone: string | null;
  celular: string | null;
}

export interface ProfissionalWriteRequest {
  id: number;
  numeroCns: string;
  numeroCr?: string | null;
  ufConselho?: string | null;
  conselhoClasseCod?: number | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  cnesId?: string | null;
  exportaEsus: boolean;
  situacao: number;
  // person write-back (R2)
  nome: string;
  cpfCnpj?: string | null;
  telefone?: string | null;
  celular?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LookupItem {
  id: number;
  nome: string;
}

export interface ProfissionalFilters {
  nome?: string;
  numeroCns?: string;
  situacao?: number;
  externo?: number;
}

// --- query key factory ---

export const profissionalKeys = {
  all: ["profissionais"] as const,
  list: (filters: ProfissionalFilters, page: number) =>
    [...profissionalKeys.all, "list", filters, page] as const,
  detail: (id: number) => [...profissionalKeys.all, "detail", id] as const,
  lookup: (q: string) => [...profissionalKeys.all, "lookup", q] as const,
};

// --- hooks ---

export function useProfissionais(filters: ProfissionalFilters = {}, page = 0, size = 20) {
  return useQuery<Page<ProfissionalListItem>>({
    queryKey: profissionalKeys.list(filters, page),
    queryFn: async () => {
      const params: Record<string, string> = { page: String(page), size: String(size) };
      if (filters.nome) params["nome"] = filters.nome;
      if (filters.numeroCns) params["numeroCns"] = filters.numeroCns;
      if (filters.situacao != null) params["situacao"] = String(filters.situacao);
      if (filters.externo != null) params["externo"] = String(filters.externo);
      const { data } = await http.get<Page<ProfissionalListItem>>("/api/profissionais", { params });
      return data;
    },
  });
}

export function useProfissional(id: number | null) {
  return useQuery<ProfissionalDetail>({
    queryKey: id == null ? [...profissionalKeys.all, "detail", "none"] : profissionalKeys.detail(id),
    enabled: id != null,
    queryFn: async () => (await http.get<ProfissionalDetail>(`/api/profissionais/${id}`)).data,
  });
}

export function useCreateProfissional() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ProfissionalWriteRequest) =>
      (await http.post<ProfissionalDetail>("/api/profissionais", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: profissionalKeys.all }),
  });
}

export function useUpdateProfissional(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ProfissionalWriteRequest) =>
      (await http.put<ProfissionalDetail>(`/api/profissionais/${id}`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: profissionalKeys.all });
      qc.invalidateQueries({ queryKey: profissionalKeys.detail(id) });
    },
  });
}

export function useDeleteProfissional() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await http.delete(`/api/profissionais/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: profissionalKeys.all }),
  });
}

/** Async-search lookup (prescriber selector / FK picker for other slices, e.g. SAU_RECESP). */
export function useProfissionalLookup(q: string, enabled = true) {
  return useQuery<LookupItem[]>({
    queryKey: profissionalKeys.lookup(q),
    enabled,
    queryFn: async () =>
      (await http.get<LookupItem[]>("/api/profissionais/lookup", { params: { q: q || undefined } }))
        .data,
  });
}

export type { ProblemDetail };

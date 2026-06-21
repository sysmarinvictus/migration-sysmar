import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http, type ProblemDetail } from "../../lib/apiClient";

// --- types ---

export interface Impedimento {
  codigo: number;
  dataCadastro: string | null;
  dataInicio: string;
  dataFim: string;
  profissionalCodigo: number;
  profissionalNome: string | null;
  profissionalSituacao: number | null;
  especialidadeCodigo: number | null;
  especialidadeNome: string | null;
  cboCode: string | null;
  cboDescricao: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ImpedimentoFilters {
  profissionalNome?: string;
  profissionalId?: number;
  especialidadeId?: number;
  dataInicioFrom?: string;
  dataFimAte?: string;
}

export interface ImpedimentoCreateRequest {
  dataCadastro?: string | null;
  dataInicio: string;
  dataFim: string;
  profissionalCodigo: number;
  especialidadeCodigo: number;
}

export interface ImpedimentoUpdateRequest {
  dataCadastro: string;
  dataInicio: string;
  dataFim: string;
  profissionalCodigo: number;
  especialidadeCodigo: number;
}

// --- query key factory ---

export const impedimentoKeys = {
  all: ["impedimentos"] as const,
  list: (filters: ImpedimentoFilters, page: number) =>
    [...impedimentoKeys.all, "list", filters, page] as const,
  detail: (codigo: number) => [...impedimentoKeys.all, "detail", codigo] as const,
};

// --- hooks ---

export function useImpedimentos(filters: ImpedimentoFilters = {}, page = 0, size = 20) {
  return useQuery<Page<Impedimento>>({
    queryKey: impedimentoKeys.list(filters, page),
    queryFn: async () => {
      const params: Record<string, string> = { page: String(page), size: String(size) };
      if (filters.profissionalNome) params["profissionalNome"] = filters.profissionalNome;
      if (filters.profissionalId) params["profissionalId"] = String(filters.profissionalId);
      if (filters.especialidadeId) params["especialidadeId"] = String(filters.especialidadeId);
      if (filters.dataInicioFrom) params["dataInicioFrom"] = filters.dataInicioFrom;
      if (filters.dataFimAte) params["dataFimAte"] = filters.dataFimAte;
      const { data } = await http.get<Page<Impedimento>>("/api/impedimentos", { params });
      return data;
    },
  });
}

export function useImpedimento(codigo: number) {
  return useQuery<Impedimento>({
    queryKey: impedimentoKeys.detail(codigo),
    queryFn: async () => (await http.get<Impedimento>(`/api/impedimentos/${codigo}`)).data,
    enabled: !!codigo,
  });
}

export function useCreateImpedimento() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (data: ImpedimentoCreateRequest) =>
      (await http.post<Impedimento>("/api/impedimentos", data)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: impedimentoKeys.all }),
  });
}

export function useUpdateImpedimento(codigo: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (data: ImpedimentoUpdateRequest) =>
      (await http.put<Impedimento>(`/api/impedimentos/${codigo}`, data)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: impedimentoKeys.all });
      qc.invalidateQueries({ queryKey: impedimentoKeys.detail(codigo) });
    },
  });
}

export function useDeleteImpedimento() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (codigo: number) => {
      await http.delete(`/api/impedimentos/${codigo}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: impedimentoKeys.all }),
  });
}

export type { ProblemDetail };

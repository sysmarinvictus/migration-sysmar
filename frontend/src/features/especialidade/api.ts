import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/**
 * Especialidade API hooks. Hand-written here for the reference slice; once the backend is running,
 * `npm run gen:api` (orval) generates these from OpenAPI and this file is replaced by the import.
 */

export interface AgendaParametros {
  estagnadoMuitoUrgente?: number | null;
  estagnadoUrgente?: number | null;
  estagnadoPrioritario?: number | null;
  estagnadoNormal?: number | null;
  tempoMaxMuitoUrgente?: number | null;
  tempoMaxUrgente?: number | null;
  tempoMaxPrioritario?: number | null;
  tempoMaxNormal?: number | null;
  vagaMuitoUrgenteMin?: number | null;
  vagaMuitoUrgenteMax?: number | null;
  vagaUrgenteMin?: number | null;
  vagaUrgenteMax?: number | null;
  vagaPrioritarioMin?: number | null;
  vagaPrioritarioMax?: number | null;
  vagaNormalMin?: number | null;
  vagaNormalMax?: number | null;
}

export interface Especialidade {
  codigo: number;
  nome: string;
  situacao?: string | null;
  auxiliar?: boolean | null;
  cborCodigo?: number | null;
  cborDescricao?: string | null;
  agenda?: AgendaParametros | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const especialidadeKeys = {
  all: ["especialidades"] as const,
  list: (nome: string, page: number) => ["especialidades", "list", nome, page] as const,
  detail: (codigo: number) => ["especialidades", "detail", codigo] as const,
};

export function useEspecialidades(nome: string, page = 0, size = 20) {
  return useQuery({
    queryKey: especialidadeKeys.list(nome, page),
    queryFn: async () => {
      const { data } = await http.get<Page<Especialidade>>("/api/especialidades", {
        params: { nome: nome || undefined, page, size },
      });
      return data;
    },
  });
}

export function useEspecialidade(codigo: number | null) {
  return useQuery({
    queryKey: codigo == null ? ["especialidades", "detail", "none"] : especialidadeKeys.detail(codigo),
    enabled: codigo != null,
    queryFn: async () => (await http.get<Especialidade>(`/api/especialidades/${codigo}`)).data,
  });
}

export function useCreateEspecialidade() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<Especialidade>) =>
      (await http.post<Especialidade>("/api/especialidades", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: especialidadeKeys.all }),
  });
}

export function useUpdateEspecialidade(codigo: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<Especialidade>) =>
      (await http.put<Especialidade>(`/api/especialidades/${codigo}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: especialidadeKeys.all }),
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/**
 * Conselho de Classe API hooks. Hand-written for this slice; once the backend is running,
 * `npm run gen:api` (orval) generates these from OpenAPI and this file is replaced by the import.
 */

export interface ConselhoClasse {
  codigo: number;
  sigla?: string | null;
  nome?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const conselhoClasseKeys = {
  all: ["conselhos-classe"] as const,
  list: (q: string, page: number) => ["conselhos-classe", "list", q, page] as const,
  detail: (codigo: number) => ["conselhos-classe", "detail", codigo] as const,
};

export function useConselhosClasse(q: string, page = 0, size = 20) {
  return useQuery({
    queryKey: conselhoClasseKeys.list(q, page),
    queryFn: async () => {
      const { data } = await http.get<Page<ConselhoClasse>>("/api/conselhos-classe", {
        params: { q: q || undefined, page, size },
      });
      return data;
    },
  });
}

export function useConselhoClasse(codigo: number | null) {
  return useQuery({
    queryKey:
      codigo == null ? ["conselhos-classe", "detail", "none"] : conselhoClasseKeys.detail(codigo),
    enabled: codigo != null,
    queryFn: async () => (await http.get<ConselhoClasse>(`/api/conselhos-classe/${codigo}`)).data,
  });
}

export function useCreateConselhoClasse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<ConselhoClasse>) =>
      (await http.post<ConselhoClasse>("/api/conselhos-classe", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: conselhoClasseKeys.all }),
  });
}

export function useUpdateConselhoClasse(codigo: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<ConselhoClasse>) =>
      (await http.put<ConselhoClasse>(`/api/conselhos-classe/${codigo}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: conselhoClasseKeys.all }),
  });
}

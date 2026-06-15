import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/**
 * Local API hooks. Hand-written for this slice; once the backend is running, `npm run gen:api`
 * (orval) generates these from OpenAPI and this file is replaced by the import.
 */

export interface Local {
  codigo: number;
  nome: string;
  municipioCodigo: number;
  municipioNome?: string | null;
  municipioUf?: string | null;
  municipioIbge?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const localKeys = {
  all: ["locais"] as const,
  list: (nome: string, page: number) => ["locais", "list", nome, page] as const,
  detail: (codigo: number) => ["locais", "detail", codigo] as const,
};

export function useLocais(nome: string, page = 0, size = 20) {
  return useQuery({
    queryKey: localKeys.list(nome, page),
    queryFn: async () => {
      const { data } = await http.get<Page<Local>>("/api/locais", {
        params: { nome: nome || undefined, page, size },
      });
      return data;
    },
  });
}

export function useLocal(codigo: number | null) {
  return useQuery({
    queryKey: codigo == null ? ["locais", "detail", "none"] : localKeys.detail(codigo),
    enabled: codigo != null,
    queryFn: async () => (await http.get<Local>(`/api/locais/${codigo}`)).data,
  });
}

export function useCreateLocal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<Local>) => (await http.post<Local>("/api/locais", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: localKeys.all }),
  });
}

export function useUpdateLocal(codigo: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<Local>) =>
      (await http.put<Local>(`/api/locais/${codigo}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: localKeys.all }),
  });
}

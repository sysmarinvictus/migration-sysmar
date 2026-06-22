import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/** Forma de Apresentação (SAU_APRREM) API hooks. Replaced by `npm run gen:api` once OpenAPI is reachable. */

export interface FormaApresentacao {
  id: number;
  descricao: string;
  abreviacao: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const formaApresentacaoKeys = {
  all: ["formas-apresentacao"] as const,
  list: (descricao: string, page: number) => ["formas-apresentacao", "list", descricao, page] as const,
  detail: (id: number) => ["formas-apresentacao", "detail", id] as const,
};

export function useFormasApresentacao(descricao: string, page = 0, size = 20) {
  return useQuery({
    queryKey: formaApresentacaoKeys.list(descricao, page),
    queryFn: async () => {
      const { data } = await http.get<Page<FormaApresentacao>>("/api/formas-apresentacao", {
        params: { descricao: descricao || undefined, page, size },
      });
      return data;
    },
  });
}

export function useFormaApresentacao(id: number | null) {
  return useQuery({
    queryKey: id == null ? ["formas-apresentacao", "detail", "none"] : formaApresentacaoKeys.detail(id),
    enabled: id != null,
    queryFn: async () => (await http.get<FormaApresentacao>(`/api/formas-apresentacao/${id}`)).data,
  });
}

export function useCreateFormaApresentacao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<FormaApresentacao>) =>
      (await http.post<FormaApresentacao>("/api/formas-apresentacao", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: formaApresentacaoKeys.all }),
  });
}

export function useUpdateFormaApresentacao(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<FormaApresentacao>) =>
      (await http.put<FormaApresentacao>(`/api/formas-apresentacao/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: formaApresentacaoKeys.all }),
  });
}

export function useDeleteFormaApresentacao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await http.delete(`/api/formas-apresentacao/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: formaApresentacaoKeys.all }),
  });
}

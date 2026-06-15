import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/**
 * Tipo de Medicamento API hooks. Hand-written for this slice; once the backend is running,
 * `npm run gen:api` (orval) generates these from OpenAPI and this file is replaced by the import.
 */

export interface TipoMedicamento {
  codigo: number;
  descricao: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const tipoMedicamentoKeys = {
  all: ["tipos-medicamento"] as const,
  list: (descricao: string, page: number) => ["tipos-medicamento", "list", descricao, page] as const,
  detail: (codigo: number) => ["tipos-medicamento", "detail", codigo] as const,
};

export function useTiposMedicamento(descricao: string, page = 0, size = 20) {
  return useQuery({
    queryKey: tipoMedicamentoKeys.list(descricao, page),
    queryFn: async () => {
      const { data } = await http.get<Page<TipoMedicamento>>("/api/tipos-medicamento", {
        params: { descricao: descricao || undefined, page, size },
      });
      return data;
    },
  });
}

export function useTipoMedicamento(codigo: number | null) {
  return useQuery({
    queryKey:
      codigo == null ? ["tipos-medicamento", "detail", "none"] : tipoMedicamentoKeys.detail(codigo),
    enabled: codigo != null,
    queryFn: async () => (await http.get<TipoMedicamento>(`/api/tipos-medicamento/${codigo}`)).data,
  });
}

export function useCreateTipoMedicamento() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<TipoMedicamento>) =>
      (await http.post<TipoMedicamento>("/api/tipos-medicamento", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: tipoMedicamentoKeys.all }),
  });
}

export function useUpdateTipoMedicamento(codigo: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Partial<TipoMedicamento>) =>
      (await http.put<TipoMedicamento>(`/api/tipos-medicamento/${codigo}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: tipoMedicamentoKeys.all }),
  });
}

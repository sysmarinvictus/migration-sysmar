import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";
import type { SetorFormValues } from "./schema";

export interface UniSetorResponse {
  uniCod: number;
  setorCod: number;
  nome: string;
  estocador: number;
  situacao: string;
  dataInativo?: string | null;
  horarioInicio?: string | null;
  horarioFim?: string | null;
  unidadeNome?: string | null;
  unidadeCnes?: number | null;
  unidadeSituacao?: number | null;
}

interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface UniSetorLookupItem {
  uniCod: number;
  setorCod: number;
  nome: string;
}

export function useSetores(unidadeId: number, nome: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (nome) params.set("nome", nome);
  return useQuery<Page<UniSetorResponse>>({
    queryKey: ["setores", unidadeId, nome, page],
    queryFn: () => http.get(`/api/unidades/${unidadeId}/setores?${params}`).then((r) => r.data),
  });
}

export function useSetor(unidadeId: number, setorId: number | null) {
  return useQuery<UniSetorResponse>({
    queryKey: ["setores", unidadeId, setorId],
    queryFn: () => http.get(`/api/unidades/${unidadeId}/setores/${setorId}`).then((r) => r.data),
    enabled: setorId != null,
  });
}

export function useCreateSetor(unidadeId: number) {
  const qc = useQueryClient();
  return useMutation<UniSetorResponse, unknown, SetorFormValues>({
    mutationFn: (body) => http.post(`/api/unidades/${unidadeId}/setores`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["setores", unidadeId] }),
  });
}

export function useUpdateSetor(unidadeId: number, setorId: number) {
  const qc = useQueryClient();
  return useMutation<UniSetorResponse, unknown, SetorFormValues>({
    mutationFn: (body) => http.put(`/api/unidades/${unidadeId}/setores/${setorId}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["setores", unidadeId] }),
  });
}

export function useDeleteSetor(unidadeId: number) {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (setorId) => http.delete(`/api/unidades/${unidadeId}/setores/${setorId}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["setores", unidadeId] }),
  });
}

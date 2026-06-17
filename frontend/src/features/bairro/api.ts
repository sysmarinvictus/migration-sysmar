import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "../../lib/apiClient";
import type { BairroFormValues } from "./schema";

export interface BairroResponse {
  codigo: number;
  nome: string;
}

interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export function useBairros(nome: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (nome) params.set("nome", nome);
  return useQuery<Page<BairroResponse>>({
    queryKey: ["bairros", nome, page],
    queryFn: () => apiClient.get(`/api/bairros?${params}`).then((r) => r.data),
  });
}

export function useBairro(codigo: number | null) {
  return useQuery<BairroResponse>({
    queryKey: ["bairros", codigo],
    queryFn: () => apiClient.get(`/api/bairros/${codigo}`).then((r) => r.data),
    enabled: codigo != null,
  });
}

export function useCreateBairro() {
  const qc = useQueryClient();
  return useMutation<BairroResponse, unknown, BairroFormValues>({
    mutationFn: (body) => apiClient.post("/api/bairros", body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["bairros"] }),
  });
}

export function useUpdateBairro(codigo: number) {
  const qc = useQueryClient();
  return useMutation<BairroResponse, unknown, BairroFormValues>({
    mutationFn: (body) => apiClient.put(`/api/bairros/${codigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["bairros"] }),
  });
}

export function useDeleteBairro() {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (codigo) => apiClient.delete(`/api/bairros/${codigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["bairros"] }),
  });
}

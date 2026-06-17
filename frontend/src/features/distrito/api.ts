import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";
import type { DistritoFormValues } from "./schema";

export interface DistritoResponse {
  codigo: number;
  nome: string;
  endereco?: string | null;
  numero?: number | null;
  complemento?: string | null;
  cep?: number | null;
  ddd?: string | null;
  telefone?: number | null;
  fax?: number | null;
  tipoLogradouroCodigo?: number | null;
  bairroCodigo?: number | null;
}

interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export function useDistritos(nome: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (nome) params.set("nome", nome);
  return useQuery<Page<DistritoResponse>>({
    queryKey: ["distritos", nome, page],
    queryFn: () => http.get(`/api/distritos?${params}`).then((r) => r.data),
  });
}

export function useDistrito(codigo: number | null) {
  return useQuery<DistritoResponse>({
    queryKey: ["distritos", codigo],
    queryFn: () => http.get(`/api/distritos/${codigo}`).then((r) => r.data),
    enabled: codigo != null,
  });
}

export function useCreateDistrito() {
  const qc = useQueryClient();
  return useMutation<DistritoResponse, unknown, DistritoFormValues>({
    mutationFn: (body) => http.post("/api/distritos", body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["distritos"] }),
  });
}

export function useUpdateDistrito(codigo: number) {
  const qc = useQueryClient();
  return useMutation<DistritoResponse, unknown, DistritoFormValues>({
    mutationFn: (body) => http.put(`/api/distritos/${codigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["distritos"] }),
  });
}

export function useDeleteDistrito() {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (codigo) => http.delete(`/api/distritos/${codigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["distritos"] }),
  });
}

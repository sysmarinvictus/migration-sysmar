import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";
import type { PosologiaFormValues } from "./schema";

export interface PosologiaResponse {
  codigo: number;
  descricao: string;
  internamento?: boolean;
  quantidadeDose?: number;
  medidaDose?: number;
  intervaloHoras?: number;
  duracaoDias?: number;
  usuarioCodigo?: number;
}

interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export function usePosologias(descricao: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (descricao) params.set("descricao", descricao);
  return useQuery<Page<PosologiaResponse>>({
    queryKey: ["posologias", descricao, page],
    queryFn: () => http.get(`/api/posologias?${params}`).then((r) => r.data),
  });
}

export function usePosologia(codigo: number | null) {
  return useQuery<PosologiaResponse>({
    queryKey: ["posologias", codigo],
    queryFn: () => http.get(`/api/posologias/${codigo}`).then((r) => r.data),
    enabled: codigo != null,
  });
}

export function useCreatePosologia() {
  const qc = useQueryClient();
  return useMutation<PosologiaResponse, unknown, PosologiaFormValues>({
    mutationFn: (body) => http.post("/api/posologias", body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posologias"] }),
  });
}

export function useUpdatePosologia(codigo: number) {
  const qc = useQueryClient();
  return useMutation<PosologiaResponse, unknown, PosologiaFormValues>({
    mutationFn: (body) => http.put(`/api/posologias/${codigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posologias"] }),
  });
}

export function useDeletePosologia() {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (codigo) => http.delete(`/api/posologias/${codigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posologias"] }),
  });
}

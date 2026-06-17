import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";
import type { UnidadeFormValues } from "./schema";

export interface UnidadeResponse {
  codigo: number;
  nome: string;
  razaoSocial?: string | null;
  cnpj: string;
  cep: string;
  endereco: string;
  enderecoNumero: string;
  enderecoComplemento?: string | null;
  bairro: string;
  telefone?: string | null;
  fax?: string | null;
  licencaFuncionamento?: string | null;
  responsavel?: string | null;
  email?: string | null;
  cnes?: number | null;
  bpa?: number | null;
  sipni?: number | null;
  orgaoEmissor?: string | null;
  estrategiaFamiliar?: number | null;
  psf?: number | null;
  sisPreNatal?: number | null;
  hiperdia?: number | null;
  gestao?: number | null;
  sia?: string | null;
  sigla?: string | null;
  situacao?: number | null;
  siaSus?: string | null;
  scnesId?: string | null;
  exportarEsus?: boolean | null;
  exportarBnafar?: boolean | null;
  externo?: boolean | null;
  municipioCodigo?: number | null;
  distritoCodigo?: number | null;
  tipoUnidadeCodigo?: number | null;
}

interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export function useUnidades(nome: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (nome) params.set("nome", nome);
  return useQuery<Page<UnidadeResponse>>({
    queryKey: ["unidades", nome, page],
    queryFn: () => http.get(`/api/unidades?${params}`).then((r) => r.data),
  });
}

export function useUnidade(codigo: number | null) {
  return useQuery<UnidadeResponse>({
    queryKey: ["unidades", codigo],
    queryFn: () => http.get(`/api/unidades/${codigo}`).then((r) => r.data),
    enabled: codigo != null,
  });
}

export function useCreateUnidade() {
  const qc = useQueryClient();
  return useMutation<UnidadeResponse, unknown, UnidadeFormValues>({
    mutationFn: (body) => http.post("/api/unidades", body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["unidades"] }),
  });
}

export function useUpdateUnidade(codigo: number) {
  const qc = useQueryClient();
  return useMutation<UnidadeResponse, unknown, UnidadeFormValues>({
    mutationFn: (body) => http.put(`/api/unidades/${codigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["unidades"] }),
  });
}

export function useDeleteUnidade() {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (codigo) => http.delete(`/api/unidades/${codigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["unidades"] }),
  });
}

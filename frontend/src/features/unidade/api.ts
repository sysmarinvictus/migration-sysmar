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
  esferaAdministrativa?: number | null;
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

export const unidadeKeys = {
  all: ["unidades"] as const,
  list: (nome: string, page: number) => ["unidades", "list", nome, page] as const,
  detail: (codigo: number) => ["unidades", "detail", codigo] as const,
  hiperdia: (uniCod: number) => ["unidades", uniCod, "hiperdia-profissionais"] as const,
  sisPreNatal: (uniCod: number) => ["unidades", uniCod, "sisprenatal-profissionais"] as const,
  nutricionistas: (uniCod: number) => ["unidades", uniCod, "nutricionistas"] as const,
  salas: (uniCod: number) => ["unidades", uniCod, "salas"] as const,
};

export function useUnidades(nome: string, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (nome) params.set("nome", nome);
  return useQuery<Page<UnidadeResponse>>({
    queryKey: unidadeKeys.list(nome, page),
    queryFn: () => http.get(`/api/unidades?${params}`).then((r) => r.data),
  });
}

export function useUnidade(codigo: number | null) {
  return useQuery<UnidadeResponse>({
    queryKey: codigo == null ? ["unidades", "detail", "none"] : unidadeKeys.detail(codigo),
    queryFn: () => http.get(`/api/unidades/${codigo}`).then((r) => r.data),
    enabled: codigo != null,
  });
}

export function useCreateUnidade() {
  const qc = useQueryClient();
  return useMutation<UnidadeResponse, unknown, UnidadeFormValues>({
    mutationFn: (body) => http.post("/api/unidades", body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.all }),
  });
}

export function useUpdateUnidade(codigo: number) {
  const qc = useQueryClient();
  return useMutation<UnidadeResponse, unknown, UnidadeFormValues>({
    mutationFn: (body) => http.put(`/api/unidades/${codigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.all }),
  });
}

export function useDeleteUnidade() {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (codigo) => http.delete(`/api/unidades/${codigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.all }),
  });
}

// ---------------------------------------------------------------------------
// Sub-resource types
// ---------------------------------------------------------------------------

export interface HiperdiaProfissional {
  uniCod: number;
  profissionalId: number;
  dataInclusao?: string | null;
  matricula?: string | null;
  cbo?: string | null;
  status?: number | null;
  dataDesativacao?: string | null;
}

export interface SisPreNatalProfissional {
  uniCod: number;
  profissionalId: number;
  especialidadeId: number;
  dataInclusao?: string | null;
  status?: number | null;
  dataDesativacao?: string | null;
}

export interface NutricionistaProfissional {
  uniCod: number;
  profissionalId: number;
  especialidadeId: number;
  dataInclusao?: string | null;
  status?: number | null;
  dataDesativacao?: string | null;
}

export interface Sala {
  uniCod: number;
  salaCodigo: number;
  nome?: string | null;
  status?: string | null;
  dataAlteracao?: string | null;
  usuarioLogin?: string | null;
}

export interface UnidadeLookupItem {
  codigo: number;
  nome: string;
  sigla?: string | null;
}

// ---------------------------------------------------------------------------
// Hiperdia profissionais
// ---------------------------------------------------------------------------

export function useHiperdiaProfissionais(uniCod: number) {
  return useQuery<HiperdiaProfissional[]>({
    queryKey: unidadeKeys.hiperdia(uniCod),
    queryFn: () =>
      http.get(`/api/unidades/${uniCod}/hiperdia-profissionais`).then((r) => r.data),
    enabled: uniCod > 0,
  });
}

export interface AddHiperdiaBody {
  profissionalId: number;
  dataInclusao?: string | null;
  status?: number | null;
  cbo?: string | null;
  matricula?: string | null;
}

export function useAddHiperdiaProfissional(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<HiperdiaProfissional, unknown, AddHiperdiaBody>({
    mutationFn: (body) =>
      http.post(`/api/unidades/${uniCod}/hiperdia-profissionais`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.hiperdia(uniCod) }),
  });
}

export function useRemoveHiperdiaProfissional(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (profId) =>
      http
        .delete(`/api/unidades/${uniCod}/hiperdia-profissionais/${profId}`)
        .then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.hiperdia(uniCod) }),
  });
}

// ---------------------------------------------------------------------------
// SisPré-Natal profissionais
// ---------------------------------------------------------------------------

export function useSisPreNatalProfissionais(uniCod: number) {
  return useQuery<SisPreNatalProfissional[]>({
    queryKey: unidadeKeys.sisPreNatal(uniCod),
    queryFn: () =>
      http.get(`/api/unidades/${uniCod}/sisprenatal-profissionais`).then((r) => r.data),
    enabled: uniCod > 0,
  });
}

export interface AddSisPreNatalBody {
  profissionalId: number;
  especialidadeId: number;
  dataInclusao?: string | null;
}

export function useAddSisPreNatalProfissional(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<SisPreNatalProfissional, unknown, AddSisPreNatalBody>({
    mutationFn: (body) =>
      http
        .post(`/api/unidades/${uniCod}/sisprenatal-profissionais`, body)
        .then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.sisPreNatal(uniCod) }),
  });
}

export interface RemoveSisPreNatalArgs {
  profId: number;
  espId: number;
}

export function useRemoveSisPreNatalProfissional(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<void, unknown, RemoveSisPreNatalArgs>({
    mutationFn: ({ profId, espId }) =>
      http
        .delete(`/api/unidades/${uniCod}/sisprenatal-profissionais/${profId}/${espId}`)
        .then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.sisPreNatal(uniCod) }),
  });
}

// ---------------------------------------------------------------------------
// Nutricionistas
// ---------------------------------------------------------------------------

export function useNutricionistas(uniCod: number) {
  return useQuery<NutricionistaProfissional[]>({
    queryKey: unidadeKeys.nutricionistas(uniCod),
    queryFn: () =>
      http.get(`/api/unidades/${uniCod}/nutricionistas`).then((r) => r.data),
    enabled: uniCod > 0,
  });
}

export interface AddNutricionistaBody {
  profissionalId: number;
  especialidadeId: number;
  dataInclusao?: string | null;
}

export function useAddNutricionista(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<NutricionistaProfissional, unknown, AddNutricionistaBody>({
    mutationFn: (body) =>
      http.post(`/api/unidades/${uniCod}/nutricionistas`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.nutricionistas(uniCod) }),
  });
}

export interface RemoveNutricionistaArgs {
  profId: number;
  espId: number;
}

export function useRemoveNutricionista(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<void, unknown, RemoveNutricionistaArgs>({
    mutationFn: ({ profId, espId }) =>
      http
        .delete(`/api/unidades/${uniCod}/nutricionistas/${profId}/${espId}`)
        .then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.nutricionistas(uniCod) }),
  });
}

// ---------------------------------------------------------------------------
// Salas
// ---------------------------------------------------------------------------

export function useSalas(uniCod: number) {
  return useQuery<Sala[]>({
    queryKey: unidadeKeys.salas(uniCod),
    queryFn: () => http.get(`/api/unidades/${uniCod}/salas`).then((r) => r.data),
    enabled: uniCod > 0,
  });
}

export interface AddSalaBody {
  salaCodigo: number;
  nome?: string | null;
  status?: string | null;
}

export function useAddSala(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<Sala, unknown, AddSalaBody>({
    mutationFn: (body) =>
      http.post(`/api/unidades/${uniCod}/salas`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.salas(uniCod) }),
  });
}

export interface UpdateSalaBody {
  nome?: string | null;
  status?: string | null;
}

export function useUpdateSala(uniCod: number, salaCodigo: number) {
  const qc = useQueryClient();
  return useMutation<Sala, unknown, UpdateSalaBody>({
    mutationFn: (body) =>
      http.put(`/api/unidades/${uniCod}/salas/${salaCodigo}`, body).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.salas(uniCod) }),
  });
}

export function useDeleteSala(uniCod: number) {
  const qc = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (salaCodigo) =>
      http.delete(`/api/unidades/${uniCod}/salas/${salaCodigo}`).then(() => {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: unidadeKeys.salas(uniCod) }),
  });
}

// ---------------------------------------------------------------------------
// Lookup (FK picker)
// ---------------------------------------------------------------------------

export function useUnidadeLookup(q: string) {
  return useQuery<UnidadeLookupItem[]>({
    queryKey: ["unidades", "lookup", q],
    queryFn: () =>
      http
        .get("/api/unidades/lookup", { params: q ? { q } : {} })
        .then((r) => r.data),
    enabled: q.length >= 1,
    staleTime: 30_000,
  });
}

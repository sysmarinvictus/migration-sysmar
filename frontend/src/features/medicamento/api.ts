import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../lib/apiClient";

/**
 * Medicamento (SAU_REM) API hooks. Hand-written for this slice; replaced by `npm run gen:api`
 * (orval) once the backend OpenAPI is reachable.
 */

export interface Medicamento {
  id: number;
  nome: string;
  tipoMedicamentoCodigo: number | null;
  dcbCodigo: string | null;
  renameCodigo: string | null;
  renameAtualCodigo: string | null;
  apresentacaoCodigo: number | null;
  obmCodigo: string | null;
  tipoProduto: number | null;
  concentracao: string | null;
  farmaciaBasica: number | null;
  psicotropico: number | null;
  controleEspecial: number | null;
  etico: number | null;
  valorHospitalar: number | null;
  valorUnitario: number | null;
  semRename: boolean | null;
  portariaPsicotropico: string | null;
  situacao: number | null;
  omitirSaldo: boolean | null;
  usarPosologia: boolean | null;
  medicamentoPotencialmentePerigoso: boolean | null;
  mppEfeitos: string | null;
  mppCancelamentoMotivo: string | null;
  mppCancelamentoData: string | null;
  usuarioLogin: string | null;
  renameDescricao: string | null;
  posologiaCount: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LookupItem {
  id: number;
  nome: string;
}

export const medicamentoKeys = {
  all: ["medicamentos"] as const,
  list: (nome: string, page: number) => ["medicamentos", "list", nome, page] as const,
  detail: (id: number) => ["medicamentos", "detail", id] as const,
};

export function useMedicamentos(nome: string, page = 0, size = 20) {
  return useQuery({
    queryKey: medicamentoKeys.list(nome, page),
    queryFn: async () => {
      const { data } = await http.get<Page<Medicamento>>("/api/medicamentos", {
        params: { nome: nome || undefined, page, size },
      });
      return data;
    },
  });
}

export function useMedicamento(id: number | null) {
  return useQuery({
    queryKey: id == null ? ["medicamentos", "detail", "none"] : medicamentoKeys.detail(id),
    enabled: id != null,
    queryFn: async () => (await http.get<Medicamento>(`/api/medicamentos/${id}`)).data,
  });
}

export function useCreateMedicamento() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Record<string, unknown>) =>
      (await http.post<Medicamento>("/api/medicamentos", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: medicamentoKeys.all }),
  });
}

export function useUpdateMedicamento(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: Record<string, unknown>) =>
      (await http.put<Medicamento>(`/api/medicamentos/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: medicamentoKeys.all }),
  });
}

export function useDeleteMedicamento() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await http.delete(`/api/medicamentos/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: medicamentoKeys.all }),
  });
}

export function useMedicamentoLookup(q: string) {
  return useQuery({
    queryKey: ["medicamentos", "lookup", q],
    queryFn: async () =>
      (await http.get<LookupItem[]>("/api/medicamentos/lookup", { params: { q } })).data,
  });
}

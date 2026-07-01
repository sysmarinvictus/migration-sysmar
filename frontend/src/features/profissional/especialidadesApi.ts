import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http, type ProblemDetail } from "../../lib/apiClient";

/**
 * Especialidades do Profissional (SAU_PROESP) API hooks — a sub-resource of profissional.
 * Hand-written following the project convention (replaced by orval once the OpenAPI is regenerated).
 * Endpoints: /api/profissionais/{proPesCod}/especialidades. Role: SAUDE_CADASTRO.
 */

/** One specialty of a professional (mirrors EspecialidadeDoProfissionalResponse). */
export interface EspecialidadeDoProfissional {
  profissionalId: number;
  especialidadeId: number;
  prioritario: boolean;
  situacao: number | null;
  agendaManhaQtd: number | null;
  agendaTardeQtd: number | null;
  agendaNoiteQtd: number | null;
}

export interface EspecialidadeCreateRequest {
  especialidadeId: number;
  prioritario?: boolean;
  agendaManhaQtd?: number | null;
  agendaTardeQtd?: number | null;
  agendaNoiteQtd?: number | null;
}

export interface EspecialidadeUpdateRequest {
  prioritario?: boolean;
  situacao?: number | null;
  agendaManhaQtd?: number | null;
  agendaTardeQtd?: number | null;
  agendaNoiteQtd?: number | null;
}

export const proEspKeys = {
  all: (proId: number) => ["profissionais", proId, "especialidades"] as const,
};

const base = (proId: number) => `/api/profissionais/${proId}/especialidades`;

export function useEspecialidadesDoProfissional(proId: number | null) {
  return useQuery<EspecialidadeDoProfissional[]>({
    queryKey: proId == null ? ["profissionais", "none", "especialidades"] : proEspKeys.all(proId),
    enabled: proId != null,
    queryFn: async () => (await http.get<EspecialidadeDoProfissional[]>(base(proId as number))).data,
  });
}

export function useAddEspecialidade(proId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: EspecialidadeCreateRequest) =>
      (await http.post<EspecialidadeDoProfissional>(base(proId), body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: proEspKeys.all(proId) }),
  });
}

export function useUpdateEspecialidadeDoProfissional(proId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ espCod, body }: { espCod: number; body: EspecialidadeUpdateRequest }) =>
      (await http.put<EspecialidadeDoProfissional>(`${base(proId)}/${espCod}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: proEspKeys.all(proId) }),
  });
}

export function useRemoveEspecialidade(proId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (espCod: number) => {
      await http.delete(`${base(proId)}/${espCod}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: proEspKeys.all(proId) }),
  });
}

export type { ProblemDetail };

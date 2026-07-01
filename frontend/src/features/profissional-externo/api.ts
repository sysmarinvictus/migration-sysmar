import { useMutation, useQueryClient } from "@tanstack/react-query";
import { http, type ProblemDetail } from "../../lib/apiClient";

/**
 * Profissional Externo (SAU_PESF_PROFEXT) API — a lean composite create of a person (SYS_PES) + an
 * external professional (SAU_PRO, ProExt=1). Role: SAUDE_CADASTRO. Edit/delete go through the
 * profissional (SAU_PRO) flow.
 */

export interface ProfissionalExternoCreateRequest {
  nome: string;
  cns: string;
  municipioCod: number;
  conselhoClasseCod: number;
  numeroConselho: string;
  dataFim?: string | null;
}

export interface ProfissionalExternoResponse extends ProfissionalExternoCreateRequest {
  id: number;
  dataInicio: string | null;
  situacao: number | null;
  externo: number | null;
}

export function useCreateProfissionalExterno() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ProfissionalExternoCreateRequest) =>
      (await http.post<ProfissionalExternoResponse>("/api/profissionais-externos", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profissionais"] }),
  });
}

export type { ProblemDetail };

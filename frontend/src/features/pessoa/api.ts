import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http, type ProblemDetail } from "../../lib/apiClient";

/**
 * Pessoa (SYS_PES) cadastro API — the SAU_PESF write path. Hand-written per the project convention.
 * Read/search/lookup already exist on /api/pessoas; this adds create/update/delete + the full-cadastro
 * read-back used by the edit form. Role: SAUDE_CADASTRO. No credential fields (PesSenha quarantined).
 */

/** Full cadastro payload — mirrors the backend PessoaCadastroRequest (subset the form edits). */
export interface PessoaCadastroRequest {
  nome: string;
  nomeSocial?: string | null;
  usaNomeSocial?: boolean | null;
  nomePai?: string | null;
  nomeMae?: string | null;
  nomeConjuge?: string | null;
  cpfCnpj?: string | null;
  cns: string;
  rgIe?: string | null;
  orgaoEmissorCod?: number | null;
  rgUf?: string | null;
  rgDataEmissao?: string | null;
  dataNascimento: string;
  sexo: string;
  corCod: number;
  estadoCivilCod?: number | null;
  situacaoFamiliarCod?: number | null;
  etniaCod?: number | null;
  tipoSanguineo?: string | null;
  nacionalidadeTipo: number;
  paisCod?: number | null;
  municipioNascCod?: number | null;
  dataNaturalizacao?: string | null;
  numeroPortaria?: string | null;
  dataEntradaPais?: string | null;
  cep: string;
  tipoLogradouroCod: number;
  endereco: string;
  enderecoNumero: string;
  enderecoComplemento?: string | null;
  bairroCod: number;
  municipioCod: number;
  telefone?: string | null;
  celular?: string | null;
  fax?: string | null;
  email?: string | null;
  homePage?: string | null;
  cboCod?: string | null;
  observacao?: string | null;
  gerarBpa?: number | null;
}

export interface PessoaCadastroResponse extends PessoaCadastroRequest {
  id: number;
  tipoPessoa: number | null;
  dataCadastro: string | null;
}

export const pessoaKeys = {
  all: ["pessoas"] as const,
  cadastro: (id: number) => ["pessoas", "cadastro", id] as const,
};

/** Full cadastro read-back for the edit form. */
export function usePessoaCadastro(id: number | null) {
  return useQuery<PessoaCadastroResponse>({
    queryKey: id == null ? ["pessoas", "cadastro", "none"] : pessoaKeys.cadastro(id),
    enabled: id != null,
    queryFn: async () => (await http.get<PessoaCadastroResponse>(`/api/pessoas/${id}/cadastro`)).data,
  });
}

export function useCreatePessoa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: PessoaCadastroRequest) =>
      (await http.post<PessoaCadastroResponse>("/api/pessoas", body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  });
}

export function useUpdatePessoa(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: PessoaCadastroRequest) =>
      (await http.put<PessoaCadastroResponse>(`/api/pessoas/${id}`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: pessoaKeys.all });
      qc.invalidateQueries({ queryKey: pessoaKeys.cadastro(id) });
    },
  });
}

export function useDeletePessoa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await http.delete(`/api/pessoas/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  });
}

export type { ProblemDetail };

import { useMemo, useState } from "react";
import { useAuth } from "../../lib/auth";
import { useEspecialidades } from "../especialidade/api";
import {
  useAddEspecialidade,
  useEspecialidadesDoProfissional,
  useRemoveEspecialidade,
  useUpdateEspecialidadeDoProfissional,
  type EspecialidadeDoProfissional,
} from "./especialidadesApi";

function apiError(e: unknown, fallback: string): string {
  const p = (e as { response?: { data?: { detail?: string; title?: string } } })?.response?.data;
  return p?.detail ?? p?.title ?? fallback;
}

function situacaoLabel(sit: number | null): string {
  if (sit == null) return "—";
  return sit === 1 ? "Ativo" : "Inativo";
}

/**
 * Especialidades do Profissional (SAU_PROESP) — embedded panel on the profissional detail page.
 * Replaces the GeneXus sau_proesp grid inside the professional transaction. Writes gated by
 * SAUDE_CADASTRO. Deleting a specialty that has an Impedimento is blocked by the backend (R5) and
 * the 409 is surfaced inline.
 */
export default function ProfissionalEspecialidadesPanel({ profissionalId }: { profissionalId: number }) {
  const { hasRole } = useAuth();
  const canWrite = hasRole("SAUDE_CADASTRO");

  const { data, isLoading, isError } = useEspecialidadesDoProfissional(profissionalId);
  const addMut = useAddEspecialidade(profissionalId);
  const updateMut = useUpdateEspecialidadeDoProfissional(profissionalId);
  const removeMut = useRemoveEspecialidade(profissionalId);

  const [novaEsp, setNovaEsp] = useState<string>("");
  const [novaPri, setNovaPri] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  // Especialidades available to add (first page); filter out already-linked ones.
  const { data: espPage } = useEspecialidades("", 0, 100);
  const linkedIds = useMemo(() => new Set((data ?? []).map((e) => e.especialidadeId)), [data]);
  const options = (espPage?.content ?? []).filter((e) => !linkedIds.has(e.codigo));

  function handleAdd() {
    setErro(null);
    const espCod = Number(novaEsp);
    if (!Number.isFinite(espCod) || espCod <= 0) {
      setErro("Selecione uma especialidade.");
      return;
    }
    addMut.mutate(
      { especialidadeId: espCod, prioritario: novaPri },
      {
        onSuccess: () => {
          setNovaEsp("");
          setNovaPri(false);
        },
        onError: (e) => setErro(apiError(e, "Não foi possível adicionar a especialidade.")),
      },
    );
  }

  function togglePrioritario(row: EspecialidadeDoProfissional) {
    setErro(null);
    updateMut.mutate(
      { espCod: row.especialidadeId, body: { prioritario: !row.prioritario } },
      { onError: (e) => setErro(apiError(e, "Não foi possível atualizar.")) },
    );
  }

  function toggleSituacao(row: EspecialidadeDoProfissional) {
    setErro(null);
    updateMut.mutate(
      { espCod: row.especialidadeId, body: { situacao: row.situacao === 1 ? 2 : 1 } },
      { onError: (e) => setErro(apiError(e, "Não foi possível atualizar.")) },
    );
  }

  function handleRemove(row: EspecialidadeDoProfissional) {
    setErro(null);
    if (!confirm(`Remover a especialidade #${row.especialidadeId} do profissional?`)) return;
    removeMut.mutate(row.especialidadeId, {
      onError: (e) => setErro(apiError(e, "Não foi possível remover a especialidade.")),
    });
  }

  return (
    <section className="mt-6">
      <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">Especialidades</h2>

      {erro && <p className="mb-2 text-sm text-red-600">{erro}</p>}

      {isLoading && <p className="text-sm text-gray-500">Carregando…</p>}
      {isError && <p className="text-sm text-red-600">Erro ao carregar as especialidades.</p>}

      {!isLoading && !isError && (
        <table className="w-full rounded border bg-gray-50 text-sm">
          <thead>
            <tr className="border-b text-left text-gray-600">
              <th className="px-3 py-2">Especialidade</th>
              <th className="px-3 py-2">Prioritário</th>
              <th className="px-3 py-2">Situação</th>
              <th className="px-3 py-2">Agenda (M/T/N)</th>
              {canWrite && <th className="px-3 py-2 text-right">Ações</th>}
            </tr>
          </thead>
          <tbody>
            {(data ?? []).length === 0 && (
              <tr>
                <td colSpan={canWrite ? 5 : 4} className="px-3 py-3 text-gray-500">
                  Nenhuma especialidade cadastrada.
                </td>
              </tr>
            )}
            {(data ?? []).map((row) => (
              <tr key={row.especialidadeId} className="border-b last:border-0">
                <td className="px-3 py-2">#{row.especialidadeId}</td>
                <td className="px-3 py-2">
                  {canWrite ? (
                    <input
                      type="checkbox"
                      checked={row.prioritario}
                      disabled={updateMut.isPending}
                      onChange={() => togglePrioritario(row)}
                    />
                  ) : row.prioritario ? (
                    "Sim"
                  ) : (
                    "Não"
                  )}
                </td>
                <td className="px-3 py-2">
                  {situacaoLabel(row.situacao)}
                  {canWrite && (
                    <button
                      onClick={() => toggleSituacao(row)}
                      disabled={updateMut.isPending}
                      className="ml-2 text-xs text-blue-600 hover:underline disabled:opacity-50"
                    >
                      alternar
                    </button>
                  )}
                </td>
                <td className="px-3 py-2">
                  {(row.agendaManhaQtd ?? 0)}/{(row.agendaTardeQtd ?? 0)}/{(row.agendaNoiteQtd ?? 0)}
                </td>
                {canWrite && (
                  <td className="px-3 py-2 text-right">
                    <button
                      onClick={() => handleRemove(row)}
                      disabled={removeMut.isPending}
                      className="text-xs text-red-600 hover:underline disabled:opacity-50"
                    >
                      Remover
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {canWrite && (
        <div className="mt-3 flex items-end gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-gray-600">Adicionar especialidade</span>
            <select
              value={novaEsp}
              onChange={(e) => setNovaEsp(e.target.value)}
              className="rounded border px-2 py-1.5"
            >
              <option value="">Selecione…</option>
              {options.map((e) => (
                <option key={e.codigo} value={e.codigo}>
                  #{e.codigo} — {e.nome}
                </option>
              ))}
            </select>
          </label>
          <label className="flex items-center gap-1.5 text-sm">
            <input type="checkbox" checked={novaPri} onChange={(e) => setNovaPri(e.target.checked)} />
            Prioritário
          </label>
          <button
            onClick={handleAdd}
            disabled={addMut.isPending || !novaEsp}
            className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
          >
            Adicionar
          </button>
        </div>
      )}
    </section>
  );
}

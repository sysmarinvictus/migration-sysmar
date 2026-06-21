import { useState } from "react";
import { Link } from "react-router-dom";
import type { ImpedimentoFilters } from "./api";
import { useDeleteImpedimento, useImpedimentos } from "./api";

function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

export default function ImpedimentoListPage() {
  const [filters, setFilters] = useState<ImpedimentoFilters>({});
  const [draft, setDraft] = useState({ profissionalNome: "", dataInicioFrom: "", dataFimAte: "" });
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useImpedimentos(filters, page);
  const deleteMut = useDeleteImpedimento();

  function applyFilters(e: React.FormEvent) {
    e.preventDefault();
    setFilters({
      profissionalNome: draft.profissionalNome || undefined,
      dataInicioFrom: draft.dataInicioFrom || undefined,
      dataFimAte: draft.dataFimAte || undefined,
    });
    setPage(0);
  }

  function handleDelete(codigo: number) {
    if (!confirm(`Excluir impedimento ${codigo}?`)) return;
    deleteMut.mutate(codigo);
  }

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Impedimentos do Profissional</h1>
        <Link to="/impedimentos/novo" className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700">
          Novo Impedimento
        </Link>
      </div>

      <form onSubmit={applyFilters} className="mb-4 flex flex-wrap gap-3">
        <input
          type="text"
          placeholder="Nome do Profissional"
          value={draft.profissionalNome}
          onChange={(e) => setDraft((d) => ({ ...d, profissionalNome: e.target.value }))}
          className="rounded border px-2 py-1.5 text-sm"
        />
        <input
          type="date"
          title="Data Início (de)"
          value={draft.dataInicioFrom}
          onChange={(e) => setDraft((d) => ({ ...d, dataInicioFrom: e.target.value }))}
          className="rounded border px-2 py-1.5 text-sm"
        />
        <input
          type="date"
          title="Data Fim (até)"
          value={draft.dataFimAte}
          onChange={(e) => setDraft((d) => ({ ...d, dataFimAte: e.target.value }))}
          className="rounded border px-2 py-1.5 text-sm"
        />
        <button type="submit" className="rounded bg-gray-200 px-3 py-1.5 text-sm hover:bg-gray-300">
          Filtrar
        </button>
      </form>

      {isLoading && <p className="text-sm text-gray-500">Carregando…</p>}
      {isError && <p className="text-sm text-red-600">Erro ao carregar impedimentos.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b bg-gray-50 text-left">
                <th className="px-3 py-2">Código</th>
                <th className="px-3 py-2">Profissional</th>
                <th className="px-3 py-2">Especialidade</th>
                <th className="px-3 py-2">Período</th>
                <th className="px-3 py-2">Ações</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((imp) => (
                <tr key={imp.codigo} className="border-b hover:bg-gray-50">
                  <td className="px-3 py-2">{imp.codigo}</td>
                  <td className="px-3 py-2">{imp.profissionalNome ?? imp.profissionalCodigo}</td>
                  <td className="px-3 py-2">{imp.especialidadeNome ?? imp.especialidadeCodigo}</td>
                  <td className="px-3 py-2">
                    {formatDate(imp.dataInicio)} – {formatDate(imp.dataFim)}
                  </td>
                  <td className="px-3 py-2 space-x-2">
                    <Link to={`/impedimentos/${imp.codigo}`} className="text-blue-600 hover:underline">
                      Editar
                    </Link>
                    <button
                      onClick={() => handleDelete(imp.codigo)}
                      className="text-red-600 hover:underline"
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {data.content.length === 0 && (
            <p className="mt-4 text-sm text-gray-500">Nenhum impedimento encontrado.</p>
          )}

          <div className="mt-4 flex items-center gap-3 text-sm">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded border px-3 py-1 disabled:opacity-40"
            >
              Anterior
            </button>
            <span>
              Página {data.number + 1} de {Math.max(1, data.totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= data.totalPages - 1}
              className="rounded border px-3 py-1 disabled:opacity-40"
            >
              Próxima
            </button>
          </div>
        </>
      )}
    </div>
  );
}

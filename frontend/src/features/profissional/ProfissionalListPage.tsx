import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useProfissionais, type ProfissionalFilters } from "./api";
import { situacaoLabel } from "./schema";

/** Replaces hwwsau_pro: searchable, server-paged grid of professionals. Row → detail. */
export default function ProfissionalListPage() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<ProfissionalFilters>({});
  const [draft, setDraft] = useState({ nome: "", numeroCns: "", situacao: "", externo: "" });
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useProfissionais(filters, page);

  function applyFilters(e: React.FormEvent) {
    e.preventDefault();
    setFilters({
      nome: draft.nome || undefined,
      numeroCns: draft.numeroCns || undefined,
      situacao: draft.situacao ? Number(draft.situacao) : undefined,
      externo: draft.externo ? Number(draft.externo) : undefined,
    });
    setPage(0);
  }

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Profissionais</h1>
        <Link
          to="/profissionais/novo"
          className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
        >
          Novo Profissional
        </Link>
      </div>

      <form onSubmit={applyFilters} className="mb-4 flex flex-wrap items-end gap-3">
        <label className="grid gap-1 text-sm">
          <span>Nome</span>
          <input
            type="text"
            value={draft.nome}
            onChange={(e) => setDraft((d) => ({ ...d, nome: e.target.value }))}
            className="rounded border px-2 py-1.5 text-sm"
          />
        </label>
        <label className="grid gap-1 text-sm">
          <span>Número CNS</span>
          <input
            type="text"
            inputMode="numeric"
            value={draft.numeroCns}
            onChange={(e) => setDraft((d) => ({ ...d, numeroCns: e.target.value }))}
            className="rounded border px-2 py-1.5 text-sm"
          />
        </label>
        <label className="grid gap-1 text-sm">
          <span>Situação</span>
          <select
            value={draft.situacao}
            onChange={(e) => setDraft((d) => ({ ...d, situacao: e.target.value }))}
            className="rounded border px-2 py-1.5 text-sm"
          >
            <option value="">Todas</option>
            <option value="1">Ativo</option>
            <option value="2">Inativo</option>
          </select>
        </label>
        <label className="grid gap-1 text-sm">
          <span>Externo</span>
          <select
            value={draft.externo}
            onChange={(e) => setDraft((d) => ({ ...d, externo: e.target.value }))}
            className="rounded border px-2 py-1.5 text-sm"
          >
            <option value="">Todos</option>
            <option value="0">Não</option>
            <option value="1">Sim</option>
          </select>
        </label>
        <button type="submit" className="rounded bg-gray-200 px-3 py-1.5 text-sm hover:bg-gray-300">
          Filtrar
        </button>
      </form>

      {isLoading && <p className="text-sm text-gray-500">Carregando…</p>}
      {isError && <p className="text-sm text-red-600">Erro ao carregar profissionais.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b bg-gray-50 text-left">
                <th className="px-3 py-2">Código</th>
                <th className="px-3 py-2">Nome</th>
                <th className="px-3 py-2">CNS</th>
                <th className="px-3 py-2">Registro / UF</th>
                <th className="px-3 py-2">Conselho</th>
                <th className="px-3 py-2">Situação</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((p) => (
                <tr
                  key={p.id}
                  tabIndex={0}
                  onClick={() => navigate(`/profissionais/${p.id}`)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") navigate(`/profissionais/${p.id}`);
                  }}
                  className="cursor-pointer border-b hover:bg-gray-50 focus:bg-blue-50 focus:outline-none"
                >
                  <td className="px-3 py-2">{p.id}</td>
                  <td className="px-3 py-2">{p.nome ?? "—"}</td>
                  <td className="px-3 py-2">{p.numeroCns?.trim() || "—"}</td>
                  <td className="px-3 py-2">
                    {p.numeroCr?.trim() || "—"}
                    {p.ufConselho?.trim() ? ` / ${p.ufConselho.trim()}` : ""}
                  </td>
                  <td className="px-3 py-2">{p.conselhoClasseSigla?.trim() || "—"}</td>
                  <td className="px-3 py-2">{situacaoLabel(p.situacao)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {data.content.length === 0 && (
            <p className="mt-4 text-sm text-gray-500">Nenhum profissional encontrado.</p>
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

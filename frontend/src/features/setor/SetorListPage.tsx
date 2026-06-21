import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useSetores, useDeleteSetor } from "./api";

export default function SetorListPage() {
  const { unidadeId } = useParams<{ unidadeId: string }>();
  const uniId = Number(unidadeId);
  const [nome, setNome] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useSetores(uniId, nome, page);
  const deleteMutation = useDeleteSetor(uniId);

  function handleDelete(setorCod: number, nomeSetor: string) {
    if (!window.confirm(`Excluir setor "${nomeSetor}"?`)) return;
    deleteMutation.mutate(setorCod);
  }

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Setores — Unidade {uniId}</h1>
        <Link to={`/unidades/${uniId}/setores/novo`} className="rounded bg-blue-600 px-3 py-2 text-white">
          Novo setor
        </Link>
      </header>

      <input
        aria-label="Buscar por nome"
        placeholder="Buscar por nome…"
        value={nome}
        onChange={(e) => { setNome(e.target.value); setPage(0); }}
        className="mb-4 w-full max-w-sm rounded border px-3 py-2"
      />

      {isLoading && <p>Carregando…</p>}
      {isError && <p role="alert">Falha ao carregar setores.</p>}
      {deleteMutation.isError && (
        <p role="alert" className="mb-3 text-red-600">Erro ao excluir setor.</p>
      )}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Nome</th>
                <th className="py-2">Situação</th>
                <th className="py-2">Estocador</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((s) => (
                <tr key={s.setorCod} className="border-b hover:bg-gray-50">
                  <td className="py-2">{s.setorCod}</td>
                  <td className="py-2">{s.nome}</td>
                  <td className="py-2">{s.situacao}</td>
                  <td className="py-2">{s.estocador === 1 ? "Sim" : "Não"}</td>
                  <td className="py-2 text-right flex gap-3 justify-end">
                    <Link to={`/unidades/${uniId}/setores/${s.setorCod}`} className="text-blue-600">
                      Editar
                    </Link>
                    <button onClick={() => handleDelete(s.setorCod, s.nome)} className="text-red-600">
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <nav className="mt-4 flex items-center gap-3" aria-label="Paginação">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Anterior</button>
            <span>Página {data.number + 1} de {Math.max(data.totalPages, 1)}</span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Próxima</button>
          </nav>
        </>
      )}
    </section>
  );
}

import { useState } from "react";
import { Link } from "react-router-dom";
import { useDistritos, useDeleteDistrito } from "./api";

/** Replaces the GeneXus Work-With screen for distritos sanitários. */
export default function DistritoListPage() {
  const [nome, setNome] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useDistritos(nome, page);
  const deleteMutation = useDeleteDistrito();

  function handleDelete(codigo: number, nomeDistrito: string) {
    if (!window.confirm(`Excluir "${nomeDistrito}"?`)) return;
    deleteMutation.mutate(codigo);
  }

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Distritos Sanitários</h1>
        <Link to="/distritos/novo" className="rounded bg-blue-600 px-3 py-2 text-white">
          Novo distrito
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
      {isError && <p role="alert">Falha ao carregar distritos.</p>}
      {deleteMutation.isError && (
        <p role="alert" className="mb-3 text-red-600">Erro ao excluir distrito.</p>
      )}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Nome</th>
                <th className="py-2">Endereço</th>
                <th className="py-2">DDD</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((d) => (
                <tr key={d.codigo} className="border-b hover:bg-gray-50">
                  <td className="py-2">{d.codigo}</td>
                  <td className="py-2">{d.nome}</td>
                  <td className="py-2">{d.endereco ?? "—"}</td>
                  <td className="py-2">{d.ddd ?? "—"}</td>
                  <td className="py-2 text-right flex gap-3 justify-end">
                    <Link to={`/distritos/${d.codigo}`} className="text-blue-600">
                      Editar
                    </Link>
                    <button
                      onClick={() => handleDelete(d.codigo, d.nome)}
                      className="text-red-600"
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <nav className="mt-4 flex items-center gap-3" aria-label="Paginação">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </button>
            <span>Página {data.number + 1} de {Math.max(data.totalPages, 1)}</span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
              Próxima
            </button>
          </nav>
        </>
      )}
    </section>
  );
}

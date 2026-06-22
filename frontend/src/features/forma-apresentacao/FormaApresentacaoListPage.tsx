import { useState } from "react";
import { Link } from "react-router-dom";
import { useFormasApresentacao } from "./api";

/** Replaces the GeneXus lookup/list (hpromptsau_aprrem) for formas de apresentação. */
export default function FormaApresentacaoListPage() {
  const [descricao, setDescricao] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useFormasApresentacao(descricao, page);

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Formas de Apresentação</h1>
        <Link to="/formas-apresentacao/nova" className="rounded bg-blue-600 px-3 py-2 text-white">
          Nova forma
        </Link>
      </header>

      <input
        aria-label="Buscar por descrição"
        placeholder="Buscar por descrição…"
        value={descricao}
        onChange={(e) => {
          setDescricao(e.target.value);
          setPage(0);
        }}
        className="mb-4 w-full max-w-sm rounded border px-3 py-2"
      />

      {isLoading && <p>Carregando…</p>}
      {isError && <p role="alert">Falha ao carregar formas de apresentação.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Descrição</th>
                <th className="py-2">Abreviação</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((f) => (
                <tr key={f.id} className="border-b hover:bg-gray-50">
                  <td className="py-2">{f.id}</td>
                  <td className="py-2">{f.descricao}</td>
                  <td className="py-2">{f.abreviacao}</td>
                  <td className="py-2 text-right">
                    <Link to={`/formas-apresentacao/${f.id}`} className="text-blue-600">
                      Editar
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <nav className="mt-4 flex items-center gap-3" aria-label="Paginação">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </button>
            <span>
              Página {data.number + 1} de {Math.max(data.totalPages, 1)}
            </span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
              Próxima
            </button>
          </nav>
        </>
      )}
    </section>
  );
}

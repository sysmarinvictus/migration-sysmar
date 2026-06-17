import { useState } from "react";
import { Link } from "react-router-dom";
import { usePosologias } from "./api";

/** Replaces the GeneXus Work-With screen for posologias (hpromptsau_remobs / list). */
export default function PosologiaListPage() {
  const [descricao, setDescricao] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = usePosologias(descricao, page);

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Posologias</h1>
        <Link to="/posologias/nova" className="rounded bg-blue-600 px-3 py-2 text-white">
          Nova posologia
        </Link>
      </header>

      <input
        aria-label="Buscar por descrição"
        placeholder="Buscar por descrição…"
        value={descricao}
        onChange={(e) => { setDescricao(e.target.value); setPage(0); }}
        className="mb-4 w-full max-w-sm rounded border px-3 py-2"
      />

      {isLoading && <p>Carregando…</p>}
      {isError && <p role="alert">Falha ao carregar posologias.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Descrição</th>
                <th className="py-2">Internamento</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((p) => (
                <tr key={p.codigo} className="border-b hover:bg-gray-50">
                  <td className="py-2">{p.codigo}</td>
                  <td className="py-2">{p.descricao}</td>
                  <td className="py-2">{p.internamento ? "Sim" : "—"}</td>
                  <td className="py-2 text-right">
                    <Link to={`/posologias/${p.codigo}`} className="text-blue-600">
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

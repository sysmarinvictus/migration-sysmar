import { useState } from "react";
import { Link } from "react-router-dom";
import { useConselhosClasse } from "./api";

/** Replaces the GeneXus Work-With (hpromptsau_concla / list) screen for conselhos de classe. */
export default function ConselhoClasseListPage() {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useConselhosClasse(q, page);

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Conselhos de Classe</h1>
        <Link to="/conselhos-classe/novo" className="rounded bg-blue-600 px-3 py-2 text-white">
          Novo conselho
        </Link>
      </header>

      <input
        aria-label="Buscar por sigla ou nome"
        placeholder="Buscar por sigla ou nome…"
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          setPage(0);
        }}
        className="mb-4 w-full max-w-sm rounded border px-3 py-2"
      />

      {isLoading && <p>Carregando…</p>}
      {isError && <p role="alert">Falha ao carregar conselhos de classe.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Sigla</th>
                <th className="py-2">Nome</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((c) => (
                <tr key={c.codigo} className="border-b hover:bg-gray-50">
                  <td className="py-2">{c.codigo}</td>
                  <td className="py-2">{c.sigla ?? "—"}</td>
                  <td className="py-2">{c.nome ?? "—"}</td>
                  <td className="py-2 text-right">
                    <Link to={`/conselhos-classe/${c.codigo}`} className="text-blue-600">
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

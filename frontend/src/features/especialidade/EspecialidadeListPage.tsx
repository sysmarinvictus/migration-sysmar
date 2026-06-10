import { useState } from "react";
import { Link } from "react-router-dom";
import { useEspecialidades } from "./api";

/** Replaces the GeneXus Work-With (hpromptsau_esp / list) screen for especialidades. */
export default function EspecialidadeListPage() {
  const [nome, setNome] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useEspecialidades(nome, page);

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Especialidades</h1>
        <Link to="/especialidades/nova" className="rounded bg-blue-600 px-3 py-2 text-white">
          Nova especialidade
        </Link>
      </header>

      <input
        aria-label="Buscar por nome"
        placeholder="Buscar por nome…"
        value={nome}
        onChange={(e) => {
          setNome(e.target.value);
          setPage(0);
        }}
        className="mb-4 w-full max-w-sm rounded border px-3 py-2"
      />

      {isLoading && <p>Carregando…</p>}
      {isError && <p role="alert">Falha ao carregar especialidades.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Nome</th>
                <th className="py-2">CBO</th>
                <th className="py-2">Situação</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.codigo} className="border-b hover:bg-gray-50">
                  <td className="py-2">{e.codigo}</td>
                  <td className="py-2">{e.nome}</td>
                  <td className="py-2">{e.cborDescricao ?? e.cborCodigo ?? "—"}</td>
                  <td className="py-2">{e.situacao ?? "—"}</td>
                  <td className="py-2 text-right">
                    <Link to={`/especialidades/${e.codigo}`} className="text-blue-600">
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
            <button
              disabled={page + 1 >= data.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Próxima
            </button>
          </nav>
        </>
      )}
    </section>
  );
}

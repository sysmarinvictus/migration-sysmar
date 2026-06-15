import { useState } from "react";
import { Link } from "react-router-dom";
import { useTiposMedicamento } from "./api";

/** Replaces the GeneXus Work-With (hpromptsau_tiprem / list) screen for tipos de medicamento. */
export default function TipoMedicamentoListPage() {
  const [descricao, setDescricao] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useTiposMedicamento(descricao, page);

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Tipos de Medicamento</h1>
        <Link to="/tipos-medicamento/novo" className="rounded bg-blue-600 px-3 py-2 text-white">
          Novo tipo
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
      {isError && <p role="alert">Falha ao carregar tipos de medicamento.</p>}

      {data && (
        <>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b">
                <th className="py-2">Código</th>
                <th className="py-2">Descrição</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.codigo} className="border-b hover:bg-gray-50">
                  <td className="py-2">{t.codigo}</td>
                  <td className="py-2">{t.descricao}</td>
                  <td className="py-2 text-right">
                    <Link to={`/tipos-medicamento/${t.codigo}`} className="text-blue-600">
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

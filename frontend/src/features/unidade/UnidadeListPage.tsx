import { useState } from "react";
import { Link } from "react-router-dom";
import { useUnidades, useDeleteUnidade } from "./api";

export default function UnidadeListPage() {
  const [nome, setNome] = useState("");
  const [page, setPage] = useState(0);
  const { data, isLoading } = useUnidades(nome, page);
  const del = useDeleteUnidade();

  return (
    <section className="p-6">
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-semibold">Unidades de Atendimento</h1>
        <Link to="/unidades/nova" className="ml-auto rounded bg-blue-600 px-4 py-2 text-sm text-white">
          Nova Unidade
        </Link>
      </div>

      <input
        className="input mb-4 w-full max-w-sm"
        placeholder="Filtrar por nome…"
        value={nome}
        onChange={(e) => { setNome(e.target.value); setPage(0); }}
      />

      {isLoading ? (
        <p>Carregando…</p>
      ) : (
        <>
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b bg-gray-50 text-left">
                <th className="px-3 py-2">Código</th>
                <th className="px-3 py-2">Nome</th>
                <th className="px-3 py-2">CNPJ</th>
                <th className="px-3 py-2">Situação</th>
                <th className="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((u) => (
                <tr key={u.codigo} className="border-b hover:bg-gray-50">
                  <td className="px-3 py-2">{u.codigo}</td>
                  <td className="px-3 py-2">{u.nome}</td>
                  <td className="px-3 py-2">{u.cnpj}</td>
                  <td className="px-3 py-2">{u.situacao === 1 ? "Ativado" : u.situacao === 2 ? "Desativado" : "-"}</td>
                  <td className="px-3 py-2 text-right">
                    <Link to={`/unidades/${u.codigo}`} className="mr-3 text-blue-600">Editar</Link>
                    <button
                      className="text-red-600"
                      onClick={() => { if (confirm("Excluir?")) del.mutate(u.codigo); }}
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="mt-3 flex gap-2 text-sm">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
              className="disabled:opacity-40">← Anterior</button>
            <span>Página {(data?.number ?? 0) + 1} / {data?.totalPages ?? 1}</span>
            <button disabled={page + 1 >= (data?.totalPages ?? 1)} onClick={() => setPage(p => p + 1)}
              className="disabled:opacity-40">Próxima →</button>
          </div>
        </>
      )}
    </section>
  );
}

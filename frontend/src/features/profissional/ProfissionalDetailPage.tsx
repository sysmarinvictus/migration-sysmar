import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../lib/auth";
import { useDeleteProfissional, useProfissional } from "./api";
import { situacaoLabel } from "./schema";

function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [y, m, d] = iso.split("-");
  return d && m && y ? `${d}/${m}/${y}` : iso;
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <>
      <dt className="font-medium text-gray-600">{label}</dt>
      <dd>{value ?? "—"}</dd>
    </>
  );
}

/**
 * Replaces hviewsau_pro: read-only detail. Edit/Delete are gated by role (SAU_PRO:UPD/DLT,
 * mapped to SAUDE_CADASTRO per the slice auth notes / OQ6).
 *
 * SECURITY: certificadoSenha, certificado and assinaturaImagem are intentionally absent —
 * the API never returns them and this view never displays them (slice v1).
 */
export default function ProfissionalDetailPage() {
  const { id } = useParams<{ id: string }>();
  const idNum = Number(id);
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canWrite = hasRole("SAUDE_CADASTRO");

  const { data, isLoading, isError } = useProfissional(Number.isFinite(idNum) ? idNum : null);
  const deleteMut = useDeleteProfissional();

  function handleDelete() {
    if (!data) return;
    if (!confirm(`Excluir o profissional ${data.id}?`)) return;
    deleteMut.mutate(data.id, {
      onSuccess: () => navigate("/profissionais"),
      onError: (e: unknown) => {
        const msg = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
        alert(msg ?? "Não foi possível excluir o profissional.");
      },
    });
  }

  if (isLoading) return <p className="p-6 text-sm text-gray-500">Carregando…</p>;
  if (isError || !data)
    return <p className="p-6 text-sm text-red-600">Erro ao carregar o profissional.</p>;

  const cr = data.numeroCr?.trim();
  const uf = data.ufConselho?.trim();

  return (
    <div className="p-6 max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">
          Profissional #{data.id} — {data.nome ?? "—"}
        </h1>
        <div className="flex gap-3">
          {canWrite && (
            <Link
              to={`/profissionais/${data.id}/editar`}
              className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
            >
              Editar
            </Link>
          )}
          {canWrite && (
            <button
              onClick={handleDelete}
              disabled={deleteMut.isPending}
              className="rounded border border-red-300 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50"
            >
              Excluir
            </button>
          )}
          <Link to="/profissionais" className="rounded border px-3 py-1.5 text-sm hover:bg-gray-50">
            Voltar
          </Link>
        </div>
      </div>

      <section className="mb-6">
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">Pessoa</h2>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-1.5 rounded border bg-gray-50 p-4 text-sm">
          <Row label="Código (Pessoa)" value={data.id} />
          <Row label="Nome" value={data.nome} />
          <Row label="CPF/CNPJ" value={data.cpfCnpj?.trim()} />
          <Row label="RG/IE" value={data.rgIe?.trim()} />
          <Row label="Sexo" value={data.sexo?.trim()} />
          <Row label="Nascimento" value={formatDate(data.dataNascimento)} />
          <Row label="Endereço" value={data.endereco?.trim()} />
          <Row label="Telefone" value={data.telefone?.trim()} />
          <Row label="Celular" value={data.celular?.trim()} />
        </dl>
      </section>

      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">
          Dados profissionais
        </h2>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-1.5 rounded border bg-gray-50 p-4 text-sm">
          <Row label="Número CNS" value={data.numeroCns?.trim()} />
          <Row label="Registro (CR)" value={cr} />
          <Row label="UF Conselho" value={uf} />
          <Row
            label="Conselho de Classe"
            value={
              data.conselhoClasseSigla || data.conselhoClasseNome
                ? `${data.conselhoClasseSigla?.trim() ?? ""}${data.conselhoClasseNome ? ` — ${data.conselhoClasseNome}` : ""}`
                : "—"
            }
          />
          <Row label="Início de validade" value={formatDate(data.dataInicio)} />
          <Row label="Fim de validade" value={formatDate(data.dataFim)} />
          <Row label="CNES" value={data.cnesId?.trim()} />
          <Row label="Exporta e-SUS" value={data.exportaEsus ? "Sim" : "Não"} />
          <Row label="Externo" value={data.externo ? "Sim" : "Não"} />
          <Row label="Situação" value={situacaoLabel(data.situacao)} />
        </dl>
      </section>
    </div>
  );
}

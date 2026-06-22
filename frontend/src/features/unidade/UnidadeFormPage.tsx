import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import { unidadeFormSchema, type UnidadeFormValues } from "./schema";
import {
  useCreateUnidade,
  useUnidade,
  useUpdateUnidade,
  useHiperdiaProfissionais,
  useAddHiperdiaProfissional,
  useRemoveHiperdiaProfissional,
  useSisPreNatalProfissionais,
  useAddSisPreNatalProfissional,
  useRemoveSisPreNatalProfissional,
  useNutricionistas,
  useAddNutricionista,
  useRemoveNutricionista,
  useSalas,
  useAddSala,
  useDeleteSala,
  useUpdateSala,
  type HiperdiaProfissional,
  type SisPreNatalProfissional,
  type NutricionistaProfissional,
  type Sala,
} from "./api";
import { useAuth } from "../../lib/auth";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="grid gap-1">
      <span className="text-sm">{label}</span>
      {children}
      {error && <span className="text-sm text-red-600">{error}</span>}
    </label>
  );
}

function CheckField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex items-center gap-2 text-sm">
      {children}
      {label}
    </label>
  );
}

type TabId =
  | "dados-gerais"
  | "configuracoes"
  | "hiperdia"
  | "sisprenatal"
  | "nutricionistas"
  | "salas";

const ALL_TABS: { id: TabId; label: string; editOnly?: boolean }[] = [
  { id: "dados-gerais", label: "Dados Gerais" },
  { id: "configuracoes", label: "Configurações" },
  { id: "hiperdia", label: "Hiperdia", editOnly: true },
  { id: "sisprenatal", label: "SISPRENATAL", editOnly: true },
  { id: "nutricionistas", label: "Nutricionistas", editOnly: true },
  { id: "salas", label: "Salas", editOnly: true },
];

// ---------------------------------------------------------------------------
// Sub-resource panels
// ---------------------------------------------------------------------------

function HiperdiaTab({ uniCod }: { uniCod: number }) {
  const { data: rows = [], isLoading } = useHiperdiaProfissionais(uniCod);
  const add = useAddHiperdiaProfissional(uniCod);
  const remove = useRemoveHiperdiaProfissional(uniCod);

  const [form, setForm] = useState({
    profissionalId: "",
    dataInclusao: "",
    status: "",
    cbo: "",
    matricula: "",
  });

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const profId = Number(form.profissionalId);
    if (!profId) return;
    add.mutate({
      profissionalId: profId,
      dataInclusao: form.dataInclusao || null,
      status: form.status ? Number(form.status) : null,
      cbo: form.cbo || null,
      matricula: form.matricula || null,
    });
    setForm({ profissionalId: "", dataInclusao: "", status: "", cbo: "", matricula: "" });
  }

  if (isLoading) return <p className="p-4 text-sm">Carregando...</p>;

  return (
    <div className="grid gap-4">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-3 py-2">Profissional ID</th>
            <th className="px-3 py-2">Data Inclusão</th>
            <th className="px-3 py-2">CBO</th>
            <th className="px-3 py-2">Matrícula</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2">Data Desativação</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row: HiperdiaProfissional) => (
            <tr key={row.profissionalId} className="border-b hover:bg-gray-50">
              <td className="px-3 py-2">{row.profissionalId}</td>
              <td className="px-3 py-2">{row.dataInclusao ?? "-"}</td>
              <td className="px-3 py-2">{row.cbo ?? "-"}</td>
              <td className="px-3 py-2">{row.matricula ?? "-"}</td>
              <td className="px-3 py-2">
                {row.status === 1 ? "Ativo" : row.status === 2 ? "Inativo" : "-"}
              </td>
              <td className="px-3 py-2">{row.dataDesativacao ?? "-"}</td>
              <td className="px-3 py-2 text-right">
                <button
                  type="button"
                  className="text-red-600 text-sm"
                  onClick={() => {
                    if (confirm("Remover profissional do Hiperdia?"))
                      remove.mutate(row.profissionalId);
                  }}
                >
                  Remover
                </button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr>
              <td colSpan={7} className="px-3 py-4 text-center text-gray-500">
                Nenhum profissional cadastrado.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <form onSubmit={handleAdd} className="grid grid-cols-5 gap-3 rounded border p-3">
        <legend className="col-span-5 text-sm font-medium mb-1">Adicionar profissional</legend>
        <Field label="Profissional ID *">
          <input
            type="number"
            className="input"
            value={form.profissionalId}
            onChange={(e) => setForm((f) => ({ ...f, profissionalId: e.target.value }))}
            required
          />
        </Field>
        <Field label="Data Inclusão">
          <input
            type="date"
            className="input"
            value={form.dataInclusao}
            onChange={(e) => setForm((f) => ({ ...f, dataInclusao: e.target.value }))}
          />
        </Field>
        <Field label="Status">
          <select
            className="input"
            value={form.status}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}
          >
            <option value="">--</option>
            <option value="1">Ativo</option>
            <option value="2">Inativo</option>
          </select>
        </Field>
        <Field label="CBO">
          <input
            className="input"
            maxLength={6}
            value={form.cbo}
            onChange={(e) => setForm((f) => ({ ...f, cbo: e.target.value }))}
          />
        </Field>
        <Field label="Matrícula">
          <input
            className="input"
            maxLength={20}
            value={form.matricula}
            onChange={(e) => setForm((f) => ({ ...f, matricula: e.target.value }))}
          />
        </Field>
        <div className="col-span-5 flex justify-end">
          <button
            type="submit"
            disabled={add.isPending}
            className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white disabled:opacity-50"
          >
            {add.isPending ? "Salvando..." : "Adicionar"}
          </button>
        </div>
      </form>
    </div>
  );
}

function SisPreNatalTab({ uniCod }: { uniCod: number }) {
  const { data: rows = [], isLoading } = useSisPreNatalProfissionais(uniCod);
  const add = useAddSisPreNatalProfissional(uniCod);
  const remove = useRemoveSisPreNatalProfissional(uniCod);

  const [form, setForm] = useState({
    profissionalId: "",
    especialidadeId: "",
    dataInclusao: "",
  });

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const profId = Number(form.profissionalId);
    const espId = Number(form.especialidadeId);
    if (!profId || !espId) return;
    add.mutate({
      profissionalId: profId,
      especialidadeId: espId,
      dataInclusao: form.dataInclusao || null,
    });
    setForm({ profissionalId: "", especialidadeId: "", dataInclusao: "" });
  }

  if (isLoading) return <p className="p-4 text-sm">Carregando...</p>;

  return (
    <div className="grid gap-4">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-3 py-2">Profissional ID</th>
            <th className="px-3 py-2">Especialidade ID</th>
            <th className="px-3 py-2">Data Inclusão</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row: SisPreNatalProfissional) => (
            <tr
              key={`${row.profissionalId}-${row.especialidadeId}`}
              className="border-b hover:bg-gray-50"
            >
              <td className="px-3 py-2">{row.profissionalId}</td>
              <td className="px-3 py-2">{row.especialidadeId}</td>
              <td className="px-3 py-2">{row.dataInclusao ?? "-"}</td>
              <td className="px-3 py-2">
                {row.status === 1 ? "Ativo" : row.status === 2 ? "Inativo" : "-"}
              </td>
              <td className="px-3 py-2 text-right">
                <button
                  type="button"
                  className="text-red-600 text-sm"
                  onClick={() => {
                    if (confirm("Remover profissional do SisPré-Natal?"))
                      remove.mutate({
                        profId: row.profissionalId,
                        espId: row.especialidadeId,
                      });
                  }}
                >
                  Remover
                </button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr>
              <td colSpan={5} className="px-3 py-4 text-center text-gray-500">
                Nenhum profissional cadastrado.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <form onSubmit={handleAdd} className="grid grid-cols-3 gap-3 rounded border p-3">
        <legend className="col-span-3 text-sm font-medium mb-1">Adicionar profissional</legend>
        <Field label="Profissional ID *">
          <input
            type="number"
            className="input"
            value={form.profissionalId}
            onChange={(e) => setForm((f) => ({ ...f, profissionalId: e.target.value }))}
            required
          />
        </Field>
        <Field label="Especialidade ID *">
          <input
            type="number"
            className="input"
            value={form.especialidadeId}
            onChange={(e) => setForm((f) => ({ ...f, especialidadeId: e.target.value }))}
            required
          />
        </Field>
        <Field label="Data Inclusão">
          <input
            type="date"
            className="input"
            value={form.dataInclusao}
            onChange={(e) => setForm((f) => ({ ...f, dataInclusao: e.target.value }))}
          />
        </Field>
        <div className="col-span-3 flex justify-end">
          <button
            type="submit"
            disabled={add.isPending}
            className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white disabled:opacity-50"
          >
            {add.isPending ? "Salvando..." : "Adicionar"}
          </button>
        </div>
      </form>
    </div>
  );
}

function NutricionistasTab({ uniCod }: { uniCod: number }) {
  const { data: rows = [], isLoading } = useNutricionistas(uniCod);
  const add = useAddNutricionista(uniCod);
  const remove = useRemoveNutricionista(uniCod);

  const [form, setForm] = useState({
    profissionalId: "",
    especialidadeId: "",
    dataInclusao: "",
  });

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const profId = Number(form.profissionalId);
    const espId = Number(form.especialidadeId);
    if (!profId || !espId) return;
    add.mutate({
      profissionalId: profId,
      especialidadeId: espId,
      dataInclusao: form.dataInclusao || null,
    });
    setForm({ profissionalId: "", especialidadeId: "", dataInclusao: "" });
  }

  if (isLoading) return <p className="p-4 text-sm">Carregando...</p>;

  return (
    <div className="grid gap-4">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-3 py-2">Profissional ID</th>
            <th className="px-3 py-2">Especialidade ID</th>
            <th className="px-3 py-2">Data Inclusão</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row: NutricionistaProfissional) => (
            <tr
              key={`${row.profissionalId}-${row.especialidadeId}`}
              className="border-b hover:bg-gray-50"
            >
              <td className="px-3 py-2">{row.profissionalId}</td>
              <td className="px-3 py-2">{row.especialidadeId}</td>
              <td className="px-3 py-2">{row.dataInclusao ?? "-"}</td>
              <td className="px-3 py-2">
                {row.status === 1 ? "Ativo" : row.status === 2 ? "Inativo" : "-"}
              </td>
              <td className="px-3 py-2 text-right">
                <button
                  type="button"
                  className="text-red-600 text-sm"
                  onClick={() => {
                    if (confirm("Remover nutricionista?"))
                      remove.mutate({
                        profId: row.profissionalId,
                        espId: row.especialidadeId,
                      });
                  }}
                >
                  Remover
                </button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr>
              <td colSpan={5} className="px-3 py-4 text-center text-gray-500">
                Nenhum nutricionista cadastrado.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <form onSubmit={handleAdd} className="grid grid-cols-3 gap-3 rounded border p-3">
        <legend className="col-span-3 text-sm font-medium mb-1">Adicionar nutricionista</legend>
        <Field label="Profissional ID *">
          <input
            type="number"
            className="input"
            value={form.profissionalId}
            onChange={(e) => setForm((f) => ({ ...f, profissionalId: e.target.value }))}
            required
          />
        </Field>
        <Field label="Especialidade ID *">
          <input
            type="number"
            className="input"
            value={form.especialidadeId}
            onChange={(e) => setForm((f) => ({ ...f, especialidadeId: e.target.value }))}
            required
          />
        </Field>
        <Field label="Data Inclusão">
          <input
            type="date"
            className="input"
            value={form.dataInclusao}
            onChange={(e) => setForm((f) => ({ ...f, dataInclusao: e.target.value }))}
          />
        </Field>
        <div className="col-span-3 flex justify-end">
          <button
            type="submit"
            disabled={add.isPending}
            className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white disabled:opacity-50"
          >
            {add.isPending ? "Salvando..." : "Adicionar"}
          </button>
        </div>
      </form>
    </div>
  );
}

// Inline-editable row for a single Sala.
function SalaRow({
  sala,
  uniCod,
}: {
  sala: Sala;
  uniCod: number;
}) {
  const [editing, setEditing] = useState(false);
  const [nome, setNome] = useState(sala.nome ?? "");
  const [status, setStatus] = useState(sala.status ?? "");
  const del = useDeleteSala(uniCod);
  const update = useUpdateSala(uniCod, sala.salaCodigo);

  function save() {
    update.mutate({ nome: nome || null, status: status || null });
    setEditing(false);
  }

  return (
    <tr className="border-b hover:bg-gray-50">
      <td className="px-3 py-2">{sala.salaCodigo}</td>
      <td className="px-3 py-2">
        {editing ? (
          <input
            className="input"
            value={nome}
            maxLength={50}
            onChange={(e) => setNome(e.target.value)}
            aria-label="Nome da sala"
          />
        ) : (
          sala.nome ?? "-"
        )}
      </td>
      <td className="px-3 py-2">
        {editing ? (
          <select
            className="input"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            aria-label="Status da sala"
          >
            <option value="">--</option>
            <option value="A">Ativo</option>
            <option value="I">Inativo</option>
          </select>
        ) : (
          sala.status ?? "-"
        )}
      </td>
      <td className="px-3 py-2 text-right">
        {editing ? (
          <button
            type="button"
            disabled={update.isPending}
            className="mr-3 rounded bg-blue-600 px-3 py-1 text-sm text-white disabled:opacity-50"
            onClick={save}
          >
            {update.isPending ? "..." : "Salvar"}
          </button>
        ) : (
          <button
            type="button"
            className="mr-3 text-blue-600 text-sm"
            onClick={() => setEditing(true)}
          >
            Editar
          </button>
        )}
        {editing && (
          <button
            type="button"
            className="mr-3 text-sm"
            onClick={() => {
              setNome(sala.nome ?? "");
              setStatus(sala.status ?? "");
              setEditing(false);
            }}
          >
            Cancelar
          </button>
        )}
        <button
          type="button"
          className="text-red-600 text-sm"
          onClick={() => {
            if (confirm("Excluir sala?")) del.mutate(sala.salaCodigo);
          }}
        >
          Excluir
        </button>
      </td>
    </tr>
  );
}

function SalasTab({ uniCod }: { uniCod: number }) {
  const { data: rows = [], isLoading } = useSalas(uniCod);
  const add = useAddSala(uniCod);
  const [form, setForm] = useState({ salaCodigo: "", nome: "", status: "" });

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const cod = Number(form.salaCodigo);
    if (!cod) return;
    add.mutate({
      salaCodigo: cod,
      nome: form.nome || null,
      status: form.status || null,
    });
    setForm({ salaCodigo: "", nome: "", status: "" });
  }

  if (isLoading) return <p className="p-4 text-sm">Carregando...</p>;

  return (
    <div className="grid gap-4">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-3 py-2">Código</th>
            <th className="px-3 py-2">Nome</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((sala: Sala) => (
            <SalaRow key={sala.salaCodigo} sala={sala} uniCod={uniCod} />
          ))}
          {rows.length === 0 && (
            <tr>
              <td colSpan={4} className="px-3 py-4 text-center text-gray-500">
                Nenhuma sala cadastrada.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <form onSubmit={handleAdd} className="grid grid-cols-3 gap-3 rounded border p-3">
        <legend className="col-span-3 text-sm font-medium mb-1">Adicionar sala</legend>
        <Field label="Código *">
          <input
            type="number"
            className="input"
            value={form.salaCodigo}
            onChange={(e) => setForm((f) => ({ ...f, salaCodigo: e.target.value }))}
            required
          />
        </Field>
        <Field label="Nome">
          <input
            className="input"
            maxLength={50}
            value={form.nome}
            onChange={(e) => setForm((f) => ({ ...f, nome: e.target.value }))}
          />
        </Field>
        <Field label="Status">
          <select
            className="input"
            value={form.status}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}
          >
            <option value="">--</option>
            <option value="A">Ativo</option>
            <option value="I">Inativo</option>
          </select>
        </Field>
        <div className="col-span-3 flex justify-end">
          <button
            type="submit"
            disabled={add.isPending}
            className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white disabled:opacity-50"
          >
            {add.isPending ? "Salvando..." : "Adicionar"}
          </button>
        </div>
      </form>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main form page
// ---------------------------------------------------------------------------

export default function UnidadeFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const isAdmin = hasRole("SAUDE_ADMIN");

  const isEdit = params.codigo != null && params.codigo !== "nova";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useUnidade(codigo);
  const create = useCreateUnidade();
  const update = useUpdateUnidade(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("dados-gerais");

  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<UnidadeFormValues>({ resolver: zodResolver(unidadeFormSchema) });

  const watchExterno = watch("externo");

  // Track first reset so we don't re-reset when user edits
  const hasReset = useRef(false);

  useEffect(() => {
    if (existing && !hasReset.current) {
      hasReset.current = true;
      reset({
        nome: existing.nome,
        razaoSocial: existing.razaoSocial ?? "",
        cnpj: existing.cnpj,
        cep: existing.cep,
        endereco: existing.endereco,
        enderecoNumero: existing.enderecoNumero,
        enderecoComplemento: existing.enderecoComplemento ?? "",
        bairro: existing.bairro,
        telefone: existing.telefone ?? "",
        fax: existing.fax ?? "",
        email: existing.email ?? "",
        licencaFuncionamento: existing.licencaFuncionamento ?? "",
        responsavel: existing.responsavel ?? "",
        orgaoEmissor: existing.orgaoEmissor ?? "",
        sigla: existing.sigla ?? "",
        cnes: existing.cnes ?? undefined,
        municipioCodigo: existing.municipioCodigo ?? undefined,
        distritoCodigo: existing.distritoCodigo ?? undefined,
        tipoUnidadeCodigo: existing.tipoUnidadeCodigo ?? undefined,
        situacao: existing.situacao ?? undefined,
        gestao: existing.gestao ?? undefined,
        bpa: existing.bpa ?? undefined,
        sipni: existing.sipni ?? undefined,
        sisPreNatal: existing.sisPreNatal ?? undefined,
        hiperdia: existing.hiperdia ?? undefined,
        psf: existing.psf ?? undefined,
        sia: existing.sia ?? "",
        siaSus: existing.siaSus ?? "",
        scnesId: existing.scnesId ?? "",
        exportarEsus: existing.exportarEsus ?? false,
        exportarBnafar: existing.exportarBnafar ?? false,
        externo: existing.externo ?? false,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: UnidadeFormValues) {
    setServerError(null);
    try {
      if (isEdit) {
        await update.mutateAsync(values);
      } else {
        const created = await create.mutateAsync(values);
        // Navigate to edit page so sub-resource tabs become available
        navigate(`/unidades/${created.codigo}`, { replace: true });
        return;
      }
      navigate("/unidades");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof UnidadeFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  const visibleTabs = ALL_TABS.filter((t) => !t.editOnly || isEdit);

  return (
    <section className="p-6 max-w-5xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar unidade #${codigo}` : "Nova unidade de atendimento"}
      </h1>

      {/* Tab bar */}
      <div
        role="tablist"
        aria-label="Seções da unidade"
        className="mb-4 flex gap-1 border-b"
      >
        {visibleTabs.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={activeTab === t.id}
            aria-controls={`tabpanel-${t.id}`}
            id={`tab-${t.id}`}
            onClick={() => setActiveTab(t.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === t.id
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-600 hover:text-gray-900"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {serverError && (
        <p role="alert" className="mb-3 text-red-600 text-sm">
          {serverError}
        </p>
      )}

      {/* Tabs 1–2 share the same <form>; sub-resource tabs are separate forms */}
      {(activeTab === "dados-gerais" || activeTab === "configuracoes") && (
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          {/* ---- TAB 1: Dados Gerais ---- */}
          <div
            role="tabpanel"
            id="tabpanel-dados-gerais"
            aria-labelledby="tab-dados-gerais"
            hidden={activeTab !== "dados-gerais"}
            className="grid gap-4"
          >
            {/* Identificação */}
            <fieldset className="grid gap-3 rounded border p-4">
              <legend className="px-1 text-sm font-medium">Identificação</legend>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Nome *" error={errors.nome?.message}>
                  <input {...register("nome")} className="input" maxLength={50} />
                </Field>
                <Field label="Razão Social" error={errors.razaoSocial?.message}>
                  <input {...register("razaoSocial")} className="input" maxLength={50} />
                </Field>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <Field label="CNPJ *" error={errors.cnpj?.message}>
                  <input
                    {...register("cnpj")}
                    className="input"
                    maxLength={18}
                    placeholder="XX.XXX.XXX/XXXX-XX"
                  />
                </Field>
                <Field label="Sigla" error={errors.sigla?.message}>
                  <input {...register("sigla")} className="input" maxLength={6} />
                </Field>
                <Field label="Tipo Unidade (cód.)" error={errors.tipoUnidadeCodigo?.message}>
                  <input
                    type="number"
                    {...register("tipoUnidadeCodigo")}
                    className="input"
                  />
                </Field>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <Field label="Situação" error={errors.situacao?.message}>
                  <select {...register("situacao")} className="input">
                    <option value="">--</option>
                    <option value="1">Ativado</option>
                    <option value="2">Desativado</option>
                  </select>
                </Field>
                <Field label="Gestão" error={errors.gestao?.message}>
                  <select {...register("gestao")} className="input">
                    <option value="">--</option>
                    <option value="1">Municipal</option>
                    <option value="2">Estadual</option>
                  </select>
                </Field>
                <Field label="CNES" error={errors.cnes?.message}>
                  <input type="number" {...register("cnes")} className="input" />
                </Field>
              </div>
            </fieldset>

            {/* Endereço */}
            <fieldset className="grid gap-3 rounded border p-4">
              <legend className="px-1 text-sm font-medium">Endereço</legend>
              <div className="grid grid-cols-3 gap-4">
                <Field label="CEP *" error={errors.cep?.message}>
                  <input {...register("cep")} className="input" maxLength={8} />
                </Field>
                <Field label="Município (cód.) *" error={errors.municipioCodigo?.message}>
                  <input
                    type="number"
                    {...register("municipioCodigo")}
                    className="input"
                  />
                </Field>
                <Field label="Distrito (cód.)" error={errors.distritoCodigo?.message}>
                  <input
                    type="number"
                    {...register("distritoCodigo")}
                    className="input"
                  />
                </Field>
              </div>
              <Field label="Endereço *" error={errors.endereco?.message}>
                <input {...register("endereco")} className="input" maxLength={70} />
              </Field>
              <div className="grid grid-cols-3 gap-4">
                <Field label="Número *" error={errors.enderecoNumero?.message}>
                  <input
                    {...register("enderecoNumero")}
                    className="input"
                    maxLength={10}
                  />
                </Field>
                <Field label="Complemento" error={errors.enderecoComplemento?.message}>
                  <input
                    {...register("enderecoComplemento")}
                    className="input"
                    maxLength={40}
                  />
                </Field>
                <Field label="Bairro *" error={errors.bairro?.message}>
                  <input {...register("bairro")} className="input" maxLength={70} />
                </Field>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Telefone" error={errors.telefone?.message}>
                  <input
                    {...register("telefone")}
                    className="input"
                    maxLength={20}
                    placeholder="(44) 3221-5000"
                  />
                </Field>
                <Field label="Fax" error={errors.fax?.message}>
                  <input
                    {...register("fax")}
                    className="input"
                    maxLength={20}
                    placeholder="(44) 3221-5000"
                  />
                </Field>
              </div>
              <Field label="E-mail" error={errors.email?.message}>
                <input
                  type="email"
                  {...register("email")}
                  className="input"
                  maxLength={70}
                />
              </Field>
            </fieldset>

            {/* Profissionais Responsáveis */}
            <fieldset className="grid gap-3 rounded border p-4">
              <legend className="px-1 text-sm font-medium">Profissionais Responsáveis</legend>
              <div className="grid grid-cols-2 gap-4">
                <Field
                  label="Resp. Profissional (cód.)"
                  error={errors.respProfissionalCodigo?.message}
                >
                  <input
                    type="number"
                    {...register("respProfissionalCodigo")}
                    className="input"
                  />
                </Field>
                <Field label="Diretor (cód.)" error={errors.diretorCodigo?.message}>
                  <input
                    type="number"
                    {...register("diretorCodigo")}
                    className="input"
                  />
                </Field>
                <Field label="Auditor (cód.)" error={errors.auditorCodigo?.message}>
                  <input
                    type="number"
                    {...register("auditorCodigo")}
                    className="input"
                  />
                </Field>
                <Field label="Autorizador (cód.)" error={errors.autorizadorCodigo?.message}>
                  <input
                    type="number"
                    {...register("autorizadorCodigo")}
                    className="input"
                  />
                </Field>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Órgão Emissor" error={errors.orgaoEmissor?.message}>
                  <input
                    {...register("orgaoEmissor")}
                    className="input"
                    maxLength={10}
                  />
                </Field>
                <Field label="Licença de Funcionamento" error={errors.licencaFuncionamento?.message}>
                  <input
                    {...register("licencaFuncionamento")}
                    className="input"
                    maxLength={10}
                  />
                </Field>
              </div>
              <Field label="Responsável" error={errors.responsavel?.message}>
                <input {...register("responsavel")} className="input" maxLength={50} />
              </Field>
            </fieldset>
          </div>

          {/* ---- TAB 2: Configurações ---- */}
          <div
            role="tabpanel"
            id="tabpanel-configuracoes"
            aria-labelledby="tab-configuracoes"
            hidden={activeTab !== "configuracoes"}
            className="grid gap-4"
          >
            {!isAdmin && (
              <p
                role="note"
                className="rounded border border-yellow-300 bg-yellow-50 px-4 py-3 text-sm text-yellow-800"
              >
                Você não tem permissão para alterar configurações de unidade (requer
                SAUDE_ADMIN). Os campos abaixo são exibidos somente para leitura.
              </p>
            )}

            {/* Programas */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">Programas</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="Realiza BPA">
                  <input type="checkbox" {...register("bpa")} />
                </CheckField>
                <CheckField label="Realiza SIPNI">
                  <input type="checkbox" {...register("sipni")} />
                </CheckField>
                <CheckField label="Realiza SisPré-Natal">
                  <input type="checkbox" {...register("sisPreNatal")} />
                </CheckField>
                <CheckField label="Realiza Hiperdia">
                  <input type="checkbox" {...register("hiperdia")} />
                </CheckField>
                <CheckField label="PSF">
                  <input type="checkbox" {...register("psf")} />
                </CheckField>
                <CheckField label="Atenção Secundária">
                  <input type="checkbox" {...register("atencaoSecundaria")} />
                </CheckField>
              </div>
            </fieldset>

            {/* Exportações */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">Exportações</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="Exportar ESUS">
                  <input type="checkbox" {...register("exportarEsus")} />
                </CheckField>
                <CheckField label="Exportar BNAFAR">
                  <input type="checkbox" {...register("exportarBnafar")} />
                </CheckField>
              </div>
            </fieldset>

            {/* Regras de Cadastro */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">Regras de Cadastro</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="Permite cadastro sem CNS">
                  <input type="checkbox" {...register("cadastroCns")} />
                </CheckField>
                <CheckField label="Permite cadastro sem CPF">
                  <input type="checkbox" {...register("cadastroCpf")} />
                </CheckField>
                <CheckField label="Permite cadastro sem endereço">
                  <input type="checkbox" {...register("cadastroEndereco")} />
                </CheckField>
                <CheckField label="Atendimento sem CNS">
                  <input type="checkbox" {...register("atendimentoSemCns")} />
                </CheckField>
                <CheckField label="Atendimento sem endereço">
                  <input type="checkbox" {...register("atendimentoSemEndereco")} />
                </CheckField>
                <CheckField label="Encaminhamento fisioterapia">
                  <input type="checkbox" {...register("encaminhamentoFisioterapia")} />
                </CheckField>
              </div>
            </fieldset>

            {/* Bloqueios */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">Bloqueios e Avisos</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="Bloqueio pac. sem cad. individual">
                  <input type="checkbox" {...register("bloqueioPacSemCadInd")} />
                </CheckField>
                <CheckField label="Bloqueio lançamento PCD aut.">
                  <input type="checkbox" {...register("bloqueioLancPcdAut")} />
                </CheckField>
                <CheckField label="Bloqueio disp. pac. externo">
                  <input type="checkbox" {...register("bloqueioDispPacExt")} />
                </CheckField>
                <CheckField label="Bloqueio agend. sol. exame pac. ext.">
                  <input type="checkbox" {...register("bloqueioAgendSolExaPacExt")} />
                </CheckField>
                <CheckField label="Aviso vacina atrasada">
                  <input type="checkbox" {...register("avisoVacinaAtrasada")} />
                </CheckField>
                <CheckField label="Baixa rem. sem CNS">
                  <input type="checkbox" {...register("baixaRemSemCns")} />
                </CheckField>
                <CheckField label="Painel">
                  <input type="checkbox" {...register("painel")} />
                </CheckField>
              </div>
            </fieldset>

            {/* Externo */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">Externo</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="Unidade Externa">
                  <input type="checkbox" {...register("externo")} />
                </CheckField>
                <CheckField label="Recepção Intermed. MPP">
                  <input type="checkbox" {...register("recepcaoIntermedMpp")} />
                </CheckField>
                <CheckField label="Recepção Intermed. MPP (Imp.)">
                  <input type="checkbox" {...register("recepcaoIntermedMppImp")} />
                </CheckField>
              </div>
              {watchExterno && (
                <div className="mt-2">
                  <Field label="Fornecedor (cód.)" error={errors.forPesCod?.message}>
                    <input
                      type="number"
                      {...register("forPesCod")}
                      className="input max-w-xs"
                    />
                  </Field>
                </div>
              )}
            </fieldset>

            {/* SIA/SUS */}
            <fieldset className="grid gap-3 rounded border p-4" disabled={!isAdmin}>
              <legend className="px-1 text-sm font-medium">SIA/SUS</legend>
              <div className="grid grid-cols-3 gap-3">
                <CheckField label="SIA">
                  <input type="checkbox" {...register("sia")} />
                </CheckField>
                <CheckField label="SIA SUS">
                  <input type="checkbox" {...register("siaSus")} />
                </CheckField>
              </div>
              <div className="max-w-xs">
                <Field label="SCNES ID" error={errors.scnesId?.message}>
                  <input {...register("scnesId")} className="input" maxLength={20} />
                </Field>
              </div>
            </fieldset>
          </div>

          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded bg-blue-600 px-4 py-2 text-white text-sm disabled:opacity-50"
            >
              {isSubmitting ? "Salvando..." : "Salvar"}
            </button>
            <button
              type="button"
              onClick={() => navigate("/unidades")}
              className="px-4 py-2 text-sm"
            >
              Cancelar
            </button>
          </div>
        </form>
      )}

      {/* Sub-resource tabs — only rendered in edit mode */}
      {isEdit && codigo != null && (
        <>
          {activeTab === "hiperdia" && (
            <div
              role="tabpanel"
              id="tabpanel-hiperdia"
              aria-labelledby="tab-hiperdia"
            >
              <HiperdiaTab uniCod={codigo} />
            </div>
          )}
          {activeTab === "sisprenatal" && (
            <div
              role="tabpanel"
              id="tabpanel-sisprenatal"
              aria-labelledby="tab-sisprenatal"
            >
              <SisPreNatalTab uniCod={codigo} />
            </div>
          )}
          {activeTab === "nutricionistas" && (
            <div
              role="tabpanel"
              id="tabpanel-nutricionistas"
              aria-labelledby="tab-nutricionistas"
            >
              <NutricionistasTab uniCod={codigo} />
            </div>
          )}
          {activeTab === "salas" && (
            <div
              role="tabpanel"
              id="tabpanel-salas"
              aria-labelledby="tab-salas"
            >
              <SalasTab uniCod={codigo} />
            </div>
          )}
        </>
      )}
    </section>
  );
}

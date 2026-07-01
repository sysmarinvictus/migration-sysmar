import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { AxiosError } from "axios";
import {
  COR_OPTIONS,
  ESTADO_CIVIL_OPTIONS,
  NACIONALIDADE_OPTIONS,
  SEXO_OPTIONS,
  pessoaFormSchema,
  type PessoaFormValues,
  type PessoaFormOutput,
} from "./schema";
import {
  useCreatePessoa,
  usePessoaCadastro,
  useUpdatePessoa,
  useDeletePessoa,
  type PessoaCadastroRequest,
  type ProblemDetail,
} from "./api";

function Field({ label, required, error, children }: {
  label: string; required?: boolean; error?: string; children: React.ReactNode;
}) {
  return (
    <label className="grid gap-1">
      <span className="text-sm font-medium">
        {label} {required && <span className="text-red-600">*</span>}
      </span>
      {children}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  );
}

const input = "rounded border px-2 py-1.5 text-sm";

function num(v: unknown): number | null {
  if (v === "" || v === null || v === undefined) return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}
function str(v: unknown): string | null {
  const s = (v ?? "").toString().trim();
  return s === "" ? null : s;
}

function toRequest(v: PessoaFormOutput): PessoaCadastroRequest {
  return {
    nome: v.nome.trim(),
    nomeSocial: str(v.nomeSocial),
    usaNomeSocial: v.usaNomeSocial,
    nomePai: str(v.nomePai),
    nomeMae: str(v.nomeMae),
    cpfCnpj: str(v.cpfCnpj),
    cns: v.cns.trim(),
    rgIe: str(v.rgIe),
    rgUf: str(v.rgUf),
    dataNascimento: v.dataNascimento,
    sexo: v.sexo,
    corCod: v.corCod,
    etniaCod: num(v.etniaCod),
    estadoCivilCod: num(v.estadoCivilCod),
    nacionalidadeTipo: v.nacionalidadeTipo,
    paisCod: num(v.paisCod),
    municipioNascCod: num(v.municipioNascCod),
    dataNaturalizacao: str(v.dataNaturalizacao),
    numeroPortaria: str(v.numeroPortaria),
    dataEntradaPais: str(v.dataEntradaPais),
    cep: v.cep.trim(),
    tipoLogradouroCod: v.tipoLogradouroCod,
    endereco: v.endereco.trim(),
    enderecoNumero: v.enderecoNumero.trim(),
    enderecoComplemento: str(v.enderecoComplemento),
    bairroCod: v.bairroCod,
    municipioCod: v.municipioCod,
    telefone: str(v.telefone),
    celular: str(v.celular),
    email: str(v.email),
    cboCod: str(v.cboCod),
    observacao: str(v.observacao),
  };
}

/**
 * Pessoa cadastro form (SAU_PESF) — create/edit a person over SYS_PES. Client validation mirrors the
 * key required/cross-field rules; the backend enforces the full set and its 422/409 messages surface
 * inline. Subtype provisioning is out of scope (a person is created here; roles are assigned elsewhere).
 */
export default function PessoaFormPage() {
  const { id } = useParams<{ id: string }>();
  const idNum = id ? Number(id) : null;
  const isEdit = idNum != null;
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const { data: loaded } = usePessoaCadastro(isEdit ? idNum : null);
  const createMut = useCreatePessoa();
  const updateMut = useUpdatePessoa(idNum ?? 0);
  const deleteMut = useDeletePessoa();

  const {
    register, handleSubmit, reset, formState: { errors, isSubmitting },
  } = useForm<PessoaFormValues>({
    resolver: zodResolver(pessoaFormSchema),
    defaultValues: { usaNomeSocial: false, nacionalidadeTipo: 1 },
  });

  useEffect(() => {
    if (loaded) {
      reset({
        ...loaded,
        usaNomeSocial: !!loaded.usaNomeSocial,
        corCod: loaded.corCod ?? undefined,
        nacionalidadeTipo: loaded.nacionalidadeTipo ?? 1,
        tipoLogradouroCod: loaded.tipoLogradouroCod ?? undefined,
        bairroCod: loaded.bairroCod ?? undefined,
        municipioCod: loaded.municipioCod ?? undefined,
      } as PessoaFormValues);
    }
  }, [loaded, reset]);

  function onSubmit(values: PessoaFormValues) {
    setServerError(null);
    const body = toRequest(pessoaFormSchema.parse(values));
    const opts = {
      onSuccess: (r: { id: number }) => navigate(`/pessoas/${r.id}/editar`),
      onError: (e: unknown) => {
        const p = (e as AxiosError<ProblemDetail>)?.response?.data;
        setServerError(p?.detail ?? "Não foi possível salvar o cadastro.");
      },
    };
    if (isEdit) updateMut.mutate(body, opts);
    else createMut.mutate(body, opts);
  }

  function handleDelete() {
    if (!isEdit || !confirm("Excluir esta pessoa?")) return;
    setServerError(null);
    deleteMut.mutate(idNum, {
      onSuccess: () => navigate("/pessoas/novo"),
      onError: (e: unknown) => {
        const p = (e as AxiosError<ProblemDetail>)?.response?.data;
        setServerError(p?.detail ?? "Não foi possível excluir.");
      },
    });
  }

  const err = (k: keyof PessoaFormValues) => errors[k]?.message as string | undefined;

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="max-w-4xl p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">{isEdit ? `Pessoa #${idNum}` : "Nova pessoa"}</h1>
        <div className="flex gap-3">
          {isEdit && (
            <button type="button" onClick={handleDelete} disabled={deleteMut.isPending}
              className="rounded border border-red-300 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50">
              Excluir
            </button>
          )}
          <button type="submit" disabled={isSubmitting || createMut.isPending || updateMut.isPending}
            className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
            Salvar
          </button>
        </div>
      </div>

      {serverError && <p className="mb-3 rounded border border-red-300 bg-red-50 p-2 text-sm text-red-700">{serverError}</p>}

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Identificação</legend>
        <Field label="Nome" required error={err("nome")}><input className={input} {...register("nome")} /></Field>
        <Field label="Nome social" error={err("nomeSocial")}><input className={input} {...register("nomeSocial")} /></Field>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("usaNomeSocial")} /> Usa nome social
        </label>
        <div />
        <Field label="Nome do pai" error={err("nomePai")}><input className={input} {...register("nomePai")} /></Field>
        <Field label="Nome da mãe" error={err("nomeMae")}><input className={input} {...register("nomeMae")} /></Field>
      </fieldset>

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Documentos</legend>
        <Field label="CPF/CNPJ" error={err("cpfCnpj")}><input className={input} {...register("cpfCnpj")} /></Field>
        <Field label="CNS" required error={err("cns")}><input className={input} {...register("cns")} /></Field>
        <Field label="RG / IE" error={err("rgIe")}><input className={input} {...register("rgIe")} /></Field>
        <Field label="UF do RG" error={err("rgUf")}><input className={input} maxLength={2} {...register("rgUf")} /></Field>
      </fieldset>

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Nascimento e demografia</legend>
        <Field label="Data de nascimento" required error={err("dataNascimento")}>
          <input type="date" className={input} {...register("dataNascimento")} />
        </Field>
        <Field label="Sexo" required error={err("sexo")}>
          <select className={input} {...register("sexo")}>
            <option value="">Selecione…</option>
            {SEXO_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="Cor/Raça" required error={err("corCod")}>
          <select className={input} {...register("corCod")}>
            <option value="">Selecione…</option>
            {COR_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="Etnia (código, se indígena)" error={err("etniaCod")}>
          <input type="number" className={input} {...register("etniaCod")} />
        </Field>
        <Field label="Estado civil" error={err("estadoCivilCod")}>
          <select className={input} {...register("estadoCivilCod")}>
            <option value="">Selecione…</option>
            {ESTADO_CIVIL_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
      </fieldset>

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Nacionalidade</legend>
        <Field label="Nacionalidade" required error={err("nacionalidadeTipo")}>
          <select className={input} {...register("nacionalidadeTipo")}>
            {NACIONALIDADE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="País (código)" error={err("paisCod")}><input type="number" className={input} {...register("paisCod")} /></Field>
        <Field label="Município de nascimento (código)" error={err("municipioNascCod")}>
          <input type="number" className={input} {...register("municipioNascCod")} />
        </Field>
        <Field label="Data de naturalização" error={err("dataNaturalizacao")}>
          <input type="date" className={input} {...register("dataNaturalizacao")} />
        </Field>
        <Field label="Portaria de naturalização" error={err("numeroPortaria")}>
          <input className={input} {...register("numeroPortaria")} />
        </Field>
        <Field label="Data de entrada no Brasil" error={err("dataEntradaPais")}>
          <input type="date" className={input} {...register("dataEntradaPais")} />
        </Field>
      </fieldset>

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Endereço</legend>
        <Field label="CEP" required error={err("cep")}><input className={input} {...register("cep")} /></Field>
        <Field label="Tipo de logradouro (código)" required error={err("tipoLogradouroCod")}>
          <input type="number" className={input} {...register("tipoLogradouroCod")} />
        </Field>
        <Field label="Logradouro" required error={err("endereco")}><input className={input} {...register("endereco")} /></Field>
        <Field label="Número" required error={err("enderecoNumero")}><input className={input} {...register("enderecoNumero")} /></Field>
        <Field label="Complemento" error={err("enderecoComplemento")}><input className={input} {...register("enderecoComplemento")} /></Field>
        <Field label="Bairro (código)" required error={err("bairroCod")}><input type="number" className={input} {...register("bairroCod")} /></Field>
        <Field label="Município (código)" required error={err("municipioCod")}><input type="number" className={input} {...register("municipioCod")} /></Field>
      </fieldset>

      <fieldset className="mb-6 grid grid-cols-2 gap-4">
        <legend className="mb-1 text-sm font-semibold uppercase tracking-wide text-gray-500">Contato e outros</legend>
        <Field label="Telefone" error={err("telefone")}><input className={input} placeholder="(NN) NNNN-NNNN" {...register("telefone")} /></Field>
        <Field label="Celular" error={err("celular")}><input className={input} placeholder="(NN) 9NNNN-NNNN" {...register("celular")} /></Field>
        <Field label="E-mail" error={err("email")}><input className={input} {...register("email")} /></Field>
        <Field label="Ocupação (CBO)" error={err("cboCod")}><input className={input} maxLength={6} {...register("cboCod")} /></Field>
        <Field label="Observação" error={err("observacao")}><input className={input} {...register("observacao")} /></Field>
      </fieldset>
    </form>
  );
}

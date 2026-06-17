import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { unidadeFormSchema, type UnidadeFormValues } from "./schema";
import { useCreateUnidade, useUnidade, useUpdateUnidade } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

export default function UnidadeFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "nova";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useUnidade(codigo);
  const create = useCreateUnidade();
  const update = useUpdateUnidade(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } =
    useForm<UnidadeFormValues>({ resolver: zodResolver(unidadeFormSchema) });

  useEffect(() => {
    if (existing) {
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
        municipioCodigo: existing.municipioCodigo ?? undefined,
        distritoCodigo: existing.distritoCodigo ?? undefined,
        tipoUnidadeCodigo: existing.tipoUnidadeCodigo ?? undefined,
        situacao: existing.situacao ?? undefined,
        gestao: existing.gestao ?? undefined,
        cnes: existing.cnes ?? undefined,
        sigla: existing.sigla ?? "",
        email: existing.email ?? "",
        externo: existing.externo ?? false,
        exportarEsus: existing.exportarEsus ?? false,
        exportarBnafar: existing.exportarBnafar ?? false,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: UnidadeFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
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

  return (
    <section className="p-6 max-w-3xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar unidade #${codigo}` : "Nova unidade de atendimento"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
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
              <input {...register("cnpj")} className="input" maxLength={18} placeholder="XX.XXX.XXX/XXXX-XX" />
            </Field>
            <Field label="Sigla" error={errors.sigla?.message}>
              <input {...register("sigla")} className="input" maxLength={6} />
            </Field>
            <Field label="Tipo Unidade (cód.)" error={errors.tipoUnidadeCodigo?.message}>
              <input type="number" {...register("tipoUnidadeCodigo")} className="input" />
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
              <input type="number" {...register("municipioCodigo")} className="input" />
            </Field>
            <Field label="Distrito (cód.)" error={errors.distritoCodigo?.message}>
              <input type="number" {...register("distritoCodigo")} className="input" />
            </Field>
          </div>
          <Field label="Endereço *" error={errors.endereco?.message}>
            <input {...register("endereco")} className="input" maxLength={70} />
          </Field>
          <div className="grid grid-cols-3 gap-4">
            <Field label="Número *" error={errors.enderecoNumero?.message}>
              <input {...register("enderecoNumero")} className="input" maxLength={10} />
            </Field>
            <Field label="Complemento" error={errors.enderecoComplemento?.message}>
              <input {...register("enderecoComplemento")} className="input" maxLength={40} />
            </Field>
            <Field label="Bairro *" error={errors.bairro?.message}>
              <input {...register("bairro")} className="input" maxLength={70} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Telefone" error={errors.telefone?.message}>
              <input {...register("telefone")} className="input" maxLength={20} placeholder="(44) 3221-5000" />
            </Field>
            <Field label="Fax" error={errors.fax?.message}>
              <input {...register("fax")} className="input" maxLength={20} placeholder="(44) 3221-5000" />
            </Field>
          </div>
          <Field label="E-mail" error={errors.email?.message}>
            <input type="email" {...register("email")} className="input" maxLength={70} />
          </Field>
        </fieldset>

        {/* Programas */}
        <fieldset className="grid gap-3 rounded border p-4">
          <legend className="px-1 text-sm font-medium">Programas e Configurações</legend>
          <div className="grid grid-cols-3 gap-4">
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("exportarEsus")} /> Exportar ESUS
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("exportarBnafar")} /> Exportar BNAFAR
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("externo")} /> Unidade Externa
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("atencaoSecundaria")} /> Atenção Secundária
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("cadastroCns")} /> Cadastro CNS
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("cadastroCpf")} /> Cadastro CPF
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("atendimentoSemCns")} /> Atend. sem CNS
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register("painel")} /> Painel
            </label>
          </div>
        </fieldset>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/unidades")} className="px-4 py-2">
            Cancelar
          </button>
        </div>
      </form>
    </section>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <label className="grid gap-1">
      <span className="text-sm">{label}</span>
      {children}
      {error && <span className="text-sm text-red-600">{error}</span>}
    </label>
  );
}

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { distritoFormSchema, type DistritoFormValues } from "./schema";
import { useCreateDistrito, useDistrito, useUpdateDistrito } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_dis transaction form. Create + edit. */
export default function DistritoFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "novo";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useDistrito(codigo);
  const create = useCreateDistrito();
  const update = useUpdateDistrito(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DistritoFormValues>({ resolver: zodResolver(distritoFormSchema) });

  useEffect(() => {
    if (existing) {
      reset({
        nome: existing.nome,
        endereco: existing.endereco ?? "",
        numero: existing.numero ?? undefined,
        complemento: existing.complemento ?? "",
        cep: existing.cep ?? undefined,
        ddd: existing.ddd ?? "",
        telefone: existing.telefone ?? undefined,
        fax: existing.fax ?? undefined,
        tipoLogradouroCodigo: existing.tipoLogradouroCodigo ?? undefined,
        bairroCodigo: existing.bairroCodigo ?? undefined,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: DistritoFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/distritos");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof DistritoFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-2xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar distrito #${codigo}` : "Novo distrito sanitário"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <Field label="Nome *" error={errors.nome?.message}>
          <input {...register("nome")} className="input" maxLength={30} />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Endereço" error={errors.endereco?.message}>
            <input {...register("endereco")} className="input" maxLength={50} />
          </Field>
          <Field label="Número" error={errors.numero?.message}>
            <input type="number" {...register("numero")} className="input" />
          </Field>
        </div>

        <Field label="Complemento" error={errors.complemento?.message}>
          <input {...register("complemento")} className="input" maxLength={15} />
        </Field>

        <div className="grid grid-cols-3 gap-4">
          <Field label="CEP" error={errors.cep?.message}>
            <input type="number" {...register("cep")} className="input" />
          </Field>
          <Field label="DDD" error={errors.ddd?.message}>
            <input {...register("ddd")} className="input" maxLength={3} placeholder="ex: 44" />
          </Field>
          <Field label="Telefone" error={errors.telefone?.message}>
            <input type="number" {...register("telefone")} className="input" />
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Fax" error={errors.fax?.message}>
            <input type="number" {...register("fax")} className="input" />
          </Field>
          <Field label="Tipo de Logradouro (código)" error={errors.tipoLogradouroCodigo?.message}>
            <input type="number" {...register("tipoLogradouroCodigo")} className="input" />
          </Field>
        </div>

        <Field label="Bairro (código)" error={errors.bairroCodigo?.message}>
          <input type="number" {...register("bairroCodigo")} className="input" />
        </Field>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting}
            className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/distritos")} className="px-4 py-2">
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

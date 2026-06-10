import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { especialidadeFormSchema, type EspecialidadeFormValues } from "./schema";
import { useCreateEspecialidade, useEspecialidade, useUpdateEspecialidade } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_esp transaction form. Create + edit; Zod mirrors backend validation. */
export default function EspecialidadeFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "nova";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useEspecialidade(codigo);
  const create = useCreateEspecialidade();
  const update = useUpdateEspecialidade(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EspecialidadeFormValues>({ resolver: zodResolver(especialidadeFormSchema) });

  useEffect(() => {
    if (existing) {
      reset({
        codigo: existing.codigo,
        nome: existing.nome,
        situacao: existing.situacao ?? "",
        auxiliar: existing.auxiliar ?? false,
        cborCodigo: existing.cborCodigo ?? undefined,
        vagaMuitoUrgenteMin: existing.agenda?.vagaMuitoUrgenteMin ?? undefined,
        vagaMuitoUrgenteMax: existing.agenda?.vagaMuitoUrgenteMax ?? undefined,
        vagaNormalMin: existing.agenda?.vagaNormalMin ?? undefined,
        vagaNormalMax: existing.agenda?.vagaNormalMax ?? undefined,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: EspecialidadeFormValues) {
    setServerError(null);
    const body = {
      ...values,
      agenda: {
        vagaMuitoUrgenteMin: values.vagaMuitoUrgenteMin,
        vagaMuitoUrgenteMax: values.vagaMuitoUrgenteMax,
        vagaNormalMin: values.vagaNormalMin,
        vagaNormalMax: values.vagaNormalMax,
      },
    };
    try {
      if (isEdit) await update.mutateAsync(body);
      else await create.mutateAsync(body);
      navigate("/especialidades");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      // surface backend RFC-7807 field errors inline
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof EspecialidadeFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? "Editar especialidade" : "Nova especialidade"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <Field label="Código" error={errors.codigo?.message}>
          <input type="number" disabled={isEdit} {...register("codigo")} className="input" />
        </Field>
        <Field label="Nome" error={errors.nome?.message}>
          <input {...register("nome")} className="input" maxLength={50} />
        </Field>
        <Field label="Situação" error={errors.situacao?.message}>
          <input {...register("situacao")} className="input" maxLength={1} />
        </Field>
        <Field label="CBO (código)" error={errors.cborCodigo?.message}>
          <input type="number" {...register("cborCodigo")} className="input" />
        </Field>
        <label className="flex items-center gap-2">
          <input type="checkbox" {...register("auxiliar")} /> Auxiliar
        </label>

        <fieldset className="grid grid-cols-2 gap-3 rounded border p-3">
          <legend className="px-1 text-sm font-medium">Agenda — vagas</legend>
          <Field label="Muito urgente (mín)" error={errors.vagaMuitoUrgenteMin?.message}>
            <input type="number" {...register("vagaMuitoUrgenteMin")} className="input" />
          </Field>
          <Field label="Muito urgente (máx)" error={errors.vagaMuitoUrgenteMax?.message}>
            <input type="number" {...register("vagaMuitoUrgenteMax")} className="input" />
          </Field>
          <Field label="Normal (mín)" error={errors.vagaNormalMin?.message}>
            <input type="number" {...register("vagaNormalMin")} className="input" />
          </Field>
          <Field label="Normal (máx)" error={errors.vagaNormalMax?.message}>
            <input type="number" {...register("vagaNormalMax")} className="input" />
          </Field>
        </fieldset>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/especialidades")} className="px-4 py-2">
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

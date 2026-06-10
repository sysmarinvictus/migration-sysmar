import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { conselhoClasseFormSchema, type ConselhoClasseFormValues } from "./schema";
import { useConselhoClasse, useCreateConselhoClasse, useUpdateConselhoClasse } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_concla transaction form. Create + edit; Zod mirrors backend validation. */
export default function ConselhoClasseFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "novo";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useConselhoClasse(codigo);
  const create = useCreateConselhoClasse();
  const update = useUpdateConselhoClasse(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ConselhoClasseFormValues>({ resolver: zodResolver(conselhoClasseFormSchema) });

  useEffect(() => {
    if (existing) {
      reset({
        codigo: existing.codigo,
        sigla: existing.sigla ?? "",
        nome: existing.nome ?? "",
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: ConselhoClasseFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/conselhos-classe");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof ConselhoClasseFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? "Editar conselho de classe" : "Novo conselho de classe"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <Field label="Código" error={errors.codigo?.message}>
          <input type="number" disabled={isEdit} {...register("codigo")} className="input" />
        </Field>
        <Field label="Sigla" error={errors.sigla?.message}>
          <input {...register("sigla")} className="input" maxLength={10} />
        </Field>
        <Field label="Nome" error={errors.nome?.message}>
          <input {...register("nome")} className="input" maxLength={100} />
        </Field>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/conselhos-classe")} className="px-4 py-2">
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

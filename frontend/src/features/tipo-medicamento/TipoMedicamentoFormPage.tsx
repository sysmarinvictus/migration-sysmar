import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { tipoMedicamentoFormSchema, type TipoMedicamentoFormValues } from "./schema";
import { useCreateTipoMedicamento, useTipoMedicamento, useUpdateTipoMedicamento } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_tiprem transaction form. Create + edit; Zod mirrors backend validation. */
export default function TipoMedicamentoFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "novo";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useTipoMedicamento(codigo);
  const create = useCreateTipoMedicamento();
  const update = useUpdateTipoMedicamento(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<TipoMedicamentoFormValues>({ resolver: zodResolver(tipoMedicamentoFormSchema) });

  useEffect(() => {
    if (existing) {
      reset({ codigo: existing.codigo, descricao: existing.descricao });
    }
  }, [existing, reset]);

  async function onSubmit(values: TipoMedicamentoFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/tipos-medicamento");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof TipoMedicamentoFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? "Editar tipo de medicamento" : "Novo tipo de medicamento"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <Field label="Código" error={errors.codigo?.message}>
          <input type="number" disabled={isEdit} {...register("codigo")} className="input" />
        </Field>
        <Field label="Descrição" error={errors.descricao?.message}>
          <input {...register("descricao")} className="input" maxLength={50} />
        </Field>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/tipos-medicamento")} className="px-4 py-2">
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

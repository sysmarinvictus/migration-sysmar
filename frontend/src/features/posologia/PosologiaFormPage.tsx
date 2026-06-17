import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { posologiaFormSchema, type PosologiaFormValues } from "./schema";
import { useCreatePosologia, usePosologia, useUpdatePosologia } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_remobs transaction form. Create + edit. */
export default function PosologiaFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "nova";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = usePosologia(codigo);
  const create = useCreatePosologia();
  const update = useUpdatePosologia(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PosologiaFormValues>({ resolver: zodResolver(posologiaFormSchema) });

  useEffect(() => {
    if (existing) {
      reset({
        descricao: existing.descricao,
        internamento: existing.internamento ?? false,
        quantidadeDose: existing.quantidadeDose ?? undefined,
        medidaDose: existing.medidaDose ?? undefined,
        intervaloHoras: existing.intervaloHoras ?? undefined,
        duracaoDias: existing.duracaoDias ?? undefined,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: PosologiaFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/posologias");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof PosologiaFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar posologia #${codigo}` : "Nova posologia"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <Field label="Descrição *" error={errors.descricao?.message}>
          <input {...register("descricao")} className="input" maxLength={60} />
        </Field>

        <Field label="Prescrição de Internamento?" error={errors.internamento?.message}>
          <input type="checkbox" {...register("internamento")} />
        </Field>

        <Field label="Quantidade de Dose" error={errors.quantidadeDose?.message}>
          <input type="number" step="0.01" min="0"
            {...register("quantidadeDose", { setValueAs: (v) => v === "" ? null : Number(v) })}
            className="input" />
        </Field>

        <Field label="Unidade Medida da Dose" error={errors.medidaDose?.message}>
          <input type="number" min="0"
            {...register("medidaDose", { setValueAs: (v) => v === "" ? null : Number(v) })}
            className="input" />
        </Field>

        <Field label="Intervalo entre Doses (horas, 0–99)" error={errors.intervaloHoras?.message}>
          <input type="number" min="0" max="99"
            {...register("intervaloHoras", { setValueAs: (v) => v === "" ? null : Number(v) })}
            className="input" />
        </Field>

        <Field label="Duração do Tratamento (dias, 0–999)" error={errors.duracaoDias?.message}>
          <input type="number" min="0" max="999"
            {...register("duracaoDias", { setValueAs: (v) => v === "" ? null : Number(v) })}
            className="input" />
        </Field>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting}
            className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/posologias")} className="px-4 py-2">
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

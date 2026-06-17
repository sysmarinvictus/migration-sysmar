import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { bairroFormSchema, type BairroFormValues } from "./schema";
import { useCreateBairro, useBairro, useUpdateBairro } from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_bai transaction form. Create + edit. */
export default function BairroFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.codigo != null && params.codigo !== "novo";
  const codigo = isEdit ? Number(params.codigo) : null;

  const { data: existing } = useBairro(codigo);
  const create = useCreateBairro();
  const update = useUpdateBairro(codigo ?? 0);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<BairroFormValues>({ resolver: zodResolver(bairroFormSchema) });

  useEffect(() => {
    if (existing) reset({ nome: existing.nome });
  }, [existing, reset]);

  async function onSubmit(values: BairroFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/bairros");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof BairroFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar bairro #${codigo}` : "Novo bairro"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <label className="grid gap-1">
          <span className="text-sm">Nome *</span>
          <input {...register("nome")} className="input" maxLength={50} />
          {errors.nome && <span className="text-sm text-red-600">{errors.nome.message}</span>}
        </label>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting}
            className="rounded bg-blue-600 px-4 py-2 text-white">
            Salvar
          </button>
          <button type="button" onClick={() => navigate("/bairros")} className="px-4 py-2">
            Cancelar
          </button>
        </div>
      </form>
    </section>
  );
}

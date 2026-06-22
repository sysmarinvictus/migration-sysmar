import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { formaApresentacaoFormSchema, type FormaApresentacaoFormValues } from "./schema";
import {
  useCreateFormaApresentacao,
  useDeleteFormaApresentacao,
  useFormaApresentacao,
  useUpdateFormaApresentacao,
} from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_aprrem transaction form. Create + edit; Zod mirrors backend validation. */
export default function FormaApresentacaoFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.id != null && params.id !== "nova";
  const id = isEdit ? Number(params.id) : null;

  const { data: existing } = useFormaApresentacao(id);
  const create = useCreateFormaApresentacao();
  const update = useUpdateFormaApresentacao(id ?? 0);
  const remove = useDeleteFormaApresentacao();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormaApresentacaoFormValues>({ resolver: zodResolver(formaApresentacaoFormSchema) });

  useEffect(() => {
    if (existing) reset({ descricao: existing.descricao, abreviacao: existing.abreviacao });
  }, [existing, reset]);

  async function onSubmit(values: FormaApresentacaoFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values);
      else await create.mutateAsync(values);
      navigate("/formas-apresentacao");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof FormaApresentacaoFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  async function onDelete() {
    if (id == null) return;
    if (!confirm("Excluir esta forma de apresentação?")) return;
    setServerError(null);
    try {
      await remove.mutateAsync(id);
      navigate("/formas-apresentacao");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      setServerError(pd?.detail ?? "Não foi possível excluir.");
    }
  }

  return (
    <section className="p-6 max-w-xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? "Editar forma de apresentação" : "Nova forma de apresentação"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <label className="grid gap-1">
          <span>Descrição</span>
          <input {...register("descricao")} className="rounded border px-3 py-2" />
          {errors.descricao && <span role="alert" className="text-sm text-red-600">{errors.descricao.message}</span>}
        </label>

        <label className="grid gap-1">
          <span>Abreviação</span>
          <input {...register("abreviacao")} maxLength={5} className="rounded border px-3 py-2" />
          {errors.abreviacao && <span role="alert" className="text-sm text-red-600">{errors.abreviacao.message}</span>}
        </label>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            {isSubmitting ? "Salvando…" : "Salvar"}
          </button>
          {isEdit && (
            <button type="button" onClick={onDelete} className="rounded border border-red-600 px-4 py-2 text-red-600">
              Excluir
            </button>
          )}
          <button type="button" onClick={() => navigate("/formas-apresentacao")} className="px-4 py-2">
            Cancelar
          </button>
        </div>
      </form>
    </section>
  );
}

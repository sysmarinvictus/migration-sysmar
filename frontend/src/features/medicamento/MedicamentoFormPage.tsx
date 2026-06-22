import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { medicamentoFormSchema, type MedicamentoFormValues } from "./schema";
import {
  useCreateMedicamento,
  useDeleteMedicamento,
  useMedicamento,
  useUpdateMedicamento,
} from "./api";
import type { ProblemDetail } from "../../lib/apiClient";
import { AxiosError } from "axios";

/** Replaces the GeneXus sau_rem transaction form. Create + edit; Zod mirrors backend validation. */
export default function MedicamentoFormPage() {
  const params = useParams();
  const navigate = useNavigate();
  const isEdit = params.id != null && params.id !== "novo";
  const id = isEdit ? Number(params.id) : null;

  const { data: existing } = useMedicamento(id);
  const create = useCreateMedicamento();
  const update = useUpdateMedicamento(id ?? 0);
  const remove = useDeleteMedicamento();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<MedicamentoFormValues>({
    resolver: zodResolver(medicamentoFormSchema),
    defaultValues: { situacao: 1 },
  });

  useEffect(() => {
    if (existing) reset(existing as unknown as MedicamentoFormValues);
  }, [existing, reset]);

  const psicotropico = watch("psicotropico");

  async function onSubmit(values: MedicamentoFormValues) {
    setServerError(null);
    try {
      if (isEdit) await update.mutateAsync(values as Record<string, unknown>);
      else await create.mutateAsync(values as Record<string, unknown>);
      navigate("/medicamentos");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof MedicamentoFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? "Erro ao salvar.");
    }
  }

  async function onDelete() {
    if (id == null) return;
    if (!confirm("Excluir este medicamento?")) return;
    setServerError(null);
    try {
      await remove.mutateAsync(id);
      navigate("/medicamentos");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      setServerError(pd?.detail ?? "Não foi possível excluir.");
    }
  }

  const err = (f: keyof MedicamentoFormValues) =>
    errors[f] && (
      <span role="alert" className="text-sm text-red-600">
        {errors[f]?.message as string}
      </span>
    );

  return (
    <section className="p-6 max-w-2xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? "Editar medicamento" : "Novo medicamento"}
      </h1>
      {serverError && <p role="alert" className="mb-3 text-red-600">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
        <label className="grid gap-1">
          <span>Nome</span>
          <input {...register("nome")} className="rounded border px-3 py-2" />
          {err("nome")}
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="grid gap-1">
            <span>Tipo de medicamento (código)</span>
            <input type="number" {...register("tipoMedicamentoCodigo")} className="rounded border px-3 py-2" />
          </label>
          <label className="grid gap-1">
            <span>Concentração</span>
            <input {...register("concentracao")} className="rounded border px-3 py-2" />
          </label>
          <label className="grid gap-1">
            <span>DCB</span>
            <input {...register("dcbCodigo")} className="rounded border px-3 py-2" />
          </label>
          <label className="grid gap-1">
            <span>Forma de apresentação (código)</span>
            <input type="number" {...register("apresentacaoCodigo")} className="rounded border px-3 py-2" />
          </label>
          <label className="grid gap-1">
            <span>Tipo de produto</span>
            <select {...register("tipoProduto")} className="rounded border px-3 py-2">
              <option value="0">Nenhum</option>
              <option value="1">Básico</option>
              <option value="2">Estratégico</option>
              <option value="3">Próprio</option>
              <option value="4">Especializado</option>
            </select>
          </label>
          <label className="grid gap-1">
            <span>Situação</span>
            <select {...register("situacao")} className="rounded border px-3 py-2">
              <option value="1">Ativo</option>
              <option value="2">Inativo</option>
            </select>
          </label>
        </div>

        <fieldset className="grid grid-cols-2 gap-2 rounded border p-3">
          <legend className="px-1 text-sm text-gray-600">Classificação</legend>
          <label className="flex gap-2"><input type="checkbox" {...register("farmaciaBasica")} /> Farmácia básica</label>
          <label className="flex gap-2"><input type="checkbox" {...register("etico")} /> Ético</label>
          <label className="flex gap-2"><input type="checkbox" {...register("usarPosologia")} /> Usar posologia</label>
          <label className="flex gap-2"><input type="checkbox" {...register("omitirSaldo")} /> Omitir saldo</label>
          <label className="flex gap-2"><input type="checkbox" {...register("semRename")} /> Sem RENAME</label>
          <label className="flex gap-2"><input type="checkbox" {...register("controleEspecial")} /> Controle especial</label>
        </fieldset>

        <fieldset className="grid gap-2 rounded border p-3">
          <legend className="px-1 text-sm text-gray-600">Psicotrópico (Portaria 344/98)</legend>
          <label className="flex gap-2"><input type="checkbox" {...register("psicotropico")} /> Psicotrópico</label>
          {psicotropico === 1 && (
            <label className="grid gap-1">
              <span>Portaria</span>
              <input {...register("portariaPsicotropico")} className="rounded border px-3 py-2" />
              {err("portariaPsicotropico")}
            </label>
          )}
        </fieldset>

        <div className="grid grid-cols-2 gap-4">
          <label className="grid gap-1">
            <span>Valor hospitalar</span>
            <input type="number" step="0.0001" {...register("valorHospitalar")} className="rounded border px-3 py-2" />
          </label>
          <label className="grid gap-1">
            <span>Valor unitário</span>
            <input type="number" step="0.0001" {...register("valorUnitario")} className="rounded border px-3 py-2" />
          </label>
        </div>

        <fieldset className="grid gap-2 rounded border p-3">
          <legend className="px-1 text-sm text-gray-600">Medicamento Potencialmente Perigoso</legend>
          <label className="flex gap-2"><input type="checkbox" {...register("medicamentoPotencialmentePerigoso")} /> MPP</label>
          <label className="grid gap-1">
            <span>Efeitos</span>
            <textarea {...register("mppEfeitos")} className="rounded border px-3 py-2" />
          </label>
          {isEdit && (
            <label className="grid gap-1">
              <span>Motivo do cancelamento (ao desmarcar MPP)</span>
              <input {...register("mppCancelamentoMotivo")} className="rounded border px-3 py-2" />
            </label>
          )}
        </fieldset>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded bg-blue-600 px-4 py-2 text-white">
            {isSubmitting ? "Salvando…" : "Salvar"}
          </button>
          {isEdit && (
            <button type="button" onClick={onDelete} className="rounded border border-red-600 px-4 py-2 text-red-600">
              Excluir
            </button>
          )}
          <button type="button" onClick={() => navigate("/medicamentos")} className="px-4 py-2">
            Cancelar
          </button>
        </div>
      </form>
    </section>
  );
}

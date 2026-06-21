import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { useCreateImpedimento, useImpedimento, useUpdateImpedimento } from "./api";
import { impedimentoFormSchema, type ImpedimentoFormValues } from "./schema";

export default function ImpedimentoFormPage() {
  const { codigo } = useParams<{ codigo: string }>();
  const isEdit = codigo !== "novo";
  const codigoNum = isEdit ? Number(codigo) : 0;
  const navigate = useNavigate();

  const { data: existing, isLoading } = useImpedimento(codigoNum);
  const createMut = useCreateImpedimento();
  const updateMut = useUpdateImpedimento(codigoNum);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ImpedimentoFormValues>({
    resolver: zodResolver(impedimentoFormSchema),
  });

  useEffect(() => {
    if (existing) {
      reset({
        profissionalCodigo: existing.profissionalCodigo,
        especialidadeCodigo: existing.especialidadeCodigo ?? undefined,
        dataInicio: existing.dataInicio,
        dataFim: existing.dataFim,
        dataCadastro: existing.dataCadastro ?? undefined,
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: ImpedimentoFormValues) {
    try {
      if (isEdit) {
        await updateMut.mutateAsync({
          dataCadastro: values.dataCadastro ?? new Date().toISOString().slice(0, 10),
          dataInicio: values.dataInicio,
          dataFim: values.dataFim,
          profissionalCodigo: values.profissionalCodigo,
          especialidadeCodigo: values.especialidadeCodigo,
        });
      } else {
        await createMut.mutateAsync({
          dataCadastro: values.dataCadastro || null,
          dataInicio: values.dataInicio,
          dataFim: values.dataFim,
          profissionalCodigo: values.profissionalCodigo,
          especialidadeCodigo: values.especialidadeCodigo,
        });
      }
      navigate("/impedimentos");
    } catch (err: unknown) {
      const msg = (err as { title?: string })?.title ?? "Erro ao salvar impedimento.";
      alert(msg);
    }
  }

  if (isEdit && isLoading) return <p className="p-6 text-sm text-gray-500">Carregando…</p>;

  return (
    <div className="p-6 max-w-lg">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar Impedimento #${codigo}` : "Novo Impedimento"}
      </h1>

      {isEdit && existing && (
        <dl className="mb-4 rounded border bg-gray-50 p-3 text-sm grid grid-cols-2 gap-x-4 gap-y-1">
          <dt className="font-medium text-gray-600">Profissional</dt>
          <dd>{existing.profissionalNome ?? existing.profissionalCodigo}</dd>
          <dt className="font-medium text-gray-600">Especialidade</dt>
          <dd>{existing.especialidadeNome ?? existing.especialidadeCodigo}</dd>
          <dt className="font-medium text-gray-600">CBO</dt>
          <dd>{existing.cboDescricao ? `${existing.cboCode} — ${existing.cboDescricao}` : existing.cboCode ?? "—"}</dd>
        </dl>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Código do Profissional *</label>
          <input
            type="number"
            {...register("profissionalCodigo", { valueAsNumber: true })}
            className="mt-1 w-full rounded border px-2 py-1.5 text-sm"
          />
          {errors.profissionalCodigo && (
            <p className="mt-0.5 text-xs text-red-600">{errors.profissionalCodigo.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Código da Especialidade *</label>
          <input
            type="number"
            {...register("especialidadeCodigo", { valueAsNumber: true })}
            className="mt-1 w-full rounded border px-2 py-1.5 text-sm"
          />
          {errors.especialidadeCodigo && (
            <p className="mt-0.5 text-xs text-red-600">{errors.especialidadeCodigo.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Data Início *</label>
          <input
            type="date"
            {...register("dataInicio")}
            className="mt-1 w-full rounded border px-2 py-1.5 text-sm"
          />
          {errors.dataInicio && (
            <p className="mt-0.5 text-xs text-red-600">{errors.dataInicio.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Data Fim *</label>
          <input
            type="date"
            {...register("dataFim")}
            className="mt-1 w-full rounded border px-2 py-1.5 text-sm"
          />
          {errors.dataFim && (
            <p className="mt-0.5 text-xs text-red-600">{errors.dataFim.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Data de Cadastro</label>
          <input
            type="date"
            {...register("dataCadastro")}
            className="mt-1 w-full rounded border px-2 py-1.5 text-sm"
          />
          <p className="mt-0.5 text-xs text-gray-500">Deixe em branco para usar a data de hoje.</p>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "Salvando…" : "Salvar"}
          </button>
          <button
            type="button"
            onClick={() => navigate("/impedimentos")}
            className="rounded border px-4 py-2 text-sm hover:bg-gray-50"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}

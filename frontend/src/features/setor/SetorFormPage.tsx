import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { setorSchema, type SetorFormValues } from "./schema";
import { useSetor, useCreateSetor, useUpdateSetor } from "./api";

export default function SetorFormPage() {
  const { unidadeId, setorId } = useParams<{ unidadeId: string; setorId?: string }>();
  const uniId = Number(unidadeId);
  const sId = setorId ? Number(setorId) : null;
  const isEdit = sId != null;
  const navigate = useNavigate();

  const { data: existing } = useSetor(uniId, sId);
  const createMutation = useCreateSetor(uniId);
  const updateMutation = useUpdateSetor(uniId, sId ?? 0);

  const { register, handleSubmit, reset, formState: { errors } } = useForm<SetorFormValues>({
    resolver: zodResolver(setorSchema),
    defaultValues: { estocador: 0, situacao: "ativo" },
  });

  useEffect(() => {
    if (existing) reset({
      setorCod: existing.setorCod,
      nome: existing.nome,
      estocador: existing.estocador,
      situacao: existing.situacao as "ativo" | "inativo",
    });
  }, [existing, reset]);

  function onSubmit(values: SetorFormValues) {
    const mutation = isEdit ? updateMutation : createMutation;
    (mutation as typeof createMutation).mutate(values, {
      onSuccess: () => navigate(`/unidades/${uniId}/setores`),
    });
  }

  return (
    <section className="p-6 max-w-lg">
      <h1 className="text-xl font-semibold mb-4">
        {isEdit ? "Editar Setor" : "Novo Setor"} — Unidade {uniId}
      </h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {!isEdit && (
          <div>
            <label className="block text-sm font-medium">Código do Setor</label>
            <input type="number" {...register("setorCod")} className="mt-1 w-full rounded border px-3 py-2" />
            {errors.setorCod && <p className="text-red-600 text-sm">{errors.setorCod.message}</p>}
          </div>
        )}
        <div>
          <label className="block text-sm font-medium">Nome</label>
          <input {...register("nome")} className="mt-1 w-full rounded border px-3 py-2" />
          {errors.nome && <p className="text-red-600 text-sm">{errors.nome.message}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium">Situação</label>
          <select {...register("situacao")} className="mt-1 w-full rounded border px-3 py-2">
            <option value="ativo">Ativo</option>
            <option value="inativo">Inativo</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium">Estocador</label>
          <input type="number" min={0} max={9} {...register("estocador")} className="mt-1 w-24 rounded border px-3 py-2" />
        </div>
        {(createMutation.isError || updateMutation.isError) && (
          <p role="alert" className="text-red-600">Erro ao salvar setor.</p>
        )}
        <div className="flex gap-3">
          <button type="submit" className="rounded bg-blue-600 px-4 py-2 text-white">
            {isEdit ? "Salvar" : "Criar"}
          </button>
          <button type="button" onClick={() => navigate(`/unidades/${uniId}/setores`)}
            className="rounded border px-4 py-2">
            Cancelar
          </button>
        </div>
      </form>
    </section>
  );
}

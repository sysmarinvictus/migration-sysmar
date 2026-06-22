import { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import { AxiosError } from "axios";
import { maskCns, maskCpf } from "../../lib/masks";
import { useConselhosClasse } from "../conselho-classe/api";
import {
  profissionalFormSchema,
  type ProfissionalFormValues,
  type ProfissionalFormOutput,
  SITUACAO_ATIVO,
  SITUACAO_INATIVO,
} from "./schema";
import {
  useCreateProfissional,
  useProfissional,
  useUpdateProfissional,
  type ProblemDetail,
  type ProfissionalWriteRequest,
} from "./api";

function Field({
  label,
  required,
  error,
  hint,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="grid gap-1">
      <span className="text-sm font-medium">
        {label} {required && <span className="text-red-600">*</span>}
      </span>
      {children}
      {hint && !error && <span className="text-xs text-gray-500">{hint}</span>}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  );
}

/**
 * Replaces the sau_pro transaction form. Create + edit.
 *
 * On CREATE the user enters the existing Pessoa code (id) — it is required and NOT
 * auto-generated (R1). numeroCns is required (R3). The writable person sub-fields
 * (nome, cpfCnpj, telefone, celular) write back to SYS_PES (R2).
 *
 * SECURITY: NO certificate / signature / certificate-password inputs in v1.
 */
export default function ProfissionalFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = id != null && id !== "novo";
  const idNum = isEdit ? Number(id) : 0;
  const navigate = useNavigate();

  const { data: existing, isLoading } = useProfissional(isEdit ? idNum : null);
  const createMut = useCreateProfissional();
  const updateMut = useUpdateProfissional(idNum);
  const [serverError, setServerError] = useState<string | null>(null);

  // Conselho de classe options (small table; first page is enough for a select).
  const { data: conclaPage } = useConselhosClasse("", 0, 200);
  const conselhos = conclaPage?.content ?? [];

  const {
    register,
    handleSubmit,
    reset,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfissionalFormValues>({
    resolver: zodResolver(profissionalFormSchema),
    defaultValues: {
      exportaEsus: false,
      situacao: SITUACAO_ATIVO,
    },
  });

  useEffect(() => {
    if (existing) {
      reset({
        id: existing.id,
        numeroCns: existing.numeroCns?.trim() ?? "",
        numeroCr: existing.numeroCr?.trim() ?? "",
        ufConselho: existing.ufConselho?.trim() ?? "",
        conselhoClasseCod: existing.conselhoClasseCod ?? undefined,
        dataInicio: existing.dataInicio ?? "",
        dataFim: existing.dataFim ?? "",
        cnesId: existing.cnesId?.trim() ?? "",
        exportaEsus: existing.exportaEsus ?? false,
        situacao: existing.situacao ?? SITUACAO_ATIVO,
        nome: existing.nome ?? "",
        cpfCnpj: existing.cpfCnpj?.trim() ?? "",
        telefone: existing.telefone?.trim() ?? "",
        celular: existing.celular?.trim() ?? "",
      });
    }
  }, [existing, reset]);

  async function onSubmit(values: ProfissionalFormValues) {
    setServerError(null);
    // zod transforms applied → output type carries the cleaned values.
    const v = profissionalFormSchema.parse(values) as ProfissionalFormOutput;
    const body: ProfissionalWriteRequest = {
      id: v.id,
      numeroCns: v.numeroCns,
      numeroCr: v.numeroCr ?? null,
      ufConselho: v.ufConselho ?? null,
      conselhoClasseCod: v.conselhoClasseCod ?? null,
      dataInicio: v.dataInicio ?? null,
      dataFim: v.dataFim ?? null,
      cnesId: v.cnesId ?? null,
      exportaEsus: v.exportaEsus,
      situacao: v.situacao,
      nome: v.nome,
      cpfCnpj: v.cpfCnpj ?? null,
      telefone: v.telefone ?? null,
      celular: v.celular ?? null,
    };
    try {
      if (isEdit) await updateMut.mutateAsync(body);
      else await createMut.mutateAsync(body);
      navigate(isEdit ? `/profissionais/${idNum}` : "/profissionais");
    } catch (e) {
      const pd = (e as AxiosError<ProblemDetail>).response?.data;
      if (pd?.errors) {
        for (const [field, msg] of Object.entries(pd.errors)) {
          setError(field as keyof ProfissionalFormValues, { message: msg });
        }
      }
      setServerError(pd?.detail ?? pd?.title ?? "Erro ao salvar o profissional.");
    }
  }

  if (isEdit && isLoading) return <p className="p-6 text-sm text-gray-500">Carregando…</p>;

  return (
    <div className="p-6 max-w-2xl">
      <h1 className="mb-4 text-xl font-semibold">
        {isEdit ? `Editar Profissional #${idNum}` : "Novo Profissional"}
      </h1>
      {serverError && (
        <p role="alert" className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">
          {serverError}
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-5">
        <fieldset className="grid gap-4 rounded border p-4">
          <legend className="px-1 text-sm font-semibold text-gray-600">Pessoa</legend>

          <Field
            label="Código da Pessoa"
            required
            error={errors.id?.message}
            hint={
              isEdit
                ? undefined
                : "Informe o código de uma Pessoa já cadastrada — não é gerado automaticamente."
            }
          >
            <input
              type="number"
              disabled={isEdit}
              {...register("id", { valueAsNumber: true })}
              className="w-full rounded border px-2 py-1.5 text-sm disabled:bg-gray-100"
            />
          </Field>

          <Field label="Nome" required error={errors.nome?.message}>
            <input {...register("nome")} maxLength={100} className="w-full rounded border px-2 py-1.5 text-sm" />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Controller
              control={control}
              name="cpfCnpj"
              render={({ field }) => (
                <Field label="CPF/CNPJ" error={errors.cpfCnpj?.message}>
                  <input
                    inputMode="numeric"
                    value={field.value ?? ""}
                    onChange={(e) => field.onChange(maskCpf(e.target.value))}
                    className="w-full rounded border px-2 py-1.5 text-sm"
                  />
                </Field>
              )}
            />
            <Field label="" error={undefined}>
              <span className="sr-only">espaço</span>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Telefone" error={errors.telefone?.message} hint="(NN) NNNN-NNNN">
              <input {...register("telefone")} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
            <Field label="Celular" error={errors.celular?.message} hint="(NN) 9NNNN-NNNN">
              <input {...register("celular")} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
          </div>
        </fieldset>

        <fieldset className="grid gap-4 rounded border p-4">
          <legend className="px-1 text-sm font-semibold text-gray-600">Dados profissionais</legend>

          <Controller
            control={control}
            name="numeroCns"
            render={({ field }) => (
              <Field label="Número CNS" required error={errors.numeroCns?.message}>
                <input
                  inputMode="numeric"
                  value={field.value ?? ""}
                  onChange={(e) => field.onChange(maskCns(e.target.value))}
                  className="w-full rounded border px-2 py-1.5 text-sm"
                />
              </Field>
            )}
          />

          <div className="grid grid-cols-2 gap-4">
            <Field label="Registro (CR)" error={errors.numeroCr?.message}>
              <input {...register("numeroCr")} maxLength={20} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
            <Field label="UF Conselho" error={errors.ufConselho?.message}>
              <input
                {...register("ufConselho")}
                maxLength={2}
                className="w-full rounded border px-2 py-1.5 text-sm uppercase"
              />
            </Field>
          </div>

          <Field label="Conselho de Classe" error={errors.conselhoClasseCod?.message}>
            <select
              {...register("conselhoClasseCod", {
                setValueAs: (v) => (v === "" || v == null ? undefined : Number(v)),
              })}
              className="w-full rounded border px-2 py-1.5 text-sm"
            >
              <option value="">— Nenhum —</option>
              {conselhos.map((c) => (
                <option key={c.codigo} value={c.codigo}>
                  {c.sigla ? `${c.sigla} — ` : ""}
                  {c.nome ?? c.codigo}
                </option>
              ))}
            </select>
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Início de validade" error={errors.dataInicio?.message}>
              <input type="date" {...register("dataInicio")} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
            <Field label="Fim de validade" error={errors.dataFim?.message}>
              <input type="date" {...register("dataFim")} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="CNES" error={errors.cnesId?.message}>
              <input {...register("cnesId")} maxLength={20} className="w-full rounded border px-2 py-1.5 text-sm" />
            </Field>
            <Field label="Situação" required error={errors.situacao?.message}>
              <select
                {...register("situacao", { valueAsNumber: true })}
                className="w-full rounded border px-2 py-1.5 text-sm"
              >
                <option value={SITUACAO_ATIVO}>Ativo</option>
                <option value={SITUACAO_INATIVO}>Inativo</option>
              </select>
            </Field>
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...register("exportaEsus")} />
            Exporta para o e-SUS
          </label>
        </fieldset>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "Salvando…" : "Salvar"}
          </button>
          <button
            type="button"
            onClick={() => navigate(isEdit ? `/profissionais/${idNum}` : "/profissionais")}
            className="rounded border px-4 py-2 text-sm hover:bg-gray-50"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}

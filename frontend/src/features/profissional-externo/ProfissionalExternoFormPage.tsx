import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { z } from "zod";
import {
  useCreateProfissionalExterno,
  type ProfissionalExternoCreateRequest,
  type ProblemDetail,
} from "./api";

const schema = z.object({
  nome: z.string().trim().min(3, "Informe o nome").refine((v) => v.includes(" "), "Informe o sobrenome"),
  cns: z.string().trim().min(1, "Informe o CNS"),
  municipioCod: z.coerce.number().int().positive("Informe o município"),
  conselhoClasseCod: z.coerce.number().int().positive("Informe o conselho de classe"),
  numeroConselho: z.string().trim().min(1, "Informe o número do conselho"),
  dataFim: z.string().optional().or(z.literal("")),
});
type FormValues = z.input<typeof schema>;

function Field({ label, required, error, children }: {
  label: string; required?: boolean; error?: string; children: React.ReactNode;
}) {
  return (
    <label className="grid gap-1">
      <span className="text-sm font-medium">{label} {required && <span className="text-red-600">*</span>}</span>
      {children}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  );
}

const input = "rounded border px-2 py-1.5 text-sm";

/**
 * Cadastro de Profissional Externo (SAU_PESF_PROFEXT). Lean form → registers a person + an external
 * professional (SAU_PRO, externo) atomically. On success, opens the professional detail page.
 */
export default function ProfissionalExternoFormPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const createMut = useCreateProfissionalExterno();

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  function onSubmit(values: FormValues) {
    setServerError(null);
    const parsed = schema.parse(values);
    const body: ProfissionalExternoCreateRequest = {
      nome: parsed.nome.trim(),
      cns: parsed.cns.trim(),
      municipioCod: parsed.municipioCod,
      conselhoClasseCod: parsed.conselhoClasseCod,
      numeroConselho: parsed.numeroConselho.trim(),
      dataFim: parsed.dataFim ? parsed.dataFim : null,
    };
    createMut.mutate(body, {
      onSuccess: (r) => navigate(`/profissionais/${r.id}`),
      onError: (e: unknown) => {
        const p = (e as AxiosError<ProblemDetail>)?.response?.data;
        setServerError(p?.detail ?? "Não foi possível registrar o profissional externo.");
      },
    });
  }

  const err = (k: keyof FormValues) => errors[k]?.message as string | undefined;

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Novo profissional externo</h1>
        <button type="submit" disabled={isSubmitting || createMut.isPending}
          className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
          Salvar
        </button>
      </div>

      {serverError && <p className="mb-3 rounded border border-red-300 bg-red-50 p-2 text-sm text-red-700">{serverError}</p>}

      <div className="grid grid-cols-2 gap-4">
        <Field label="Nome" required error={err("nome")}><input className={input} {...register("nome")} /></Field>
        <Field label="CNS" required error={err("cns")}><input className={input} {...register("cns")} /></Field>
        <Field label="Município (código)" required error={err("municipioCod")}>
          <input type="number" className={input} {...register("municipioCod")} />
        </Field>
        <Field label="Conselho de classe (código)" required error={err("conselhoClasseCod")}>
          <input type="number" className={input} {...register("conselhoClasseCod")} />
        </Field>
        <Field label="Número do conselho" required error={err("numeroConselho")}>
          <input className={input} {...register("numeroConselho")} />
        </Field>
        <Field label="Fim de validade" error={err("dataFim")}>
          <input type="date" className={input} {...register("dataFim")} />
        </Field>
      </div>
    </form>
  );
}

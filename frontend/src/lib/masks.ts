/** pt-BR input masks / formatters — replace the GeneXus HMask widget. */

export const onlyDigits = (v: string) => v.replace(/\D/g, "");

export function maskCpf(v: string): string {
  const d = onlyDigits(v).slice(0, 11);
  return d
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

export function maskCns(v: string): string {
  const d = onlyDigits(v).slice(0, 15);
  return d.replace(/(\d{3})(\d{4})(\d{4})(\d{0,4})/, "$1 $2 $3 $4").trim();
}

export function maskCep(v: string): string {
  const d = onlyDigits(v).slice(0, 8);
  return d.replace(/(\d{5})(\d{1,3})$/, "$1-$2");
}

/** Format a number with the pt-BR decimal comma / thousand dot (mirrors GeneXus locale). */
export function formatNumberBR(n: number | null | undefined): string {
  if (n == null) return "";
  return new Intl.NumberFormat("pt-BR").format(n);
}

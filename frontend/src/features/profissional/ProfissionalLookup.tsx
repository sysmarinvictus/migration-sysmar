import { useEffect, useId, useRef, useState } from "react";
import { useProfissionalLookup, type LookupItem } from "./api";

interface Props {
  /** Currently selected professional id (the SYS_PES person code), or null. */
  value: number | null;
  onChange: (id: number | null, item: LookupItem | null) => void;
  /** Pre-resolved label for the current value (e.g. when editing an existing record). */
  initialLabel?: string | null;
  placeholder?: string;
  disabled?: boolean;
  id?: string;
}

/**
 * Async-search combobox replacing hpromptsau_pro — the prescriber selector / FK picker.
 * Used by other slices (e.g. SAU_RECESP) to choose a professional.
 *
 * Keyboard accessible (WCAG 2.1 AA): ArrowUp/Down to move, Enter to select, Escape to close;
 * the listbox is wired with aria-controls / aria-activedescendant.
 */
export default function ProfissionalLookup({
  value,
  onChange,
  initialLabel,
  placeholder = "Buscar profissional…",
  disabled,
  id,
}: Props) {
  const reactId = useId();
  const inputId = id ?? `prof-lookup-${reactId}`;
  const listId = `${inputId}-list`;

  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [label, setLabel] = useState<string>(initialLabel ?? "");
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (initialLabel != null) setLabel(initialLabel);
  }, [initialLabel]);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 250);
    return () => clearTimeout(t);
  }, [query]);

  const { data: items = [], isFetching } = useProfissionalLookup(
    debounced,
    open && debounced.length >= 1,
  );

  // Close on outside click.
  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, []);

  function select(item: LookupItem) {
    onChange(item.id, item);
    setLabel(`${item.id} — ${item.nome}`);
    setQuery("");
    setOpen(false);
    setActive(-1);
  }

  function clear() {
    onChange(null, null);
    setLabel("");
    setQuery("");
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setOpen(true);
      setActive((a) => Math.min(a + 1, items.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, 0));
    } else if (e.key === "Enter") {
      if (open && active >= 0 && items[active]) {
        e.preventDefault();
        select(items[active]);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  return (
    <div ref={rootRef} className="relative">
      {value != null && label ? (
        <div className="flex items-center gap-2">
          <span className="flex-1 rounded border bg-gray-50 px-2 py-1.5 text-sm">{label}</span>
          {!disabled && (
            <button
              type="button"
              onClick={clear}
              className="rounded border px-2 py-1 text-xs hover:bg-gray-100"
              aria-label="Limpar seleção de profissional"
            >
              Limpar
            </button>
          )}
        </div>
      ) : (
        <>
          <input
            id={inputId}
            type="text"
            role="combobox"
            aria-expanded={open}
            aria-controls={listId}
            aria-autocomplete="list"
            aria-activedescendant={active >= 0 ? `${listId}-opt-${active}` : undefined}
            autoComplete="off"
            disabled={disabled}
            placeholder={placeholder}
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setOpen(true);
              setActive(-1);
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={onKeyDown}
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          {open && debounced.length >= 1 && (
            <ul
              id={listId}
              role="listbox"
              className="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded border bg-white text-sm shadow"
            >
              {isFetching && <li className="px-2 py-1.5 text-gray-500">Buscando…</li>}
              {!isFetching && items.length === 0 && (
                <li className="px-2 py-1.5 text-gray-500">Nenhum profissional encontrado.</li>
              )}
              {items.map((item, i) => (
                <li
                  key={item.id}
                  id={`${listId}-opt-${i}`}
                  role="option"
                  aria-selected={i === active}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    select(item);
                  }}
                  onMouseEnter={() => setActive(i)}
                  className={`cursor-pointer px-2 py-1.5 ${i === active ? "bg-blue-100" : "hover:bg-gray-50"}`}
                >
                  <span className="text-gray-500">{item.id}</span> — {item.nome}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}

import fr from "./fr.json";
import en from "./en.json";

export type Locale = "fr" | "en";
const messages: Record<Locale, unknown> = { fr, en };
let current: Locale = "fr";

export function setLocale(l: Locale) {
  current = l;
}

export function getLocale(): Locale {
  return current;
}

function lookup(obj: unknown, path: string): string | undefined {
  const parts = path.split(".");
  let cur: unknown = obj;
  for (const p of parts) {
    if (cur && typeof cur === "object" && p in cur) {
      cur = (cur as Record<string, unknown>)[p];
    } else {
      return undefined;
    }
  }
  return typeof cur === "string" ? cur : undefined;
}

export function t(key: string, params?: Record<string, string | number>): string {
  let str =
    lookup(messages[current], key) ?? lookup(messages.fr, key) ?? key;
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      str = str.replace(new RegExp(`\\{${k}\\}`, "g"), String(v));
    }
  }
  return str;
}

export function useT() {
  return t;
}

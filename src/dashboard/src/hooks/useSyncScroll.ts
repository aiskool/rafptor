import { useEffect, type RefObject } from "react";

export function useSyncScroll(refs: RefObject<HTMLElement>[]) {
  useEffect(() => {
    const elems = refs.map((r) => r.current).filter((e): e is HTMLElement => !!e);
    if (elems.length < 2) return;
    let syncing = false;

    const handlers = elems.map((source) => {
      const handler = () => {
        if (syncing) return;
        syncing = true;
        const maxScrollSource = source.scrollHeight - source.clientHeight;
        const ratio = maxScrollSource > 0 ? source.scrollTop / maxScrollSource : 0;
        elems.forEach((target) => {
          if (target === source) return;
          const maxScrollTarget = target.scrollHeight - target.clientHeight;
          target.scrollTop = ratio * maxScrollTarget;
        });
        requestAnimationFrame(() => (syncing = false));
      };
      source.addEventListener("scroll", handler, { passive: true });
      return { source, handler };
    });

    return () => handlers.forEach(({ source, handler }) => source.removeEventListener("scroll", handler));
  }, [refs]);
}

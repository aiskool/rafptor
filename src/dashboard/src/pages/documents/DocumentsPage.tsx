import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { FileText, Plus, AlertCircle } from "lucide-react";
import { DocumentCard, type DocumentCardData } from "@/components/data/DocumentCard";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { Skeleton } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { listDocuments } from "@/api/documents";
import type { Document } from "@/api/types";
import { t } from "@/i18n";

type Filter = "all" | "converted" | "toCheck" | "errors";

const FILTER_TO_STATUS: Record<Filter, string | undefined> = {
  all: undefined,
  converted: "ACCEPTED",
  toCheck: "NEEDS_REVIEW",
  errors: "REJECTED",
};

const PAGE_SIZE = 20;

function toCardData(d: Document): DocumentCardData {
  return {
    id: d.id,
    name: d.sourceAfpName || d.id,
    pageCount: d.pageCount,
    fidelityScore: d.compositeScore,
    createdAt: d.createdAt,
    thumbnailUrl: `/api/documents/${d.id}/thumbnail`,
  };
}

export default function DocumentsPage() {
  const [filter, setFilter] = useState<Filter>("all");
  const [items, setItems] = useState<DocumentCardData[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const sentinel = useRef<HTMLDivElement>(null);

  const load = useCallback(
    async (newFilter: Filter, pageToLoad: number, replace: boolean) => {
      setLoading(true);
      setError(null);
      try {
        const response = await listDocuments({
          status: FILTER_TO_STATUS[newFilter],
          page: pageToLoad,
          size: PAGE_SIZE,
        });
        const mapped = response.content.map(toCardData);
        setItems((prev) => (replace ? mapped : [...prev, ...mapped]));
        const loaded = (replace ? 0 : items.length) + mapped.length;
        setHasMore(loaded < response.totalElements && mapped.length > 0);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "Erreur de chargement");
      } finally {
        setLoading(false);
      }
    },
    [items.length]
  );

  useEffect(() => {
    setPage(0);
    load(filter, 0, true);
    // `load` intentionally not in deps to avoid re-running on every item append.
  }, [filter]);

  useEffect(() => {
    if (!sentinel.current || !hasMore || loading) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          const next = page + 1;
          setPage(next);
          load(filter, next, false);
        }
      },
      { rootMargin: "400px" }
    );
    io.observe(sentinel.current);
    return () => io.disconnect();
  }, [filter, hasMore, loading, page, load]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">{t("documents.title")}</h1>
        <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)}>
          <TabsList>
            <TabsTrigger value="all">{t("documents.filters.all")}</TabsTrigger>
            <TabsTrigger value="converted">{t("documents.filters.converted")}</TabsTrigger>
            <TabsTrigger value="toCheck">{t("documents.filters.toCheck")}</TabsTrigger>
            <TabsTrigger value="errors">{t("documents.filters.errors")}</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {error ? (
        <EmptyState
          icon={<AlertCircle className="h-5 w-5" />}
          title="Impossible de charger les documents"
          description={error}
        />
      ) : loading && items.length === 0 ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[5/7] w-full" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={<FileText className="h-5 w-5" />}
          title={
            filter === "all"
              ? "Aucun document pour l'instant"
              : "Aucun document dans ce filtre"
          }
          description={
            filter === "all"
              ? "Lancez une première analyse pour voir vos documents convertis ici."
              : undefined
          }
          action={
            filter === "all" ? (
              <Link to="/onboarding/welcome">
                <Button leftIcon={<Plus className="h-4 w-4" />}>Nouvelle analyse</Button>
              </Link>
            ) : undefined
          }
        />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {items.map((d) => (
              <DocumentCard key={d.id} doc={d} />
            ))}
          </div>
          {hasMore ? <div ref={sentinel} className="h-8 w-full" /> : null}
        </>
      )}
    </div>
  );
}
